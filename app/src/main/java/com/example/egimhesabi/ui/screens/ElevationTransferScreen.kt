package com.example.egimhesabi.ui.screens

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
import com.example.egimhesabi.theme.SlopeDanger
import com.example.egimhesabi.theme.SlopeOk
import com.example.egimhesabi.theme.SlopeOkSoft
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.ui.components.GlassCard
import com.example.egimhesabi.ui.components.GlassCardAccent
import com.example.egimhesabi.ui.components.StakeoutPicker
import com.example.egimhesabi.ui.components.StakeoutTransferFields
import com.example.egimhesabi.util.NumberParser
import com.example.egimhesabi.viewmodel.ElevationTransferViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElevationTransferScreen(
    viewModel: ElevationTransferViewModel,
    onBack: () -> Unit,
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
                    Text("Kot Taşıma", fontWeight = FontWeight.Bold, color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    com.example.egimhesabi.ui.components.ConfirmClearAction(onConfirm = viewModel::clearAll)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
                    .verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    backgroundAlpha = 1f,
                    contentPadding = 16.dp,
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TransferSectionTitle(
                            title = "1  ·  RS'DEN ÇIKIŞ AL",
                            description = "Bilinen RS kotunu ve RS üzerindeki geri mira okumasını girin."
                        )
                        StakeoutPicker("RS noktası", state.rsSource, viewModel::importRs, fields = StakeoutTransferFields.UPPER)
                        Spacer(modifier = Modifier.height(14.dp))
                        TransferInput(
                            value = state.rsElevationInput,
                            onValueChange = viewModel::updateRsElevation,
                            label = "RS kotu",
                            placeholder = "Örn. 100,000",
                            isError = state.rsElevationError,
                            errorText = "Geçerli bir RS kotu girin.",
                            imeAction = ImeAction.Next,
                            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                            onSignToggle = viewModel::toggleRsSign
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TransferInput(
                            value = state.rsReadingInput,
                            onValueChange = viewModel::updateRsReading,
                            label = "RS mira okuması (geri okuma)",
                            placeholder = "Örn. 1,325",
                            isError = state.rsReadingError,
                            errorText = "Geçerli, sıfırdan küçük olmayan bir değer girin.",
                            imeAction = ImeAction.Next,
                            onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TransferInstrumentStatus(state.instrumentElevation)
                    }
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    backgroundAlpha = 1f,
                    contentPadding = 16.dp,
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TransferSectionTitle(
                            title = "2  ·  KOTU YENİ NOKTAYA TAŞI",
                            description = "Miranın yeni noktada gösterdiği değeri girin; taşınan kot anında hesaplanır."
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        TransferInput(
                            value = state.targetReadingInput,
                            onValueChange = viewModel::updateTargetReading,
                            label = "Kot taşınacak noktadaki mira okuması",
                            placeholder = if (state.instrumentElevation != null) {
                                "Örn. 0,875"
                            } else {
                                "Önce RS'den çıkış alın"
                            },
                            isError = state.targetReadingError,
                            errorText = "Geçerli, sıfırdan küçük olmayan bir değer girin.",
                            imeAction = ImeAction.Done,
                            onImeAction = { focusManager.clearFocus() },
                            enabled = state.instrumentElevation != null
                        )
                    }
                }

                state.targetElevation?.let { targetElevation ->
                    GlassCardAccent(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        accentColor = AccentOrange,
                        cornerRadius = 24.dp,
                        contentPadding = 18.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "TAŞINAN NOKTANIN KOTU",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange,
                                letterSpacing = 0.7.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = NumberParser.formatDecimal(targetElevation, 3),
                                    fontSize = 40.sp,
                                    lineHeight = 44.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                                Text(
                                    text = " m",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }
                            state.heightDifference?.let { difference ->
                                Spacer(modifier = Modifier.height(7.dp))
                                Text(
                                    text = transferDifferenceText(difference),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text(
                            text = "HESAP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Yeni kot = RS kotu + RS mira − Yeni nokta mira",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun TransferInstrumentStatus(instrumentElevation: Double?) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (instrumentElevation != null) SlopeOkSoft else AccentOrange.copy(alpha = 0.08f),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        if (instrumentElevation != null) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SlopeOk,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "  RS bağlantısı hazır",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlopeOk
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ALET KOTU",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.4.sp
                )
                Text(
                    text = "${NumberParser.formatDecimal(instrumentElevation, 3)} m",
                    fontSize = 25.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = "  RS kotu ile geri okumayı girin.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun TransferSectionTitle(title: String, description: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = AccentOrange,
        letterSpacing = 0.5.sp
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = description, fontSize = 13.sp, lineHeight = 18.sp, color = TextSecondary)
}

@Composable
private fun TransferInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isError: Boolean,
    errorText: String,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    enabled: Boolean = true,
    onSignToggle: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isError) SlopeDanger else TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    when {
                        isError -> SlopeDanger.copy(alpha = 0.08f)
                        enabled -> Color(0xFFF1F5F9)
                        else -> Color(0xFFE9EDF2)
                    }
                )
                .padding(horizontal = 11.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    textStyle = TextStyle(
                        color = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.55f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = imeAction
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onImeAction() },
                        onNext = { onImeAction() }
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(AccentOrange),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            contentDescription = "$label, metre"
                            if (isError) error(errorText)
                        },
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = TextSecondary.copy(alpha = if (enabled) 0.45f else 0.32f),
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                if (onSignToggle != null) {
                    IconButton(onClick = onSignToggle, modifier = Modifier.size(40.dp)) {
                        Text("±", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                    }
                }
                Text("m", color = TextSecondary.copy(alpha = 0.7f), fontSize = 13.sp)
            }
        }
        if (isError) {
            Text(
                text = errorText,
                fontSize = 10.sp,
                color = SlopeDanger,
                modifier = Modifier.padding(start = 4.dp, top = 3.dp)
            )
        }
    }
}

private fun transferDifferenceText(difference: Double): String {
    if (abs(difference) < 0.0005) return "RS noktasıyla aynı kotta"
    val amount = NumberParser.formatDecimal(abs(difference), 3)
    return if (difference > 0.0) {
        "RS noktasından $amount m daha yüksek"
    } else {
        "RS noktasından $amount m daha alçak"
    }
}
