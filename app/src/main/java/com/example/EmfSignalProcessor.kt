package com.example

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * High-performance, zero-allocation signal processor for magnetometer telemetry.
 *
 * Mathematical Formulations:
 * 1. Total 3-Axis Magnitude:
 *    B = sqrt(x^2 + y^2 + z^2) [µT]
 *
 * 2. Dynamic Geomagnetic Baseline (EMA Filter):
 *    B_baseline[t] = α * B[t] + (1 - α) * B_baseline[t-1]
 *    where default α = 0.01 (slow temporal adaptation to ambient Earth field).
 *
 * 3. Localized Anomaly Delta:
 *    ΔB = |B[t] - B_baseline[t]| [µT]
 *
 * 4. High-Frequency AC Jitter / Ripple Index:
 *    Computed via Rolling Mean Absolute Deviation (MAD) over a circular primitive window (N=50):
 *    µ_window = (1 / K) * Σ B_i
 *    MAD = (1 / K) * Σ |B_i - µ_window|
 *
 * 5. Anomaly Separation:
 *    - Static DC Anomaly: ΔB >= 15.0 µT and MAD < 2.0 µT (e.g. structural steel, permanent magnets)
 *    - Dynamic AC Ripple / Active Draw: MAD >= 2.0 µT (e.g. 50/60Hz AC power conduits, switching power supplies)
 */
class EmfSignalProcessor(
    val windowSize: Int = 50,
    val emaAlpha: Float = 0.01f,
    val staticDcThresholdUtd: Float = 15.0f,
    val dynamicAcRippleThresholdUtd: Float = 2.0f
) {
    // Circular buffer for rolling magnitude samples (Zero-Allocation primitive buffer)
    private val magnitudeWindow = FloatArray(windowSize)
    private var windowHead = 0
    private var windowCount = 0
    private var runningWindowSum = 0.0

    // Baseline tracking
    private var baselineMagnitude: Float = 0f
    private var isBaselineInitialized: Boolean = false
    private var totalSamplesProcessed: Long = 0L

    /**
     * Calculates Euclidean magnitude of a 3-axis magnetic vector.
     * Rejects NaN and infinite values, returning 0.0f on corrupt inputs.
     */
    fun calculateMagnitude(x: Float, y: Float, z: Float): Float {
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) {
            return 0.0f
        }
        return sqrt((x * x) + (y * y) + (z * z))
    }

    /**
     * Updates and retrieves the Exponential Moving Average baseline.
     */
    fun updateBaseline(currentMagnitude: Float): Float {
        if (!currentMagnitude.isFinite() || currentMagnitude <= 0f) {
            return baselineMagnitude
        }

        if (!isBaselineInitialized) {
            baselineMagnitude = currentMagnitude
            isBaselineInitialized = true
        } else {
            baselineMagnitude = (emaAlpha * currentMagnitude) + ((1.0f - emaAlpha) * baselineMagnitude)
        }
        return baselineMagnitude
    }

    /**
     * Manually resets the geomagnetic baseline.
     */
    fun resetBaseline(initialValue: Float? = null) {
        if (initialValue != null && initialValue.isFinite() && initialValue > 0f) {
            baselineMagnitude = initialValue
            isBaselineInitialized = true
        } else {
            baselineMagnitude = 0f
            isBaselineInitialized = false
        }
        windowHead = 0
        windowCount = 0
        runningWindowSum = 0.0
        magnitudeWindow.fill(0f)
        totalSamplesProcessed = 0L
    }

    /**
     * Inserts a sample into the circular primitive buffer and calculates rolling Mean Absolute Deviation (MAD).
     */
    fun updateWindowAndComputeJitter(magnitude: Float): Float {
        if (!magnitude.isFinite()) return 0f

        // Remove oldest sample from running sum if buffer is full
        if (windowCount == windowSize) {
            runningWindowSum -= magnitudeWindow[windowHead]
        } else {
            windowCount++
        }

        // Insert new sample
        magnitudeWindow[windowHead] = magnitude
        runningWindowSum += magnitude
        windowHead = (windowHead + 1) % windowSize

        if (windowCount < 2) return 0f

        val mean = (runningWindowSum / windowCount).toFloat()

        // Zero-allocation computation of Mean Absolute Deviation
        var absoluteDevSum = 0f
        for (i in 0 until windowCount) {
            absoluteDevSum += abs(magnitudeWindow[i] - mean)
        }

        return absoluteDevSum / windowCount
    }

    /**
     * Computes the bounded threat score in [0.0, 1.0].
     * Blends localized DC delta and high-frequency AC jitter with calibrated weighting.
     */
    fun calculateThreatScore(anomalyDelta: Float, jitterMad: Float, isDynamicAc: Boolean): Float {
        val safeDelta = anomalyDelta.coerceAtLeast(0f)
        val safeJitter = jitterMad.coerceAtLeast(0f)

        // Baseline threat components
        val deltaComponent = (safeDelta / 100.0f).coerceIn(0.0f, 1.0f) * 0.55f
        val jitterComponent = (safeJitter / 10.0f).coerceIn(0.0f, 1.0f) * 0.45f

        var score = deltaComponent + jitterComponent

        // Elevate score when active AC ripple / dynamic drawing is confirmed
        if (isDynamicAc && score < 0.35f) {
            score = (score + 0.35f).coerceAtMost(1.0f)
        }

        return score.coerceIn(0.0f, 1.0f)
    }

    /**
     * Processes a single [MagneticSample] and generates an updated [EmfTelemetryState].
     * Returns null if sample values are non-finite or corrupt.
     */
    fun processSample(sample: MagneticSample): EmfTelemetryState? {
        return processRaw(
            x = sample.x,
            y = sample.y,
            z = sample.z,
            timestampNs = sample.timestampNs
        )
    }

    /**
     * Directly processes 3-axis raw sensor values into telemetry state.
     */
    fun processRaw(
        x: Float,
        y: Float,
        z: Float,
        timestampNs: Long = System.nanoTime()
    ): EmfTelemetryState? {
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) {
            return null
        }

        val rawMag = calculateMagnitude(x, y, z)
        if (!rawMag.isFinite()) return null

        totalSamplesProcessed++

        val baseline = updateBaseline(rawMag)
        val delta = abs(rawMag - baseline)
        val jitter = updateWindowAndComputeJitter(rawMag)

        val isDynamicAc = jitter >= dynamicAcRippleThresholdUtd
        val isStaticDc = delta >= staticDcThresholdUtd && jitter < dynamicAcRippleThresholdUtd
        val threatScore = calculateThreatScore(delta, jitter, isDynamicAc)

        return EmfTelemetryState(
            rawMagnitudeUtd = rawMag,
            anomalyDeltaUtd = delta,
            jitterRippleUtd = jitter,
            isDynamicAcDetected = isDynamicAc,
            isStaticDcAnomaly = isStaticDc,
            normalizedEmfThreatScore = threatScore,
            baselineMagnitudeUtd = baseline,
            sampleCount = totalSamplesProcessed,
            timestampNs = timestampNs
        )
    }
}
