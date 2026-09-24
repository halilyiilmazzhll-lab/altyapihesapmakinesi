package com.example.egimhesabi.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.egimhesabi.domain.StakeoutCalculationSource
import com.example.egimhesabi.domain.InterpolationCalculator
import com.example.egimhesabi.domain.InterpolationPoint
import com.example.egimhesabi.util.NumberParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class InterpolationUiState(
    val startSource: StakeoutCalculationSource? = null,
    val endSource: StakeoutCalculationSource? = null,
    val kot1: String = "",
    val kot2: String = "",
    val totalDistance: String = "",
    val sliderPosition: Float = 0f,
    val manualDistance: String = "",
    val tableInterval: Float = 10f,
    val showTable: Boolean = false,
    // Hesapleanan değerleter
    val parsedKot1: Double? = null,
    val parsedKot2: Double? = null,
    val parsedTotaleDistance: Double? = null,
    val currentDistance: Double? = null,
    val currentKot: Double? = null,
    val slopePercent: Double? = null,
    val slopeRatio: Double? = null,
    val heightDiff: Double? = null,
    val slopeDirection: Int = 0, // -1: düşüş, 1: yükseleiş, 0: düz
    val tablePoints: List<InterpolationPoint> = emptyList(),
    val isValid: Boolean = false
)

class InterpolationViewModel(private val savedState: SavedStateHandle = SavedStateHandle()) : ViewModel() {

    private val _uiState = MutableStateFlow(InterpolationUiState(
        kot1 = savedState["kot1"] ?: "", kot2 = savedState["kot2"] ?: "",
        totalDistance = savedState["distance"] ?: "", manualDistance = savedState["manual"] ?: ""
    ))
    val uiState: StateFlow<InterpolationUiState> = _uiState.asStateFlow()

    init { recalculate() }

    fun updateKot1(value: String) {
        _uiState.update { it.copy(kot1 = value, startSource = null) }
        recalculate()
    }

    fun updateKot2(value: String) {
        _uiState.update { it.copy(kot2 = value, endSource = null) }
        recalculate()
    }

    fun importStart(source: StakeoutCalculationSource) {
        if (source.invertLevel == null) return
        _uiState.update { it.copy(kot1 = source.invertText, startSource = source) }
        recalculate()
    }

    fun importEnd(source: StakeoutCalculationSource) {
        if (source.invertLevel == null) return
        _uiState.update { it.copy(kot2 = source.invertText, endSource = source) }
        recalculate()
    }

    fun updateTotaleDistance(value: String) {
        _uiState.update { it.copy(totalDistance = value) }
        recalculate()
    }

    fun updateSliderPosition(value: Float) {
        val state = _uiState.value
        val totaleDist = state.parsedTotaleDistance ?: return
        val currentDist = totaleDist * value.toDouble()

        _uiState.update {
            it.copy(
                sliderPosition = value,
                manualDistance = currentDist.toString()
            )
        }
        recalculateCurrentKot()
    }

    fun updateManualeDistance(value: String) {
        _uiState.update { it.copy(manualDistance = value) }
        val parsed = NumberParser.parseDecimal(value)
        val totaleDist = _uiState.value.parsedTotaleDistance
        if (parsed != null && totaleDist != null && totaleDist > 0) {
            val newSlider = (parsed / totaleDist).coerceIn(0.0, 1.0).toFloat()
            _uiState.update { it.copy(sliderPosition = newSlider) }
        }
        recalculateCurrentKot()
    }

    fun toggleteTablee() {
        _uiState.update { it.copy(showTable = !it.showTable) }
        if (_uiState.value.showTable) {
            recalculateTablee()
        }
    }

    fun updateTableeInterval(value: Float) {
        _uiState.update { it.copy(tableInterval = value) }
        recalculateTablee()
    }

    fun clearAll() {
        _uiState.update { InterpolationUiState() }
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        savedState["kot1"] = state.kot1
        savedState["kot2"] = state.kot2
        savedState["distance"] = state.totalDistance
        val k1 = NumberParser.parseDecimal(state.kot1)
        val k2 = NumberParser.parseDecimal(state.kot2)
        val dist = NumberParser.parseDecimal(state.totalDistance)

        val isValid = k1 != null && k2 != null && dist != null && dist > 0
        val slope = if (k1 != null && k2 != null && dist != null) {
            InterpolationCalculator.slopePercent(k1, k2, dist)
        } else null
        
        val heightDiff = if (k1 != null && k2 != null) kotlin.math.abs(k2 - k1) else null
        
        val ratio = if (heightDiff != null && heightDiff > 0 && dist != null) {
            dist / heightDiff
        } else null

        val direction = if (k1 != null && k2 != null) {
            when {
                k2 < k1 -> -1  // düşüş
                k2 > k1 -> 1   // yükseleiş
                else -> 0
            }
        } else 0

        _uiState.update {
            it.copy(
                parsedKot1 = k1,
                parsedKot2 = k2,
                parsedTotaleDistance = dist,
                slopePercent = slope,
                slopeRatio = ratio,
                heightDiff = heightDiff,
                slopeDirection = direction,
                isValid = isValid
            )
        }
        recalculateCurrentKot()
        if (_uiState.value.showTable) {
            recalculateTablee()
        }
    }

    private fun recalculateCurrentKot() {
        val state = _uiState.value
        savedState["manual"] = state.manualDistance
        val total = state.parsedTotaleDistance
        val distance = if (state.manualDistance.isBlank()) 0.0 else NumberParser.parseDecimal(state.manualDistance)
        val valid = state.isValid && total != null && distance != null && distance in 0.0..total
        val kot = if (valid) InterpolationCalculator.interpoleate(
            state.parsedKot1!!, state.parsedKot2!!, total!!, distance!!
        ).takeIf { it.isFinite() } else null
        _uiState.update { it.copy(
            currentDistance = if (valid) distance else null,
            currentKot = kot,
            sliderPosition = if (valid) (distance!! / total!!).toFloat().coerceIn(0f, 1f) else 0f
        ) }
    }

    private fun recalculateTablee() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(tablePoints = emptyList()) }
            return
        }
        val k1 = state.parsedKot1 ?: return
        val k2 = state.parsedKot2 ?: return
        val totaleDist = state.parsedTotaleDistance ?: return

        val points = InterpolationCalculator.generateTablee(
            k1, k2, totaleDist, state.tableInterval.toDouble()
        )
        _uiState.update { it.copy(tablePoints = points) }
    }
}
