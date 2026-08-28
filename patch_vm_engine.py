with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

target = "private val anomalyEngine = ExplainableAnomalyEngine()"
replacement = "private val anomalyEngine = ExplainableAnomalyEngine()\n    private val correlationEngine = MultiSensorCorrelationEngine()"

if "private val correlationEngine" not in content:
    content = content.replace(target, replacement)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
