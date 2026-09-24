package com.example.egimhesabi.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.egimhesabi.data.StakeoutManholeEntity
import com.example.egimhesabi.domain.*
import java.util.Locale
import kotlin.math.*

private val CadInk = Color(0xFF1A2332)
private val CadBlue = Color(0xFF125BB5)
private val CadWarning = Color(0xFF965200)
private val CadError = Color(0xFFB42318)
private data class CadChange(val id: Long, val before: String?, val after: String?)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StakeoutMapView(records: List<StakeoutManholeEntity>,
    onUpdateConnection: (Long, String?, (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier, busy: Boolean = false) {
    var acknowledged by remember { mutableStateOf<Map<Long,String?>>(emptyMap()) }
    val effectiveRecords = remember(records, acknowledged) { records.map { if (acknowledged.containsKey(it.id)) it.copy(connectedToNameKey=acknowledged[it.id]) else it } }
    val valid = remember(effectiveRecords) { effectiveRecords.filter { it.projectX?.isFinite() == true && it.projectY?.isFinite() == true } }
    val byId = remember(effectiveRecords) { effectiveRecords.associateBy { it.id } }
    val byName = remember(effectiveRecords) { effectiveRecords.associateBy { it.nameKey } }
    val neighborhood = records.firstOrNull()?.neighborhoodId
    var zoom by rememberSaveable(neighborhood) { mutableDoubleStateOf(1.0) }
    var panX by rememberSaveable(neighborhood) { mutableDoubleStateOf(0.0) }
    var panY by rememberSaveable(neighborhood) { mutableDoubleStateOf(0.0) }
    var selectedId by rememberSaveable(neighborhood) { mutableStateOf<Long?>(null) }
    var sourceId by rememberSaveable(neighborhood) { mutableStateOf<Long?>(null) }
    var lineId by rememberSaveable(neighborhood) { mutableStateOf<Long?>(null) }
    var drawing by rememberSaveable(neighborhood) { mutableStateOf(false) }
    var names by rememberSaveable { mutableStateOf(true) }
    var levels by rememberSaveable { mutableStateOf(false) }
    var terrainLevels by rememberSaveable { mutableStateOf(false) }
    var arrows by rememberSaveable { mutableStateOf(true) }
    var smartLabels by rememberSaveable { mutableStateOf(true) }
    var grid by rememberSaveable { mutableStateOf(false) }
    var showLayers by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var candidates by remember { mutableStateOf<List<Long>?>(null) }
    var pendingChange by remember { mutableStateOf<CadChange?>(null) }
    var undo by remember { mutableStateOf<List<CadChange>>(emptyList()) }
    var saving by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    var savedWidth by rememberSaveable(neighborhood) { mutableIntStateOf(0) }
    var savedHeight by rememberSaveable(neighborhood) { mutableIntStateOf(0) }
    var canvasSize by remember { mutableStateOf(IntSize(savedWidth,savedHeight)) }
    val locked = busy || saving
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 512)
    val labelHits = remember { mutableMapOf<Long,Rect>() }
    val renderOrder = remember(valid, selectedId, sourceId) { valid.sortedByDescending { it.id==selectedId||it.id==sourceId } }
    val bounds = remember(valid) {
        if (valid.isEmpty()) listOf(0.0, 1.0, 1.0, 1.0) else {
            val east = valid.minOf { it.projectY!! }; val north = valid.maxOf { it.projectX!! }
            val rx = max(valid.maxOf { it.projectY!! } - east, 1.0)
            val ry = max(north - valid.minOf { it.projectX!! }, 1.0)
            listOf(east - rx * .15, north + ry * .15, rx * 1.3, ry * 1.3)
        }
    }
    val viewport = remember(bounds, canvasSize) { CadViewport(bounds[0], bounds[1], bounds[2], bounds[3],
        canvasSize.width.coerceAtLeast(1).toDouble(), canvasSize.height.coerceAtLeast(1).toDouble()) }
    fun screen(r: StakeoutManholeEntity) = viewport.project(r.projectY!!, r.projectX!!, zoom, CadPoint(panX,panY))
    fun fit() { zoom = 1.0; panX = 0.0; panY = 0.0 }
    fun focus(r: StakeoutManholeEntity) {
        if (r.projectX == null || r.projectY == null) { feedback = "Bu bacanın X/Y koordinatı eksik."; return }
        val p = viewport.project(r.projectY, r.projectX, zoom, CadPoint(0.0,0.0))
        panX = canvasSize.width / 2.0 - p.x; panY = canvasSize.height / 2.0 - p.y
    }
    fun zoomAt(ratio: Double) {
        val next = (zoom * ratio).coerceIn(.2, 100.0)
        val p = CadGeometry.transformPan(CadPoint(panX,panY), CadPoint(canvasSize.width/2.0,canvasSize.height/2.0),CadPoint(0.0,0.0),next/zoom)
        panX=p.x; panY=p.y; zoom=next
    }
    fun commit(change: CadChange, undoing: Boolean = false) {
        if (saving || busy) return
        saving = true; feedback = "Bağlantı kaydediliyor…"
        onUpdateConnection(change.id, change.after) { success ->
            saving = false
            if (success) {
                acknowledged = acknowledged + (change.id to change.after)
                undo = if (undoing) undo.dropLast(1) else (undo + change).takeLast(50)
                feedback = if (undoing) "Son bağlantı işlemi geri alındı." else "Bağlantı kaydedildi."
                if (drawing) sourceId = change.after?.let { byName[it]?.id } ?: change.id
                if (change.after == null) lineId = null
            } else feedback = "Bağlantı kaydedilemedi. Başlangıç korunuyor; tekrar deneyin."
        }
    }
    fun choose(id: Long) {
        if (saving || busy) return
        val r = byId[id] ?: return
        selectedId = id; lineId = null
        if (drawing) {
            val source = sourceId?.let { byId[it] }
            if (source == null) sourceId = id
            else if (source.id == id) sourceId = null
            else if (source.connectedToNameKey == r.nameKey) { sourceId=id; feedback="Bu bağlantı zaten var; zincire devam edebilirsiniz." }
            else {
                val change = CadChange(source.id,source.connectedToNameKey,r.nameKey)
                if (change.before != null) pendingChange = change else commit(change)
            }
        }
    }
    LaunchedEffect(records) {
        acknowledged = acknowledged.filter { (id,target) -> records.any { it.id==id && it.connectedToNameKey!=target } }
        if (selectedId !in byId) selectedId=null
        if (sourceId !in byId) sourceId=null
        if (lineId !in byId) lineId=null
    }
    var previousCanvasSize by remember { mutableStateOf(canvasSize) }
    LaunchedEffect(canvasSize) {
        if (previousCanvasSize != canvasSize) selectedId?.let { byId[it] }?.let { focus(it) }
        previousCanvasSize = canvasSize
    }
    Column(modifier.fillMaxSize().background(Color.White)) {
        FlowRow(Modifier.fillMaxWidth().padding(horizontal=8.dp), horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            TextButton(onClick={query=""; candidates=null; choosing=true}, enabled=!locked) { Text(if(drawing) "Baca seç" else "Baca bul") }
            TextButton(onClick={drawing=!drawing; sourceId=null; lineId=null}, enabled=!locked) { Text(if(drawing) "Çizimi bitir" else "Bağlantı çiz") }
            TextButton(onClick={selectedId=null; lineId=null; fit()}) { Text("Tümünü göster") }
            TextButton(onClick={showLayers=true}) { Text("Katmanlar") }
            TextButton(onClick={undo.lastOrNull()?.let { commit(CadChange(it.id,it.after,it.before),true) }}, enabled=undo.isNotEmpty()&&!locked) { Text("Geri al") }
        }
        if (drawing) Text(if(sourceId==null) "Başlangıç bacasını seçin." else "Başlangıç: ${byId[sourceId]?.name.orEmpty()} → Hedef bacayı seçin.",Modifier.padding(horizontal=12.dp,vertical=4.dp),color=CadBlue)
        if (feedback.isNotBlank()) Text(feedback,Modifier.padding(horizontal=12.dp,vertical=4.dp),color=CadInk,fontSize=12.sp)
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("↑ Kuzey / X   → Doğu / Y",fontSize=12.sp,color=CadInk,modifier=Modifier.weight(1f).padding(top=12.dp))
            TextButton(onClick={zoomAt(1/1.5)},modifier=Modifier.semantics { contentDescription="Uzaklaştır" }) { Text("−") }
            TextButton(onClick={zoomAt(1.5)},modifier=Modifier.semantics { contentDescription="Yakınlaştır" }) { Text("+") }
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFFF0F4F9)).onSizeChanged { nextSize ->
            if (canvasSize.width > 0 && canvasSize.height > 0 && nextSize.width > 0 && nextSize.height > 0) {
                val east = viewport.minEast + (((viewport.width/2-panX)/zoom - (viewport.width-viewport.eastRange*viewport.baseScale)/2)/viewport.baseScale)
                val north = viewport.maxNorth - (((viewport.height/2-panY)/zoom - (viewport.height-viewport.northRange*viewport.baseScale)/2)/viewport.baseScale)
                val next = viewport.copy(width=nextSize.width.toDouble(),height=nextSize.height.toDouble())
                val p = next.project(east,north,zoom,CadPoint(0.0,0.0))
                panX=next.width/2-p.x; panY=next.height/2-p.y
            }
            canvasSize=nextSize
            savedWidth=nextSize.width; savedHeight=nextSize.height
        }) {
            val latestTransform by rememberUpdatedState<(Offset,Offset,Float)->Unit>({ center,pan,z ->
                val next=(zoom*z).coerceIn(.2,100.0)
                val p=CadGeometry.transformPan(CadPoint(panX,panY),CadPoint(center.x.toDouble(),center.y.toDouble()),CadPoint(pan.x.toDouble(),pan.y.toDouble()),next/zoom)
                panX=p.x; panY=p.y; zoom=next
            })
            val latestTap by rememberUpdatedState<(Offset)->Unit>({ tap ->
                if (!locked) {
                    val threshold=with(density){24.dp.toPx()}
                    val hits=valid.map { it to screen(it) }.filter { hypot(it.second.x-tap.x,it.second.y-tap.y)<=threshold || labelHits[it.first.id]?.contains(tap)==true }.sortedBy { hypot(it.second.x-tap.x,it.second.y-tap.y) }
                    if(hits.size>1) { candidates=hits.map { it.first.id }; query=""; choosing=true }
                    else if(hits.size==1) choose(hits.first().first.id)
                    else if(!drawing) {
                        selectedId=null
                        lineId=valid.mapNotNull { a -> byName[a.connectedToNameKey]?.takeIf { it.projectX!=null && it.projectY!=null }?.let { b ->
                            a.id to CadGeometry.segmentDistance(CadPoint(tap.x.toDouble(),tap.y.toDouble()),screen(a),screen(b))
                        }}.filter { it.second<with(density){12.dp.toPx()} }.minByOrNull { it.second }?.first
                    }
                }
            })
            Canvas(Modifier.fillMaxSize().semantics { contentDescription="CAD harita. Nesne seçmek için Baca bul veya Baca seç düğmesini kullanın." }
                .pointerInput(Unit){detectTransformGestures { c,p,z,_->latestTransform(c,p,z) }}
                .pointerInput(Unit){detectTapGestures { latestTap(it) }}) {
                val ppm=viewport.baseScale*zoom
                fun point(r:StakeoutManholeEntity):Offset { val p=screen(r); return Offset(p.x.toFloat(),p.y.toFloat()) }
                if(grid) {
                    val step=CadGeometry.scaleMeters(ppm,80.dp.toPx().toDouble())
                    val origin=viewport.project(0.0,0.0,zoom,CadPoint(panX,panY));val stepPx=(step*ppm).toFloat()
                    var x=((origin.x%stepPx+stepPx)%stepPx).toFloat()
                    while(x<size.width) { drawLine(Color(0xFFE2E7EE),Offset(x,0f),Offset(x,size.height)); x+=stepPx }
                    var y=((origin.y%stepPx+stepPx)%stepPx).toFloat()
                    while(y<size.height) { drawLine(Color(0xFFE2E7EE),Offset(0f,y),Offset(size.width,y)); y+=stepPx }
                }
                valid.forEach { a ->
                    val b=byName[a.connectedToNameKey]?.takeIf { it.projectX!=null&&it.projectY!=null } ?: return@forEach
                    val p=point(a);val q=point(b)
                    if(max(p.x,q.x)<0||min(p.x,q.x)>size.width||max(p.y,q.y)<0||min(p.y,q.y)>size.height) return@forEach
                    val active = lineId==a.id || selectedId==a.id || selectedId==b.id || sourceId==a.id
                    val color=if(lineId==a.id) CadWarning else CadBlue.copy(alpha=if(active) 1f else .55f)
                    drawLine(color,p,q,if(active) 3.dp.toPx() else 1.5.dp.toPx(),cap=StrokeCap.Round)
                    if(arrows && (p-q).getDistance()>(if(smartLabels&&!active) 90.dp.toPx() else 30.dp.toPx())) {
                        val tip=p+(q-p)*.6f
                        val wings=CadGeometry.arrowWings(CadPoint(p.x.toDouble(),p.y.toDouble()),CadPoint(tip.x.toDouble(),tip.y.toDouble()),10.dp.toPx().toDouble())
                        drawPath(Path().apply { moveTo(wings.first.x.toFloat(),wings.first.y.toFloat());lineTo(tip.x,tip.y);lineTo(wings.second.x.toFloat(),wings.second.y.toFloat()) },color,style=Stroke(2.dp.toPx()))
                    }
                }
                val occupied=mutableListOf<Rect>()
                val visible = renderOrder.map { it to point(it) }.filter { (_,p) -> p.x in 0f..size.width && p.y in 0f..size.height }
                var detailedLabels = 0
                var measuredLabels = 0
                // Leave room around every symbol, not just around previously placed text.
                val symbols = visible.map { (_,p) -> Rect(p-Offset(10.dp.toPx(),10.dp.toPx()),Size(20.dp.toPx(),20.dp.toPx())) }
                labelHits.clear()
                visible.forEach { (r,p) ->
                    val selected=r.id==selectedId||r.id==sourceId
                    val missing=r.projectCoverLevel==null||r.projectInvertLevel==null
                    val terrainInvert = r.terrainInvertLevel
                    val warning=r.projectInvertLevel!=null&&terrainInvert!=null&&r.projectInvertLevel>terrainInvert
                    val color=when { selected->CadBlue;missing->CadError;warning->CadWarning;else->CadInk }
                    if(selected) drawCircle(CadBlue.copy(alpha=.16f),18.dp.toPx(),p)
                    drawCircle(Color.White,8.dp.toPx(),p);drawCircle(color,6.dp.toPx(),p)
                    val detail = !smartLabels || selected || (zoom>=3.0 && visible.size<=30 && detailedLabels<8)
                    if((names||selected||((levels||terrainLevels)&&detail)) && (measuredLabels<100||selected)) {
                        measuredLabels++
                        val heading=(if(missing) "! " else if(warning) "△ " else "")+r.name
                        val detailLines=listOfNotNull(
                            if(levels&&detail) "Proje K ${cadNumber(r.projectCoverLevel)} · A ${cadNumber(r.projectInvertLevel)}" else null,
                            if(terrainLevels&&detail) "Arazi K ${cadNumber(r.terrainGroundLevel)} · A ${cadNumber(r.terrainInvertLevel)}" else null)
                        val label=(listOf(heading)+detailLines).joinToString("\n")
                        var layout=measurer.measure(label,TextStyle(color=color,fontSize=if(selected) 12.sp else 11.sp,fontWeight=if(selected) FontWeight.Bold else FontWeight.Normal),maxLines=3,
                            overflow=TextOverflow.Ellipsis,constraints=Constraints(maxWidth=(size.width-32.dp.toPx()).coerceAtLeast(1f).toInt()))
                        fun place(w:Float,h:Float):Rect? {
                            val gap=14.dp.toPx()
                            val rects=listOf(Offset(p.x+gap,p.y-h/2),Offset(p.x-gap-w,p.y-h/2),Offset(p.x-w/2,p.y-gap-h),Offset(p.x-w/2,p.y+gap)).map { Rect(it,Size(w,h)) }
                            return rects.firstOrNull { candidate -> candidate.left>=4.dp.toPx()&&candidate.top>=4.dp.toPx()&&candidate.right<=size.width-4.dp.toPx()&&candidate.bottom<=size.height-64.dp.toPx()&&occupied.none { it.overlaps(candidate.inflate(5.dp.toPx())) }&&symbols.none { it.overlaps(candidate) } }
                        }
                        var rect=place(layout.size.width.toFloat(),layout.size.height.toFloat())
                        if(rect==null&&detailLines.isNotEmpty()) {
                            layout=measurer.measure(heading,TextStyle(color=color,fontSize=12.sp,fontWeight=if(selected) FontWeight.Bold else FontWeight.Normal),maxLines=1,
                                overflow=TextOverflow.Ellipsis,constraints=Constraints(maxWidth=(size.width-32.dp.toPx()).coerceAtLeast(1f).toInt()))
                            rect=place(layout.size.width.toFloat(),layout.size.height.toFloat())
                        } else if(rect!=null&&detailLines.isNotEmpty()) detailedLabels++
                        rect?.let { occupied.add(it);labelHits[r.id]=it
                            drawRoundRect(Color.White.copy(alpha=if(selected) .96f else .75f),it.topLeft-Offset(3.dp.toPx(),2.dp.toPx()),Size(it.width+6.dp.toPx(),it.height+4.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                            drawText(layout,topLeft=it.topLeft) }
                    }
                }
                val meters=CadGeometry.scaleMeters(ppm,size.width*.20)
                val width=(meters*ppm).toFloat();val left=16.dp.toPx();val bottom=size.height-18.dp.toPx()
                val label=measurer.measure(if(meters>=1000) "${cadNumber(meters/1000)} km" else "${cadNumber(meters)} m",TextStyle(color=CadInk,fontSize=12.sp))
                drawRect(Color.White,Offset(left-6.dp.toPx(),bottom-label.size.height-12.dp.toPx()),Size(max(width,label.size.width.toFloat())+12.dp.toPx(),label.size.height+24.dp.toPx()))
                drawText(label,topLeft=Offset(left,bottom-label.size.height-6.dp.toPx()))
                drawLine(CadInk,Offset(left,bottom),Offset(left+width,bottom),2.dp.toPx())
                drawLine(CadInk,Offset(left,bottom-4.dp.toPx()),Offset(left,bottom+4.dp.toPx()),2.dp.toPx())
                drawLine(CadInk,Offset(left+width,bottom-4.dp.toPx()),Offset(left+width,bottom+4.dp.toPx()),2.dp.toPx())
            }
            if(valid.isEmpty()) Text("Koordinatlı baca yok. X/Y alanlarını kontrol edin.",Modifier.padding(24.dp),color=CadInk)
        }
        val record=byId[selectedId];val line=byId[lineId]
        if(record!=null||line!=null) Column(Modifier.fillMaxWidth().heightIn(max=190.dp).verticalScroll(rememberScrollState()).padding(12.dp)) {
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(record?.name ?: "${line?.name} → ${byName[line?.connectedToNameKey]?.name.orEmpty()}",fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                TextButton(onClick={selectedId=null;lineId=null}) { Text("Kapat") }
            }
            if(record!=null) {
                Text("X: ${cadNumber(record.projectX)}   Y: ${cadNumber(record.projectY)}",fontSize=12.sp)
                Text("Kapak: ${cadNumber(record.projectCoverLevel)} m • Proje akar: ${cadNumber(record.projectInvertLevel)} m",fontSize=14.sp)
                Text("Arazi: ${cadNumber(record.terrainGroundLevel)} m • Arazi akar: ${cadNumber(record.terrainInvertLevel)} m",fontSize=14.sp)
                if(record.projectCoverLevel==null||record.projectInvertLevel==null) Text("! Kot verisi eksik",color=CadError)
                val terrainInvert = record.terrainInvertLevel
                if(record.projectInvertLevel!=null&&terrainInvert!=null&&record.projectInvertLevel>terrainInvert) Text("△ Proje akar kotu arazi akar kotundan yüksek",color=CadWarning)
                Text("Bağlı baca: ${byName[record.connectedToNameKey]?.name ?: "Yok"}")
                FlowRow {
                    TextButton(onClick={focus(record)}) { Text("Odaklan") }
                    TextButton(onClick={drawing=true;sourceId=record.id},enabled=!locked) { Text("Buradan bağlantı çiz") }
                    if(record.connectedToNameKey!=null) TextButton(onClick={pendingChange=CadChange(record.id,record.connectedToNameKey,null)},enabled=!locked) { Text("Bağlantıyı kaldır") }
                }
            } else if(line!=null) {
                val target=byName[line.connectedToNameKey]
                if(target?.projectX!=null&&target.projectY!=null&&line.projectX!=null&&line.projectY!=null) {
                    val distance=hypot(target.projectX-line.projectX,target.projectY-line.projectY)
                    Text("Yatay mesafe: ${cadNumber(distance)} m")
                    if(distance>0&&line.projectInvertLevel!=null&&target.projectInvertLevel!=null) Text("Bağlantı yönünde kot düşümü: %${cadNumber((line.projectInvertLevel-target.projectInvertLevel)/distance*100)}")
                    Text("Ok bağlantı yönünü gösterir; otomatik akış yönü değildir.",fontSize=12.sp)
                }
                TextButton(onClick={pendingChange=CadChange(line.id,line.connectedToNameKey,null)},enabled=!locked) { Text("Bağlantıyı kaldır") }
            }
        }
    }
    if(choosing) AlertDialog(onDismissRequest={choosing=false},title={Text(if(candidates==null) "Baca bul ve seç" else "Birden fazla baca var")},text={
        Column {
            OutlinedTextField(query,{query=it},label={Text("Baca adı")},singleLine=true)
            val results=records.filter { (candidates==null||it.id in candidates.orEmpty())&&it.name.contains(query,true) }
            LazyColumn(Modifier.heightIn(max=320.dp)) {
                items(results,key={it.id}) { r -> TextButton(onClick={choosing=false;choose(r.id);focus(r)},enabled=!locked,modifier=Modifier.fillMaxWidth()) { Text(r.name+if(r.projectX==null||r.projectY==null) " • Koordinat eksik" else "") } }
                if(results.isEmpty()) item { Text("Eşleşen baca bulunamadı.",Modifier.padding(12.dp)) }
            }
        }
    },confirmButton={TextButton(onClick={choosing=false}) { Text("Kapat") }})
    if(showLayers) AlertDialog(onDismissRequest={showLayers=false},title={Text("Harita katmanları")},text={Column(Modifier.verticalScroll(rememberScrollState())) {
        Row { Checkbox(smartLabels,{smartLabels=it});Text("Sade görünüm (önerilen)",Modifier.padding(top=12.dp)) }
        Text("Genel görünümde baca adları gösterilir. Açık kot katmanları bacayı seçince veya yeterince yakınlaşınca görünür; tüm değerler baca panelinde bulunur.",fontSize=13.sp)
        Row { Checkbox(names,{names=it});Text("Baca adları",Modifier.padding(top=12.dp)) }
        Row { Checkbox(levels,{levels=it});Text("Proje kotları",Modifier.padding(top=12.dp)) }
        Row { Checkbox(terrainLevels,{terrainLevels=it});Text("Arazi kotları",Modifier.padding(top=12.dp)) }
        Row { Checkbox(arrows,{arrows=it});Text("Bağlantı yönü okları",Modifier.padding(top=12.dp)) }
        Row { Checkbox(grid,{grid=it});Text("Koordinat ızgarası",Modifier.padding(top=12.dp)) }
        Text("! Eksik kot   △ Kot farkı uyarısı\nK: kapak / zemin kotu, A: akar kotu\nX: Kuzey, Y: Doğu; koordinatlar metre kabul edilir.\nSade görünümü kapatsanız da çakışan etiketler gizlenir. Kot ayrıntısı sığmazsa yalnızca baca adı gösterilir.")
    }},confirmButton={TextButton(onClick={showLayers=false}) { Text("Tamam") }})
    pendingChange?.let { change -> AlertDialog(onDismissRequest={if(!locked) pendingChange=null},title={Text(if(change.after==null) "Bağlantı kaldırılsın mı?" else "Bağlantı değiştirilsin mi?")},
        text={Text("${byId[change.id]?.name}: ${byName[change.before]?.name ?: "Yok"} → ${byName[change.after]?.name ?: "Bağlantı yok"}. İşlemden sonra Geri al kullanabilirsiniz.")},
        confirmButton={TextButton(enabled=!locked,onClick={pendingChange=null;commit(change)}) { Text("Uygula") }},
        dismissButton={TextButton(enabled=!locked,onClick={pendingChange=null}) { Text("Vazgeç") }}) }
}

private fun cadNumber(value: Double?): String = if(value==null) "—" else if(value!=0.0&&abs(value)<.01) String.format(Locale.US,"%.3g",value) else String.format(Locale.US,"%.2f",value)
