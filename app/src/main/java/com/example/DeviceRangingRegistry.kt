package com.example

import kotlin.math.pow

/**
 * 1D Kalman Filter tailored for smoothing signal characteristics (RSSI or raw distance).
 */
class RangingKalmanFilter(
    private val processNoise: Double = 0.012, // Q: true environment variance
    private val measurementNoise: Double = 0.85, // R: noise/multipath fluctuation
    private var estimatedError: Double = 1.0,
    private var lastEstimate: Double = -75.0
) {
    fun update(measurement: Double): Double {
        estimatedError += processNoise
        val gain = estimatedError / (estimatedError + measurementNoise)
        val currentEstimate = lastEstimate + gain * (measurement - lastEstimate)
        estimatedError *= (1.0 - gain)
        lastEstimate = currentEstimate
        return currentEstimate
    }
}

/**
 * High-fidelity ranging telemetry calculated for a specific device frame.
 */
data class DeviceRangingTelemetry(
    val macAddress: String,
    val smoothedRssi: Double,
    val estimatedDistanceMeters: Double
)

/**
 * Thread-safe registry that manages individual Kalman filters and TX power profiles for each device.
 */
class DeviceRangingRegistry {
    private val registry = java.util.concurrent.ConcurrentHashMap<String, Pair<RangingKalmanFilter, Int>>()

    fun processSignalFrame(
        macAddress: String,
        rawRssi: Int,
        txPower: Int? = null
    ): DeviceRangingTelemetry {
        val entry = registry.getOrPut(macAddress) {
            val filter = RangingKalmanFilter(lastEstimate = rawRssi.toDouble())
            // Fallback to -59 dBm (typical BLE TxPower at 1m) if txPower is null or zero
            val resolvedTx = if (txPower == null || txPower == 0) -59 else txPower
            Pair(filter, resolvedTx)
        }

        val filter = entry.first
        val resolvedTxPower = if (txPower == null || txPower == 0) entry.second else txPower

        // 1. Smooth out raw RSSI readings using the Kalman filter
        val smoothedRssi = filter.update(rawRssi.toDouble())

        // 2. Map smoothed RSSI to distance using the Log-Distance Path Loss Model:
        // d = 10^((TxPower - SmoothedRSSI) / (10 * n))
        // Where n = 2.6 represents an office or residential cluttered indoor multipath coefficient
        val pathLossExponent = 2.6
        val exponent = (resolvedTxPower.toDouble() - smoothedRssi) / (10.0 * pathLossExponent)
        val distance = 10.0.pow(exponent)

        return DeviceRangingTelemetry(
            macAddress = macAddress,
            smoothedRssi = smoothedRssi,
            estimatedDistanceMeters = distance
        )
    }
}

/**
 * Safe, non-intrusive integration bridge to inject advanced log-distance ranging
 * into the existing radar state without modifying primary model structures.
 */
object SignalRangingBridge {
    private val registry = DeviceRangingRegistry()

    /**
     * Call this where you currently update your radar blips.
     * It returns a smoothed distance while leaving all other blip fields intact.
     */
    fun getFilteredDistance(
        macAddress: String,
        rawRssi: Int,
        fallbackDistance: Double
    ): Double {
        return try {
            val telemetry = registry.processSignalFrame(
                macAddress = macAddress,
                rawRssi = rawRssi
            )
            telemetry.estimatedDistanceMeters
        } catch (e: Exception) {
            // Failsafe fallback
            fallbackDistance
        }
    }
}
