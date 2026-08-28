package com.example

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Calculated real-time telemetry metrics for an RF signal stream.
 */
data class ComputedTelemetryMetrics(
    val currentRssi: Double,
    val peakRssi: Double,
    val minRssi: Double,
    val avgRssi: Double,
    val jitterDbm: Double,
    val stabilityPercent: Double,
    val standardDeviation: Double,
    val sampleCount: Int,
    val driftVelocityDbmPerSec: Double = 0.0
)

/**
 * Dynamic Y-axis viewport bounds.
 */
data class DynamicYAxisBounds(
    val minY: Float,
    val maxY: Float
)

/**
 * Mathematical calculation engine for RF signal fluctuations, jitter, stability,
 * and adaptive canvas viewports.
 */
object SignalTelemetryCalculator {

    /**
     * Computes real-time telemetry metrics from a list of rolling samples.
     * Sanitizes inputs and handles edge cases (empty list, single sample, constant signal, extreme jitter).
     */
    fun calculateMetrics(samples: List<TelemetrySample>): ComputedTelemetryMetrics {
        val validSamples = samples.filter { it.isValid() }
        if (validSamples.isEmpty()) {
            return ComputedTelemetryMetrics(
                currentRssi = -100.0,
                peakRssi = -100.0,
                minRssi = -100.0,
                avgRssi = -100.0,
                jitterDbm = 0.0,
                stabilityPercent = 0.0,
                standardDeviation = 0.0,
                sampleCount = 0,
                driftVelocityDbmPerSec = 0.0
            )
        }

        if (validSamples.size == 1) {
            val singleRssi = validSamples[0].rssiDbm
            return ComputedTelemetryMetrics(
                currentRssi = singleRssi,
                peakRssi = singleRssi,
                minRssi = singleRssi,
                avgRssi = singleRssi,
                jitterDbm = 0.0,
                stabilityPercent = 100.0, // Single observation has 0 variance
                standardDeviation = 0.0,
                sampleCount = 1,
                driftVelocityDbmPerSec = 0.0
            )
        }

        val rssiValues = validSamples.map { it.rssiDbm }
        val currentRssi = rssiValues.last()
        val peakRssi = rssiValues.maxOrNull() ?: currentRssi
        val minRssi = rssiValues.minOrNull() ?: currentRssi
        val avgRssi = rssiValues.average()

        // 1. Jitter Calculation: Mean Absolute Deviation (MAD) of consecutive RSSI deltas
        var deltaSum = 0.0
        for (i in 1 until rssiValues.size) {
            deltaSum += abs(rssiValues[i] - rssiValues[i - 1])
        }
        val jitterDbm = deltaSum / (rssiValues.size - 1)

        // 2. Standard Deviation of the active window
        val variance = rssiValues.map { (it - avgRssi).pow(2) }.average()
        val standardDeviation = sqrt(variance)

        // 3. Drift Velocity (dBm/sec) between first and last sample in window
        val timeSpanSec = (validSamples.last().timestampMs - validSamples.first().timestampMs).coerceAtLeast(100L) / 1000.0
        val driftVelocity = ((validSamples.last().rssiDbm - validSamples.first().rssiDbm) / timeSpanSec).coerceIn(-25.0, 25.0)

        // 4. Normalized Stability Index (0.0% to 100.0%)
        // Formula combines jitter penalty and variance penalty
        val jitterPenalty = jitterDbm * 7.5
        val variancePenalty = standardDeviation * 2.5
        val rawStability = 100.0 - jitterPenalty - variancePenalty
        val stabilityPercent = rawStability.coerceIn(0.0, 100.0)

        return ComputedTelemetryMetrics(
            currentRssi = currentRssi,
            peakRssi = peakRssi,
            minRssi = minRssi,
            avgRssi = avgRssi,
            jitterDbm = jitterDbm,
            stabilityPercent = stabilityPercent,
            standardDeviation = standardDeviation,
            sampleCount = validSamples.size,
            driftVelocityDbmPerSec = driftVelocity
        )
    }

    /**
     * Calculates dynamic Y-axis bounds (minY, maxY) with adaptive headroom based on the active rolling window.
     * Ensures minimum headroom (e.g. 10 dBm) and prevents scale collapse for flat signals.
     */
    fun calculateDynamicYAxisBounds(
        samples: List<TelemetrySample>,
        minHeadroomDbm: Float = 10.0f,
        clampedAbsoluteMin: Float = -110.0f,
        clampedAbsoluteMax: Float = -10.0f
    ): DynamicYAxisBounds {
        val validSamples = samples.filter { it.isValid() }
        if (validSamples.isEmpty()) {
            return DynamicYAxisBounds(minY = -100.0f, maxY = -20.0f)
        }

        var observedMin = validSamples.minOf { it.rssiDbm.toFloat() }
        var observedMax = validSamples.maxOf { it.rssiDbm.toFloat() }

        // Expand bounds by minHeadroom
        var calculatedMin = observedMin - minHeadroomDbm
        var calculatedMax = observedMax + minHeadroomDbm

        // Ensure a minimum span of at least 20 dBm to prevent graphical distortion on flat signals
        if (calculatedMax - calculatedMin < 20.0f) {
            val center = (calculatedMin + calculatedMax) / 2.0f
            calculatedMin = center - 10.0f
            calculatedMax = center + 10.0f
        }

        // Clamp to absolute RF bounds
        val clampedMin = calculatedMin.coerceIn(clampedAbsoluteMin, clampedAbsoluteMax - 10.0f)
        val clampedMax = calculatedMax.coerceIn(clampedMin + 10.0f, clampedAbsoluteMax)

        return DynamicYAxisBounds(
            minY = clampedMin,
            maxY = clampedMax
        )
    }

    /**
     * Computes spectrum density and channel occupancy ratios across protocols.
     */
    fun computeSpectrumDensity(
        snapshots: Map<String, List<TelemetrySample>>
    ): Map<String, Float> {
        val totalValidSamples = snapshots.values.sumOf { list -> list.count { it.isValid() } }
        if (totalValidSamples == 0) {
            return mapOf(
                "WIFI" to 0.0f,
                "BLE" to 0.0f,
                "CELLULAR" to 0.0f,
                "MAGNETIC" to 0.0f
            )
        }

        val result = mutableMapOf<String, Float>()
        listOf("WIFI", "BLE", "CELLULAR", "MAGNETIC").forEach { proto ->
            val count = snapshots[proto]?.count { it.isValid() } ?: 0
            result[proto] = (count.toFloat() / totalValidSamples.toFloat()).coerceIn(0.0f, 1.0f)
        }
        return result
    }
}
