package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "signal_fingerprints")
data class SignalFingerprintEntity(
    @PrimaryKey val id: String,
    val signalType: String, // "WIFI", "BLE", "CELLULAR", etc.
    val frequencyMean: Double,
    val bandwidthMean: Double,
    val rssiMean: Double,
    val timingIntervalMean: Double, // ms
    val observationCount: Int,
    val firstObservedMs: Long,
    val lastObservedMs: Long,
    val provenance: String, // from DataProvenance
    val knownMacAddress: String? = null, // For strong matching when available
    val lastAnomalyScore: Int? = null,
    val lastAnomalyConfidence: Float? = null
)


fun SignalFingerprintEntity.toDomainModel(): SignalFingerprint {
    return SignalFingerprint(
        id = this.id,
        signalType = this.signalType,
        frequencyMean = this.frequencyMean,
        bandwidthMean = this.bandwidthMean,
        rssiMean = this.rssiMean,
        timingIntervalMean = this.timingIntervalMean,
        observationCount = this.observationCount,
        firstObservedMs = this.firstObservedMs,
        lastObservedMs = this.lastObservedMs,
        provenance = DataProvenance.valueOf(this.provenance),
        knownMacAddress = this.knownMacAddress,
        lastAnomalyScore = this.lastAnomalyScore,
        lastAnomalyConfidence = this.lastAnomalyConfidence
    )
}
