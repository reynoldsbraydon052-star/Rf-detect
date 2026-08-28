import re
with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("anomalyScore = currentState.activeBlips.maxOfOrNull { it.anomalyResult?.score ?: 0 } ?: 0,", "anomalyScore = (currentState.activeBlips.maxOfOrNull { it.anomalyResult?.score ?: 0 } ?: 0).toFloat(),")
content = content.replace("e -> e.reason", "e -> e.description")

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
