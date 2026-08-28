package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class EvidenceType {
    RSSI,
    FREQUENCY,
    CHANNEL,
    SIGNAL_FINGERPRINT,
    TIMING,
    DEVICE_METADATA,
    MANUFACTURER_INFORMATION,
    SPATIAL_OBSERVATION,
    NOISE_FLOOR,
    HISTORICAL_RECURRENCE,
    CLASSIFICATION,
    BEHAVIORAL_SIMILARITY,
    IDENTITY_SIMILARITY
}

@Entity(tableName = "rf_evidence")
data class EvidenceItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val type: String, // EvidenceType name
    val sourceEventId: String?,
    val sourceSensor: String,
    val timestampMs: Long,
    val measurement: String,
    val value: String,
    val unit: String,
    val reliability: Float, // 0.0 to 1.0
    val confidence: Float, // 0.0 to 1.0
    val weight: Float,
    val isSupporting: Boolean,
    val analysisComponent: String,
    val relatedDeviceId: String?,
    val relatedAnomalyId: String?,
    val relatedPatternId: String?
)
