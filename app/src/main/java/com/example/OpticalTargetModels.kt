package com.example

/**
 * Optical glint target representing a candidate retroreflective lens detected by strobe differencing.
 *
 * @property id Unique identifier for the persistent optical target track.
 * @property xPixel Horizontal center coordinate in pixel space of the sensor frame.
 * @property yPixel Vertical center coordinate in pixel space of the sensor frame.
 * @property normalizedX Normalized horizontal coordinate in [0.0, 1.0].
 * @property normalizedY Normalized vertical coordinate in [0.0, 1.0].
 * @property diameterPixels Estimated optical Airy disk / glint diameter in pixels.
 * @property circularityRatio Geometric circularity metric: (4 * PI * Area) / (Perimeter^2).
 * @property deltaLuminance Differential brightness intensity (|Frame_ON - Frame_OFF|) in range [0, 255].
 * @property confidence Estimated retroreflective confidence score in range [0.0, 1.0].
 * @property persistenceFrameCount Number of consecutive strobe cycles this target has persisted.
 * @property isTelephotoHandoffReady True if the target has persisted > 5 frames, qualifying for telephoto zoom handoff.
 * @property timestampMs System millisecond timestamp when last updated.
 */
data class OpticalGlintTarget(
    val id: String,
    val xPixel: Float,
    val yPixel: Float,
    val normalizedX: Float,
    val normalizedY: Float,
    val diameterPixels: Float,
    val circularityRatio: Float,
    val deltaLuminance: Int,
    val confidence: Float,
    val persistenceFrameCount: Int = 1,
    val isTelephotoHandoffReady: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * High-level state representing the active optical retroreflection and camera strobe subsystem.
 *
 * @property isScanning True if the camera strobe differencing loop is actively running.
 * @property activeCameraId Currently open physical/logical camera sensor ID.
 * @property isTelephotoLensActive True if the engine is currently streaming through the telephoto/periscope lens.
 * @property availableRearCameraIds List of available rear-facing physical camera IDs.
 * @property telephotoCameraId Specific physical camera ID of the rear telephoto/periscope lens, if available.
 * @property detectedTargets List of active candidate optical glint targets identified in the current view.
 * @property activeHandoffTarget Candidate target that has satisfied persistence (>5 frames) for optical zoom handoff.
 * @property processedFrameCount Total count of strobe frame pairs processed since scan inception.
 * @property flashStrobeActive Current state of the LED flash strobe in the alternating sequence.
 * @property lastDiffProcessingTimeMs Execution time in milliseconds for the latest differencing & morphological pass.
 */
data class OpticalTargetState(
    val isScanning: Boolean = false,
    val activeCameraId: String = "",
    val isTelephotoLensActive: Boolean = false,
    val availableRearCameraIds: List<String> = emptyList(),
    val telephotoCameraId: String? = null,
    val detectedTargets: List<OpticalGlintTarget> = emptyList(),
    val activeHandoffTarget: OpticalGlintTarget? = null,
    val processedFrameCount: Long = 0L,
    val flashStrobeActive: Boolean = false,
    val lastDiffProcessingTimeMs: Long = 0L
)
