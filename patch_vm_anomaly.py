import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

target1 = "private val baselineEngine = EnvironmentalBaselineEngine()"
if "private val anomalyEngine = ExplainableAnomalyEngine()" not in content:
    content = content.replace(target1, target1 + "\n    private val anomalyEngine = ExplainableAnomalyEngine()")

target2 = """            if (_uiState.value.baselineSummary.isLearning) {"""
replacement2 = """
            val evaluatedBlips = processedBlips.map { blip ->
                val anomaly = anomalyEngine.evaluateAnomaly(
                    blip = blip,
                    fingerprintDb = cachedFingerprints,
                    baselineSummary = summary
                )
                blip.copy(anomalyResult = anomaly)
            }

            if (_uiState.value.baselineSummary.isLearning) {"""

if "val evaluatedBlips =" not in content:
    content = content.replace(target2, replacement2)
    content = content.replace("activeBlips = processedBlips,", "activeBlips = evaluatedBlips,")
    content = content.replace("val currentFreq = processedBlips.sumOf", "val currentFreq = evaluatedBlips.sumOf")
    content = content.replace("val newAvgBlips = cachedBaselineStats.avgActiveBlips + ((processedBlips.size - cachedBaselineStats.avgActiveBlips) / currentObs)", "val newAvgBlips = cachedBaselineStats.avgActiveBlips + ((evaluatedBlips.size - cachedBaselineStats.avgActiveBlips) / currentObs)")


with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
