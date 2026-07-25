package com.example

/**
 * 1D Kalman Filter to process incoming RF signal strength readings (RSSI dBm, microTesla flux, or distance),
 * reducing environmental noise and measurement jitter in UI visualizations.
 *
 * @param processNoise Process noise covariance (Q) representing internal signal variance.
 * @param measurementNoise Measurement noise covariance (R) representing environmental noise.
 * @param estimatedError Initial estimation error covariance (P).
 */
class KalmanFilter(
    private var processNoise: Float = 0.008f,
    private var measurementNoise: Float = 0.5f,
    private var estimatedError: Float = 1.0f
) {
    private var currentEstimate: Float = Float.NaN

    /**
     * Filter a new scalar measurement.
     * @param measurement Raw incoming sensor reading (e.g., RSSI in dBm, microTesla, or distance).
     * @return Smoothed estimate with reduced noise and jitter.
     */
    fun update(measurement: Float): Float {
        if (currentEstimate.isNaN()) {
            currentEstimate = measurement
            return currentEstimate
        }

        // Time Update (Prediction)
        estimatedError += processNoise

        // Measurement Update (Correction)
        val kalmanGain = estimatedError / (estimatedError + measurementNoise)
        currentEstimate += kalmanGain * (measurement - currentEstimate)
        estimatedError = (1.0f - kalmanGain) * estimatedError

        return currentEstimate
    }

    /**
     * Returns the current filtered estimate.
     */
    fun getEstimate(): Float = currentEstimate

    /**
     * Set process noise covariance Q.
     */
    fun setProcessNoise(noise: Float) {
        this.processNoise = noise
    }

    /**
     * Set measurement noise covariance R.
     */
    fun setMeasurementNoise(noise: Float) {
        this.measurementNoise = noise
    }

    /**
     * Reset filter state for fresh signal streams.
     */
    fun reset(initialEstimate: Float = Float.NaN) {
        currentEstimate = initialEstimate
        estimatedError = 1.0f
    }
}

