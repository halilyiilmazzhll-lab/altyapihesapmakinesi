package com.example.egimhesabi.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.egimhesabi.data.ImpactHistoryEntry
import com.example.egimhesabi.viewmodel.ManholeNode
import com.example.egimhesabi.viewmodel.ManholeNodeData
import com.example.egimhesabi.viewmodel.ImpactCalculationState
import com.example.egimhesabi.viewmodel.ImpactSlopeStatus
import com.example.egimhesabi.util.NumberParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

internal fun impactPdfManholeTitle(index: Int, node: ManholeNode): String {
    val numberedTitle = node.name.trim().takeIf { it.isNotEmpty() }
        ?.let { "${index + 1}. Baca ($it)" }
        ?: "${index + 1}. Baca"
    return if (index == 0) "$numberedTitle (Sabit)" else numberedTitle
}

internal fun impactPdfSegmentLabel(
    index: Int,
    nodes: List<ManholeNode>,
    flowLeftToRight: Boolean?
): String {
    val firstName = nodes.getOrNull(index)?.name?.trim()
        ?.takeIf { it.isNotEmpty() } ?: "${index + 1}"
    val secondName = nodes.getOrNull(index + 1)?.name?.trim()
        ?.takeIf { it.isNotEmpty() } ?: "${index + 2}"
    return when (flowLeftToRight) {
        true -> "Hat $firstName\u2192$secondName"
        false -> "Hat $secondName\u2192$firstName"
        null -> "Hat $firstName\u2014$secondName"
    }
}

