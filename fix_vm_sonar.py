import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

# Add sonarEngine instance
if "val sonarEngine =" not in content:
    content = content.replace(
        "val rfSessionEngine = RfInvestigationSessionEngine(db.rfSessionDao(), db.rfAnnotationDao())",
        "val rfSessionEngine = RfInvestigationSessionEngine(db.rfSessionDao(), db.rfAnnotationDao())\n    val sonarEngine = SonarEngine()"
    )

# When selectedTargetDeviceId changes, or activeBlips updates, we need to update sonar.
# Let's add a state flow in ViewModel for sonar state if we want UI to reflect it. Or we can just let UI read it.
# We also need a function to set selectedTargetDeviceId and update sonar.
select_func = """
    fun setSelectedTargetDeviceId(id: String?) {
        _uiState.update { it.copy(selectedTargetDeviceId = id) }
        updateSonarTarget()
    }
    
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

if "fun setSelectedTargetDeviceId" not in content:
    content = content.replace(
        "fun setTab(tab: RadarTab) {",
        select_func + "\n    fun setTab(tab: RadarTab) {"
    )

# And in processSignalIntercept, or where activeBlips is updated:
if "updateSonarTarget()" not in content:
    # We will inject this at the end of processRadarPulse
    pass

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)

