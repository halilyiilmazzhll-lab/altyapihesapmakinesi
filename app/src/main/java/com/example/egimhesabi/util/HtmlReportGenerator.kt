package com.example.egimhesabi.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.egimhesabi.viewmodel.OrderBookItem
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HtmlReportGenerator {
    fun generateHtml(projectName: String, tabName: String, items: List<OrderBookItem>): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("tr"))
        val grouped = items.groupBy { it.workOrderWithPhotos.workOrder.title }
        
        val htmlBuilder = StringBuilder()
        htmlBuilder.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: sans-serif; padding: 20px; color: #333; }
                    h1 { text-align: center; color: #333; font-size: 24px; }
                    h2 { color: #E65100; border-bottom: 2px solid #E65100; padding-bottom: 5px; margin-top: 30px; font-size: 20px; }
                    .manhole-card { margin-bottom: 20px; border: 1px solid #ccc; padding: 15px; border-radius: 8px; page-break-inside: avoid; background-color: #fafafa; }
                    .manhole-title { font-weight: bold; font-size: 16px; margin-bottom: 5px; color: #333; }
                    .manhole-date { font-size: 12px; color: #777; margin-bottom: 10px; }
                    .manhole-note { margin-bottom: 15px; font-size: 14px; white-space: pre-wrap; }
                    .photo-grid { display: flex; flex-wrap: wrap; gap: 10px; }
                    .photo-img { max-width: 250px; max-height: 250px; object-fit: cover; border-radius: 4px; border: 1px solid #ddd; }
                </style>
            </head>
            <body>
                <h1>Emir Defteri Raporu</h1>
                <div style="text-align: center; color: #555; margin-bottom: 30px;">
                    Proje: <strong>${escapeHtml(projectName)}</strong> | <strong>${escapeHtml(tabName)}</strong>
                </div>
        """.trimIndent())

        for ((title, itemsInGroup) in grouped) {
            htmlBuilder.append("<h2>${escapeHtml(title)}</h2>\n")
            
            for (item in itemsInGroup) {
                val workOrder = item.workOrderWithPhotos.workOrder
                val photos = item.workOrderWithPhotos.photos
                
                htmlBuilder.append("""
                    <div class="manhole-card">
                        <div class="manhole-title">Baca: ${escapeHtml(item.manholeName)}</div>
                        <div class="manhole-date">Tarih: ${dateFormat.format(Date(workOrder.createdAt))}</div>
                """.trimIndent())
                
                if (workOrder.note.isNotBlank()) {
                    htmlBuilder.append("""<div class="manhole-note">${escapeHtml(workOrder.note)}</div>""")
                }
                
                if (photos.isNotEmpty()) {
                    htmlBuilder.append("""<div class="photo-grid">""")
                    for (photo in photos) {
                        val base64 = encodeImageToBase64(photo.path)
                        if (base64 != null) {
                            htmlBuilder.append("""<img class="photo-img" src="data:image/jpeg;base64,$base64" />""")
                        }
                    }
                    htmlBuilder.append("</div>\n")
                }
                
                htmlBuilder.append("</div>\n") // end manhole-card
            }
        }

        htmlBuilder.append("""
            </body>
            </html>
        """.trimIndent())
        
        return htmlBuilder.toString()
    }

    internal fun escapeHtml(value: String): String = value.replace("&", "&amp;")
        .replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")

    private fun encodeImageToBase64(path: String): String? {
        return try {
            val bitmap = SafeImages.load(path, 1200) ?: return null
            val outputStream = ByteArrayOutputStream()
            // Compress to JPEG with 70% quality to keep HTML size reasonable.
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}

