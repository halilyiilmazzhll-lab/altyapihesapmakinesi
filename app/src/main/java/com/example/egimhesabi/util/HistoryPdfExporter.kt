package com.example.egimhesabi.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.egimhesabi.data.HistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object HistoryPdfExporter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val PAGE_MARGIN = 32f
    private const val CARD_HEIGHT = 315f
    private const val CARD_GAP = 14f
    private const val CARDS_PER_PAGE = 2

    private val turkishLocale = Locale.forLanguageTag("tr-TR")
    private val titleTypeface = Typeface.create("sans-serif", Typeface.BOLD)
    private val mediumTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val regularTypeface = Typeface.create("sans-serif", Typeface.NORMAL)
    private val monoTypeface = Typeface.create("monospace", Typeface.NORMAL)

    fun suggestedFileName(now: Long = System.currentTimeMillis()): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", turkishLocale).format(Date(now))
        return "Egim_Hesabi_Gecmis_$stamp.pdf"
    }

    fun exportToCache(context: Context, entries: List<HistoryEntry>): java.io.File {
        require(entries.isNotEmpty()) { "PDF için en az bir geçmiş kaydı gereklidir." }
        val document = PdfDocument()
        val file = java.io.File(context.cacheDir, suggestedFileName())
        try {
            val pages = entries.chunked(CARDS_PER_PAGE)
            pages.forEachIndexed { pageIndex, pageEntries ->
                val pageNumber = pageIndex + 1
                val pageInfo = PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH,
                    PAGE_HEIGHT,
                    pageNumber
                ).create()
                val page = document.startPage(pageInfo)
                drawPage(page.canvas, pageEntries, pageNumber, pages.size, entries.size)
                document.finishPage(page)
            }
            java.io.FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
        } finally {
            document.close()
        }
        return file
    }

    fun export(context: Context, uri: Uri, entries: List<HistoryEntry>) {
        require(entries.isNotEmpty()) { "PDF için en az bir geçmiş kaydı gereklidir." }
        val document = PdfDocument()
        try {
            val pages = entries.chunked(CARDS_PER_PAGE)
            pages.forEachIndexed { pageIndex, pageEntries ->
                val pageNumber = pageIndex + 1
                val pageInfo = PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH,
                    PAGE_HEIGHT,
                    pageNumber
                ).create()
                val page = document.startPage(pageInfo)
                drawPage(
                    canvas = page.canvas,
                    entries = pageEntries,
                    pageNumber = pageNumber,
                    totalPages = pages.size,
                    totalEntries = entries.size
                )
                document.finishPage(page)
            }

            val output = context.contentResolver.openOutputStream(uri, "w")
                ?: error("PDF dosyası açılamadı.")
            output.use(document::writeTo)
        } finally {
            document.close()
        }
    }

    private fun drawPage(
        canvas: Canvas,
        entries: List<HistoryEntry>,
        pageNumber: Int,
        totalPages: Int,
        totalEntries: Int
    ) {
        canvas.drawColor(PAGE_BACKGROUND)

        drawText(
            canvas,
            "EĞİM HESABI",
            PAGE_MARGIN,
            36f,
            9f,
            ACCENT,
            titleTypeface,
            letterSpacing = 0.8f
        )
        drawText(canvas, "İşlem Geçmişi", PAGE_MARGIN, 61f, 21f, TEXT, titleTypeface)
        drawText(
            canvas,
            "$totalEntries kayıt \u00B7 tüm ayrıntılar açık",
            PAGE_MARGIN,
            78f,
            9f,
            TEXT_MUTED,
            regularTypeface
        )
        val generatedAt = SimpleDateFormat("dd.MM.yyyy HH:mm", turkishLocale).format(Date())
        drawText(
            canvas,
            generatedAt,
            PAGE_WIDTH - PAGE_MARGIN,
            61f,
            9f,
            TEXT_MUTED,
            regularTypeface,
            Paint.Align.RIGHT
        )

        entries.forEachIndexed { index, entry ->
            val top = 96f + index * (CARD_HEIGHT + CARD_GAP)
            drawEntryCard(canvas, entry, top)
        }

        drawText(
            canvas,
            "Sayfa $pageNumber / $totalPages",
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 14f,
            8f,
            TEXT_MUTED,
            regularTypeface,
            Paint.Align.CENTER
        )
    }

    private fun drawEntryCard(canvas: Canvas, entry: HistoryEntry, top: Float) {
        val left = PAGE_MARGIN
        val right = PAGE_WIDTH - PAGE_MARGIN
        val bottom = top + CARD_HEIGHT
        val statusColor = statusColor(entry.slopeStatus)
        val card = RectF(left, top, right, bottom)
        canvas.drawRoundRect(card, 18f, 18f, fillPaint(Color.WHITE))
        canvas.drawRoundRect(card, 18f, 18f, strokePaint(CARD_BORDER, 0.8f))

        val name1 = displayName(entry.baca1Name, "1. Baca", "B1", "BACA 1")
        val name2 = displayName(entry.baca2Name, "2. Baca", "B2", "BACA 2")
        canvas.drawCircle(left + 16f, top + 20f, 4f, fillPaint(statusColor))
        drawText(canvas, "$name1  \u2192  $name2", left + 27f, top + 24f, 11f, TEXT, mediumTypeface)

        val date = SimpleDateFormat("dd.MM.yyyy HH:mm", turkishLocale).format(Date(entry.timestamp))
        drawText(
            canvas,
            date,
            right - 15f,
            top + 24f,
            8f,
            TEXT_MUTED,
            regularTypeface,
            Paint.Align.RIGHT
        )

        val ratio = if (entry.slopeRatio > 0.0) "1 / ${format(entry.slopeRatio, 0)}" else "1 / -"
        drawMetric(canvas, left + 16f, top + 43f, "EĞİM", "%${format(entry.slopePercent)}", statusColor)
        drawMetric(canvas, left + 125f, top + 43f, "ORAN", ratio, statusColor)
        drawMetric(canvas, left + 237f, top + 43f, "KOT FARKI", "${format(entry.heightDiff)} m", ACCENT)
        drawMetric(canvas, left + 363f, top + 43f, "MESAFE", "${format(entry.mesafe)} m", TEXT)
        drawText(
            canvas,
            statusLabel(entry.slopeStatus),
            right - 15f,
            top + 69f,
            8f,
            statusColor,
            mediumTypeface,
            Paint.Align.RIGHT
        )

        drawProfile(
            canvas = canvas,
            entry = entry,
            bounds = RectF(left + 14f, top + 83f, right - 14f, bottom - 14f),
            statusColor = statusColor,
            name1 = name1,
            name2 = name2
        )
    }

    private fun drawMetric(
        canvas: Canvas,
        x: Float,
        y: Float,
        label: String,
        value: String,
        color: Int
    ) {
        drawText(canvas, label, x, y, 7f, TEXT_MUTED, mediumTypeface, letterSpacing = 0.4f)
        drawText(canvas, value, x, y + 19f, 13f, color, monoTypeface)
    }

    private fun drawProfile(
        canvas: Canvas,
        entry: HistoryEntry,
        bounds: RectF,
        statusColor: Int,
        name1: String,
        name2: String
    ) {
        canvas.drawRoundRect(bounds, 13f, 13f, fillPaint(PROFILE_BACKGROUND))

        val x1 = bounds.left + 72f
        val x2 = bounds.right - 72f
        val manholeWidth = 30f
        val chartTop = bounds.top + 35f
        val chartBottom = bounds.bottom - 42f

        val syntheticHeight = max(
            max(entry.baca1KapakKotu?.minus(entry.baca1AkarKotu) ?: 0.0, 1.2),
            max(entry.baca2KapakKotu?.minus(entry.baca2AkarKotu) ?: 0.0, 1.2)
        )
        val b1CoverForDrawing = entry.baca1KapakKotu ?: entry.baca1AkarKotu + syntheticHeight
        val b2CoverForDrawing = entry.baca2KapakKotu ?: entry.baca2AkarKotu + syntheticHeight
        val values = listOf(
            b1CoverForDrawing,
            entry.baca1AkarKotu,
            b2CoverForDrawing,
            entry.baca2AkarKotu
        )
        val maxLevel = values.maxOrNull() ?: 1.0
        val minLevel = values.minOrNull() ?: 0.0
        val range = max(maxLevel - minLevel, 0.5)
        fun y(level: Double): Float = chartTop +
            ((maxLevel - level) / range).toFloat() * (chartBottom - chartTop)

        val b1Top = y(b1CoverForDrawing)
        val b1Bottom = y(entry.baca1AkarKotu)
        val b2Top = y(b2CoverForDrawing)
        val b2Bottom = y(entry.baca2AkarKotu)

        val groundPaint = strokePaint(GROUND, 1.2f).apply {
            pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
        }
        canvas.drawLine(bounds.left + 12f, b1Top, bounds.right - 12f, b2Top, groundPaint)

        drawManhole(canvas, x1, b1Top, b1Bottom, manholeWidth)
        drawManhole(canvas, x2 - manholeWidth, b2Top, b2Bottom, manholeWidth)

        val pipePaint = strokePaint(statusColor, 8f).apply { strokeCap = Paint.Cap.ROUND }
        canvas.drawLine(
            x1 + manholeWidth,
            b1Bottom - 3f,
            x2 - manholeWidth,
            b2Bottom - 3f,
            pipePaint
        )
        canvas.drawLine(
            x1 + manholeWidth,
            b1Bottom - 4.5f,
            x2 - manholeWidth,
            b2Bottom - 4.5f,
            strokePaint(Color.argb(120, 255, 255, 255), 2.5f)
        )

        drawText(canvas, name1, x1 + manholeWidth / 2f, bounds.top + 18f, 8f, TEXT, mediumTypeface, Paint.Align.CENTER)
        drawText(canvas, name2, x2 - manholeWidth / 2f, bounds.top + 18f, 8f, TEXT, mediumTypeface, Paint.Align.CENTER)

        drawLevelLabel(
            canvas,
            entry.baca1KapakKotu?.let { "Kapak ${format(it)}" },
            x1 - 7f,
            b1Top - 5f,
            Paint.Align.RIGHT
        )
        drawLevelLabel(
            canvas,
            "Akar ${format(entry.baca1AkarKotu)}",
            x1 - 7f,
            b1Bottom + 4f,
            Paint.Align.RIGHT
        )
        drawLevelLabel(
            canvas,
            entry.baca2KapakKotu?.let { "Kapak ${format(it)}" },
            x2 + 7f,
            b2Top - 5f,
            Paint.Align.LEFT
        )
        drawLevelLabel(
            canvas,
            "Akar ${format(entry.baca2AkarKotu)}",
            x2 + 7f,
            b2Bottom + 4f,
            Paint.Align.LEFT
        )

        val depth1 = entry.baca1KapakKotu?.minus(entry.baca1AkarKotu)
        val depth2 = entry.baca2KapakKotu?.minus(entry.baca2AkarKotu)
        val detailText = buildString {
            if (depth1 != null) append("$name1 derinlik ${format(depth1)} m")
            if (depth1 != null && depth2 != null) append("     \u00B7     ")
            if (depth2 != null) append("$name2 derinlik ${format(depth2)} m")
            if (isEmpty()) append("Kapak kotu girilmemiş")
        }
        drawText(
            canvas,
            detailText,
            bounds.centerX(),
            bounds.bottom - 10f,
            8f,
            TEXT_MUTED,
            regularTypeface,
            Paint.Align.CENTER
        )
        drawText(
            canvas,
            "\u2190 ${format(entry.mesafe)} m  \u2192",
            bounds.centerX(),
            bounds.bottom - 25f,
            8f,
            TEXT,
            monoTypeface,
            Paint.Align.CENTER
        )
    }

    private fun drawManhole(canvas: Canvas, left: Float, top: Float, bottom: Float, width: Float) {
        val safeBottom = max(bottom, top + 18f)
        val body = RectF(left, top, left + width, safeBottom)
        canvas.drawRoundRect(body, 4f, 4f, fillPaint(Color.WHITE))
        canvas.drawRoundRect(body, 4f, 4f, strokePaint(MANHOLE_BORDER, 1f))
        canvas.drawLine(left - 4f, top, left + width + 4f, top, strokePaint(TEXT_MUTED, 3f))
    }

    private fun drawLevelLabel(
        canvas: Canvas,
        text: String?,
        x: Float,
        y: Float,
        align: Paint.Align
    ) {
        if (text == null) return
        drawText(canvas, text, x, y, 7f, TEXT_MUTED, monoTypeface, align)
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        typeface: Typeface,
        align: Paint.Align = Paint.Align.LEFT,
        letterSpacing: Float = 0f
    ) {
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

    private fun displayName(value: String, fallback: String, vararg genericNames: String): String {
        return if (value.isBlank() || genericNames.any { value.equals(it, ignoreCase = true) }) {
            fallback
        } else {
            value
        }
    }

    private fun statusLabel(status: String): String = when (status) {
        "OK" -> "Eğim uygun"
        "NEAR_LIMIT" -> "Sınıra yakın"
        "TOO_LOW" -> "Eğim yetersiz"
        "TOO_HIGH" -> "Eğim fazla dik"
        else -> "Durum belirsiz"
    }

    private fun statusColor(status: String): Int = when (status) {
        "OK" -> Color.rgb(34, 197, 94)
        "NEAR_LIMIT" -> Color.rgb(245, 158, 11)
        "TOO_LOW", "TOO_HIGH" -> Color.rgb(239, 68, 68)
        else -> TEXT_MUTED
    }

    private fun format(value: Double, decimals: Int = 2): String =
        String.format(turkishLocale, "%.${decimals}f", value)

    private const val PAGE_BACKGROUND = 0xFFF2F2F7.toInt()
    private const val PROFILE_BACKGROUND = 0xFFF4F6FA.toInt()
    private const val CARD_BORDER = 0xFFE6E9EE.toInt()
    private const val MANHOLE_BORDER = 0xFFC8D0DA.toInt()
    private const val GROUND = 0xFFBAC3CE.toInt()
    private const val TEXT = 0xFF1A2332.toInt()
    private const val TEXT_MUTED = 0xFF6F7E90.toInt()
    private const val ACCENT = 0xFFFF6B35.toInt()
}
