import re
with open('app/src/main/java/com/example/RfTemporalPatternEngine.kt', 'r') as f:
    content = f.read()

replacement = """
    private val mutex = kotlinx.coroutines.sync.Mutex()

    suspend fun processEvents(events: List<RadarBlip>, sessionId: String) {
        val newPatterns = mutableListOf<RfPatternEntity>()
        val now = System.currentTimeMillis()
        
        mutex.withLock {
            for (blip in events) {
                val history = observationHistory.getOrPut(blip.id) { mutableListOf() }
                history.add(blip)
                
                // Keep history bounded
                if (history.size > 100) history.removeAt(0)
                
                // 1. Detect Periodic Transmissions
                if (history.size >= 10) {
                    // Calculate average interval
                    var totalInterval = 0.0
                    for (i in 1 until history.size) {
                        totalInterval += (history[i].timestampMs - history[i-1].timestampMs)
                    }
                    val avgInterval = totalInterval / (history.size - 1)
                    
                    // Check variance
                    var variance = 0.0
                    for (i in 1 until history.size) {
                        val interval = (history[i].timestampMs - history[i-1].timestampMs)
                        variance += Math.pow(interval - avgInterval, 2.0)
                    }
                    val stdDev = Math.sqrt(variance / (history.size - 1))
                    
                    if (stdDev < 500 && avgInterval > 100) { // Highly periodic 
                         // Create or update periodic pattern 
                         if (history.isNotEmpty()) {
                             newPatterns.add(RfPatternEntity(
                                id = UUID.randomUUID().toString(),
                                sessionId = sessionId,
                                deviceHypothesisId = blip.id,
                                type = PatternType.PERIODIC.name,
                                stability = PatternStability.STABLE.name,
                                confidenceScore = 90,
                                firstObservedMs = history.first().timestampMs,
                                lastObservedMs = history.last().timestampMs,
                                observationCount = history.size,
                                frequencyMhzMean = blip.frequencyMhz,
                                supportingEventIdsJson = "[]"
                            ))
                         }
                        // Clear history to avoid rapid duplicates
                        history.clear()
                    }
                }
            }
        }
        
        if (newPatterns.isNotEmpty()) {
            _patterns.update { it + newPatterns }
            newPatterns.forEach { patternDao.insertPattern(it) }
        }
    }
"""

# Need to add import for Mutex
if "import kotlinx.coroutines.sync.Mutex" not in content:
    content = content.replace("import kotlinx.coroutines.flow.update", "import kotlinx.coroutines.flow.update\nimport kotlinx.coroutines.sync.Mutex\nimport kotlinx.coroutines.sync.withLock")

# Replace processEvents function
content = re.sub(r"suspend fun processEvents.*?if \(newPatterns\.isNotEmpty\(\)\).*?\}", replacement.strip(), content, flags=re.DOTALL)

with open('app/src/main/java/com/example/RfTemporalPatternEngine.kt', 'w') as f:
    f.write(content)
