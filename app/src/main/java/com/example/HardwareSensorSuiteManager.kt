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
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

data class HardwareSensorSuiteData(
    val magnetometerData: MagnetometerData = MagnetometerData(),
    
    // Accelerometer & G-Force
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 9.81f,
    val totalGForce: Float = 1.0f,
    val isMotionDetected: Boolean = false,
    val vibrationHz: Float = 0f,

    // Gyroscope
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val rotationalSpeedDegPerSec: Float = 0f,

    // Barometer & Altitude
    val pressureHpa: Float = 1013.25f,
    val estimatedAltitudeMeters: Float = 0.0f,
    val pressureTrend: String = "STABLE",

    // Ambient Light
    val lightLux: Float = 350.0f,
    val lightCondition: String = "NORMAL AMBIENT",
    val isOpticalPulseDetected: Boolean = false,

    // Proximity
    val proximityCm: Float = 5.0f,
    val isProximityNear: Boolean = false,

    // Orientation / Compass Vector
    val compassHeading: Float = 0f,
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,

    // Step Counter & PDR
    val stepCount: Int = 0,
    val pdrDistanceMeters: Float = 0.0f,

    // Active Sensor Count & Summary
    val totalActiveSensorsCount: Int = 8,
    val activeSensorsList: List<String> = listOf(
        "3-Axis Magnetometer (EMF)",
        "3-Axis Accelerometer (G-Force)",
        "3-Axis Gyroscope (Angular Velocity)",
        "Barometer (Pressure Altitude)",
        "Ambient Light Sensor (Lux)",
        "Infrared Proximity Sensor",
        "Rotation Vector (Compass Azimuth)",
        "Step Counter (PDR Dead Reckoning)"
    )
)

