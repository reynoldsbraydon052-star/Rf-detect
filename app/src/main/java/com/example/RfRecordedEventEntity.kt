package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.util.UUID

@Entity(tableName = "rf_recorded_events")
@JsonClass(generateAdapter = true)
data class RfRecordedEventEntity(
    @PrimaryKey val eventId: String = UUID.randomUUID().toString(),
    val sessionId: String = "", // Added for Feature 25
    val timestampMs: Long = System.currentTimeMillis(),
    val sensorSource: String,
    val signalType: String,
    val deviceId: String,
    val frequencyMhz: Double,
    val channel: Int?,
    val rssi: Int,
    val distanceMeters: Float?,
    val bandLabel: String,
    val classification: String?,
    val fingerprintId: String?,
    val classificationConfidence: Float?,
    val anomalyScore: Float?,
    val evidenceScore: Float?,
    val isSelectedTarget: Boolean,
    val manufacturerInfo: String?,
    val rawMetadata: String?,
    val provenance: String
)
