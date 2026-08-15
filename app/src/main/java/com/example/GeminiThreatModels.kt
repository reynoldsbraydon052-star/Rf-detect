package com.example

import androidx.compose.ui.graphics.Color

enum class ThreatLevel(val label: String, val color: Color, val priority: Int) {
    SECURE("SECURE / NORMAL", Color(0xFF00FF66), 0),
    LOW_CAUTION("LOW CAUTION", Color(0xFF33CCFF), 1),
    ELEVATED("ELEVATED THREAT", Color(0xFFFFCC00), 2),
    HIGH("HIGH ALERT", Color(0xFFFF6600), 3),
    CRITICAL("CRITICAL HOSTILE", Color(0xFFFF2244), 4)
}

enum class ThreatCategory(val label: String, val iconName: String) {
    SURVEILLANCE_TRACKER("Physical Tracker / Beacon", "LocationSearching"),
    ROGUE_WIFI_EVIL_TWIN("Rogue Wi-Fi / AP Spoof", "Wifi"),
    IMSI_CELL_CATCHER("Cellular Interceptor", "CellTower"),
    ULTRASONIC_ACOUSTIC_SPY("Ultrasonic Beacon Spike", "GraphicEq"),
    EMF_MAGNETIC_ANOMALY("EMF / Magnetic Anomaly", "Equalizer"),
    RF_JAMMING_ELECTRONIC_WAR("RF Jamming / Denial", "Warning"),
    UNREGISTERED_BLE_BEACON("Unregistered BLE Beacon", "BluetoothSearching"),
    UNKNOWN_ANOMALOUS_NODE("Anomalous Emitter", "Sensors")
}

data class ProtocolVulnerability(
    val protocol: String, // "BLE 5.2", "Wi-Fi 6 (WPA2/3)", "LTE/5G RRC", "Ultrasonic PPM", "EMF Induction"
    val riskLevel: ThreatLevel,
    val attackSurface: String,
    val exploitationVector: String,
    val containmentFix: String
)

data class DetailedTargetAudit(
    val targetId: String,
    val targetName: String,
    val macAddress: String,
    val signalType: String,
    val rssiDbm: Int,
    val estimatedDistanceMeters: Float,
    val threatScore: Int,
    val threatCategory: ThreatCategory,
    val manufacturerVendor: String,
    val radioFingerprintSummary: String,
    val trackingHeuristicConfidence: Int, // 0 to 100%
    val surveillanceRiskAnalysis: String,
    val hardwareVectorAnalysis: String,
    val cryptographicProfile: String,
    val vulnerabilities: List<ProtocolVulnerability> = emptyList(),
    val stepByStepNeutralizationPlan: List<String> = emptyList(),
    val isAuditLoading: Boolean = false
)

data class FlaggedThreatEmitter(
    val id: String,
    val name: String,
    val macAddress: String?,
    val signalType: String,
    val rssiDbm: Int,
    val distanceMeters: Float,
    val threatCategory: ThreatCategory,
    val threatScore: Int, // 0 to 100
    val riskSummary: String,
    val recommendedAction: String,
    val deepAuditResult: DetailedTargetAudit? = null
)

data class TacticalCountermeasure(
    val title: String,
    val detail: String,
    val urgency: String, // "IMMEDIATE", "RECOMMENDED", "MONITOR"
    val isCompleted: Boolean = false
)

data class ThreatAnalysisReport(
    val reportId: String = "SIGINT-${System.currentTimeMillis()}",
    val timestampMs: Long = System.currentTimeMillis(),
    val threatLevel: ThreatLevel = ThreatLevel.SECURE,
    val threatScore: Int = 12, // 0 to 100
    val executiveSummary: String = "Ambient RF environment appears standard with normal consumer BLE and Wi-Fi baseline signals.",
    val flaggedEmitters: List<FlaggedThreatEmitter> = emptyList(),
    val identifiedVectors: List<String> = emptyList(),
    val countermeasures: List<TacticalCountermeasure> = emptyList(),
    val rawSigintDetails: String = "",
    val isAiGenerated: Boolean = true
)

data class TacticalCopilotMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val threatLevelTag: ThreatLevel? = null
)

data class RfEnvironmentSnapshot(
    val totalBlipsCount: Int,
    val activeBlips: List<RadarBlip>,
    val nearestBlip: RadarBlip?,
    val isRfJammingDetected: Boolean,
    val isGnssSpoofingDetected: Boolean,
    val isImsiAlertActive: Boolean,
    val isUltrasonicAlertActive: Boolean,
    val ultrasonicFreqHz: Int,
    val ultrasonicDb: Float,
    val magneticFluxMicroTesla: Float,
    val compassHeading: Float,
    val breachCount: Int
)

enum class RadarBoostLevel(
    val label: String,
    val multiplier: Float,
    val gainDb: Int,
    val noiseCutoffDbm: Int,
    val badgeColor: Color,
    val description: String
) {
    NORMAL_1X("1x STD", 1.0f, 0, -90, Color(0xFF00FF66), "Standard RF sensitivity and receiver gain"),
    HIGH_GAIN_2X("2x +6dB", 2.0f, 6, -96, Color(0xFF33CCFF), "Amplified RF front-end with low-noise filtering"),
    TURBO_4X("4x +12dB", 4.0f, 12, -102, Color(0xFFFFCC00), "Turbo RF beam-forming & deep packet integration"),
    SNIPER_8X("8x +18dB", 8.0f, 18, -108, Color(0xFFFF3366), "Extreme sniper gain with predictive AoA tracing")
}

data class AiPinpointResult(
    val targetId: String,
    val targetName: String,
    val macAddress: String,
    val signalType: String,
    val currentRssiDbm: Int,
    val distanceMeters: Float,
    val accuracyMarginMeters: Float,
    val confidencePercent: Int,
    val azimuthDegrees: Float,
    val relativeClockHeading: String,
    val elevationPitchDeg: Float, // +25° = Looking up, -35° = Looking down, 0° = Eye level
    val altitudeOffsetMeters: Float, // +1.5m = Above user, -1.0m = Below user
    val floorClassification: String, // "SAME LEVEL (Desk/Waist)", "UPPER ELEVATION (+1 Floor / Ceiling)", "LOWER ELEVATION (-1 Floor / Floor Cavity)"
    val physicalZoneEstimation: String, // "Ceiling drop tile / upper lighting fixture", "Under desk / luggage compartment", etc.
    val spatialVectorXyz: String, // "X: +1.2m, Y: +2.1m, Z: +1.5m"
    val isAimSightAligned: Boolean = false,
    val aiTacticalGuidance: String,
    val searchChecklist: List<String> = emptyList(),
    val isPinpointingLoading: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)
