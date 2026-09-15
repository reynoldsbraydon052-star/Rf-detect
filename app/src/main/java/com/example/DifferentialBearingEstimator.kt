package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

enum class ScanState {
    IDLE,
    CALIBRATING_ROTATION,
    LOCKED,
    FAILED_INSUFFICIENT_DATA
}

data class BearingSweepResult(
    val estimatedBearing: Float,
    val peakRssi: Double,
    val troughRssi: Double,
    val signalDelta: Double,
    val confidenceScore: Float
)

class DifferentialBearingEstimator(
    private val binSizeDegrees: Int = 15
) {
    private val totalBins = 360 / binSizeDegrees
    private val angularBins = Array(totalBins) { mutableListOf<Double>() }

    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _sweepResult = MutableStateFlow<BearingSweepResult?>(null)
    val sweepResult: StateFlow<BearingSweepResult?> = _sweepResult.asStateFlow()

    private var targetMacAddress: String? = null
    private var startingAzimuth: Float = 0f
    private var accumulatedRotationDegrees: Float = 0f
    private var lastObservedAzimuth: Float = 0f

    fun startCalibration(macAddress: String, initialAzimuth: Float) {
        targetMacAddress = macAddress
        startingAzimuth = initialAzimuth
        lastObservedAzimuth = initialAzimuth
        accumulatedRotationDegrees = 0f
        
        for (bin in angularBins) {
            bin.clear()
        }
        
        _sweepResult.value = null
        _scanState.value = ScanState.CALIBRATING_ROTATION
    }

    fun onSensorSample(currentAzimuth: Float, incomingMac: String, smoothedRssi: Double) {
        if (_scanState.value != ScanState.CALIBRATING_ROTATION) return
        if (incomingMac != targetMacAddress) return

        val angleDiff = shortestAngleDifference(lastObservedAzimuth, currentAzimuth)
        accumulatedRotationDegrees += abs(angleDiff)
        lastObservedAzimuth = currentAzimuth

        val binIndex = ((currentAzimuth % 360) / binSizeDegrees).toInt().coerceIn(0, totalBins - 1)
        angularBins[binIndex].add(smoothedRssi)

        if (accumulatedRotationDegrees >= 340f) {
            evaluateBearing()
        }
    }

    private fun evaluateBearing() {
        val binAverages = FloatArray(totalBins) { -999f }
        var populatedBins = 0

        for (i in 0 until totalBins) {
            val samples = angularBins[i]
            if (samples.isNotEmpty()) {
                binAverages[i] = samples.average().toFloat()
                populatedBins++
            }
        }

        if (populatedBins < totalBins / 2) {
            _scanState.value = ScanState.FAILED_INSUFFICIENT_DATA
            return
        }

        var maxRssi = -999f
        var minRssi = 0f
        var peakBinIndex = 0

        for (i in 0 until totalBins) {
            val avg = binAverages[i]
            if (avg == -999f) continue
            if (avg > maxRssi) {
                maxRssi = avg
                peakBinIndex = i
            }
            if (avg < minRssi) {
                minRssi = avg
            }
        }

        val delta = (maxRssi - minRssi).toDouble()
        val confidence = (delta / 18.0).coerceIn(0.1, 1.0).toFloat()
        val calculatedBearing = (peakBinIndex * binSizeDegrees + (binSizeDegrees / 2f)) % 360f

        _sweepResult.value = BearingSweepResult(
            estimatedBearing = calculatedBearing,
            peakRssi = maxRssi.toDouble(),
            troughRssi = minRssi.toDouble(),
            signalDelta = delta,
            confidenceScore = confidence
        )

        _scanState.value = ScanState.LOCKED
    }

    fun calculateRelativeOffset(currentHeading: Float): Float? {
        val lockedBearing = _sweepResult.value?.estimatedBearing ?: return null
        var diff = lockedBearing - currentHeading
        while (diff < -180f) diff += 360f
        while (diff > 180f) diff -= 360f
        return diff
    }

    fun reset() {
        _scanState.value = ScanState.IDLE
        _sweepResult.value = null
        for (bin in angularBins) bin.clear()
        accumulatedRotationDegrees = 0f
    }

    private fun shortestAngleDifference(from: Float, to: Float): Float {
        var diff = to - from
        while (diff < -180f) diff += 360f
        while (diff > 180f) diff -= 360f
        return diff
    }
}
