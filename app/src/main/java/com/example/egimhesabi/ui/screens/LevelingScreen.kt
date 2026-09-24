package com.example.egimhesabi.ui.screens

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.example.egimhesabi.domain.CorrectionDirection
import com.example.egimhesabi.domain.DEFAULT_PIPE_LENGTH_METERS
import com.example.egimhesabi.domain.LevelCorrection
import com.example.egimhesabi.domain.LevelingCalculator
import com.example.egimhesabi.domain.MeterLevelPoint
import com.example.egimhesabi.domain.PIPE_TOP_OFFSET_METERS
import com.example.egimhesabi.domain.PipeLevelPoint
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.GradientEnd
import com.example.egimhesabi.theme.GradientMid
import com.example.egimhesabi.theme.GradientStart
import com.example.egimhesabi.theme.SlopeDanger
import com.example.egimhesabi.theme.SlopeOk
import com.example.egimhesabi.theme.SlopeOkSoft
import com.example.egimhesabi.theme.SlopeWarning
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.ui.components.GlassCard
import com.example.egimhesabi.ui.components.GlassCardAccent
import com.example.egimhesabi.ui.components.StakeoutPicker
import com.example.egimhesabi.ui.components.StakeoutTransferFields
import com.example.egimhesabi.domain.StakeoutCalculationSource
import com.example.egimhesabi.util.NumberParser
import com.example.egimhesabi.viewmodel.LevelingTableMode
import com.example.egimhesabi.viewmodel.LevelingUiState
import com.example.egimhesabi.viewmodel.LevelingViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelingScreen(
    viewModel: LevelingViewModel,
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
                    Text(
                        text = "Nivo / Boru Kot Kontrolü",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                RsConnectionCard(
                    state = state,
                    onRsElevationChange = viewModel::updateRsElevation,
                    onRsBacksightChange = viewModel::updateRsBacksight,
                    onRsSignToggle = viewModel::toggleRsSign,
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )

                PipelineCard(
                    state = state,
                    onImportManhole1 = viewModel::importManhole1,
                    onImportManhole2 = viewModel::importManhole2,
                    onManhole1Change = viewModel::updateManhole1Elevation,
                    onManhole2Change = viewModel::updateManhole2Elevation,
                    onDistanceChange = viewModel::updateDistance,
                    onPipeLengthChange = viewModel::updatePipeLength,
                    onPipeTopOffsetChange = viewModel::updatePipeTopOffset,
                    onManhole1SignToggle = viewModel::toggleManhole1Sign,
                    onManhole2SignToggle = viewModel::toggleManhole2Sign,
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { focusManager.clearFocus() }
                )

                if (state.isLineReady) {
                    FieldCheckCard(
                        state = state,
                        onPreviousPipe = viewModel::selectPreviousPipe,
                        onNextPipe = viewModel::selectNextPipe,
                        onActualReadingChange = viewModel::updateActualReading,
                        onCheck = viewModel::checkActualReading,
                        onDone = {
                            viewModel.checkActualReading()
                            focusManager.clearFocus()
                        }
                    )

                    state.correction?.let { correction ->
                        CorrectionResultCard(state = state, correction = correction)
                    }

                    PlanTableCard(
                        state = state,
                        onModeChange = viewModel::updateTableMode,
                        onSelectPipe = viewModel::selectPipe
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun RsConnectionCard(
    state: LevelingUiState,
    onRsElevationChange: (String) -> Unit,
    onRsBacksightChange: (String) -> Unit,
    onRsSignToggle: () -> Unit,
    onNext: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        backgroundAlpha = 1f,
        contentPadding = 16.dp,
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionTitle(
                title = "1  ·  RS BAĞLANTISI",
                description = "RS, çıkış aldığınız sabit kot noktasıdır. Üzerindeki geri okumayla alet kotunu kurun."
            )
            Spacer(modifier = Modifier.height(14.dp))

            LevelingInput(
                value = state.rsElevationInput,
                onValueChange = onRsElevationChange,
                label = "RS kotu",
                placeholder = "Örn. 100,000",
                isError = state.rsElevationError,
                errorText = "Geçerli bir RS kotu girin.",
                imeAction = ImeAction.Next,
                onImeAction = onNext,
                onSignToggle = onRsSignToggle
            )
            Spacer(modifier = Modifier.height(12.dp))
            LevelingInput(
                value = state.rsBacksightInput,
                onValueChange = onRsBacksightChange,
                label = "RS mira okuması (geri okuma)",
                placeholder = "Örn. 1,325",
                isError = state.rsBacksightError,
                errorText = "Geçerli, sıfırdan küçük olmayan bir değer girin.",
                imeAction = ImeAction.Next,
                onImeAction = onNext
            )
            Spacer(modifier = Modifier.height(16.dp))
            InstrumentStatus(instrumentElevation = state.instrumentElevation)
        }
    }
}

@Composable
private fun PipelineCard(
    state: LevelingUiState,
    onImportManhole1: (StakeoutCalculationSource) -> Unit,
    onImportManhole2: (StakeoutCalculationSource) -> Unit,
    onManhole1Change: (String) -> Unit,
    onManhole2Change: (String) -> Unit,
    onDistanceChange: (String) -> Unit,
    onPipeLengthChange: (String) -> Unit,
    onPipeTopOffsetChange: (String) -> Unit,
    onManhole1SignToggle: () -> Unit,
    onManhole2SignToggle: () -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        backgroundAlpha = 1f,
        contentPadding = 16.dp,
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionTitle(
                title = "2  ·  PROJE HATTI",
                description = "Boru numaraları Baca 1'den Baca 2'ye doğru ilerler."
            )
            StakeoutPicker("Baca 1", state.manhole1Source, onImportManhole1, fields = StakeoutTransferFields.INVERT)
            StakeoutPicker("Baca 2", state.manhole2Source, onImportManhole2, fields = StakeoutTransferFields.INVERT)
            Spacer(modifier = Modifier.height(14.dp))
            LevelingInput(
                value = state.manhole1ElevationInput,
                onValueChange = onManhole1Change,
                label = "Baca 1 akar kotu (başlangıç)",
                placeholder = "Örn. 99,850",
                isError = state.manhole1ElevationError,
                errorText = "Geçerli bir akar kotu girin.",
                imeAction = ImeAction.Next,
                onImeAction = onNext,
                onSignToggle = onManhole1SignToggle
            )
            Spacer(modifier = Modifier.height(12.dp))
            LevelingInput(
                value = state.manhole2ElevationInput,
                onValueChange = onManhole2Change,
                label = "Baca 2 akar kotu (bitiş)",
                placeholder = "Örn. 99,550",
                isError = state.manhole2ElevationError,
                errorText = "Geçerli bir akar kotu girin.",
                imeAction = ImeAction.Next,
                onImeAction = onNext,
                onSignToggle = onManhole2SignToggle
            )
            Spacer(modifier = Modifier.height(12.dp))
            LevelingInput(
                value = state.distanceInput,
                onValueChange = onDistanceChange,
                label = "Baca 1 – Baca 2 mesafesi",
                placeholder = "Örn. 30,00",
                isError = state.distanceError,
                errorText = "Mesafe sıfırdan büyük ve en fazla 15.000 m olmalıdır.",
                imeAction = ImeAction.Next,
                onImeAction = onNext
            )
            Spacer(modifier = Modifier.height(12.dp))
            LevelingInput(
                value = state.pipeLengthInput,
                onValueChange = onPipeLengthChange,
                label = "Bir borunun boyu",
                placeholder = "Örn. 1,50 veya 7,00",
                isError = state.pipeLengthError,
                errorText = "Boru boyu sıfırdan büyük olmalıdır.",
                imeAction = ImeAction.Next,
                onImeAction = onNext
            )
            Spacer(modifier = Modifier.height(12.dp))
            LevelingInput(
                value = state.pipeTopOffsetInput,
                onValueChange = onPipeTopOffsetChange,
                label = "Akar kotu – mira temas noktası mesafesi",
                placeholder = "Örn. boru üstü için 0,40",
                isError = state.pipeTopOffsetError,
                errorText = "Mesafe sıfırdan küçük olamaz.",
                imeAction = ImeAction.Done,
                onImeAction = onDone
            )

            if (state.isLineReady) {
                Spacer(modifier = Modifier.height(16.dp))
                PipelineSummary(state)
            }
        }
    }
}

