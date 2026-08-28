import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

# Remove sonarEngine val
content = content.replace("val sonarEngine = SonarEngine()", "")

# Remove setSonarEnabled and updateSonarTarget
to_remove = """
    fun setSonarEnabled(enabled: Boolean) {
        sonarEngine.isEnabled = enabled
        _uiState.update { it.copy(isAudioSonarActive = enabled) }
        updateSonarTarget()
    }
    
    private fun updateSonarTarget() {
        val targetId = _uiState.value.selectedTargetDeviceId
        if (targetId == null) {
            sonarEngine.updateTarget(null)
            return
        }
        val targetBlip = _uiState.value.activeBlips.find { it.deviceId == targetId }
        sonarEngine.updateTarget(targetBlip?.distanceMeters)
    }
"""

content = content.replace(to_remove, "")
content = content.replace("updateSonarTarget()", "")

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
