import re

with open('app/src/main/java/com/example/RfEventRecorderEngine.kt', 'r') as f:
    content = f.read()

json_export = """
    suspend fun generateJsonExport(uiState: SignalRadarUiState): String {
        val events = repository.allEvents.firstOrNull() ?: emptyList()
        val now = System.currentTimeMillis()
        val state = _recorderState.value
        
        val exportObj = InvestigationExport(
            metadata = ExportMetadata(
                applicationVersion = "1.0",
                recordingStartTimeMs = state.recordingStartTimeMs ?: now,
                recordingEndTimeMs = now,
                exportTimeMs = now
            ),
            configuration = ExportConfiguration(
                perimeterThresholdMeters = uiState.perimeterThresholdMeters,
                rssiAlertThresholdDbm = uiState.rssiAlertThresholdDbm,
                isRssiAlertEnabled = uiState.isRssiAlertEnabled,
                isPerimeterAlarmEnabled = uiState.isPerimeterAlarmEnabled,
                stealthModeEnabled = uiState.stealthModeEnabled,
                activeScanMode = uiState.scanMode.name
            ),
            hardwareCapabilities = listOf("BLE Scanner", "Audio Mic"),
            events = events
        )
        
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(InvestigationExport::class.java)
        return adapter.toJson(exportObj)
    }
"""

content = content.replace("    fun processObservations(", json_export + "\n    fun processObservations(")

with open('app/src/main/java/com/example/RfEventRecorderEngine.kt', 'w') as f:
    f.write(content)
