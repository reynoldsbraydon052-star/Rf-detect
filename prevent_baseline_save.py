with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""            if (_uiState.value.baselineSummary.isLearning) {
                // Update running averages
                val currentObs = cachedBaselineStats.observations + 1""",
"""            if (_uiState.value.baselineSummary.isLearning && _uiState.value.operatingMode == OperatingMode.LIVE) {
                // Update running averages
                val currentObs = cachedBaselineStats.observations + 1"""
)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
