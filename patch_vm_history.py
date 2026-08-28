with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

target = """            val evaluatedBlips = processedBlips.map { blip ->
                val anomaly = anomalyEngine.evaluateAnomaly(
                    blip = blip,
                    fingerprintDb = cachedFingerprints,
                    baselineSummary = summary
                )
                blip.copy(anomalyResult = anomaly)
            }"""

replacement = """            val evaluatedBlips = processedBlips.map { blip ->
                val anomaly = anomalyEngine.evaluateAnomaly(
                    blip = blip,
                    fingerprintDb = cachedFingerprints,
                    baselineSummary = summary
                )
                
                // Historical tracking: Update the database occasionally if score changes significantly
                blip.fingerprintId?.let { fpId ->
                    val fp = cachedFingerprints[fpId]
                    if (fp != null) {
                        val prevScore = fp.lastAnomalyScore ?: 0
                        if (kotlin.math.abs(prevScore - anomaly.score) > 10) {
                            val updatedFp = fp.copy(lastAnomalyScore = anomaly.score, lastAnomalyConfidence = anomaly.confidence)
                            cachedFingerprints[fpId] = updatedFp // Update local cache
                            viewModelScope.launch(Dispatchers.IO) {
                                bleDatabase.signalFingerprintDao().updateFingerprint(updatedFp.toEntity())
                            }
                        }
                    }
                }
                
                blip.copy(anomalyResult = anomaly)
            }"""

if "Historical tracking" not in content:
    content = content.replace(target, replacement)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
