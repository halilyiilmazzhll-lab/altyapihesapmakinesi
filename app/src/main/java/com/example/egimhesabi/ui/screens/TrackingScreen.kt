package com.example.egimhesabi.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.example.egimhesabi.theme.SurfaceCard

import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.egimhesabi.data.DistrictEntity
import com.example.egimhesabi.data.ProjectEntity
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.viewmodel.TrackingViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

private val TrackingBackground = Color(0xFFF5F5FA)
private val InsetSurface = Color(0xFFF0F4F9)
private val QuietBorder = Color(0x8FFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    viewModel: TrackingViewModel,
    onOpenDrawer: () -> Unit,
    onOpenProject: (ProjectEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val actionMessage by viewModel.message.collectAsStateWithLifecycle()
    actionMessage?.let { message ->
        AlertDialog(onDismissRequest = viewModel::clearMessage,
            title = { Text("İşlem tamamlanamadı") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearMessage) { Text("Tamam") } })
    }
    val districts by viewModel.districts.collectAsState()
    val projects by viewModel.projectsForSelectedDistrict.collectAsState()
    val selectedDistrictId by viewModel.selectedDistrictId.collectAsState()
    val projectCount by viewModel.projectCount.collectAsState()
    val hazeState = remember { HazeState() }
    var showAddDistrictDialeog by remember { mutableStateOf(false) }
    var showAddProjectDialogFor by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteDistrict by remember { mutableStateOf<DistrictEntity?>(null) }
    var pendingDeleteProject by remember { mutableStateOf<ProjectEntity?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF8F8FC), TrackingBackground, Color(0xFFF2F4F8))
                )
            )
            .haze(hazeState)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    modifier = Modifier.hazeChild(
                        state = hazeState,
                        style = HazeStyle(
                            tint = HazeTint(TrackingBackground.copy(alpha = 0.82f)),
                            blurRadius = 18.dp
                        )
                    ),
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Menüyü aç",
                                tint = TextPrimary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    },
                    title = {
                        Text(
                            text = "İmalat Takibi",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = TrackingBackground.copy(alpha = 0.70f)
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDistrictDialeog = true },
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = AccentOrange.copy(alpha = 0.18f),
                            spotColor = AccentOrange.copy(alpha = 0.22f)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    containerColor = AccentOrange,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "İlçe ekle")
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding()),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                item {
                    TrackingSummaryCard(
                        districtCount = districts.size,
                        projectCount = projectCount,
                        hazeState = hazeState
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 8.dp, top = 9.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "SAHA LİSTESİ",
                                color = AccentOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.25.sp
                            )
                            Text(
                                text = "İlçeler ve bağlı projeler",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "${districts.size} ilçe",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                if (districts.isEmpty()) {
                    item {
                        EmptyDistrictCard(hazeState) { showAddDistrictDialeog = true }
                    }
                } else {
                    items(districts, key = { it.id }) { district ->
                        val isExpanded = selectedDistrictId == district.id
                        DistrictCard(
                            district = district,
                            isExpanded = isExpanded,
                            projects = if (isExpanded) projects else emptyList(),
                            hazeState = hazeState,
                            onClick = { viewModel.toggleDistrict(district.id) },
                            onAddProject = { showAddProjectDialogFor = district.id },
                            onOpenProject = onOpenProject,
                            onDeleteDistrict = { pendingDeleteDistrict = district },
                            onDeleteProject = { pendingDeleteProject = it }
                        )
                    }
                }
            }
        }
    }

    if (showAddDistrictDialeog) {
        InputDialog(
            title = "İlçe Ekle",
            label = "İlçe adı",
            actionLabel = "Ekle",
            onDismiss = { showAddDistrictDialeog = false },
            onConfirm = { name ->
                viewModel.addDistrict(name) { showAddDistrictDialeog = false }
            }
        )
    }

    showAddProjectDialogFor?.let { districtId ->
        val settings by viewModel.projectSettings.collectAsStateWithLifecycle()
        AddProjectDialog(
            pipeTypes = settings.pipeTypes,
            pipeWidths = settings.pipeWidths,
            onDismiss = { showAddProjectDialogFor = null },
            onConfirm = { name, pipeType, pipeWidth ->
                viewModel.addProject(districtId, name, pipeType, pipeWidth) { showAddProjectDialogFor = null }
            }
        )
    }

    pendingDeleteDistrict?.let { district ->
        ConfirmDeleteDialog(
            title = "İlçe silinsin mi?",
            message = "${district.name} ilçesi, bağlı tüm projeler, aplikasyon mahalleleri, bacalar, hatlar ve emir kayıtları silinecek.",
            onDismiss = { pendingDeleteDistrict = null },
            onConfirm = {
                viewModel.deleteDistrict(district.id)
                pendingDeleteDistrict = null
            }
        )
    }

    pendingDeleteProject?.let { project ->
        ConfirmDeleteDialog(
            title = "Köy / proje silinsin mi?",
            message = "${project.name} ve bu projeye bağlı tüm bacalar ile hatlar silinecek.",
            onDismiss = { pendingDeleteProject = null },
            onConfirm = {
                viewModel.deleteProject(project.id, project.districtId)
                pendingDeleteProject = null
            }
        )
    }
}

