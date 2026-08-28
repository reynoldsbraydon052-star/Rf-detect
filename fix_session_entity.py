import re

with open('app/src/main/java/com/example/IntelligenceModels.kt', 'r') as f:
    content = f.read()

replacement = """
enum class SessionState {
    RECORDING, COMPLETED, PAUSED, ARCHIVED, CORRUPTED,
    ACTIVE, CLOSED // kept for backward compatibility during migration
}

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
"""

content = re.sub(r'enum class SessionState \{.*?\}\s*@Entity\(tableName = "rf_session"\)\s*data class RfSessionEntity\([^\)]+\)', replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/IntelligenceModels.kt', 'w') as f:
    f.write(content)

