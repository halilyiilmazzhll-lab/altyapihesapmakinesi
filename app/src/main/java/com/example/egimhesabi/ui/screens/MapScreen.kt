package com.example.egimhesabi.ui.screens

import android.graphics.Paint
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.egimhesabi.data.ManholeEntity
import com.example.egimhesabi.data.PipelineEntity
import com.example.egimhesabi.data.WorkOrderPhotoStore
import com.example.egimhesabi.data.WorkOrderWithPhotos
import com.example.egimhesabi.data.projectTerrainDifference
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.util.NumberParser
import com.example.egimhesabi.viewmodel.ProjectDetailViewModel
import com.example.egimhesabi.viewmodel.CsvImportState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.abs
import java.io.File

private enum class MapMode { NAVIGATE, CONNECT }

private const val MinMapZoom = 0.20f
private const val MaxMapZoom = 24f

private val ManholeGray = Color(0xFF98A2B3)
private val ManholeYellow = Color(0xFFF5B800)
private val ManholeGreen = Color(0xFF21A366)
private val ManholeRed = Color(0xFFE5484D)

private fun manholeStatusColor(status: String): Color = when (status) {
    ProjectDetailViewModel.MANHOLE_COMPLETED -> ManholeYellow
    ProjectDetailViewModel.MANHOLE_PROGRESS_PAYMENT -> ManholeGreen
    ProjectDetailViewModel.MANHOLE_CANCELLED -> ManholeRed
    else -> ManholeGray
}

