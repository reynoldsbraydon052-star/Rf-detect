with open('app/src/main/java/com/example/SettingsDataStore.kt', 'r') as f:
    content = f.read()

import re

# Add imports
if "androidx.datastore.preferences.core.longPreferencesKey" not in content:
    content = content.replace("import androidx.datastore.preferences.core.intPreferencesKey",
                              "import androidx.datastore.preferences.core.intPreferencesKey\nimport androidx.datastore.preferences.core.longPreferencesKey")

# Add keys
if "KEY_BASELINE_LEARNING_MODE" not in content:
    keys_target = 'val KEY_SHOW_COVERAGE_ZONES = booleanPreferencesKey("show_coverage_zones")'
    keys_replacement = keys_target + """
        val KEY_BASELINE_LEARNING_MODE = booleanPreferencesKey("baseline_learning_mode")
        val KEY_BASELINE_STARTED_AT_MS = longPreferencesKey("baseline_started_at_ms")
        val KEY_BASELINE_OBSERVATIONS = longPreferencesKey("baseline_observations")
        val KEY_BASELINE_AVG_ACTIVE_BLIPS = androidx.datastore.preferences.core.floatPreferencesKey("baseline_avg_active_blips")
        val KEY_BASELINE_AVG_FREQ_OCCUPANCY = androidx.datastore.preferences.core.floatPreferencesKey("baseline_avg_freq_occupancy")"""
    content = content.replace(keys_target, keys_replacement)

# Add flows
if "val isBaselineLearningMode" not in content:
    flows_target = 'val showCoverageZones: Flow<Boolean> = context.dataStore.data.map {\n        preferences[KEY_SHOW_COVERAGE_ZONES] ?: true\n    }'
    flows_replacement = flows_target + """
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
    }"""
    content = content.replace(flows_target, flows_replacement)

# Add mutators
if "suspend fun updateBaselineLearningMode" not in content:
    mutators_target = 'suspend fun clearAllSettings() {'
    mutators_replacement = """suspend fun updateBaselineLearningMode(enabled: Boolean) {
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

    suspend fun clearAllSettings() {"""
    content = content.replace(mutators_target, mutators_replacement)

with open('app/src/main/java/com/example/SettingsDataStore.kt', 'w') as f:
    f.write(content)
