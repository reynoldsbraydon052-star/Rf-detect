package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Optical Retroreflection & Active LED Strobe Telemetry Engine (Phase 5A).
 *
 * Implements:
 * 1. Hardware Manual Camera2 Controls:
 *    - Full bypass of Auto-Exposure (AE), Auto-White Balance (AWB), and Auto-Focus (AF).
 *    - Minimum ISO sensor sensitivity locking & fast sensor exposure time (1 ms).
 * 2. Synchronized Alternating Strobe Burst:
 *    - Strict interleaving of [FLASH_MODE_TORCH] (Frame_ON) and [FLASH_MODE_OFF] (Frame_OFF).
 * 3. Y-Plane Luminance Differencing:
 *    - Ingests YUV_420_888 frames, extracts only Plane 0 (Luminance 'Y'), and computes |Frame_ON - Frame_OFF|.
 * 4. Airy Disk Morphological Profiling:
 *    - Extracts candidates meeting strict circularity (>0.80) and lens diameter bounds (2.0 - 15.0 px).
 * 5. Temporal Tracking & Telephoto Handoff:
 *    - Tracks candidates across strobe cycles; triggers telephoto lens handoff recommendation after > 5 frames.
 */
class OpticalStrobeScanner(
    private val context: Context,
    val morphologicalFilter: OpticalMorphologicalFilter = OpticalMorphologicalFilter(),
    val tracker: OpticalTargetTracker = OpticalTargetTracker(),
    private val cameraManagerOverride: CameraManager? = null
) {
    companion object {
        private const val TAG = "OpticalStrobeScanner"
        private const val DEFAULT_FRAME_WIDTH = 640
        private const val DEFAULT_FRAME_HEIGHT = 480
        private const val MANUAL_EXPOSURE_TIME_NS = 1_000_000L // 1 ms exposure
        private const val DEFAULT_MIN_ISO = 100
    }

    private val cameraManager: CameraManager? by lazy {
        cameraManagerOverride ?: (context.getSystemService(Context.SENSOR_SERVICE) as? CameraManager
            ?: context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager)
    }

    private val _targetState = MutableStateFlow(OpticalTargetState())
    val targetState: StateFlow<OpticalTargetState> = _targetState.asStateFlow()

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    // Double buffering for Y-plane differencing (Zero-allocation)
    private var frameBufferOn: ByteArray? = null
    private var frameBufferOff: ByteArray? = null
    private var diffBuffer: ByteArray? = null
    private var currentBufferCapacity = 0

    private val isScanningActive = AtomicBoolean(false)
    private var nextFrameIsFlashOn = true
    private var primaryRearCameraId: String? = null
    private var telephotoCameraId: String? = null
    private var currentActiveCameraId: String = ""
    private var processedFrameCounter = 0L

    init {
        discoverCameras()
    }

    /**
     * Enumerates rear camera hardware sensors and discovers primary and telephoto/periscope lenses.
     */
    fun discoverCameras() {
        val cm = cameraManager ?: return
        try {
            val cameraIds = cm.cameraIdList
            val rearCameras = mutableListOf<String>()
            var maxFocalLength = 0.0f
            var primaryId: String? = null
            var teleId: String? = null

            for (id in cameraIds) {
                val chars = cm.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    rearCameras.add(id)
                    val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    val focal = focalLengths?.maxOrNull() ?: 0.0f

                    if (primaryId == null) {
                        primaryId = id
                    }

                    if (focal > maxFocalLength) {
                        maxFocalLength = focal
                        if (focal > 6.0f) { // Typical telephoto threshold
                            teleId = id
                        }
                    }
                }
            }

            primaryRearCameraId = primaryId
            telephotoCameraId = teleId
            _targetState.value = _targetState.value.copy(
                availableRearCameraIds = rearCameras,
                telephotoCameraId = teleId,
                activeCameraId = primaryId ?: ""
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Error enumerating camera characteristics: ${e.message}")
        }
    }

    /**
     * Configures a manual capture request disabling AE, AWB, and AF.
     */
    fun configureManualCaptureRequest(
        builder: CaptureRequest.Builder,
        characteristics: CameraCharacteristics,
        flashOn: Boolean
    ) {
        // Completely disable 3A (Auto-Exposure, Auto-White Balance, Auto-Focus)
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF)
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)

        // Lock ISO to sensor minimum
        val isoRange: Range<Int>? = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val minIso = isoRange?.lower ?: DEFAULT_MIN_ISO
        builder.set(CaptureRequest.SENSOR_SENSITIVITY, minIso)

        // Lock fast exposure time (1 ms)
        val exposureRange: Range<Long>? = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val exposureTime = if (exposureRange != null) {
            MANUAL_EXPOSURE_TIME_NS.coerceIn(exposureRange.lower, exposureRange.upper)
        } else {
            MANUAL_EXPOSURE_TIME_NS
        }
        builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime)

        // Alternating Strobe Torch Mode
        if (flashOn) {
            builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
        } else {
            builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
        }
    }

    /**
     * Starts the optical retroreflection scanning and strobe differencing loop.
     */
    @SuppressLint("MissingPermission")
    fun startScan(targetCameraId: String? = null) {
        if (isScanningActive.getAndSet(true)) return

        val cm = cameraManager ?: return
        val cameraIdToOpen = targetCameraId ?: primaryRearCameraId ?: return
        currentActiveCameraId = cameraIdToOpen

        startBackgroundThread()

        val width = DEFAULT_FRAME_WIDTH
        val height = DEFAULT_FRAME_HEIGHT
        val totalPixels = width * height

        ensureBuffers(totalPixels)

        imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 3).apply {
            setOnImageAvailableListener({ reader ->
                onImageReceived(reader)
            }, backgroundHandler)
        }

        try {
            cm.openCamera(cameraIdToOpen, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    startCaptureSession(camera, cameraIdToOpen)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                    isScanningActive.set(false)
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    isScanningActive.set(false)
                    Log.e(TAG, "Camera device error: $error")
                }
            }, backgroundHandler)

            _targetState.value = _targetState.value.copy(
                isScanning = true,
                activeCameraId = cameraIdToOpen,
                isTelephotoLensActive = cameraIdToOpen == telephotoCameraId
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start camera strobe scanner: ${e.message}")
            stopScan()
        }
    }

    private fun startCaptureSession(camera: CameraDevice, cameraId: String) {
        val reader = imageReader ?: return
        val cm = cameraManager ?: return

        try {
            val characteristics = cm.getCameraCharacteristics(cameraId)
            val surface = reader.surface

            camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    issueNextAlternatingCapture(session, camera, characteristics)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Capture session configuration failed")
                    stopScan()
                }
            }, backgroundHandler)
        } catch (e: Throwable) {
            Log.e(TAG, "Error starting capture session: ${e.message}")
        }
    }

    /**
     * Issues an alternating capture request (Flash Torch vs Flash Off).
     */
    private fun issueNextAlternatingCapture(
        session: CameraCaptureSession,
        camera: CameraDevice,
        characteristics: CameraCharacteristics
    ) {
        if (!isScanningActive.get()) return

        try {
            val surface = imageReader?.surface ?: return
            val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL).apply {
                addTarget(surface)
            }

            val flashState = nextFrameIsFlashOn
            nextFrameIsFlashOn = !nextFrameIsFlashOn

            configureManualCaptureRequest(requestBuilder, characteristics, flashOn = flashState)

            session.capture(requestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    // Queue the next consecutive frame with inverted flash state
                    issueNextAlternatingCapture(session, camera, characteristics)
                }
            }, backgroundHandler)

            _targetState.value = _targetState.value.copy(
                flashStrobeActive = flashState
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Error submitting alternating capture request: ${e.message}")
        }
    }

    /**
     * Processes Y-plane luminance buffers as they arrive from the Camera2 ImageReader.
     */
    private fun onImageReceived(reader: ImageReader) {
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
            val totalPixels = width * height

            ensureBuffers(totalPixels)

            val onBuf = frameBufferOn ?: return
            val offBuf = frameBufferOff ?: return
            val dBuf = diffBuffer ?: return

            val isFlashOnFrame = !nextFrameIsFlashOn // State of the frame just delivered

            if (isFlashOnFrame) {
                extractYPlaneToBuffer(buffer, onBuf, width, height, rowStride)
            } else {
                extractYPlaneToBuffer(buffer, offBuf, width, height, rowStride)

                // Execute Strobe Differencing pass when we have a full (ON, OFF) pair
                val startTimeMs = System.currentTimeMillis()
                morphologicalFilter.computeLuminanceDifference(onBuf, offBuf, dBuf, totalPixels)

                val candidates = morphologicalFilter.extractGlintCandidates(dBuf, width, height, width)
                val (updatedTargets, handoffTarget) = tracker.updateTracks(candidates, startTimeMs)

                val elapsedMs = System.currentTimeMillis() - startTimeMs
                processedFrameCounter++

                _targetState.value = _targetState.value.copy(
                    detectedTargets = updatedTargets,
                    activeHandoffTarget = handoffTarget,
                    processedFrameCount = processedFrameCounter,
                    lastDiffProcessingTimeMs = elapsedMs
                )
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error processing optical frame: ${e.message}")
        } finally {
            try {
                image?.close()
            } catch (_: Throwable) {}
        }
    }

    /**
     * Extracts Y plane luminance bytes into a pre-allocated primitive array.
     */
    fun extractYPlaneToBuffer(
        yBuffer: ByteBuffer,
        destination: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int
    ) {
        yBuffer.rewind()
        if (rowStride == width) {
            val bytesToRead = (width * height).coerceAtMost(destination.size).coerceAtMost(yBuffer.remaining())
            yBuffer.get(destination, 0, bytesToRead)
        } else {
            var destOffset = 0
            for (row in 0 until height) {
                val rowStart = row * rowStride
                if (rowStart >= yBuffer.limit()) break
                yBuffer.position(rowStart)
                val bytesInRow = width.coerceAtMost(destination.size - destOffset).coerceAtMost(yBuffer.remaining())
                yBuffer.get(destination, destOffset, bytesInRow)
                destOffset += bytesInRow
            }
        }
    }

    /**
     * Switches the active optical capture stream to the Telephoto/Periscope lens if available.
     */
    fun requestTelephotoLensSwitch(cameraId: String? = null) {
        val targetId = cameraId ?: telephotoCameraId ?: return
        if (targetId == currentActiveCameraId) return

        stopScan()
        startScan(targetId)
    }

    /**
     * Switches optical capture back to the primary wide rear lens.
     */
    fun switchBackToPrimaryLens() {
        val primaryId = primaryRearCameraId ?: return
        if (primaryId == currentActiveCameraId) return

        stopScan()
        startScan(primaryId)
    }

    /**
     * Directly processes synthetic or external Y-plane frame pairs (useful for offline evaluation and unit testing).
     */
    fun processFramePair(
        frameOn: ByteArray,
        frameOff: ByteArray,
        width: Int,
        height: Int,
        timestampMs: Long = System.currentTimeMillis()
    ): Pair<List<OpticalGlintTarget>, OpticalGlintTarget?> {
        val totalPixels = width * height
        ensureBuffers(totalPixels)

        val dBuf = diffBuffer ?: ByteArray(totalPixels).also { diffBuffer = it }
        morphologicalFilter.computeLuminanceDifference(frameOn, frameOff, dBuf, totalPixels)

        val candidates = morphologicalFilter.extractGlintCandidates(dBuf, width, height, width)
        val result = tracker.updateTracks(candidates, timestampMs)

        processedFrameCounter++
        _targetState.value = _targetState.value.copy(
            detectedTargets = result.first,
            activeHandoffTarget = result.second,
            processedFrameCount = processedFrameCounter
        )
        return result
    }

    private fun ensureBuffers(totalPixels: Int) {
        if (currentBufferCapacity < totalPixels) {
            frameBufferOn = ByteArray(totalPixels)
            frameBufferOff = ByteArray(totalPixels)
            diffBuffer = ByteArray(totalPixels)
            currentBufferCapacity = totalPixels
        }
    }

    /**
     * Halts capture session, releases camera handles, and tears down background handler threads.
     */
    fun stopScan() {
        isScanningActive.set(false)
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
        tracker.reset()

        _targetState.value = _targetState.value.copy(
            isScanning = false,
            detectedTargets = emptyList(),
            activeHandoffTarget = null,
            flashStrobeActive = false
        )
    }

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("CameraStrobeBackground").apply {
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