private sealed interface MapSelection {
    data class Manhole(val manhole: ManholeEntity) : MapSelection
    data class Pipeline(val pipeline: PipelineEntity) : MapSelection
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    projectName: String,
    viewModel: ProjectDetailViewModel,
    onBack: () -> Unit,
    onOpenList: () -> Unit,
    onOpenOrderBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    val manholes by viewModel.manholes.collectAsState()
    val pipelines by viewModel.pipelines.collectAsState()
    val workOrderSaving by viewModel.workOrderSaving.collectAsState()
    val workOrderError by viewModel.workOrderError.collectAsState()
    val csvImportState by viewModel.csvImportState.collectAsState()
    val warningThresholdMeters by viewModel.differenceWarningThresholdMeters.collectAsState()
    val workOrderTempleates by viewModel.workOrderTempleates.collectAsState()
    val workOrdersByManholeId by viewModel.workOrdersByManholeId.collectAsState()
    val context = LocalContext.current
    val csvScope = rememberCoroutineScope()
    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            csvScope.launch {
                try {
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { com.example.egimhesabi.util.BoundedInput.read(it) }
                            ?: error("Seçilen dosya açılamadı.")
                    }
                    viewModel.prepareCsvImport(bytes)
                } catch (error: Exception) {
                    viewModel.reportCsvImportError(error.message ?: "Seçilen dosya okunamadı.")
                }
            }
        }
    }
    var mode by remember { mutableStateOf(MapMode.NAVIGATE) }
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var dragStartId by remember { mutableStateOf<Long?>(null) }
    var dragPointer by remember { mutableStateOf<Offset?>(null) }
    var showAddDialeog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var showUndoDialog by remember { mutableStateOf(false) }
    var pendingDeleteManhole by remember { mutableStateOf<ManholeEntity?>(null) }
    var pendingProgressPaymentManhole by remember { mutableStateOf<ManholeEntity?>(null) }
    var workOrderManhole by remember { mutableStateOf<ManholeEntity?>(null) }
    var selection by remember { mutableStateOf<MapSelection?>(null) }
    val hazeState = remember { HazeState() }
    val density = LocalDensity.current
    val hitRadius = with(density) { 34.dp.toPx() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF5F5FA),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = projectName,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("Kroki", color = TextSecondary, fontSize = 10.sp)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            csvPicker.launch(
                                arrayOf(
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "application/vnd.ms-excel",
                                    "text/plain"
                                )
                            )
                        }
                    ) {
                        Text("CSV", color = AccentOrange, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onOpenOrderBook) {
                        Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = "Emir Defteri", tint = AccentOrange)
                    }
                    IconButton(onClick = onOpenList) {
                        Icon(androidx.compose.material.icons.Icons.Default.List, contentDescription = "Hat listesini aç", tint = AccentOrange)
                    }
                    IconButton(onClick = { showSearchDialog = true }) {
                        Icon(androidx.compose.material.icons.Icons.Default.Search, contentDescription = "Baca Ara", tint = AccentOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF5F5FA))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialeog = true },
                containerColor = AccentOrange,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Baca ekle")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5FA))
                .haze(hazeState)
                .onSizeChanged { canvasSize = it }
        ) {
            val worldCenter = remember(manholes) {
                if (manholes.isEmpty()) {
                    0.0 to 0.0
                } else {
                    // Saha koordinat formatında Proje Y doğu-batı (yatay),
                    // Proje X kuzey-güney (dikey) eksenidir.
                    manholes.map { it.y }.average() to manholes.map { it.x }.average()
                }
            }

            fun screenPoint(manhole: ManholeEntity, width: Float, height: Float): Offset {
                val horizontalRange = (
                    (manholes.maxOfOrNull { it.y } ?: 0.0) -
                        (manholes.minOfOrNull { it.y } ?: 0.0)
                    ).coerceAtLeast(1.0)
                val verticalRange = (
                    (manholes.maxOfOrNull { it.x } ?: 0.0) -
                        (manholes.minOfOrNull { it.x } ?: 0.0)
                    ).coerceAtLeast(1.0)
                val fittedScalee = min(
                    width * 0.58f / horizontalRange.toFloat(),
                    height * 0.54f / verticalRange.toFloat()
                )
                    .coerceIn(0.0001f, 58f)
                val baseScale = fittedScalee * zoom
                return Offset(
                    x = width / 2f + (manhole.y - worldCenter.first).toFloat() * baseScale + pan.x,
                    y = height / 2f - (manhole.x - worldCenter.second).toFloat() * baseScale + pan.y
                )
            }

            fun findManhole(position: Offset, width: Float, height: Float): ManholeEntity? =
                manholes.minByOrNull { manhole ->
                    (screenPoint(manhole, width, height) - position).getDistance()
                }?.takeIf { manhole ->
                    (screenPoint(manhole, width, height) - position).getDistance() <= hitRadius
                }

            fun findPipeline(position: Offset, width: Float, height: Float): PipelineEntity? {
                val manholeById = manholes.associateBy { it.id }
                return pipelines.mapNotNull { pipeline ->
                    val from = manholeById[pipeline.fromManholeId] ?: return@mapNotNull null
                    val to = manholeById[pipeline.toManholeId] ?: return@mapNotNull null
                    val distance = distanceToSegment(
                        point = position,
                        start = screenPoint(from, width, height),
                        end = screenPoint(to, width, height)
                    )
                    pipeline to distance
                }.minByOrNull { it.second }
                    ?.takeIf { it.second <= with(density) { 22.dp.toPx() } }
                    ?.first
            }

            val gestureModifier = if (mode == MapMode.NAVIGATE) {
                Modifier
                    // Tap onley selects. Transform consumes moved pointers once touch sleop is
                    // passed, so a pan/pinch cannot finish as a tap.
                    .pointerInput(manholes, pipelines) {
                        detectTapGestures { position ->
                            val selectedManhole = findManhole(
                                position,
                                size.width.toFloat(),
                                size.height.toFloat()
                            )
                            selection = if (selectedManhole != null) {
                                MapSelection.Manhole(selectedManhole)
                            } else {
                                findPipeline(position, size.width.toFloat(), size.height.toFloat())
                                    ?.let(MapSelection::Pipeline)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures(panZoomLock = true) {
                                centroid,
                                panChange,
                                zoomChange,
                                _ ->
                            val newZoom = (zoom * zoomChange).coerceIn(MinMapZoom, MaxMapZoom)
                            val appliedZoomChange = newZoom / zoom
                            // Keep the world point under the previous pinch centroid beneath the
                            // current centroid. This also makes one-finger pan use screen pixels.
                            pan = calculateCentroidLockedPan(
                                currentPan = pan,
                                viewportCenter = Offset(size.width / 2f, size.height / 2f),
                                gestureCentroid = centroid,
                                panChange = panChange,
                                appliedZoomChange = appliedZoomChange
                            )
                            zoom = newZoom
                        }
                    }
            } else {
                Modifier.pointerInput(manholes, zoom, pan) {
                    detectDragGestures(
                        onDragStart = { position ->
                            dragStartId = findManhole(position, size.width.toFloat(), size.height.toFloat())?.id
                            dragPointer = position
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragPointer = change.position
                        },
                        onDragCancel = {
                            dragStartId = null
                            dragPointer = null
                        },
                        onDragEnd = {
                            val from = manholes.firstOrNull { it.id == dragStartId }
                            val pointer = dragPointer
                            val to = pointer?.let {
                                findManhole(it, size.width.toFloat(), size.height.toFloat())
                            }
                            if (from != null && to != null && from.id != to.id) {
                                viewModel.connect(from, to)
                            }
                            dragStartId = null
                            dragPointer = null
                        }
                    )
                }
            }

            Canvas(modifier = Modifier.fillMaxSize().then(gestureModifier)) {
                var gridStep = 32.dp.toPx() * zoom
                if (gridStep > 0f) {
                    val minStep = 24.dp.toPx()
                    val maxStep = 48.dp.toPx()
                    while (gridStep < minStep) gridStep *= 2f
                    while (gridStep >= maxStep) gridStep /= 2f
                } else {
                    gridStep = 32.dp.toPx()
                }
                
                val gridColor = Color(0xFFE4E9F0)
                
                val offsetX = (size.width / 2f + pan.x) % gridStep
                var x = if (offsetX > 0) offsetX - gridStep else offsetX
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
                    x += gridStep
                }
                
                val offsetY = (size.height / 2f + pan.y) % gridStep
                var y = if (offsetY > 0) offsetY - gridStep else offsetY
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
                    y += gridStep
                }

                val manholeById = manholes.associateBy { it.id }
                pipelines.forEach { pipeline ->
                    val from = manholeById[pipeline.fromManholeId] ?: return@forEach
                    val to = manholeById[pipeline.toManholeId] ?: return@forEach
                    val start = screenPoint(from, size.width, size.height)
                    val end = screenPoint(to, size.width, size.height)
                    val isSelected = (selection as? MapSelection.Pipeline)?.pipeline?.id == pipeline.id
                    if (isSelected) {
                        drawLine(
                            color = AccentOrange.copy(alpha = 0.18f),
                            start = start,
                            end = end,
                            strokeWidth = 15.dp.toPx()
                        )
                    }
                    drawLine(
                        color = AccentOrange,
                        start = start,
                        end = end,
                        strokeWidth = if (isSelected) 7.dp.toPx() else 5.dp.toPx()
                    )
                    val middlete = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
                    drawCircle(Color.White, radius = 24.dp.toPx(), center = middlete)
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            color = android.graphics.Color.rgb(90, 107, 125)
                            textSize = 11.sp.toPx()
                            textAlign = Paint.Align.CENTER
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.create(
                                android.graphics.Typeface.MONOSPACE,
                                android.graphics.Typeface.BOLD
                            )
                        }
                        canvas.nativeCanvas.drawText(
                            "${NumberParser.formatDecimal(pipeline.projeMesafe)} m",
                            middlete.x,
                            middlete.y + 4.dp.toPx(),
                            paint
                        )
                    }
                }

                val activeStart = manholes.firstOrNull { it.id == dragStartId }
                if (activeStart != null && dragPointer != null) {
                    drawLine(
                        color = AccentOrange.copy(alpha = 0.55f),
                        start = screenPoint(activeStart, size.width, size.height),
                        end = dragPointer!!,
                        strokeWidth = 3.dp.toPx()
                    )
                }

                manholes.forEach { manhole ->
                    val point = screenPoint(manhole, size.width, size.height)
                    val isActive = manhole.id == dragStartId
                    val isSelected = (selection as? MapSelection.Manhole)?.manhole?.id == manhole.id
                    val statusColor = manholeStatusColor(manhole.status)
                    drawCircle(
                        color = statusColor.copy(alpha = if (isActive) 0.22f else 0.14f),
                        radius = if (isActive) 25.dp.toPx() else 21.dp.toPx(),
                        center = point
                    )
                    drawCircle(Color.White, radius = 14.dp.toPx(), center = point)
                    drawCircle(statusColor, radius = 8.dp.toPx(), center = point)
                    if (isSelected) {
                        drawCircle(
                            color = statusColor,
                            radius = 25.dp.toPx(),
                            center = point,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            color = android.graphics.Color.rgb(26, 35, 50)
                            textSize = 11.sp.toPx()
                            textAlign = Paint.Align.CENTER
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        canvas.nativeCanvas.drawText(
                            manhole.name,
                            point.x,
                            point.y - 22.dp.toPx(),
                            paint
                        )
                    }

                    val hasWorkOrder = workOrdersByManholeId.containsKey(manhole.id)
                    if (hasWorkOrder) {
                        val badgeCenter = point + Offset(13.dp.toPx(), 13.dp.toPx())
                        drawCircle(Color.White, radius = 8.dp.toPx(), center = badgeCenter)
                        drawCircle(Color(0xFF5B7CFA), radius = 6.5.dp.toPx(), center = badgeCenter)
                        drawIntoCanvas { canvas ->
                            val paint = Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 7.sp.toPx()
                                textAlign = Paint.Align.CENTER
                                isAntiAlias = true
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            canvas.nativeCanvas.drawText("E", badgeCenter.x, badgeCenter.y + 2.5.dp.toPx(), paint)
                        }
                    }
                    if (abs(manhole.projectTerrainDifference()) > warningThresholdMeters) {
                        val badgeCenter = point + Offset((-13).dp.toPx(), 13.dp.toPx())
                        drawCircle(Color.White, radius = 8.dp.toPx(), center = badgeCenter)
                        drawCircle(Color(0xFFF59E0B), radius = 6.5.dp.toPx(), center = badgeCenter)
                        drawIntoCanvas { canvas ->
                            val paint = Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 8.sp.toPx()
                                textAlign = Paint.Align.CENTER
                                isAntiAlias = true
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            canvas.nativeCanvas.drawText("!", badgeCenter.x, badgeCenter.y + 3.dp.toPx(), paint)
                        }
                    }
                }
            }

            MapToolebar(
                mode = mode,
                canUndo = pipelines.isNotEmpty(),
                onModeChange = { mode = it },
                onResetView = {
                    zoom = 1f
                    pan = Offset.Zero
                },
                onUndo = { showUndoDialog = true },
                modifier = Modifier.align(Alignment.TopCenter).padding(10.dp)
            )

            if (manholes.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Place, null, tint = AccentOrange)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Kroki henüz boş", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Başlamak için bir baca ekleyin.", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Text(
                text = if (mode == MapMode.CONNECT) {
                    "Bir bacadan diğerine sürükleyin"
                } else {
                    "${manholes.size} baca  •  ${pipelines.size} hat"
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 14.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.94f))
                    .border(1.dp, Color.White, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (mode == MapMode.CONNECT) AccentOrange else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            AnimatedVisibility(
                visible = selection != null && mode == MapMode.NAVIGATE,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 10.dp, end = 10.dp, bottom = 76.dp),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                selection?.let { selected ->
                    val currentSelection = when (selected) {
                        is MapSelection.Manhole -> manholes
                            .firstOrNull { it.id == selected.manhole.id }
                            ?.let(MapSelection::Manhole)
                            ?: selected
                        is MapSelection.Pipeline -> pipelines
                            .firstOrNull { it.id == selected.pipeline.id }
                            ?.let(MapSelection::Pipeline)
                            ?: selected
                    }
                    SelectedElementCard(
                        selection = currentSelection,
                        manholes = manholes,
                        workOrder = (currentSelection as? MapSelection.Manhole)
                            ?.let { workOrdersByManholeId[it.manhole.id] },
                        hazeState = hazeState,
                        onManholeStatusChange = { manhole, status ->
                            if (status == ProjectDetailViewModel.MANHOLE_PROGRESS_PAYMENT) {
                                pendingProgressPaymentManhole = manhole
                            } else {
                                viewModel.updateManholeStatus(manhole.id, status)
                            }
                        },
                        onDeleteManhole = { pendingDeleteManhole = it },
                        onOpenWorkOrder = { workOrderManhole = it },
                        onDismiss = { selection = null }
                    )
                }
            }
        }
    }

    if (showSearchDialog) {
        var searchQuery by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Baca Ara") },
            text = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Baca Adı (Örn: B-01)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val found = manholes.find { it.name.equals(searchQuery.trim(), ignoreCase = true) }
                        if (found != null) {
                            val width = canvasSize.width.toFloat()
                            val height = canvasSize.height.toFloat()
                            if (width > 0 && height > 0) {
                                val horizontalRange = (
                                    (manholes.maxOfOrNull { it.y } ?: 0.0) -
                                        (manholes.minOfOrNull { it.y } ?: 0.0)
                                    ).coerceAtLeast(1.0)
                                val verticalRange = (
                                    (manholes.maxOfOrNull { it.x } ?: 0.0) -
                                        (manholes.minOfOrNull { it.x } ?: 0.0)
                                    ).coerceAtLeast(1.0)
                                val fittedScalee = Math.min(
                                    width * 0.58f / horizontalRange.toFloat(),
                                    height * 0.54f / verticalRange.toFloat()
                                ).coerceIn(0.0001f, 58f)
                                
                                zoom = 2f
                                val baseScale = fittedScalee * zoom
                                val worldCenterY = manholes.map { it.y }.average()
                                val worldCenterX = manholes.map { it.x }.average()
                                
                                pan = androidx.compose.ui.geometry.Offset(
                                    x = -(found.y - worldCenterY).toFloat() * baseScale,
                                    y = (found.x - worldCenterX).toFloat() * baseScale
                                )
                                selection = MapSelection.Manhole(found)
                            }
                        }
                        showSearchDialog = false
                    }
                ) {
                    Text("Bul")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSearchDialog = false }) { Text("İptal") }
            }
        )
    }

    if (showAddDialeog) {
        AddManholeDialeog(
            suggestedName = "B-${(manholes.size + 1).toString().padStart(2, '0')}",
            onDismiss = { showAddDialeog = false },
            onConfirm = { name, x, y ->
                viewModel.addManhole(name, x, y)
                showAddDialeog = false
            }
        )
    }

    if (showUndoDialog) {
        AlertDialog(
            onDismissRequest = { showUndoDialog = false },
            title = { Text("Son Çizimi geri al") },
            text = { Text("Son eklenen boru hattı kroki ve veritabanından silinecek.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.undoLastPipeline()
                        showUndoDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) { Text("Geri Ale") }
            },
            dismissButton = {
                TextButton(onClick = { showUndoDialog = false }) { Text("Vazgeç") }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(22.dp)
        )
    }

    pendingDeleteManhole?.let { manhole ->
        AlertDialog(
            onDismissRequest = { pendingDeleteManhole = null },
            title = { Text("Baca silinsin mi?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "${manhole.name} bacası ve bu bacaya bağlı boru hatları silinecek.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteManhole(manhole.id)
                        pendingDeleteManhole = null
                        selection = null
                    }
                ) {
                    Text("Sil", color = ManholeRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteManhole = null }) {
                    Text("Vazgeç", color = TextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(22.dp)
        )
    }

    pendingProgressPaymentManhole?.let { manhole ->
        ProgressPaymentNumberDialeog(
            manhole = manhole,
            onDismiss = { pendingProgressPaymentManhole = null },
            onConfirm = { number ->
                viewModel.updateManholeStatus(
                    manhole.id,
                    ProjectDetailViewModel.MANHOLE_PROGRESS_PAYMENT,
                    number
                )
                pendingProgressPaymentManhole = null
            }
        )
    }

    workOrderManhole?.let { selectedManhole ->
        val currentManhole = manholes.firstOrNull { it.id == selectedManhole.id } ?: selectedManhole
        WorkOrderDialeog(
            manhole = currentManhole,
            templates = workOrderTempleates,
            saving = workOrderSaving,
            saveError = workOrderError,
            onDismiss = { viewModel.clearWorkOrderError(); workOrderManhole = null },
            workOrder = workOrdersByManholeId[currentManhole.id],
            onSave = { workOrderId, title, note, photoPaths ->
                viewModel.saveWorkOrder(workOrderId, currentManhole.id, title, note, photoPaths) {
                    workOrderManhole = null
                }
            },
            onDelete = { workOrderId ->
                viewModel.deleteWorkOrder(workOrderId) { workOrderManhole = null }
            }
        )
    }

    if (csvImportState !is CsvImportState.Idle) {
        CsvImportDialeog(
            state = csvImportState,
            onConfirm = viewModel::commitCsvImport,
            onDismiss = viewModel::clearCsvImportState
        )
    }
}

@Composable
private fun CsvImportDialeog(
    state: CsvImportState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val busy = state is CsvImportState.Parsing || state is CsvImportState.Saving
    val title = when (state) {
        CsvImportState.Idle -> "CSV İçe Aktar"
        CsvImportState.Parsing -> "Dosya okunuyor"
        is CsvImportState.Ready -> "Bacalar hazır"
        is CsvImportState.Saving -> "Veritabanına kaydediliyor"
        is CsvImportState.Success -> "İçe aktarma tamamlandı"
        is CsvImportState.Error -> "Dosya okunamadı"
    }
    val message = when (state) {
        CsvImportState.Idle -> ""
        CsvImportState.Parsing -> "CSV satırları ve koordinatlar kontrol ediliyor."
        is CsvImportState.Ready -> buildString {
            append("${state.validRows} geçerli baca bulundu.")
            if (state.skippedRows > 0) append(" ${state.skippedRows} geçersiz satır atlanacak.")
            append(" Tek işlemde Room veritabanına kaydedilsin mi?")
        }
        is CsvImportState.Saving -> "${state.rowCount} baca tek toplu işlemde kaydediliyor."
        is CsvImportState.Success -> "${state.rowCount} baca koordinat ve kot bilgileriyle kaydedildi."
        is CsvImportState.Error -> state.message
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = AccentOrange,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(Modifier.size(12.dp))
                }
                Text(message, color = TextSecondary, fontSize = 13.sp)
            }
        },
        confirmButton = {
            when (state) {
                is CsvImportState.Ready -> {
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                    ) { Text("Veritabanına Kaydet") }
                }
                is CsvImportState.Success, is CsvImportState.Error -> {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                    ) { Text("Tamam") }
                }
                else -> Unit
            }
        },
        dismissButton = {
            if (state is CsvImportState.Ready) {
                TextButton(onClick = onDismiss) { Text("Vazgeç") }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp)
    )
}

