import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

target = """            val breaches = blipsList.count { it.distance < threshold }

            _uiState.update { state ->
                state.copy(
                    activeBlips = blipsList,
                    nearestBlip = nearest,
                    perimeterBreachCount = breaches
                )
            }"""

replacement = """            val breaches = blipsList.count { it.distance < threshold }

            val (processedBlips, summary) = baselineEngine.processBaseline(
                blips = blipsList,
                fingerprintDb = cachedFingerprints,
                isLearning = _uiState.value.baselineSummary.isLearning,
                baselineObservations = cachedBaselineStats.observations,
                baselineAvgActiveBlips = cachedBaselineStats.avgActiveBlips,
                baselineAvgFreqOccupancy = cachedBaselineStats.avgFreqOccupancy,
                baselineStartedAtMs = cachedBaselineStats.startedAtMs
            )
            
            if (_uiState.value.baselineSummary.isLearning) {
                // Update running averages
                val currentObs = cachedBaselineStats.observations + 1
                val newAvgBlips = cachedBaselineStats.avgActiveBlips + ((processedBlips.size - cachedBaselineStats.avgActiveBlips) / currentObs)
                val currentFreq = processedBlips.sumOf { (it.bandwidthMhz ?: 20.0) }.toFloat()
                val newAvgFreq = cachedBaselineStats.avgFreqOccupancy + ((currentFreq - cachedBaselineStats.avgFreqOccupancy) / currentObs)
                viewModelScope.launch {
                    settingsDataStore.updateBaselineStats(currentObs, newAvgBlips, newAvgFreq)
                }
            }

            _uiState.update { state ->
                state.copy(
                    activeBlips = processedBlips,
                    nearestBlip = nearest,
                    perimeterBreachCount = breaches,
                    baselineSummary = summary
                )
            }"""

if "val (processedBlips, summary) = baselineEngine.processBaseline" not in content:
    content = content.replace(target, replacement)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
