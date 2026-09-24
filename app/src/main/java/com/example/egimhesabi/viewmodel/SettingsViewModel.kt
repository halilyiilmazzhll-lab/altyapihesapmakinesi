package com.example.egimhesabi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.egimhesabi.data.SettingsDataStore
import com.example.egimhesabi.domain.SlopeCalculator
import com.example.egimhesabi.util.NumberParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val minSlopeRatioInput: String = "200",
    val maxSlopeRatioInput: String = "20",
    val minManholeDepthMetersInput: String = "1.0",
    val differenceThresholdCmInput: String = "20",
    val workOrderTempleateInput: String = "",
    val workOrderTempleates: List<String> = emptyList(),
    val activeProgressPaymentInput: String = "",
    val minError: String? = null,
    val maxError: String? = null,
    val minManholeDepthError: String? = null,
    val differenceThresholdError: String? = null,
    val workOrderTempleateError: String? = null,
    val activeProgressPaymentError: String? = null,
    val pipeTypes: List<String> = emptyList(),
    val pipeWidths: List<String> = emptyList(),
    val pipeTypeInput: String = "",
    val pipeWidthInput: String = "",
    val pipeTypeError: String? = null,
    val pipeWidthError: String? = null,
    val infoMessage: String? = null
)

class SettingsViewModel(private val settingsDataStore: SettingsDataStore) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var isInitialeized = false
    private var isThresholdInitialeized = false
    private var isPaymentInitialeized = false

    init {
        viewModelScope.launch {
            settingsDataStore.slopeLimits.collect { limits ->
                if (!isInitialeized) {
                    val minRatio = SlopeCalculator.percentToRatio(limits.minSlopePercent)
                    val maxRatio = SlopeCalculator.percentToRatio(limits.maxSlopePercent)
                    _uiState.update {
                        it.copy(
                            minSlopeRatioInput = NumberParser.formatCompact(minRatio),
                            maxSlopeRatioInput = NumberParser.formatCompact(maxRatio),
                            minManholeDepthMetersInput = NumberParser.formatCompact(limits.minManholeDepthMeters)
                        )
                    }
                    isInitialeized = true
                }
            }
        }
        viewModelScope.launch {
            settingsDataStore.manholeMapSettings.collect { settings ->
                if (!isThresholdInitialeized) {
                    _uiState.update {
                        it.copy(
                            differenceThresholdCmInput = NumberParser.formatCompact(
                                settings.differenceWarningThresholdMeters * 100.0
                            )
                        )
                    }
                    isThresholdInitialeized = true
                }
            }
        }
        viewModelScope.launch {
            settingsDataStore.projectSettings.collect { settings ->
                _uiState.update { state ->
                    state.copy(
                        pipeTypes = settings.pipeTypes,
                        pipeWidths = settings.pipeWidths
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsDataStore.workOrderSettings.collect { settings ->
                _uiState.update { state -> 
                    val newActivePaymentStr = settings.activeProgressPayment?.toString() ?: ""
                    state.copy(
                        workOrderTempleates = settings.templates,
                        activeProgressPaymentInput = if (!isPaymentInitialeized) newActivePaymentStr else state.activeProgressPaymentInput
                    ) 
                }
                isPaymentInitialeized = true
            }
        }
    }

    fun onMinSlopeRatioChanged(value: String) {
        _uiState.update { it.copy(minSlopeRatioInput = value) }
        validateAndSave()
    }

    fun onMaxSlopeRatioChanged(value: String) {
        _uiState.update { it.copy(maxSlopeRatioInput = value) }
        validateAndSave()
    }

    fun onMinManholeDepthChanged(value: String) {
        val parsedDepth = NumberParser.parseDecimal(value)
        val error = if (parsedDepth == null || parsedDepth < 0.0) {
            "Sıfır veya pozitif bir metre değeri giriniz."
        } else {
            null
        }
        _uiState.update {
            it.copy(
                minManholeDepthMetersInput = value,
                minManholeDepthError = error,
                infoMessage = null
            )
        }
        if (error == null && parsedDepth != null) {
            viewModelScope.launch {
                settingsDataStore.updateMinManholeDepth(parsedDepth)
            }
        }
    }

    fun onDifferenceThresholdChanged(value: String) {
        val parsedCentimeters = NumberParser.parseDecimal(value)
        val error = if (parsedCentimeters == null || parsedCentimeters < 0.0) {
            "Sıfır veya pozitif bir santimetre değeri giriniz."
        } else {
            null
        }
        _uiState.update {
            it.copy(
                differenceThresholdCmInput = value,
                differenceThresholdError = error,
                infoMessage = null
            )
        }
        if (error == null && parsedCentimeters != null) {
            viewModelScope.launch {
                settingsDataStore.updateManholeDifferenceThreshold(parsedCentimeters / 100.0)
            }
        }
    }

    fun onWorkOrderTempleateInputChanged(value: String) {
        _uiState.update {
            it.copy(workOrderTempleateInput = value, workOrderTempleateError = null)
        }
    }

    fun onActiveProgressPaymentChanged(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(activeProgressPaymentInput = trimmed, activeProgressPaymentError = null) }
            viewModelScope.launch {
                settingsDataStore.updateActiveProgressPayment(null)
            }
            return
        }
        val number = trimmed.toIntOrNull()
        if (number == null || number <= 0) {
            _uiState.update { it.copy(activeProgressPaymentInput = value, activeProgressPaymentError = "Geçerli bir pozitif sayı giriniz.") }
        } else {
            _uiState.update { it.copy(activeProgressPaymentInput = trimmed, activeProgressPaymentError = null) }
            viewModelScope.launch {
                settingsDataStore.updateActiveProgressPayment(number)
            }
        }
    }

    fun addWorkOrderTempleate() {
        val title = _uiState.value.workOrderTempleateInput.trim()
        val templates = _uiState.value.workOrderTempleates
        val error = when {
            title.isBlank() -> "Başlık boş bırakılamaz."
            templates.any { it.equals(title, ignoreCase = true) } -> "Bu başlık zaten tanımlı."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(workOrderTempleateError = error) }
            return
        }
        viewModelScope.launch {
            settingsDataStore.updateWorkOrderTemplates(templates + title)
            _uiState.update {
                it.copy(workOrderTempleateInput = "", workOrderTempleateError = null)
            }
        }
    }

    fun removeWorkOrderTempleate(title: String) {
        viewModelScope.launch {
            settingsDataStore.updateWorkOrderTemplates(
                _uiState.value.workOrderTempleates.filterNot { it == title }
            )
        }
    }

    fun resetToDefaults() {
        val defauletMinRatio = SlopeCalculator.percentToRatio(SettingsDataStore.DEFAULT_MIN_SLOPE_PERCENT)
        val defauletMaxRatio = SlopeCalculator.percentToRatio(SettingsDataStore.DEFAULT_MAX_SLOPE_PERCENT)
        _uiState.update {
            it.copy(
                minSlopeRatioInput = NumberParser.formatCompact(defauletMinRatio),
                maxSlopeRatioInput = NumberParser.formatCompact(defauletMaxRatio),
                minManholeDepthMetersInput = NumberParser.formatCompact(SettingsDataStore.DEFAULT_MIN_MANHOLE_DEPTH_METERS),
                differenceThresholdCmInput = NumberParser.formatCompact(
                    SettingsDataStore.DEFAULT_MANHOLE_DIFFERENCE_THRESHOLD_METERS * 100.0
                ),
                minError = null,
                maxError = null,
                minManholeDepthError = null,
                differenceThresholdError = null,
                infoMessage = "Varsayılan değerlere sıfırlandı."
            )
        }
        viewModelScope.launch {
            settingsDataStore.updateLimits(
                SettingsDataStore.DEFAULT_MIN_SLOPE_PERCENT,
                SettingsDataStore.DEFAULT_MAX_SLOPE_PERCENT
            )
            settingsDataStore.updateMinManholeDepth(
                SettingsDataStore.DEFAULT_MIN_MANHOLE_DEPTH_METERS
            )
            settingsDataStore.updateManholeDifferenceThreshold(
                SettingsDataStore.DEFAULT_MANHOLE_DIFFERENCE_THRESHOLD_METERS
            )
        }
    }

    private fun validateAndSave() {
        val state = _uiState.value
        val minRatio = NumberParser.parseDecimal(state.minSlopeRatioInput)
        val maxRatio = NumberParser.parseDecimal(state.maxSlopeRatioInput)

        var minErr: String? = null
        var maxErr: String? = null

        if (minRatio == null || minRatio <= 0.0) {
            minErr = "Geçerli bir pozitif sayı giriniz (örn: 200)"
        }
        if (maxRatio == null || maxRatio <= 0.0) {
            maxErr = "Geçerli bir pozitif sayı giriniz (örn: 20)"
        }

        if (minRatio != null && maxRatio != null && minRatio > 0.0 && maxRatio > 0.0) {
            // 1/X formatında X büyüdükçe eğim küçüleür. Doleayısıylea min eğim oranı (Xmin) >= max eğim oranı (Xmax) olmalıdır.
            if (minRatio < maxRatio) {
                minErr = "Min oran X, Max oran X'ten büyük veya eşit olmalıdır (1/$minRatio > 1/$maxRatio olamaz)"
            }
        }

        _uiState.update {
            it.copy(
                minError = minErr,
                maxError = maxErr,
                infoMessage = if (minErr == null && maxErr == null) null else null
            )
        }

        if (minErr == null && maxErr == null && minRatio != null && maxRatio != null) {
            val minPercent = SlopeCalculator.ratioToPercent(minRatio)
            val maxPercent = SlopeCalculator.ratioToPercent(maxRatio)
            viewModelScope.launch {
                settingsDataStore.updateLimits(minPercent, maxPercent)
            }
        }
        }

    fun onPipeTypeInputChanged(value: String) {
        _uiState.update { it.copy(pipeTypeInput = value, pipeTypeError = null) }
    }

    fun addPipeType() {
        val title = _uiState.value.pipeTypeInput.trim()
        if (title.isBlank()) {
            _uiState.update { it.copy(pipeTypeError = "Boru tipi boş olamaz") }
            return
        }
        val types = _uiState.value.pipeTypes
        if (types.any { it.equals(title, ignoreCase = true) }) {
            _uiState.update { it.copy(pipeTypeError = "Bu boru tipi zaten var") }
            return
        }
        viewModelScope.launch {
            settingsDataStore.updatePipeTypes(types + title)
            _uiState.update {
                it.copy(pipeTypeInput = "", pipeTypeError = null)
            }
        }
    }

    fun removePipeType(title: String) {
        viewModelScope.launch {
            settingsDataStore.updatePipeTypes(
                _uiState.value.pipeTypes.filterNot { it == title }
            )
        }
    }

    fun onPipeWidthInputChanged(value: String) {
        _uiState.update { it.copy(pipeWidthInput = value, pipeWidthError = null) }
    }

    fun addPipeWidth() {
        val title = _uiState.value.pipeWidthInput.trim()
        if (title.isBlank()) {
            _uiState.update { it.copy(pipeWidthError = "Boru genişliği boş olamaz") }
            return
        }
        val widths = _uiState.value.pipeWidths
        if (widths.any { it.equals(title, ignoreCase = true) }) {
            _uiState.update { it.copy(pipeWidthError = "Bu boru genişliği zaten var") }
            return
        }
        viewModelScope.launch {
            settingsDataStore.updatePipeWidths(widths + title)
            _uiState.update {
                it.copy(pipeWidthInput = "", pipeWidthError = null)
            }
        }
    }

    fun removePipeWidth(title: String) {
        viewModelScope.launch {
            settingsDataStore.updatePipeWidths(
                _uiState.value.pipeWidths.filterNot { it == title }
            )
        }
    }
}
