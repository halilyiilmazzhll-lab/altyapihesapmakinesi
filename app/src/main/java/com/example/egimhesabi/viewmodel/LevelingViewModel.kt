package com.example.egimhesabi.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.egimhesabi.domain.StakeoutCalculationSource
import com.example.egimhesabi.domain.DEFAULT_PIPE_LENGTH_METERS
import com.example.egimhesabi.domain.LevelCorrection
import com.example.egimhesabi.domain.LevelingCalculator
import com.example.egimhesabi.domain.MeterLevelPoint
import com.example.egimhesabi.domain.PIPE_TOP_OFFSET_METERS
import com.example.egimhesabi.domain.PipeLevelPoint
import com.example.egimhesabi.util.NumberParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class LevelingTableMode {
    PIPE_ENDS,
    EVERY_METER
}

data class LevelingUiState(
    val manhole1Source: StakeoutCalculationSource? = null,
    val manhole2Source: StakeoutCalculationSource? = null,
    val rsElevationInput: String = "",
    val rsBacksightInput: String = "",
    val manhole1ElevationInput: String = "",
    val manhole2ElevationInput: String = "",
    val distanceInput: String = "",
    val pipeLengthInput: String = "1,50",
    val pipeTopOffsetInput: String = "0,40",
    val actualReadingInput: String = "",
    val tableMode: LevelingTableMode = LevelingTableMode.PIPE_ENDS,
    val rsElevation: Double? = null,
    val rsBacksight: Double? = null,
    val instrumentElevation: Double? = null,
    val manhole1Elevation: Double? = null,
    val manhole2Elevation: Double? = null,
    val distance: Double? = null,
    val pipeLength: Double? = DEFAULT_PIPE_LENGTH_METERS,
    val pipeTopOffset: Double? = PIPE_TOP_OFFSET_METERS,
    val slopePerMille: Double? = null,
    val pipeCount: Int? = null,
    val pipePoints: List<PipeLevelPoint> = emptyList(),
    val meterPoints: List<MeterLevelPoint> = emptyList(),
    val selectedPipeNumber: Int? = null,
    val selectedPipe: PipeLevelPoint? = null,
    val actualReading: Double? = null,
    val actualPipeTopElevation: Double? = null,
    val actualElevation: Double? = null,
    val correction: LevelCorrection? = null,
    val isReadingChecked: Boolean = false,
    val isInstrumentSet: Boolean = false,
    val isLineReady: Boolean = false,
    val rsElevationError: Boolean = false,
    val rsBacksightError: Boolean = false,
    val manhole1ElevationError: Boolean = false,
    val manhole2ElevationError: Boolean = false,
    val distanceError: Boolean = false,
    val pipeLengthError: Boolean = false,
    val pipeTopOffsetError: Boolean = false,
    val actualReadingError: Boolean = false
)

class LevelingViewModel(private val savedState: SavedStateHandle = SavedStateHandle()) : ViewModel() {

    private val _uiState = MutableStateFlow(LevelingUiState(
        rsElevationInput = savedState["rsElevationInput"] ?: "",
        rsBacksightInput = savedState["rsBacksightInput"] ?: "",
        manhole1ElevationInput = savedState["manhole1ElevationInput"] ?: "",
        manhole2ElevationInput = savedState["manhole2ElevationInput"] ?: "",
        distanceInput = savedState["distanceInput"] ?: "",
        pipeLengthInput = savedState["pipeLengthInput"] ?: "1,50",
        pipeTopOffsetInput = savedState["pipeTopOffsetInput"] ?: "0,40",
        actualReadingInput = savedState["actualReadingInput"] ?: ""
    ))
    val uiState: StateFlow<LevelingUiState> = _uiState.asStateFlow()

    init { recalculate() }

    fun updateRsElevation(value: String) {
        _uiState.update {
            it.copy(
                rsElevationInput = value,
                actualReadingInput = "",
                actualReading = null,
                actualPipeTopElevation = null,
                actualElevation = null,
                correction = null,
                isReadingChecked = false,
                actualReadingError = false
            )
        }
        recalculate()
    }

    fun updateRsBacksight(value: String) {
        _uiState.update {
            it.copy(
                rsBacksightInput = value,
                actualReadingInput = "",
                actualReading = null,
                actualPipeTopElevation = null,
                actualElevation = null,
                correction = null,
                isReadingChecked = false,
                actualReadingError = false
            )
        }
        recalculate()
    }

    fun updateManhole1Elevation(value: String) {
        updateLineInput { it.copy(manhole1ElevationInput = value, manhole1Source = null) }
    }

    fun updateManhole2Elevation(value: String) {
        updateLineInput { it.copy(manhole2ElevationInput = value, manhole2Source = null) }
    }

