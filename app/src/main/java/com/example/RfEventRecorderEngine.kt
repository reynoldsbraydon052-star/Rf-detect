package com.example

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory


data class RecorderState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val recordingStartTimeMs: Long? = null,
    val lastRecordedTimestampMs: Long? = null,
    val totalRecordedEventsSession: Int = 0
)

class RfEventRecorderEngine(
    private val repository: RfRecordingRepository,
    private val coroutineScope: CoroutineScope,
    private val sessionEngine: RfInvestigationSessionEngine
) {
    private val _recorderState = MutableStateFlow(RecorderState())
    val recorderState: StateFlow<RecorderState> = _recorderState.asStateFlow()
    
    val totalDbEventCount = repository.eventCount

    fun getRecentEvents() = repository.allEvents

    fun startRecording() {
        _recorderState.update { 
            it.copy(
                isRecording = true, 
                isPaused = false,
                recordingStartTimeMs = System.currentTimeMillis()
            ) 
        }
    }

    fun pauseRecording() {
        _recorderState.update { it.copy(isPaused = true) }
    }

    fun resumeRecording() {
        _recorderState.update { it.copy(isPaused = false) }
    }

    fun stopRecording() {
        _recorderState.update { 
            it.copy(
                isRecording = false, 
                isPaused = false,
                recordingStartTimeMs = null,
                totalRecordedEventsSession = 0
            ) 
        }
    }

    fun clearRecording() {
        coroutineScope.launch(Dispatchers.IO) {
            repository.clearAllEvents()
        }
        _recorderState.update { it.copy(totalRecordedEventsSession = 0, lastRecordedTimestampMs = null) }
    }


    suspend fun generateCsvExport(): String {
        val events = repository.allEvents.firstOrNull() ?: emptyList()
        val builder = StringBuilder()
        builder.append("timestamp,event_id,source,signal_type,device_id,frequency_mhz,channel,band,rssi,distance,classification,confidence,fingerprint_id,anomaly_score,evidence_score\n")
        events.forEach { e ->
            builder.append("${e.timestampMs},\"${e.eventId}\",\"${e.sensorSource}\",\"${e.signalType}\",\"${e.deviceId}\",${e.frequencyMhz},${e.channel ?: ""},\"${e.bandLabel}\",${e.rssi},${e.distanceMeters ?: ""},\"${e.classification ?: ""}\",${e.classificationConfidence ?: ""},\"${e.fingerprintId ?: ""}\",${e.anomalyScore ?: ""},${e.evidenceScore ?: ""}\n")
        }
        return builder.toString()
    }

    suspend fun generateJsonExport(uiState: SignalRadarUiState, hypotheses: List<DeviceIdentityHypothesis>, anomalies: List<RfAnomalyEntity>? = null, patterns: List<RfPatternEntity>? = null, sessions: List<RfSessionEntity>? = null, annotations: List<RfAnnotationEntity>? = null): String {
        val events = repository.allEvents.firstOrNull() ?: emptyList()
        val now = System.currentTimeMillis()
        val state = _recorderState.value
        
        val exportObj = InvestigationExport(
            metadata = ExportMetadata(
                applicationVersion = "SignalRadar v1.2",
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
            hardwareCapabilities = listOf("BLE Scanner", "WiFi RTT", "UWB Tracker", "Acoustic Sensors", "SDR", "Cellular API"),
            sensorSourceInformation = listOf("Internal Bluetooth", "WiFi Chipset", "Mic Array"),
            provenanceInformation = ProvenanceInformation(
                generator = "SignalRadar Core",
                securityHash = null,
                originDevice = android.os.Build.MODEL,
                certificationStatus = "UNVERIFIED"
            ),
            events = events,
            identityHypotheses = hypotheses,
            anomalies = anomalies,
            patterns = patterns,
            sessions = sessions,
            annotations = annotations
        )
        
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(InvestigationExport::class.java)
        return adapter.toJson(exportObj)
    }

    private val lastWriteTimes = java.util.concurrent.ConcurrentHashMap<String, Long>()

    fun processObservations(blips: List<RadarBlip>, selectedTargetId: String?) {
        val state = _recorderState.value
        if (!state.isRecording || state.isPaused) return

        val now = System.currentTimeMillis()
        val sessionId = sessionEngine.getActiveSessionId() ?: ""
        
        // Filter and map only those blips that haven't been written to the database in the last 1000ms.
        val entities = blips.filter { blip ->
            val lastWrite = lastWriteTimes[blip.id] ?: 0L
            if (now - lastWrite >= 1000L) {
                lastWriteTimes[blip.id] = now
                true
            } else {
                false
            }
        }.map { blip ->
            val channel: Int? = null

            RfRecordedEventEntity(
                sessionId = sessionId,
                timestampMs = now,
                sensorSource = if (blip.provenance == DataProvenance.SIMULATED) "SimulationLab" else "HardwareScanner",
                signalType = blip.type,
                deviceId = blip.id,
                frequencyMhz = blip.frequencyMhz,
                channel = channel,
                rssi = blip.rssi,
                distanceMeters = blip.distance,
                bandLabel = blip.bandLabel,
                classification = blip.anomalyResult?.explanations?.firstOrNull()?.description,
                fingerprintId = blip.fingerprintId,
                classificationConfidence = blip.anomalyResult?.confidence,
                anomalyScore = blip.anomalyResult?.score?.toFloat(),
                evidenceScore = null,
                isSelectedTarget = blip.id == selectedTargetId,
                manufacturerInfo = blip.ouiVendor,
                rawMetadata = null,
                provenance = blip.provenance.name
            )
        }

        if (entities.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                repository.insertEvents(entities)
            }
        }

        // Maintain accurate 'Last Seen' timestamp in state even if some specific database insertions were throttled
        _recorderState.update { 
            it.copy(
                lastRecordedTimestampMs = now,
                totalRecordedEventsSession = it.totalRecordedEventsSession + entities.size
            ) 
        }
    }
}
