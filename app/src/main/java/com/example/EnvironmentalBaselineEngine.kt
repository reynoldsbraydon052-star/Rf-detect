package com.example

import kotlin.math.abs

class EnvironmentalBaselineEngine {
    
    // Configurable thresholds
    var minObservationsForKnown = 5
    var learningRate = 0.05f
    var anomalyRssiThresholdDb = 20.0
    var anomalousActivityDeltaThreshold = 0.5f // 50% change

    fun processBaseline(
        blips: List<RadarBlip>,
        fingerprintDb: Map<String, SignalFingerprint>,
        isLearning: Boolean,
        baselineObservations: Long,
        baselineAvgActiveBlips: Float,
        baselineAvgFreqOccupancy: Float,
        baselineStartedAtMs: Long
    ): Pair<List<RadarBlip>, BaselineSummary> {
        var knownCount = 0
        var newCount = 0
        var anomalousCount = 0
        var changedCount = 0

        val currentActiveBlips = blips.size
        var currentFreqOccupancy = 0f

        val processedBlips = blips.map { blip ->
            // Update occupancy calculation (simple approximation: sum of bandwidths or arbitrary weight per signal)
            val bw = blip.bandwidthMhz ?: 20.0
            currentFreqOccupancy += bw.toFloat()

            var state = BaselineState.UNKNOWN
            
            if (blip.provenance == DataProvenance.SIMULATED || blip.provenance == DataProvenance.REPLAY) {
                // Ignore simulated/replay data for baseline calculations
                return@map blip.copy(baselineState = BaselineState.UNKNOWN)
            }
            
            val fp = blip.fingerprintId?.let { fingerprintDb[it] }

            if (fp == null) {
                state = BaselineState.NEW
                newCount++
            } else {
                if (fp.observationCount < minObservationsForKnown) {
                    state = BaselineState.NEW
                    newCount++
                } else {
                    // Check for deviations from established baseline
                    val rssiDeviation = abs(blip.rssi - fp.rssiMean)
                    if (rssiDeviation > anomalyRssiThresholdDb) {
                        state = BaselineState.ANOMALOUS
                        anomalousCount++
                    } else if (rssiDeviation > (anomalyRssiThresholdDb * 0.6)) {
                        state = BaselineState.CHANGED
                        changedCount++
                    } else {
                        state = BaselineState.KNOWN
                        knownCount++
                    }
                }
            }
            blip.copy(baselineState = state)
        }

        // Calculate missing fingerprints
        // A missing fingerprint is one that is "KNOWN" in DB but not currently active.
        // For simplicity, we assume fingerprintDb passed here are only the recent ones, 
        // or we need to pass all known fingerprints. We will let the ViewModel pass all known fingerprints.
        val activeFpIds = processedBlips.mapNotNull { it.fingerprintId }.toSet()
        val missingCount = fingerprintDb.values.count { 
            it.observationCount >= minObservationsForKnown && !activeFpIds.contains(it.id)
        }

        // Calculate deltas
        val rfActivityDelta = if (baselineAvgActiveBlips > 0f) {
            (currentActiveBlips - baselineAvgActiveBlips) / baselineAvgActiveBlips
        } else 0f
        
        val freqOccupancyDelta = if (baselineAvgFreqOccupancy > 0f) {
            (currentFreqOccupancy - baselineAvgFreqOccupancy) / baselineAvgFreqOccupancy
        } else 0f

        // Confidence grows with observations
        val confidence = (baselineObservations / 1000f).coerceIn(0f, 1f)
        
        val summary = BaselineSummary(
            knownFingerprints = knownCount,
            newFingerprints = newCount,
            missingFingerprints = missingCount,
            rfActivityDeltaPercent = rfActivityDelta,
            freqOccupancyDeltaPercent = freqOccupancyDelta,
            baselineConfidence = confidence,
            isLearning = isLearning,
            baselineAgeMs = if (baselineStartedAtMs > 0) System.currentTimeMillis() - baselineStartedAtMs else 0L,
            observationsCollected = baselineObservations
        )

        return Pair(processedBlips, summary)
    }
}
