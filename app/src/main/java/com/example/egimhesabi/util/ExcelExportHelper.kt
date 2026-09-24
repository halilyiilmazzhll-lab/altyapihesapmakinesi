package com.example.egimhesabi.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.egimhesabi.viewmodel.OrderBookItem
import org.dhatim.fastexcel.Workbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExportHelper {

    fun createExcel(
        context: Context,
        projectName: String,
        tabName: String,
        items: List<OrderBookItem>
    ): File {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr"))
        val grouped = items.groupBy { it.workOrderWithPhotos.workOrder.title }
        
        val safeProjectName = projectName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val safeTabName = tabName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "EmirDefteri_${safeProjectName}_${safeTabName}.xlsx"
        val file = File(context.cacheDir, fileName)
        
        FileOutputStream(file).use { os ->
            val wb = Workbook(os, "EgimHesabi", "1.0")
            
            val usedNames = mutableSetOf<String>()
            for ((title, itemsInGroup) in grouped) {
                val sheetName = uniqueSheetName(title, usedNames)
                val ws = wb.newWorksheet(sheetName)
                
                // Headers
                ws.value(0, 0, "Baca No")
                ws.value(0, 1, "Açıklama")
                ws.value(0, 2, "Tarih")
                
                ws.style(0, 0).bold().set()
                ws.style(0, 1).bold().set()
                ws.style(0, 2).bold().set()
                
                ws.width(0, 20.0)
                ws.width(1, 50.0)
                ws.width(2, 25.0)
                
                var row = 1
                for (item in itemsInGroup) {
                    val workOrder = item.workOrderWithPhotos.workOrder
                    
                    ws.value(row, 0, item.manholeName)
                    ws.value(row, 1, workOrder.note)
                    ws.value(row, 2, dateFormat.format(Date(workOrder.createdAt)))
                    
                    row++
                }
            }
            
            if (grouped.isEmpty()) {
                val ws = wb.newWorksheet("Boş")
                ws.value(0, 0, "Kayıt bulunamadı.")
            }
            
            wb.finish()
        }
        
        return file
    }

    internal fun uniqueSheetName(title: String, used: MutableSet<String>): String {
        val base = title.map { if (it in "\\/?*[]:" || it.code < 32) '_' else it }.joinToString("")
            .trim().trim('\'').take(31).trim('\'').ifBlank { "Genel" }
        var name = base
        var counter = 2
        while (!used.add(name.lowercase(Locale.ROOT))) {
            val suffix = " (${counter++})"
            name = base.take(31 - suffix.length) + suffix
        }
        return name
    }

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            // Updated MIME type to XLSX
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Excel'e Paylaş"))
    }
}

