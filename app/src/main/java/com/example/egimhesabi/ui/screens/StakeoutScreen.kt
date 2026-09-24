package com.example.egimhesabi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.egimhesabi.data.StakeoutManholeEntity
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.OutlineLight
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.ui.components.GlassCard
import com.example.egimhesabi.util.*
import com.example.egimhesabi.viewmodel.StakeoutImportState
import com.example.egimhesabi.viewmodel.StakeoutViewModel

// ── Design tokens ────────────────────────────────────────────────────────────
private val StakeoutBackground = Color(0xFFF5F5FA)
private val InsetSurface = Color(0xFFF0F4F9)
private val QuietBorder = Color(0x8FFFFFFF)
private val DeleteRed = Color(0xFFD94A4A)
private val stakeoutHeaders = StakeoutField.entries.map { it.label } + "Araziye göre akar kotu" + "Emir defteri"
private val StakeoutGradient = listOf(Color(0xFFF8F8FC), StakeoutBackground, Color(0xFFF2F4F8))

@Composable
private fun stakeoutFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentOrange,
    unfocusedBorderColor = OutlineLight,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White.copy(alpha = 0.85f),
    focusedLabelColor = AccentOrange,
    unfocusedLabelColor = TextSecondary,
    cursorColor = AccentOrange,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)

