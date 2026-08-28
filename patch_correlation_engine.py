with open('app/src/main/java/com/example/MultiSensorCorrelationEngine.kt', 'r') as f:
    content = f.read()

target = """                val event = buildCorrelationEvent(historicalObs, newObservation, timeSeparationMs)
                if (event.confidence >= config.minConfidenceThreshold) {
                    correlations.add(event)
                }"""

replacement = """                val pairId = setOf(historicalObs.id, newObservation.id)
                val lastTime = lastCorrelatedPairs[pairId] ?: 0L
                if (newObservation.timestampMs - lastTime > config.maxTemporalWindowMs * 2) {
                    val event = buildCorrelationEvent(historicalObs, newObservation, timeSeparationMs)
                    if (event.confidence >= config.minConfidenceThreshold) {
                        correlations.add(event)
                        lastCorrelatedPairs[pairId] = newObservation.timestampMs
                    }
                }"""

if "lastCorrelatedPairs" not in content:
    content = content.replace("private val historicalPatterns = mutableMapOf<Set<String>, Int>()", "private val historicalPatterns = mutableMapOf<Set<String>, Int>()\n    private val lastCorrelatedPairs = mutableMapOf<Set<String>, Long>()")
    content = content.replace(target, replacement)

with open('app/src/main/java/com/example/MultiSensorCorrelationEngine.kt', 'w') as f:
    f.write(content)
