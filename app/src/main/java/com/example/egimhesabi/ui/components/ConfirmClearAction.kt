package com.example.egimhesabi.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun ConfirmClearAction(
    title: String = "Girdiler temizlensin mi?",
    message: String = "Bu ekrandaki girdiler temizlenecek. Kaydedilmiş geçmişiniz korunur.",
    onConfirm: () -> Unit
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { confirming = true }) {
        Icon(Icons.Default.DeleteOutline, contentDescription = "Temizle")
    }
    if (confirming) AlertDialog(
        onDismissRequest = { confirming = false },
        title = { Text(title) }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { confirming = false; onConfirm() }) { Text("Temizle") } },
        dismissButton = { TextButton(onClick = { confirming = false }) { Text("Vazgeç") } }
    )
}
