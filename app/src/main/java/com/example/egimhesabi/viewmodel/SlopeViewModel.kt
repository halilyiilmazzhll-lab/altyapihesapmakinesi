package com.example.egimhesabi.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.egimhesabi.data.HistoryDao
import com.example.egimhesabi.data.HistoryEntry
import com.example.egimhesabi.data.SettingsDataStore
import com.example.egimhesabi.domain.CalculationInput
import com.example.egimhesabi.domain.CalculationResult
import com.example.egimhesabi.domain.SlopeCalculator
import com.example.egimhesabi.domain.SlopeStatus
import com.example.egimhesabi.domain.StakeoutCalculationSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class SlopeUiState(
    val baca1Source: StakeoutCalculationSource? = null,
    val baca2Source: StakeoutCalculationSource? = null,
    val baca1Name: String = "",
    val baca2Name: String = "",
    val baca1KapakKotu: String = "",
    val baca1AkarKotu: String = "",
    val baca2KapakKotu: String = "",
    val baca2AkarKotu: String = "",
    val mesafe: String = "",
    val minSlopePercent: Double = 0.5,
    val maxSlopePercent: Double = 5.0,
    val minSlopeRatio: Double = 200.0,
    val maxSlopeRatio: Double = 20.0,
    val calculation: CalculationResult = CalculationResult(),
    val historySaved: Boolean = false,
    val historySaving: Boolean = false,
    val historySaveError: String? = null
)

class SlopeViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val historyDao: HistoryDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_B1_NAME = "b1_name"
        private const val KEY_B2_NAME = "b2_name"
        private const val KEY_B1_KAPAK = "b1_kapak"
        private const val KEY_B1_AKAR = "b1_akar"
        private const val KEY_B2_KAPAK = "b2_kapak"
        private const val KEY_B2_AKAR = "b2_akar"
        private const val KEY_MESAFE = "mesafe"
        private const val KEY_B1_SOURCE = "b1_stakeout_source"
        private const val KEY_B2_SOURCE = "b2_stakeout_source"
    }

    private val _uiState = MutableStateFlow(
        SlopeUiState(
            baca1Source = restoreSource(KEY_B1_SOURCE),
            baca2Source = restoreSource(KEY_B2_SOURCE),
            baca1Name = savedStateHandle[KEY_B1_NAME] ?: "",
            baca2Name = savedStateHandle[KEY_B2_NAME] ?: "",
            baca1KapakKotu = savedStateHandle[KEY_B1_KAPAK] ?: "",
            baca1AkarKotu = savedStateHandle[KEY_B1_AKAR] ?: "",
            baca2KapakKotu = savedStateHandle[KEY_B2_KAPAK] ?: "",
            baca2AkarKotu = savedStateHandle[KEY_B2_AKAR] ?: "",
            mesafe = savedStateHandle[KEY_MESAFE] ?: ""
        )
    )
    val uiState: StateFlow<SlopeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.slopeLimits.collect { limits ->
                val minRatio = SlopeCalculator.percentToRatio(limits.minSlopePercent)
                val maxRatio = SlopeCalculator.percentToRatio(limits.maxSlopePercent)
                _uiState.update {
                    it.copy(
                        minSlopePercent = limits.minSlopePercent,
                        maxSlopePercent = limits.maxSlopePercent,
                        minSlopeRatio = minRatio,
                        maxSlopeRatio = maxRatio
                    )
                }
                recalculate()
            }
        }
        recalculate()
    }

    fun updateBaca1Name(value: String) {
        savedStateHandle[KEY_B1_NAME] = value
        savedStateHandle.remove<String>(KEY_B1_SOURCE)
        _uiState.update { it.copy(baca1Name = value, baca1Source = null, historySaved = false, historySaveError = null) }
    }

    fun updateBaca2Name(value: String) {
        savedStateHandle[KEY_B2_NAME] = value
        savedStateHandle.remove<String>(KEY_B2_SOURCE)
        _uiState.update { it.copy(baca2Name = value, baca2Source = null, historySaved = false, historySaveError = null) }
    }

    fun updateBaca1KapakKotu(value: String) {
        savedStateHandle[KEY_B1_KAPAK] = value
        savedStateHandle.remove<String>(KEY_B1_SOURCE)
        _uiState.update { it.copy(baca1KapakKotu = value, baca1Source = null, historySaved = false, historySaveError = null) }
        recalculate()
    }

    fun updateBaca1AkarKotu(value: String) {
        savedStateHandle[KEY_B1_AKAR] = value
        savedStateHandle.remove<String>(KEY_B1_SOURCE)
        _uiState.update { it.copy(baca1AkarKotu = value, baca1Source = null, historySaved = false, historySaveError = null) }
        recalculate()
    }

    fun updateBaca2KapakKotu(value: String) {
        savedStateHandle[KEY_B2_KAPAK] = value
        savedStateHandle.remove<String>(KEY_B2_SOURCE)
        _uiState.update { it.copy(baca2KapakKotu = value, baca2Source = null, historySaved = false, historySaveError = null) }
        recalculate()
    }

    fun updateBaca2AkarKotu(value: String) {
        savedStateHandle[KEY_B2_AKAR] = value
        savedStateHandle.remove<String>(KEY_B2_SOURCE)
        _uiState.update { it.copy(baca2AkarKotu = value, baca2Source = null, historySaved = false, historySaveError = null) }
        recalculate()
    }

    fun importBaca1(source: StakeoutCalculationSource) {
        if (source.invertLevel == null) return
        savedStateHandle[KEY_B1_NAME] = source.manholeName
        savedStateHandle[KEY_B1_KAPAK] = source.upperText
        savedStateHandle[KEY_B1_AKAR] = source.invertText
        savedStateHandle[KEY_B1_SOURCE] = Json.encodeToString(source)
        _uiState.update {
            it.copy(
                baca1Name = source.manholeName,
                baca1KapakKotu = source.upperText,
                baca1AkarKotu = source.invertText,
                baca1Source = source,
                historySaved = false,
                historySaveError = null
            )
        }
        recalculate()
    }

    fun importBaca2(source: StakeoutCalculationSource) {
        if (source.invertLevel == null) return
        savedStateHandle[KEY_B2_NAME] = source.manholeName
        savedStateHandle[KEY_B2_KAPAK] = source.upperText
        savedStateHandle[KEY_B2_AKAR] = source.invertText
        savedStateHandle[KEY_B2_SOURCE] = Json.encodeToString(source)
        _uiState.update {
            it.copy(
                baca2Name = source.manholeName,
                baca2KapakKotu = source.upperText,
                baca2AkarKotu = source.invertText,
                baca2Source = source,
                historySaved = false,
                historySaveError = null
            )
        }
        recalculate()
    }

    private fun restoreSource(key: String): StakeoutCalculationSource? =
        savedStateHandle.get<String>(key)?.let { raw ->
            runCatching { Json.decodeFromString<StakeoutCalculationSource>(raw) }.getOrNull()
        }

    fun updateMesafe(value: String) {
        savedStateHandle[KEY_MESAFE] = value
        _uiState.update { it.copy(mesafe = value, historySaved = false, historySaveError = null) }
        recalculate()
    }

    fun clearAll() {
        savedStateHandle.remove<String>(KEY_B1_SOURCE)
        savedStateHandle.remove<String>(KEY_B2_SOURCE)
        listOf(KEY_B1_NAME, KEY_B2_NAME, KEY_B1_KAPAK, KEY_B1_AKAR, KEY_B2_KAPAK, KEY_B2_AKAR, KEY_MESAFE).forEach {
            savedStateHandle[it] = ""
        }
        _uiState.update {
            SlopeUiState(
                minSlopePercent = it.minSlopePercent,
                maxSlopePercent = it.maxSlopePercent,
                minSlopeRatio = it.minSlopeRatio,
                maxSlopeRatio = it.maxSlopeRatio
            )
        }
    }

    private fun recalculate() {
        val state = _uiState.value
        val input = CalculationInput(
            baca1KapakKotu = state.baca1KapakKotu,
            baca1AkarKotu = state.baca1AkarKotu,
            baca2KapakKotu = state.baca2KapakKotu,
            baca2AkarKotu = state.baca2AkarKotu,
            mesafe = state.mesafe,
            minSlopePercent = state.minSlopePercent,
            maxSlopePercent = state.maxSlopePercent
        )
        val result = SlopeCalculator.calculate(input)
        _uiState.update { it.copy(calculation = result) }
    }

    fun saveCurrentCalculation() {
        val state = _uiState.value
        val result = state.calculation

        if (state.historySaved || state.historySaving) return

        val slopePercent = result.slopePercent
        val b1Akar = result.b1Akar
        val b2Akar = result.b2Akar
        val mesafe = result.mesafe
        if (slopePercent == null || b1Akar == null || b2Akar == null || mesafe == null) {
            _uiState.update {
                it.copy(historySaveError = "Kaydetmek için iki akar kotunu ve mesafeyi girin.")
            }
            return
        }

        _uiState.update { it.copy(historySaving = true, historySaveError = null) }

        viewModelScope.launch {
            runCatching {
                historyDao.insert(
                    HistoryEntry(
                        baca1Name = state.baca1Name.ifBlank { "B1" },
                        baca2Name = state.baca2Name.ifBlank { "B2" },
                        // Kapak kotları eğim hesabı için zorunlu değildir.
                        baca1KapakKotu = result.b1Kapak,
                        baca1AkarKotu = b1Akar,
                        baca2KapakKotu = result.b2Kapak,
                        baca2AkarKotu = b2Akar,
                        mesafe = mesafe,
                        slopePercent = slopePercent,
                        slopeRatio = result.slopeRatio ?: 0.0,
                        heightDiff = result.heightDiff ?: 0.0,
                        slopeStatus = result.slopeStatus.name,
                        slopeDirection = result.slopeDirection
                    )
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(historySaved = true, historySaving = false, historySaveError = null)
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        historySaving = false,
                        historySaveError = "Hesap geçmişe kaydedilemedi. Lütfen tekrar deneyin."
                    )
                }
            }
        }
    }
}
