package com.example.egimhesabi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.egimhesabi.data.WorkOrderWithPhotos
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkOrderItemCard(
    workOrderWithPhotos: WorkOrderWithPhotos,
    manholeName: String? = null,
    onOpenPhoto: (String) -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val workOrder = workOrderWithPhotos.workOrder
    val photos = workOrderWithPhotos.photos
    val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("tr"))

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        backgroundAlpha = 0.6f,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workOrder.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentOrange,
                        fontWeight = FontWeight.Bold
                    )
                    if (manholeName != null) {
                        Text(
                            text = "Baca: $manholeName",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            if (workOrder.note.isNotBlank()) {
                Text(
                    text = workOrder.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Oluşturulma: ${dateFormat.format(Date(workOrder.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (photos.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(photos, key = { it.id }) { photo ->
                        com.example.egimhesabi.util.rememberPhotoBitmap(photo.path)?.let { bitmap ->
                            androidx.compose.foundation.Image(
                                bitmap = bitmap,
                                contentDescription = "Emir fotoğrafı",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.1f))
                                    .clickable { onOpenPhoto(photo.path) }
                            )
                        }
                    }
                }
            }
        }
    }
}