// ── Main Screen ──────────────────────────────────────────────────────────────
private enum class StakeoutFilter { ALL, EMIR_DEFTERI, MISSING_DATA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StakeoutScreen(viewModel: StakeoutViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val districts by viewModel.districts.collectAsStateWithLifecycle()
    val neighborhoods by viewModel.neighborhoods.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val districtId by viewModel.districtId.collectAsStateWithLifecycle()
    val neighborhoodId by viewModel.neighborhoodId.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val district = districts.find { it.id == districtId }
    val neighborhood = neighborhoods.find { it.id == neighborhoodId && it.districtId == districtId }
    val currentRecords = records.filter { it.neighborhoodId == neighborhood?.id }
    val location = listOfNotNull(district?.name, neighborhood?.name).joinToString(" / ")
    var addKind by rememberSaveable { mutableStateOf<String?>(null) }
    var search by rememberSaveable(neighborhoodId) { mutableStateOf("") }
    var currentFilter by rememberSaveable(neighborhoodId) { mutableStateOf(StakeoutFilter.ALL) }
    var isMapView by rememberSaveable(neighborhoodId) { mutableStateOf(false) }
    val cadState = rememberSaveableStateHolder()
    var showTable by rememberSaveable(neighborhoodId) { mutableStateOf(false) }
    var editRecord by remember { mutableStateOf<StakeoutManholeEntity?>(null) }
    var deleteRecord by remember { mutableStateOf<StakeoutManholeEntity?>(null) }
    var confirmClear by remember { mutableStateOf<Long?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var importTarget by rememberSaveable { mutableStateOf<Long?>(null) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = importTarget
        if (uri != null && target != null) viewModel.loadWorkbook(context, uri, target)
        importTarget = null
    }
    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri -> viewModel.exportPrepared(context, uri) }
    BackHandler(enabled = busy) { }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(StakeoutGradient))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Aplikasyon Tutanağı",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = (-0.2).sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, enabled = !busy) {
                            Icon(Icons.Default.ArrowBack, "Geri", tint = TextPrimary, modifier = Modifier.size(21.dp))
                        }
                    },
                    actions = {
                        if (neighborhood != null && currentRecords.isNotEmpty()) {
                            IconButton(onClick = { showTable = true }, enabled = !busy) {
                                Icon(Icons.Default.TableChart, "Tablo Görünümü", tint = TextPrimary)
                            }
                            Box {
                                IconButton(onClick = { showMenu = true }, enabled = !busy) {
                                    Icon(Icons.Default.MoreVert, "Diğer seçenekler", tint = TextPrimary)
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = Color.White) {
                                    DropdownMenuItem(
                                        text = { Text("Excel olarak dışa aktar", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.Download, null, tint = AccentOrange) },
                                        onClick = {
                                            showMenu = false
                                            viewModel.prepareExport(currentRecords, location)
                                            exportPicker.launch("${neighborhood.name.replace(Regex("[^\\p{L}\\p{N} _-]"), "_")}_aplikasyon.xlsx")
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Tüm mahalle kayıtlarını sil", color = DeleteRed) },
                                        leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = DeleteRed) },
                                        onClick = {
                                            showMenu = false
                                            confirmClear = neighborhood.id
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = StakeoutBackground.copy(alpha = 0.70f)
                    )
                )
            },
            floatingActionButton = {
                if (neighborhood != null && !busy) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            importTarget = neighborhood.id
                            filePicker.launch(arrayOf("application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel.sheet.macroEnabled.12", "application/octet-stream"))
                        },
                        containerColor = AccentOrange,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Excel Yükle", fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 80.dp), // Extra bottom padding for FAB
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── District selector ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StakeoutChoice("İlçe", district?.name ?: "İlçe seçin", districts.map { it.id to it.name }, !busy, Modifier.weight(1f), viewModel::selectDistrict)
                        IconButton(onClick = { addKind = "İlçe" }, enabled = !busy) { Icon(Icons.Default.Add, "İlçe ekle", tint = AccentOrange) }
                    }
                }
                // ── Neighborhood selector ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StakeoutChoice("Mahalle", neighborhood?.name ?: "Mahalle seçin", neighborhoods.filter { it.districtId == districtId }.map { it.id to it.name }, district != null && !busy, Modifier.weight(1f), viewModel::selectNeighborhood)
                        IconButton(onClick = { addKind = "Mahalle" }, enabled = district != null && !busy) { Icon(Icons.Default.Add, "Mahalle ekle", tint = AccentOrange) }
                    }
                }
                // ── Progress ──
                if (busy) item {
                    LinearProgressIndicator(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                        color = AccentOrange,
                        trackColor = AccentOrange.copy(alpha = 0.12f)
                    )
                }
                // ── Content ──
                if (neighborhood != null) {
                    item {
                        Column(Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("KONUM", color = AccentOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.25.sp)
                            Text(location, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("${currentRecords.size} baca • İsimler yalnızca bu mahallede eşleştirilir.", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    // ── Records ──
                    if (currentRecords.isNotEmpty()) {
                        item {
                            OutlinedTextField(
                                search, { search = it },
                                label = { Text("Baca no ile ara") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = stakeoutFieldColors(),
                                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) }
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StakeoutFilterButton("Tümü", currentFilter == StakeoutFilter.ALL) { currentFilter = StakeoutFilter.ALL }
                                StakeoutFilterButton("Kot farkı", currentFilter == StakeoutFilter.EMIR_DEFTERI) { currentFilter = StakeoutFilter.EMIR_DEFTERI }
                                StakeoutFilterButton("Eksik Veri", currentFilter == StakeoutFilter.MISSING_DATA) { currentFilter = StakeoutFilter.MISSING_DATA }
                            }
                            OutlinedButton(onClick = { isMapView = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Map, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("CAD haritayı aç")
                            }
                        }
                        
                        val matching = currentRecords.filter { record ->
                            if (!record.name.contains(search, ignoreCase = true)) return@filter false
                            when (currentFilter) {
                                StakeoutFilter.ALL -> true
                                StakeoutFilter.EMIR_DEFTERI -> {
                                    val pInvert = record.projectInvertLevel
                                    val tInvert = record.terrainInvertLevel
                                    pInvert != null && tInvert != null && pInvert > tInvert
                                }
                                StakeoutFilter.MISSING_DATA -> record.projectCoverLevel == null || record.projectInvertLevel == null
                            }
                        }
                        
                        if (!isMapView) {
                            if (matching.isEmpty()) item {
                                Text("Seçili filtrelere uygun baca bulunamadı.", color = TextSecondary, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp))
                            }
                            items(matching, key = { it.id }) { record ->
                                StakeoutRecordCard(record, busy, onEdit = { editRecord = record }, onDelete = { deleteRecord = record })
                            }
                        }
                    } else item {
                        StakeoutEmptyCard("Kayıt yok") {
                            TextButton(onClick = {
                                importTarget = neighborhood.id
                                filePicker.launch(arrayOf("application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel.sheet.macroEnabled.12", "application/octet-stream"))
                            }) {
                                Text("Excel yükle", color = AccentOrange, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else item {
                    StakeoutEmptyCard("İlçe ve mahalle seçilmedi")
                }
            }
        }
    }

    if (isMapView && neighborhood != null) {
        Dialog(onDismissRequest = { if (!busy) isMapView = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            cadState.SaveableStateProvider(neighborhood.id) {
                Column(Modifier.fillMaxSize().background(Color.White).safeDrawingPadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("CAD • ${neighborhood.name}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        TextButton(enabled = !busy, onClick = { isMapView = false }) { Text("Listeye dön") }
                    }
                    com.example.egimhesabi.ui.components.StakeoutMapView(
                        records = currentRecords, busy = busy, modifier = Modifier.weight(1f),
                        onUpdateConnection = { id, target, done -> viewModel.updateConnection(id, target, done) })
                }
            }
        }
    }

    // ── Dialogs ──
    addKind?.let { kind ->
        StakeoutNameDialog(kind, onDismiss = { addKind = null }) { name ->
            if (kind == "İlçe") viewModel.addDistrict(name) else viewModel.addNeighborhood(name)
            addKind = null
        }
    }
    if (showTable) StakeoutFullDialog("$location • ${currentRecords.size} baca", { showTable = false }) {
        StakeoutTable(stakeoutHeaders, currentRecords.mapIndexed { index, record -> index + 4 to record.cells() }, Modifier.weight(1f)) { row -> editRecord = currentRecords.getOrNull(row - 4) }
    }
    editRecord?.let { record ->
        StakeoutRecordEditor(record, busy, onDismiss = { if (!busy) editRecord = null }) {
            viewModel.saveRecord(it) { editRecord = null }
        }
    }
    deleteRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { deleteRecord = null },
            shape = RoundedCornerShape(22.dp),
            containerColor = Color.White,
            title = { Text("Baca kaydını sil", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("$location / ${record.name} silinecek. Diğer mahallelerin kayıtları etkilenmez.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = { TextButton(onClick = { viewModel.deleteRecord(record); deleteRecord = null }) { Text("Sil", color = DeleteRed, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { deleteRecord = null }) { Text("Vazgeç", color = TextSecondary) } }
        )
    }
    confirmClear?.let { nid ->
        AlertDialog(
            onDismissRequest = { confirmClear = null },
            shape = RoundedCornerShape(22.dp),
            containerColor = Color.White,
            title = { Text("Tüm mahalleyi temizle", color = DeleteRed, fontWeight = FontWeight.Bold) },
            text = { Text("$location konumundaki tüm bacalar kalıcı olarak silinecek. Bu işlem geri alınamaz. Emin misiniz?", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = { 
                Button(
                    onClick = { viewModel.clearNeighborhoodRecords(nid); confirmClear = null },
                    colors = ButtonDefaults.buttonColors(containerColor = DeleteRed),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Hepsini Sil", color = Color.White, fontWeight = FontWeight.Bold) } 
            },
            dismissButton = { TextButton(onClick = { confirmClear = null }) { Text("Vazgeç", color = TextSecondary) } }
        )
    }
    importState?.let { state ->
        StakeoutImportDialog(state, location, currentRecords, busy, viewModel)
    }
    message?.let {
        AlertDialog(
            onDismissRequest = viewModel::clearMessage,
            shape = RoundedCornerShape(22.dp),
            containerColor = Color.White,
            title = { Text("Aplikasyon tutanağı", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(it, color = TextSecondary) },
            confirmButton = {
                Button(onClick = viewModel::clearMessage, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)) {
                    Text("Tamam", color = Color.White)
                }
            }
        )
    }
}

// ── Filter Button ────────────────────────────────────────────────────────────
@Composable
private fun StakeoutFilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) AccentOrange.copy(alpha = 0.15f) else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) AccentOrange else OutlineLight),
        modifier = Modifier.height(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(label, color = if (selected) AccentOrange else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Record card ──────────────────────────────────────────────────────────────
@Composable
private fun StakeoutRecordCard(record: StakeoutManholeEntity, busy: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardShape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, cardShape, ambientColor = Color.Black.copy(alpha = 0.025f), spotColor = Color.Black.copy(alpha = 0.035f))
            .clip(cardShape)
            .background(Color.White.copy(alpha = 0.92f))
            .border(1.dp, QuietBorder, cardShape)
            .clickable(onClick = onEdit)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(InsetSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Layers, null, tint = AccentOrange, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(record.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Proje akar: ${stakeoutNumber(record.projectInvertLevel)} m", color = TextSecondary, fontSize = 11.sp)
                Text("Araziye göre akar: ${stakeoutNumber(record.terrainInvertLevel)} m", color = AccentOrange, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (record.sourceFileName.isNotBlank()) Text(record.sourceFileName, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextSecondary, fontSize = 10.sp)
            }
            IconButton(onClick = onEdit, enabled = !busy, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Edit, "${record.name} düzenle", tint = TextSecondary, modifier = Modifier.size(17.dp))
            }
            IconButton(onClick = onDelete, enabled = !busy, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.DeleteOutline, "${record.name} sil", tint = DeleteRed, modifier = Modifier.size(17.dp))
            }
        }
    }
}

// ── Empty state card ─────────────────────────────────────────────────────────
@Composable
private fun StakeoutEmptyCard(message: String, action: (@Composable () -> Unit)? = null) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        backgroundAlpha = 0.92f,
        elevation = 2.dp,
        contentPadding = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(Color(0xFFCAD5E2), CircleShape))
                Spacer(Modifier.size(7.dp))
                Text(message, color = TextSecondary, fontSize = 13.sp)
            }
            action?.invoke()
        }
    }
}

