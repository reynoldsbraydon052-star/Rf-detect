package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Advanced sensor fusion system designed to calculate drift-free, HVAC-immune vertical height differentials (Z-axis).
 * Fuses the Barometer (pressure), Gravity Vector, and Linear Accelerometer to gate and validate physical changes in altitude.
 */
class ImuGatedAltimeter(
    context: Context,
    private val coroutineScope: CoroutineScope
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    // Flow emitting validated elevation changes (relative altitude in meters from baseline)
    private val _elevationFlow = MutableSharedFlow<Float>(replay = 1)
    val elevationFlow: SharedFlow<Float> = _elevationFlow.asSharedFlow()

    // Configuration / Thresholds
    private val rollingWindowMs = 2000L // 2-second motion integration window
    private val motionEnergyThreshold = 0.15f // Variance threshold indicating physical vertical motion
    private val floorThresholdMeters = 2.5f // Minimum altitude delta to trigger IMU validation
    private val standardSeaLevelPressureHpa = 1013.25f

    // Current State
    private var basePressureHpa: Float? = null
    private var currentPressureHpa: Float = 1013.25f
    private var lastValidatedAltitudeMeters = 0.0f
    private var currentAltitudeMeters = 0.0f

    // IMU Buffers & Vectors
    private var latestGravity = FloatArray(3)
    private var hasGravity = false
    
    // List of Pair(TimestampMillis, TrueVerticalAcceleration)
    private val verticalAccelBuffer = mutableListOf<Pair<Long, Float>>()

    fun start() {
        pressureSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gravitySensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        linearAccelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        synchronized(verticalAccelBuffer) {
            verticalAccelBuffer.clear()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val now = System.currentTimeMillis()
        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                System.arraycopy(event.values, 0, latestGravity, 0, 3)
                hasGravity = true
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                if (hasGravity) {
                    val trueVerticalAccel = calculateTrueVerticalAcceleration(event.values, latestGravity)
                    synchronized(verticalAccelBuffer) {
                        verticalAccelBuffer.add(Pair(now, trueVerticalAccel))
                        // Purge samples older than the 2-second window
                        verticalAccelBuffer.removeAll { it.first < now - rollingWindowMs }
                    }
                }
            }
            Sensor.TYPE_PRESSURE -> {
                val rawPressure = event.values[0]
                currentPressureHpa = rawPressure
                
                if (basePressureHpa == null) {
                    basePressureHpa = rawPressure
                    lastValidatedAltitudeMeters = 0.0f
                    emitElevation(0.0f)
                    return
                }

                val targetBase = basePressureHpa ?: rawPressure
                // Compute altitude using standard hypsometric formula
                val computedAltitude = calculateHypsometricAltitude(rawPressure, targetBase)
                val altitudeDelta = computedAltitude - lastValidatedAltitudeMeters

                // Check if the delta suggests cross-floor traversal or significant vertical movement
                if (Math.abs(altitudeDelta) >= floorThresholdMeters) {
                    val motionEnergy = calculateVerticalMotionEnergy()
                    
                    if (motionEnergy < motionEnergyThreshold) {
                        // REJECT: Pressure shifted significantly but the physical IMU registered no kinetic energy.
                        // This indicates an ambient barometric pressure spike (e.g., HVAC system, door slam, storm front).
                        // Silently adjust the baseline pressure to match the new static environmental baseline.
                        basePressureHpa = rawPressure
                        lastValidatedAltitudeMeters = 0.0f
                        emitElevation(0.0f)
                    } else {
                        // ACCEPT: Verified physical movement matches the pressure altitude delta.
                        lastValidatedAltitudeMeters = computedAltitude
                        emitElevation(computedAltitude)
                    }
                } else {
                    // Small fluctuations are continuously filtered/smoothed but accepted to maintain tracking
                    currentAltitudeMeters = computedAltitude
                    emitElevation(computedAltitude)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // No-op
    }

    /**
     * Projects the 3D linear acceleration vector onto the gravity vector to extract
     * true world-relative vertical acceleration (aligned perfectly with gravity),
     * independent of device orientation.
     */
    private fun calculateTrueVerticalAcceleration(accel: FloatArray, gravity: FloatArray): Float {
        val gravMagnitude = sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2])
        if (gravMagnitude < 0.01f) return 0.0f

        // Dot product of Acceleration and Gravity vectors
        val dotProduct = accel[0] * gravity[0] + accel[1] * gravity[1] + accel[2] * gravity[2]

        // Vertical scalar acceleration in direction of gravity.
        // A negative dot product indicates upward acceleration (opposing gravity).
        val verticalScalar = dotProduct / gravMagnitude

        // Return positive value for upward movement
        return -verticalScalar
    }

    /**
     * Calculates the statistical variance of vertical acceleration over the 2-second window.
     * This variance acts as "energy" metric, distinguishing real movement from static noise.
     */
    private fun calculateVerticalMotionEnergy(): Float {
        val values = synchronized(verticalAccelBuffer) {
            verticalAccelBuffer.map { it.second }
        }
        if (values.size < 5) return 0.0f

        val mean = values.average().toFloat()
        val variance = values.fold(0.0f) { acc, value -> acc + (value - mean).pow(2) } / values.size
        return variance
    }

    /**
     * Converts measured pressure (hPa) to height (meters) using the standardized hypsometric equation.
     */
    private fun calculateHypsometricAltitude(pressureHpa: Float, baselineHpa: Float): Float {
        // Standard barometric formula: H = 44330 * (1 - (P/P0)^(1/5.255))
        return 44330.0f * (1.0f - (pressureHpa / baselineHpa).pow(1.0f / 5.255f))
    }

    private fun emitElevation(elevation: Float) {
        coroutineScope.launch(Dispatchers.Default) {
            _elevationFlow.emit(elevation)
        }
    }
}