    fun importManhole1(source: StakeoutCalculationSource) {
        if (source.invertLevel == null) return
        updateLineInput { it.copy(manhole1ElevationInput = source.invertText, manhole1Source = source) }
    }

    fun importManhole2(source: StakeoutCalculationSource) {
        if (source.invertLevel == null) return
        updateLineInput { it.copy(manhole2ElevationInput = source.invertText, manhole2Source = source) }
    }

    fun updateDistance(value: String) {
        updateLineInput { it.copy(distanceInput = value) }
    }

    fun updatePipeLength(value: String) {
        updateLineInput { it.copy(pipeLengthInput = value) }
    }

    fun updatePipeTopOffset(value: String) {
        updateLineInput { it.copy(pipeTopOffsetInput = value) }
    }

    fun toggleRsSign() = updateRsElevation(toggleSign(_uiState.value.rsElevationInput))

    fun toggleManhole1Sign() =
        updateManhole1Elevation(toggleSign(_uiState.value.manhole1ElevationInput))

    fun toggleManhole2Sign() =
        updateManhole2Elevation(toggleSign(_uiState.value.manhole2ElevationInput))

    fun updateTableMode(mode: LevelingTableMode) {
        _uiState.update { it.copy(tableMode = mode) }
    }

    fun selectPipe(pipeNumber: Int) {
        val count = _uiState.value.pipeCount ?: return
        val safeNumber = pipeNumber.coerceIn(1, count)
        _uiState.update {
            it.copy(
                selectedPipeNumber = safeNumber,
                actualReadingInput = "",
                actualReading = null,
                actualPipeTopElevation = null,
                actualElevation = null,
                correction = null,
                isReadingChecked = false,
                actualReadingError = false
            )
        }
        recalculate()
    }

    fun selectPreviousPipe() {
        val current = _uiState.value.selectedPipeNumber ?: 1
        selectPipe(current - 1)
    }

    fun selectNextPipe() {
        val current = _uiState.value.selectedPipeNumber ?: 1
        selectPipe(current + 1)
    }

    fun updateActualReading(value: String) {
        _uiState.update {
            it.copy(
                actualReadingInput = value,
                actualPipeTopElevation = null,
                actualElevation = null,
                correction = null,
                isReadingChecked = false
            )
        }
        recalculate()
    }

    fun checkActualReading() {
        _uiState.update { it.copy(isReadingChecked = true) }
        recalculate()
    }

    fun clearAll() {
        _uiState.value = LevelingUiState()
        recalculate()
    }

