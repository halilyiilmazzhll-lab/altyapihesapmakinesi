package com.example.egimhesabi.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.egimhesabi.data.*
import com.example.egimhesabi.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class StakeoutImportState(
    val neighborhoodId: Long,
    val fileName: String,
    val workbook: StakeoutWorkbookData,
    val sheetIndex: Int,
    val mapping: Map<StakeoutField, Int?>,
    val dataStartRow: Int = 3,
    val preview: StakeoutPreview
)

@OptIn(ExperimentalCoroutinesApi::class)
class StakeoutViewModel(
    private val districtDao: DistrictDao,
    private val dao: StakeoutDao,
    private val savedState: SavedStateHandle
) : ViewModel() {
    val districts = districtDao.getAllDistricts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val districtId = savedState.getStateFlow<Long?>("stakeoutDistrict", null)
    val neighborhoodId = savedState.getStateFlow<Long?>("stakeoutNeighborhood", null)
    val neighborhoods = districtId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else dao.neighborhoods(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val records = neighborhoodId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else dao.records(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importState = MutableStateFlow<StakeoutImportState?>(null)
    val importState = _importState.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private var exportSnapshot: Pair<List<StakeoutManholeEntity>, String>? = null

    fun prepareExport(snapshot: List<StakeoutManholeEntity>, title: String) {
        exportSnapshot = snapshot.toList() to title
    }
    fun exportPrepared(context: Context, uri: Uri?) {
        val snapshot = exportSnapshot
        exportSnapshot = null
        if (uri != null && snapshot != null) export(context, uri, snapshot.first, snapshot.second)
    }

    fun clearMessage() { _message.value = null }
    fun selectDistrict(id: Long) {
        if (_busy.value || _importState.value != null || districtId.value == id) return
        savedState["stakeoutNeighborhood"] = null
        savedState["stakeoutDistrict"] = id
    }
    fun selectNeighborhood(id: Long) {
        if (_busy.value || _importState.value != null) return
        if (neighborhoods.value.any { it.id == id && it.districtId == districtId.value }) {
            savedState["stakeoutNeighborhood"] = id
        }
    }
    fun addDistrict(name: String) = perform {
        require(_importState.value == null) { "Önce açık yükleme önizlemesini tamamlayın veya kapatın." }
        require(name.isNotBlank()) { "İlçe adı boş olamaz." }
        val id = districtDao.insertDistrict(DistrictEntity(name = name.trim()))
        require(id != -1L) { "Bu ilçe zaten tanımlı. Listeden seçebilirsiniz." }
        savedState["stakeoutNeighborhood"] = null
        savedState["stakeoutDistrict"] = id
    }
    fun addNeighborhood(name: String) {
        val parent = districtId.value ?: return
        perform {
            require(_importState.value == null) { "Önce açık yükleme önizlemesini tamamlayın veya kapatın." }
            val id = dao.insertNeighborhood(NeighborhoodEntity(districtId = parent, name = name.trim()))
            require(id != -1L) { "Bu mahalle seçili ilçede zaten tanımlı." }
            if (districtId.value == parent) savedState["stakeoutNeighborhood"] = id
        }
    }

    fun loadWorkbook(context: Context, uri: Uri, targetNeighborhoodId: Long) {
        val resolver = context.applicationContext.contentResolver
        perform {
            require(targetNeighborhoodId == neighborhoodId.value) {
                "Dosya seçilirken mahalle değişti. Lütfen dosyayı seçili mahalle için yeniden yükleyin."
            }
            val state = withContext(Dispatchers.IO) {
                val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                } ?: "Aplikasyon.xlsx"
                val workbook = resolver.openInputStream(uri)?.use { StakeoutWorkbook.read(it, name) }
                    ?: error("Dosya açılamadı. Lütfen yeniden seçin.")
                require(workbook.sheets.isNotEmpty()) { "Dosyada çalışma sayfası bulunamadı." }
                val sheet = workbook.sheets.first()
                val mapping = StakeoutWorkbook.defaultMapping(sheet)
                StakeoutImportState(targetNeighborhoodId, name, workbook, 0, mapping, 3, StakeoutWorkbook.preview(sheet, mapping, 3))
            }
            _importState.value = state
        }
    }

    fun selectSheet(index: Int) {
        val state = _importState.value ?: return
        perform {
            require(index in state.workbook.sheets.indices) { "Excel sayfası bulunamadı." }
            _importState.value = withContext(Dispatchers.Default) {
                val sheet = state.workbook.sheets[index]
                val mapping = StakeoutWorkbook.defaultMapping(sheet)
                state.copy(sheetIndex = index, mapping = mapping, preview = StakeoutWorkbook.preview(sheet, mapping, state.dataStartRow))
            }
        }
    }
    fun setDataStartRow(rowIndex: Int) {
        val state = _importState.value ?: return
        if (state.dataStartRow == rowIndex) return
        perform {
            require(rowIndex >= 0) { "Başlangıç satırı geçersiz." }
            _importState.value = withContext(Dispatchers.Default) {
                val sheet = state.workbook.sheets[state.sheetIndex]
                state.copy(dataStartRow = rowIndex, preview = StakeoutWorkbook.preview(sheet, state.mapping, rowIndex))
            }
        }
    }

    fun mapColumn(field: StakeoutField, column: Int?) {
        val state = _importState.value ?: return
        perform {
            _importState.value = withContext(Dispatchers.Default) {
                val mapping = state.mapping + (field to column)
                state.copy(mapping = mapping, preview = StakeoutWorkbook.preview(state.workbook.sheets[state.sheetIndex], mapping, state.dataStartRow))
            }
        }
    }
    fun editSourceRow(rowIndex: Int, cells: List<String>) {
        val state = _importState.value ?: return
        perform {
            _importState.value = withContext(Dispatchers.Default) {
                val sheets = state.workbook.sheets.toMutableList()
                val sheet = sheets[state.sheetIndex]
                require(rowIndex >= state.dataStartRow && rowIndex in sheet.rows.indices) {
                    "Yalnızca seçilen başlangıç satırından itibaren veri satırları düzenlenebilir."
                }
                val rows = sheet.rows.toMutableList().also { it[rowIndex] = cells }
                sheets[state.sheetIndex] = sheet.copy(rows = rows)
                state.copy(workbook = state.workbook.copy(sheets = sheets), preview = StakeoutWorkbook.preview(sheets[state.sheetIndex], state.mapping, state.dataStartRow))
            }
        }
    }
    fun cancelImport() { if (!_busy.value) _importState.value = null }
    fun confirmImport() {
        val state = _importState.value ?: return
        if (!state.preview.canImport) return
        perform {
            require(state.neighborhoodId == neighborhoodId.value) {
                "Önizlemenin mahallesi değişti. Lütfen dosyayı yeniden yükleyin."
            }
            val count = dao.importRecords(state.neighborhoodId, state.preview.rows.map {
                it.manhole.copy(neighborhoodId = state.neighborhoodId, sourceFileName = state.fileName)
            })
            _importState.value = null
            _message.value = "$count baca mahalleye kaydedildi."
        }
    }
    fun saveRecord(record: StakeoutManholeEntity, onSaved: () -> Unit) = perform {
        require(record.neighborhoodId == neighborhoodId.value) {
            "Düzenlenecek baca seçili mahalleye ait olmalıdır."
        }
        dao.saveRecord(record)
        onSaved()
        _message.value = "Baca bilgileri kaydedildi."
    }
    fun deleteRecord(record: StakeoutManholeEntity) = perform {
        require(record.neighborhoodId == neighborhoodId.value) {
            "Silinecek baca seçili mahalleye ait olmalıdır."
        }
        dao.deleteRecord(record.neighborhoodId, record.id)
        _message.value = "Baca kaydı silindi."
    }
    fun clearNeighborhoodRecords(id: Long) = perform {
        dao.clearRecords(id)
        _message.value = "Mahalledeki tüm bacalar silindi."
    }
    fun export(context: Context, uri: Uri, snapshot: List<StakeoutManholeEntity>, title: String) = perform {
        require(snapshot.isNotEmpty()) { "Dışa aktarılacak kayıt yok." }
        withContext(Dispatchers.IO) {
            context.applicationContext.contentResolver.openOutputStream(uri, "wt")?.use {
                StakeoutWorkbook.export(it, snapshot, title)
            } ?: error("Excel dosyası oluşturulamadı.")
        }
        _message.value = "Düzenlenmiş tutanak Excel dosyası olarak kaydedildi."
    }

    private fun perform(action: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _message.value = error.message ?: "İşlem tamamlanamadı. Lütfen tekrar deneyin." }
            finally { _busy.value = false }
        }
    }

    fun updateConnection(manholeId: Long, targetNameKey: String?, onComplete: (Boolean) -> Unit) {
        if (_busy.value) { onComplete(false); return }
        _busy.value = true
        viewModelScope.launch {
            var success = false
            try {
                dao.updateConnection(manholeId, targetNameKey, System.currentTimeMillis())
                success = true
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _message.value = error.message ?: "Bağlantı kaydedilemedi." }
            finally { _busy.value = false; onComplete(success) }
        }
    }
}
