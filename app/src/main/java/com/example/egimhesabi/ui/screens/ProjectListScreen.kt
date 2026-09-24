package com.example.egimhesabi.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.egimhesabi.data.ManholeEntity
import com.example.egimhesabi.data.PipelineEntity
import com.example.egimhesabi.data.WorkOrderWithPhotos
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.SlopeOk
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.util.NumberParser
import com.example.egimhesabi.viewmodel.ProjectDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    projectName: String,
    viewModel: ProjectDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val manholes by viewModel.manholes.collectAsState()
    val project by viewModel.project.collectAsState()
    val pipelines by viewModel.pipelines.collectAsState()
    val workOrdersByManholeId by viewModel.workOrdersByManholeId.collectAsState()
    val manholesById = manholes.associateBy { it.id }
    var pendingProgressPaymentPipelineId by remember { mutableStateOf<Long?>(null) }

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
                        Text("Boru hatları", color = TextSecondary, fontSize = 10.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF5F5FA))
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "İMALAT VE HAKEDİŞ",
                        color = AccentOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sağa: imalata ekle  •  Sola: hakedişe gönder",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            if (pipelines.isEmpty()) {
                item { EmptyPipelineCard() }
            } else {
                items(pipelines, key = { it.id }) { pipeline ->
                    PipelineSwipeRow(
                        pipeline = pipeline,
                        from = manholesById[pipeline.fromManholeId],
                        to = manholesById[pipeline.toManholeId],
                        fromWorkOrder = workOrdersByManholeId[pipeline.fromManholeId],
                        toWorkOrder = workOrdersByManholeId[pipeline.toManholeId],
                        project = project,
                        onProduction = { viewModel.moveToProduction(pipeline.id) },
                        onProgressPayment = { pendingProgressPaymentPipelineId = pipeline.id }
                    )
                }
            }
        }
    }

    pendingProgressPaymentPipelineId?.let { pipelineId ->
        PipelineProgressPaymentDialeog(
            onDismiss = { pendingProgressPaymentPipelineId = null },
            onConfirm = { number ->
                viewModel.moveToProgressPayment(pipelineId, number)
                pendingProgressPaymentPipelineId = null
            }
        )
    }
}

@Composable
private fun EmptyPipelineCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(Color(0xFFFFEEE8), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Build, null, tint = AccentOrange)
        }
        Spacer(Modifier.height(10.dp))
        Text("Henüz boru hattı yok", color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Text("Kroki ekranında bacaları birbirine başlayın.", color = TextSecondary, fontSize = 11.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PipelineSwipeRow(
    pipeline: PipelineEntity,
    from: ManholeEntity?,
    to: ManholeEntity?,
    fromWorkOrder: WorkOrderWithPhotos?,
    toWorkOrder: WorkOrderWithPhotos?,
    project: com.example.egimhesabi.data.ProjectEntity?,
    onProduction: () -> Unit,
    onProgressPayment: () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onProduction()
                SwipeToDismissBoxValue.EndToStart -> onProgressPayment()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f }
    )

    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val isProduction = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isProduction) AccentOrange else SlopeOk)
                    .padding(horizontal = 18.dp),
                horizontalArrangement = if (isProduction) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isProduction) {
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White)
                    Text("İmalata Ekle", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Hakedişe Gönder", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.KeyboardArrowLeft, null, tint = Color.White)
                }
            }
        }
    ) {
        PipelineCard(pipeline, from, to, fromWorkOrder, toWorkOrder, project)
    }
}

@Composable
private fun PipelineProgressPaymentDialeog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var value by remember { mutableStateOf("") }
    val number = value.toIntOrNull()?.takeIf { it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hakediş numarası") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.filter(Char::isDigit) },
                label = { Text("Kaçıncı hakediş?") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(number!!) },
                enabled = number != null,
                colors = ButtonDefaults.buttonColors(containerColor = SlopeOk)
            ) { Text("Hakedişe gönder") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
private fun PipelineCard(
    pipeline: PipelineEntity,
    from: ManholeEntity?,
    to: ManholeEntity?,
    fromWorkOrder: WorkOrderWithPhotos?,
    toWorkOrder: WorkOrderWithPhotos?,
    project: com.example.egimhesabi.data.ProjectEntity?
) {
    val statusColor = when (pipeline.status) {
        ProjectDetailViewModel.STATUS_IN_PRODUCTION -> AccentOrange
        ProjectDetailViewModel.STATUS_PROGRESS_PAYMENT -> SlopeOk
        else -> TextSecondary
    }
    val statusLabele = when (pipeline.status) {
        ProjectDetailViewModel.STATUS_IN_PRODUCTION -> "İmalatta"
        ProjectDetailViewModel.STATUS_PROGRESS_PAYMENT -> "Hakedişte"
        else -> "Planlandı"
    }
    val netDepth = calculateNetDepth(from, to)
    val workOrderManholes = listOfNotNull(
        from?.let { it to fromWorkOrder },
        to?.let { it to toWorkOrder }
    ).filter { (_, workOrder) -> workOrder != null }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFFF0F4F9), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Build, null, tint = AccentOrange, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.size(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${from?.name ?: "?"}  →  ${to?.name ?: "?"}",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${NumberParser.formatDecimal(pipeline.projeMesafe)} m proje hattı",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = statusLabele,
                color = statusColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.10f))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            )
        }

        workOrderManholes.forEach { (manhole, order) ->
            val workOrder = requireNotNull(order)
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFFEFF3FF))
                    .padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Emir Defteri • ${manhole.name}",
                    color = Color(0xFF5B7CFA),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                if (workOrder.workOrder.title.isNotBlank()) {
                    Text(
                        text = workOrder.workOrder.title,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (workOrder.workOrder.note.isNotBlank()) {
                    Text(
                        text = workOrder.workOrder.note,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val photos = workOrder.orderedPhotos()
                if (photos.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(photos, key = { it.id }) { photo ->
                            com.example.egimhesabi.util.rememberPhotoBitmap(photo.path)?.let { bitmap ->
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "${manhole.name} emir defteri fotoğrafı",
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(9.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }

        if (pipeline.status != ProjectDetailViewModel.STATUS_PLANNED) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFFF0F4F9))
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Net kazı derinliği", color = TextSecondary, fontSize = 10.sp)
                Text(
                    text = netDepth?.let { "${NumberParser.formatDecimal(it)} m" } ?: "İmalat kotu bekleniyor",
                    color = if (netDepth != null) TextPrimary else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun calculateNetDepth(from: ManholeEntity?, to: ManholeEntity?): Double? {
    val depths = listOfNotNull(
        from?.let { manhole ->
            val cover = manhole.imalatKapakKotu
            val invert = manhole.imalatAkarKotu
            if (cover != null && invert != null) cover - invert else null
        },
        to?.let { manhole ->
            val cover = manhole.imalatKapakKotu
            val invert = manhole.imalatAkarKotu
            if (cover != null && invert != null) cover - invert else null
        }
    )
    return depths.takeIf { it.isNotEmpty() }?.average()
}







