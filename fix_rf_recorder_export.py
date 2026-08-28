import re

with open('app/src/main/java/com/example/RfEventRecorderEngine.kt', 'r') as f:
    content = f.read()

imports = """import kotlinx.coroutines.flow.firstOrNull
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
"""

content = content.replace("import kotlinx.coroutines.launch", "import kotlinx.coroutines.launch\n" + imports)

export_methods = """
    suspend fun generateCsvExport(): String {
        val events = repository.allEvents.firstOrNull() ?: emptyList()
        val builder = StringBuilder()
        builder.append("timestamp,event_id,source,signal_type,device_id,frequency_mhz,channel,band,rssi,distance,classification,confidence,fingerprint_id,anomaly_score,evidence_score\n")
        events.forEach { e ->
            builder.append("${e.timestampMs},\"${e.eventId}\",\"${e.sensorSource}\",\"${e.signalType}\",\"${e.deviceId}\",${e.frequencyMhz},${e.channel ?: ""},\"${e.bandLabel}\",${e.rssi},${e.distanceMeters ?: ""},\"${e.classification ?: ""}\",${e.classificationConfidence ?: ""},\"${e.fingerprintId ?: ""}\",${e.anomalyScore ?: ""},${e.evidenceScore ?: ""}\n")
        }
        return builder.toString()
    }

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
                activeScanMode = uiState.activeScanMode.name
            ),
            hardwareCapabilities = listOf("BLE Scanner", "Audio Mic"),
            events = events
        )
        
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(InvestigationExport::class.java)
        return adapter.toJson(exportObj)
    }
"""

content = content.replace("    fun processObservations", export_methods + "\n    fun processObservations")

with open('app/src/main/java/com/example/RfEventRecorderEngine.kt', 'w') as f:
    f.write(content)