object ImpactHistoryPdfExporter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val PAGE_MARGIN = 32f

    private val turkishLocale = Locale.forLanguageTag("tr-TR")
    private val titleTypeface = Typeface.create("sans-serif", Typeface.BOLD)
    private val mediumTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val regularTypeface = Typeface.create("sans-serif", Typeface.NORMAL)
    private val monoTypeface = Typeface.create("monospace", Typeface.NORMAL)

    fun suggestedFileName(now: Long = System.currentTimeMillis()): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", turkishLocale).format(Date(now))
        return "Etki_Hesabi_Gecmis_$stamp.pdf"
    }

    fun exportToCache(context: Context, entries: List<ImpactHistoryEntry>): java.io.File {
        val document = PdfDocument()
        val file = java.io.File(context.cacheDir, suggestedFileName())
        try {
            var pageNumber = 1
            var yOffset = PAGE_MARGIN + 100f

            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            drawHeader(page.canvas, entries.size, pageNumber)

            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

            for ((index, entry) in entries.withIndex()) {
                val nodes: List<ManholeNodeData> = try {
                    json.decodeFromString(entry.nodesJson)
                } catch (e: Exception) { emptyList() }
                
                val manholes = nodes.map { ManholeNode.fromData(it) }
                val state = ImpactCalculationState(
                    nodes = manholes,
                    minSlopePercent = entry.minSlopePercent,
                    maxSlopePercent = entry.maxSlopePercent,
                    minManholeDepthMeters = entry.minManholeDepthMeters
                )

                val manholeCount = manholes.size
                val segmentCount = max(0, manholeCount - 1)
                val requiredHeight = 80f + (manholeCount * 25f) + (segmentCount * 25f) + 40f

                if (yOffset + requiredHeight > PAGE_HEIGHT - PAGE_MARGIN) {
                    drawFooter(page.canvas, pageNumber)
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvasReset(page.canvas)
                    yOffset = PAGE_MARGIN + 40f
                }

                drawEntry(page.canvas, entry, manholes, state, yOffset)
                yOffset += requiredHeight + 20f
            }

            drawFooter(page.canvas, pageNumber)
            document.finishPage(page)

            java.io.FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
        } catch (e: Exception) {
            try { document.close() } catch (ignored: Exception) {}
            throw e
        }
        
        document.close()
        return file
    }
    
    private fun canvasReset(canvas: Canvas) {
        canvas.drawColor(PAGE_BACKGROUND)
    }

    private fun drawHeader(canvas: Canvas, totalEntries: Int, pageNumber: Int) {
        canvasReset(canvas)
        drawText(canvas, "ETKİ HESABI", PAGE_MARGIN, 36f, 9f, ACCENT, titleTypeface, letterSpacing = 0.8f)
        drawText(canvas, "İşlem Geçmişi", PAGE_MARGIN, 61f, 21f, TEXT, titleTypeface)
        drawText(canvas, "$totalEntries kayıt · tüm ayrıntılar", PAGE_MARGIN, 78f, 9f, TEXT_MUTED, regularTypeface)
        val generatedAt = SimpleDateFormat("dd.MM.yyyy HH:mm", turkishLocale).format(Date())
        drawText(canvas, generatedAt, PAGE_WIDTH - PAGE_MARGIN, 61f, 9f, TEXT_MUTED, regularTypeface, Paint.Align.RIGHT)
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int) {
        drawText(canvas, "Sayfa $pageNumber", PAGE_WIDTH / 2f, PAGE_HEIGHT - 14f, 8f, TEXT_MUTED, regularTypeface, Paint.Align.CENTER)
    }

    private fun drawEntry(canvas: Canvas, entry: ImpactHistoryEntry, nodes: List<ManholeNode>, state: ImpactCalculationState, top: Float) {
        val left = PAGE_MARGIN
        val right = PAGE_WIDTH - PAGE_MARGIN
        
        val manholeCount = nodes.size
        val segmentCount = max(0, manholeCount - 1)
        val height = 80f + (manholeCount * 25f) + (segmentCount * 25f)
        
        val card = RectF(left, top, right, top + height)
        canvas.drawRoundRect(card, 12f, 12f, fillPaint(Color.WHITE))
        canvas.drawRoundRect(card, 12f, 12f, strokePaint(CARD_BORDER, 1f))

        val overallColor = if (entry.allValid) OK_COLOR else WARNING_COLOR
        
        canvas.drawCircle(left + 16f, top + 20f, 4f, fillPaint(overallColor))
        drawText(canvas, "${nodes.size} Bacalı Profil", left + 27f, top + 24f, 11f, TEXT, mediumTypeface)
        
        val date = SimpleDateFormat("dd.MM.yyyy HH:mm", turkishLocale).format(Date(entry.timestamp))
        drawText(canvas, date, right - 15f, top + 24f, 8f, TEXT_MUTED, regularTypeface, Paint.Align.RIGHT)

        drawText(canvas, "Min eğim: %${format(entry.minSlopePercent)}   Max eğim: %${format(entry.maxSlopePercent)}", left + 16f, top + 42f, 8f, TEXT_MUTED, monoTypeface)
        drawText(canvas, if (entry.allValid) "Tüm hatlar dengede" else "Hatalı hatlar var", right - 15f, top + 42f, 9f, overallColor, mediumTypeface, Paint.Align.RIGHT)

        var y = top + 70f
        drawText(canvas, "BACA BİLGİLERİ", left + 16f, y, 7f, ACCENT, titleTypeface)
        y += 15f
        
        for ((i, node) in nodes.withIndex()) {
            val title = impactPdfManholeTitle(i, node)
            drawText(canvas, title, left + 16f, y, 9f, TEXT, mediumTypeface)
            
            val kapak = node.cover?.let { "Kapak: ${format(it)}" } ?: ""
            val akar = node.invert?.let { "Akar: ${format(it)}" } ?: ""
            drawText(canvas, "$kapak   $akar", left + 165f, y, 9f, TEXT_MUTED, monoTypeface)
            
            val depth = node.depth?.let { "H: ${format(it)} m" } ?: ""
            val delta = if (node.deltaCm != 0.0) {
                val sign = if (node.deltaCm > 0) "+" else ""
                "  Δ: $sign${format(node.deltaCm, 0)} cm"
            } else ""
            
            drawText(canvas, "$depth$delta", right - 16f, y, 9f, TEXT_MUTED, monoTypeface, Paint.Align.RIGHT)
            y += 20f
        }
        
        y += 10f
        if (segmentCount > 0) {
            drawText(canvas, "HAT ANALİZLERİ", left + 16f, y, 7f, ACCENT, titleTypeface)
            y += 15f
            
            for (i in 0 until segmentCount) {
                val segment = state.getSegmentAnalysis(i)
                val sColor = statusColor(segment.status)
                
                canvas.drawCircle(left + 16f, y - 3f, 3f, fillPaint(sColor))
                val segmentLabel = impactPdfSegmentLabel(i, nodes, state.flowLeftToRight)
                drawText(canvas, segmentLabel, left + 27f, y, 9f, TEXT, mediumTypeface)
                
                val pct = segment.slopePercent?.let { "%${format(it)}" } ?: "-"
                drawText(canvas, pct, left + 180f, y, 9f, sColor, monoTypeface)
                
                val ratio = segment.slopeRatio?.let { "1/${format(it, 0)}" } ?: ""
                drawText(canvas, ratio, left + 230f, y, 9f, TEXT_MUTED, monoTypeface)
                
                val dist = NumberParser.parseDecimal(nodes[i].distanceToNextText)?.let { "L=${format(it)} m" } ?: ""
                val drop = segment.dropMeters?.let { "Δh=${format(it)} m" } ?: ""
                drawText(canvas, "$dist   $drop", right - 85f, y, 9f, TEXT_MUTED, monoTypeface, Paint.Align.RIGHT)
                
                drawText(canvas, statusText(segment.status), right - 16f, y, 9f, sColor, mediumTypeface, Paint.Align.RIGHT)
                y += 20f
            }
        }
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, y: Float, size: Float, color: Int, typeface: Typeface, align: Paint.Align = Paint.Align.LEFT, letterSpacing: Float = 0f) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = size
            this.color = color
            this.typeface = typeface
            this.textAlign = align
            this.letterSpacing = letterSpacing / max(size, 1f)
        }
        canvas.drawText(text, x, y, paint)
    }

    private fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }
    
    private fun strokePaint(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = width
        this.color = color
    }

    private fun statusText(status: ImpactSlopeStatus): String = when (status) {
        ImpactSlopeStatus.VALID -> "Eğim uygun"
        ImpactSlopeStatus.REVERSE -> "Ters eğim"
        ImpactSlopeStatus.TOO_FLAT -> "Eğim yetersiz"
        ImpactSlopeStatus.TOO_STEEP -> "Eğim fazla dik"
        ImpactSlopeStatus.INCOMPLETE -> "Veri eksik"
    }

    private fun statusColor(status: ImpactSlopeStatus): Int = when (status) {
        ImpactSlopeStatus.VALID -> OK_COLOR
        ImpactSlopeStatus.REVERSE -> Color.rgb(229, 57, 53)
        ImpactSlopeStatus.TOO_FLAT, ImpactSlopeStatus.TOO_STEEP -> WARNING_COLOR
        ImpactSlopeStatus.INCOMPLETE -> TEXT_MUTED
    }
    private fun format(value: Double, decimals: Int = 2): String = String.format(turkishLocale, "%.${decimals}f", value)
    private const val PAGE_BACKGROUND = 0xFFF2F2F7.toInt()
    private const val CARD_BORDER = 0xFFE6E9EE.toInt()
    private const val TEXT = 0xFF1A2332.toInt()
    private const val TEXT_MUTED = 0xFF6F7E90.toInt()
    private const val ACCENT = 0xFFFF6B35.toInt()
    private val OK_COLOR = Color.rgb(34, 197, 94)
    private val WARNING_COLOR = Color.rgb(245, 158, 11)
}
