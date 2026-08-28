package com.example

/**
 * Immutable telemetry state representing the processed electromagnetic anomaly analysis.
 *
 * @property rawMagnitudeUtd Instantaneous total Euclidean magnetic field strength in microteslas (µT).
 * @property anomalyDeltaUtd Absolute differential shift (|B_instant - B_baseline|) above ambient geomagnetic baseline.
 * @property jitterRippleUtd High-frequency AC jitter calculated via rolling Mean Absolute Deviation (MAD).
 * @property isDynamicAcDetected True if jitter indicates dynamic active power draw (transformers, motors, SMPS).
 * @property isStaticDcAnomaly True if a high localized shift exists without high-frequency AC oscillation (ferromagnetic objects).
 * @property normalizedEmfThreatScore Threat level index bounded strictly between 0.0f (ambient) and 1.0f (critical proximity).
 * @property baselineMagnitudeUtd Exponential Moving Average (EMA) baseline of the ambient geomagnetic field.
 * @property sampleCount Total count of valid magnetic samples processed since engine start.
 * @property timestampNs System nanosecond timestamp of the latest sample.
 */
data class EmfTelemetryState(
    val rawMagnitudeUtd: Float = 0f,
    val anomalyDeltaUtd: Float = 0f,
    val jitterRippleUtd: Float = 0f,
    val isDynamicAcDetected: Boolean = false,
    val isStaticDcAnomaly: Boolean = false,
    val normalizedEmfThreatScore: Float = 0f,
    val baselineMagnitudeUtd: Float = 0f,
    val sampleCount: Long = 0L,
    val timestampNs: Long = 0L
)