    private fun updateLineInput(transform: (LevelingUiState) -> LevelingUiState) {
        _uiState.update {
            transform(it).copy(
                actualReadingInput = "",
                actualReading = null,
                actualPipeTopElevation = null,
                actualElevation = null,
                correction = null,
                isReadingChecked = false,
                actualReadingError = false
            )
        }
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        savedState["rsElevationInput"] = state.rsElevationInput
        savedState["rsBacksightInput"] = state.rsBacksightInput
        savedState["manhole1ElevationInput"] = state.manhole1ElevationInput
        savedState["manhole2ElevationInput"] = state.manhole2ElevationInput
        savedState["distanceInput"] = state.distanceInput
        savedState["pipeLengthInput"] = state.pipeLengthInput
        savedState["pipeTopOffsetInput"] = state.pipeTopOffsetInput
        savedState["actualReadingInput"] = state.actualReadingInput
        val rsElevation = NumberParser.parseDecimal(state.rsElevationInput)
        val parsedRsBacksight = NumberParser.parseDecimal(state.rsBacksightInput)
        val rsBacksight = parsedRsBacksight?.takeIf { it >= 0.0 }
        val instrumentElevation = if (rsElevation != null && rsBacksight != null) {
            LevelingCalculator.instrumentElevation(rsElevation, rsBacksight)
        } else {
            null
        }

        val manhole1Elevation = NumberParser.parseDecimal(state.manhole1ElevationInput)
        val manhole2Elevation = NumberParser.parseDecimal(state.manhole2ElevationInput)
        val parsedDistance = NumberParser.parseDecimal(state.distanceInput)
        val positiveDistance = parsedDistance?.takeIf { it > 0.0 }
        val pipeLength = NumberParser.parseDecimal(state.pipeLengthInput)?.takeIf { it > 0.0 }
        val pipeTopOffset = NumberParser.parseDecimal(state.pipeTopOffsetInput)?.takeIf { it >= 0.0 }
        val calculatedPipeCount = if (positiveDistance != null && pipeLength != null) {
            LevelingCalculator.pipeCount(positiveDistance, pipeLength)
        } else {
            null
        }
        val distance = positiveDistance
        val isLineReady = manhole1Elevation != null &&
            manhole2Elevation != null &&
            distance != null &&
            pipeLength != null &&
            pipeTopOffset != null &&
            calculatedPipeCount != null

        val canReusePlan = isLineReady &&
            state.isLineReady &&
            state.manhole1Elevation == manhole1Elevation &&
            state.manhole2Elevation == manhole2Elevation &&
            state.distance == distance &&
            state.pipeLength == pipeLength &&
            state.pipeTopOffset == pipeTopOffset &&
            state.instrumentElevation == instrumentElevation

        val pipePoints = if (canReusePlan) {
            state.pipePoints
        } else if (isLineReady) {
            LevelingCalculator.generatePipePoints(
                startElevation = manhole1Elevation,
                endElevation = manhole2Elevation,
                totalDistance = distance,
                instrumentElevation = instrumentElevation,
                pipeLength = pipeLength,
                staffContactOffset = pipeTopOffset
            )
        } else {
            emptyList()
        }

        val meterPoints = if (canReusePlan) {
            state.meterPoints
        } else if (isLineReady) {
            LevelingCalculator.generateMeterPoints(
                startElevation = manhole1Elevation,
                endElevation = manhole2Elevation,
                totalDistance = distance,
                instrumentElevation = instrumentElevation,
                staffContactOffset = pipeTopOffset
            )
        } else {
            emptyList()
        }

        val selectedPipeNumber = if (pipePoints.isNotEmpty()) {
            (state.selectedPipeNumber ?: 1).coerceIn(1, pipePoints.size)
        } else {
            null
        }
        val selectedPipe = selectedPipeNumber?.let { pipePoints.getOrNull(it - 1) }

        val parsedActualReading = NumberParser.parseDecimal(state.actualReadingInput)
        val actualReading = parsedActualReading?.takeIf { it >= 0.0 }
        val actualPipeTopElevation = if (
            state.isReadingChecked && instrumentElevation != null && actualReading != null
        ) {
            LevelingCalculator.pointElevation(instrumentElevation, actualReading)
        } else {
            null
        }
        val actualElevation = actualPipeTopElevation
            ?.minus(pipeTopOffset ?: PIPE_TOP_OFFSET_METERS)
            ?.takeIf { it.isFinite() }
        val correction = if (selectedPipe != null && actualElevation != null) {
            LevelingCalculator.correction(selectedPipe.designElevation, actualElevation)
        } else {
            null
        }

        val slopePerMille = if (isLineReady) {
            (manhole2Elevation - manhole1Elevation) / distance * 1000.0
        } else {
            null
        }

        _uiState.update {
            it.copy(
                rsElevation = rsElevation,
                rsBacksight = rsBacksight,
                instrumentElevation = instrumentElevation,
                manhole1Elevation = manhole1Elevation,
                manhole2Elevation = manhole2Elevation,
                distance = distance,
                pipeLength = pipeLength,
                pipeTopOffset = pipeTopOffset,
                slopePerMille = slopePerMille,
                pipeCount = calculatedPipeCount,
                pipePoints = pipePoints,
                meterPoints = meterPoints,
                selectedPipeNumber = selectedPipeNumber,
                selectedPipe = selectedPipe,
                actualReading = actualReading,
                actualPipeTopElevation = actualPipeTopElevation,
                actualElevation = actualElevation,
                correction = correction,
                isInstrumentSet = instrumentElevation != null,
                isLineReady = isLineReady,
                rsElevationError = invalidSignedInput(state.rsElevationInput, rsElevation),
                rsBacksightError = state.rsBacksightInput.isNotBlank() && rsBacksight == null,
                manhole1ElevationError = invalidSignedInput(
                    state.manhole1ElevationInput,
                    manhole1Elevation
                ),
                manhole2ElevationError = invalidSignedInput(
                    state.manhole2ElevationInput,
                    manhole2Elevation
                ),
                distanceError = state.distanceInput.isNotBlank() &&
                    (positiveDistance == null || (pipeLength != null && calculatedPipeCount == null)),
                pipeLengthError = state.pipeLengthInput.isNotBlank() && pipeLength == null,
                pipeTopOffsetError = state.pipeTopOffsetInput.isNotBlank() && pipeTopOffset == null,
                actualReadingError = state.actualReadingInput.isNotBlank() && actualReading == null
            )
        }
    }

    private fun invalidSignedInput(input: String, parsed: Double?): Boolean {
        return input.isNotBlank() && input != "-" && parsed == null
    }

    private fun toggleSign(value: String): String {
        return if (value.startsWith("-")) value.drop(1) else "-$value"
    }
}
