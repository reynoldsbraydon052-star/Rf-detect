import re

with open('app/src/main/java/com/example/MultiSensorCorrelationEngine.kt', 'r') as f:
    content = f.read()

content = content.replace("class MultiSensorCorrelationEngine(\n    private val config: CorrelationEngineConfig = CorrelationEngineConfig()\n) {",
"""class MultiSensorCorrelationEngine(
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
    }""")

with open('app/src/main/java/com/example/MultiSensorCorrelationEngine.kt', 'w') as f:
    f.write(content)
