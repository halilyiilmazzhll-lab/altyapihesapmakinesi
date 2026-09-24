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
import androidx.compose.ui.unit.dp
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.util.NumberParser

@Composable
fun InterpolationDiagram(
    kot1: Double,
    kot2: Double,
    totalDistance: Double,
    sliderPosition: Float,
    currentKot: Double?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val textColorAndroid = android.graphics.Color.parseColor("#1A2332")
    val dimColorAndroid = android.graphics.Color.parseColor("#5A6B7D")
    val badgeBgAndroid = android.graphics.Color.parseColor("#E2E8F0")
    val orangeAndroid = android.graphics.Color.parseColor("#FF6B35")

    val surfaceColor = Color.White.copy(alpha = 0.8f)
    val borderColor = TextSecondary.copy(alpha = 0.2f)
    val groundColorTop = Color(0xFFE2E8F0).copy(alpha = 0.5f)
    val groundColorBottom = Color(0xFFF1F5F9).copy(alpha = 0.0f)

    // Flow animation
    val infiniteTransition = rememberInfiniteTransition(label = "interpFlow")
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "interpFlowPhase"
    )

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        backgroundAlpha = 1f,
        contentPadding = 0.dp,
        elevation = 4.dp
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val bacaWidth = with(density) { 32.dp.toPx() }
            val marginX = with(density) { 40.dp.toPx() }
            val topMargin = with(density) { 36.dp.toPx() }
            val bottomMargin = with(density) { 32.dp.toPx() }

            val baca1Left = marginX
            val baca2Left = canvasWidth - marginX - bacaWidth

            val maxKot = maxOf(kot1, kot2)
            val minKot = minOf(kot1, kot2)
            val kotRange = if (maxKot - minKot > 0.001) maxKot - minKot else 1.0

            val drawableteHeight = canvasHeight - topMargin - bottomMargin

            fun kotToY(kot: Double): Float {
                return topMargin + ((maxKot - kot) / kotRange * drawableteHeight).toFloat()
            }

            val y1 = kotToY(kot1)
            val y2 = kotToY(kot2)
            val bacaHeight = with(density) { 40.dp.toPx() }

            // --- Zemin (Toprak) ---
            val groundPath = Path().apply {
                moveTo(0f, y1)
                lineTo(baca1Left, y1)
                lineTo(baca2Left + bacaWidth, y2)
                lineTo(canvasWidth, y2)
                lineTo(canvasWidth, canvasHeight)
                lineTo(0f, canvasHeight)
                close()
            }
            drawPath(
                path = groundPath,
                brush = Brush.verticalGradient(
                    colors = listOf(groundColorTop, groundColorBottom),
                    startY = (y1 + y2) / 2,
                    endY = canvasHeight
                )
            )

            // Zemin çizgisi (Dashed)
            drawLine(
                color = TextSecondary.copy(alpha = 0.25f),
                start = Offset(0f, y1),
                end = Offset(canvasWidth, y2),
                strokeWidth = 2f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )

            // --- Bacalar ---
            val cornerRadius = CornerRadius(with(density) { 4.dp.toPx() })

            // Baca 1
            drawRoundRect(color = surfaceColor, topLeft = Offset(baca1Left, y1), size = Size(bacaWidth, bacaHeight), cornerRadius = cornerRadius)
            drawRoundRect(color = borderColor, topLeft = Offset(baca1Left, y1), size = Size(bacaWidth, bacaHeight), cornerRadius = cornerRadius, style = Stroke(width = 2f))
            drawLine(color = TextPrimary.copy(alpha = 0.6f), start = Offset(baca1Left - 4f, y1), end = Offset(baca1Left + bacaWidth + 4f, y1), strokeWidth = 5f, cap = androidx.compose.ui.graphics.StrokeCap.Round)

            // Baca 2
            drawRoundRect(color = surfaceColor, topLeft = Offset(baca2Left, y2), size = Size(bacaWidth, bacaHeight), cornerRadius = cornerRadius)
            drawRoundRect(color = borderColor, topLeft = Offset(baca2Left, y2), size = Size(bacaWidth, bacaHeight), cornerRadius = cornerRadius, style = Stroke(width = 2f))
            drawLine(color = TextPrimary.copy(alpha = 0.6f), start = Offset(baca2Left - 4f, y2), end = Offset(baca2Left + bacaWidth + 4f, y2), strokeWidth = 5f, cap = androidx.compose.ui.graphics.StrokeCap.Round)

            // --- Boru ---
            val pipeStartX = baca1Left + bacaWidth
            val pipeEndX = baca2Left
            val pipeStartY = y1 + bacaHeight - with(density) { 10.dp.toPx() }
            val pipeEndY = y2 + bacaHeight - with(density) { 10.dp.toPx() }
            val pipeThickness = with(density) { 12.dp.toPx() }

            val pipeColor = TextSecondary.copy(alpha = 0.4f)

            drawLine(
                color = pipeColor,
                start = Offset(pipeStartX, pipeStartY),
                end = Offset(pipeEndX, pipeEndY),
                strokeWidth = pipeThickness,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(pipeStartX, pipeStartY - 1.5f),
                end = Offset(pipeEndX, pipeEndY - 1.5f),
                strokeWidth = pipeThickness / 2.5f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            // --- Slider Noktası (Turuncu daire) ---
            val pointX = pipeStartX + (pipeEndX - pipeStartX) * sliderPosition
            val pointY = pipeStartY + (pipeEndY - pipeStartY) * sliderPosition

            // Dikey kıleavuz çizgi
            drawLine(
                color = AccentOrange.copy(alpha = 0.3f),
                start = Offset(pointX, topMargin - with(density) { 8.dp.toPx() }),
                end = Offset(pointX, canvasHeight - bottomMargin + with(density) { 8.dp.toPx() }),
                strokeWidth = 2f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            // Dış haleka
            val outerRadius = with(density) { 10.dp.toPx() }
            drawCircle(
                color = AccentOrange.copy(alpha = 0.2f),
                radius = outerRadius,
                center = Offset(pointX, pointY)
            )
            // İç daire
            val innerRadius = with(density) { 6.dp.toPx() }
            drawCircle(
                color = AccentOrange,
                radius = innerRadius,
                center = Offset(pointX, pointY)
            )
            // Beyaz iç nokta
            drawCircle(
                color = Color.White,
                radius = with(density) { 2.5.dp.toPx() },
                center = Offset(pointX, pointY)
            )

            // --- Etiketleter ---
            val textPaint = android.graphics.Paint().apply {
                color = textColorAndroid
                textSize = with(density) { 10.dp.toPx() }
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
            }

            val dimPaint = android.graphics.Paint().apply {
                color = dimColorAndroid
                textSize = with(density) { 9.dp.toPx() }
                isAntiAlias = true
                typeface = android.graphics.Typeface.MONOSPACE
                textAlign = android.graphics.Paint.Align.CENTER
            }

            fun drawBadge(text: String, cx: Float, cy: Float, paint: android.graphics.Paint, bgAndroid: Int) {
                val paddingX = with(density) { 5.dp.toPx() }
                val paddingY = with(density) { 3.dp.toPx() }
                val textWidth = paint.measureText(text)
                val fontMetrics = paint.fontMetrics
                val rectL = cx - textWidth / 2 - paddingX
                val rectT = cy - (fontMetrics.descent - fontMetrics.ascent) / 2 - paddingY + fontMetrics.descent
                val rectR = cx + textWidth / 2 + paddingX
                val rectB = cy + (fontMetrics.descent - fontMetrics.ascent) / 2 + paddingY + fontMetrics.descent
                val bgPaint = android.graphics.Paint().apply { color = bgAndroid; isAntiAlias = true }
                val radius = with(density) { 6.dp.toPx() }
                drawContext.canvas.nativeCanvas.drawRoundRect(rectL, rectT, rectR, rectB, radius, radius, bgPaint)
                drawContext.canvas.nativeCanvas.drawText(text, cx, cy - (fontMetrics.descent + fontMetrics.ascent) / 2, paint)
            }

            // Baca etiketleteri
            drawBadge(NumberParser.formatDecimal(kot1), baca1Left + bacaWidth / 2, y1 - with(density) { 14.dp.toPx() }, textPaint, badgeBgAndroid)
            drawBadge(NumberParser.formatDecimal(kot2), baca2Left + bacaWidth / 2, y2 - with(density) { 14.dp.toPx() }, textPaint, badgeBgAndroid)

            // Mevcut kot badge (turuncu)
            if (currentKot != null) {
                val kotPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = with(density) { 10.dp.toPx() }
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawBadge(NumberParser.formatDecimal(currentKot), pointX, pointY - with(density) { 18.dp.toPx() }, kotPaint, orangeAndroid)
            }

            // Mesafe etiketi
            val distText = "◄── ${NumberParser.formatDecimal(totalDistance)} m ──►"
            drawBadge(distText, canvasWidth / 2, canvasHeight - with(density) { 6.dp.toPx() }, dimPaint, badgeBgAndroid)
        }
    }
}
