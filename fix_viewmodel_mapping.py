import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

# Add to enum
content = content.replace("    EVENT_RECORDER", "    EVENT_RECORDER,\n    ENVIRONMENT_MAP")

# Add property to ViewModel
prop_decl = """    val rfEventRecorderEngine = RfEventRecorderEngine(getApplication())
    val environmentMappingEngine = RfEnvironmentMappingEngine()"""
content = content.replace("    val rfEventRecorderEngine = RfEventRecorderEngine(getApplication())", prop_decl)

# Update map along with activeBlips update (approx every 65ms)
update_call = """            if (_uiState.value.baselineSummary.isLearning && _uiState.value.operatingMode == OperatingMode.LIVE) {
                // Update running averages
                val currentObs = cachedBaselineStats.observations + 1
                val newAvgBlips = cachedBaselineStats.avgActiveBlips + ((evaluatedBlips.size - cachedBaselineStats.avgActiveBlips) / currentObs)
                val currentFreq = evaluatedBlips.sumOf { (it.bandwidthMhz ?: 20.0) }.toFloat()
                val newAvgFreq = cachedBaselineStats.avgFreqOccupancy + ((currentFreq - cachedBaselineStats.avgFreqOccupancy) / currentObs)
                viewModelScope.launch {
                    settingsDataStore.updateBaselineStats(currentObs, newAvgBlips, newAvgFreq)
                }
            }
            
            // Dispatch to mapping engine (background)
            viewModelScope.launch(Dispatchers.Default) {
                environmentMappingEngine.updateMap(
                    blips = evaluatedBlips,
                    headingDegrees = _uiState.value.headingDegrees,
                    userX = 0f, 
                    userY = 0f
                )
            }

            _uiState.update { state ->"""

content = content.replace("""            if (_uiState.value.baselineSummary.isLearning && _uiState.value.operatingMode == OperatingMode.LIVE) {
                // Update running averages
                val currentObs = cachedBaselineStats.observations + 1
                val newAvgBlips = cachedBaselineStats.avgActiveBlips + ((evaluatedBlips.size - cachedBaselineStats.avgActiveBlips) / currentObs)
                val currentFreq = evaluatedBlips.sumOf { (it.bandwidthMhz ?: 20.0) }.toFloat()
                val newAvgFreq = cachedBaselineStats.avgFreqOccupancy + ((currentFreq - cachedBaselineStats.avgFreqOccupancy) / currentObs)
                viewModelScope.launch {
                    settingsDataStore.updateBaselineStats(currentObs, newAvgBlips, newAvgFreq)
                }
            }

            _uiState.update { state ->""", update_call)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