// ── Import dialog ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StakeoutImportDialog(state: StakeoutImportState, location: String, savedRecords: List<StakeoutManholeEntity>, busy: Boolean, viewModel: StakeoutViewModel) {
    val sheet = state.workbook.sheets[state.sheetIndex]
    val columnCount = sheet.rows.maxOfOrNull { it.size } ?: 0
    var tab by rememberSaveable(state.fileName, state.sheetIndex) { mutableIntStateOf(0) }
    var confirmed by remember(state) { mutableStateOf(false) }
    var editRow by remember { mutableStateOf<Int?>(null) }
    val existingKeys = savedRecords.filter { it.neighborhoodId == state.neighborhoodId }.map { it.nameKey }.toSet()
    val updates = state.preview.rows.count { it.manhole.nameKey in existingKeys }
    StakeoutFullDialog("Yükleme önizlemesi", { viewModel.cancelImport() }, canDismiss = !busy) {
        Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(location, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(state.fileName, maxLines = 2, overflow = TextOverflow.Ellipsis, color = TextSecondary, fontSize = 11.sp)
            StakeoutChoice("Excel sayfası", sheet.name, state.workbook.sheets.mapIndexed { i, s -> i.toLong() to s.name }, !busy, Modifier.fillMaxWidth()) { viewModel.selectSheet(it.toInt()) }
        }
        PrimaryTabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
            contentColor = AccentOrange
        ) {
            listOf("Sütunlar", "Kaynak Excel", "Alınacak veri").forEachIndexed { i, label ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = { Text(label, fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal, fontSize = 13.sp) },
                    selectedContentColor = AccentOrange,
                    unselectedContentColor = TextSecondary
                )
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = AccentOrange, trackColor = AccentOrange.copy(alpha = 0.12f))
        when (tab) {
            0 -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    val rowChoices = (0 until minOf(20, sheet.rows.size)).map { i -> 
                        i.toLong() to "${i + 1}. Satır: " + sheet.rows[i].take(3).joinToString(" | ") { it.take(15) }
                    }
                    StakeoutChoice(
                        label = "Veri Başlangıç Satırı",
                        value = rowChoices.find { it.first == state.dataStartRow.toLong() }?.second ?: "${state.dataStartRow + 1}. Satır",
                        options = rowChoices,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.setDataStartRow(it.toInt())
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(StakeoutField.entries) { field ->
                    val selected = state.mapping[field]
                    val columns = (0 until columnCount).map { i -> i.toLong() to "${columnLabel(i)} · ${sheet.rows.getOrNull(2)?.getOrNull(i).orEmpty().ifBlank { "Başlık boş" }}" }
                    val choices = if (field == StakeoutField.NAME) columns else listOf(-1L to "Alma") + columns
                    StakeoutChoice(field.label, choices.find { it.first == selected?.toLong() }?.second ?: "Alma", choices, !busy, Modifier.fillMaxWidth()) {
                        viewModel.mapColumn(field, if (it < 0) null else it.toInt())
                    }
                }
            }
            1 -> StakeoutTable(
                (0 until columnCount).map { columnLabel(it) },
                sheet.rows.mapIndexed { index, cells -> index + 1 to List(columnCount) { cells.getOrNull(it).orEmpty() } },
                Modifier.weight(1f),
                onEdit = { if (it - 1 >= state.dataStartRow && !busy) editRow = it - 1 }
            )
            else -> StakeoutTable(stakeoutHeaders, state.preview.rows.map { it.excelRow to it.manhole.cells() }, Modifier.weight(1f))
        }
        if (state.preview.issues.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .heightIn(max = 130.dp)
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text("${state.preview.issues.size} sorun • Kaydetmeden önce düzeltin", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                state.preview.issues.forEach { Text(it, color = TextPrimary, fontSize = 11.sp) }
            }
        }
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            cornerRadius = 16.dp,
            backgroundAlpha = 0.95f,
            elevation = 2.dp,
            contentPadding = 14.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("${state.preview.rows.size - updates} yeni kayıt • $updates baca güncellenecek • Bağlantılar korunur", fontWeight = FontWeight.Medium, color = TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(confirmed, { confirmed = it }, enabled = !busy && state.preview.canImport, colors = CheckboxDefaults.colors(checkedColor = AccentOrange))
                    Text("Önizlemeyi onaylıyorum", color = TextSecondary, fontSize = 12.sp)
                }
                Button(
                    onClick = viewModel::confirmImport,
                    enabled = confirmed && state.preview.canImport && !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) { Text("Mahalleye kaydet", color = Color.White) }
            }
        }
    }
    editRow?.let { index ->
        StakeoutSourceRowEditor(index, sheet, columnCount, busy, onDismiss = { editRow = null }) { cells ->
            viewModel.editSourceRow(index, cells)
            editRow = null
        }
    }
}

