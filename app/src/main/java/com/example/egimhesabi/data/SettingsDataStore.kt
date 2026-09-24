package com.example.egimhesabi.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class SlopeLimits(
    val minSlopePercent: Double = 0.5,
    val maxSlopePercent: Double = 5.0,
    val minManholeDepthMeters: Double = 1.0
)

data class ManholeMapSettings(
    val differenceWarningThresholdMeters: Double = 0.20
)

data class ProjectSettings(
    val pipeTypes: List<String> = emptyList(),
    val pipeWidths: List<String> = emptyList()
)

data class WorkOrderSettings(
    val templates: List<String> = emptyList(),
    val activeProgressPayment: Int? = null
)

class SettingsDataStore(private val context: Context) {

    companion object {
        val MIN_SLOPE_PERCENT_KEY = doublePreferencesKey("min_slope_percent")
        val MAX_SLOPE_PERCENT_KEY = doublePreferencesKey("max_slope_percent")
        val MIN_MANHOLE_DEPTH_KEY = doublePreferencesKey("min_manhole_depth")
        val MANHOLE_DIFFERENCE_THRESHOLD_KEY = doublePreferencesKey("manhole_difference_threshold_meters")
        val WORK_ORDER_TEMPLATES_KEY = stringSetPreferencesKey("work_order_templates")
        val ACTIVE_PROGRESS_PAYMENT_KEY = androidx.datastore.preferences.core.intPreferencesKey("active_progress_payment")
        const val DEFAULT_MIN_SLOPE_PERCENT = 0.5
        const val DEFAULT_MAX_SLOPE_PERCENT = 5.0
        const val DEFAULT_MIN_MANHOLE_DEPTH_METERS = 1.0
        const val DEFAULT_MANHOLE_DIFFERENCE_THRESHOLD_METERS = 0.20
        val PIPE_TYPES_KEY = stringSetPreferencesKey("pipe_types")
        val PIPE_WIDTHS_KEY = stringSetPreferencesKey("pipe_widths")
        val DEFAULT_PIPE_TYPES = setOf("Koruge", "Beton", "HDPE")
        val DEFAULT_PIPE_WIDTHS = setOf("200mm", "300mm", "400mm", "500mm", "600mm")
    }

    val slopeLimits: Flow<SlopeLimits> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val min = preferences[MIN_SLOPE_PERCENT_KEY] ?: DEFAULT_MIN_SLOPE_PERCENT
            val max = preferences[MAX_SLOPE_PERCENT_KEY] ?: DEFAULT_MAX_SLOPE_PERCENT
            val minDepth = preferences[MIN_MANHOLE_DEPTH_KEY] ?: DEFAULT_MIN_MANHOLE_DEPTH_METERS
            SlopeLimits(minSlopePercent = min, maxSlopePercent = max, minManholeDepthMeters = minDepth)
        }

    val manholeMapSettings: Flow<ManholeMapSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            ManholeMapSettings(
                differenceWarningThresholdMeters =
                    preferences[MANHOLE_DIFFERENCE_THRESHOLD_KEY]
                        ?: DEFAULT_MANHOLE_DIFFERENCE_THRESHOLD_METERS
            )
        }

    val projectSettings: Flow<ProjectSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            ProjectSettings(
                pipeTypes = preferences[PIPE_TYPES_KEY]?.toList()?.sortedWith(String.CASE_INSENSITIVE_ORDER) ?: DEFAULT_PIPE_TYPES.toList().sortedWith(String.CASE_INSENSITIVE_ORDER),
                pipeWidths = preferences[PIPE_WIDTHS_KEY]?.toList()?.sortedWith(String.CASE_INSENSITIVE_ORDER) ?: DEFAULT_PIPE_WIDTHS.toList().sortedWith(String.CASE_INSENSITIVE_ORDER)
            )
        }

    val workOrderSettings: Flow<WorkOrderSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            WorkOrderSettings(
                templates = preferences[WORK_ORDER_TEMPLATES_KEY]
                    .orEmpty()
                    .filter { it.isNotBlank() }
                    .sortedWith(String.CASE_INSENSITIVE_ORDER),
                activeProgressPayment = preferences[ACTIVE_PROGRESS_PAYMENT_KEY]
            )
        }

    suspend fun updateLimits(minPercent: Double, maxPercent: Double) {
        context.dataStore.edit { preferences ->
            preferences[MIN_SLOPE_PERCENT_KEY] = minPercent
            preferences[MAX_SLOPE_PERCENT_KEY] = maxPercent
        }
    }

    suspend fun updateMinManholeDepth(minDepth: Double) {
        context.dataStore.edit { preferences ->
            preferences[MIN_MANHOLE_DEPTH_KEY] = minDepth
        }
    }


    suspend fun updateManholeDifferenceThreshold(thresholdMeters: Double) {
        context.dataStore.edit { preferences ->
            preferences[MANHOLE_DIFFERENCE_THRESHOLD_KEY] = thresholdMeters
        }
    }

    suspend fun updatePipeTypes(types: Collection<String>) {
        context.dataStore.edit { preferences ->
            preferences[PIPE_TYPES_KEY] = types.map(String::trim).filter(String::isNotBlank).toSet()
        }
    }

    suspend fun updatePipeWidths(widths: Collection<String>) {
        context.dataStore.edit { preferences ->
            preferences[PIPE_WIDTHS_KEY] = widths.map(String::trim).filter(String::isNotBlank).toSet()
        }
    }

    suspend fun updateWorkOrderTemplates(templates: Collection<String>) {
        context.dataStore.edit { preferences ->
            preferences[WORK_ORDER_TEMPLATES_KEY] = templates
                .map(String::trim)
                .filter(String::isNotBlank)
                .toSet()
        }
    }

    suspend fun updateActiveProgressPayment(paymentNumber: Int?) {
        context.dataStore.edit { preferences ->
            if (paymentNumber == null) {
                preferences.remove(ACTIVE_PROGRESS_PAYMENT_KEY)
            } else {
                preferences[ACTIVE_PROGRESS_PAYMENT_KEY] = paymentNumber
            }
        }
    }
}

