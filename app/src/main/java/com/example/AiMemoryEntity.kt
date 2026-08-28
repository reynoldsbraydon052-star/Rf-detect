package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_memory")
data class AiMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val targetId: String?,
    val deviceType: String,
    val protocol: String,
    val displayName: String,
    val sanitizedAddress: String,
    val rssi: Int,
    val timestamp: Long,
    val anomalySummary: String?,
    val measurementSummary: String?,
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AiMemoryEntity

        if (id != other.id) return false
        if (targetId != other.targetId) return false
        if (deviceType != other.deviceType) return false
        if (protocol != other.protocol) return false
        if (displayName != other.displayName) return false
        if (sanitizedAddress != other.sanitizedAddress) return false
        if (rssi != other.rssi) return false
        if (timestamp != other.timestamp) return false
        if (anomalySummary != other.anomalySummary) return false
        if (measurementSummary != other.measurementSummary) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (targetId?.hashCode() ?: 0)
        result = 31 * result + deviceType.hashCode()
        result = 31 * result + protocol.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + sanitizedAddress.hashCode()
        result = 31 * result + rssi
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + (anomalySummary?.hashCode() ?: 0)
        result = 31 * result + (measurementSummary?.hashCode() ?: 0)
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
