package com.example.egimhesabi.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.egimhesabi.theme.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoseptikScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    if (manholeInvert != null && distance != null && slopeVal != null && slopeVal != 0.0) {
        val slopeDecimal = when (slopeType) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Foseptik Hesabı", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF2F2F7)
                )
            )
        },
        containerColor = Color(0xFFF2F2F7)
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Değerleri Giriniz", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)

                    OutlinedTextField(
                        value = manholeInvertStr,
                        onValueChange = { manholeInvertStr = it },
                        label = { Text("Baca Akar Kotu (m)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = distanceStr,
                        onValueChange = { distanceStr = it },
                        label = { Text("Mesafe (m)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column {
                        Text("Eğim Tipi", fontSize = 14.sp, color = TextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = slopeType == 0, onClick = { slopeType = 0 })
                            Text("1/x", modifier = Modifier.padding(end = 8.dp))
                            RadioButton(selected = slopeType == 1, onClick = { slopeType = 1 })
                            Text("%", modifier = Modifier.padding(end = 8.dp))
                            RadioButton(selected = slopeType == 2, onClick = { slopeType = 2 })
                            Text("cm/m")
                        }
                    }

                    OutlinedTextField(
                        value = slopeStr,
                        onValueChange = { slopeStr = it },
                        label = { Text("Eğim Değeri") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = groundElevStr,
                        onValueChange = { groundElevStr = it },
                        label = { Text("Zemin Kotu (m)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tankHeightStr,
                        onValueChange = { tankHeightStr = it },
                        label = { Text("Foseptik Boyu (m)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = coverToInletStr,
                        onValueChange = { coverToInletStr = it },
                        label = { Text("Kapak - Akar Mesafesi (m)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (bottomElev != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AccentOrange.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Sonuçlar", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AccentOrange)
                        inletElev?.let {
                            Text("Foseptik Akar Kotu: ${String.format("%.3f", it)} m")
                        }
                        coverElev?.let {
                            Text("Foseptik Kapak Kotu: ${String.format("%.3f", it)} m")
                        }
                        Text("Foseptik Taban Kotu: ${String.format("%.3f", bottomElev)} m")
                        
                        excavationDepth?.let {
                            Text("Kazı Derinliği: ${String.format("%.2f", it)} m")
                        }
                    }
                }
                
                // Visualization
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(250.dp)
                ) {
                    FoseptikCanvas(
                        manholeInvert = manholeInvert!!,
                        inletElev = inletElev!!,
                        coverElev = coverElev!!,
                        bottomElev = bottomElev!!,
                        groundElev = groundElev
                    )
                }
            }
        }
    }
}

@Composable
fun FoseptikCanvas(
    manholeInvert: Double,
    inletElev: Double,
    coverElev: Double,
    bottomElev: Double,
    groundElev: Double?
) {
    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val width = size.width
        val height = size.height

        // Find min and max elevations for scaling
        val maxElev = maxOf(manholeInvert, coverElev, groundElev ?: coverElev) + 1.0
        val minElev = bottomElev - 1.0
        val elevRange = maxElev - minElev
        
        fun y(elev: Double): Float {
            return height - ((elev - minElev) / elevRange * height).toFloat()
        }

        // Draw Ground Line
        if (groundElev != null) {
            val gy = y(groundElev)
            drawLine(
                color = Color(0xFF8B4513), // Brown
                start = Offset(0f, gy),
                end = Offset(width, gy),
                strokeWidth = 4f
            )
        }

        // Draw Manhole (Left side)
        val manholeX = width * 0.1f
        val my = y(manholeInvert)
        val mTop = y(maxElev - 0.5)
        drawRect(
            color = Color.Gray,
            topLeft = Offset(manholeX - 20f, mTop),
            size = Size(40f, my - mTop),
            style = Stroke(width = 4f)
        )
        // Manhole invert point
        drawCircle(color = Color.Blue, radius = 8f, center = Offset(manholeX, my))

        // Draw Septic Tank (Right side)
        val tankX = width * 0.8f
        val tTop = y(coverElev)
        val tBottom = y(bottomElev)
        val tInlet = y(inletElev)
        val tankWidth = 60f
        
        drawRect(
            color = Color.DarkGray,
            topLeft = Offset(tankX - tankWidth/2, tTop),
            size = Size(tankWidth, tBottom - tTop),
            style = Stroke(width = 4f)
        )
        // Tank inlet point
        drawCircle(color = Color.Blue, radius = 8f, center = Offset(tankX - tankWidth/2, tInlet))

        // Draw Pipe from manhole invert to tank inlet
        drawLine(
            color = Color.Blue,
            start = Offset(manholeX, my),
            end = Offset(tankX - tankWidth/2, tInlet),
            strokeWidth = 6f
        )
    }
}
