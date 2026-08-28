import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

target = "class SignalRadarViewModel(application: Application) : AndroidViewModel(application) {"

stats_class = """data class BaselineStats(
    val observations: Long = 0L,
    val avgActiveBlips: Float = 0f,
    val avgFreqOccupancy: Float = 0f,
    val startedAtMs: Long = 0L
)

"""

if "data class BaselineStats" not in content:
    content = content.replace(target, stats_class + target)
    
collects = """
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                settingsDataStore.baselineObservations,
                settingsDataStore.baselineAvgActiveBlips,
                settingsDataStore.baselineAvgFreqOccupancy,
                settingsDataStore.baselineStartedAtMs
            ) { obs, blips, freq, started ->
                BaselineStats(obs, blips, freq, started)
            }.collect { stats ->
                cachedBaselineStats = stats
            }
        }
"""

if "cachedBaselineStats = stats" not in content:
    content = content.replace("cachedFingerprints = entities.associateBy { it.id }.mapValues { it.value.toDomainModel() }\n            }\n        }", "cachedFingerprints = entities.associateBy { it.id }.mapValues { it.value.toDomainModel() }\n            }\n        }" + collects)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
