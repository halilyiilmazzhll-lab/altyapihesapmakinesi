package com.example.egimhesabi.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.egimhesabi.data.AppDatabase
import com.example.egimhesabi.domain.StakeoutCalculationSource
import com.example.egimhesabi.domain.StakeoutElevationBasis
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.OutlineLight
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary

enum class StakeoutTransferFields { UPPER_AND_INVERT, INVERT, UPPER }

/** Holds the last user selections globally. */
object StakeoutSession {
    var lastDistrictName: String? = null
    var lastNeighborhoodId: Long? = null
    var lastBasis: StakeoutElevationBasis? = null
}

@Composable
fun ActiveStakeoutCard(modifier: Modifier = Modifier) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var triggerUpdate by remember { mutableIntStateOf(0) }
    val dName = StakeoutSession.lastDistrictName
    val nId = StakeoutSession.lastNeighborhoodId

    val context = LocalContext.current
    val recordsFlow = remember(context) { AppDatabase.getInstance(context).stakeoutDao().allRecords() }
    val records by recordsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val nName = remember(records, triggerUpdate) {
        records.firstOrNull { it.manhole.neighborhoodId == nId }?.neighborhoodName
    }

    GlassCard(
        modifier = modifier.fillMaxWidth().clickable { showDialog = true },
        cornerRadius = 14.dp,
        backgroundAlpha = 0.85f,
        elevation = 2.dp,
        contentPadding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(36.dp).background(Color(0xFFF0F4F9), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Map, null, tint = AccentOrange, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Aktif Tutanak", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (dName != null && nName != null) "$dName / $nName" else "Seçilmedi",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.Edit, "Değiştir", tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }

    if (showDialog) {
        ActiveStakeoutDialog(
            records = records,
            onDismiss = { showDialog = false },
            onSave = { d, n ->
                StakeoutSession.lastDistrictName = d
                StakeoutSession.lastNeighborhoodId = n
                triggerUpdate++
                showDialog = false
            }
        )
    }
}

@Composable
private fun ActiveStakeoutDialog(
    records: List<com.example.egimhesabi.data.StakeoutRecord>,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit
) {
    var districtName by rememberSaveable { mutableStateOf(StakeoutSession.lastDistrictName) }
    var neighborhoodId by rememberSaveable { mutableStateOf(StakeoutSession.lastNeighborhoodId) }
    
    val districts = records.map { it.districtName }.distinct().sorted()
    if (districtName != null && !districts.contains(districtName)) {
        districtName = null
        neighborhoodId = null
    }
    
    val neighborhoods = records.filter { it.districtName == districtName }
        .distinctBy { it.manhole.neighborhoodId }.sortedBy { it.neighborhoodName }
    if (neighborhoodId != null && !neighborhoods.any { it.manhole.neighborhoodId == neighborhoodId }) {
        neighborhoodId = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = Color.White,
        title = { Text("Aktif Tutanağı Belirle", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (records.isEmpty()) {
                    Text("Tutanak kaydı bulunamadı.", color = TextSecondary, fontSize = 13.sp)
                } else {
                    PickerMenu("İlçe", districtName, districts.map { it to it }) {
                        districtName = it
                        neighborhoodId = null
                    }
                    PickerMenu(
                        label = "Mahalle",
                        selected = neighborhoods.firstOrNull { it.manhole.neighborhoodId == neighborhoodId }?.neighborhoodName,
                        options = neighborhoods.map { it.manhole.neighborhoodId to it.neighborhoodName },
                        enabled = districtName != null
                    ) {
                        neighborhoodId = it
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = districtName != null && neighborhoodId != null,
                onClick = { onSave(districtName!!, neighborhoodId!!) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) { Text("Uygula", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç", color = TextSecondary) } }
    )
}

@Composable
fun StakeoutPicker(
    targetLabel: String,
    source: StakeoutCalculationSource?,
    onSelect: (StakeoutCalculationSource) -> Unit,
    modifier: Modifier = Modifier,
    fields: StakeoutTransferFields = StakeoutTransferFields.UPPER_AND_INVERT
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var errorDialog by rememberSaveable { mutableStateOf(false) }
    
    IconButton(
        onClick = { 
            if (StakeoutSession.lastNeighborhoodId == null) errorDialog = true 
            else showDialog = true 
        },
        modifier = modifier
            .background(Color(0xFFF0F4F9), RoundedCornerShape(8.dp))
            .size(32.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Tutanaktan seç", tint = AccentOrange, modifier = Modifier.size(18.dp))
    }
    
    if (errorDialog) {
        AlertDialog(
            onDismissRequest = { errorDialog = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = Color.White,
            title = { Text("Aktif Tutanak Seçilmedi", color = TextPrimary) },
            text = { Text("Lütfen önce sayfanın üst kısmından bir aktif tutanak belirleyin.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { errorDialog = false }) { Text("Tamam", color = AccentOrange) }
            }
        )
    }
    
    if (showDialog) {
        StakeoutPickerDialog(
            targetLabel = targetLabel,
            fields = fields,
            initialSource = source,
            onDismiss = { showDialog = false },
            onSelect = {
                onSelect(it)
                showDialog = false
            }
        )
    }
}

@Composable
private fun StakeoutPickerDialog(
    targetLabel: String,
    fields: StakeoutTransferFields,
    initialSource: StakeoutCalculationSource?,
    onDismiss: () -> Unit,
    onSelect: (StakeoutCalculationSource) -> Unit
) {
    val context = LocalContext.current
    val neighborhoodId = StakeoutSession.lastNeighborhoodId ?: return
    val recordsFlow = remember(context) { AppDatabase.getInstance(context).stakeoutDao().allRecords() }
    val allRecords by recordsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val records = remember(allRecords, neighborhoodId) { allRecords.filter { it.manhole.neighborhoodId == neighborhoodId } }
    
    var recordId by rememberSaveable { mutableStateOf<Long?>(null) }
    var basis by rememberSaveable { mutableStateOf(initialSource?.basis ?: StakeoutSession.lastBasis ?: StakeoutElevationBasis.PROJECT) }
    
    val neighborhoodRecords = records.sortedBy { it.manhole.name }
    val selectedRecord = neighborhoodRecords.firstOrNull { it.manhole.id == recordId }
    val selection = selectedRecord?.let { StakeoutCalculationSource.from(it, basis) }
    
    val hasRequiredValue = selection != null && when (fields) {
        StakeoutTransferFields.UPPER -> selection.upperLevel != null
        StakeoutTransferFields.INVERT, StakeoutTransferFields.UPPER_AND_INVERT -> selection.invertLevel != null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = Color.White,
        title = {
            Text(
                "$targetLabel için baca seç",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (records.isEmpty()) {
                    Text(
                        "Aktif mahallede kayıtlı baca bulunamadı.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                } else {
                    PickerMenu(
                        label = "Baca",
                        selected = selectedRecord?.manhole?.name,
                        options = neighborhoodRecords.map { it.manhole.id to it.manhole.name }
                    ) { recordId = it }
                    PickerMenu(
                        label = "Kot kaynağı",
                        selected = basis.label,
                        options = StakeoutElevationBasis.entries.map { it to it.label }
                    ) { 
                        basis = it
                        StakeoutSession.lastBasis = it
                    }
                    
                    selection?.let {
                        Text(
                            it.qualifiedName, // Note: qualified name will have empty district/neighborhood names here but we don't display them in calculation anyway
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (fields != StakeoutTransferFields.INVERT) {
                            Text(
                                "${if (basis == StakeoutElevationBasis.PROJECT) "Proje kapak" else "Arazi siyah"}: ${it.upperText.ifEmpty { "Eksik" }} m",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        if (fields != StakeoutTransferFields.UPPER) {
                            Text(
                                "${if (basis == StakeoutElevationBasis.PROJECT) "Proje akar" else "Araziye göre akar"}: ${it.invertText.ifEmpty { "Eksik" }} m",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        if (!hasRequiredValue) {
                            Text(
                                "Kot eksik",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = hasRequiredValue,
                onClick = { selection?.let(onSelect) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentOrange,
                    disabledContainerColor = AccentOrange.copy(alpha = 0.5f)
                )
            ) { 
                Text("Hesaba aktar", color = Color.White) 
            }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("Vazgeç", color = TextSecondary) 
            } 
        }
    )
}

@Composable
private fun <T> PickerMenu(
    label: String,
    selected: String?,
    options: List<Pair<T, String>>,
    enabled: Boolean = true,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled && options.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (enabled) OutlineLight else OutlineLight.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextPrimary,
                disabledContentColor = TextSecondary.copy(alpha = 0.5f)
            )
        ) { 
            Text("$label: ${selected ?: "Seçin"}") 
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 280.dp),
            containerColor = Color.White
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text, color = TextPrimary) },
                    onClick = { onSelect(value); expanded = false }
                )
            }
        }
    }
}
