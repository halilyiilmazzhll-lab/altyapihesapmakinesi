package com.example.egimhesabi.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.egimhesabi.domain.StakeoutCalculationSource
import com.example.egimhesabi.domain.LevelingCalculator
import com.example.egimhesabi.util.NumberParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ElevationTransferUiState(
    val rsSource: StakeoutCalculationSource? = null,
    val rsElevationInput: String = "",
    val rsReadingInput: String = "",
    val targetReadingInput: String = "",
    val rsElevation: Double? = null,
    val rsReading: Double? = null,
    val targetReading: Double? = null,
    val instrumentElevation: Double? = null,
    val targetElevation: Double? = null,
    val heightDifference: Double? = null,
    val rsElevationError: Boolean = false,
    val rsReadingError: Boolean = false,
    val targetReadingError: Boolean = false
)

class ElevationTransferViewModel(private val savedState: SavedStateHandle = SavedStateHandle()) : ViewModel() {
    private val _uiState = MutableStateFlow(ElevationTransferUiState(
        rsElevationInput = savedState["rsElevationInput"] ?: "",
        rsReadingInput = savedState["rsReadingInput"] ?: "",
        targetReadingInput = savedState["targetReadingInput"] ?: ""
    ))
    val uiState: StateFlow<ElevationTransferUiState> = _uiState.asStateFlow()

    init { recalculate() }

    fun updateRsElevation(value: String) {
        _uiState.update { it.copy(rsElevationInput = value, rsSource = null) }
        recalculate()
    }

    fun importRs(source: StakeoutCalculationSource) {
        if (source.upperLevel == null) return
        _uiState.update { it.copy(rsElevationInput = source.upperText, rsSource = source) }
        recalculate()
    }

    fun updateRsReading(value: String) {
        _uiState.update { it.copy(rsReadingInput = value) }
        recalculate()
    }

    fun updateTargetReading(value: String) {
        _uiState.update { it.copy(targetReadingInput = value) }
        recalculate()
    }

    fun toggleRsSign() {
        val current = _uiState.value.rsElevationInput
        updateRsElevation(if (current.startsWith("-")) current.drop(1) else "-$current")
    }

    fun clearAll() {
        _uiState.value = ElevationTransferUiState()
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        savedState["rsElevationInput"] = state.rsElevationInput
        savedState["rsReadingInput"] = state.rsReadingInput
        savedState["targetReadingInput"] = state.targetReadingInput
        val rsElevation = NumberParser.parseDecimal(state.rsElevationInput)
        val rsReading = NumberParser.parseDecimal(state.rsReadingInput)?.takeIf { it >= 0.0 }
        val targetReading = NumberParser.parseDecimal(state.targetReadingInput)?.takeIf { it >= 0.0 }
        val instrumentElevation = if (rsElevation != null && rsReading != null) {
            LevelingCalculator.instrumentElevation(rsElevation, rsReading)
        } else null
        val targetElevation = if (instrumentElevation != null && targetReading != null) {
            LevelingCalculator.pointElevation(instrumentElevation, targetReading)
        } else null
        val heightDifference = if (rsReading != null && targetReading != null) {
            LevelingCalculator.heightDifference(rsReading, targetReading)
        } else null

        _uiState.update {
            it.copy(
                rsElevation = rsElevation,
                rsReading = rsReading,
                targetReading = targetReading,
                instrumentElevation = instrumentElevation,
                targetElevation = targetElevation,
                heightDifference = heightDifference,
                rsElevationError = state.rsElevationInput.isNotBlank() &&
                    state.rsElevationInput != "-" &&
                    rsElevation == null,
                rsReadingError = state.rsReadingInput.isNotBlank() && rsReading == null,
                targetReadingError = state.targetReadingInput.isNotBlank() && targetReading == null
            )
        }
    }
}