// ── Dropdown selector ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StakeoutChoice(label: String, value: String, options: List<Pair<Long, String>>, enabled: Boolean, modifier: Modifier = Modifier, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value, {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            maxLines = 2,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentOrange,
                unfocusedBorderColor = OutlineLight,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White.copy(alpha = 0.85f),
                focusedLabelColor = AccentOrange,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedTrailingIconColor = AccentOrange,
                unfocusedTrailingIconColor = TextSecondary
            )
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            if (options.isEmpty()) DropdownMenuItem(text = { Text("Henüz kayıt yok; + ile ekleyin", color = TextSecondary) }, onClick = { expanded = false }, enabled = false)
            options.forEach { (id, name) -> DropdownMenuItem(text = { Text(name, color = TextPrimary) }, onClick = { expanded = false; onSelect(id) }) }
        }
    }
}

// ── Full-screen dialog ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StakeoutFullDialog(title: String, onDismiss: () -> Unit, canDismiss: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = { if (canDismiss) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = canDismiss, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding(), color = Color.Transparent) {
            Column(Modifier.background(Brush.verticalGradient(StakeoutGradient))) {
                TopAppBar(
                    title = { Text(title, maxLines = 2, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, enabled = canDismiss) { Icon(Icons.Default.Close, "Kapat", tint = TextPrimary) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                content()
            }
        }
    }
}

