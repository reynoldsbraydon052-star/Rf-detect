package com.example

enum class OperatingMode {
    LIVE,
    REPLAY,
    SIMULATION
}

data class SessionEvent(
    val timestampMs: Long,
    val type: SessionEventType,
    val blip: RadarBlip? = null,
    val fingerprint: SignalFingerprint? = null,
    val anomaly: AnomalyResult? = null,
    val correlation: CorrelationEvent? = null,
    val bookmarkText: String? = null
)

enum class SessionEventType {
    BLIP,
    FINGERPRINT,
    ANOMALY,
    CORRELATION,
    SPECTRUM,
    BOOKMARK,
    START,
    END
}

/**
 * Snapshot of state at a specific measurement event.
 */
data class SessionMeasurementSnapshot(
    val timestamp: Long,
    val targetId: String,
    val xOffset: Float,
    val yOffset: Float,
    val heading: Float,
    val rssi: Int,
    val filteredRssi: Float,
    val qualityScore: Int,
    val localizationX: Double?,
    val localizationY: Double?,
    val uncertainty: Float?,
    val modelConsistency: String,
    val spatialCoveragePercent: Int
)

/**
 * Structured record containing summary metrics and historical snapshots.
 */
data class SavedSession(
    val id: String, // e.g., "SESSION 0042"
    val targetId: String,
    val timestamp: Long,
    val durationSeconds: Long,
    val startingRssi: Int,
    val finalRssi: Int,
    val initialUncertainty: Float?,
    val finalUncertainty: Float?,
    val initialSpatialCoverage: Int,
    val finalSpatialCoverage: Int,
    val modelConsistency: String,
    val measurements: List<SessionMeasurementSnapshot>
)

