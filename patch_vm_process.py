with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

target = "private fun processSignalIntercept(rawBlip: RadarBlip) {"

replacement = """private fun processSignalIntercept(rawBlip: RadarBlip) {
        val anomaly = anomalyEngine.evaluateAnomaly(rawBlip, cachedFingerprints, _uiState.value.baselineSummary)
        val blipWithAnomaly = rawBlip.copy(anomalyResult = anomaly)
        
        val newCorrelations = correlationEngine.processObservation(blipWithAnomaly)
        if (newCorrelations.isNotEmpty()) {
            _uiState.update { state -> 
                val updatedCorrelations = (newCorrelations + state.correlationEvents)
                    .sortedByDescending { it.firstObservationMs }
                    .take(50) // Keep latest 50
                state.copy(correlationEvents = updatedCorrelations)
            }
        }"""

if "correlationEngine.processObservation" not in content:
    content = content.replace(target, replacement)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
