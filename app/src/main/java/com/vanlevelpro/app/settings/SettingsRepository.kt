package com.vanlevelpro.app.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the user-adjustable green/yellow level thresholds (in
 * degrees) used across the gauges and level indicators.
 *
 * - Below [greenThreshold]  -> green ("level")
 * - Below [yellowThreshold] -> yellow ("close")
 * - Above [yellowThreshold] -> red ("not level")
 */
class SettingsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "VanLevelSettings"
        private const val KEY_GREEN_THRESHOLD = "green_threshold_deg"
        private const val KEY_YELLOW_THRESHOLD = "yellow_threshold_deg"

        const val DEFAULT_GREEN_THRESHOLD = 0.5f
        const val DEFAULT_YELLOW_THRESHOLD = 2.0f

        const val MIN_GREEN_THRESHOLD = 0.1f
        const val MAX_YELLOW_THRESHOLD = 10.0f
    }

    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _greenThreshold = MutableStateFlow(
        prefs.getFloat(KEY_GREEN_THRESHOLD, DEFAULT_GREEN_THRESHOLD)
    )

    val greenThreshold: StateFlow<Float> =
        _greenThreshold.asStateFlow()

    private val _yellowThreshold = MutableStateFlow(
        prefs.getFloat(KEY_YELLOW_THRESHOLD, DEFAULT_YELLOW_THRESHOLD)
    )

    val yellowThreshold: StateFlow<Float> =
        _yellowThreshold.asStateFlow()

    //--------------------------------------------------
    // Setters
    //--------------------------------------------------

    fun setGreenThreshold(value: Float) {

        val clamped = value.coerceIn(MIN_GREEN_THRESHOLD, _yellowThreshold.value)

        _greenThreshold.value = clamped

        prefs.edit().putFloat(KEY_GREEN_THRESHOLD, clamped).apply()
    }

    fun setYellowThreshold(value: Float) {

        val clamped = value.coerceIn(_greenThreshold.value, MAX_YELLOW_THRESHOLD)

        _yellowThreshold.value = clamped

        prefs.edit().putFloat(KEY_YELLOW_THRESHOLD, clamped).apply()
    }

    fun resetToDefaults() {
        setGreenThreshold(DEFAULT_GREEN_THRESHOLD)
        setYellowThreshold(DEFAULT_YELLOW_THRESHOLD)
    }
}