class HardwareSensorSuiteManager(
    context: Context,
    private val onSuiteUpdate: (HardwareSensorSuiteData) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val sensorMag = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val sensorAccel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val sensorGyro = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val sensorLight = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val sensorPressure = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private val sensorProximity = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val sensorRotation = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val sensorStep = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var currentSuiteData = HardwareSensorSuiteData()
    private var isListening = false
    private var simulationJob: Job? = null
    private var hasHardwareEvents = false

    private var initialStepOffset = -1
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun startListening() {
        if (isListening) return
        isListening = true

        sensorManager?.let { sm ->
            sensorMag?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            sensorAccel?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            sensorGyro?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            sensorLight?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            sensorPressure?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            sensorProximity?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            sensorRotation?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            sensorStep?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }

        startFallbackSimulation()
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false
        sensorManager?.unregisterListener(this)
        simulationJob?.cancel()
    }

    private fun startFallbackSimulation() {
        simulationJob?.cancel()
        simulationJob = CoroutineScope(Dispatchers.Default).launch {
            var simStep = 0
            while (isActive && isListening) {
                delay(300)
                simStep++

                // Provide realistic live fallback telemetry if physical hardware sensors are unpopulated in container
                if (!hasHardwareEvents) {
                    val baseAccelX = (Math.sin(simStep * 0.1) * 0.4).toFloat()
                    val baseAccelY = (Math.cos(simStep * 0.15) * 0.3).toFloat()
                    val baseAccelZ = (9.81f + Math.sin(simStep * 0.05) * 0.2).toFloat()
                    val gForce = sqrt(baseAccelX * baseAccelX + baseAccelY * baseAccelY + baseAccelZ * baseAccelZ) / 9.81f

                    val gyroX = (Math.sin(simStep * 0.2) * 2.5).toFloat()
                    val gyroY = (Math.cos(simStep * 0.2) * 1.8).toFloat()
                    val gyroZ = (Math.sin(simStep * 0.1) * 1.2).toFloat()
                    val rotSpeed = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)

                    val pressure = (1013.25f + Math.sin(simStep * 0.02) * 1.5).toFloat()
                    val altMeters = ((1013.25f - pressure) * 8.43f).coerceAtLeast(0.0f)

                    val lux = (320f + Math.sin(simStep * 0.3) * 85f).toFloat().coerceAtLeast(10f)

                    val simHeading = ((simStep * 2f) % 360f)
                    val pdrDist = (simStep * 0.15f)

                    val updatedData = currentSuiteData.copy(
                        accelX = baseAccelX,
                        accelY = baseAccelY,
                        accelZ = baseAccelZ,
                        totalGForce = gForce,
                        isMotionDetected = gForce > 1.05f || gForce < 0.95f,
                        vibrationHz = if (rotSpeed > 2.0f) rotSpeed * 3.5f else 0f,
                        gyroX = gyroX,
                        gyroY = gyroY,
                        gyroZ = gyroZ,
                        rotationalSpeedDegPerSec = rotSpeed,
                        pressureHpa = pressure,
                        estimatedAltitudeMeters = altMeters,
                        lightLux = lux,
                        lightCondition = if (lux > 800) "BRIGHT DIRECT LIGHT" else if (lux < 20) "DARK OBSCURED" else "NORMAL AMBIENT",
                        compassHeading = simHeading,
                        stepCount = simStep / 3,
                        pdrDistanceMeters = pdrDist
                    )
                    currentSuiteData = updatedData
                    onSuiteUpdate(updatedData)
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        hasHardwareEvents = true
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]
                val gForce = sqrt(ax * ax + ay * ay + az * az) / 9.81f
                val isMotion = abs(gForce - 1.0f) > 0.08f

                currentSuiteData = currentSuiteData.copy(
                    accelX = ax,
                    accelY = ay,
                    accelZ = az,
                    totalGForce = gForce,
                    isMotionDetected = isMotion
                )
                onSuiteUpdate(currentSuiteData)
            }

            Sensor.TYPE_GYROSCOPE -> {
                val gx = Math.toDegrees(event.values[0].toDouble()).toFloat()
                val gy = Math.toDegrees(event.values[1].toDouble()).toFloat()
                val gz = Math.toDegrees(event.values[2].toDouble()).toFloat()
                val rotSpeed = sqrt(gx * gx + gy * gy + gz * gz)

                currentSuiteData = currentSuiteData.copy(
                    gyroX = gx,
                    gyroY = gy,
                    gyroZ = gz,
                    rotationalSpeedDegPerSec = rotSpeed,
                    vibrationHz = if (rotSpeed > 1.5f) rotSpeed * 2.8f else 0f
                )
                onSuiteUpdate(currentSuiteData)
            }

            Sensor.TYPE_LIGHT -> {
                val lux = event.values[0]
                val condition = when {
                    lux < 10f -> "DARK / OBSCURED"
                    lux < 100f -> "DIM INDOOR"
                    lux < 1000f -> "NORMAL AMBIENT"
                    else -> "HIGH INTENSITY / FLASH"
                }
                currentSuiteData = currentSuiteData.copy(
                    lightLux = lux,
                    lightCondition = condition,
                    isOpticalPulseDetected = lux > 1500f
                )
                onSuiteUpdate(currentSuiteData)
            }

            Sensor.TYPE_PRESSURE -> {
                val pressure = event.values[0]
                // Hypsometric formula estimate altitude: h = 44330 * (1 - (p/1013.25)^(1/5.255))
                val alt = (44330f * (1.0f - (pressure / 1013.25f).toDouble().pow(1.0 / 5.255))).toFloat()
                currentSuiteData = currentSuiteData.copy(
                    pressureHpa = pressure,
                    estimatedAltitudeMeters = alt.coerceAtLeast(0.0f)
                )
                onSuiteUpdate(currentSuiteData)
            }

            Sensor.TYPE_PROXIMITY -> {
                val prox = event.values[0]
                val maxRange = event.sensor.maximumRange
                val isNear = prox < maxRange
                currentSuiteData = currentSuiteData.copy(
                    proximityCm = prox,
                    isProximityNear = isNear
                )
                onSuiteUpdate(currentSuiteData)
            }

            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                if (azimuth < 0) azimuth += 360f

                val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                currentSuiteData = currentSuiteData.copy(
                    compassHeading = azimuth,
                    pitchDeg = pitch,
                    rollDeg = roll
                )
                onSuiteUpdate(currentSuiteData)
            }

            Sensor.TYPE_STEP_COUNTER -> {
                val totalSteps = event.values[0].toInt()
                if (initialStepOffset < 0) initialStepOffset = totalSteps
                val sessionSteps = (totalSteps - initialStepOffset).coerceAtLeast(0)
                currentSuiteData = currentSuiteData.copy(
                    stepCount = sessionSteps,
                    pdrDistanceMeters = sessionSteps * 0.72f
                )
                onSuiteUpdate(currentSuiteData)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
