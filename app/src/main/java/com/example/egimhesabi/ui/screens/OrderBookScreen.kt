package com.example.egimhesabi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import com.example.egimhesabi.util.ExcelExportHelper
import com.example.egimhesabi.util.HtmlReportGenerator
import com.example.egimhesabi.util.PrintHelper
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.GradientEnd
import com.example.egimhesabi.theme.GradientMid
import com.example.egimhesabi.theme.GradientStart
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.ui.components.WorkOrderItemCard
import com.example.egimhesabi.viewmodel.OrderBookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderBookScreen(
    projectName: String,
    viewModel: OrderBookViewModel,
    onBack: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val distinctPayments by viewModel.distinctProgressPayments.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val workOrders by viewModel.workOrders.collectAsStateWithLifecycle()

    val tabs = mutableListOf<Int?>()
    tabs.add(null) // Bekleyenler
    tabs.addAll(distinctPayments)

    val exportScope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    exportError?.let { message ->
        androidx.compose.material3.AlertDialog(onDismissRequest = { exportError = null },
            title = { Text("Dışa aktarma tamamlanamadı") }, text = { Text(message) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { exportError = null }) { Text("Tamam") } })
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientMid, GradientEnd)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Emir Defteri",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = projectName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Geri",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    val context = LocalContext.current
                    
                    IconButton(enabled = !exporting, onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Dışa Aktar",
                            tint = TextPrimary
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("PDF Yazdır/Kaydet") },
                            onClick = {
                                showMenu = false
                                val tabTitle = if (selectedTab == null) "Bekleyenler" else "$selectedTab. Hakediş"
                                exporting = true
                                exportScope.launch {
                                    try {
                                        val html = withContext(Dispatchers.IO) { HtmlReportGenerator.generateHtml(projectName, tabTitle, workOrders) }
                                        PrintHelper.printHtml(context, html, "Emir Defteri")
                                    } catch (error: Exception) { exportError = error.message ?: "Rapor oluşturulamadı." }
                                    finally { exporting = false }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Excel'e (XLSX) Olarak Paylaş") },
                            onClick = {
                                showMenu = false
                                val tabTitle = if (selectedTab == null) "Bekleyenler" else "$selectedTab. Hakediş"
                                exporting = true
                                exportScope.launch {
                                    try {
                                        val file = withContext(Dispatchers.IO) { ExcelExportHelper.createExcel(context, projectName, tabTitle, workOrders) }
                                        ExcelExportHelper.shareFile(context, file)
                                    } catch (error: Exception) { exportError = error.message ?: "Excel dosyası oluşturulamadı." }
                                    finally { exporting = false }
                                }
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            if (exporting) Text("Dosya hazırlanıyor…", modifier = Modifier.padding(16.dp))

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = if (selectedTab == null) 0 else distinctPayments.indexOf(selectedTab) + 1,
                containerColor = Color.Transparent,
                contentColor = AccentOrange,
                edgePadding = 16.dp
            ) {
                Tab(
                    selected = selectedTab == null,
                    onClick = { viewModel.selectTab(null) },
                    text = { Text("Bekleyenler") }
                )
                distinctPayments.forEach { paymentNumber ->
                    Tab(
                        selected = selectedTab == paymentNumber,
                        onClick = { viewModel.selectTab(paymentNumber) },
                        text = { Text("$paymentNumber. Hakediş") }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                if (workOrders.isEmpty()) {
                    item {
                        Text(
                            text = "Bu sekme için emir kaydı bulunamadı.",
                            modifier = Modifier.padding(16.dp),
                            color = TextPrimary.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    items(workOrders, key = { it.workOrderWithPhotos.workOrder.id }) { item ->
                        WorkOrderItemCard(
                            workOrderWithPhotos = item.workOrderWithPhotos,
                            manholeName = item.manholeName,
                            onOpenPhoto = onOpenPhoto,
                            onDelete = null
                        )
                    }
                }
            }
        }
    }
}



