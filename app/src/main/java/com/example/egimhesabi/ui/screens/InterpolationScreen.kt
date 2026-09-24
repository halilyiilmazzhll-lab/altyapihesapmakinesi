package com.example.egimhesabi.ui.screens

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.imePadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.GradientEnd
import com.example.egimhesabi.theme.GradientMid
import com.example.egimhesabi.theme.GradientStart
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.ui.components.GlassCard
import com.example.egimhesabi.ui.components.GlassCardAccent
import com.example.egimhesabi.ui.components.InterpolationDiagram
import com.example.egimhesabi.ui.components.StakeoutPicker
import com.example.egimhesabi.ui.components.StakeoutTransferFields
import com.example.egimhesabi.util.NumberParser
import com.example.egimhesabi.viewmodel.InterpolationViewModel

import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterpolationScreen(
    viewModel: InterpolationViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

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
                        text = "Ara Kot Hesabı",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Geri",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    com.example.egimhesabi.ui.components.ConfirmClearAction(onConfirm = viewModel::clearAll)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
                    .verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- Kot Girişleri ---
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    backgroundAlpha = 1f,
                    contentPadding = 16.dp,
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "KOT DEĞERLERİ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Kot 1
                            Column(modifier = Modifier.weight(1f)) {
                                CompactInput(
                                    value = state.kot1,
                                    onValueChange = { viewModel.updateKot1(it) },
                                    label = "Başlangıç Kotu",
                                    headerAction = { com.example.egimhesabi.ui.components.StakeoutPicker("Başlangıç", state.startSource, viewModel::importStart, fields = com.example.egimhesabi.ui.components.StakeoutTransferFields.INVERT) },
                                    placeholder = "0.00",
                                    suffix = "m",
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.clearFocus() }
                                    )
                                )
                            }

                            // Kot 2
                            Column(modifier = Modifier.weight(1f)) {
                                CompactInput(
                                    value = state.kot2,
                                    onValueChange = { viewModel.updateKot2(it) },
                                    label = "Bitiş Kotu",
                                    headerAction = { com.example.egimhesabi.ui.components.StakeoutPicker("Bitiş", state.endSource, viewModel::importEnd, fields = com.example.egimhesabi.ui.components.StakeoutTransferFields.INVERT) },
                                    placeholder = "0.00",
                                    suffix = "m",
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.clearFocus() }
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Topleam Mesafe
                        CompactInput(
                            value = state.totalDistance,
                            onValueChange = { viewModel.updateTotaleDistance(it) },
                            label = "Toplam Mesafe",
                            placeholder = "0.00",
                            suffix = "m",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            )
                        )

                        // Eğim bilegisi
                        if (state.slopePercent != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Eğim: % ${NumberParser.formatDecimal(state.slopePercent!!)}" +
                                            (state.slopeRatio?.let { " (1/${NumberParser.formatCompact(it)})" } ?: ""),
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (state.heightDiff != null) {
                                    Text(
                                        text = "Δh = ${NumberParser.formatDecimal(state.heightDiff!!)} m",
                                        fontSize = 12.sp,
                                        color = AccentOrange,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // --- Slider Kartı ---
                if (state.isValid) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        backgroundAlpha = 1f,
                        contentPadding = 16.dp,
                        elevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "MESAFE",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Mevcut mesafe göstergesi
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (state.currentDistance != null) NumberParser.formatDecimal(state.currentDistance!!) else "0.00",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "m",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Slider
                            Slider(
                                value = state.sliderPosition,
                                onValueChange = { viewModel.updateSliderPosition(it) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentOrange,
                                    activeTrackColor = AccentOrange,
                                    inactiveTrackColor = AccentOrange.copy(alpha = 0.15f)
                                )
                            )

                            // 0 ve max etiketleteri
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "0 m",
                                    fontSize = 11.sp,
                                    color = TextSecondary.copy(alpha = 0.6f),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${NumberParser.formatDecimal(state.parsedTotaleDistance ?: 0.0)} m",
                                    fontSize = 11.sp,
                                    color = TextSecondary.copy(alpha = 0.6f),
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Manuele mesafe girişi
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Elle giriş:",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = state.manualDistance,
                                    onValueChange = { viewModel.updateManualeDistance(it) },
                                    textStyle = TextStyle(
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.End
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { focusManager.clearFocus() }
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(AccentOrange),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterEnd) {
                                            if (state.manualDistance.isEmpty()) {
                                                Text(
                                                    text = "0.00",
                                                    color = TextSecondary.copy(alpha = 0.3f),
                                                    fontSize = 16.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.End,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "m",
                                    fontSize = 14.sp,
                                    color = TextSecondary.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (state.isValid && state.currentKot == null) Text(
                        "Mesafe 0 ile ${state.totalDistance} m arasında geçerli bir sayı olmalıdır.",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp)
                    )
                    // --- Sonuç Kartı ---
                    if (state.currentKot != null) {
                        GlassCardAccent(
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = AccentOrange,
                            cornerRadius = 24.dp,
                            elevation = 8.dp,
                            contentPadding = 20.dp
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "HESAPLANAN KOT",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentOrange,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = NumberParser.formatDecimal(state.currentKot!!, 3),
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "m",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Yön bilegisi
                                val directionText = when (state.slopeDirection) {
                                    -1 -> "Düşüş yönünde"
                                    1 -> "Yükseliş yönünde"
                                    else -> "Düz hat"
                                }
                                Text(
                                    text = directionText,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // --- Profile Diyagramı ---
                    if (state.parsedKot1 != null && state.parsedKot2 != null && state.parsedTotaleDistance != null) {
                        InterpolationDiagram(
                            kot1 = state.parsedKot1!!,
                            kot2 = state.parsedKot2!!,
                            totalDistance = state.parsedTotaleDistance!!,
                            sliderPosition = state.sliderPosition,
                            currentKot = state.currentKot
                        )
                    }

                    // --- Tablo Butonu ---
                    TextButton(
                        onClick = { viewModel.toggleteTablee() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (state.showTable) "Tabloyu Gizle" else "Kot Tablosunu Göster",
                            color = AccentOrange,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    // --- Tablo ---
                    AnimatedVisibility(
                        visible = state.showTable,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            backgroundAlpha = 1f,
                            contentPadding = 16.dp,
                            elevation = 2.dp
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Tablo aralık seçimi
                                Text(
                                    text = "ARALIK: ${state.tableInterval.toInt()} m",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentOrange,
                                    letterSpacing = 0.5.sp
                                )
                                Slider(
                                    value = state.tableInterval,
                                    onValueChange = { viewModel.updateTableeInterval(it) },
                                    valueRange = 1f..50f,
                                    steps = 48,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentOrange,
                                        activeTrackColor = AccentOrange.copy(alpha = 0.5f),
                                        inactiveTrackColor = AccentOrange.copy(alpha = 0.1f)
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Tablo başlığı
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            AccentOrange.copy(alpha = 0.08f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Mesafe (m)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Kot (m)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                if (state.tablePoints.isEmpty()) Text(
                                    "Tablo oluşturulamadı. Geçerli kot ve mesafe girin; en fazla 15.001 satır için aralığı artırın.",
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp)
                                )
                                var tablePage by androidx.compose.runtime.saveable.rememberSaveable(state.tablePoints.size) { androidx.compose.runtime.mutableStateOf(0) }
                                val pageCount = (state.tablePoints.size + 99) / 100
                                if (pageCount > 1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TextButton(enabled = tablePage > 0, onClick = { tablePage-- }) { Text("Önceki") }
                                    Text("${tablePage + 1} / $pageCount", modifier = Modifier.padding(12.dp))
                                    TextButton(enabled = tablePage + 1 < pageCount, onClick = { tablePage++ }) { Text("Sonraki") }
                                }
                                state.tablePoints.drop(tablePage * 100).take(100).forEachIndexed { index, point ->
                                    val isEven = index % 2 == 0
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isEven) Color.Transparent else Color(0xFFF8FAFC),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = NumberParser.formatDecimal(point.distance),
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = NumberParser.formatDecimal(point.kot, 3),
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(84.dp)) // Bottom nav space
            }
        }
    }
}

@Composable
private fun CompactInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    suffix: String? = null,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    headerAction: @Composable (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            if (headerAction != null) {
                headerAction()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    singleLine = true,
                    cursorBrush = SolidColor(AccentOrange),
                    modifier = Modifier.weight(1f).semantics { contentDescription = label },
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        innerTextField()
                    }
                )

                if (suffix != null) {
                    Text(
                        text = suffix,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


