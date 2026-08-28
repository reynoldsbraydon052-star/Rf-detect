import re

with open('app/src/main/java/com/example/SettingsDataStore.kt', 'r') as f:
    content = f.read()

target = """    val showCoverageZones: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SHOW_COVERAGE_ZONES] ?: true
    }"""
    
replacement = target + """
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
    
if "isBaselineLearningMode" not in content:
    content = content.replace(target, replacement)

with open('app/src/main/java/com/example/SettingsDataStore.kt', 'w') as f:
    f.write(content)
