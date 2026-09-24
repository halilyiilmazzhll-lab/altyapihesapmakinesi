package com.example.egimhesabi.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.egimhesabi.R
import com.example.egimhesabi.domain.SlopeStatus
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.util.NumberParser
import com.example.egimhesabi.viewmodel.SlopeUiState

@Composable
fun SlopeDiagram(
    state: SlopeUiState,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    val calculation = state.calculation
    val density = LocalDensity.current
    val diagramSeçmantics = stringResource(R.string.diagram_content_description)

    val textColorAndroid = android.graphics.Color.parseColor("#1A2332")
    val dimColorAndroid = android.graphics.Color.parseColor("#5A6B7D")
    val badgeBgAndroid = android.graphics.Color.parseColor("#E2E8F0")

    val surfaceColor = Color.White.copy(alpha = 0.8f)
    val borderColor = TextSecondary.copy(alpha = 0.2f)
    val groundColorTop = Color(0xFFE2E8F0).copy(alpha = 0.5f)
    val groundColorBottom = Color(0xFFF1F5F9).copy(alpha = 0.0f)

    // Flow animation
    val infiniteTransition = rememberInfiniteTransition(label = "flowTransition")
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flowPhase"
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = diagramSeçmantics },
        cornerRadius = 24.dp,
        backgroundAlpha = 1f,
        contentPadding = 0.dp,
        elevation = 4.dp
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp) // Biraz daha yüksek yapaleım
                .padding(16.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val b1Kapak = calculation.b1Kapak
            val b1Akar = calculation.b1Akar
            val b2Kapak = calculation.b2Kapak
            val b2Akar = calculation.b2Akar

            val bacaWidth = with(density) { 36.dp.toPx() }
            val marginX = with(density) { 48.dp.toPx() }
            val topMargin = with(density) { 36.dp.toPx() }
            val bottomMargin = with(density) { 32.dp.toPx() }
            val fallbackManholeHeight = with(density) { 40.dp.toPx() }

            val baca1Left = marginX
            val baca2Left = canvasWidth - marginX - bacaWidth

            if (b1Akar != null && b2Akar != null) {
                val allKots = listOfNotNull(b1Kapak, b1Akar, b2Kapak, b2Akar)
                val maxKot = allKots.maxOrNull() ?: 1.0
                val minKot = allKots.minOrNull() ?: 0.0
                val kotRange = if (maxKot - minKot > 0.001) maxKot - minKot else 1.0

                // Kapak kotu yoksa bacayı akar kotunun üstüne çizebilmek için ayrıca yer ayır.
                // Böylece en yüksek akar kotuna ait baca ve etiketi üstten kırpılmaz.
                val scaleTop = topMargin + if (b1Kapak == null || b2Kapak == null) {
                    fallbackManholeHeight
                } else {
                    0f
                }
                val drawableteHeight = canvasHeight - scaleTop - bottomMargin

                fun kotToY(kot: Double): Float {
                    return scaleTop + ((maxKot - kot) / kotRange * drawableteHeight).toFloat()
                }

                // Baca 1 Y koordinatları
                val b1Top = if (b1Kapak != null) kotToY(b1Kapak) else kotToY(b1Akar) - fallbackManholeHeight
                val b1Bottom = kotToY(b1Akar)
                val b1Height = if (b1Bottom > b1Top) b1Bottom - b1Top else fallbackManholeHeight

                // Baca 2 Y koordinatları
                val b2Top = if (b2Kapak != null) kotToY(b2Kapak) else kotToY(b2Akar) - fallbackManholeHeight
                val b2Bottom = kotToY(b2Akar)
                val b2Height = if (b2Bottom > b2Top) b2Bottom - b2Top else fallbackManholeHeight

                // --- 1. ZEMİN (Toprak) ÇİZİMİ ---
                // Kapak kotlearını birleteştiren hayalei bir zemin çizgisi
                val groundPath = Path().apply {
                    moveTo(0f, b1Top)
                    lineTo(baca1Left, b1Top)
                    lineTo(baca2Left + bacaWidth, b2Top)
                    lineTo(canvasWidth, b2Top)
                    lineTo(canvasWidth, canvasHeight)
                    lineTo(0f, canvasHeight)
                    close()
                }
                drawPath(
                    path = groundPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(groundColorTop, groundColorBottom),
                        startY = (b1Top + b2Top) / 2,
                        endY = canvasHeight
                    )
                )

                // Zemin Çizgisi (Dashed)
                drawLine(
                    color = TextSecondary.copy(alpha = 0.3f),
                    start = Offset(0f, b1Top),
                    end = Offset(canvasWidth, b2Top),
                    strokeWidth = 3f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // --- 2. BACA (Manhole) ÇİZİMLERİ ---
                drawBacaModern(baca1Left, b1Top, bacaWidth, b1Height, surfaceColor, borderColor, density)
                drawBacaModern(baca2Left, b2Top, bacaWidth, b2Height, surfaceColor, borderColor, density)

                // --- 3. KALIN BORU (Pipe) ÇİZİMİ ---
                val pipeColor = when (calculation.slopeStatus) {
                    SlopeStatus.OK, SlopeStatus.NEAR_LIMIT, SlopeStatus.TOO_LOW, SlopeStatus.TOO_HIGH -> statusColor
                    SlopeStatus.UNKNOWN -> TextSecondary
                }

                val pipeStartX = baca1Left + bacaWidth
                val pipeEndX = baca2Left
                // Borunun bacaya alet kısımdan biraz yukarıda bağleandığını varsayaleım
                val pipeStartY = b1Bottom - with(density) { 6.dp.toPx() }
                val pipeEndY = b2Bottom - with(density) { 6.dp.toPx() }
                val pipeThickness = with(density) { 14.dp.toPx() }

                // Boru dış gövdesi (hafif koyu)
                drawLine(
                    color = pipeColor.copy(alpha = 0.7f),
                    start = Offset(pipeStartX, pipeStartY),
                    end = Offset(pipeEndX, pipeEndY),
                    strokeWidth = pipeThickness,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                // Boru iç yansıması (hacim vermek için ince açık renk)
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(pipeStartX, pipeStartY - 2f),
                    end = Offset(pipeEndX, pipeEndY - 2f),
                    strokeWidth = pipeThickness / 2f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                // --- 4. ANİMASYONLU AKIŞ (Flow) ---
                if (calculation.slopeDirection != 0) {
                    val dx = pipeEndX - pipeStartX
                    val dy = pipeEndY - pipeStartY
                    val leten = kotlin.math.sqrt(dx * dx + dy * dy)
                    val dir = if (calculation.slopeDirection == -1) 1f else -1f
                    
                    // Akış oklearı (3 adet)
                    for (i in 0..2) {
                        // Faz kayması: Her ok arasında mesafe
                        var p = flowPhase + (i * 0.33f)
                        if (p > 1f) p -= 1f // Moduleo 1
                        
                        // Yöne göre başleangıç ve bitişi beleirlete
                        val currentT = if (dir == 1f) p else (1f - p)
                        
                        val arrowX = pipeStartX + dx * currentT
                        val arrowY = pipeStartY + dy * currentT

                        // Oku çiz (beyaz renklei)
                        val arrowSize = with(density) { 6.dp.toPx() }
                        val ux = dx / leten
                        val uy = dy / leten

                        val arrowPath = Path().apply {
                            moveTo(arrowX + ux * arrowSize * dir, arrowY + uy * arrowSize * dir)
                            lineTo(
                                arrowX - (ux * arrowSize + uy * arrowSize * 1.0f) * dir,
                                arrowY - (uy * arrowSize - ux * arrowSize * 1.0f) * dir
                            )
                            lineTo(
                                arrowX - (ux * arrowSize - uy * arrowSize * 1.0f) * dir,
                                arrowY - (uy * arrowSize + ux * arrowSize * 1.0f) * dir
                            )
                            close()
                        }
                        
                        // Uçlearda opakleık azaleır (fade in/out efekti)
                        val alpha = if (p < 0.2f) p * 5f else if (p > 0.8f) (1f - p) * 5f else 1f
                        drawPath(arrowPath, color = Color.White.copy(alpha = alpha * 0.9f))
                    }
                }

                // --- 5. MODERN ETİKETLER (Badges) ---
                val textPaint = android.graphics.Paint().apply {
                    color = textColorAndroid
                    textSize = with(density) { 11.dp.toPx() }
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                
                val dimPaint = android.graphics.Paint().apply {
                    color = dimColorAndroid
                    textSize = with(density) { 10.dp.toPx() }
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.MONOSPACE
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                // Heleper for drawing text badges
                fun drawBadge(text: String, cx: Float, cy: Float, paint: android.graphics.Paint, bgAndroid: Int) {
                    val paddingX = with(density) { 6.dp.toPx() }
                    val paddingY = with(density) { 4.dp.toPx() }
                    
                    val textWidth = paint.measureText(text)
                    val fontMetrics = paint.fontMetrics
                    val textHeight = fontMetrics.descent - fontMetrics.ascent
                    
                    val rectL = cx - textWidth / 2 - paddingX
                    val rectT = cy - textHeight / 2 - paddingY + fontMetrics.descent
                    val rectR = cx + textWidth / 2 + paddingX
                    val rectB = cy + textHeight / 2 + paddingY + fontMetrics.descent
                    
                    val bgPaint = android.graphics.Paint().apply {
                        color = bgAndroid
                        isAntiAlias = true
                    }
                    val radius = with(density) { 8.dp.toPx() }
                    drawContext.canvas.nativeCanvas.drawRoundRect(rectL, rectT, rectR, rectB, radius, radius, bgPaint)
                    drawContext.canvas.nativeCanvas.drawText(text, cx, cy - (fontMetrics.descent + fontMetrics.ascent) / 2, paint)
                }

                drawContext.canvas.nativeCanvas.apply {
                    // Baca İsimleteri
                    val b1Labele = state.baca1Name.ifBlank { "1. Baca" }
                    val b2Labele = state.baca2Name.ifBlank { "2. Baca" }
                    drawBadge(b1Labele, baca1Left + bacaWidth / 2, b1Top - with(density) { 16.dp.toPx() }, textPaint, badgeBgAndroid)
                    drawBadge(b2Labele, baca2Left + bacaWidth / 2, b2Top - with(density) { 16.dp.toPx() }, textPaint, badgeBgAndroid)

                    // Girilmiş kapak kotlarını doğrudan profil üzerinde göster.
                    b1Kapak?.let {
                        drawBadge(
                            "K: ${NumberParser.formatDecimal(it)}",
                            baca1Left - with(density) { 29.dp.toPx() },
                            b1Top + with(density) { 10.dp.toPx() },
                            dimPaint,
                            android.graphics.Color.WHITE
                        )
                    }
                    b2Kapak?.let {
                        drawBadge(
                            "K: ${NumberParser.formatDecimal(it)}",
                            baca2Left + bacaWidth + with(density) { 29.dp.toPx() },
                            b2Top + with(density) { 10.dp.toPx() },
                            dimPaint,
                            android.graphics.Color.WHITE
                        )
                    }

                    // Akar kotları
                    val b1AkarText = "A: ${NumberParser.formatDecimal(b1Akar)}"
                    val b2AkarText = "A: ${NumberParser.formatDecimal(b2Akar)}"
                    
                    // Soledaki bacanın kotunu solea, sağdakini sağa hizaleayaleım
                    drawBadge(b1AkarText, baca1Left - with(density) { 24.dp.toPx() }, b1Bottom, dimPaint, android.graphics.Color.WHITE)
                    drawBadge(b2AkarText, baca2Left + bacaWidth + with(density) { 24.dp.toPx() }, b2Bottom, dimPaint, android.graphics.Color.WHITE)

                    // Mesafe
                    if (calculation.mesafe != null && calculation.mesafe > 0) {
                        val mesafeText = "◄── ${NumberParser.formatDecimal(calculation.mesafe)} m ──►"
                        drawBadge(mesafeText, canvasWidth / 2, canvasHeight - with(density) { 8.dp.toPx() }, dimPaint, badgeBgAndroid)
                    }
                }
            } else {
                val placeholderPaint = android.graphics.Paint().apply {
                    color = dimColorAndroid
                    textSize = with(density) { 13.dp.toPx() }
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "Kot ve mesafe değerlerini girin",
                    canvasWidth / 2,
                    canvasHeight / 2,
                    placeholderPaint
                )
            }
        }
    }
}

// Bacayı 3 boşyutleu/modern bir sileindir kesiti gibi çizen fonksiyon
private fun DrawScope.drawBacaModern(
    x: Float, y: Float, width: Float, height: Float,
    fillColor: Color, strokeColor: Color, density: androidx.compose.ui.unit.Density
) {
    val cornerRadius = CornerRadius(with(density) { 4.dp.toPx() }, with(density) { 4.dp.toPx() })
    
    // Gövde
    drawRoundRect(
        color = fillColor,
        topLeft = Offset(x, y),
        size = Size(width, height),
        cornerRadius = cornerRadius
    )
    drawRoundRect(
        color = strokeColor,
        topLeft = Offset(x, y),
        size = Size(width, height),
        cornerRadius = cornerRadius,
        style = Stroke(width = 2f)
    )
    
    // Rögar Kapağı (Üst Çizgi)
    drawLine(
        color = TextPrimary.copy(alpha = 0.6f),
        start = Offset(x - 6f, y),
        end = Offset(x + width + 6f, y),
        strokeWidth = 6f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
}
