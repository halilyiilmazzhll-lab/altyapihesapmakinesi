package com.example.egimhesabi.ui.screens

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.imePadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.GradientEnd
import com.example.egimhesabi.theme.GradientMid
import com.example.egimhesabi.theme.GradientStart
import com.example.egimhesabi.theme.SlopeDanger
import com.example.egimhesabi.theme.SlopeOk
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.ui.components.GlassCard
import com.example.egimhesabi.ui.components.GlassCardAccent
import com.example.egimhesabi.ui.components.StakeoutPicker
import com.example.egimhesabi.ui.components.StakeoutTransferFields
import com.example.egimhesabi.util.NumberParser
import com.example.egimhesabi.viewmodel.ReverseCalculationViewModel

import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReverseCalculationScreen(
    viewModel: ReverseCalculationViewModel,
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
                        text = "Ters Hesaplama",
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
                // --- Girdi Kartı ---
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    backgroundAlpha = 1f,
                    contentPadding = 16.dp,
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "BİLİNEN DEĞERLER",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Başlangıç Kotu ve Yön Seçici
                        StakeoutPicker("Başlangıç", state.startSource, viewModel::importStart, fields = StakeoutTransferFields.INVERT)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                CompactInput(
                                    value = state.startKot,
                                    onValueChange = { viewModel.updateStartKot(it) },
                                    label = "Başlangıç Kotu",
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

                            // Yön Butonu
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Eğim Yönü",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                )
                                TextButton(
                                    onClick = { viewModel.toggleteDirection() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (state.isDownhilele) SlopeDanger.copy(alpha = 0.1f) else SlopeOk.copy(alpha = 0.1f))
                                ) {
                                    Text(
                                        text = if (state.isDownhilele) "▼ Düşüş" else "▲ Yükseliş",
                                        color = if (state.isDownhilele) SlopeDanger else SlopeOk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Eğim ve Mesafe
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Eğim Input
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Hedef Eğim",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                    )
                                    Text(
                                        text = if (state.isSlopePercent) "1/X Yap" else "% Yap",
                                        fontSize = 10.sp,
                                        color = AccentOrange,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .clickable { viewModel.toggleteSlopeMode() }
                                    )
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
                                        if (!state.isSlopePercent) {
                                            Text(
                                                text = "1/",
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        } else {
                                            Text(
                                                text = "% ",
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        BasicTextField(
                                            value = state.slopeInput,
                                            onValueChange = { viewModel.updateSlopeInput(it) },
                                            textStyle = TextStyle(
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Decimal,
                                                imeAction = ImeAction.Next
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onNext = { focusManager.clearFocus() }
                                            ),
                                            singleLine = true,
                                            cursorBrush = SolidColor(AccentOrange),
                                            modifier = Modifier.weight(1f),
                                            decorationBox = { innerTextField ->
                                                if (state.slopeInput.isEmpty()) {
                                                    Text(
                                                        text = if (state.isSlopePercent) "1.5" else "200",
                                                        color = TextSecondary.copy(alpha = 0.4f),
                                                        fontSize = 14.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )
                                    }
                                }
                            }

                            // Mesafe
                            Column(modifier = Modifier.weight(1f)) {
                                CompactInput(
                                    value = state.distance,
                                    onValueChange = { viewModel.updateDistance(it) },
                                    label = "İstenen Mesafe",
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
                            }
                        }

                        // Karşıt birim gösterimi (Eğer % girdiyse 1/X göster, 1/X girdiyse % göster)
                        if (state.parsedSlopePercent != null && state.displayRatio != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val aletText = if (state.isSlopePercent) {
                                "Oran: 1/${NumberParser.formatCompact(state.displayRatio!!)}"
                            } else {
                                "Yüzde: %${NumberParser.formatDecimal(state.parsedSlopePercent!!)}"
                            }
                            Text(
                                text = "($aletText)",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                // --- Sonuç Kartı ---
                if (state.resultKot != null) {
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
                                text = "OLMASI GEREKEN KOT",
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
                                    text = NumberParser.formatDecimal(state.resultKot!!, 3),
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

                            Text(
                                text = "Δh = ${NumberParser.formatDecimal(state.heightDiff!!)} m",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
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
    keyboardActions: KeyboardActions
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )

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