private fun distanceToSegment(point: Offset, start: Offset, end: Offset): Float {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val lengthSquared = dx * dx + dy * dy
    if (lengthSquared == 0f) return (point - start).getDistance()
    val projection = (((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared)
        .coerceIn(0f, 1f)
    val nearest = Offset(start.x + projection * dx, start.y + projection * dy)
    return hypot(
        (point.x - nearest.x).toDouble(),
        (point.y - nearest.y).toDouble()
    ).toFloat()
}

internal fun calculateCentroidLockedPan(
    currentPan: Offset,
    viewportCenter: Offset,
    gestureCentroid: Offset,
    panChange: Offset,
    appliedZoomChange: Float
): Offset {
    val centroidFromViewportCenter = gestureCentroid - viewportCenter
    return gestureCentroid + panChange - viewportCenter -
        (centroidFromViewportCenter - currentPan) * appliedZoomChange
}

@Composable
private fun SelectedElementCard(
    selection: MapSelection,
    manholes: List<ManholeEntity>,
    workOrder: WorkOrderWithPhotos?,
    hazeState: HazeState,
    onManholeStatusChange: (ManholeEntity, String) -> Unit,
    onDeleteManhole: (ManholeEntity) -> Unit,
    onOpenWorkOrder: (ManholeEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.10f)
            )
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    tint = HazeTint(Color.White.copy(alpha = 0.72f)),
                    blurRadius = 24.dp
                )
            )
            .background(Color.White.copy(alpha = 0.82f))
            .border(1.dp, Color.White.copy(alpha = 0.92f), shape)
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFEEE8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selection is MapSelection.Manhole) {
                            Icons.Default.Place
                        } else {
                            Icons.Default.List
                        },
                        contentDescription = null,
                        tint = AccentOrange,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = when (selection) {
                            is MapSelection.Manhole -> selection.manhole.name
                            is MapSelection.Pipeline -> {
                                val byId = manholes.associateBy { it.id }
                                val from = byId[selection.pipeline.fromManholeId]?.name ?: "?"
                                val to = byId[selection.pipeline.toManholeId]?.name ?: "?"
                                "$from → $to"
                            }
                        },
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (selection is MapSelection.Manhole) "BACA DETAYI" else "BORU HATTI DETAYI",
                        color = AccentOrange,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                if (selection is MapSelection.Manhole) {
                    IconButton(
                        onClick = { onDeleteManhole(selection.manhole) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Bacayı sil",
                            tint = ManholeRed,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 7.dp)) {
                    Text("Kapat", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(10.dp))
            when (selection) {
                is MapSelection.Manhole -> {
                    ManholeDetailGrid(selection.manhole)
                    Spacer(Modifier.height(10.dp))
                    ManholeStatusSelector(
                        selectedStatus = selection.manhole.status,
                        progressPaymentNumber = selection.manhole.progressPaymentNumber,
                        onStatusSelected = { status ->
                            onManholeStatusChange(selection.manhole, status)
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { onOpenWorkOrder(selection.manhole) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(39.dp),
                        shape = RoundedCornerShape(11.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = Color(0xDDF0F4F9),
                            contentColor = AccentOrange
                        )
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(
                            if (workOrder != null) "Emir Defterini Düzenle" else "Emir Defteri",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                is MapSelection.Pipeline -> PipelineDetailGrid(selection.pipeline, manholes)
            }
        }
    }
}

private data class ManholeStatusOption(
    val value: String,
    val label: String,
    val color: Color
)

@Composable
private fun ManholeStatusSelector(
    selectedStatus: String,
    progressPaymentNumber: Int?,
    onStatusSelected: (String) -> Unit
) {
    val options = listOf(
        ManholeStatusOption(ProjectDetailViewModel.MANHOLE_NOT_STARTED, "İmalat yapılamadı", ManholeGray),
        ManholeStatusOption(ProjectDetailViewModel.MANHOLE_COMPLETED, "İmalat yapıldı", ManholeYellow),
        ManholeStatusOption(
            ProjectDetailViewModel.MANHOLE_PROGRESS_PAYMENT,
            progressPaymentNumber?.let { "Hakedişe girdi • $it" } ?: "Hakedişe girdi",
            ManholeGreen
        ),
        ManholeStatusOption(ProjectDetailViewModel.MANHOLE_CANCELLED, "İptal edildi", ManholeRed)
    )

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = "BACA DURUMU",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        options.chunked(2).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                rowOptions.forEach { option ->
                    val selected = selectedStatus == option.value
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                if (selected) option.color.copy(alpha = 0.14f)
                                else Color(0xDDF0F4F9)
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) option.color.copy(alpha = 0.55f) else Color.Transparent,
                                shape = RoundedCornerShape(11.dp)
                            )
                            .clickable { onStatusSelected(option.value) }
                            .padding(horizontal = 9.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(option.color)
                        )
                        Spacer(Modifier.size(7.dp))
                        Text(
                            text = option.label,
                            color = if (selected) TextPrimary else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManholeDetailGrid(manhole: ManholeEntity) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            DetailValue("X", NumberParser.formatDecimal(manhole.x), Modifier.weight(1f))
            DetailValue("Y", NumberParser.formatDecimal(manhole.y), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            DetailValue("Proje Kapak Kotu", "${NumberParser.formatDecimal(manhole.projeKapakKotu)} m", Modifier.weight(1f))
            DetailValue("Arazi Siyah Kot", "${NumberParser.formatDecimal(manhole.araziSiyahKot)} m", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            DetailValue("Proje Akar Kot", "${NumberParser.formatDecimal(manhole.projeAkarKot)} m", Modifier.weight(1f))
            DetailValue(
                "Proje Siyah Kot Farkı",
                "${NumberParser.formatDecimal(manhole.projectTerrainDifference())} m",
                Modifier.weight(1f)
            )
        }
        manhole.progressPaymentNumber?.let { number ->
            DetailValue(
                "Hakediş",
                "$number. Hakediş",
                Modifier.fillMaxWidth(),
                accent = true
            )
        }
    }
}

@Composable
private fun PipelineDetailGrid(pipeline: PipelineEntity, manholes: List<ManholeEntity>) {
    val byId = manholes.associateBy { it.id }
    val from = byId[pipeline.fromManholeId]
    val to = byId[pipeline.toManholeId]
    val slope = if (from != null && to != null && pipeline.projeMesafe != 0.0) {
        (from.projeAkarKot - to.projeAkarKot) / pipeline.projeMesafe * 100.0
    } else {
        null
    }
    val status = when (pipeline.status) {
        ProjectDetailViewModel.STATUS_IN_PRODUCTION -> "İmalatta"
        ProjectDetailViewModel.STATUS_PROGRESS_PAYMENT -> "Hakedişte"
        else -> "Planlandı"
    }

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            DetailValue("Proje Mesafesi", "${NumberParser.formatDecimal(pipeline.projeMesafe)} m", Modifier.weight(1f))
            DetailValue("Proje Eğimi", slope?.let { "%${NumberParser.formatDecimal(it)}" } ?: "—", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            DetailValue("Başlangıç Akar", from?.let { "${NumberParser.formatDecimal(it.projeAkarKot)} m" } ?: "—", Modifier.weight(1f))
            DetailValue("Bitiş Akar", to?.let { "${NumberParser.formatDecimal(it.projeAkarKot)} m" } ?: "—", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            DetailValue("Başlangıç Kapak", from?.let { "${NumberParser.formatDecimal(it.projeKapakKotu)} m" } ?: "—", Modifier.weight(1f))
            DetailValue("Durum", status, Modifier.weight(1f), accent = true)
        }
    }
}

@Composable
private fun DetailValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0xDDF0F4F9))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = if (accent) AccentOrange else TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MapToolebar(
    mode: MapMode,
    canUndo: Boolean,
    onModeChange: (MapMode) -> Unit,
    onResetView: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(Color.White.copy(alpha = 0.96f))
            .border(1.dp, Color.White, RoundedCornerShape(17.dp))
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeButton("Gezin", mode == MapMode.NAVIGATE, Modifier.weight(1f)) {
            onModeChange(MapMode.NAVIGATE)
        }
        ModeButton("Başla", mode == MapMode.CONNECT, Modifier.weight(1f)) {
            onModeChange(MapMode.CONNECT)
        }
        IconButton(onClick = onResetView, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Refresh, "Görünümü sıfırla", tint = TextSecondary)
        }
        IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(40.dp)) {
            Text("↶", fontSize = 23.sp, color = if (canUndo) AccentOrange else Color(0xFFCAD1DA))
        }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) AccentOrange else Color.Transparent,
            contentColor = if (selected) Color.White else TextSecondary
        ),
        contentPadding = PaddingValues(horizontal = 6.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProgressPaymentNumberDialeog(
    manhole: ManholeEntity,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var numberInput by remember(manhole.id) {
        mutableStateOf(manhole.progressPaymentNumber?.toString().orEmpty())
    }
    val number = numberInput.toIntOrNull()?.takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kaçıncı hakediş?", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${manhole.name} bacasının dahile olduğu hakediş numarasını girin.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                CompactMapField(
                    value = numberInput,
                    onValueChange = { value ->
                        if (value.all(Char::isDigit)) numberInput = value
                    },
                    label = "Hakediş numarası",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(number!!) },
                enabled = number != null,
                colors = ButtonDefaults.buttonColors(containerColor = ManholeGreen)
            ) { Text("Hakedişe Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkOrderDialeog(
    manhole: ManholeEntity,
    workOrder: WorkOrderWithPhotos?,
    templates: List<String>,
    saving: Boolean,
    saveError: String?,
    onDismiss: () -> Unit,
    onSave: (Long?, String, String, List<String>) -> Unit,
    onDelete: (Long) -> Unit
) {
    val context = LocalContext.current
    val photoStore = remember(context) { WorkOrderPhotoStore(context.applicationContext) }
    val existingOrder = workOrder?.workOrder
    var selectedTitlee by rememberSaveable(manhole.id, existingOrder?.id) {
        mutableStateOf(existingOrder?.title.orEmpty())
    }
    var titleMenuExpanded by remember(manhole.id) { mutableStateOf(false) }
    var note by rememberSaveable(manhole.id, existingOrder?.id) { mutableStateOf(existingOrder?.note.orEmpty()) }
    var photoPaths by remember(manhole.id, existingOrder?.id) {
        mutableStateOf(workOrder?.orderedPhotos()?.map { it.path }.orEmpty())
    }
    val scope = rememberCoroutineScope()
    var staging by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            staging = true
            scope.launch {
                val copied = mutableListOf<String>()
                try {
                    withContext(Dispatchers.IO) {
                        uris.take((10 - photoPaths.size).coerceAtLeast(0)).forEach { copied += photoStore.stage(it, manhole.id) }
                    }
                    photoPaths = (photoPaths + copied).distinct()
                    photoError = null
                } catch (error: Exception) {
                    withContext(Dispatchers.IO) { photoStore.discardDrafts(copied) }
                    photoError = error.message ?: "Fotoğraflardan biri kaydedilemedi."
                } finally { staging = false }
            }
        }
    }
    val dismiss = {
        if (!saving && !staging) {
            photoStore.discardDrafts(photoPaths)
            onDismiss()
        }
    }
    if (confirmDelete) {
        AlertDialog(onDismissRequest = { confirmDelete = false },
            title = { Text("Emir kaydı silinsin mi?") },
            text = { Text("Bu emrin açıklaması ve fotoğrafları silinecek.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; existingOrder?.let { onDelete(it.id) } }) { Text("Sil") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") } })
    }
    val availeableteTitlees = (templates + selectedTitlee.takeIf(String::isNotBlank).orEmpty())
        .filter(String::isNotBlank)
        .distinct()
        .sortedWith(String.CASE_INSENSITIVE_ORDER)

    AlertDialog(
        onDismissRequest = dismiss,
        title = {
            Column {
                Text("Emir Defteri", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("${manhole.name} bacası", color = AccentOrange, fontSize = 11.sp)
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("BAŞLIK ŞABLONU", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                if (availeableteTitlees.isEmpty()) {
                    OutlinedTextField(value = selectedTitlee, onValueChange = { selectedTitlee = it },
                        label = { Text("Emir başlığı") }, modifier = Modifier.fillMaxWidth(), enabled = !saving)
                } else {
                    ExposedDropdownMenuBox(
                        expanded = titleMenuExpanded,
                        onExpandedChange = { if (!saving && !staging) titleMenuExpanded = !titleMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedTitlee,
                            onValueChange = { selectedTitlee = it },
                            readOnly = false,
                            enabled = !saving && !staging,
                            label = { Text("Emir defteri başlığı") },
                            placeholder = { Text("Listeden başlık seçin") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = titleMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(
                                    ExposedDropdownMenuAnchorType.PrimaryEditable,
                                    enabled = !saving && !staging
                                )
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(13.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentOrange,
                                focusedLabelColor = AccentOrange,
                                unfocusedContainerColor = Color(0xFFF0F4F9)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = titleMenuExpanded,
                            onDismissRequest = { titleMenuExpanded = false },
                            modifier = Modifier.heightIn(max = 280.dp)
                        ) {
                            availeableteTitlees.forEach { title ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            title,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedTitlee == title) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    },
                                    onClick = {
                                        selectedTitlee = title
                                        titleMenuExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    enabled = !saving && !staging,
                    label = { Text("Açıklama (isteğe bağlı)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(13.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange,
                        focusedLabelColor = AccentOrange,
                        unfocusedContainerColor = Color(0xFFF0F4F9)
                    )
                )
                if (photoPaths.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(photoPaths, key = { it }) { path ->
                            Box {
                                val bitmap = com.example.egimhesabi.util.rememberPhotoBitmap(path)
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = "Emir defteri fotoğrafı",
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(11.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(ManholeRed)
                                        .clickable(enabled = !saving && !staging) {
                                            photoStore.discardDrafts(listOf(path))
                                            photoPaths = photoPaths - path
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("×", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = {
                        photoPicker.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    enabled = photoPaths.size < 10 && !saving && !staging,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (photoPaths.isEmpty()) "Fotoğraf Ekle" else "Fotoğraf Ekle (${photoPaths.size}/10)",
                        fontSize = 11.sp
                    )
                }
                existingOrder?.let { Text("Hakediş: ${it.progressPaymentNumber?.toString() ?: "Bekleyen"}", color = TextSecondary) }
                saveError?.let { Text(it, color = ManholeRed) }
                if (staging) Text("Fotoğraflar hazırlanıyor…")
                photoError?.let { Text(it, color = ManholeRed, fontSize = 10.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(existingOrder?.id, selectedTitlee, note, photoPaths) },
                enabled = selectedTitlee.isNotBlank() && !saving && !staging,
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) { Text(if (saving) "Kaydediliyor…" else "Kaydet") }
        },
        dismissButton = {
            Row {
                existingOrder?.let { order ->
                    TextButton(enabled = !saving && !staging, onClick = { confirmDelete = true }) {
                        Text("Kaydı Temizle", color = ManholeRed, fontSize = 10.sp)
                    }
                }
                TextButton(
                    enabled = !saving && !staging,
                    onClick = dismiss
                ) { Text("Vazgeç") }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun AddManholeDialeog(
    suggestedName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf(suggestedName) }
    var x by remember { mutableStateOf("") }
    var y by remember { mutableStateOf("") }
    val parsedX = NumberParser.parseDecimal(x)
    val parsedY = NumberParser.parseDecimal(y)
    val canSubmit = name.isNotBlank() && parsedX != null && parsedY != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Baca Ekle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactMapField(name, { name = it }, "Baca adı")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactMapField(x, { x = it }, "X", Modifier.weight(1f))
                    CompactMapField(y, { y = it }, "Y", Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, parsedX!!, parsedY!!) },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun CompactMapField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentOrange,
            focusedLabelColor = AccentOrange,
            unfocusedContainerColor = Color(0xFFF0F4F9),
            focusedContainerColor = Color(0xFFF0F4F9),
            unfocusedBorderColor = Color.Transparent
        )
    )
}






