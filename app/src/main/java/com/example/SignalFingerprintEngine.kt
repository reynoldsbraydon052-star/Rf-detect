package com.example

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SignalFingerprintEngine(private val dao: SignalFingerprintDao) {
    fun getAllFingerprintsFlow(): Flow<Map<String, SignalFingerprint>> {
        return dao.getAllFingerprintsFlow().map { list ->
            list.associate { it.id to it.toDomainModel() }
        }
    }

    suspend fun updateFingerprint(fingerprint: SignalFingerprint) {
        dao.insertFingerprint(fingerprint.toEntity())
    }

    fun processObservation(blip: RadarBlip): FingerprintMatchResult {
        // Placeholder implementation
        val fingerprint = SignalFingerprint(
            signalType = blip.type,
            frequencyMean = blip.frequencyMhz,
            bandwidthMean = blip.bandwidthMhz ?: 20.0,
            rssiMean = blip.rssi.toDouble(),
            timingIntervalMean = blip.pulseRepetitionIntervalMs ?: 100.0,
            provenance = blip.provenance
        )
        return FingerprintMatchResult(fingerprint, 0.8f, emptyMap())
    }

}