// ── Table ────────────────────────────────────────────────────────────────────
@Composable
private fun StakeoutTable(headers: List<String>, rows: List<Pair<Int, List<String>>>, modifier: Modifier = Modifier, onEdit: ((Int) -> Unit)? = null) {
    if (rows.isEmpty()) {
        Box(modifier.fillMaxWidth().padding(20.dp)) { Text("Gösterilecek veri yok.", color = TextSecondary) }
        return
    }
    
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val style = MaterialTheme.typography.bodySmall
    val boldStyle = style.copy(fontWeight = FontWeight.Bold)
    
    val colWidths = remember(headers, rows, density) {
        headers.indices.map { col ->
            val headerWidth = textMeasurer.measure(headers[col], style = boldStyle).size.width
            val maxContentWidth = rows.maxOfOrNull { 
                textMeasurer.measure(it.second.getOrNull(col).orEmpty(), style = style).size.width 
            } ?: 0
            val dpWidth = with(density) { maxOf(headerWidth, maxContentWidth).toDp() }
            (dpWidth + 24.dp).coerceAtMost(250.dp).coerceAtLeast(80.dp)
        }
    }
    
    val hScroll = rememberScrollState()
    val fixedWidth = colWidths.getOrElse(0) { 150.dp }
    val totalScrollableWidth = colWidths.drop(1).fold(0.dp) { acc, w -> acc + w } + if (onEdit == null) 0.dp else 56.dp
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(hScroll)
            .width(fixedWidth + totalScrollableWidth)
    ) {
        // Header
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.background(AccentOrange.copy(alpha = 0.10f))) {
                Spacer(Modifier.width(fixedWidth))
                headers.drop(1).forEachIndexed { i, title -> StakeoutCell(title, Modifier.width(colWidths[i + 1]), true) }
                if (onEdit != null) StakeoutCell("Düzenle", Modifier.width(56.dp), true)
            }
            // Fixed Overlay
            Row(Modifier
                .offset { IntOffset(hScroll.value, 0) }
                .zIndex(1f)
                .background(Color(0xFFF7EFE8)) // Opaque approximation of 10% AccentOrange on StakeoutBackground
                .drawBehind {
                    drawLine(
                        color = OutlineLight.copy(alpha = 0.5f),
                        start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = 2f
                    )
                }
            ) {
                StakeoutCell(headers[0], Modifier.width(fixedWidth), true)
            }
        }
        
        // Body
        LazyColumn(Modifier.weight(1f)) {
            items(rows, key = { it.first }) { (row, cells) ->
                val isEven = row % 2 == 0
                Box(Modifier.fillMaxWidth().background(if (isEven) Color.Transparent else InsetSurface.copy(alpha = 0.5f))) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(fixedWidth))
                        headers.indices.drop(1).forEach { i -> StakeoutCell(cells.getOrNull(i).orEmpty(), Modifier.width(colWidths[i])) }
                        if (onEdit != null) {
                            Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                                IconButton(onClick = { onEdit(row) }) { Icon(Icons.Default.Edit, "Düzenle", tint = TextSecondary, modifier = Modifier.size(17.dp)) }
                            }
                        }
                    }
                    // Fixed Overlay
                    Row(Modifier
                        .offset { IntOffset(hScroll.value, 0) }
                        .zIndex(1f)
                        .background(if (isEven) StakeoutBackground else Color(0xFFF2F4F9)) // Opaque approximation of 50% InsetSurface on StakeoutBackground
                        .drawBehind {
                            drawLine(
                                color = OutlineLight.copy(alpha = 0.5f),
                                start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                                strokeWidth = 2f
                            )
                        }
                    ) {
                        StakeoutCell(cells.getOrNull(0).orEmpty(), Modifier.width(fixedWidth))
                    }
                }
                HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun StakeoutCell(text: String, modifier: Modifier, header: Boolean = false) {
    Text(
        text.ifBlank { "—" },
        modifier.padding(10.dp).heightIn(min = if (header) 44.dp else 28.dp),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
        color = if (header) TextPrimary else TextSecondary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

// ── Name dialog ──────────────────────────────────────────────────────────────
@Composable
private fun StakeoutNameDialog(kind: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = Color.White,
        title = { Text("$kind ekle", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            OutlinedTextField(
                name, { name = it },
                label = { Text("$kind adı") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentOrange,
                    focusedLabelColor = AccentOrange,
                    cursorColor = AccentOrange,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = InsetSurface,
                    unfocusedContainerColor = InsetSurface
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim()) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) { Text("Ekle", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç", color = TextSecondary) } }
    )
}

// ── Record editor ────────────────────────────────────────────────────────────
@Composable
private fun StakeoutRecordEditor(record: StakeoutManholeEntity, busy: Boolean, onDismiss: () -> Unit, onSave: (StakeoutManholeEntity) -> Unit) {
    var cells by remember(record.id) { mutableStateOf(record.cells().take(9).map { if (it == "—") "" else it }) }
    val fakeSheet = remember(cells) { StakeoutSheet("Düzenleme", listOf(emptyList(), emptyList(), stakeoutHeaders.take(9), cells)) }
    val preview = remember(fakeSheet) { StakeoutWorkbook.preview(fakeSheet, StakeoutWorkbook.defaultMapping(fakeSheet)) }
    StakeoutFullDialog("${record.name} • Baca bilgileri", onDismiss, !busy) {
        LazyColumn(Modifier.weight(1f).imePadding(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(StakeoutField.entries) { field ->
                OutlinedTextField(
                    cells[field.ordinal],
                    { value -> cells = cells.toMutableList().also { it[field.ordinal] = value } },
                    label = { Text(field.label) },
                    enabled = !busy,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = if (field == StakeoutField.NAME) KeyboardType.Text else KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = stakeoutFieldColors()
                )
            }
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    backgroundAlpha = 0.95f,
                    elevation = 2.dp,
                    contentPadding = 12.dp
                ) {
                    Text(
                        "Araziye göre akar kotu: ${stakeoutNumber(preview.rows.firstOrNull()?.manhole?.terrainInvertLevel)} m",
                        color = AccentOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
            items(preview.issues) { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        }
        Button(
            onClick = {
                val edited = preview.rows.single().manhole
                onSave(edited.copy(id = record.id, neighborhoodId = record.neighborhoodId, sourceFileName = record.sourceFileName))
            },
            enabled = !busy && preview.canImport,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
        ) { Text("Değişiklikleri kaydet", color = Color.White) }
    }
}

// ── Source row editor ────────────────────────────────────────────────────────
@Composable
private fun StakeoutSourceRowEditor(rowIndex: Int, sheet: StakeoutSheet, columnCount: Int, busy: Boolean, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    var cells by remember(rowIndex) { mutableStateOf(List(columnCount) { sheet.rows[rowIndex].getOrNull(it).orEmpty() }) }
    StakeoutFullDialog("Excel • ${rowIndex + 1}. satır", onDismiss, !busy) {
        LazyColumn(Modifier.weight(1f).imePadding(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(columnCount) { i ->
                OutlinedTextField(
                    cells[i],
                    { value -> cells = cells.toMutableList().also { it[i] = value } },
                    label = { Text("${columnLabel(i)} · ${sheet.rows.getOrNull(2)?.getOrNull(i).orEmpty()}") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = stakeoutFieldColors()
                )
            }
        }
        Button(
            onClick = { onSave(cells) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
        ) { Text("Önizlemeye uygula", color = Color.White) }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun stakeoutNumber(value: Double?): String = value?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "—"
private fun StakeoutManholeEntity.cells(): List<String> {
    val pInvert = projectInvertLevel
    val tInvert = terrainInvertLevel
    val emirDefteri = if (pInvert != null && tInvert != null && pInvert > tInvert) {
        val diff = stakeoutNumber(pInvert - tInvert)
        "Var (Proje akar, arazi akardan $diff m yüksek)"
    } else {
        "Yok"
    }
    return listOf(name) + listOf(projectY, projectX, projectCoverLevel, projectGroundLevel, projectInvertLevel, terrainGroundLevel, dischargeCoverLevel, dischargeDepth, terrainInvertLevel).map(::stakeoutNumber) + emirDefteri
}
private fun columnLabel(index: Int): String {
    var number = index + 1
    var label = ""
    while (number > 0) { number--; label = ('A' + number % 26) + label; number /= 26 }
    return label
}
