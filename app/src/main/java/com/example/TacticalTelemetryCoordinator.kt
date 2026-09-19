package com.example

import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

data class TargetTelemetry(
    val macAddress: String,
    val primaryAssignedId: String,
    val manufacturer: String,
    val smoothedRssi: Double,
    val estimatedDistanceMeters: Double,
    val timestamp: Long
)

/**
 * Centralized structural coordinator acting as a facade to aggregate, persist, and process
 * core SIGINT sensors, multi-stage ranging Kalman filters, and spatial tracking algorithms.
 */
class TacticalTelemetryCoordinator(
    private val fingerprintRegistry: DeviceFingerprintRegistry = DeviceFingerprintRegistry(),
    private val rangingRegistry: DeviceRangingRegistry = DeviceRangingRegistry(),
    val bearingEstimator: DifferentialBearingEstimator = DifferentialBearingEstimator(),
    val anomalyDetector: RogueFrameAnomalyDetector = RogueFrameAnomalyDetector(),
    val deadReckoningTracker: DeadReckoningTracker = DeadReckoningTracker()
) {
    private val _activeThreats = MutableStateFlow<List<DetectedThreatEvent>>(emptyList())
    val activeThreats: StateFlow<List<DetectedThreatEvent>> = _activeThreats.asStateFlow()

    private val _trackedEmitters = MutableStateFlow<List<TargetTelemetry>>(emptyList())
    val trackedEmitters: StateFlow<List<TargetTelemetry>> = _trackedEmitters.asStateFlow()

    val bearingScanState: StateFlow<ScanState> = bearingEstimator.scanState
    val bearingSweepResult: StateFlow<BearingSweepResult?> = bearingEstimator.sweepResult
    val bearingAccumulatedRotation: StateFlow<Float> = bearingEstimator.accumulatedRotation

    // Thread-safe map of active tracked emitter profiles
    private val emitterMap = ConcurrentHashMap<String, TargetTelemetry>()

    /**
     * Ingests BLE scanning parameters to extract underlying hardware profiles,
     * filter multi-path RSSI spikes, and calculate distance.
     */
    fun ingestBleObservation(
        mac: String,
        rssi: Int,
        txPower: Int?,
        rawManufacturerBytes: ByteArray?,
        serviceUuids: List<ParcelUuid>?
    ): TargetTelemetry {
        val now = System.currentTimeMillis()

        // 1. Resolve or extract hardware fingerprint
        val fpHash = FingerprintExtractor.extractBleFingerprint(rawManufacturerBytes, serviceUuids)
        val signature = fingerprintRegistry.ingestSignal(mac, fpHash, null)

        // 2. Perform Kalman-smoothed log-distance ranging
        val ranging = rangingRegistry.processSignalFrame(mac, rssi, txPower)

        val telemetry = TargetTelemetry(
            macAddress = mac,
            primaryAssignedId = signature.primaryAssignedId,
            manufacturer = signature.manufacturer,
            smoothedRssi = ranging.smoothedRssi,
            estimatedDistanceMeters = ranging.estimatedDistanceMeters,
            timestamp = now
        )

        // Update tracking states
        emitterMap[mac] = telemetry
        _trackedEmitters.update { emitterMap.values.toList() }

        return telemetry
    }

    /**
     * Audits Wi-Fi AP parameters against Rogue Frame/Evil Twin detectors,
     * updating signal range profiles.
     */
    fun ingestWifiObservation(
        bssid: String,
        ssid: String,
        capabilities: String,
        frequencyMhz: Int,
        rssi: Int
    ): Pair<TargetTelemetry, DetectedThreatEvent?> {
        val now = System.currentTimeMillis()

        // 1. Audit AP for Clone/Evil-Twin vulnerabilities
        val threat = anomalyDetector.auditAccessPoint(bssid, ssid, capabilities, frequencyMhz, rssi)
        threat?.let { event ->
            _activeThreats.update { (it + event).takeLast(100) } // Retain last 100 alerts
        }

        // 2. Map and smooth signal ranging telemetry
        val ranging = rangingRegistry.processSignalFrame(bssid, rssi, null)
        val signature = fingerprintRegistry.ingestSignal(bssid, null, null)

        val telemetry = TargetTelemetry(
            macAddress = bssid,
            primaryAssignedId = signature.primaryAssignedId,
            manufacturer = signature.manufacturer,
            smoothedRssi = ranging.smoothedRssi,
            estimatedDistanceMeters = ranging.estimatedDistanceMeters,
            timestamp = now
        )

        emitterMap[bssid] = telemetry
        _trackedEmitters.update { emitterMap.values.toList() }

        return Pair(telemetry, threat)
    }

    /**
     * Logs disassociation / deauth packets on a given BSSID to detect flood denial attacks.
     */
    fun recordWifiDeauth(bssid: String): DetectedThreatEvent? {
        val threat = anomalyDetector.recordDisassociationEvent(bssid)
        threat?.let { event ->
            _activeThreats.update { (it + event).takeLast(100) }
        }
        return threat
    }

    /**
     * Logs physical pedometer steps, updating user coordinates via polar trigonometry.
     */
    fun recordPedometerStep(azimuthDegrees: Float): Point2D {
        return deadReckoningTracker.onStepDetected(azimuthDegrees)
    }

    /**
     * Clears all temporary tracking, thread buffers, and anomaly state timelines.
     */
    fun clearAllData() {
        emitterMap.clear()
        anomalyDetector.clearHistory()
        bearingEstimator.reset()
        deadReckoningTracker.reset()
        _activeThreats.value = emptyList()
        _trackedEmitters.value = emptyList()
    }
}
