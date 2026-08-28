with open('app/src/main/java/com/example/MultiSensorCorrelationEngine.kt', 'r') as f:
    content = f.read()

content = content.replace("class MultiSensorCorrelationEngine {",
"""class MultiSensorCorrelationEngine {
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
    }
""")

content = content.replace(
"""class ExplainableAnomalyEngine {""",
"""class ExplainableAnomalyEngine {
    private var isIsolated = false

    fun isolateStateForSimulation() {
        isIsolated = true
    }

    fun resetIsolatedState() {
        isIsolated = false
    }
"""
)

with open('app/src/main/java/com/example/MultiSensorCorrelationEngine.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ExplainableAnomalyEngine.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""class ExplainableAnomalyEngine {""",
"""class ExplainableAnomalyEngine {
    private var isIsolated = false

    fun isolateStateForSimulation() {
        isIsolated = true
    }

    fun resetIsolatedState() {
        isIsolated = false
    }
"""
)

with open('app/src/main/java/com/example/ExplainableAnomalyEngine.kt', 'w') as f:
    f.write(content)
