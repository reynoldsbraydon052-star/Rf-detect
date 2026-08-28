import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

# Add the missing methods
methods = """
    fun clearReplayState() {
        // Clear active replay data
        _activeBlips.value = emptyList()
        _selectedTargetDeviceId.value = null
        signalHistoryLogger.clearHistory()
        // Reset environment map if applicable
    }

    fun reconstructStateFromEvents(validEvents: List<RfRecordedEventEntity>) {
        if (_uiState.value.operatingMode != OperatingMode.REPLAY) return
        
        // Find the latest state for each device
        val latestDeviceEvents = validEvents.groupBy { it.deviceId }
            .mapValues { it.value.maxByOrNull { e -> e.timestampMs } }
            .values.filterNotNull()
            
        // Filter out devices that haven't been seen recently (e.g. within 30 seconds of the seek target)
        // For replay scrubbing, we just take the last known state, but realistically we should age them out
        val seekTargetMs = validEvents.lastOrNull()?.timestampMs ?: 0L
        val activeDeviceEvents = latestDeviceEvents.filter { seekTargetMs - it.timestampMs < 30000 }
        
        val reconstructedBlips = activeDeviceEvents.map { entity ->
            val anomalyResult = if (entity.anomalyScore != null || entity.classification != null) {
                AnomalyResult(
                    score = entity.anomalyScore?.toDouble() ?: 0.0,
                    isAnomaly = entity.anomalyScore != null && entity.anomalyScore > 0.5f,
                    confidence = entity.classificationConfidence ?: 0f,
                    explanations = if (entity.classification != null) listOf(AnomalyExplanation(entity.classification, 1.0, "Classification")) else emptyList()
                )
            } else null
            
            RadarBlip(
                id = entity.deviceId,
                name = entity.manufacturerInfo ?: "Unknown Replay Device",
                distance = entity.distanceMeters ?: 0f,
                targetAngleOffset = 0f,
                type = entity.signalType,
                rssi = entity.rssi,
                frequencyMhz = entity.frequencyMhz,
                bandLabel = entity.bandLabel,
                anomalyResult = anomalyResult,
                provenance = DataProvenance.REPLAY,
                timestampMs = entity.timestampMs
            )
        }
        
        _activeBlips.value = reconstructedBlips
        signalHistoryLogger.clearHistory() // or rebuild history if we want
        reconstructedBlips.forEach { signalHistoryLogger.logSignal(it.id, it.rssi, it.frequencyMhz) }
    }
"""

# inject right before `fun injectReplayBlip`
content = content.replace("fun injectReplayBlip(blip: RadarBlip)", methods + "\n    fun injectReplayBlip(blip: RadarBlip)")

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)

