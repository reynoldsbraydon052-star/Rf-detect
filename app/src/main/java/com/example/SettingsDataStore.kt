package com.example

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sensed_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val KEY_DEFAULT_RANGE_METERS = intPreferencesKey("default_range_meters")
        val KEY_RSSI_CUTOFF_DBM = intPreferencesKey("rssi_cutoff_dbm")
        val KEY_ENABLE_SMOOTHING_LERP = booleanPreferencesKey("enable_smoothing_lerp")
        val KEY_ENABLE_BACKGROUND_RECON = booleanPreferencesKey("enable_background_recon")
        val KEY_ULTRASONIC_ALERT_THRESHOLD_HZ = intPreferencesKey("ultrasonic_alert_threshold_hz")
        val KEY_ENABLED_CATEGORIES = stringSetPreferencesKey("enabled_categories")
        val KEY_BREACH_PERIMETER_METERS = intPreferencesKey("breach_perimeter_meters")
        val KEY_MAX_VISIBLE_DEVICES = intPreferencesKey("max_visible_devices")
        val KEY_RADAR_GRID_MODE = androidx.datastore.preferences.core.stringPreferencesKey("radar_grid_mode")
        val KEY_RADAR_GRID_OPACITY = androidx.datastore.preferences.core.floatPreferencesKey("radar_grid_opacity")
        val KEY_SHOW_COVERAGE_ZONES = booleanPreferencesKey("show_coverage_zones")
        val KEY_BASELINE_LEARNING_MODE = booleanPreferencesKey("baseline_learning_mode")
        val KEY_BASELINE_STARTED_AT_MS = longPreferencesKey("baseline_started_at_ms")
        val KEY_BASELINE_OBSERVATIONS = longPreferencesKey("baseline_observations")
        val KEY_BASELINE_AVG_ACTIVE_BLIPS = androidx.datastore.preferences.core.floatPreferencesKey("baseline_avg_active_blips")
        val KEY_BASELINE_AVG_FREQ_OCCUPANCY = androidx.datastore.preferences.core.floatPreferencesKey("baseline_avg_freq_occupancy")
        val KEY_AI_INFERENCE_MODE = androidx.datastore.preferences.core.stringPreferencesKey("ai_inference_mode")

        val DEFAULT_CATEGORIES = setOf("MOBILE", "AUDIO_WEAR", "IOT", "WIFI_AP", "BEACONS", "CELLULAR", "ACOUSTIC_EMF")
    }

    val aiInferenceMode: Flow<AiInferenceMode> = context.dataStore.data.map { preferences ->
        val modeStr = preferences[KEY_AI_INFERENCE_MODE] ?: AiInferenceMode.GEMINI_CLOUD.name
        try {
            AiInferenceMode.valueOf(modeStr)
        } catch (e: Exception) {
            AiInferenceMode.GEMINI_CLOUD
        }
    }

    val radarGridModeStr: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_RADAR_GRID_MODE] ?: "POLAR"
    }

    val radarGridOpacity: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_RADAR_GRID_OPACITY] ?: 0.25f
    }

    val showCoverageZones: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SHOW_COVERAGE_ZONES] ?: true
    }
    val isBaselineLearningMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BASELINE_LEARNING_MODE] ?: false
    }
    val baselineStartedAtMs: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_BASELINE_STARTED_AT_MS] ?: 0L
    }
    val baselineObservations: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_BASELINE_OBSERVATIONS] ?: 0L
    }
    val baselineAvgActiveBlips: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_BASELINE_AVG_ACTIVE_BLIPS] ?: 0f
    }
    val baselineAvgFreqOccupancy: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_BASELINE_AVG_FREQ_OCCUPANCY] ?: 0f
    }

    val defaultRangeMeters: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_RANGE_METERS] ?: 25
    }

    val maxVisibleDevices: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_MAX_VISIBLE_DEVICES] ?: 10
    }

    val rssiCutoffDbm: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_RSSI_CUTOFF_DBM] ?: -80
    }

    val enableSmoothingLerp: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ENABLE_SMOOTHING_LERP] ?: true
    }

    val enableBackgroundRecon: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ENABLE_BACKGROUND_RECON] ?: false
    }

    val ultrasonicAlertThresholdHz: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_ULTRASONIC_ALERT_THRESHOLD_HZ] ?: 18000
    }

    val enabledCategories: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_ENABLED_CATEGORIES] ?: DEFAULT_CATEGORIES
    }

    val breachPerimeterMeters: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_BREACH_PERIMETER_METERS] ?: 5
    }

    suspend fun updateDefaultRangeMeters(range: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_RANGE_METERS] = range
        }
    }

    suspend fun updateRssiCutoffDbm(cutoff: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RSSI_CUTOFF_DBM] = cutoff
        }
    }

    suspend fun updateEnableSmoothingLerp(enable: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ENABLE_SMOOTHING_LERP] = enable
        }
    }

    suspend fun updateEnableBackgroundRecon(enable: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ENABLE_BACKGROUND_RECON] = enable
        }
    }

    suspend fun updateUltrasonicAlertThresholdHz(thresholdHz: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ULTRASONIC_ALERT_THRESHOLD_HZ] = thresholdHz
        }
    }

    suspend fun updateEnabledCategories(categories: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ENABLED_CATEGORIES] = categories
        }
    }

    suspend fun updateBreachPerimeterMeters(perimeterMeters: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BREACH_PERIMETER_METERS] = perimeterMeters
        }
    }

    suspend fun updateMaxVisibleDevices(maxDevices: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MAX_VISIBLE_DEVICES] = maxDevices
        }
    }

    suspend fun updateRadarGridMode(modeStr: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RADAR_GRID_MODE] = modeStr
        }
    }

    suspend fun updateRadarGridOpacity(opacity: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RADAR_GRID_OPACITY] = opacity
        }
    }

    suspend fun updateShowCoverageZones(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_COVERAGE_ZONES] = show
        }
    }

    suspend fun updateBaselineLearningMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BASELINE_LEARNING_MODE] = enabled
            if (enabled && (preferences[KEY_BASELINE_STARTED_AT_MS] ?: 0L) == 0L) {
                preferences[KEY_BASELINE_STARTED_AT_MS] = System.currentTimeMillis()
            }
        }
    }
    
    suspend fun resetBaseline() {
        context.dataStore.edit { preferences ->
            preferences[KEY_BASELINE_STARTED_AT_MS] = 0L
            preferences[KEY_BASELINE_OBSERVATIONS] = 0L
            preferences[KEY_BASELINE_AVG_ACTIVE_BLIPS] = 0f
            preferences[KEY_BASELINE_AVG_FREQ_OCCUPANCY] = 0f
        }
    }
    
    suspend fun updateBaselineStats(observations: Long, avgBlips: Float, avgFreq: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BASELINE_OBSERVATIONS] = observations
            preferences[KEY_BASELINE_AVG_ACTIVE_BLIPS] = avgBlips
            preferences[KEY_BASELINE_AVG_FREQ_OCCUPANCY] = avgFreq
        }
    }

    suspend fun updateAiInferenceMode(mode: AiInferenceMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AI_INFERENCE_MODE] = mode.name
        }
    }

    suspend fun clearAllSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