@Composable
private fun FieldCheckCard(
    state: LevelingUiState,
    onPreviousPipe: () -> Unit,
    onNextPipe: () -> Unit,
    onActualReadingChange: (String) -> Unit,
    onCheck: () -> Unit,
    onDone: () -> Unit
) {
    val selectedPipe = state.selectedPipe ?: return
    val expectedReading = selectedPipe.expectedStaffReading
    val canRead = state.isInstrumentSet && expectedReading != null && expectedReading >= 0.0
    val pipeTopOffset = state.pipeTopOffset ?: PIPE_TOP_OFFSET_METERS

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        backgroundAlpha = 1f,
        contentPadding = 16.dp,
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionTitle(
                title = "3  ·  SAHA MİRA KONTROLÜ",
                description = "Mira temas noktası, proje akar kotundan ${NumberParser.formatDecimal(pipeTopOffset, 2)} m yukarı kabul edilir."
            )
            Spacer(modifier = Modifier.height(14.dp))

            PipeSelector(
                selectedPipe = selectedPipe,
                pipeCount = state.pipeCount ?: 1,
                onPrevious = onPreviousPipe,
                onNext = onNextPipe
            )
            Spacer(modifier = Modifier.height(12.dp))

            TargetSummary(
                selectedPipe = selectedPipe,
                isInstrumentSet = state.isInstrumentSet,
                pipeTopOffset = pipeTopOffset
            )
            Spacer(modifier = Modifier.height(12.dp))

            when {
                !state.isInstrumentSet -> WarningBanner(
                    text = "Hedef mira için önce RS bağlantısıyla alet kotunu oluşturun.",
                    danger = false
                )

                expectedReading != null && expectedReading < 0.0 -> WarningBanner(
                    text = "Hedef nokta alet kotunun üstünde. Normal mira ile okunamaz; nivoyu yeniden kurun.",
                    danger = true
                )
            }

            if (!canRead) Spacer(modifier = Modifier.height(12.dp))

            LevelingInput(
                value = state.actualReadingInput,
                onValueChange = onActualReadingChange,
                label = "Okunan gerçek mira",
                placeholder = if (canRead) "Örn. 1,885" else "Önce alet kotunu uygun kurun",
                isError = state.actualReadingError,
                errorText = "Geçerli, sıfırdan küçük olmayan bir değer girin.",
                imeAction = ImeAction.Done,
                onImeAction = onDone,
                enabled = canRead
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCheck,
                enabled = canRead && state.actualReading != null && !state.actualReadingError,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentOrange,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE2E8F0),
                    disabledContentColor = TextSecondary.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "OKUMAYI KONTROL ET",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                )
            }
        }
    }
}

