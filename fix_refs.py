import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace('Icons.Default.Science', 'Icons.Default.Build')
content = content.replace('collectAsState()', 'collectAsStateWithLifecycle()')

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/MultiSensorCorrelationEngine.kt', 'r') as f:
    content = f.read()

content = re.sub(
    r'class MultiSensorCorrelationEngine\([^\)]*\)\s*\{',
    '''class MultiSensorCorrelationEngine(
    private val config: CorrelationEngineConfig = CorrelationEngineConfig()
) {
    private var isIsolated = false

    fun isolateStateForSimulation() {
        isIsolated = true
        observationBuffer.clear()
        activeCorrelations.clear()
    }

    fun resetIsolatedState() {
        isIsolated = false
        observationBuffer.clear()
        activeCorrelations.clear()
    }''', content, count=1)

with open('app/src/main/java/com/example/MultiSensorCorrelationEngine.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ExplainableAnomalyEngine.kt', 'r') as f:
    content = f.read()

content = content.replace("class ExplainableAnomalyEngine {",
"""class ExplainableAnomalyEngine {
    private var isIsolated = false

    fun isolateStateForSimulation() {
        isIsolated = true
    }

    fun resetIsolatedState() {
        isIsolated = false
    }
""")

with open('app/src/main/java/com/example/ExplainableAnomalyEngine.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("// anomalyEngine.resetIsolatedState()", "anomalyEngine.resetIsolatedState()")
content = content.replace("// anomalyEngine.isolateStateForSimulation()", "anomalyEngine.isolateStateForSimulation()")

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
