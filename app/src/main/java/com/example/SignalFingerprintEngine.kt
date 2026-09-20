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

    suspend fun processObservation(blip: RadarBlip): FingerprintMatchResult {
        val fingerprintId = "${blip.type}:${blip.id}"
        val previous = dao.getFingerprint(fingerprintId)?.toDomainModel()
        val observationCount = (previous?.observationCount ?: 0) + 1
        val frequency = blip.frequencyMhz
        val bandwidth = blip.bandwidthMhz ?: previous?.bandwidthMean ?: 20.0
        val rssi = blip.rssi.toDouble()
        val timing = blip.pulseRepetitionIntervalMs ?: previous?.timingIntervalMean ?: 0.0
        val weight = previous?.observationCount?.toDouble() ?: 0.0
        val fingerprint = SignalFingerprint(
            id = fingerprintId,
            signalType = blip.type,
            frequencyMean = ((previous?.frequencyMean ?: frequency) * weight + frequency) / observationCount,
            bandwidthMean = ((previous?.bandwidthMean ?: bandwidth) * weight + bandwidth) / observationCount,
            rssiMean = ((previous?.rssiMean ?: rssi) * weight + rssi) / observationCount,
            timingIntervalMean = ((previous?.timingIntervalMean ?: timing) * weight + timing) / observationCount,
            observationCount = observationCount,
            firstObservedMs = previous?.firstObservedMs ?: System.currentTimeMillis(),
            lastObservedMs = System.currentTimeMillis(),
            provenance = if (previous?.provenance == DataProvenance.MEASURED && blip.provenance == DataProvenance.MEASURED) {
                DataProvenance.MEASURED
            } else {
                blip.provenance
            }
        )
        dao.insertFingerprint(fingerprint.toEntity())

        val rssiDelta = kotlin.math.abs(rssi - fingerprint.rssiMean)
        val frequencyDelta = kotlin.math.abs(frequency - fingerprint.frequencyMean)
        val confidence = (1f - (rssiDelta / 40.0 + frequencyDelta / 200.0).toFloat()).coerceIn(0f, 1f)
        return FingerprintMatchResult(
            fingerprint = fingerprint,
            confidence = confidence,
            supportingCharacteristics = mapOf(
                "rssiStability" to (1f - (rssiDelta / 40.0).toFloat()).coerceIn(0f, 1f),
                "frequencyStability" to (1f - (frequencyDelta / 200.0).toFloat()).coerceIn(0f, 1f),
                "observationSupport" to (observationCount / 10f).coerceIn(0f, 1f)
            )
        )
    }

}
