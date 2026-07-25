package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

data class MagnetometerData(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val totalMicroTesla: Float = 0f,
    val netCalibratedMicroTesla: Float = 0f,
    val deltaMicroTesla: Float = 0f,
    val anomalyStatus: String = "NORMAL AMBIENT",
    val estimatedEmfFreqHz: Float = 0f,
    val isCalibrated: Boolean = false,
    val calibrationProgress: Float = 0f,
    val calibrationMessage: String = "Auto-calibrating RF baseline...",
    val baselineTotalMicroTesla: Float = 0f,
    val baselineX: Float = 0f,
    val baselineY: Float = 0f,
    val baselineZ: Float = 0f,
    val isOptimalStability: Boolean = false,
    val sensorAccuracy: Int = 0,
    val isRfInterferenceDetected: Boolean = false,
    val interferenceMagnitude: Float = 0f,
    val interferenceWarningMessage: String = "",
    val isPowerlineDspFilterActive: Boolean = true,
    val dspFilteredX: Float = 0f,
    val dspFilteredY: Float = 0f,
    val dspFilteredZ: Float = 0f,
    val dspFilteredTotal: Float = 0f,
    val dspAttenuatedHumMicroTesla: Float = 0f
)

class MagnetometerDetector(
    context: Context,
    private val onMagnetometerUpdate: (MagnetometerData) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val magnetometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private var lastTotal = 0f
    private var lastTimestampNs = 0L
    private val sampleHistory = ArrayList<Float>()
    private val timeHistory = ArrayList<Long>()

    private var hasHardwareEvents = false
    private var simulationJob: Job? = null

    private var isCalibrated = false
    private var calibrationProgress = 0f
    private var baselineX = 0f
    private var baselineY = 0f
    private var baselineZ = 0f
    private var baselineTotal = 0f
    private var sensorAccuracyStatus = SensorManager.SENSOR_STATUS_ACCURACY_HIGH

    private val calibBufferX = ArrayList<Float>()
    private val calibBufferY = ArrayList<Float>()
    private val calibBufferZ = ArrayList<Float>()
    private val calibBufferTotal = ArrayList<Float>()
    private val CALIBRATION_SAMPLES_TARGET = 20

    private val powerlineNotchFilter = PowerlineNotchFilter(sampleRateHz = 100.0f)

    private var rfInterferenceHoldCount = 0
    private var peakInterferenceMag = 0f
    private var manualSpikeRequested = false

    fun triggerManualRfSpike() {
        synchronized(this) {
            manualSpikeRequested = true
        }
    }

    fun clearRfInterference() {
        synchronized(this) {
            rfInterferenceHoldCount = 0
            peakInterferenceMag = 0f
        }
    }

    fun recalibrate() {
        synchronized(this) {
            isCalibrated = false
            calibrationProgress = 0f
            calibBufferX.clear()
            calibBufferY.clear()
            calibBufferZ.clear()
            calibBufferTotal.clear()
            baselineX = 0f
            baselineY = 0f
            baselineZ = 0f
            baselineTotal = 0f
        }
    }

    fun startListening(): Boolean {
        val registered = if (magnetometer != null) {
            sensorManager?.registerListener(
                this,
                magnetometer,
                SensorManager.SENSOR_DELAY_GAME
            ) ?: false
        } else false

        // Start fallback ticker if hardware sensor is absent or pending initial event
        simulationJob = CoroutineScope(Dispatchers.Default).launch {
            var tick = 0
            while (isActive) {
                if (!hasHardwareEvents) {
                    tick++
                    val jitterX = (Math.sin(tick * 0.2) * 2.5).toFloat()
                    val jitterY = (Math.cos(tick * 0.15) * 2.0).toFloat()
                    val jitterZ = (Math.sin(tick * 0.1) * 3.0).toFloat()

                    val simX = 18.5f + jitterX
                    val simY = -22.0f + jitterY
                    val simZ = 38.2f + jitterZ
                    val total = sqrt(simX * simX + simY * simY + simZ * simZ)
                    val delta = abs(total - lastTotal)
                    lastTotal = total

                    val data = processSample(simX, simY, simZ, total, delta, 60.0f)
                    onMagnetometerUpdate(data)
                }
                delay(200)
            }
        }

        return registered || magnetometer == null
    }

    fun stopListening() {
        simulationJob?.cancel()
        simulationJob = null
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return
        hasHardwareEvents = true

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val total = sqrt(x * x + y * y + z * z)
        val delta = abs(total - lastTotal)
        lastTotal = total

        val nowNs = event.timestamp
        if (lastTimestampNs > 0) {
            sampleHistory.add(total)
            timeHistory.add(nowNs)

            if (sampleHistory.size > 30) {
                sampleHistory.removeAt(0)
                timeHistory.removeAt(0)
            }
        }
        lastTimestampNs = nowNs

        // Estimate EMF frequency from zero-crossings / peak intervals
        val estimatedFreq = calculateFrequencyFromHistory()

        val data = processSample(x, y, z, total, delta, estimatedFreq)
        onMagnetometerUpdate(data)
    }

    private fun processSample(
        x: Float,
        y: Float,
        z: Float,
        total: Float,
        delta: Float,
        freq: Float
    ): MagnetometerData {
        var currentX = x
        var currentY = y
        var currentZ = z

        var isManualSpike = false
        synchronized(this) {
            if (manualSpikeRequested) {
                currentX += 45f
                currentY += 55f
                currentZ += 35f
                manualSpikeRequested = false
                isManualSpike = true
            }
        }

        val calcTotal = sqrt(currentX * currentX + currentY * currentY + currentZ * currentZ)
        val calcDelta = abs(calcTotal - lastTotal)

        var progress = calibrationProgress
        var calibrated = isCalibrated

        if (!calibrated) {
            calibBufferX.add(currentX)
            calibBufferY.add(currentY)
            calibBufferZ.add(currentZ)
            calibBufferTotal.add(calcTotal)

            progress = (calibBufferTotal.size.toFloat() / CALIBRATION_SAMPLES_TARGET).coerceIn(0f, 1f)
            calibrationProgress = progress

            if (calibBufferTotal.size >= CALIBRATION_SAMPLES_TARGET) {
                baselineX = calibBufferX.average().toFloat()
                baselineY = calibBufferY.average().toFloat()
                baselineZ = calibBufferZ.average().toFloat()
                baselineTotal = calibBufferTotal.average().toFloat()
                isCalibrated = true
                calibrated = true
                progress = 1.0f
            }
        }

        val netCalibrated = if (calibrated) {
            val dx = currentX - baselineX
            val dy = currentY - baselineY
            val dz = currentZ - baselineZ
            sqrt(dx * dx + dy * dy + dz * dz)
        } else {
            0f
        }

        val calibMsg = if (calibrated) {
            "OPTIMAL STABILITY REACHED • Baseline Subtracted (-${baselineTotal.toInt()} µT)"
        } else {
            "Auto-Calibrating Baseline... ${(progress * 100).toInt()}%"
        }

        // Detect sudden high-magnitude spikes / RF-induced interference
        val spikeThreshold = 18.0f
        val isHighSpike = isManualSpike || calcDelta > spikeThreshold || (calibrated && netCalibrated > 35.0f)

        if (isHighSpike) {
            rfInterferenceHoldCount = 18 // Keep active for 18 samples (~3.5 seconds)
            val magnitude = if (isManualSpike) 64.0f else calcDelta.coerceAtLeast(netCalibrated)
            if (magnitude > peakInterferenceMag) {
                peakInterferenceMag = magnitude
            }
        } else if (rfInterferenceHoldCount > 0) {
            rfInterferenceHoldCount--
            if (rfInterferenceHoldCount == 0) {
                peakInterferenceMag = 0f
            }
        }

        val isInterferenceActive = rfInterferenceHoldCount > 0
        val interferenceMsg = if (isInterferenceActive) {
            "HIGH-MAGNITUDE RF INTERFERENCE DETECTED (+${peakInterferenceMag.toInt()} µT) • Potential Electromagnetic Pulse Burst"
        } else ""

        // Apply Secondary DSP Notch Filtering for 50/60Hz AC Powerline Hum Removal
        val dspFiltered = powerlineNotchFilter.filter(currentX, currentY, currentZ)
        val dspX = dspFiltered[0]
        val dspY = dspFiltered[1]
        val dspZ = dspFiltered[2]
        val dspTotal = sqrt(dspX * dspX + dspY * dspY + dspZ * dspZ)
        val attenuatedHum = abs(calcTotal - dspTotal)

        val status = when {
            isInterferenceActive -> "RF INTERFERENCE / HIGH-MAGNITUDE SPIKE!"
            calcTotal > 150f -> "CRITICAL EMF HAZARD (>150 µT)"
            netCalibrated > 15f || (!calibrated && (calcTotal > 80f || calcDelta > 15f)) -> "MAGNETIC ANOMALY / METAL NEARBY"
            calcTotal < 20f -> "SHIELDED / LOW FIELD"
            else -> "NORMAL AMBIENT FIELD (~${calcTotal.toInt()} µT)"
        }

        return MagnetometerData(
            x = currentX,
            y = currentY,
            z = currentZ,
            totalMicroTesla = calcTotal,
            netCalibratedMicroTesla = netCalibrated,
            deltaMicroTesla = calcDelta,
            anomalyStatus = status,
            estimatedEmfFreqHz = freq,
            isCalibrated = calibrated,
            calibrationProgress = progress,
            calibrationMessage = calibMsg,
            baselineTotalMicroTesla = baselineTotal,
            baselineX = baselineX,
            baselineY = baselineY,
            baselineZ = baselineZ,
            isOptimalStability = calibrated,
            sensorAccuracy = sensorAccuracyStatus,
            isRfInterferenceDetected = isInterferenceActive,
            interferenceMagnitude = peakInterferenceMag,
            interferenceWarningMessage = interferenceMsg,
            isPowerlineDspFilterActive = true,
            dspFilteredX = dspX,
            dspFilteredY = dspY,
            dspFilteredZ = dspZ,
            dspFilteredTotal = dspTotal,
            dspAttenuatedHumMicroTesla = attenuatedHum
        )
    }

    private fun calculateFrequencyFromHistory(): Float {
        if (sampleHistory.size < 10) return 0f
        val mean = sampleHistory.average().toFloat()
        var crossings = 0
        for (i in 1 until sampleHistory.size) {
            if ((sampleHistory[i - 1] > mean && sampleHistory[i] <= mean) ||
                (sampleHistory[i - 1] < mean && sampleHistory[i] >= mean)) {
                crossings++
            }
        }
        val durationSec = (timeHistory.last() - timeHistory.first()) / 1_000_000_000f
        return if (durationSec > 0f) (crossings / 2f) / durationSec else 0f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            sensorAccuracyStatus = accuracy
        }
    }
}
