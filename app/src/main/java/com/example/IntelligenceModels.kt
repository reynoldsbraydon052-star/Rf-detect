package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// --- Session Models ---


enum class SessionState {
    RECORDING, COMPLETED, PAUSED, ARCHIVED, CORRUPTED,
    ACTIVE, CLOSED // kept for backward compatibility during migration
}

@com.squareup.moshi.JsonClass(generateAdapter = true)
@Entity(tableName = "rf_session")
data class RfSessionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val startTimeMs: Long,
    val endTimeMs: Long?,
    val state: String, // SessionState name
    val eventCount: Int,
    val anomalyCount: Int,
    val deviceCount: Int,
    val mapCellCount: Int,
    val configurationSnapshotJson: String,
    
    // New fields for Feature 25
    val recordingSource: String? = null,
    val sensorSourceInfo: String? = null,
    val appVersion: String? = null,
    val analysisVersion: String? = null,
    val detectorVersions: String? = null,
    val frequencyCount: Int = 0,
    val locationAvailability: Boolean = false,
    val provenance: String? = null
)



// --- Anomaly Models ---
enum class TemporalAnomalyType(val displayName: String) {
    SUDDEN_SIGNAL_APPEARANCE("Sudden Signal Appearance"),
    SUDDEN_SIGNAL_DISAPPEARANCE("Sudden Signal Disappearance"),
    RSSI_ANOMALY("RSSI Anomaly"),
    FREQUENCY_ANOMALY("Frequency Anomaly"),
    CHANNEL_ANOMALY("Channel Anomaly"),
    TIMING_ANOMALY("Timing Anomaly"),
    DEVICE_BEHAVIOR_CHANGE("Device Behavior Change"),
    NEW_DEVICE_CLUSTER("New Device Cluster"),
    FREQUENCY_DENSITY_ANOMALY("Frequency Density Anomaly"),
    NOISE_FLOOR_ANOMALY("Noise Floor Anomaly")
}

enum class AnomalySeverity {
    INFO, LOW, MEDIUM, HIGH, CRITICAL
}

enum class AnomalyStatus {
    NEW, ACKNOWLEDGED, INVESTIGATING, RESOLVED, DISMISSED
}

@Entity(tableName = "rf_anomaly")
data class RfAnomalyEntity(
    @PrimaryKey val id: String, // anomalyId
    val sessionId: String,
    val timestampMs: Long,
    val startTimestampMs: Long,
    val endTimestampMs: Long,
    val type: String, // TemporalAnomalyType name
    val severity: String, // AnomalySeverity name
    val confidenceScore: Int, // 0-100
    val evidenceScore: Int,
    val deviceId: String?,
    val frequencyMhz: Double?,
    val band: String?,
    val locationReference: String?,
    val sourceEventIdsJson: String, // List<String>
    val relatedPatternIdsJson: String, // List<String>
    val relatedIdentityHypothesisId: String?,
    val baselineInformationJson: String,
    val detectionAlgorithm: String,
    val algorithmVersion: String,
    val supportingEvidenceJson: String, // List<String>
    val contradictingEvidenceJson: String, // List<String>
    val status: String, // AnomalyStatus name
    val provenance: String, // DataProvenance name
    
    // Kept for backward compatibility/simplicity
    val spatialX: Float?,
    val spatialY: Float?
)

@Entity(tableName = "rf_anomaly_correlation")
data class AnomalyCorrelationEntity(
    @PrimaryKey val correlationId: String,
    val relatedAnomalyIdsJson: String, // List<String>
    val confidence: Int,
    val correlationType: String, // AnomalyCorrelationType name
    val timeRelationship: String,
    val spatialRelationship: String,
    val frequencyRelationship: String,
    val deviceRelationship: String,
    val supportingEvidenceJson: String,
    val contradictingEvidenceJson: String,
    val provenance: String
)

enum class AnomalyCorrelationType {
    TEMPORAL, SPATIAL, FREQUENCY, DEVICE, BEHAVIORAL, MULTI_FACTOR
}


// --- Pattern Models ---

enum class PatternType {
    PERIODIC, BURST, DIURNAL, SESSION_CLUSTER, RANDOMIZED_INTERVAL, CHANNEL_HOPPING, BAND_SHIFT
}

enum class PatternStability {
    STABLE, SEMI_STABLE, UNSTABLE, EMERGING, DISAPPEARING
}

@Entity(tableName = "rf_pattern")
data class RfPatternEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val deviceHypothesisId: String?,
    val type: String, // PatternType
    val stability: String, // PatternStability
    val confidenceScore: Int, // 0-100
    val firstObservedMs: Long,
    val lastObservedMs: Long,
    val observationCount: Int,
    val frequencyMhzMean: Double?,
    val supportingEventIdsJson: String
)

// --- Correlation Graph Models (In-Memory) ---

data class CorrelationNode(
    val id: String,
    val type: NodeType,
    val label: String,
    val timestamp: Long
)

enum class NodeType {
    DEVICE, EVENT, ANOMALY, PATTERN, SPATIAL_CELL, SESSION
}

data class CorrelationEdge(
    val sourceId: String,
    val targetId: String,
    val weight: Int, // 0-100
    val relationshipType: String,
    val explanation: String
)

data class IntelligenceGraph(
    val nodes: List<CorrelationNode>,
    val edges: List<CorrelationEdge>
)
