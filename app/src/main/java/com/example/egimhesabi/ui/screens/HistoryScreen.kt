package com.example.egimhesabi.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.egimhesabi.R
import com.example.egimhesabi.data.HistoryEntry
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.GradientStart
import com.example.egimhesabi.theme.SlopeDanger
import com.example.egimhesabi.theme.SlopeOk
import com.example.egimhesabi.theme.SlopeWarning
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.ui.components.GlassCard
import com.example.egimhesabi.util.HistoryPdfExporter
import com.example.egimhesabi.util.NumberParser
import com.example.egimhesabi.viewmodel.HistoryViewModel
import com.example.egimhesabi.domain.CalculationResult
import com.example.egimhesabi.viewmodel.SlopeUiState
import com.example.egimhesabi.domain.SlopeStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text(
                            text = "${selectedIds.size} Seçildi",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Geçmiş İşlemler",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { 
                            isSelectionMode = false 
                            selectedIds.clear()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "İptal", tint = TextPrimary)
                        }
                    } else {
                        IconButton(onClick = onClose) {
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
                            com.example.egimhesabi.ui.components.ConfirmClearAction(
                                title = "Tüm geçmiş silinsin mi?",
                                message = "Kaydedilmiş tüm eğim hesapları silinecek. Bu işlem geri alınamaz.",
                                onConfirm = viewModel::clearAll
                            )
                        } else {
                            IconButton(
                                enabled = !isExporting && selectedIds.isNotEmpty(),
                                onClick = {
                                    val entriesToExport = history.filter { it.id in selectedIds }
                                    isExporting = true
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            runCatching {
                                                HistoryPdfExporter.exportToCache(context, entriesToExport)
                                            }
                                        }
                                        isExporting = false
                                        if (result.isSuccess) {
                                            val file = result.getOrThrow()
                                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                                context,
                                                context.packageName + ".provider",
                                                file
                                            )
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(intent, "PDF Paylaş"))
                                            isSelectionMode = false
                                            selectedIds.clear()
                                        } else {
                                            Toast.makeText(context, "PDF oluşturulamadı.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                if (isExporting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = AccentOrange
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Paylaş",
                                        tint = if (selectedIds.isNotEmpty()) TextPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GradientStart
                )
            )
        },
        containerColor = GradientStart,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        modifier = Modifier.padding(32.dp),
                        cornerRadius = 24.dp,
                        backgroundAlpha = 0.5f
                    ) {
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
                                text = "Henüz işlem yok",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Yaptığınız hesaplamalar burada görünecek",
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
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(history, key = { it.id }) { entry ->
                        val isSelected = selectedIds.contains(entry.id)
                        HistoryCard(
                            entry = entry,
                            onDelete = { viewModel.deleteEntry(entry) },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryCard(
    entry: HistoryEntry,
    onDelete: () -> Unit,
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

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SlopeDanger, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.White)
            }
        },
        modifier = modifier
    ) {
        val statusColor = when (entry.slopeStatus) {
            "OK" -> SlopeOk
            "NEAR_LIMIT" -> SlopeWarning
            "TOO_LOW", "TOO_HIGH" -> SlopeDanger
            else -> TextSecondary
        }

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(entry.timestamp))

        var isExpanded by remember { mutableStateOf(false) }

        val bgAlpha = 1f
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { 
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        isExpanded = !isExpanded 
                    }
                },
            cornerRadius = 16.dp,
            backgroundAlpha = bgAlpha,
            contentPadding = 14.dp,
            elevation = if (isSelected) 4.dp else 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    val name1 = if (entry.baca1Name.isBlank() || entry.baca1Name.equals("BACA 1", ignoreCase = true) || entry.baca1Name == "B1") "1. Baca" else entry.baca1Name
                    val name2 = if (entry.baca2Name.isBlank() || entry.baca2Name.equals("BACA 2", ignoreCase = true) || entry.baca2Name == "B2") "2. Baca" else entry.baca2Name
                    
                    Text(
                        text = "$name1 â†’ $name2",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "% ${NumberParser.formatDecimal(entry.slopePercent)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = statusColor
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    if (entry.slopeRatio > 0) {
                        Text(
                            text = "1/${NumberParser.formatCompact(entry.slopeRatio)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = statusColor
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Î”h = ${NumberParser.formatDecimal(entry.heightDiff)} m",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentOrange,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "L = ${NumberParser.formatDecimal(entry.mesafe)} m",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                }
                
                if (isExpanded && !isSelectionMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val calculation = CalculationResult(
                        b1Kapak = entry.baca1KapakKotu,
                        b1Akar = entry.baca1AkarKotu,
                        b2Kapak = entry.baca2KapakKotu,
                        b2Akar = entry.baca2AkarKotu,
                        mesafe = entry.mesafe,
                        slopePercent = entry.slopePercent,
                        slopeRatio = entry.slopeRatio,
                        slopeStatus = try { SlopeStatus.valueOf(entry.slopeStatus) } catch (e: Exception) { SlopeStatus.UNKNOWN },
                        slopeDirection = entry.slopeDirection,
                        heightDiff = entry.heightDiff
                    )
                    val diagramState = SlopeUiState(
                        baca1Name = entry.baca1Name,
                        baca2Name = entry.baca2Name,
                        calculation = calculation
                    )
                    
                    com.example.egimhesabi.ui.components.SlopeDiagram(
                        state = diagramState,
                        statusColor = statusColor
                    )
                }
            }
        }
    }
}


