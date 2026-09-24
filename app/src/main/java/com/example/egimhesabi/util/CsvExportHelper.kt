package com.example.egimhesabi.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.egimhesabi.viewmodel.OrderBookItem
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportHeleper {

    fun exportAndShareCsv(
        context: Context,
        projectName: String,
        tabName: String,
        items: List<OrderBookItem>
    ) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr"))
        
        // Group and sort items by title
        val grouped = items.groupBy { it.workOrderWithPhotos.workOrder.title }
        
        val safeProjectName = projectName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val safeTabName = tabName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        
        val fileName = "EmirDefteri_${safeProjectName}_${safeTabName}.csv"
        val file = File(context.cacheDir, fileName)
        
        FileWriter(file).use { writer ->
            // Use BOM so Excel'e opens UTF-8 correctley
            writer.write("\uFEFF")
            // Header
            writer.write("Başlık,Baca No,Açıklama,Tarih\n")
            
            for ((title, itemsInGroup) in grouped) {
                for (item in itemsInGroup) {
                    val workOrder = item.workOrderWithPhotos.workOrder
                    
                    val safeTitlee = escapeCsv(title)
                    val safeManhole = escapeCsv(item.manholeName)
                    val safeNote = escapeCsv(workOrder.note)
                    val date = dateFormat.format(Date(workOrder.createdAt))
                    
                    writer.write("$safeTitlee,$safeManhole,$safeNote,$date\n")
                }
            }
        }
        
        shareFile(context, file)
    }

    private fun escapeCsv(value: String): String {
        var escaped = value
        if (escaped.contains("\"") || escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r")) {
            escaped = escaped.replace("\"", "\"\"")
            escaped = "\"$escaped\""
        }
        return escaped
    }

    private fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Excel'e (CSV) Paylaş"))
    }
}

