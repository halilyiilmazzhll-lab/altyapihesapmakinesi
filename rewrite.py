import codecs
path = 'app/src/main/java/com/example/egimhesabi/ui/components/StakeoutMapView.kt'
code = '''package com.example.egimhesabi.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.egimhesabi.data.StakeoutManholeEntity
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.util.NumberParser
import kotlin.math.max

@Composable
fun StakeoutMapView(
    records: List<StakeoutManholeEntity>,
    onUpdateConnection: (Long, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val validRecords = remember(records) {
        records.filter { it.projectX != null && it.projectY != null }
    }

    if (validRecords.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Haritada gösterilecek koordinatlı baca bulunamadı.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        return
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selectedRecord by remember { mutableStateOf<StakeoutManholeEntity?>(null) }
    var sourceRecord by remember { mutableStateOf<StakeoutManholeEntity?>(null) }
    var isConnectionMode by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 10f)
                    offset += pan
                }
            }
            .pointerInput(isConnectionMode, sourceRecord) {
                detectTapGestures { tapOffset ->
                    val minX = validRecords.minOf { it.projectX!! }
                    val maxX = validRecords.maxOf { it.projectX!! }
                    val minY = validRecords.minOf { it.projectY!! }
                    val maxY = validRecords.maxOf { it.projectY!! }
                    
                    val rangeX = max(maxX - minX, 1.0)
                    val rangeY = max(maxY - minY, 1.0)
                    val padX = rangeX * 0.1
                    val padY = rangeY * 0.1
                    
                    val adjMinX = minX - padX
                    val adjMaxX = maxX + padX
                    val adjMinY = minY - padY
                    val adjMaxY = maxY + padY
                    
                    val totalRangeX = adjMaxX - adjMinX
                    val totalRangeY = adjMaxY - adjMinY
                    
                    val scaleX = size.width / totalRangeX.toFloat()
                    val scaleY = size.height / totalRangeY.toFloat()
                    val baseScale = minOf(scaleX, scaleY)
                    
                    val drawWidth = totalRangeX.toFloat() * baseScale
                    val drawHeight = totalRangeY.toFloat() * baseScale
                    val drawOffsetX = (size.width - drawWidth) / 2f
                    val drawOffsetY = (size.height - drawHeight) / 2f
                    
                    var closest: StakeoutManholeEntity? = null
                    var minDistance = Float.MAX_VALUE
                    val threshold = 40.dp.toPx()
                    
                    validRecords.forEach { record ->
                        val x = record.projectX!!
                        val y = record.projectY!!
                        
                        val canvasX = drawOffsetX + ((x - adjMinX) * baseScale).toFloat()
                        val canvasY = drawOffsetY + ((adjMaxY - y) * baseScale).toFloat()
                        
                        val finalX = canvasX * scale + offset.x
                        val finalY = canvasY * scale + offset.y
                        
                        val dist = kotlin.math.hypot(finalX - tapOffset.x, finalY - tapOffset.y)
                        if (dist < minDistance && dist < threshold) {
                            minDistance = dist
                            closest = record
                        }
                    }
                    
                    if (isConnectionMode) {
                        if (closest != null) {
                            if (sourceRecord == null) {
                                sourceRecord = closest
                            } else {
                                if (sourceRecord!!.id == closest!!.id) {
                                    sourceRecord = null
                                } else {
                                    onUpdateConnection(sourceRecord!!.id, closest!!.nameKey)
                                    sourceRecord = closest
                                }
                            }
                        } else {
                            sourceRecord = null
                        }
                    } else {
                        selectedRecord = closest
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minX = validRecords.minOf { it.projectX!! }
            val maxX = validRecords.maxOf { it.projectX!! }
            val minY = validRecords.minOf { it.projectY!! }
            val maxY = validRecords.maxOf { it.projectY!! }

            val rangeX = max(maxX - minX, 1.0)
            val rangeY = max(maxY - minY, 1.0)
            
            val padX = rangeX * 0.1
            val padY = rangeY * 0.1
            
            val adjMinX = minX - padX
            val adjMaxX = maxX + padX
            val adjMinY = minY - padY
            val adjMaxY = maxY + padY
            
            val totalRangeX = adjMaxX - adjMinX
            val totalRangeY = adjMaxY - adjMinY
            
            val scaleX = size.width / totalRangeX.toFloat()
            val scaleY = size.height / totalRangeY.toFloat()
            val baseScale = minOf(scaleX, scaleY)
            
            val drawWidth = totalRangeX.toFloat() * baseScale
            val drawHeight = totalRangeY.toFloat() * baseScale
            val drawOffsetX = (size.width - drawWidth) / 2f
            val drawOffsetY = (size.height - drawHeight) / 2f

            // First pass: Draw connections
            validRecords.forEach { record ->
                val targetKey = record.connectedToNameKey
                if (targetKey != null) {
                    val target = validRecords.find { it.nameKey == targetKey }
                    if (target != null) {
                        val x1 = record.projectX!!
                        val y1 = record.projectY!!
                        val canvasX1 = drawOffsetX + ((x1 - adjMinX) * baseScale).toFloat()
                        val canvasY1 = drawOffsetY + ((adjMaxY - y1) * baseScale).toFloat()
                        val finalX1 = canvasX1 * scale + offset.x
                        val finalY1 = canvasY1 * scale + offset.y

                        val x2 = target.projectX!!
                        val y2 = target.projectY!!
                        val canvasX2 = drawOffsetX + ((x2 - adjMinX) * baseScale).toFloat()
                        val canvasY2 = drawOffsetY + ((adjMaxY - y2) * baseScale).toFloat()
                        val finalX2 = canvasX2 * scale + offset.x
                        val finalY2 = canvasY2 * scale + offset.y

                        val lineColor = Color(0xFF007AFF).copy(alpha = 0.6f)
                        val strokeW = 2.dp.toPx() * scale.coerceAtMost(2f)

                        drawLine(
                            color = lineColor,
                            start = Offset(finalX1, finalY1),
                            end = Offset(finalX2, finalY2),
                            strokeWidth = strokeW
                        )
                        
                        val angle = kotlin.math.atan2(finalY2 - finalY1, finalX2 - finalX1)
                        val arrowLen = 12.dp.toPx() * scale.coerceAtMost(2f)
                        val angle1 = angle + kotlin.math.PI / 6
                        val angle2 = angle - kotlin.math.PI / 6
                        val p1 = Offset(finalX2 - arrowLen * kotlin.math.cos(angle1).toFloat(), finalY2 - arrowLen * kotlin.math.sin(angle1).toFloat())
                        val p2 = Offset(finalX2 - arrowLen * kotlin.math.cos(angle2).toFloat(), finalY2 - arrowLen * kotlin.math.sin(angle2).toFloat())
                        
                        drawLine(color = lineColor, start = Offset(finalX2, finalY2), end = p1, strokeWidth = strokeW)
                        drawLine(color = lineColor, start = Offset(finalX2, finalY2), end = p2, strokeWidth = strokeW)
                    }
                }
            }

            // Second pass: Draw manholes
            validRecords.forEach { record ->
                val x = record.projectX!!
                val y = record.projectY!!
                
                val canvasX = drawOffsetX + ((x - adjMinX) * baseScale).toFloat()
                val canvasY = drawOffsetY + ((adjMaxY - y) * baseScale).toFloat()
                
                val finalX = canvasX * scale + offset.x
                val finalY = canvasY * scale + offset.y

                val pInvert = record.projectInvertLevel
                val tInvert = record.terrainInvertLevel
                val hasEmirDefteri = pInvert != null && tInvert != null && pInvert > tInvert
                
                val isSelected = selectedRecord?.id == record.id
                val isSource = sourceRecord?.id == record.id
                val dotColor = if (hasEmirDefteri) Color(0xFFD94A4A) else Color(0xFF4CAF50)
                
                // Highlight selected or source
                if (isSelected || isSource) {
                    drawCircle(
                        color = if (isSource) Color(0xFF007AFF).copy(alpha = 0.3f) else dotColor.copy(alpha = 0.3f),
                        radius = 12.dp.toPx() * scale.coerceAtMost(2f),
                        center = Offset(finalX, finalY)
                    )
                }

                drawCircle(
                    color = dotColor,
                    radius = 5.dp.toPx() * scale.coerceAtMost(2f),
                    center = Offset(finalX, finalY)
                )
                
                val textLayoutResult = textMeasurer.measure(
                    text = record.name,
                    style = TextStyle(
                        color = if (isSelected || isSource) TextPrimary else TextSecondary,
                        fontSize = (11.sp.toPx() * scale.coerceAtMost(1.5f)).toSp(),
                        fontWeight = if (isSelected || isSource) FontWeight.Bold else FontWeight.SemiBold
                    )
                )
                
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(finalX + 8.dp.toPx() * scale, finalY - textLayoutResult.size.height / 2f)
                )
            }
        }
        
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            // Connection Mode Toggle
            Row(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bağlantı Çiz",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnectionMode) Color(0xFF007AFF) else TextSecondary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = isConnectionMode,
                    onCheckedChange = { 
                        isConnectionMode = it
                        if (!it) sourceRecord = null
                        if (it) selectedRecord = null
                    },
                    modifier = Modifier.height(24.dp),
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF007AFF))
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            // Reset Button
            Text(
                text = "Sıfırla",
                color = AccentOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .pointerInput(Unit) { detectTransformGestures { _, _, _, _ -> } }
                    .clickable {
                        scale = 1f
                        offset = Offset.Zero
                        selectedRecord = null
                        sourceRecord = null
                    }
            )
        }
        
        // Info Card
        if (!isConnectionMode) {
            selectedRecord?.let { record ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(0.9f)
                        .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) { detectTapGestures { } } // consume taps
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = record.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            IconButton(
                                onClick = { selectedRecord = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = TextSecondary)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val pInvert = record.projectInvertLevel
                        val tInvert = record.terrainInvertLevel
                        val hasEmirDefteri = pInvert != null && tInvert != null && pInvert > tInvert
                        
                        if (hasEmirDefteri) {
                            Text(
                                "Emir Defteri Var",
                                color = Color(0xFFD94A4A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                "Proje akar kotu, arazi akardan  m daha yüksek.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        } else {
                            Text(
                                "Emir Defteri Yok",
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                Text("Proje Akar", color = TextSecondary, fontSize = 11.sp)
                                Text(pInvert?.let { NumberParser.formatDecimal(it) } ?: "Eksik", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Arazi Akar", color = TextSecondary, fontSize = 11.sp)
                                Text(tInvert?.let { NumberParser.formatDecimal(it) } ?: "Eksik", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        } else {
            // Helper text for connection mode
            Text(
                text = if (sourceRecord == null) "Bağlantının başlayacağı bacaya dokunun" else "Bağlanacak hedef bacaya dokunun (Temizlemek için kendisine tekrar dokunun)",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .background(Color(0xFF007AFF).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
'''
with codecs.open(path, 'w', 'utf-8') as f:
    f.write(code)
print('Successfully rewrote StakeoutMapView.kt')