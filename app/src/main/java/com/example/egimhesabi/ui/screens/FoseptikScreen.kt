package com.example.egimhesabi.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.egimhesabi.theme.*
import com.example.egimhesabi.ui.components.GlassCard
import com.example.egimhesabi.util.NumberParser
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoseptikScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    var manholeInvertStr by remember { mutableStateOf("") }
    var distanceStr by remember { mutableStateOf("") }
    var slopeStr by remember { mutableStateOf("") }
    var slopeType by remember { mutableStateOf(0) } // 0: 1/x, 1: %, 2: cm/m
    var groundElevStr by remember { mutableStateOf("") }
    var tankHeightStr by remember { mutableStateOf("") }
    var coverToInletStr by remember { mutableStateOf("") }

    val manholeInvert = manholeInvertStr.replace(",", ".").toDoubleOrNull()
    val distance = distanceStr.replace(",", ".").toDoubleOrNull()
    val slopeVal = slopeStr.replace(",", ".").toDoubleOrNull()
    val groundElev = groundElevStr.replace(",", ".").toDoubleOrNull()
    val tankHeight = tankHeightStr.replace(",", ".").toDoubleOrNull()
    val coverToInlet = coverToInletStr.replace(",", ".").toDoubleOrNull()

    var inletElev: Double? = null
    var bottomElev: Double? = null
    var coverElev: Double? = null
    var excavationDepth: Double? = null
    var slopeDecimal: Double? = null

    if (manholeInvert != null && distance != null && slopeVal != null && slopeVal != 0.0) {
        slopeDecimal = when (slopeType) {
            0 -> 1.0 / slopeVal
            1 -> slopeVal / 100.0
            else -> slopeVal / 100.0
        }
        inletElev = manholeInvert - (distance * slopeDecimal)

        if (tankHeight != null && coverToInlet != null) {
            coverElev = inletElev + coverToInlet
            bottomElev = coverElev - tankHeight
            
            if (groundElev != null) {
                excavationDepth = groundElev - bottomElev
            }
        }
    }

    val glassFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AccentOrange,
        unfocusedBorderColor = Color(0xFFCBD5E1),
        focusedContainerColor = Color.White.copy(alpha = 0.85f),
        unfocusedContainerColor = Color.White.copy(alpha = 0.65f),
        focusedLabelColor = AccentOrange,
        unfocusedLabelColor = TextSecondary,
        cursorColor = AccentOrange,
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
    )

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
                    Text(
                        text = "Foseptik Hesabı",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // VISUALIZATION
                FoseptikDiagram(
                    manholeInvert = manholeInvert,
                    distance = distance,
                    slopeDecimal = slopeDecimal,
                    inletElev = inletElev,
                    coverElev = coverElev,
                    bottomElev = bottomElev,
                    groundElev = groundElev
                )

                // INPUTS
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Hesaplama Değerleri", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 18.sp, 
                            color = TextPrimary
                        )
                        
                        Text(
                            "Bu sayfada, mevcut bir bacadan foseptiğe gidecek hattın eğimini ve foseptik kuyusunun derinlik/kot hesaplarını yapabilirsiniz. Değerleri girdikçe çizim otomatik güncellenir.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )

                        Divider(color = Color.LightGray.copy(alpha = 0.5f))

                        // GRUP 1: Başlangıç
                        Text("1. Başlangıç Noktası (Baca)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AccentOrange)
                        OutlinedTextField(
                            value = manholeInvertStr,
                            onValueChange = { manholeInvertStr = it },
                            label = { Text("Bacanın Akar Kotu (Başlangıç)") },
                            placeholder = { Text("Örn: 100.50") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = glassFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider(color = Color.LightGray.copy(alpha = 0.5f))

                        // GRUP 2: Hat (Mesafe ve Eğim)
                        Text("2. Hat Bilgileri (Mesafe ve Eğim)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AccentOrange)
                        OutlinedTextField(
                            value = distanceStr,
                            onValueChange = { distanceStr = it },
                            label = { Text("Baca ile Foseptik Arası Mesafe (m)") },
                            placeholder = { Text("Örn: 25.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = glassFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Text("Eğim Tipi Seçimi:", fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = slopeType == 0, onClick = { slopeType = 0 }, colors = RadioButtonDefaults.colors(selectedColor = AccentOrange))
                                    Text("1/x", fontSize = 14.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = slopeType == 1, onClick = { slopeType = 1 }, colors = RadioButtonDefaults.colors(selectedColor = AccentOrange))
                                    Text("%", fontSize = 14.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = slopeType == 2, onClick = { slopeType = 2 }, colors = RadioButtonDefaults.colors(selectedColor = AccentOrange))
                                    Text("cm/m", fontSize = 14.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = slopeStr,
                            onValueChange = { slopeStr = it },
                            label = { Text("Eğim Değeri") },
                            placeholder = { Text("Seçilen tipe göre eğim girin") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = glassFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider(color = Color.LightGray.copy(alpha = 0.5f))

                        // GRUP 3: Foseptik Özellikleri
                        Text("3. Foseptik (Kuyu) Bilgileri", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AccentOrange)
                        
                        OutlinedTextField(
                            value = tankHeightStr,
                            onValueChange = { tankHeightStr = it },
                            label = { Text("Foseptik Toplam Boyu (m)") },
                            placeholder = { Text("Kuyunun iç yüksekliği") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = glassFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = coverToInletStr,
                            onValueChange = { coverToInletStr = it },
                            label = { Text("Kapak ile Akar Arası Mesafe (m)") },
                            placeholder = { Text("Borunun girdiği noktanın kapağa uzaklığı") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = glassFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = groundElevStr,
                            onValueChange = { groundElevStr = it },
                            label = { Text("Zemin Kotu (Opsiyonel)") },
                            placeholder = { Text("Kazı hesabı için foseptiğin kurulacağı zemin kotu") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = glassFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // RESULTS
                if (bottomElev != null) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        cornerRadius = 24.dp,
                        backgroundAlpha = 1f
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AccentOrange.copy(alpha = 0.1f))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Sonuçlar", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AccentOrange)
                            
                            inletElev?.let {
                                Text("• Foseptik Giriş (Akar) Kotu: ${NumberParser.formatDecimal(it)} m", color = TextPrimary)
                            }
                            coverElev?.let {
                                Text("• Foseptik Kapak Kotu: ${NumberParser.formatDecimal(it)} m", color = TextPrimary)
                            }
                            Text("• Foseptik Taban Kotu (Kazı Alt Noktası): ${NumberParser.formatDecimal(bottomElev)} m", fontWeight = FontWeight.Bold, color = TextPrimary)
                            
                            excavationDepth?.let {
                                Text("• Kazı Derinliği (Zeminden Tabana): ${NumberParser.formatDecimal(it)} m", fontWeight = FontWeight.Medium, color = Color(0xFFD97706)) // slightly darker orange
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun FoseptikDiagram(
    manholeInvert: Double?,
    distance: Double?,
    slopeDecimal: Double?,
    inletElev: Double?,
    coverElev: Double?,
    bottomElev: Double?,
    groundElev: Double?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
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
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        backgroundAlpha = 1f,
        contentPadding = 0.dp,
        elevation = 4.dp
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(16.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val bacaWidth = with(density) { 36.dp.toPx() }
            val tankWidth = with(density) { 60.dp.toPx() }
            val marginX = with(density) { 32.dp.toPx() }
            val topMargin = with(density) { 40.dp.toPx() }
            val bottomMargin = with(density) { 40.dp.toPx() }
            val fallbackHeight = with(density) { 60.dp.toPx() }

            val bacaLeft = marginX
            val tankLeft = canvasWidth - marginX - tankWidth

            if (manholeInvert != null && inletElev != null && coverElev != null && bottomElev != null) {
                val allKots = listOfNotNull(manholeInvert, inletElev, coverElev, bottomElev, groundElev)
                val maxKot = allKots.maxOrNull() ?: 1.0
                val minKot = allKots.minOrNull() ?: 0.0
                val kotRange = if (maxKot - minKot > 0.001) maxKot - minKot else 1.0

                val drawableHeight = canvasHeight - topMargin - bottomMargin

                fun kotToY(kot: Double): Float {
                    return topMargin + ((maxKot - kot) / kotRange * drawableHeight).toFloat()
                }

                val mTop = kotToY(maxKot) // Manhole top goes to max elevation
                val mAkar = kotToY(manholeInvert)
                val tTop = kotToY(coverElev)
                val tAkar = kotToY(inletElev)
                val tBottom = kotToY(bottomElev)
                
                val groundY = groundElev?.let { kotToY(it) }

                // --- 1. ZEMİN ÇİZİMİ ---
                val groundPath = Path().apply {
                    val gyLeft = groundY ?: mTop
                    val gyRight = groundY ?: tTop
                    moveTo(0f, gyLeft)
                    lineTo(bacaLeft, gyLeft)
                    lineTo(tankLeft + tankWidth, gyRight)
                    lineTo(canvasWidth, gyRight)
                    lineTo(canvasWidth, canvasHeight)
                    lineTo(0f, canvasHeight)
                    close()
                }
                drawPath(
                    path = groundPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(groundColorTop, groundColorBottom),
                        startY = topMargin,
                        endY = canvasHeight
                    )
                )
                
                if (groundY != null) {
                    drawLine(
                        color = Color(0xFF8B4513).copy(alpha = 0.5f),
                        start = Offset(0f, groundY),
                        end = Offset(canvasWidth, groundY),
                        strokeWidth = 4f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // --- 2. BACA ve FOSEPTİK ÇİZİMİ ---
                drawModernStructure(bacaLeft, mTop, bacaWidth, mAkar - mTop + with(density){10.dp.toPx()}, surfaceColor, borderColor, density, true)
                drawModernStructure(tankLeft, tTop, tankWidth, tBottom - tTop, surfaceColor, borderColor, density, false)

                // --- 3. BORU ÇİZİMİ ---
                val pipeStartX = bacaLeft + bacaWidth
                val pipeEndX = tankLeft
                val pipeStartY = mAkar - with(density) { 6.dp.toPx() }
                val pipeEndY = tAkar - with(density) { 6.dp.toPx() }
                val pipeThickness = with(density) { 14.dp.toPx() }
                val pipeColor = Color(0xFF3B82F6) // Blue for water/sewage

                drawLine(
                    color = pipeColor.copy(alpha = 0.7f),
                    start = Offset(pipeStartX, pipeStartY),
                    end = Offset(pipeEndX, pipeEndY),
                    strokeWidth = pipeThickness,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(pipeStartX, pipeStartY - 2f),
                    end = Offset(pipeEndX, pipeEndY - 2f),
                    strokeWidth = pipeThickness / 2f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                // --- 4. AKIŞ ANİMASYONU ---
                val dx = pipeEndX - pipeStartX
                val dy = pipeEndY - pipeStartY
                val length = sqrt(dx * dx + dy * dy)
                
                for (i in 0..2) {
                    var p = flowPhase + (i * 0.33f)
                    if (p > 1f) p -= 1f
                    
                    val arrowX = pipeStartX + dx * p
                    val arrowY = pipeStartY + dy * p
                    val arrowSize = with(density) { 6.dp.toPx() }
                    val ux = dx / length
                    val uy = dy / length

                    val arrowPath = Path().apply {
                        moveTo(arrowX + ux * arrowSize, arrowY + uy * arrowSize)
                        lineTo(arrowX - (ux * arrowSize + uy * arrowSize * 1.0f), arrowY - (uy * arrowSize - ux * arrowSize * 1.0f))
                        lineTo(arrowX - (ux * arrowSize - uy * arrowSize * 1.0f), arrowY - (uy * arrowSize + ux * arrowSize * 1.0f))
                        close()
                    }
                    val alpha = if (p < 0.2f) p * 5f else if (p > 0.8f) (1f - p) * 5f else 1f
                    drawPath(arrowPath, color = Color.White.copy(alpha = alpha * 0.9f))
                }

                // --- 5. ETİKETLER ---
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
                    val bgPaint = android.graphics.Paint().apply { color = bgAndroid; isAntiAlias = true }
                    val radius = with(density) { 8.dp.toPx() }
                    drawContext.canvas.nativeCanvas.drawRoundRect(rectL, rectT, rectR, rectB, radius, radius, bgPaint)
                    drawContext.canvas.nativeCanvas.drawText(text, cx, cy - (fontMetrics.descent + fontMetrics.ascent) / 2, paint)
                }

                drawContext.canvas.nativeCanvas.apply {
                    drawBadge("Baca", bacaLeft + bacaWidth / 2, mTop - with(density) { 16.dp.toPx() }, textPaint, badgeBgAndroid)
                    drawBadge("Foseptik", tankLeft + tankWidth / 2, tTop - with(density) { 16.dp.toPx() }, textPaint, badgeBgAndroid)

                    drawBadge("A: ${NumberParser.formatDecimal(manholeInvert)}", bacaLeft - with(density) { 24.dp.toPx() }, mAkar, dimPaint, android.graphics.Color.WHITE)
                    
                    drawBadge("A: ${NumberParser.formatDecimal(inletElev)}", tankLeft + tankWidth + with(density) { 24.dp.toPx() }, tAkar, dimPaint, android.graphics.Color.WHITE)
                    drawBadge("K: ${NumberParser.formatDecimal(coverElev)}", tankLeft + tankWidth + with(density) { 24.dp.toPx() }, tTop, dimPaint, android.graphics.Color.WHITE)
                    drawBadge("T: ${NumberParser.formatDecimal(bottomElev)}", tankLeft + tankWidth + with(density) { 24.dp.toPx() }, tBottom, dimPaint, badgeBgAndroid)

                    if (groundElev != null && groundY != null) {
                        drawBadge("Zemin: ${NumberParser.formatDecimal(groundElev)}", canvasWidth / 2, groundY - with(density){14.dp.toPx()}, dimPaint, android.graphics.Color.WHITE)
                    }

                    if (distance != null && distance > 0) {
                        drawBadge("◄── ${NumberParser.formatDecimal(distance)} m ──►", canvasWidth / 2, canvasHeight - with(density) { 8.dp.toPx() }, dimPaint, badgeBgAndroid)
                    }
                    if (slopeDecimal != null) {
                        drawBadge("Eğim: %${NumberParser.formatDecimal(slopeDecimal * 100)}", canvasWidth / 2, pipeStartY + dy/2 - with(density){16.dp.toPx()}, dimPaint, android.graphics.Color.WHITE)
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

private fun DrawScope.drawModernStructure(
    x: Float, y: Float, width: Float, height: Float,
    fillColor: Color, strokeColor: Color, density: androidx.compose.ui.unit.Density, isManhole: Boolean
) {
    val cornerRadius = CornerRadius(with(density) { 4.dp.toPx() }, with(density) { 4.dp.toPx() })
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
    
    // Rögar Kapağı
    drawLine(
        color = TextPrimary.copy(alpha = 0.6f),
        start = Offset(x - 6f, y),
        end = Offset(x + width + 6f, y),
        strokeWidth = 6f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
}
