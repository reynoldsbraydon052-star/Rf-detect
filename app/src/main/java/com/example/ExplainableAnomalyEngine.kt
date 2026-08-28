package com.example

import kotlin.math.abs

class ExplainableAnomalyEngine {
    private var isIsolated = false

    fun isolateStateForSimulation() {
        isIsolated = true
    }

    fun resetIsolatedState() {
        isIsolated = false
    }


    fun evaluateAnomaly(
        blip: RadarBlip,
        fingerprintDb: Map<String, SignalFingerprint>,
        baselineSummary: BaselineSummary
    ): AnomalyResult {
        var score = 0
        var confidence = 1.0f
        val explanations = mutableListOf<AnomalyExplanation>()

        if (blip.provenance == DataProvenance.SIMULATED || blip.provenance == DataProvenance.REPLAY) {
            return AnomalyResult(
                score = 0,
                confidence = 0f,
                category = AnomalyCategory.NORMAL,
                explanations = listOf(AnomalyExplanation("Simulated/Replay data ignored", 0))
            )
        }

        val fp = blip.fingerprintId?.let { fingerprintDb[it] }

        // 1. Fingerprint Evidence
        if (fp == null) {
            score += 35
            explanations.add(AnomalyExplanation("New fingerprint", 35))
            confidence *= 0.8f // Less confidence without historical baseline
        } else {
            if (fp.observationCount < 5) {
                score += 20
                explanations.add(AnomalyExplanation("Low observation history for fingerprint", 20))
                confidence *= 0.6f // Insufficient data for high confidence
            } else {
                score -= 15
                explanations.add(AnomalyExplanation("Previously observed fingerprint", -15))

                // RSSI Deviation
                val rssiDev = abs(blip.rssi - fp.rssiMean)
                if (rssiDev > 20.0) {
                    score += 25
                    explanations.add(AnomalyExplanation("Highly unusual RSSI behavior", 25))
                } else if (rssiDev > 10.0) {
                    score += 15
                    explanations.add(AnomalyExplanation("Unusual RSSI behavior", 15))
                } else if (rssiDev <= 5.0) {
                    score -= 5
                    explanations.add(AnomalyExplanation("Behavior matches normal baseline", -5))
                }

                // Frequency Deviation
                val freqDev = abs(blip.frequencyMhz - fp.frequencyMean)
                if (freqDev > 10.0) {
                    score += 20
                    explanations.add(AnomalyExplanation("Frequency behavior differs significantly from baseline", 20))
                } else if (freqDev > 2.0) {
                    score += 10
                    explanations.add(AnomalyExplanation("Frequency drift detected", 10))
                }

                // Bandwidth Deviation
                if (blip.bandwidthMhz != null) {
                    val bwDev = abs(blip.bandwidthMhz - fp.bandwidthMean)
                    if (bwDev > 10.0) {
                        score += 15
                        explanations.add(AnomalyExplanation("Unusual bandwidth", 15))
                    }
                }

                // Timing / Persistence
                if (blip.pulseDurationMs != null && fp.timingIntervalMean > 0) {
                    val timingDev = abs(blip.pulseDurationMs - fp.timingIntervalMean)
                    if (timingDev > fp.timingIntervalMean * 0.5) {
                        score += 20
                        explanations.add(AnomalyExplanation("Unusual timing pattern", 20))
                    }
                }
            }
        }

        // 2. Environmental Evidence
        if (baselineSummary.observationsCollected > 50) {
            if (baselineSummary.rfActivityDeltaPercent > 0.5f) {
                score += 15
                explanations.add(AnomalyExplanation("Environmental RF activity increased", 15))
            } else if (baselineSummary.rfActivityDeltaPercent < -0.5f) {
                score += 10
                explanations.add(AnomalyExplanation("Environmental RF activity decreased", 10))
            }

            if (baselineSummary.freqOccupancyDeltaPercent > 0.5f) {
                score += 15
                explanations.add(AnomalyExplanation("Significant environmental change (frequency)", 15))
            }
        } else {
            confidence *= 0.5f
            explanations.add(AnomalyExplanation("Insufficient historical environmental observations", 0))
        }

        // Apply global baseline confidence
        confidence *= baselineSummary.baselineConfidence

        // Bound properties
        score = score.coerceIn(0, 100)
        confidence = confidence.coerceIn(0f, 1f)

        val category = when {
            score >= 70 -> AnomalyCategory.HIGH_DEVIATION
            score >= 40 -> AnomalyCategory.MODERATE_DEVIATION
            score >= 15 -> AnomalyCategory.LOW_DEVIATION
            else -> AnomalyCategory.NORMAL
        }

        explanations.sortByDescending { abs(it.scoreImpact) }

        return AnomalyResult(
            score = score,
            confidence = confidence,
            category = category,
            previousScore = fp?.lastAnomalyScore,
            explanations = explanations
        )
    }
}
