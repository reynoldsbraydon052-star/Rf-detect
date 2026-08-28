package com.example



class RangingConfidenceEvaluator {
    private val TAG = "RangingConfidenceEvaluator"

    /**
     * Evaluates and grades the confidence score and signal quality of a ranging measurement.
     *
     * @param targetMac The MAC Address of the target emitter.
     * @param distanceMeters The computed distance in meters.
     * @param method The ranging technology used (CS or RSSI).
     * @param rssiOrSignalMetric The raw signal strength (RSSI) or channel delay metric.
     * @param txPowerKnown Whether the target's actual TxPower was advertised and used.
     */
    fun evaluate(
        targetMac: String,
        distanceMeters: Double,
        method: RangingMethod,
        rssiOrSignalMetric: Int,
        txPowerKnown: Boolean
    ): TacticalRangingResult {
        val (confidenceScore, quality) = when (method) {
            RangingMethod.BLE_CHANNEL_SOUNDING -> {
                // High precision phase-based time-of-flight / channel sounding
                // Grade based on signal metric (e.g. RSSI during sounding)
                val baseConfidence = if (rssiOrSignalMetric > -70) 0.98 else 0.92
                Pair(baseConfidence, SignalQuality.HIGH)
            }
            RangingMethod.BLE_RSSI_ESTIMATE -> {
                // Highly volatile estimate based on LDPL modeling.
                // Subtract confidence if TxPower is assumed (unknown), as path-loss parameters are uncalibrated.
                var score = 0.45

                // Degrade score if signal is weak (indicates higher multipath/reflections)
                if (rssiOrSignalMetric < -80) {
                    score -= 0.15
                } else if (rssiOrSignalMetric < -65) {
                    score -= 0.05
                }

                if (!txPowerKnown) {
                    score -= 0.10 // Penalize because we are assuming default TxPower calibration
                }

                val finalScore = score.coerceIn(0.10, 0.50)
                val qualityGrade = when {
                    finalScore >= 0.40 -> SignalQuality.MEDIUM
                    else -> SignalQuality.LOW
                }

                Pair(finalScore, qualityGrade)
            }
        }

        android.util.Log.d(TAG, "Evaluated ranging confidence for target $targetMac ($method): Score = $confidenceScore, Quality = $quality")

        return TacticalRangingResult(
            targetMac = targetMac,
            distanceMeters = distanceMeters,
            method = method,
            confidenceScore = confidenceScore,
            quality = quality,
            rttOrRssiDb = rssiOrSignalMetric
        )
    }
}
