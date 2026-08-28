import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

target = "fun setMapRangeMeters(meters: Float) {"
replacement = """    fun toggleBaselineLearning() {
        val currentState = _uiState.value.baselineSummary.isLearning
        viewModelScope.launch {
            settingsDataStore.updateBaselineLearningMode(!currentState)
        }
    }

    fun resetBaseline() {
        viewModelScope.launch {
            settingsDataStore.resetBaseline()
        }
    }

    fun setMapRangeMeters(meters: Float) {"""

if "fun toggleBaselineLearning()" not in content:
    content = content.replace(target, replacement)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
