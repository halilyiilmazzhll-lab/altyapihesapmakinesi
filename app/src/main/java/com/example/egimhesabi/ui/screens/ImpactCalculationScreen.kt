package com.example.egimhesabi.ui.screens

import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.drawText
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.egimhesabi.data.ImpactHistoryEntry
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.GradientStart
import com.example.egimhesabi.theme.OutlineLight
import com.example.egimhesabi.theme.SlopeDanger
import com.example.egimhesabi.theme.SlopeOk
import com.example.egimhesabi.theme.SlopeWarning
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.theme.TextTertiary
import com.example.egimhesabi.util.NumberParser
import com.example.egimhesabi.ui.components.StakeoutPicker
import com.example.egimhesabi.viewmodel.ImpactCalculationState
import com.example.egimhesabi.viewmodel.ImpactCalculationViewModel
import com.example.egimhesabi.viewmodel.ImpactSegmentAnalysis
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.egimhesabi.viewmodel.ImpactSlopeStatus
import com.example.egimhesabi.viewmodel.ManholeNode
import com.example.egimhesabi.viewmodel.ManholeNodeData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val FieldBackground = Color(0xFFF3F5F9)
private val MiddleAccent = Color(0xFFFF6038)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpactCalculationScreen(
    viewModel: ImpactCalculationViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAsRatio by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }

    val feedback by viewModel.message.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(feedback) {
        feedback?.let { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show(); viewModel.clearMessage() }
    }
    if (showHistory) {
        ImpactHistoryPanel(
            viewModel = viewModel,
            onDismiss = { showHistory = false },
            onOpenDrawer = onOpenDrawer,
            modifier = modifier
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Etki Hesabı",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showMapPicker = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Haritadan Hat Seç",
                            tint = AccentOrange
                        )
                    }
                    IconButton(
                        onClick = { showHistory = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Geçmiş",
                            tint = AccentOrange
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.saveCurrentCalculations()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Kaydet",
                            tint = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GradientStart)
            )
        },
        containerColor = GradientStart,
        modifier = modifier
    ) { paddingValues ->
        if (showMapPicker) {
            ImpactMapPicker(
                onDismiss = { showMapPicker = false },
                onImportChain = { chain ->
                    viewModel.importChain(chain)
                    showMapPicker = false
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            if (state.usingHistoryLimits) {
                Text("Bu kayıt, geçmişte kullanılan eğim ve derinlik sınırlarıyla açıldı.", modifier = Modifier.padding(horizontal = 16.dp), color = TextSecondary)
                TextButton(onClick = viewModel::useCurrentLimits) { Text("Güncel ayarlarla yeniden hesapla") }
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(state.nodes, key = { _, node -> node.id }) { index, node ->
                    ManholeCard(index, node, state, viewModel)
                }
                item {
                    AddManholeButton(onClick = viewModel::addManhole)
                }
                item {
                    Surface(
                        onClick = { showMapPicker = true },
                        color = Color.White.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight),
                        modifier = Modifier.height(180.dp).width(120.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).background(Color(0xFFFFF2ED), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Map, null, tint = AccentOrange, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Haritadan\nHat Seç",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ImpactSummaryCard(state, showAsRatio) { showAsRatio = !showAsRatio }
                ProfileCard(state, showAsRatio)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── İşlem Geçmişi Paneli ──

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ImpactHistoryPanel(
    viewModel: ImpactCalculationViewModel,
    onDismiss: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val history by viewModel.impactHistory.collectAsStateWithLifecycle()
    var entryToRestore by remember { mutableStateOf<ImpactHistoryEntry?>(null) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    if (entryToRestore != null) {
        AlertDialog(
            onDismissRequest = { entryToRestore = null },
            title = { Text("Geri Yükle", fontWeight = FontWeight.Bold) },
            text = { Text("Bu hesaplama geri yüklensin mi? Mevcut verilerinizin Üzerine yazılacak.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.loadFromHistory(entryToRestore!!)
                    entryToRestore = null
                    onDismiss()
                }) {
                    Text("Geri Yükle", color = AccentOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToRestore = null }) {
                    Text("İptal", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text("${selectedIds.size} Seçildi", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("Etki Hesabı Geçmişi", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { isSelectionMode = false; selectedIds.clear() }) {
                            Icon(Icons.Default.Close, contentDescription = "İptal", tint = TextPrimary)
                        }
                    } else {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = TextPrimary)
                        }
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        if (!isSelectionMode) {
                            IconButton(onClick = { isSelectionMode = true }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Seç", tint = TextPrimary)
                            }
                            IconButton(onClick = { viewModel.clearHistory() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Tümünü sil", tint = TextSecondary)
                            }
                        } else {
                            IconButton(
                                enabled = !isExporting && selectedIds.isNotEmpty(),
                                onClick = {
                                    val entriesToExport = history.filter { it.id in selectedIds }
                                    isExporting = true
                                    scope.launch {
                                        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            runCatching {
                                                com.example.egimhesabi.util.ImpactHistoryPdfExporter.exportToCache(context, entriesToExport)
                                            }
                                        }
                                        isExporting = false
                                        if (result.isSuccess) {
                                            val file = result.getOrThrow()
                                            val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".provider", file)
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(intent, "PDF Paylaş"))
                                            isSelectionMode = false
                                            selectedIds.clear()
                                        } else {
                                            val msg = result.exceptionOrNull()?.message ?: "Bilinmeyen hata"
                                            android.widget.Toast.makeText(context, "PDF Hatası: $msg", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            ) {
                                if (isExporting) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AccentOrange)
                                } else {
                                    Icon(Icons.Default.Share, contentDescription = "Paylaş", tint = if (selectedIds.isNotEmpty()) TextPrimary else TextSecondary)
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GradientStart)
            )
        },
        containerColor = GradientStart,
        modifier = modifier
    ) { paddingValues ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                AppleCard(modifier = Modifier.padding(32.dp)) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = TextSecondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Henüz kayıt yok",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Etki hesabı sonuçlarınız burada görünecek",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Geri yüklemek için kayda uzun basın",
                        color = TextTertiary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(history, key = { it.id }) { entry ->
                    val isSelected = selectedIds.contains(entry.id)
                    ImpactHistoryCard(
                        entry = entry,
                        onDelete = { viewModel.deleteHistoryEntry(entry) },
                        onLongPress = { if (!isSelectionMode) entryToRestore = entry },
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onToggleSelect = {
                            if (isSelected) selectedIds.remove(entry.id)
                            else selectedIds.add(entry.id)
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ImpactHistoryCard(
    entry: ImpactHistoryEntry,
    onDelete: () -> Unit,
    onLongPress: () -> Unit,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(entry.timestamp))

    // Parse nodes from JSON to reconstruct full state
    val json = remember { kotlinx.serialization.json.Json { ignoreUnknownKeys = true } }
    val nodes = remember(entry.nodesJson) {
        try {
            val dataList: List<ManholeNodeData> = json.decodeFromString(entry.nodesJson)
            dataList.map { ManholeNode.fromData(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }
    val reconstructedState = remember(nodes, entry) {
        ImpactCalculationState(
            nodes = nodes,
            minSlopePercent = entry.minSlopePercent,
            maxSlopePercent = entry.maxSlopePercent,
            minManholeDepthMeters = entry.minManholeDepthMeters
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SlopeDanger, RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.White)
            }
        },
        modifier = modifier
    ) {
        val overallColor = if (entry.allValid) SlopeOk else SlopeWarning

        AppleCard(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) onToggleSelect()
                    },
                    onLongClick = {
                        if (isSelectionMode) onToggleSelect() else onLongPress()
                    }
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Üst Satır: Başlık + Tarih ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(overallColor, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${nodes.size} Bacalı Profil",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (isSelectionMode) {
                        androidx.compose.material3.Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = dateStr,
                            color = TextSecondary.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }
                }

                Text(
                    text = if (entry.allValid) "Tüm hatlar dengede" else "Hatalı hatlar var",
                    color = overallColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // ── Eğim Aralığı ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FieldBackground, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Min eğim: %${NumberParser.formatDecimal(entry.minSlopePercent)}",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Max eğim: %${NumberParser.formatDecimal(entry.maxSlopePercent)}",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // ── Baca Detayları ──
                if (nodes.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FieldBackground, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "BACA BİLGİLERİ",
                            color = AccentOrange,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        nodes.forEachIndexed { index, node ->
                            val isFirst = index == 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                    Column {
                                        val defaultName = if (isFirst) "${index + 1}. Baca (Sabit)" else "${index + 1}. Baca"
                                        val displayName = if (node.name.isNotBlank()) {
                                            "${index + 1}. Baca (${node.name})" + if (isFirst) " (Sabit)" else ""
                                        } else {
                                            defaultName
                                        }
                                        Text(
                                            text = displayName,
                                        color = if (isFirst) TextPrimary else MiddleAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        node.cover?.let {
                                            Text(
                                                text = "Kapak: ${format(it)}",
                                                color = TextSecondary,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        node.invert?.let {
                                            Text(
                                                text = "Akar: ${format(it)}",
                                                color = TextSecondary,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    node.depth?.let {
                                        Text(
                                            text = "H: ${format(it)} m",
                                            color = TextTertiary,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (node.deltaCm != 0.0) {
                                        val sign = if (node.deltaCm > 0) "+" else ""
                                        Text(
                                            text = "Δ: $sign${format(node.deltaCm, 0)} cm",
                                            color = AccentOrange,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Hat Detayları ──
                if (nodes.size >= 2) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FieldBackground, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "HAT ANALİZLERİ",
                            color = AccentOrange,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        for (i in 0 until nodes.size - 1) {
                            val segment = reconstructedState.getSegmentAnalysis(i)
                            val segColor = statusColor(segment.status)
                            val distance = NumberParser.parseDecimal(nodes[i].distanceToNextText)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Durum noktası
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .background(segColor, CircleShape)
                                )
                                Spacer(Modifier.width(6.dp))

                                // Hat adı
                                val node1Name = nodes[i].name.ifBlank { "${i + 1}" }
                                val node2Name = nodes[i + 1].name.ifBlank { "${i + 2}" }
                                val segmentLabel = when (reconstructedState.flowLeftToRight) {
                                    true -> "Hat $node1Name \u2192 $node2Name"
                                    false -> "Hat $node2Name \u2192 $node1Name"
                                    null -> "Hat $node1Name \u2014 $node2Name"
                                }
                                Text(
                                    text = segmentLabel,
                                    color = TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )

                                // Eğim
                                segment.slopePercent?.let { pct ->
                                    Text(
                                        text = "%${format(pct, 2)}",
                                        color = segColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(52.dp)
                                    )
                                } ?: Text(
                                    text = "—",
                                    color = TextTertiary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(52.dp)
                                )

                                // Oran
                                segment.slopeRatio?.let { ratio ->
                                    Text(
                                        text = "1/${format(ratio, 0)}",
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(40.dp)
                                    )
                                } ?: Spacer(Modifier.width(40.dp))

                                // Mesafe & kot farkı
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    distance?.let {
                                        Text(
                                            text = "L=${format(it)} m",
                                            color = TextTertiary,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    segment.dropMeters?.let {
                                        Text(
                                            text = "Δh=${format(it)} m",
                                            color = TextTertiary,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Spacer(Modifier.width(6.dp))

                                // Durum
                                Text(
                                    text = statusText(segment.status),
                                    color = segColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // ── Alt Satır: Geri yükle ipucu ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Geri yüklemek için uzun basın",
                        color = TextTertiary,
                        fontSize = 8.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Geri yükle",
                        tint = AccentOrange.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// ── Mevcut Bileşenler (değişmedi) ──

@Composable
private fun ManholeCard(
    index: Int,
    node: ManholeNode,
    state: ImpactCalculationState,
    viewModel: ImpactCalculationViewModel
) {
    val isFixed = index == 0
    val title = if (isFixed) "${index + 1}. Baca (Sabit)" else "${index + 1}. Baca"
    val accent = if (isFixed) TextPrimary else MiddleAccent
    var showCover by remember(node.id) { mutableStateOf(node.coverText.isNotBlank()) }

    AppleCard(modifier = Modifier.width(280.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("0${index + 1}", color = AccentOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("  $title", color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (node.isDepthMode) {
                        node.invert?.let {
                            Text("Akar ${format(it, 2)}m", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        node.depth?.let {
                            Text("Derinlik ${format(it, 2)}m", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    com.example.egimhesabi.ui.components.StakeoutPicker(
                        targetLabel = "Baca ${index + 1}",
                        source = node.stakeoutSource,
                        onSelect = { viewModel.importManhole(node.id, it) }
                    )
                    if (state.nodes.size > 2) {
                        IconButton(onClick = { viewModel.removeManhole(node.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = SlopeDanger, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            
            CompactField("Baca No / Adı", node.name, { viewModel.updateName(node.id, it) }, Modifier.fillMaxWidth(), keyboardType = androidx.compose.ui.text.input.KeyboardType.Text)
if (showCover || node.coverText.isNotBlank()) {
                CompactField("Kapak kotu", node.coverText, { viewModel.updateCover(node.id, it) }, Modifier.fillMaxWidth(), suffix = "m")
            } else {
                Surface(
                    onClick = { showCover = true },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text("+ Kapak kotu ekle", modifier = Modifier.padding(4.dp), color = TextTertiary, fontSize = 9.sp)
                }
            }
            val invertLabel = if (node.deltaCm != 0.0) "Sabit Akar (Aktif: ${node.invert?.let { format(it, 2) }})" else if (node.isDepthMode) "Derinlik" else "Akar kotu"
            val invertValue = if (node.isDepthMode) node.depthText else node.invertText
            val onInvertChange = { it: String -> if (node.isDepthMode) viewModel.updateDepth(node.id, it) else viewModel.updateInvert(node.id, it) }
            
            CompactField(
                label = invertLabel, 
                value = invertValue, 
                onValueChange = onInvertChange, 
                modifier = Modifier.fillMaxWidth(), 
                suffix = "m",
                trailingIcon = {
                    IconButton(onClick = { viewModel.toggleDepthMode(node.id) }, modifier = Modifier.size(24.dp)) {
                        Icon(androidx.compose.material.icons.Icons.Default.SwapVert, "Mod", tint = AccentOrange)
                    }
                }
            )
            if (index < state.nodes.size - 1) {
                DistanceField("Sonraki bacaya mesafe", node.distanceToNextText) { viewModel.updateDistance(node.id, it) }
            }

            if (!isFixed) {
                Spacer(modifier = Modifier.height(4.dp))
                ManholeAdjustmentControls(index, node, state, viewModel)
            }
        }
    }
}

@Composable
private fun ManholeAdjustmentControls(
    index: Int,
    node: ManholeNode,
    state: ImpactCalculationState,
    viewModel: ImpactCalculationViewModel
) {
    val deltaText = when {
        node.deltaCm > 0 -> "+${format(node.deltaCm, 0)} cm"
        node.deltaCm < 0 -> "${format(node.deltaCm, 0)} cm"
        else -> "0 cm"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(FieldBackground, RoundedCornerShape(12.dp)).padding(5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepButton("−5") { viewModel.adjustDelta(node.id, -5.0) }
            StepButton("−1") { viewModel.adjustDelta(node.id, -1.0) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(deltaText, color = if (node.deltaCm == 0.0) TextPrimary else AccentOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                val activeInvertText = node.invert?.let { "${format(it, 2)} m" } ?: "ayar"
                Text(activeInvertText, color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            }
            StepButton("+1") { viewModel.adjustDelta(node.id, 1.0) }
            StepButton("+5") { viewModel.adjustDelta(node.id, 5.0) }
            IconButton(onClick = { viewModel.resetDelta(node.id) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Refresh, "Sıfırla", tint = AccentOrange, modifier = Modifier.size(16.dp))
            }
        }

        val rec = state.getRecommendationFor(index)
        if (rec != null) {
            Column(
                modifier = Modifier.fillMaxWidth().border(1.dp, OutlineLight, RoundedCornerShape(10.dp)).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("ÖNERİLEN KOT ARALIĞI", color = TextTertiary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text("${format(rec.minimumInvert, 2)} – ${format(rec.maximumInvert, 2)} m", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                if (abs(rec.recommendedDeltaCm - node.deltaCm) > 0.01) {
                    Button(
                        onClick = { viewModel.applyRecommendation(node.id) },
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        val sign = if (rec.recommendedDeltaCm > 0) "+" else ""
                        Text("Uygula $sign${format(rec.recommendedDeltaCm, 0)} cm", fontSize = 9.sp)
                    }
                }
            }
        } else {
            Text("Uygun ortak aralık yok.", color = SlopeDanger, fontSize = 9.sp)
        }
    }
}

@Composable
private fun AddManholeButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(60.dp).height(200.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, "Baca Ekle", tint = AccentOrange, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun ImpactSummaryCard(state: ImpactCalculationState, showAsRatio: Boolean, onToggleRatio: () -> Unit) {
    val allValid = state.allSegmentsValid
    val (title, description, color) = when {
        !state.allInputsValid -> Triple(
            "Verileri tamamlayın",
            "Tüm bacaların akar kotu ve hat mesafeleri gerekli.",
            TextTertiary
        )
        state.flowLeftToRight == null -> Triple(
            "Akış yönü belirsiz",
            "İlk ve son akar kotu eşit; tek yönlü doğal akış oluşmuyor.",
            SlopeWarning
        )
        allValid -> Triple(
            "Tüm hatlar dengede",
            "Profildeki tüm hatlar eğim sınırlarını sağlıyor.",
            SlopeOk
        )
        else -> Triple(
            "Hatalı hatlar var",
            "Bazı hatların eğimi sınırların dışında.",
            SlopeWarning
        )
    }

    AppleCard {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(color, CircleShape))
                Text("  N-BACALI PROFİL DURUMU", color = AccentOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
            }
            Text(title, color = TextPrimary, fontSize = 21.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
            Text(description, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldBackground, RoundedCornerShape(13.dp))
                    .clickable { onToggleRatio() }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val minText = if (showAsRatio) {
                    state.minSlopePercent.takeIf { it > 0.0 }
                        ?.let { "1/${format(100.0 / it, 0)}" } ?: "—"
                } else "%${format(state.minSlopePercent)}"
                val maxText = if (showAsRatio) {
                    state.maxSlopePercent.takeIf { it > 0.0 }
                        ?.let { "1/${format(100.0 / it, 0)}" } ?: "—"
                } else "%${format(state.maxSlopePercent)}"
                LimitValue("Minimum eğim", minText)
                LimitValue("Maksimum eğim", maxText, Alignment.End)
            }
        }
    }
}

@Composable
private fun LimitValue(label: String, value: String, alignment: Alignment.Horizontal = Alignment.Start) {
    Column(horizontalAlignment = alignment) {
        Text(label, color = TextTertiary, fontSize = 9.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProfileCard(state: ImpactCalculationState, showAsRatio: Boolean) {
    AppleCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Kot ili\u015Fkisi", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                
                val flowText = when (state.flowLeftToRight) {
                    true -> "Ak\u0131\u015F (otomatik) \u2192"
                    false -> "\u2190 Ak\u0131\u015F (otomatik)"
                    null -> "Ak\u0131\u015F y\u00F6n\u00FC belirsiz"
                }
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(flowText, color = TextSecondary, fontSize = 10.sp)
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().height(180.dp).background(FieldBackground, RoundedCornerShape(16.dp)).padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                ProfileCanvas(state)
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (0 until state.nodes.size - 1).forEach { index ->
                    val segment = state.getSegmentAnalysis(index)
                    val node1 = state.nodes[index]
                    val node2 = state.nodes[index + 1]
                    val name1 = node1.name.ifBlank { "${index + 1}" }
                    val name2 = node2.name.ifBlank { "${index + 2}" }
                    val label = when (state.flowLeftToRight) {
                        true -> "Hat $name1 \u2192 $name2"
                        false -> "Hat $name2 \u2192 $name1"
                        null -> "Hat $name1 \u2014 $name2"
                    }
                    SegmentRow(label, segment, showAsRatio)
                }
            }
        }
    }
}

@Composable
private fun SegmentRow(title: String, segment: ImpactSegmentAnalysis, showAsRatio: Boolean) {
    val color = statusColor(segment.status)
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(8.dp)).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(color, CircleShape))
            Text("  $title", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
        val text = if (showAsRatio) {
            segment.slopeRatio?.let { "1/${format(it, 0)}" } ?: "—"
        } else {
            segment.slopePercent?.let { "%${format(it, 2)}" } ?: "—"
        }
        Text(
            text = text,
            color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
        )
        Text(statusText(segment.status), color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProfileCanvas(state: ImpactCalculationState) {
    val nodes = state.nodes
    val inverts = nodes.mapNotNull { it.invert }
    val covers = nodes.map { it.cover }
    
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "flow")
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ), label = "phase"
    )

    val requiredWidthDp = maxOf(300, nodes.size * 120).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
    ) {
        if (inverts.size != nodes.size) {
            Canvas(Modifier.width(requiredWidthDp).fillMaxHeight()) {
                drawLine(
                    color = OutlineLight, start = Offset(12f, size.height / 2), end = Offset(size.width - 12f, size.height / 2),
                    strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
            }
            return@Box
        }

        Canvas(Modifier.width(requiredWidthDp).fillMaxHeight()) {
            val highest = (inverts + covers.filterNotNull()).maxOrNull() ?: 100.0
            val lowest = (inverts + covers.filterNotNull()).minOrNull() ?: 90.0
            val range = max(highest - lowest, 0.5)
            
            val topMargin = 40.dp.toPx()
            val bottomMargin = 40.dp.toPx()
            val drawHeight = size.height - topMargin - bottomMargin
            
            fun calcY(value: Double): Float = topMargin + ((highest - value) / range).toFloat() * drawHeight

            val totalDistance = nodes.dropLast(1).sumOf { NumberParser.parseDecimal(it.distanceToNextText) ?: 30.0 }
            
            var currentX = 60.dp.toPx()
            val xs = mutableListOf<Float>()
            for (i in nodes.indices) {
                xs.add(currentX)
                if (i < nodes.size - 1) {
                    val dist = NumberParser.parseDecimal(nodes[i].distanceToNextText) ?: 30.0
                    val ratio = (dist / max(totalDistance, 1.0)).toFloat()
                    currentX += max(100f, ratio * (size.width - 120.dp.toPx()))
                }
            }

            val bacaWidth = 20.dp.toPx()
            
            val dimColorAndroid = android.graphics.Color.parseColor("#8A939E")
            val badgeBgAndroid = android.graphics.Color.parseColor("#E7ECF3")
            val textColorAndroid = android.graphics.Color.parseColor("#15202B")

            val textPaint = android.graphics.Paint().apply {
                color = textColorAndroid
                textSize = 10.dp.toPx()
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val dimPaint = android.graphics.Paint().apply {
                color = dimColorAndroid
                textSize = 9.dp.toPx()
                isAntiAlias = true
                typeface = android.graphics.Typeface.MONOSPACE
                textAlign = android.graphics.Paint.Align.CENTER
            }

            fun drawBadge(text: String, cx: Float, cy: Float, paint: android.graphics.Paint, bgAndroid: Int) {
                val paddingX = 4.dp.toPx()
                val paddingY = 3.dp.toPx()
                val textWidth = paint.measureText(text)
                val fontMetrics = paint.fontMetrics
                val textHeight = fontMetrics.descent - fontMetrics.ascent
                
                val rectL = cx - textWidth / 2 - paddingX
                val rectT = cy - textHeight / 2 - paddingY + fontMetrics.descent
                val rectR = cx + textWidth / 2 + paddingX
                val rectB = cy + textHeight / 2 + paddingY + fontMetrics.descent
                
                val bgPaint = android.graphics.Paint().apply { color = bgAndroid; isAntiAlias = true }
                val radius = 6.dp.toPx()
                drawContext.canvas.nativeCanvas.drawRoundRect(rectL, rectT, rectR, rectB, radius, radius, bgPaint)
                drawContext.canvas.nativeCanvas.drawText(text, cx, cy - (fontMetrics.descent + fontMetrics.ascent) / 2, paint)
            }

            val topPoints = xs.indices.map { index ->
                val coverOffset = covers[index]?.let { cover ->
                    ((covers.filterNotNull().maxOrNull() ?: cover) - cover).toFloat() * 5f
                } ?: 0f
                Offset(xs[index], calcY(covers[index] ?: (highest - coverOffset.toDouble() / 5.0)))
            }

            if (covers.any { it != null }) {
                drawLine(color = Color(0xFFB9C1CC), start = topPoints.first(), end = topPoints.last(), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            }

            val groundPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(topPoints.first().x, topPoints.first().y)
                topPoints.forEach { lineTo(it.x, it.y) }
                lineTo(xs.last(), calcY(inverts.last()))
                for (i in xs.indices.reversed()) {
                    lineTo(xs[i], calcY(inverts[i]))
                }
                close()
            }
            drawPath(groundPath, Color(0xFFF0F3F7))

            for (i in 0 until nodes.size - 1) {
                val status = state.getSegmentAnalysis(i).status
                val pipeColor = statusColor(status)
                
                val pipeStartX = xs[i] + bacaWidth / 2
                val pipeEndX = xs[i+1] - bacaWidth / 2
                val pipeStartY = calcY(inverts[i]) - 8.dp.toPx()
                val pipeEndY = calcY(inverts[i+1]) - 8.dp.toPx()
                val pipeThickness = 12.dp.toPx()

                drawLine(
                    color = pipeColor.copy(alpha = 0.7f),
                    start = Offset(pipeStartX, pipeStartY),
                    end = Offset(pipeEndX, pipeEndY),
                    strokeWidth = pipeThickness,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(pipeStartX, pipeStartY - 2f),
                    end = Offset(pipeEndX, pipeEndY - 2f),
                    strokeWidth = pipeThickness / 2f,
                    cap = StrokeCap.Round
                )

                val dx = pipeEndX - pipeStartX
                val dy = pipeEndY - pipeStartY
                val len = kotlin.math.sqrt(dx * dx + dy * dy)
                
                // Her hattın oku, kullanıcı seçiminden bağımsız olarak yüksek
                // akar kotundan düşük akar kotuna doğru hareket eder.
                state.getSegmentFlowLeftToRight(i)?.let { shouldFlowLToR ->
                    for (arr in 0..2) {
                        var p = flowPhase + (arr * 0.33f)
                        if (!shouldFlowLToR) p = 1f - p
                        while (p < 0f) p += 1f
                        while (p > 1f) p -= 1f

                        val arrowX = pipeStartX + dx * p
                        val arrowY = pipeStartY + dy * p

                        val arrowSize = 5.dp.toPx()
                        val ux = if (shouldFlowLToR) dx / len else -dx / len
                        val uy = if (shouldFlowLToR) dy / len else -dy / len

                        val arrowPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(arrowX + ux * arrowSize, arrowY + uy * arrowSize)
                            lineTo(arrowX - (ux * arrowSize + uy * arrowSize), arrowY - (uy * arrowSize - ux * arrowSize))
                            lineTo(arrowX - (ux * arrowSize - uy * arrowSize), arrowY - (uy * arrowSize + ux * arrowSize))
                            close()
                        }
                        val alpha = if (p < 0.2f) p * 5f else if (p > 0.8f) (1f - p) * 5f else 1f
                        drawPath(arrowPath, color = Color.White.copy(alpha = alpha * 0.9f))
                    }
                }

                val dist = NumberParser.parseDecimal(nodes[i].distanceToNextText)
                if (dist != null && dist > 0) {
                    val midX = (pipeStartX + pipeEndX) / 2
                    drawBadge("<- ${NumberParser.formatDecimal(dist)} m ->", midX, size.height - 10.dp.toPx(), dimPaint, badgeBgAndroid)
                }
            }

            val surfaceColor = Color(0xFFF9FAFB)
            val borderColor = Color(0xFFD1D5DB)
            
            for (i in nodes.indices) {
                val cx = xs[i]
                val bTop = topPoints[i].y
                val bBottom = calcY(inverts[i])
                
                val manholeX = cx - bacaWidth / 2
                val manholeWidth = bacaWidth
                val manholeHeight = bBottom - bTop
                
                drawRoundRect(
                    color = surfaceColor,
                    topLeft = Offset(manholeX, bTop),
                    size = androidx.compose.ui.geometry.Size(manholeWidth, manholeHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(manholeX, bTop),
                    size = androidx.compose.ui.geometry.Size(manholeWidth, manholeHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
                drawLine(
                    color = TextSecondary,
                    start = Offset(manholeX - 4f, bTop),
                    end = Offset(manholeX + manholeWidth + 4f, bTop),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
                
                val nameLabel = nodes[i].name.ifBlank { "B${i+1}" }
                drawBadge(nameLabel, cx, bTop - 14.dp.toPx(), textPaint, badgeBgAndroid)
                
                covers[i]?.let {
                    drawBadge("K: ${NumberParser.formatDecimal(it)}", cx - 34.dp.toPx(), bTop + 10.dp.toPx(), dimPaint, android.graphics.Color.WHITE)
                }
                
                drawBadge("A: ${NumberParser.formatDecimal(inverts[i])}", cx + 34.dp.toPx(), bBottom, dimPaint, android.graphics.Color.WHITE)
            }
        }
    }
}

@Composable
private fun DistanceField(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(5.dp).background(AccentOrange.copy(alpha = 0.55f), CircleShape))
        CompactField(label, value, onValueChange, Modifier.weight(1f), suffix = "m")
    }
}

@Composable
private fun CompactField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(50.dp),
        label = { Text(label, fontSize = 9.sp) },
        suffix = suffix?.let { { Text(it, color = TextTertiary, fontSize = 9.sp) } },
        trailingIcon = trailingIcon,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FieldBackground, unfocusedContainerColor = FieldBackground,
            focusedBorderColor = AccentOrange, unfocusedBorderColor = Color.Transparent,
            focusedLabelColor = AccentOrange, unfocusedLabelColor = TextTertiary
        )
    )
}

@Composable
private fun AppleCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    padding: Dp = 14.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = Color.White.copy(alpha = 0.96f),
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
    ) {
        Box(Modifier.padding(padding)) { content() }
    }
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.size(30.dp), shape = CircleShape, color = Color.White, shadowElevation = 1.dp) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun statusColor(status: ImpactSlopeStatus): Color = when (status) {
    ImpactSlopeStatus.VALID -> SlopeOk
    ImpactSlopeStatus.REVERSE -> androidx.compose.ui.graphics.Color(0xFFE53935)
    ImpactSlopeStatus.TOO_FLAT, ImpactSlopeStatus.TOO_STEEP -> SlopeWarning
    ImpactSlopeStatus.INCOMPLETE -> TextTertiary
}

private fun statusText(status: ImpactSlopeStatus): String = when (status) {
    ImpactSlopeStatus.VALID -> "Eğim uygun"
    ImpactSlopeStatus.REVERSE -> "Ters eğim"
    ImpactSlopeStatus.TOO_FLAT -> "Eğim yetersiz"
    ImpactSlopeStatus.TOO_STEEP -> "Eğim fazla dik"
    ImpactSlopeStatus.INCOMPLETE -> "Veri eksik"
}

private fun format(value: Double, decimals: Int = 2): String = NumberParser.formatDecimal(value, decimals)

@Composable
fun ImpactMapPicker(
    onDismiss: () -> Unit,
    onImportChain: (List<Pair<com.example.egimhesabi.domain.StakeoutCalculationSource, Double?>>) -> Unit
) {
    val context = LocalContext.current
    val recordsFlow = remember(context) { com.example.egimhesabi.data.AppDatabase.getInstance(context).stakeoutDao().allRecords() }
    val allRecords by recordsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val districts = remember(allRecords) { allRecords.map { it.districtName }.distinct().sorted() }
    var selectedDistrict by rememberSaveable { mutableStateOf(com.example.egimhesabi.ui.components.StakeoutSession.lastDistrictName ?: districts.firstOrNull()) }
    if (selectedDistrict == null && districts.isNotEmpty()) {
        selectedDistrict = districts.first()
    }

    val neighborhoods = remember(allRecords, selectedDistrict) {
        allRecords.filter { it.districtName == selectedDistrict }
            .distinctBy { it.manhole.neighborhoodId }
            .sortedBy { it.neighborhoodName }
    }

    var selectedNeighborhoodId by rememberSaveable {
        mutableStateOf(com.example.egimhesabi.ui.components.StakeoutSession.lastNeighborhoodId ?: neighborhoods.firstOrNull()?.manhole?.neighborhoodId)
    }
    if ((selectedNeighborhoodId == null || !neighborhoods.any { it.manhole.neighborhoodId == selectedNeighborhoodId }) && neighborhoods.isNotEmpty()) {
        selectedNeighborhoodId = neighborhoods.first().manhole.neighborhoodId
    }

    LaunchedEffect(selectedDistrict, selectedNeighborhoodId) {
        if (selectedDistrict != null) com.example.egimhesabi.ui.components.StakeoutSession.lastDistrictName = selectedDistrict
        if (selectedNeighborhoodId != null) com.example.egimhesabi.ui.components.StakeoutSession.lastNeighborhoodId = selectedNeighborhoodId
    }

    val records = remember(allRecords, selectedNeighborhoodId) {
        allRecords.filter { it.manhole.neighborhoodId == selectedNeighborhoodId }
    }
    val validRecords = remember(records) {
        records.filter { it.manhole.projectX != null && it.manhole.projectY != null }
    }

    var basis by rememberSaveable {
        mutableStateOf(com.example.egimhesabi.ui.components.StakeoutSession.lastBasis ?: com.example.egimhesabi.domain.StakeoutElevationBasis.PROJECT)
    }

    val selectedChain = remember { androidx.compose.runtime.mutableStateListOf<com.example.egimhesabi.data.StakeoutRecord>() }

    var districtMenuExpanded by remember { mutableStateOf(false) }
    var neighborhoodMenuExpanded by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF1F5F9)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = TextPrimary)
                    }
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        Text(
                            text = "Haritadan Hat Seç",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Akış yönünde bacalara sırayla dokunun",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    if (selectedChain.isNotEmpty()) {
                        Button(
                            onClick = {
                                val chainPairs = selectedChain.mapIndexed { i, record ->
                                    val dist = if (i < selectedChain.size - 1) {
                                        val next = selectedChain[i + 1]
                                        val e1 = record.manhole
                                        val e2 = next.manhole
                                        if (e1.projectX != null && e1.projectY != null && e2.projectX != null && e2.projectY != null) {
                                            kotlin.math.hypot(e1.projectY - e2.projectY, e1.projectX - e2.projectX)
                                        } else null
                                    } else null
                                    com.example.egimhesabi.domain.StakeoutCalculationSource.from(record, basis) to dist
                                }
                                com.example.egimhesabi.ui.components.StakeoutSession.lastBasis = basis
                                onImportChain(chainPairs)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Hattı Aktar (${selectedChain.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Sub-header: Location picker and Elevation Basis
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // District selector
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            onClick = { districtMenuExpanded = true },
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedDistrict ?: "İlçe",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                maxLines = 1
                            )
                        }
                        DropdownMenu(
                            expanded = districtMenuExpanded,
                            onDismissRequest = { districtMenuExpanded = false }
                        ) {
                            districts.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d, fontSize = 13.sp) },
                                    onClick = {
                                        selectedDistrict = d
                                        districtMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Neighborhood selector
                    Box(modifier = Modifier.weight(1.2f)) {
                        Surface(
                            onClick = { neighborhoodMenuExpanded = true },
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val curNName = neighborhoods.firstOrNull { it.manhole.neighborhoodId == selectedNeighborhoodId }?.neighborhoodName
                            Text(
                                text = curNName ?: "Mahalle",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                maxLines = 1
                            )
                        }
                        DropdownMenu(
                            expanded = neighborhoodMenuExpanded,
                            onDismissRequest = { neighborhoodMenuExpanded = false }
                        ) {
                            neighborhoods.forEach { n ->
                                DropdownMenuItem(
                                    text = { Text(n.neighborhoodName, fontSize = 13.sp) },
                                    onClick = {
                                        selectedNeighborhoodId = n.manhole.neighborhoodId
                                        neighborhoodMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Basis buttons (Proje / Arazi)
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        com.example.egimhesabi.domain.StakeoutElevationBasis.entries.forEach { b ->
                            val isSel = basis == b
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) AccentOrange else Color.Transparent)
                                    .clickable {
                                        basis = b
                                        com.example.egimhesabi.ui.components.StakeoutSession.lastBasis = b
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (b == com.example.egimhesabi.domain.StakeoutElevationBasis.PROJECT) "Proje" else "Arazi",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Map canvas area
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (validRecords.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Bu mahallede koordinatlı baca bulunamadı.", color = TextSecondary, fontSize = 13.sp)
                                if (records.isNotEmpty()) {
                                    Text("(${records.size} baca var fakat X/Y koordinatları boş)", color = TextTertiary, fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        var scale by remember { mutableStateOf(1f) }
                        var offset by remember { mutableStateOf(Offset.Zero) }
                        val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()

                        // Coordinates: projectY is Easting (Canvas X), projectX is Northing (Canvas Y)
                        val minX = remember(validRecords) { validRecords.minOf { it.manhole.projectY!! } }
                        val maxX = remember(validRecords) { validRecords.maxOf { it.manhole.projectY!! } }
                        val minY = remember(validRecords) { validRecords.minOf { it.manhole.projectX!! } }
                        val maxY = remember(validRecords) { validRecords.maxOf { it.manhole.projectX!! } }

                        val rangeX = remember(minX, maxX) { kotlin.math.max(maxX - minX, 1.0) }
                        val rangeY = remember(minY, maxY) { kotlin.math.max(maxY - minY, 1.0) }
                        val padX = remember(rangeX) { rangeX * 0.12 }
                        val padY = remember(rangeY) { rangeY * 0.12 }

                        val adjMinX = remember(minX, padX) { minX - padX }
                        val adjMaxX = remember(maxX, padX) { maxX + padX }
                        val adjMinY = remember(minY, padY) { minY - padY }
                        val adjMaxY = remember(maxY, padY) { maxY + padY }
                        val totalRangeX = remember(adjMinX, adjMaxX) { adjMaxX - adjMinX }
                        val totalRangeY = remember(adjMinY, adjMaxY) { adjMaxY - adjMinY }

                        androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val cw = constraints.maxWidth
                            val ch = constraints.maxHeight
                            val bsX = cw / totalRangeX.toFloat()
                            val bsY = ch / totalRangeY.toFloat()
                            val baseScale = minOf(bsX, bsY)
                            val drawWidth = totalRangeX.toFloat() * baseScale
                            val drawHeight = totalRangeY.toFloat() * baseScale
                            val drawOffsetX = (cw - drawWidth) / 2f
                            val drawOffsetY = (ch - drawHeight) / 2f

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { centroid, pan, zoom, _ ->
                                            val newScale = (scale * zoom).coerceIn(0.3f, 30f)
                                            val actualZoom = newScale / scale
                                            offset = (offset + pan - centroid) * actualZoom + centroid
                                            scale = newScale
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectTapGestures { tapOffset ->
                                            var closest: com.example.egimhesabi.data.StakeoutRecord? = null
                                            var minDistance = Float.MAX_VALUE
                                            val threshold = 38.dp.toPx()

                                            validRecords.forEach { record ->
                                                val cX = drawOffsetX + ((record.manhole.projectY!! - adjMinX) * baseScale).toFloat()
                                                val cY = drawOffsetY + ((adjMaxY - record.manhole.projectX!!) * baseScale).toFloat()
                                                val fX = cX * scale + offset.x
                                                val fY = cY * scale + offset.y
                                                val dist = kotlin.math.hypot(fX - tapOffset.x, fY - tapOffset.y)
                                                if (dist < minDistance && dist < threshold) {
                                                    minDistance = dist
                                                    closest = record
                                                }
                                            }

                                            if (closest != null) {
                                                if (selectedChain.lastOrNull()?.manhole?.id == closest.manhole.id) {
                                                    selectedChain.removeAt(selectedChain.lastIndex)
                                                } else if (selectedChain.any { it.manhole.id == closest.manhole.id }) {
                                                    // Already in chain
                                                } else {
                                                    selectedChain.add(closest)
                                                }
                                            }
                                        }
                                    }
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // Light CAD Grid
                                    val gridSpacingPx = 80.dp.toPx() * scale.coerceIn(0.6f, 2f)
                                    val startGridX = offset.x % gridSpacingPx
                                    val startGridY = offset.y % gridSpacingPx
                                    var gx = startGridX
                                    while (gx < size.width) {
                                        drawLine(Color(0xFFE2E8F0).copy(alpha = 0.6f), Offset(gx, 0f), Offset(gx, size.height), 1f)
                                        gx += gridSpacingPx
                                    }
                                    var gy = startGridY
                                    while (gy < size.height) {
                                        drawLine(Color(0xFFE2E8F0).copy(alpha = 0.6f), Offset(0f, gy), Offset(size.width, gy), 1f)
                                        gy += gridSpacingPx
                                    }

                                    // Draw connections and arrows between chain manholes
                                    for (i in 0 until selectedChain.size - 1) {
                                        val r1 = selectedChain[i].manhole
                                        val r2 = selectedChain[i + 1].manhole
                                        if (r1.projectX != null && r1.projectY != null && r2.projectX != null && r2.projectY != null) {
                                            val cX1 = drawOffsetX + ((r1.projectY - adjMinX) * baseScale).toFloat()
                                            val cY1 = drawOffsetY + ((adjMaxY - r1.projectX) * baseScale).toFloat()
                                            val fX1 = cX1 * scale + offset.x
                                            val fY1 = cY1 * scale + offset.y

                                            val cX2 = drawOffsetX + ((r2.projectY - adjMinX) * baseScale).toFloat()
                                            val cY2 = drawOffsetY + ((adjMaxY - r2.projectX) * baseScale).toFloat()
                                            val fX2 = cX2 * scale + offset.x
                                            val fY2 = cY2 * scale + offset.y

                                            val lineStroke = 3.dp.toPx()
                                            val lineColor = Color(0xFF007AFF)

                                            // Shadow line
                                            drawLine(Color.White, Offset(fX1, fY1), Offset(fX2, fY2), strokeWidth = lineStroke + 2.dp.toPx())
                                            drawLine(lineColor, Offset(fX1, fY1), Offset(fX2, fY2), strokeWidth = lineStroke)

                                            // Direction arrow
                                            val angle = kotlin.math.atan2(fY2 - fY1, fX2 - fX1)
                                            val arrowLen = 14.dp.toPx()
                                            val a1 = angle + kotlin.math.PI / 6
                                            val a2 = angle - kotlin.math.PI / 6
                                            drawLine(lineColor, Offset(fX2, fY2), Offset(fX2 - arrowLen * kotlin.math.cos(a1).toFloat(), fY2 - arrowLen * kotlin.math.sin(a1).toFloat()), strokeWidth = lineStroke)
                                            drawLine(lineColor, Offset(fX2, fY2), Offset(fX2 - arrowLen * kotlin.math.cos(a2).toFloat(), fY2 - arrowLen * kotlin.math.sin(a2).toFloat()), strokeWidth = lineStroke)

                                            // Distance badge in middle
                                            val midX = (fX1 + fX2) / 2f
                                            val midY = (fY1 + fY2) / 2f
                                            val dMeters = kotlin.math.hypot(r1.projectY - r2.projectY, r1.projectX - r2.projectX)
                                            val distText = "${NumberParser.formatDecimal(dMeters)} m"
                                            val distLayout = textMeasurer.measure(
                                                distText,
                                                androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                                            )
                                            val padW = 6.dp.toPx()
                                            val padH = 2.dp.toPx()
                                            val badgeW = distLayout.size.width + padW * 2
                                            val badgeH = distLayout.size.height + padH * 2
                                            drawRoundRect(
                                                color = Color.White,
                                                topLeft = Offset(midX - badgeW / 2f, midY - badgeH / 2f),
                                                size = Size(badgeW, badgeH),
                                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                            )
                                            drawRoundRect(
                                                color = Color(0xFF007AFF).copy(alpha = 0.5f),
                                                topLeft = Offset(midX - badgeW / 2f, midY - badgeH / 2f),
                                                size = Size(badgeW, badgeH),
                                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                                style = Stroke(1.dp.toPx())
                                            )
                                            drawText(distLayout, topLeft = Offset(midX - distLayout.size.width / 2f, midY - distLayout.size.height / 2f))
                                        }
                                    }

                                    // Draw manhole nodes with Name, Project Invert, Depth, and Terrain Invert
                                    validRecords.forEach { record ->
                                        val pX = record.manhole.projectX!!
                                        val pY = record.manhole.projectY!!
                                        val cX = drawOffsetX + ((pY - adjMinX) * baseScale).toFloat()
                                        val cY = drawOffsetY + ((adjMaxY - pX) * baseScale).toFloat()
                                        val fX = cX * scale + offset.x
                                        val fY = cY * scale + offset.y

                                        val chainIndex = selectedChain.indexOfFirst { it.manhole.id == record.manhole.id }
                                        val isInChain = chainIndex != -1

                                        val pInv = record.manhole.projectInvertLevel
                                        val tInv = record.manhole.terrainInvertLevel
                                        val depth = record.manhole.dischargeDepth
                                            ?: if (record.manhole.projectCoverLevel != null && pInv != null) {
                                                record.manhole.projectCoverLevel - pInv
                                            } else null

                                        val hasEmir = pInv != null && tInv != null && pInv > tInv
                                        val isMissing = record.manhole.projectCoverLevel == null || pInv == null

                                        val nodeColor = when {
                                            isInChain -> Color(0xFF007AFF)
                                            hasEmir -> Color(0xFFFF9500)
                                            isMissing -> Color(0xFFFF3B30)
                                            else -> Color(0xFF34C759)
                                        }

                                        if (isInChain) {
                                            // Big badge with sequence number
                                            val rCircle = 12.dp.toPx()
                                            drawCircle(Color.White, rCircle + 2.dp.toPx(), Offset(fX, fY))
                                            drawCircle(Color(0xFF007AFF), rCircle, Offset(fX, fY))

                                            val numText = "${chainIndex + 1}"
                                            val numLayout = textMeasurer.measure(
                                                numText,
                                                androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            )
                                            drawText(numLayout, topLeft = Offset(fX - numLayout.size.width / 2f, fY - numLayout.size.height / 2f))
                                        } else {
                                            drawCircle(Color.White, 6.dp.toPx(), Offset(fX, fY))
                                            drawCircle(nodeColor, 4.5f.dp.toPx(), Offset(fX, fY))
                                        }

                                        // Text Labels:
                                        // Line 1: Manhole Name
                                        // Line 2: PA: xx.xx  D: x.xxm  AA: xx.xx
                                        val labelX = fX + (if (isInChain) 15.dp.toPx() else 8.dp.toPx())

                                        // Zoom detail threshold
                                        val showDetails = scale > 1.7f

                                        val nameStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 11.sp,
                                            fontWeight = if (isInChain) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isInChain) Color(0xFF007AFF) else Color(0xFF0F172A)
                                        )
                                        val nameLayout = textMeasurer.measure(record.manhole.name, nameStyle)
                                        val nameY = if (showDetails) fY - nameLayout.size.height + 2f else fY - nameLayout.size.height / 2f

                                        val haloOffsets = listOf(
                                            Offset(-1.5f, 0f), Offset(1.5f, 0f), Offset(0f, -1.5f), Offset(0f, 1.5f),
                                            Offset(-1f, -1f), Offset(1f, 1f), Offset(-1f, 1f), Offset(1f, -1f)
                                        )
                                        val haloNameStyle = nameStyle.copy(color = Color.White)
                                        val haloNameLayout = textMeasurer.measure(record.manhole.name, haloNameStyle)

                                        haloOffsets.forEach { ho ->
                                            drawText(haloNameLayout, topLeft = Offset(labelX + ho.x, nameY + ho.y))
                                        }
                                        drawText(nameLayout, topLeft = Offset(labelX, nameY))

                                        if (showDetails) {
                                            // Values string
                                            val pAkarText = pInv?.let { NumberParser.formatDecimal(it) } ?: "—"
                                            val depthText = depth?.let { NumberParser.formatDecimal(it) } ?: "—"
                                            val aAkarText = tInv?.let { NumberParser.formatDecimal(it) } ?: "—"
                                            val infoText = "PA: $pAkarText  D: $depthText  AA: $aAkarText"

                                            val infoStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155)
                                            )
                                            val infoLayout = textMeasurer.measure(infoText, infoStyle)
                                            val infoY = fY + 2f

                                            val haloInfoStyle = infoStyle.copy(color = Color.White)
                                            val haloInfoLayout = textMeasurer.measure(infoText, haloInfoStyle)

                                            haloOffsets.forEach { ho ->
                                                drawText(haloInfoLayout, topLeft = Offset(labelX + ho.x, infoY + ho.y))
                                            }
                                            drawText(infoLayout, topLeft = Offset(labelX, infoY))
                                        }
                                    }
                                }
                            }
                        }

                        // Reset button top-right inside canvas
                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                                .size(34.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sıfırla", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Chain mode bottom bar
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedChain.isEmpty()) {
                            Text("Hattın başlangıç bacasına dokunun", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        } else {
                            val totalDist = (0 until selectedChain.size - 1).sumOf { i ->
                                val r1 = selectedChain[i].manhole
                                val r2 = selectedChain[i + 1].manhole
                                if (r1.projectX != null && r1.projectY != null && r2.projectX != null && r2.projectY != null) {
                                    kotlin.math.hypot(r1.projectY - r2.projectY, r1.projectX - r2.projectX)
                                } else 0.0
                            }
                            Text(
                                text = "${selectedChain.size} baca • ${NumberParser.formatDecimal(totalDist)} m",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Geri Al",
                                color = Color(0xFFFF9500),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { if (selectedChain.isNotEmpty()) selectedChain.removeAt(selectedChain.lastIndex) }
                            )
                            Text(
                                text = "Sıfırla",
                                color = Color(0xFFFF3B30),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { selectedChain.clear() }
                            )
                        }
                    }
                }
            }
        }
    }
}