@Composable
private fun TrackingSummaryCard(
    districtCount: Int,
    projectCount: Int,
    hazeState: HazeState
) {
    TrackingGlassCard(
        hazeState = hazeState,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 21.dp
    ) {
        Column(Modifier.padding(13.dp)) {
            Text(
                text = "GENEL DURUM",
                color = AccentOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryMetric(
                    label = "İlçe",
                    value = districtCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                SummaryMetric(
                    label = if (projectCount > 0) "Seçili İlçe / Proje" else "Proje",
                    value = projectCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(InsetSurface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                color = Color(0xFF8D99A8),
                fontSize = 23.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp
            )
        }
    }
}

@Composable
private fun EmptyDistrictCard(hazeState: HazeState, onAddDistrict: () -> Unit) {
    TrackingGlassCard(hazeState, Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(Color(0xFFCAD5E2), CircleShape))
                Spacer(Modifier.size(7.dp))
                Text("Henüz ilçe eklenmedi", color = TextSecondary, fontSize = 13.sp)
            }
            TextButton(onClick = onAddDistrict) {
                Text("İlk ilçeyi ekle", color = AccentOrange, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun DistrictCard(
    district: DistrictEntity,
    isExpanded: Boolean,
    projects: List<ProjectEntity>,
    hazeState: HazeState,
    onClick: () -> Unit,
    onAddProject: () -> Unit,
    onOpenProject: (ProjectEntity) -> Unit,
    onDeleteDistrict: () -> Unit,
    onDeleteProject: (ProjectEntity) -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "districtArrow"
    )

    TrackingGlassCard(
        hazeState = hazeState,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(InsetSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = AccentOrange,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.size(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = district.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isExpanded) "${projects.size} proje" else "Projeleri görüntüle",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onDeleteDistrict, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "İlçeyi sil",
                        tint = Color(0xFFD94A4A),
                        modifier = Modifier.size(17.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Daralt" else "Genişlet",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp).rotate(arrowRotation)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 13.dp, end = 13.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE9EDF2)))
                    if (projects.isEmpty()) {
                        Text(
                            text = "Henüz proje eklenmemiş.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 7.dp)
                        )
                    } else {
                        projects.forEachIndexed { index, project ->
                            ProjectItem(
                                project = project,
                                index = index + 1,
                                onClick = { onOpenProject(project) },
                                onDelete = { onDeleteProject(project) }
                            )
                        }
                    }
                    TextButton(
                        onClick = onAddProject,
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = AccentOrange)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Köy / Proje Ekle", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectItem(
    project: ProjectEntity,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(InsetSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString().padStart(2, '0'),
            color = AccentOrange,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 10.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = project.name,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("Köy / Proje", color = TextSecondary, fontSize = 9.sp)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Köy veya projeyi sil",
                tint = Color(0xFFD94A4A),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = TextSecondary, fontSize = 13.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Sil", color = Color(0xFFD94A4A), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç", color = TextSecondary) }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun TrackingGlassCard(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.025f),
                spotColor = Color.Black.copy(alpha = 0.035f)
            )
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    tint = HazeTint(Color.White.copy(alpha = 0.78f)),
                    blurRadius = 18.dp
                )
            )
            .background(Color.White.copy(alpha = 0.88f))
            .border(1.dp, QuietBorder, shape)
    ) {
        content()
    }
}

@Composable
fun InputDialog(
    title: String,
    label: String,
    actionLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text(label) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentOrange,
                    focusedLabelColor = AccentOrange,
                    cursorColor = AccentOrange,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = InsetSurface,
                    unfocusedContainerColor = InsetSurface
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(inputText.trim()) },
                enabled = inputText.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) {
                Text(actionLabel, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = TextSecondary)
            }
        },
        containerColor = Color.White,
        tonalElevation = 0.dp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectDialog(
    pipeTypes: List<String>,
    pipeWidths: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(pipeTypes.firstOrNull() ?: "") }
    var selectedWidth by rememberSaveable { mutableStateOf(pipeWidths.firstOrNull() ?: "") }
    var expandedType by remember { mutableStateOf(false) }
    var expandedWidth by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = Color.White,
        title = {
            Text("Köy / Proje Ekle", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Proje adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                if (pipeTypes.isNotEmpty()) {
                    androidx.compose.material3.ExposedDropdownMenuBox(
                        expanded = expandedType,
                        onExpandedChange = { expandedType = !expandedType }
                    ) {
                        OutlinedTextField(
                            value = selectedType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Boru Tipi") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedType,
                            onDismissRequest = { expandedType = false }
                        ) {
                            pipeTypes.forEach { type ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = { selectedType = type; expandedType = false }
                                )
                            }
                        }
                    }
                }

                if (pipeWidths.isNotEmpty()) {
                    androidx.compose.material3.ExposedDropdownMenuBox(
                        expanded = expandedWidth,
                        onExpandedChange = { expandedWidth = !expandedWidth }
                    ) {
                        OutlinedTextField(
                            value = selectedWidth,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Boru Çapı") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWidth) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedWidth,
                            onDismissRequest = { expandedWidth = false }
                        ) {
                            pipeWidths.forEach { width ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(width) },
                                    onClick = { selectedWidth = width; expandedWidth = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedType, selectedWidth) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) {
                Text("Ekle", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = TextSecondary)
            }
        }
    )
}




