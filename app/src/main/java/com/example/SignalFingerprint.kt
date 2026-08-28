package com.example

import java.util.UUID

data class SignalFingerprint(
    val id: String = UUID.randomUUID().toString(),
    val signalType: String,
    val frequencyMean: Double,
    val bandwidthMean: Double,
    val rssiMean: Double,
    val timingIntervalMean: Double,
    val observationCount: Int = 1,
    val firstObservedMs: Long = System.currentTimeMillis(),
    val lastObservedMs: Long = System.currentTimeMillis(),
    val provenance: DataProvenance = DataProvenance.UNKNOWN,
    val knownMacAddress: String? = null,
    val lastAnomalyScore: Int? = null,
    val lastAnomalyConfidence: Float? = null
) {
    fun toEntity(): SignalFingerprintEntity {
        return SignalFingerprintEntity(
            id = id,
            signalType = signalType,
            frequencyMean = frequencyMean,
            bandwidthMean = bandwidthMean,
            rssiMean = rssiMean,
            timingIntervalMean = timingIntervalMean,
            observationCount = observationCount,
            firstObservedMs = firstObservedMs,
            lastObservedMs = lastObservedMs,
            provenance = provenance.name,
            knownMacAddress = knownMacAddress,
            lastAnomalyScore = lastAnomalyScore,
            lastAnomalyConfidence = lastAnomalyConfidence
        )
    }

    companion object {
        fun fromEntity(entity: SignalFingerprintEntity): SignalFingerprint {
            return SignalFingerprint(
                id = entity.id,
                signalType = entity.signalType,
                frequencyMean = entity.frequencyMean,
                bandwidthMean = entity.bandwidthMean,
                rssiMean = entity.rssiMean,
                timingIntervalMean = entity.timingIntervalMean,
                observationCount = entity.observationCount,
                firstObservedMs = entity.firstObservedMs,
                lastObservedMs = entity.lastObservedMs,
                provenance = try { DataProvenance.valueOf(entity.provenance) } catch (e: Exception) { DataProvenance.UNKNOWN },
                knownMacAddress = entity.knownMacAddress,
                lastAnomalyScore = entity.lastAnomalyScore,
                lastAnomalyConfidence = entity.lastAnomalyConfidence
            )
        }
    }
}

data class FingerprintMatchResult(
    val fingerprint: SignalFingerprint,
    val confidence: Float,
    val supportingCharacteristics: Map<String, Float>
)
