import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

replacement = """        historyLogger.logSignalEntry(
            deviceName = smoothedBlip.name,
            distanceMeters = smoothedDistance,
            type = smoothedBlip.type,
            freqMhz = smoothedBlip.frequencyMhz,
            isBreach = isBreach
        )
        
        rfEventRecorderEngine.processObservations(listOf(smoothedBlip), _uiState.value.selectedTargetDeviceId)
"""

content = content.replace("""        historyLogger.logSignalEntry(
            deviceName = smoothedBlip.name,
            distanceMeters = smoothedDistance,
            type = smoothedBlip.type,
            freqMhz = smoothedBlip.frequencyMhz,
            isBreach = isBreach
        )""", replacement)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
