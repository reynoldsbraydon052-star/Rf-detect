import re

with open('app/src/main/java/com/example/MultiSensorCorrelationEngine.kt', 'r') as f:
    content = f.read()

target = """                val pairId = setOf(historicalObs.id, newObservation.id)
                val lastTime = lastCorrelatedPairs[pairId] ?: 0L
                if (newObservation.timestampMs - lastTime > config.maxTemporalWindowMs * 2) {"""

replacement = """                val pairId = setOf(historicalObs.id, newObservation.id)
                val lastTime = lastCorrelatedPairs[pairId]
                if (lastTime == null || newObservation.timestampMs - lastTime > config.maxTemporalWindowMs * 2) {"""

if "lastTime == null" not in content:
    content = content.replace(target, replacement)

with open('app/src/main/java/com/example/MultiSensorCorrelationEngine.kt', 'w') as f:
    f.write(content)
