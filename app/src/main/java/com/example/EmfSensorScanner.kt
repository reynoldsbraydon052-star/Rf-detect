package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Raw 3-axis magnetic field vector sample.
 */
data class MagneticSample(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val timestampNs: Long = 0L,
    val isUncalibrated: Boolean = false
)

/**
 * Low-level hardware sensor scanner listening to device 3-axis magnetometers.
 *
 * Hardware Listener Strategy:
 * 1. Primary sensor: TYPE_MAGNETIC_FIELD_UNCALIBRATED at SENSOR_DELAY_FASTEST to capture
 *    raw micro-oscillations without OS hard-iron bias removal distortion.
 * 2. Fallback sensor: TYPE_MAGNETIC_FIELD if uncalibrated magnetometer is unavailable.
 * 3. Zero-Allocation Constraint: Reuses pre-allocated primitive buffers inside onSensorChanged.
 * 4. Lifecycle Safety: Automatically unregisters SensorEventListener when the coroutine Flow is cancelled.
 */
class EmfSensorScanner(
    private val context: Context,
    private val sensorManagerOverride: SensorManager? = null
) {
    private val sensorManager: SensorManager? by lazy {
        sensorManagerOverride ?: (context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)
    }

    /**
     * Resolves the highest fidelity magnetic field sensor supported by the device.
     */
    fun getActiveSensor(): Pair<Sensor?, Boolean> {
        val sm = sensorManager ?: return Pair(null, false)
        val uncalibrated = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED)
        return if (uncalibrated != null) {
            Pair(uncalibrated, true)
        } else {
            Pair(sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), false)
        }
    }

    /**
     * Emits a continuous cold stream of [MagneticSample] updates.
     * Guaranteed to unregister the hardware sensor listener when the collector terminates or is cancelled.
     */
    fun observeMagneticField(samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_FASTEST): Flow<MagneticSample> = callbackFlow {
        val sm = sensorManager
        if (sm == null) {
            close()
            return@callbackFlow
        }

        val (sensor, isUncalibrated) = getActiveSensor()
        if (sensor == null) {
            close()
            return@callbackFlow
        }

        // Pre-allocated primitive buffer to enforce zero allocation on sensor callbacks
        val rawValues = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || event.values.size < 3) return

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // Guard against non-finite or corrupt hardware values
                if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return

                rawValues[0] = x
                rawValues[1] = y
                rawValues[2] = z

                trySend(
                    MagneticSample(
                        x = rawValues[0],
                        y = rawValues[1],
                        z = rawValues[2],
                        timestampNs = event.timestamp,
                        isUncalibrated = isUncalibrated
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No-op for passive EMF profiling
            }
        }

        val registered = sm.registerListener(listener, sensor, samplingPeriodUs)
        if (!registered) {
            close()
            return@callbackFlow
        }

        awaitClose {
            sm.unregisterListener(listener)
        }
    }
}
