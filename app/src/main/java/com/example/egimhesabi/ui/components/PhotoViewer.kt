package com.example.egimhesabi.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.egimhesabi.util.rememberPhotoBitmap

@Composable
fun PhotoViewer(path: String, onDismiss: () -> Unit) {
    var scale by remember(path) { mutableFloatStateOf(1f) }
    var offset by remember(path) { mutableStateOf(Offset.Zero) }
    val bitmap = rememberPhotoBitmap(path, 1600)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { scale = 1f; offset = Offset.Zero }) { Text("Görünümü sıfırla", color = Color.White) }
                TextButton(onClick = onDismiss) { Text("Kapat", color = Color.White) }
            }
            Box(Modifier.weight(1f).fillMaxWidth().pointerInput(path) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 6f)
                    offset = if (scale == 1f) Offset.Zero else offset + pan
                }
            }, contentAlignment = Alignment.Center) {
                if (bitmap != null) Image(bitmap, "Emir fotoğrafı", Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y
                }) else Text("Fotoğraf yüklenemedi veya hazırlanıyor.", color = Color.White, modifier = Modifier.padding(24.dp))
            }
        }
    }
}
