import re
import os

with open('app/src/main/java/com/example/IntelligenceModels.kt', 'r') as f:
    content = f.read()

replacement = """
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

"""

# Replace the RfAnomalyEntity and TemporalAnomalyType
content = re.sub(
    r'// --- Anomaly Models ---.*?// --- Pattern Models ---',
    replacement + '\n// --- Pattern Models ---',
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/example/IntelligenceModels.kt', 'w') as f:
    f.write(content)

