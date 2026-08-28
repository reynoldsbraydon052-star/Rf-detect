package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Front Infrared Biometric Camera Sweeper Engine (Phase 5B).
 *
 * Implements:
 * 1. Biometric NIR Camera ID Enumeration:
 *    - Traverses logical front-facing cameras and their underlying physical camera IDs via [CameraCharacteristics.getPhysicalCameraIds].
 *    - Identifies physical sensors advertising [REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME] or non-Bayer NIR/MONO color filter arrangements.
 *    - Detects and reports secure hardware lockouts ([REQUEST_AVAILABLE_CAPABILITIES_SECURE_IMAGE_DATA]).
 * 2. Raw Stream Ingestion & Exposure Optimization:
 *    - Directs raw monochrome YUV frames into an [ImageReader].
 *    - Sets exposure parameters optimized for dark environments to maximize passive NIR sensitivity.
 * 3. High-Pass False-Color Pipeline:
 *    - Processes monochrome stream at 30 FPS, highlighting high-intensity active IR blooms in neon false-color.
 */
class InfraredSweeperEngine(
    private val context: Context,
    val processor: InfraredLuminanceProcessor = InfraredLuminanceProcessor(),
    private val cameraManagerOverride: CameraManager? = null
) {
    companion object {
        private const val TAG = "InfraredSweeperEngine"
        const val CAPABILITY_MONOCHROME = 12 // CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME
        const val CAPABILITY_SECURE_IMAGE_DATA = 13 // CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_SECURE_IMAGE_DATA
        const val CFA_NIR = 5 // SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_NIR
        const val CFA_MONO = 6 // SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_MONO

        private const val DEFAULT_STREAM_WIDTH = 640
        private const val DEFAULT_STREAM_HEIGHT = 480
        private const val TARGET_EXPOSURE_TIME_NS = 33_333_333L // ~33ms (30 FPS high-exposure in dark)
    }

    private val cameraManager: CameraManager? by lazy {
        cameraManagerOverride ?: (context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager)
    }

    private val _telemetry = MutableStateFlow(IrSweeperTelemetry())
    val telemetry: StateFlow<IrSweeperTelemetry> = _telemetry.asStateFlow()

    private val _latestFrame = MutableStateFlow<Bitmap?>(null)
    val latestFrame: StateFlow<Bitmap?> = _latestFrame.asStateFlow()

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    private var rawYBuffer: ByteArray? = null
    private var bufferCapacity = 0

    private val isStreamingActive = AtomicBoolean(false)
    private var activeDescriptor: IrCameraDescriptor? = null

    private var frameCounter = 0L
    private var lastFpsCalculationTimeMs = 0L
    private var framesInSecond = 0

    /**
     * Enumerates camera hardware to locate front biometric IR / monochrome camera sensors.
     */
    fun enumerateBiometricIrSensors(): IrEnumerationResult {
        val cm = cameraManager ?: return IrEnumerationResult.NotFound("CameraManager not available")
        _telemetry.value = _telemetry.value.copy(sensorState = IrSensorState.ENUMERATING)

        try {
            val cameraIds = cm.cameraIdList
            if (cameraIds.isEmpty()) {
                val failure = IrEnumerationResult.NotFound("No camera sensors found on device")
                _telemetry.value = _telemetry.value.copy(
                    sensorState = IrSensorState.NOT_FOUND,
                    errorMessage = failure.reason
                )
                return failure
            }

            var bestPhysicalIrDescriptor: IrCameraDescriptor? = null
            var fallbackFrontDescriptor: IrCameraDescriptor? = null

            for (logicalId in cameraIds) {
                val chars = cm.getCameraCharacteristics(logicalId)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                val isFront = facing == CameraCharacteristics.LENS_FACING_FRONT

                if (!isFront) continue

                // Check logical camera capabilities
                val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
                val isSecureLocked = capabilities.contains(CAPABILITY_SECURE_IMAGE_DATA)
                val isMonochrome = capabilities.contains(CAPABILITY_MONOCHROME)

                val cfa = chars.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT)
                val isNirCfa = cfa == CFA_NIR || cfa == CFA_MONO

                if (fallbackFrontDescriptor == null) {
                    fallbackFrontDescriptor = IrCameraDescriptor(
                        cameraId = logicalId,
                        isPhysical = false,
                        logicalParentId = null,
                        isMonochrome = isMonochrome,
                        isNearInfraredCfa = isNirCfa,
                        isSecureLocked = isSecureLocked,
                        lensFacingFront = true
                    )
                }

                if (isSecureLocked) {
                    Log.w(TAG, "Camera $logicalId is locked behind Secure Image Data hardware isolation")
                }

                // Inspect underlying physical camera IDs for multi-camera systems
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val physicalIds = chars.physicalCameraIds
                    for (physId in physicalIds) {
                        try {
                            val physChars = cm.getCameraCharacteristics(physId)
                            val physCaps = physChars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
                            val physSecure = physCaps.contains(CAPABILITY_SECURE_IMAGE_DATA)
                            val physMono = physCaps.contains(CAPABILITY_MONOCHROME)
                            val physCfa = physChars.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT)
                            val physNirCfa = physCfa == CFA_NIR || physCfa == CFA_MONO

                            if (physMono || physNirCfa) {
                                if (physSecure) {
                                    return IrEnumerationResult.SecureLocked(
                                        cameraId = physId,
                                        reason = "Physical IR sensor ($physId) is secured by OEM hardware enclave (Face Unlock/Secure Image Data)"
                                    )
                                }
                                bestPhysicalIrDescriptor = IrCameraDescriptor(
                                    cameraId = physId,
                                    isPhysical = true,
                                    logicalParentId = logicalId,
                                    isMonochrome = physMono,
                                    isNearInfraredCfa = physNirCfa,
                                    isSecureLocked = false,
                                    lensFacingFront = true
                                )
                                break
                            }
                        } catch (e: Throwable) {
                            Log.w(TAG, "Could not inspect physical camera $physId: ${e.message}")
                        }
                    }
                }

                if (bestPhysicalIrDescriptor != null) break
            }

            // Return best match or fallback
            return when {
                bestPhysicalIrDescriptor != null -> {
                    activeDescriptor = bestPhysicalIrDescriptor
                    _telemetry.value = _telemetry.value.copy(
                        sensorState = IrSensorState.DISCONNECTED,
                        activeCameraId = bestPhysicalIrDescriptor.cameraId,
                        isPhysicalCamera = true,
                        isMonochrome = bestPhysicalIrDescriptor.isMonochrome,
                        isSecureLocked = false
                    )
                    IrEnumerationResult.Success(bestPhysicalIrDescriptor)
                }
                fallbackFrontDescriptor != null -> {
                    if (fallbackFrontDescriptor.isSecureLocked) {
                        _telemetry.value = _telemetry.value.copy(
                            sensorState = IrSensorState.SECURE_HARDWARE_LOCKED,
                            activeCameraId = fallbackFrontDescriptor.cameraId,
                            isSecureLocked = true,
                            errorMessage = "Front IR sensor is locked in hardware secure enclave"
                        )
                        IrEnumerationResult.SecureLocked(
                            cameraId = fallbackFrontDescriptor.cameraId,
                            reason = "Front sensor strict-locked behind REQUEST_AVAILABLE_CAPABILITIES_SECURE_IMAGE_DATA"
                        )
                    } else {
                        activeDescriptor = fallbackFrontDescriptor
                        _telemetry.value = _telemetry.value.copy(
                            sensorState = IrSensorState.DISCONNECTED,
                            activeCameraId = fallbackFrontDescriptor.cameraId,
                            isPhysicalCamera = false,
                            isMonochrome = fallbackFrontDescriptor.isMonochrome,
                            isSecureLocked = false
                        )
                        IrEnumerationResult.FallbackOptical(
                            descriptor = fallbackFrontDescriptor,
                            reason = "Biometric IR physical ID not directly exposed; utilizing front optical sensor with high-pass NIR filter"
                        )
                    }
                }
                else -> {
                    val notFound = IrEnumerationResult.NotFound("No front-facing camera sensors available")
                    _telemetry.value = _telemetry.value.copy(
                        sensorState = IrSensorState.NOT_FOUND,
                        errorMessage = notFound.reason
                    )
                    notFound
                }
            }
        } catch (e: Throwable) {
            val err = "Camera enumeration failed: ${e.message}"
            _telemetry.value = _telemetry.value.copy(sensorState = IrSensorState.ERROR, errorMessage = err)
            return IrEnumerationResult.NotFound(err)
        }
    }

    /**
     * Starts passive IR stream ingestion and false-color analysis.
     */
    @SuppressLint("MissingPermission")
    fun startSweeper(targetDescriptor: IrCameraDescriptor? = null) {
        if (isStreamingActive.getAndSet(true)) return

        val descriptor = targetDescriptor ?: activeDescriptor ?: when (val res = enumerateBiometricIrSensors()) {
            is IrEnumerationResult.Success -> res.descriptor
            is IrEnumerationResult.FallbackOptical -> res.descriptor
            is IrEnumerationResult.SecureLocked -> {
                _telemetry.value = _telemetry.value.copy(
                    sensorState = IrSensorState.SECURE_HARDWARE_LOCKED,
                    isSecureLocked = true,
                    errorMessage = res.reason
                )
                isStreamingActive.set(false)
                return
            }
            is IrEnumerationResult.NotFound -> {
                _telemetry.value = _telemetry.value.copy(
                    sensorState = IrSensorState.NOT_FOUND,
                    errorMessage = res.reason
                )
                isStreamingActive.set(false)
                return
            }
        }

        activeDescriptor = descriptor
        val cm = cameraManager ?: return

        startBackgroundThread()

        val width = DEFAULT_STREAM_WIDTH
        val height = DEFAULT_STREAM_HEIGHT
        val total = width * height
        ensureBuffer(total)

        imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 3).apply {
            setOnImageAvailableListener({ reader ->
                onImageAvailable(reader)
            }, backgroundHandler)
        }

        try {
            cm.openCamera(descriptor.cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    startCaptureSession(camera, descriptor.cameraId)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                    isStreamingActive.set(false)
                    _telemetry.value = _telemetry.value.copy(sensorState = IrSensorState.DISCONNECTED)
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    isStreamingActive.set(false)
                    _telemetry.value = _telemetry.value.copy(
                        sensorState = IrSensorState.ERROR,
                        errorMessage = "Camera error code: $error"
                    )
                }
            }, backgroundHandler)

            _telemetry.value = _telemetry.value.copy(
                sensorState = IrSensorState.STREAMING,
                activeCameraId = descriptor.cameraId,
                isPhysicalCamera = descriptor.isPhysical,
                isMonochrome = descriptor.isMonochrome,
                frameWidth = width,
                frameHeight = height
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to open camera: ${e.message}")
            stopSweeper()
            _telemetry.value = _telemetry.value.copy(
                sensorState = IrSensorState.ERROR,
                errorMessage = e.message
            )
        }
    }

    private fun startCaptureSession(camera: CameraDevice, cameraId: String) {
        val reader = imageReader ?: return
        val cm = cameraManager ?: return

        try {
            val chars = cm.getCameraCharacteristics(cameraId)
            val surface = reader.surface

            camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                        addTarget(surface)
                    }

                    configureHighSensitivityExposure(requestBuilder, chars)

                    session.setRepeatingRequest(requestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult
                        ) {
                            updateFpsTelemetry()
                        }
                    }, backgroundHandler)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Camera capture session failed to configure")
                    stopSweeper()
                }
            }, backgroundHandler)
        } catch (e: Throwable) {
            Log.e(TAG, "Error starting IR capture session: ${e.message}")
        }
    }

    /**
     * Configures exposure parameters favoring dark scenes to maximize passive NIR sensor response.
     */
    fun configureHighSensitivityExposure(builder: CaptureRequest.Builder, chars: CameraCharacteristics) {
        // Favor high analog sensor gain (ISO)
        val isoRange: Range<Int>? = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        if (isoRange != null) {
            // Target 80% of max ISO to maximize sensitivity without excessive thermal noise
            val targetIso = (isoRange.upper * 0.8f).toInt().coerceIn(isoRange.lower, isoRange.upper)
            builder.set(CaptureRequest.SENSOR_SENSITIVITY, targetIso)
        }

        // Favor longer integration time (exposure ~33ms for dark environment illuminator pickup)
        val expRange: Range<Long>? = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        if (expRange != null) {
            val expTime = TARGET_EXPOSURE_TIME_NS.coerceIn(expRange.lower, expRange.upper)
            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expTime)
        }

        // Set exposure compensation to maximum if AE is on
        val aeCompRange = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
        if (aeCompRange != null && aeCompRange.upper > 0) {
            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, aeCompRange.upper)
        }
    }

    private fun onImageAvailable(reader: ImageReader) {
        var image: Image? = null
        try {
            image = reader.acquireLatestImage() ?: return
            val planes = image.planes
            if (planes.isEmpty()) return

            val yPlane = planes[0]
            val buffer = yPlane.buffer ?: return
            val rowStride = yPlane.rowStride
            val width = image.width
            val height = image.height
            val total = width * height

            ensureBuffer(total)
            val destBuf = rawYBuffer ?: return

            extractLuminanceBuffer(buffer, destBuf, width, height, rowStride)

            // High-pass false-color processing
            val (bitmap, bloomTargets) = processor.processMonochromeFrame(destBuf, width, height, width)

            frameCounter++
            _latestFrame.value = bitmap

            val avgLum = if (bloomTargets.isNotEmpty()) bloomTargets.map { it.averageLuminance }.average().toFloat() else 0f
            val maxLum = if (bloomTargets.isNotEmpty()) bloomTargets.maxOf { it.peakLuminance } else 0

            _telemetry.value = _telemetry.value.copy(
                bloomTargets = bloomTargets,
                averageLuminance = avgLum,
                peakLuminance = maxLum,
                processedFrames = frameCounter,
                thresholdLuminance = processor.luminanceThreshold
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Error ingesting IR frame: ${e.message}")
        } finally {
            try {
                image?.close()
            } catch (_: Throwable) {}
        }
    }

    /**
     * Extracts luminance/NIR byte plane into continuous destination array.
     */
    fun extractLuminanceBuffer(
        buffer: ByteBuffer,
        destination: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int
    ) {
        buffer.rewind()
        if (rowStride == width) {
            val bytes = (width * height).coerceAtMost(destination.size).coerceAtMost(buffer.remaining())
            buffer.get(destination, 0, bytes)
        } else {
            var destOffset = 0
            for (r in 0 until height) {
                val rowStart = r * rowStride
                if (rowStart >= buffer.limit()) break
                buffer.position(rowStart)
                val bytesInRow = width.coerceAtMost(destination.size - destOffset).coerceAtMost(buffer.remaining())
                buffer.get(destination, destOffset, bytesInRow)
                destOffset += bytesInRow
            }
        }
    }

    /**
     * Directly processes synthetic or external monochrome frame (useful for unit testing).
     */
    fun processFrameDirectly(
        monochromeData: ByteArray,
        width: Int,
        height: Int
    ): Pair<Bitmap, List<IrBloomTarget>> {
        val result = processor.processMonochromeFrame(monochromeData, width, height, width)
        _latestFrame.value = result.first
        frameCounter++
        _telemetry.value = _telemetry.value.copy(
            bloomTargets = result.second,
            processedFrames = frameCounter,
            peakLuminance = if (result.second.isNotEmpty()) result.second.maxOf { it.peakLuminance } else 0,
            thresholdLuminance = processor.luminanceThreshold
        )
        return result
    }

    private fun updateFpsTelemetry() {
        framesInSecond++
        val now = System.currentTimeMillis()
        val delta = now - lastFpsCalculationTimeMs
        if (delta >= 1000L) {
            val fps = (framesInSecond * 1000.0f) / delta.toFloat()
            framesInSecond = 0
            lastFpsCalculationTimeMs = now
            _telemetry.value = _telemetry.value.copy(frameFps = fps)
        }
    }

    private fun ensureBuffer(total: Int) {
        if (bufferCapacity < total) {
            rawYBuffer = ByteArray(total)
            bufferCapacity = total
        }
    }

    /**
     * Stops camera stream and frees resources.
     */
    fun stopSweeper() {
        isStreamingActive.set(false)
        try {
            captureSession?.close()
            captureSession = null
        } catch (_: Throwable) {}

        try {
            cameraDevice?.close()
            cameraDevice = null
        } catch (_: Throwable) {}

        try {
            imageReader?.close()
            imageReader = null
        } catch (_: Throwable) {}

        stopBackgroundThread()
        processor.release()

        _telemetry.value = _telemetry.value.copy(
            sensorState = IrSensorState.DISCONNECTED,
            bloomTargets = emptyList(),
            frameFps = 0.0f
        )
    }

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("InfraredSweeperBg").apply {
                start()
                backgroundHandler = Handler(looper)
            }
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join(500)
        } catch (_: Throwable) {}
        backgroundThread = null
        backgroundHandler = null
    }
}
