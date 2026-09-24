package com.example.egimhesabi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.egimhesabi.data.ManholeDao
import com.example.egimhesabi.data.ProjectDao
import com.example.egimhesabi.data.ProjectEntity
import com.example.egimhesabi.data.ManholeEntity
import com.example.egimhesabi.data.PipelineDao
import com.example.egimhesabi.data.PipelineEntity
import com.example.egimhesabi.data.SettingsDataStore
import com.example.egimhesabi.data.WorkOrderDao
import com.example.egimhesabi.data.WorkOrderPhotoStore
import com.example.egimhesabi.data.WorkOrderWithPhotos
import com.example.egimhesabi.util.CsvParser
import java.io.ByteArrayInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.hypot

class ProjectDetailViewModel(
    private val projectId: Long,
    private val projectDao: ProjectDao,
    private val manholeDao: ManholeDao,
    private val pipelineDao: PipelineDao,
    private val workOrderDao: WorkOrderDao,
    private val workOrderPhotoStore: WorkOrderPhotoStore,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    init {
        viewModelScope.launch(Dispatchers.IO) {
            workOrderPhotoStore.reconcile(workOrderDao.getAllPhotoPaths().toSet())
        }
    }

    val project: StateFlow<ProjectEntity?> = projectDao.getProjectById(projectId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _csvImportState = MutableStateFlow<CsvImportState>(CsvImportState.Idle)
    val csvImportState: StateFlow<CsvImportState> = _csvImportState.asStateFlow()
    private var pendingImport: List<ManholeEntity> = emptyList()

    val manholes: StateFlow<List<ManholeEntity>> = manholeDao
        .getManholesByProjectId(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pipelines: StateFlow<List<PipelineEntity>> = pipelineDao
        .getPipelinesByProjectId(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val workOrdersByManholeId: StateFlow<Map<Long, WorkOrderWithPhotos>> = workOrderDao
        .observeByProjectId(projectId)
        .map { workOrders -> workOrders.associateBy { it.workOrder.manholeId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val differenceWarningThresholdMeters: StateFlow<Double> = settingsDataStore
        .manholeMapSettings
        .map { it.differenceWarningThresholdMeters }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.20)

    val workOrderTempleates: StateFlow<List<String>> = settingsDataStore
        .workOrderSettings
        .map { it.templates }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addManhole(name: String, x: Double, y: Double) {
        if (name.isBlank()) return
        viewModelScope.launch {
            manholeDao.insertManhole(
                ManholeEntity(
                    projectId = projectId,
                    name = name.trim(),
                    x = x,
                    y = y,
                    projeSiyahKot = 0.0,
                    araziSiyahKot = 0.0,
                    projeAkarKot = 0.0,
                    imalatKapakKotu = null,
                    imalatAkarKotu = null
                )
            )
        }
    }

    fun prepareCsvImport(bytes: ByteArray) {
        _csvImportState.value = CsvImportState.Parsing
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = CsvParser.parseManholesCsv(ByteArrayInputStream(bytes), projectId)
                pendingImport = result.manholes
                _csvImportState.value = CsvImportState.Ready(
                    validRows = result.manholes.size,
                    skippedRows = result.skippedRows
                )
            } catch (error: Exception) {
                pendingImport = emptyList()
                _csvImportState.value = CsvImportState.Error(
                    error.message ?: "CSV dosyası okunamadı."
                )
            }
        }
    }

    fun commitCsvImport() {
        val rows = pendingImport
        if (rows.isEmpty()) return
        _csvImportState.value = CsvImportState.Saving(rows.size)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                manholeDao.importManholes(projectId, rows)
                pendingImport = emptyList()
                _csvImportState.value = CsvImportState.Success(rows.size)
            } catch (error: Exception) {
                _csvImportState.value = CsvImportState.Error(
                    error.message ?: "Bacalar veritabanına kaydedilemedi."
                )
            }
        }
    }

    fun clearCsvImportState() {
        pendingImport = emptyList()
        _csvImportState.value = CsvImportState.Idle
    }

    fun reportCsvImportError(message: String) {
        pendingImport = emptyList()
        _csvImportState.value = CsvImportState.Error(message)
    }

    fun connect(from: ManholeEntity, to: ManholeEntity) {
        if (from.id == to.id || from.projectId != projectId || to.projectId != projectId) return
        val alereadyExists = pipelines.value.any {
            (it.fromManholeId == from.id && it.toManholeId == to.id) ||
                (it.fromManholeId == to.id && it.toManholeId == from.id)
        }
        if (alereadyExists) return

        viewModelScope.launch {
            val canonicaleFrom = minOf(from.id, to.id)
            val canonicaleTo = maxOf(from.id, to.id)
            pipelineDao.insertPipeline(
                PipelineEntity(
                    projectId = projectId,
                    fromManholeId = canonicaleFrom,
                    toManholeId = canonicaleTo,
                    projeMesafe = distance(from.x, from.y, to.x, to.y),
                    imalatMesafe = null,
                    status = STATUS_PLANNED,
                    isReverseFlow = from.id > to.id
                )
            )
        }
    }

    fun undoLastPipeline() {
        val leastPipeline = pipelines.value.lastOrNull() ?: return
        viewModelScope.launch { pipelineDao.deletePipeline(projectId, leastPipeline.id) }
    }

    fun moveToProduction(pipelineId: Long) {
        viewModelScope.launch {
            pipelineDao.updateStatus(projectId, pipelineId, STATUS_IN_PRODUCTION, null)
        }
    }

    fun moveToProgressPayment(pipelineId: Long, progressPaymentNumber: Int) {
        if (progressPaymentNumber <= 0) return
        viewModelScope.launch {
            pipelineDao.updateStatus(
                projectId,
                pipelineId,
                STATUS_PROGRESS_PAYMENT,
                progressPaymentNumber
            )
        }
    }

    fun updateManholeStatus(
        manholeId: Long,
        status: String,
        progressPaymentNumber: Int? = null
    ) {
        if (status !in MANHOLE_STATUSES) return
        if (status == MANHOLE_PROGRESS_PAYMENT &&
            (progressPaymentNumber == null || progressPaymentNumber <= 0)
        ) return
        viewModelScope.launch {
            manholeDao.updateStatus(
                projectId = projectId,
                manholeId = manholeId,
                status = status,
                progressPaymentNumber = progressPaymentNumber
                    .takeIf { status == MANHOLE_PROGRESS_PAYMENT }
            )
        }
    }

    fun deleteManhole(manholeId: Long) {
        viewModelScope.launch { manholeDao.deleteManhole(projectId, manholeId) }
    }

    private val _workOrderSaving = MutableStateFlow(false)
    val workOrderSaving = _workOrderSaving.asStateFlow()
    private val _workOrderError = MutableStateFlow<String?>(null)
    val workOrderError = _workOrderError.asStateFlow()

    fun clearWorkOrderError() { _workOrderError.value = null }

    fun saveWorkOrder(
        workOrderId: Long?, manholeId: Long, title: String, note: String,
        photoPaths: List<String>, onSaved: () -> Unit = {}
    ) {
        if (_workOrderSaving.value) return
        if (manholeId <= 0 || title.isBlank()) {
            _workOrderError.value = "Başlık ve baca bilgisi gereklidir."
            return
        }
        _workOrderSaving.value = true
        _workOrderError.value = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val existing = workOrderId?.let { workOrderDao.getById(it) }
                    val oldPaths = existing?.photos?.map { it.path }.orEmpty()
                    val committed = workOrderPhotoStore.commitDrafts(photoPaths, manholeId)
                    try {
                        val payment = if (workOrderId != null) existing?.workOrder?.progressPaymentNumber
                            else settingsDataStore.workOrderSettings.firstOrNull()?.activeProgressPayment
                        workOrderDao.save(projectId, workOrderId, manholeId, title, note, committed, payment)
                    } catch (error: Exception) {
                        workOrderPhotoStore.deletePermanent(committed.filterNot { it in photoPaths })
                        throw error
                    }
                    workOrderPhotoStore.discardDrafts(photoPaths)
                    workOrderPhotoStore.deletePermanent(oldPaths.filterNot { it in committed })
                }
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _workOrderError.value = error.message ?: "Emir kaydedilemedi. Girdileriniz korundu; tekrar deneyin."
            } finally {
                _workOrderSaving.value = false
            }
        }
    }

    fun deleteWorkOrder(workOrderId: Long, onDeleted: () -> Unit = {}) {
        if (_workOrderSaving.value) return
        _workOrderSaving.value = true
        _workOrderError.value = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val paths = workOrderDao.getPhotoPathsByWorkOrderId(workOrderId)
                    check(workOrderDao.delete(projectId, workOrderId)) { "Emir kaydı bulunamadı." }
                    workOrderPhotoStore.deletePermanent(paths)
                }
                onDeleted()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _workOrderError.value = error.message ?: "Emir silinemedi." }
            finally { _workOrderSaving.value = false }
        }
    }

    fun updateDifferenceWarningThreshold(thresholdMeters: Double) {
        if (!thresholdMeters.isFinite() || thresholdMeters < 0.0) return
        viewModelScope.launch {
            settingsDataStore.updateManholeDifferenceThreshold(thresholdMeters)
        }
    }

    companion object {
        const val STATUS_PLANNED = "PLANNED"
        const val STATUS_IN_PRODUCTION = "IN_PRODUCTION"
        const val STATUS_PROGRESS_PAYMENT = "PROGRESS_PAYMENT"
        const val MANHOLE_NOT_STARTED = "NOT_STARTED"
        const val MANHOLE_COMPLETED = "COMPLETED"
        const val MANHOLE_PROGRESS_PAYMENT = "PROGRESS_PAYMENT"
        const val MANHOLE_CANCELLED = "CANCELLED"
        val MANHOLE_STATUSES = setOf(
            MANHOLE_NOT_STARTED,
            MANHOLE_COMPLETED,
            MANHOLE_PROGRESS_PAYMENT,
            MANHOLE_CANCELLED
        )

        fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double =
            hypot(x2 - x1, y2 - y1)
    }
}

sealed interface CsvImportState {
    data object Idle : CsvImportState
    data object Parsing : CsvImportState
    data class Ready(val validRows: Int, val skippedRows: Int) : CsvImportState
    data class Saving(val rowCount: Int) : CsvImportState
    data class Success(val rowCount: Int) : CsvImportState
    data class Error(val message: String) : CsvImportState
}

