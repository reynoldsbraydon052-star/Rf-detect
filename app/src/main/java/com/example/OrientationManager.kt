package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

/**
 * Tactical Orientation & Compass Heading Fusion Engine.
 * Features:
 * 1) Android Rotation Vector hardware fusion sensor priority (ultra-low noise, gyro-stabilized).
 * 2) Accelerometer + Magnetometer fallback with low-pass filter.
 * 3) Configurable stabilization factor (0.05 to 0.50).
 * 4) Jitter deadband rejection (< 0.25 deg).
 * 5) Shortest-path angle interpolation to eliminate 0/360 wrap glitching.
 */
class OrientationManager(
    context: Context,
    private val onHeadingChanged: (Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Sensors
    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // Fusion matrices
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var hasGravity = false
    private var hasGeomagnetic = false
    private var currentHeading = 0f
    private var lastEmittedTimestampMs = 0L

    // Configurable Stabilization Smoothing Level (0.05 = Super Smooth/Slow, 0.20 = Balanced Tactical, 0.50 = Raw Fast)
    var smoothingFactor: Float = 0.18f
    var isDeadbandFilterEnabled: Boolean = true

    fun startListening() {
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
            magnetometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                var azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                if (azimuthDeg < 0) azimuthDeg += 360f

                processStabilizedHeading(azimuthDeg)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                val alpha = 0.88f
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
                hasGravity = true
                tryProcessFallbackOrientation()
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                val alpha = 0.88f
                geomagnetic[0] = alpha * geomagnetic[0] + (1 - alpha) * event.values[0]
                geomagnetic[1] = alpha * geomagnetic[1] + (1 - alpha) * event.values[1]
                geomagnetic[2] = alpha * geomagnetic[2] + (1 - alpha) * event.values[2]
                hasGeomagnetic = true
                tryProcessFallbackOrientation()
            }
        }
    }

    private fun tryProcessFallbackOrientation() {
        if (hasGravity && hasGeomagnetic) {
            val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                var azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                if (azimuthDeg < 0) azimuthDeg += 360f
                processStabilizedHeading(azimuthDeg)
            }
        }
    }

    private fun processStabilizedHeading(rawHeadingDeg: Float) {
        // Compute shortest angular distance to prevent 359 -> 0 jumping
        var angleDiff = rawHeadingDeg - currentHeading
        while (angleDiff < -180f) angleDiff += 360f
        while (angleDiff > 180f) angleDiff -= 360f

        // Apply deadband filter to eliminate static resting jitter
        if (isDeadbandFilterEnabled && abs(angleDiff) < 0.25f) {
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastEmittedTimestampMs >= 30L) {
            lastEmittedTimestampMs = now
            val lerpFactor = smoothingFactor.coerceIn(0.04f, 0.65f)
            currentHeading = (currentHeading + angleDiff * lerpFactor + 360f) % 360f
            onHeadingChanged(currentHeading)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