@Composable
private fun CorrectionResultCard(
    state: LevelingUiState,
    correction: LevelCorrection
) {
    val selectedPipe = state.selectedPipe ?: return
    val actualElevation = state.actualElevation ?: return
    val actualReading = state.actualReading ?: return
    val expectedReading = selectedPipe.expectedStaffReading ?: return
    if (expectedReading < 0.0) return

    val accentColor = if (correction.direction == CorrectionDirection.ON_GRADE) SlopeOk else SlopeWarning
    val actionText = when (correction.direction) {
        CorrectionDirection.UP ->
            "${NumberParser.formatDecimal(correction.amountMeters * 100.0, 1)} cm YUKARI KALDIR"
        CorrectionDirection.DOWN ->
            "${NumberParser.formatDecimal(correction.amountMeters * 100.0, 1)} cm AŞAĞI İNDİR"
        CorrectionDirection.ON_GRADE -> "KOT UYGUN"
    }
    val explanation = when (correction.direction) {
        CorrectionDirection.UP -> "Mevcut nokta proje kotundan daha aşağıda."
        CorrectionDirection.DOWN -> "Mevcut nokta proje kotundan daha yukarıda."
        CorrectionDirection.ON_GRADE -> "Gösterim hassasiyetinde düzeltme gerekmiyor."
    }
    val icon = when (correction.direction) {
        CorrectionDirection.UP -> Icons.Default.ArrowUpward
        CorrectionDirection.DOWN -> Icons.Default.ArrowDownward
        CorrectionDirection.ON_GRADE -> Icons.Default.CheckCircle
    }

    GlassCardAccent(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive },
        accentColor = accentColor,
        cornerRadius = 24.dp,
        contentPadding = 18.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = actionText,
                fontSize = if (correction.direction == CorrectionDirection.ON_GRADE) 27.sp else 24.sp,
                lineHeight = 30.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = explanation,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = accentColor.copy(alpha = 0.18f))
            Spacer(modifier = Modifier.height(12.dp))
            ResultComparisonRow(
                leftLabel = "Okunan mira",
                leftValue = "${NumberParser.formatDecimal(actualReading, 3)} m",
                rightLabel = "Hedef mira",
                rightValue = "${NumberParser.formatDecimal(expectedReading, 3)} m"
            )
            Spacer(modifier = Modifier.height(10.dp))
            ResultComparisonRow(
                leftLabel = "Mevcut akar kotu",
                leftValue = "${NumberParser.formatDecimal(actualElevation, 3)} m",
                rightLabel = "Proje akar kotu",
                rightValue = "${NumberParser.formatDecimal(selectedPipe.designElevation, 3)} m"
            )
        }
    }
}

