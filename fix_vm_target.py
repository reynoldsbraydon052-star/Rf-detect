import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

# Replace the targetBlip selection logic
old_logic = """            val targetDeviceId = _uiState.value.selectedTargetDeviceId
            val targetBlip = if (targetDeviceId != null) {
                blipsList.find { it.id == targetDeviceId || it.name == targetDeviceId } ?: nearest
            } else {
                nearest
            }

            targetBlip?.let {
                if (_uiState.value.isAudioSonarActive) {
                    audioTracker.updateProximityDistance(it.distance.toDouble())
                }
            }"""

new_logic = """            val targetDeviceId = _uiState.value.selectedTargetDeviceId
            val targetBlip = if (targetDeviceId != null) {
                blipsList.find { it.id == targetDeviceId || it.name == targetDeviceId }
            } else {
                // Feature 26: If no target selected, do not track unless explicitly desired. But the instructions say:
                // "If no device is selected: sonar remains disabled or inactive."
                null
            }

            if (_uiState.value.isAudioSonarActive) {
                if (targetBlip != null) {
                    audioTracker.updateProximityDistance(targetBlip.distance.toDouble())
                } else {
                    // Send -1 to indicate unavailable or idle
                    audioTracker.updateProximityDistance(-1.0)
                }
            }"""

content = content.replace(old_logic, new_logic)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
