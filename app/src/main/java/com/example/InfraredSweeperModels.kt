package com.example

/**
 * State of the Front Infrared Biometric Camera Sweeper.
 */
enum class IrSensorState {
    DISCONNECTED,
    ENUMERATING,
    STREAMING,
    SECURE_HARDWARE_LOCKED,
    NOT_FOUND,
    ERROR
}

/**
 * Metadata descriptor for an enumerated IR or front optical camera.
 */
data class IrCameraDescriptor(
    val cameraId: String,
    val isPhysical: Boolean = false,
    val logicalParentId: String? = null,
    val isMonochrome: Boolean = false,
    val isNearInfraredCfa: Boolean = false,
    val isSecureLocked: Boolean = false,
    val lensFacingFront: Boolean = true,
    val supportedWidth: Int = 640,
    val supportedHeight: Int = 480,
    val hardwareLevel: String = "UNKNOWN"
)

/**
 * Discovered high-intensity IR bloom candidate representing active night-vision LED emitter.
 */
data class IrBloomTarget(
    val targetId: String,
    val xPixel: Float,
    val yPixel: Float,
    val normalizedX: Float,
    val normalizedY: Float,
    val radiusPixels: Float,
    val peakLuminance: Int,
    val averageLuminance: Float,
    val confidence: Float,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Live reactive telemetry state published by [InfraredSweeperEngine].
 */
data class IrSweeperTelemetry(
    val sensorState: IrSensorState = IrSensorState.DISCONNECTED,
    val activeCameraId: String? = null,
    val isPhysicalCamera: Boolean = false,
    val isMonochrome: Boolean = false,
    val isSecureLocked: Boolean = false,
    val frameFps: Float = 0.0f,
    val averageLuminance: Float = 0.0f,
    val peakLuminance: Int = 0,
    val bloomTargets: List<IrBloomTarget> = emptyList(),
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
    val processedFrames: Long = 0L,
    val thresholdLuminance: Int = 210,
    val errorMessage: String? = null
)

/**
 * Result sealed class for Camera2 IR enumeration.
 */
sealed class IrEnumerationResult {
    data class Success(val descriptor: IrCameraDescriptor) : IrEnumerationResult()
    data class FallbackOptical(val descriptor: IrCameraDescriptor, val reason: String) : IrEnumerationResult()
    data class SecureLocked(val cameraId: String, val reason: String) : IrEnumerationResult()
    data class NotFound(val reason: String) : IrEnumerationResult()
}
