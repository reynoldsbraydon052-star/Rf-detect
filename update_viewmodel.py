with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

# Add OperatingMode to UiState
if "val isSimulation: Boolean = false," in content:
    content = content.replace("val isSimulation: Boolean = false,", "val isSimulation: Boolean = false,\n    val operatingMode: OperatingMode = OperatingMode.LIVE,")
else:
    # Look for uiState declaration
    content = content.replace("val sensorSuite: HardwareSensorSuiteData = HardwareSensorSuiteData(),", "val sensorSuite: HardwareSensorSuiteData = HardwareSensorSuiteData(),\n    val operatingMode: OperatingMode = OperatingMode.LIVE,")

# Add inject methods
methods = """

    fun injectSimulationBlip(blip: RadarBlip) {
        if (_uiState.value.operatingMode != OperatingMode.SIMULATION) return
        processSignalIntercept(blip)
    }

    fun injectReplayBlip(blip: RadarBlip) {
        if (_uiState.value.operatingMode != OperatingMode.REPLAY) return
        processSignalIntercept(blip)
    }

    fun setOperatingMode(mode: OperatingMode) {
        _uiState.update { it.copy(operatingMode = mode) }
        
        // Reset or swap isolated context if needed
        if (mode == OperatingMode.LIVE) {
            // Restore live context
            anomalyEngine.resetIsolatedState()
            correlationEngine.resetIsolatedState()
        } else {
            // Enter isolated context
            anomalyEngine.isolateStateForSimulation()
            correlationEngine.isolateStateForSimulation()
        }
    }
"""

content = content.replace("    fun toggleBleScannerService() {", methods + "    fun toggleBleScannerService() {")

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)

