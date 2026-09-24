import codecs
path = 'app/src/main/java/com/example/egimhesabi/ui/screens/ImpactCalculationScreen.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

new_ui = '''
@Composable
fun ImpactMapPicker(
    onDismiss: () -> Unit,
    onImport: (List<Pair<com.example.egimhesabi.domain.StakeoutCalculationSource, Double?>>) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val neighborhoodId = com.example.egimhesabi.ui.components.StakeoutSession.lastNeighborhoodId ?: return
    val recordsFlow = remember(context) { com.example.egimhesabi.data.AppDatabase.getInstance(context).stakeoutDao().allRecords() }
    val allRecords by recordsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val records = remember(allRecords, neighborhoodId) { allRecords.filter { it.manhole.neighborhoodId == neighborhoodId } }
    
    var basis by remember { mutableStateOf(com.example.egimhesabi.ui.components.StakeoutSession.lastBasis ?: com.example.egimhesabi.domain.StakeoutElevationBasis.PROJECT) }
    val selectedChain = remember { androidx.compose.runtime.mutableStateListOf<com.example.egimhesabi.data.StakeoutRecord>() }
    
    val validRecords = remember(records) { records.filter { it.manhole.projectX != null && it.manhole.projectY != null } }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Kapat", tint = TextPrimary)
                    }
                    Text("Haritadan Hat Seç", modifier = Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    if (selectedChain.isNotEmpty()) {
                        TextButton(onClick = {
                            val chainPairs = selectedChain.mapIndexed { i, record ->
                                val dist = if (i < selectedChain.size - 1) {
                                    val next = selectedChain[i + 1]
                                    val e1 = record.manhole
                                    val e2 = next.manhole
                                    if (e1.projectX != null && e1.projectY != null && e2.projectX != null && e2.projectY != null) {
                                        kotlin.math.hypot(e1.projectX - e2.projectX, e1.projectY - e2.projectY)
                                    } else null
                                } else null
                                com.example.egimhesabi.domain.StakeoutCalculationSource.from(record, basis) to dist
                            }
                            com.example.egimhesabi.ui.components.StakeoutSession.lastBasis = basis
                            onImport(chainPairs)
                            onDismiss()
                        }) {
                            Text("Aktar ()", fontWeight = FontWeight.Bold, color = AccentOrange)
                        }
                    }
                }
                
                // Basis Selector
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.egimhesabi.domain.StakeoutElevationBasis.entries.forEach { b ->
                        val selected = basis == b
                        Button(
                            onClick = { basis = b },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) AccentOrange else Color(0xFFF1F5F9),
                                contentColor = if (selected) Color.White else TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(b.label, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
                
                // Canvas Map
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (validRecords.isEmpty()) {
                        Text("Koordinatlı baca bulunamadı.", modifier = Modifier.align(Alignment.Center))
                    } else {
                        var scale by remember { mutableStateOf(1f) }
                        var offset by remember { mutableStateOf(Offset.Zero) }
                        val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
                        
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .androidx.compose.ui.input.pointer.pointerInput(Unit) {
                                    androidx.compose.foundation.gestures.detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.5f, 10f)
                                        offset += pan
                                    }
                                }
                                .androidx.compose.ui.input.pointer.pointerInput(Unit) {
                                    androidx.compose.foundation.gestures.detectTapGestures { tapOffset ->
                                        val minX = validRecords.minOf { it.manhole.projectX!! }
                                        val maxX = validRecords.maxOf { it.manhole.projectX!! }
                                        val minY = validRecords.minOf { it.manhole.projectY!! }
                                        val maxY = validRecords.maxOf { it.manhole.projectY!! }
                                        
                                        val rangeX = kotlin.math.max(maxX - minX, 1.0)
                                        val rangeY = kotlin.math.max(maxY - minY, 1.0)
                                        val padX = rangeX * 0.1
                                        val padY = rangeY * 0.1
                                        
                                        val adjMinX = minX - padX
                                        val adjMaxX = maxX + padX
                                        val adjMinY = minY - padY
                                        val adjMaxY = maxY + padY
                                        
                                        val scaleX = size.width / (adjMaxX - adjMinX).toFloat()
                                        val scaleY = size.height / (adjMaxY - adjMinY).toFloat()
                                        val baseScale = kotlin.math.min(scaleX, scaleY)
                                        
                                        val drawWidth = (adjMaxX - adjMinX).toFloat() * baseScale
                                        val drawHeight = (adjMaxY - adjMinY).toFloat() * baseScale
                                        val drawOffsetX = (size.width - drawWidth) / 2f
                                        val drawOffsetY = (size.height - drawHeight) / 2f
                                        
                                        var closest: com.example.egimhesabi.data.StakeoutRecord? = null
                                        var minDistance = Float.MAX_VALUE
                                        val threshold = 40.dp.toPx()
                                        
                                        validRecords.forEach { record ->
                                            val x = record.manhole.projectX!!
                                            val y = record.manhole.projectY!!
                                            
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
                                        
                                        if (closest != null) {
                                            if (selectedChain.lastOrNull()?.manhole?.id == closest.manhole.id) {
                                                selectedChain.removeLast()
                                            } else if (selectedChain.any { it.manhole.id == closest.manhole.id }) {
                                                // Already in chain but not last, ignore or remove? Let's ignore for safety.
                                            } else {
                                                selectedChain.add(closest)
                                            }
                                        }
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val minX = validRecords.minOf { it.manhole.projectX!! }
                                val maxX = validRecords.maxOf { it.manhole.projectX!! }
                                val minY = validRecords.minOf { it.manhole.projectY!! }
                                val maxY = validRecords.maxOf { it.manhole.projectY!! }
                                
                                val rangeX = kotlin.math.max(maxX - minX, 1.0)
                                val rangeY = kotlin.math.max(maxY - minY, 1.0)
                                val padX = rangeX * 0.1
                                val padY = rangeY * 0.1
                                
                                val adjMinX = minX - padX
                                val adjMaxX = maxX + padX
                                val adjMinY = minY - padY
                                val adjMaxY = maxY + padY
                                
                                val scaleX = size.width / (adjMaxX - adjMinX).toFloat()
                                val scaleY = size.height / (adjMaxY - adjMinY).toFloat()
                                val baseScale = kotlin.math.min(scaleX, scaleY)
                                
                                val drawOffsetX = (size.width - (adjMaxX - adjMinX).toFloat() * baseScale) / 2f
                                val drawOffsetY = (size.height - (adjMaxY - adjMinY).toFloat() * baseScale) / 2f
                                
                                // Draw lines between selected chain
                                for (i in 0 until selectedChain.size - 1) {
                                    val r1 = selectedChain[i].manhole
                                    val r2 = selectedChain[i + 1].manhole
                                    if (r1.projectX != null && r1.projectY != null && r2.projectX != null && r2.projectY != null) {
                                        val x1 = drawOffsetX + ((r1.projectX - adjMinX) * baseScale).toFloat()
                                        val y1 = drawOffsetY + ((adjMaxY - r1.projectY) * baseScale).toFloat()
                                        val fX1 = x1 * scale + offset.x
                                        val fY1 = y1 * scale + offset.y
                                        
                                        val x2 = drawOffsetX + ((r2.projectX - adjMinX) * baseScale).toFloat()
                                        val y2 = drawOffsetY + ((adjMaxY - r2.projectY) * baseScale).toFloat()
                                        val fX2 = x2 * scale + offset.x
                                        val fY2 = y2 * scale + offset.y
                                        
                                        val lineColor = Color(0xFF007AFF)
                                        val strokeW = 3.dp.toPx() * scale.coerceAtMost(2f)
                                        drawLine(lineColor, Offset(fX1, fY1), Offset(fX2, fY2), strokeWidth = strokeW)
                                        
                                        // Arrow
                                        val angle = kotlin.math.atan2(fY2 - fY1, fX2 - fX1)
                                        val arrowLen = 15.dp.toPx() * scale.coerceAtMost(2f)
                                        val a1 = angle + kotlin.math.PI / 6
                                        val a2 = angle - kotlin.math.PI / 6
                                        drawLine(lineColor, Offset(fX2, fY2), Offset(fX2 - arrowLen * kotlin.math.cos(a1).toFloat(), fY2 - arrowLen * kotlin.math.sin(a1).toFloat()), strokeWidth = strokeW)
                                        drawLine(lineColor, Offset(fX2, fY2), Offset(fX2 - arrowLen * kotlin.math.cos(a2).toFloat(), fY2 - arrowLen * kotlin.math.sin(a2).toFloat()), strokeWidth = strokeW)
                                    }
                                }
                                
                                validRecords.forEach { record ->
                                    val x = record.manhole.projectX!!
                                    val y = record.manhole.projectY!!
                                    val canvasX = drawOffsetX + ((x - adjMinX) * baseScale).toFloat()
                                    val canvasY = drawOffsetY + ((adjMaxY - y) * baseScale).toFloat()
                                    val finalX = canvasX * scale + offset.x
                                    val finalY = canvasY * scale + offset.y
                                    
                                    val chainIndex = selectedChain.indexOfFirst { it.manhole.id == record.manhole.id }
                                    val isSelected = chainIndex != -1
                                    
                                    if (isSelected) {
                                        drawCircle(Color(0xFF007AFF).copy(alpha = 0.3f), 15.dp.toPx() * scale.coerceAtMost(2f), Offset(finalX, finalY))
                                    }
                                    
                                    drawCircle(if (isSelected) Color(0xFF007AFF) else Color(0xFF94A3B8), 6.dp.toPx() * scale.coerceAtMost(2f), Offset(finalX, finalY))
                                    
                                    val textLayout = textMeasurer.measure(
                                        if (isSelected) ". " else record.manhole.name,
                                        androidx.compose.ui.text.TextStyle(
                                            color = if (isSelected) Color(0xFF007AFF) else TextSecondary,
                                            fontSize = (if (isSelected) 13.sp else 11.sp).toPx().toSp() * scale.coerceAtMost(1.5f),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                    androidx.compose.ui.text.drawText(textLayout, topLeft = Offset(finalX + 10.dp.toPx() * scale, finalY - textLayout.size.height / 2f))
                                }
                            }
                            
                            // Floating action hints
                            Row(
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).background(Color.Black.copy(alpha=0.7f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedChain.isEmpty()) {
                                    Text("Hattın başlangıç bacasına dokunun", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                } else {
                                    Text(" baca seçildi. Sıradakine dokunun.", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    TextButton(onClick = { selectedChain.clear() }) { Text("Sıfırla", color = AccentOrange) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
'''

content = content + '\n' + new_ui
with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
print('Appended ImpactMapPicker to ImpactCalculationScreen.kt')