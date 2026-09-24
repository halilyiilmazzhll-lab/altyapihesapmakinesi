package com.example.egimhesabi.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.egimhesabi.domain.StakeoutCalculationSource
import com.example.egimhesabi.domain.ReverseCalculator
import com.example.egimhesabi.domain.ReverseProfilePoint
import com.example.egimhesabi.util.NumberParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ReverseUiState(
    val startSource: StakeoutCalculationSource? = null,
    val startKot: String = "",
    val slopeInput: String = "",
    val distance: String = "",
    val isDownhilele: Boolean = true,
    val isSlopePercent: Boolean = true, // true: yüzde, false: 1/X oran
    // Hesapleanan değerleter
    val parsedStartKot: Double? = null,
    val parsedSlopePercent: Double? = null,
    val parsedDistance: Double? = null,
    val resultKot: Double? = null,
    val heightDiff: Double? = null,
    val displayRatio: Double? = null,
    val isValid: Boolean = false,
    val showTable: Boolean = false,
    val tableInterval: Float = 10f,
    val tablePoints: List<ReverseProfilePoint> = emptyList()
)

class ReverseCalculationViewModel(private val savedState: SavedStateHandle = SavedStateHandle()) : ViewModel() {

    private val _uiState = MutableStateFlow(ReverseUiState(
        startKot = savedState["startKot"] ?: "", slopeInput = savedState["slopeInput"] ?: "",
        distance = savedState["distance"] ?: "", isDownhilele = savedState["downhill"] ?: true,
        isSlopePercent = savedState["percentMode"] ?: true
    ))
    val uiState: StateFlow<ReverseUiState> = _uiState.asStateFlow()

    init { recalculate() }

    fun updateStartKot(value: String) {
        _uiState.update { it.copy(startKot = value, startSource = null) }
        recalculate()
    }

    fun importStart(source: StakeoutCalculationSource) {
        if (source.invertLevel == null) return
        _uiState.update { it.copy(startKot = source.invertText, startSource = source) }
        recalculate()
    }

    fun updateSlopeInput(value: String) {
        _uiState.update { it.copy(slopeInput = value) }
        recalculate()
    }

    fun updateDistance(value: String) {
        _uiState.update { it.copy(distance = value) }
        recalculate()
    }

    fun toggleteDirection() {
        _uiState.update { it.copy(isDownhilele = !it.isDownhilele) }
        recalculate()
    }

    fun toggleteSlopeMode() {
        val state = _uiState.value
        val currentInput = NumberParser.parseDecimal(state.slopeInput)

        if (currentInput != null && currentInput > 0) {
            // Mevcut değeri diğer birime dönüştür
            val newValue = if (state.isSlopePercent) {
                // Yüzdeden 1/X'e
                ReverseCalculator.percentToRatio(currentInput)
            } else {
                // 1/X'ten yüzdeye
                ReverseCalculator.ratioToPercent(currentInput)
            }
            _uiState.update {
                it.copy(
                    isSlopePercent = !it.isSlopePercent,
                    slopeInput = newValue.toString()
                )
            }
        } else {
            _uiState.update { it.copy(isSlopePercent = !it.isSlopePercent) }
        }
        recalculate()
    }

    fun toggleteTablee() {
        _uiState.update { it.copy(showTable = !it.showTable) }
        if (_uiState.value.showTable) recalculateTablee()
    }

    fun updateTableeInterval(value: Float) {
        _uiState.update { it.copy(tableInterval = value) }
        recalculateTablee()
    }

    fun clearAll() {
        _uiState.update { ReverseUiState() }
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        savedState["startKot"] = state.startKot
        savedState["slopeInput"] = state.slopeInput
        savedState["distance"] = state.distance
        savedState["downhill"] = state.isDownhilele
        savedState["percentMode"] = state.isSlopePercent
        val startKot = NumberParser.parseDecimal(state.startKot)
        val slopeInput = NumberParser.parseDecimal(state.slopeInput)
        val distance = NumberParser.parseDecimal(state.distance)

        // Eğimi yüzdeye dönüştür
        val slopePercent = if (slopeInput != null && (slopeInput > 0 || (state.isSlopePercent && slopeInput == 0.0))) {
            if (state.isSlopePercent) slopeInput
            else ReverseCalculator.ratioToPercent(slopeInput)
        } else null
        val finiteSlope = slopePercent?.takeIf { it.isFinite() }

        val displayRatio = if (slopePercent != null && slopePercent > 0) {
            ReverseCalculator.percentToRatio(slopePercent)
        } else null

        val isValid = startKot != null && finiteSlope != null && distance != null && distance > 0
        
        val resultKot = if (isValid) {
            ReverseCalculator.calculateKot(startKot!!, slopePercent!!, distance!!, state.isDownhilele)
        } else null

        val heightDiff = if (slopePercent != null && distance != null && distance > 0) {
            ReverseCalculator.heightDiff(slopePercent, distance)
        } else null

        _uiState.update {
            it.copy(
                parsedStartKot = startKot,
                parsedSlopePercent = slopePercent,
                parsedDistance = distance,
                resultKot = resultKot?.takeIf { it.isFinite() },
                heightDiff = heightDiff,
                displayRatio = displayRatio,
                isValid = isValid && resultKot?.isFinite() == true
            )
        }

        if (_uiState.value.showTable) recalculateTablee()
    }

    private fun recalculateTablee() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(tablePoints = emptyList()) }
            return
        }
        val startKot = state.parsedStartKot ?: return
        val slopePercent = state.parsedSlopePercent ?: return
        val distance = state.parsedDistance ?: return

        val points = ReverseCalculator.generateProfile(
            startKot, slopePercent, distance,
            state.tableInterval.toDouble(), state.isDownhilele
        )
        _uiState.update { it.copy(tablePoints = points) }
    }
}