@Composable
private fun PlanTableCard(
    state: LevelingUiState,
    onModeChange: (LevelingTableMode) -> Unit,
    onSelectPipe: (Int) -> Unit
) {
    val startElevation = state.manhole1Elevation ?: return
    val pipeLength = state.pipeLength ?: DEFAULT_PIPE_LENGTH_METERS
    val pipeTopOffset = state.pipeTopOffset ?: PIPE_TOP_OFFSET_METERS
    val startExpectedReading = state.instrumentElevation?.let {
        LevelingCalculator.staffContactElevation(startElevation, pipeTopOffset)?.let { topElevation ->
            LevelingCalculator.expectedStaffReading(it, topElevation)
        }
    }
    val hasNegativeReading = startExpectedReading?.let { it < 0.0 } == true ||
        state.pipePoints.any { (it.expectedStaffReading ?: 0.0) < 0.0 }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        backgroundAlpha = 1f,
        contentPadding = 16.dp,
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionTitle(
                title = "4  ·  KOT / MİRA PLANI",
                description = "Her noktada akar kotu, ${NumberParser.formatDecimal(pipeTopOffset, 2)} m yukarıdaki mira temas kotu ve hedef mira gösterilir."
            )
            Spacer(modifier = Modifier.height(14.dp))
            TableModeSelector(selectedMode = state.tableMode, onModeChange = onModeChange)
            Spacer(modifier = Modifier.height(10.dp))

            if (state.tableMode == LevelingTableMode.PIPE_ENDS) {
                val lastLength = state.pipePoints.lastOrNull()?.segmentLength
                val shortPartText = if (
                    lastLength != null && abs(lastLength - pipeLength) > 0.0005
                ) {
                    " · son parça ${NumberParser.formatDecimal(lastLength, 2)} m"
                } else {
                    ""
                }
                Text(
                    text = "${state.pipeCount} parça · boru boyu ${NumberParser.formatDecimal(pipeLength, 2)} m$shortPartText",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            } else {
                Text(
                    text = "Baca 1'den itibaren 1,00 m aralıklarla",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }

            if (!state.isInstrumentSet) {
                Spacer(modifier = Modifier.height(10.dp))
                WarningBanner(
                    text = "Kotlar hazır. Hedef mira sütunu RS bağlantısından sonra hesaplanır.",
                    danger = false
                )
            } else if (hasNegativeReading) {
                Spacer(modifier = Modifier.height(10.dp))
                WarningBanner(
                    text = "Bazı hedef mira değerleri negatif. Bu noktalar için nivo kurulumu uygun değil.",
                    danger = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            val visibleRowCount = if (state.tableMode == LevelingTableMode.PIPE_ENDS) {
                state.pipePoints.size + 1
            } else {
                state.meterPoints.size
            }
            if (visibleRowCount > 7) {
                Text(
                    text = "Tüm noktalar için tablo içinde aşağı–yukarı kaydırın.",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                )
            }
            StationTableHeader()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                if (state.tableMode == LevelingTableMode.PIPE_ENDS) {
                    item(key = "start") {
                        StationRow(
                            label = "Baca 1",
                            detail = "Başlangıç",
                            distance = 0.0,
                            elevation = startElevation,
                            expectedReading = startExpectedReading,
                            pipeTopOffset = pipeTopOffset,
                            selected = false,
                            onClick = null
                        )
                    }
                    items(state.pipePoints, key = { it.pipeNumber }) { point ->
                        PipeStationRow(
                            point = point,
                            pipeLength = pipeLength,
                            pipeTopOffset = pipeTopOffset,
                            selected = state.selectedPipeNumber == point.pipeNumber,
                            onClick = { onSelectPipe(point.pipeNumber) }
                        )
                    }
                } else {
                    items(state.meterPoints, key = { it.distance }) { point ->
                        MeterStationRow(
                            point = point,
                            totalDistance = state.distance ?: point.distance,
                            pipeTopOffset = pipeTopOffset
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PipelineSummary(state: LevelingUiState) {
    val slope = state.slopePerMille ?: return
    val lastLength = state.pipePoints.lastOrNull()?.segmentLength
    val pipeLength = state.pipeLength ?: DEFAULT_PIPE_LENGTH_METERS
    val slopeDirection = when {
        slope < -0.000001 -> "düşüş"
        slope > 0.000001 -> "yükseliş"
        else -> "düz"
    }
    val slopeText = "${NumberParser.formatDecimal(abs(slope), 2)} ‰ $slopeDirection"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AccentOrange.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "BACA 1  →  BACA 2",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AccentOrange,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(7.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryValue(label = "Eğim", value = slopeText)
                SummaryValue(label = "Boru/parça", value = "${state.pipeCount} adet")
            }
            if (lastLength != null && abs(lastLength - pipeLength) > 0.0005) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Son parça ${NumberParser.formatDecimal(lastLength, 2)} m; diğerleri ${NumberParser.formatDecimal(pipeLength, 2)} m.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun InstrumentStatus(instrumentElevation: Double?) {
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
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SlopeOk,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "  Alet kotlandırıldı",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlopeOk
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ALET KOTU (GÖZLEME DÜZLEMİ)",
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
                    imageVector = Icons.Default.Info,
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
private fun PipeSelector(
    selectedPipe: PipeLevelPoint,
    pipeCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF1F5F9),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevious,
                enabled = selectedPipe.pipeNumber > 1
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Önceki boru")
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BORU ${selectedPipe.pipeNumber} SONU",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Baca 1'den ${NumberParser.formatDecimal(selectedPipe.distance, 2)} m",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            IconButton(
                onClick = onNext,
                enabled = selectedPipe.pipeNumber < pipeCount
            ) {
                Icon(Icons.Default.Add, contentDescription = "Sonraki boru")
            }
        }
    }
}

@Composable
private fun TargetSummary(
    selectedPipe: PipeLevelPoint,
    isInstrumentSet: Boolean,
    pipeTopOffset: Double
) {
    val pipeTopElevation = selectedPipe.designElevation + pipeTopOffset
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AccentOrange.copy(alpha = 0.07f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SummaryValue(
                    label = "Proje akar kotu",
                    value = "${NumberParser.formatDecimal(selectedPipe.designElevation, 3)} m",
                    modifier = Modifier.weight(1f)
                )
                SummaryValue(
                    label = "Mira temas kotu (+${NumberParser.formatDecimal(pipeTopOffset, 2)} m)",
                    value = "${NumberParser.formatDecimal(pipeTopElevation, 3)} m",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(9.dp))
            val miraText = if (isInstrumentSet && selectedPipe.expectedStaffReading != null) {
                "${NumberParser.formatDecimal(selectedPipe.expectedStaffReading, 3)} m"
            } else "RS gerekli"
            SummaryValue(
                label = "Boru üstünde hedef mira",
                value = miraText
            )
        }
    }
}

@Composable
private fun TableModeSelector(
    selectedMode: LevelingTableMode,
    onModeChange: (LevelingTableMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFFF1F5F9))
            .padding(3.dp)
    ) {
        TableModeButton(
            text = "Boru sonları",
            selected = selectedMode == LevelingTableMode.PIPE_ENDS,
            onClick = { onModeChange(LevelingTableMode.PIPE_ENDS) },
            modifier = Modifier.weight(1f)
        )
        TableModeButton(
            text = "Her 1 metre",
            selected = selectedMode == LevelingTableMode.EVERY_METER,
            onClick = { onModeChange(LevelingTableMode.EVERY_METER) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TableModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) AccentOrange else TextSecondary
        )
    }
}

@Composable
private fun StationTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccentOrange.copy(alpha = 0.09f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Nokta", modifier = Modifier.weight(1f), style = tableHeaderStyle())
        Text("Mesafe", modifier = Modifier.weight(0.75f), style = tableHeaderStyle())
        Text(
            "Akar / Boru üstü / Mira",
            modifier = Modifier.weight(1.45f),
            style = tableHeaderStyle(),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun PipeStationRow(
    point: PipeLevelPoint,
    pipeLength: Double,
    pipeTopOffset: Double,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isShort = abs(point.segmentLength - pipeLength) > 0.0005
    StationRow(
        label = "Boru ${point.pipeNumber}",
        detail = if (isShort) {
            "${NumberParser.formatDecimal(point.segmentLength, 2)} m son parça"
        } else {
            "${NumberParser.formatDecimal(pipeLength, 2)} m"
        },
        distance = point.distance,
        elevation = point.designElevation,
        expectedReading = point.expectedStaffReading,
        pipeTopOffset = pipeTopOffset,
        selected = selected,
        onClick = onClick
    )
}

@Composable
private fun MeterStationRow(
    point: MeterLevelPoint,
    totalDistance: Double,
    pipeTopOffset: Double
) {
    val label = when {
        abs(point.distance) < 0.0005 -> "Baca 1"
        abs(point.distance - totalDistance) < 0.0005 -> "Baca 2"
        else -> "${NumberParser.formatCompact(point.distance)}. m"
    }
    StationRow(
        label = label,
        detail = if (label.startsWith("Baca")) "Hat ucu" else "Ara nokta",
        distance = point.distance,
        elevation = point.designElevation,
        expectedReading = point.expectedStaffReading,
        pipeTopOffset = pipeTopOffset,
        selected = false,
        onClick = null
    )
}

@Composable
private fun StationRow(
    label: String,
    detail: String,
    distance: Double,
    elevation: Double,
    expectedReading: Double?,
    pipeTopOffset: Double,
    selected: Boolean,
    onClick: (() -> Unit)?
) {
    val background = if (selected) AccentOrange.copy(alpha = 0.10f) else Color.Transparent
    val rowModifier = Modifier
        .fillMaxWidth()
        .background(background, RoundedCornerShape(8.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 10.dp, vertical = 9.dp)

    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) AccentOrange else TextPrimary
            )
            Text(text = detail, fontSize = 9.sp, color = TextSecondary)
        }
        Text(
            text = NumberParser.formatDecimal(distance, 2),
            modifier = Modifier.weight(0.75f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary
        )
        Column(modifier = Modifier.weight(1.45f), horizontalAlignment = Alignment.End) {
            Text(
                text = "Akar ${NumberParser.formatDecimal(elevation, 3)}",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Temas ${NumberParser.formatDecimal(elevation + pipeTopOffset, 3)}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
            Text(
                text = when {
                    expectedReading == null -> "Mira: RS gerekli"
                    else -> "Mira ${NumberParser.formatDecimal(expectedReading, 3)}"
                },
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = if (expectedReading != null && expectedReading < 0.0) SlopeDanger else TextSecondary
            )
        }
    }
    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.7.dp)
}

@Composable
private fun WarningBanner(text: String, danger: Boolean) {
    val color = if (danger) SlopeDanger else AccentOrange
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.09f), RoundedCornerShape(12.dp))
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (danger) Icons.Default.Warning else Icons.Default.Info,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 11.sp, lineHeight = 15.sp, color = TextSecondary)
    }
}

@Composable
private fun ResultComparisonRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SummaryValue(leftLabel, leftValue, Modifier.weight(1f))
        SummaryValue(rightLabel, rightValue, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 0.3.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
private fun SectionTitle(title: String, description: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = AccentOrange,
        letterSpacing = 0.5.sp
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = description,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = TextSecondary
    )
}

@Composable
private fun LevelingInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isError: Boolean,
    errorText: String,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    enabled: Boolean = true,
    onSignToggle: (() -> Unit)? = null,
    suffix: String? = "m",
    keyboardType: KeyboardType = KeyboardType.Decimal
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
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        keyboardType = keyboardType,
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
                            contentDescription = if (suffix != null) "$label, $suffix" else label
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
                    IconButton(
                        onClick = onSignToggle,
                        enabled = enabled,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text(
                            text = "±",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (enabled) AccentOrange else TextSecondary.copy(alpha = 0.4f)
                        )
                    }
                }
                if (suffix != null) {
                    Text(
                        text = suffix,
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
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

@Composable
private fun tableHeaderStyle(): TextStyle = TextStyle(
    fontSize = 10.sp,
    fontWeight = FontWeight.Bold,
    color = TextPrimary
)
