with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("anomalyScore = currentState.globalAnomalyScore,", "anomalyScore = currentState.activeBlips.maxOfOrNull { it.anomalyResult?.score ?: 0 } ?: 0,")
content = content.replace("anomalyConfidence = currentState.globalAnomalyConfidence,", "anomalyConfidence = currentState.activeBlips.mapNotNull { it.anomalyResult?.confidence }.average().toFloat().takeIf { !it.isNaN() } ?: 0f,")
content = content.replace("anomalyExplanations = currentState.anomalyExplanations.toList(),", "anomalyExplanations = currentState.activeBlips.flatMap { it.anomalyResult?.explanations?.map { e -> e.reason } ?: emptyList() },")

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
