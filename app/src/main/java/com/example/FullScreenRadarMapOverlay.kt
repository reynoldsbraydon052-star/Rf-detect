package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.OrientationEventListener
import android.view.Surface
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.window.DialogProperties
import kotlin.math.cos
import kotlin.math.sin

data class ArTargetProjection(
    val id: String,
    val name: String,
    val distanceMeters: Float,
    val rssiDbm: Int,
    val type: String,
    val screenX: Float,
    val screenY: Float,
    val depthRatio: Float,
    val isVisibleInFov: Boolean,
    val relativeAngleDegrees: Float,
    val estimatedElevationMeters: Float,
    val verticalAngleDegrees: Float
)

sealed class ArObjectDetails {
    data class Source(
        val blip: RadarBlip,
        val distance: Float,
        val bearing: Float,
        val confidence: Float,
        val fingerprint: String
    ) : ArObjectDetails()

    data class Measurement(
        val rssi: Int,
        val quality: Int,
        val x: Float,
        val y: Float,
        val z: Float,
        val timestamp: Long
    ) : ArObjectDetails()

    data class Uncertainty(
        val valueMeters: Float,
        val confidence: Float,
        val supportingCount: Int,
        val contradictoryCount: Int
    ) : ArObjectDetails()

    data class GuidanceDetails(
        val moveDirection: Float,
        val expectedGain: Int,
        val rationale: String
    ) : ArObjectDetails()
}

object UwbArTracker {
    fun estimateTargetElevation(blip: RadarBlip): Float {
        val hashOffset = ((blip.id.hashCode() and 0x7FFFFFFF) % 20) / 10f - 1.0f
        return when (blip.type.uppercase()) {
            "WIFI" -> 1.8f + hashOffset
            "BLE" -> -0.4f + hashOffset
            "CELLULAR" -> 3.2f + hashOffset
            "MAGNETIC" -> -1.0f + hashOffset
            "AUDIO" -> 0.2f + hashOffset
            else -> 0.0f + hashOffset
        }
    }

    fun updateTargetPosition(
        blip: RadarBlip,
        headingDegrees: Float,
        pitchDegrees: Float,
        rollDegrees: Float,
        mapRangeMeters: Float,
        containerWidth: Float,
        containerHeight: Float,
        fovDegrees: Float = 60f,
        rotationDegrees: Int = 0
    ): ArTargetProjection {
        var rawRelAngle = (blip.targetAngleOffset - headingDegrees + 360f) % 360f
        if (rawRelAngle > 180f) rawRelAngle -= 360f

        val halfFov = fovDegrees / 2f
        val isVisibleHorizontally = kotlin.math.abs(rawRelAngle) <= halfFov

        val xFraction = 0.5f + (rawRelAngle / fovDegrees)
        val baseScreenX = (xFraction * containerWidth).coerceIn(-containerWidth * 0.5f, containerWidth * 1.5f)

        val effectiveRange = mapRangeMeters.coerceAtLeast(1.0f)
        val depthRatio = (blip.distance / effectiveRange).coerceIn(0.05f, 3.0f)

        val elevationMeters = estimateTargetElevation(blip)
        val targetVerticalAngleRad = kotlin.math.atan2(
            elevationMeters.toDouble(),
            blip.distance.coerceAtLeast(0.5f).toDouble()
        )
        val targetVerticalAngleDeg = Math.toDegrees(targetVerticalAngleRad).toFloat()

        val fovVerticalDegrees = fovDegrees * (containerHeight / containerWidth.coerceAtLeast(1f))
        val totalVerticalAngleDelta = targetVerticalAngleDeg + pitchDegrees
        val yFraction = 0.5f - (totalVerticalAngleDelta / fovVerticalDegrees)
        val baseScreenY = yFraction * containerHeight

        // Rotate coordinate projection relative to screen center based on screen orientation
        val cx = containerWidth / 2f
        val cy = containerHeight / 2f
        val dx = baseScreenX - cx
        val dy = baseScreenY - cy

        val rad = Math.toRadians(-rotationDegrees.toDouble())
        val cosR = kotlin.math.cos(rad).toFloat()
        val sinR = kotlin.math.sin(rad).toFloat()

        val finalScreenX = cx + (dx * cosR - dy * sinR)
        val finalScreenY = cy + (dx * sinR + dy * cosR)

        return ArTargetProjection(
            id = blip.id,
            name = blip.name,
            distanceMeters = blip.distance,
            rssiDbm = blip.rssi,
            type = blip.type,
            screenX = finalScreenX,
            screenY = finalScreenY,
            depthRatio = depthRatio,
            isVisibleInFov = isVisibleHorizontally && finalScreenY >= -containerHeight * 0.2f && finalScreenY <= containerHeight * 1.2f,
            relativeAngleDegrees = rawRelAngle,
            estimatedElevationMeters = elevationMeters,
            verticalAngleDegrees = targetVerticalAngleDeg
        )
    }
}

@Composable
fun SpectrumWaterfallCanvas(
    blips: List<RadarBlip>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color(0xFF030805))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val gridCols = 8
            for (i in 0..gridCols) {
                val x = w * (i / gridCols.toFloat())
                drawLine(
                    color = Color(0xFF00FF66).copy(alpha = 0.15f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
            }

            val timeRows = 10
            for (j in 0..timeRows) {
                val y = h * (j / timeRows.toFloat())
                drawLine(
                    color = Color(0xFF00FF66).copy(alpha = 0.1f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            blips.forEach { blip ->
                val freq = if (blip.frequencyMhz > 0) blip.frequencyMhz.toFloat() else 2442f
                val normFreq = (freq / 6000f).coerceIn(0.02f, 0.98f)
                val blipX = normFreq * w

                val signalColor = when (blip.type) {
                    "WIFI" -> Color(0xFF00FF66)
                    "BLE" -> Color(0xFF00E5FF)
                    "CELLULAR" -> Color(0xFFFF3366)
                    "MAGNETIC" -> Color(0xFFFFCC00)
                    else -> Color(0xFFFF9900)
                }

                for (row in 0..8) {
                    val y = h * (row / 8f)
                    val alpha = (1.0f - (row / 8f) * 0.85f).coerceIn(0.1f, 1.0f)
                    val bandwidthPx = (24f + (kotlin.math.abs(blip.rssi) / 100f) * 35f) / (row * 0.2f + 1f)

                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                signalColor.copy(alpha = 0f),
                                signalColor.copy(alpha = alpha * 0.75f),
                                signalColor.copy(alpha = 0f)
                            ),
                            startX = (blipX - bandwidthPx).coerceAtLeast(0f),
                            endX = (blipX + bandwidthPx).coerceAtMost(w)
                        ),
                        topLeft = Offset((blipX - bandwidthPx).coerceAtLeast(0f), y),
                        size = androidx.compose.ui.geometry.Size(bandwidthPx * 2f, h / 8f)
                    )
                }

                drawCircle(
                    color = signalColor,
                    radius = 5f,
                    center = Offset(blipX, 10f)
                )
            }
        }
    }
}

@Composable
fun SatelliteElevationGridCanvas(
    blips: List<RadarBlip>,
    gnssSatellites: List<GnssSatelliteMetric> = emptyList(),
    headingDegrees: Float,
    modifier: Modifier = Modifier
) {
    val satellites = if (gnssSatellites.isNotEmpty()) gnssSatellites else listOf(
        GnssSatelliteMetric(12, "GPS Dual L1/L5", 1575420000L, "L1", 42.5f, 135f, 62f),
        GnssSatelliteMetric(24, "GPS Dual L1/L5", 1176450000L, "L5", 39.8f, 140f, 60f),
        GnssSatelliteMetric(7, "GALILEO E1/E5a", 1575420000L, "E1", 41.2f, 210f, 45f),
        GnssSatelliteMetric(19, "GLONASS L1", 1602000000L, "L1", 38.0f, 45f, 30f)
    )

    Box(
        modifier = modifier.background(Color(0xFF020704))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val centerY = h / 2f
            val maxRadius = minOf(w, h) * 0.42f

            val elevationSteps = listOf("90° (Zenith)" to 0.15f, "60°" to 0.4f, "30°" to 0.7f, "0° (Horizon)" to 1.0f)
            elevationSteps.forEach { (_, ratio) ->
                val r = maxRadius * ratio
                drawCircle(
                    color = Color(0xFF00FF66).copy(alpha = if (ratio == 1.0f) 0.5f else 0.2f),
                    radius = r,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = if (ratio == 1.0f) 2f else 1f)
                )
            }

            for (angle in 0 until 360 step 45) {
                val trueAngleRad = Math.toRadians((angle - headingDegrees).toDouble())
                val endX = centerX + (maxRadius * sin(trueAngleRad)).toFloat()
                val endY = centerY - (maxRadius * cos(trueAngleRad)).toFloat()
                drawLine(
                    color = Color(0xFF00FF66).copy(alpha = 0.25f),
                    start = Offset(centerX, centerY),
                    end = Offset(endX, endY),
                    strokeWidth = 1f
                )
            }

            satellites.forEach { sat ->
                val elevRatio = (1.0f - (sat.elevationDegrees / 90f).coerceIn(0f, 1f))
                val r = maxRadius * elevRatio
                val azRad = Math.toRadians((sat.azimuthDegrees - headingDegrees).toDouble())
                val sx = centerX + (r * sin(azRad)).toFloat()
                val sy = centerY - (r * cos(azRad)).toFloat()

                drawCircle(
                    color = Color(0xFF00FF66),
                    radius = 8f,
                    center = Offset(sx, sy)
                )
                drawCircle(
                    color = Color(0xFF00FF66).copy(alpha = 0.3f),
                    radius = 14f,
                    center = Offset(sx, sy),
                    style = Stroke(width = 1.5f)
                )

                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GREEN
                    textSize = 20f
                    typeface = android.graphics.Typeface.MONOSPACE
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "PRN${sat.svid} (${sat.constellationType})",
                    sx + 10f,
                    sy + 6f,
                    paint
                )
            }

            blips.forEach { blip ->
                val azRad = Math.toRadians((blip.targetAngleOffset - headingDegrees).toDouble())
                val normDist = (blip.distance / 30f).coerceIn(0.1f, 1.0f)
                val r = maxRadius * normDist
                val bx = centerX + (r * sin(azRad)).toFloat()
                val by = centerY - (r * cos(azRad)).toFloat()

                val color = if (blip.type == "WIFI") Color(0xFF00FF66) else Color(0xFF00E5FF)
                drawCircle(color = color, radius = 6f, center = Offset(bx, by))
            }
        }
    }
}

@Composable
fun FullScreenRadarScreen(
    uiState: SignalRadarUiState,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onSetMapRange: (Float) -> Unit,
    onSelectTargetDevice: (String?) -> Unit,
    onToggleAudioSonar: () -> Unit,
    onSetFullScreenMapMode: (String) -> Unit,
    onSetMaxDevices: (Int) -> Unit = {},
    onToggleFocusMode: () -> Unit = {},
    onTriggerAiPinpoint: (RadarBlip) -> Unit = {},
    onCycleRadarBoost: () -> Unit = {},
    onRunAiDeepAudit: () -> Unit = {}
) {
    FullScreenRadarContent(
        uiState = uiState,
        onDismiss = null,
        onZoomIn = onZoomIn,
        onZoomOut = onZoomOut,
        onSetMapRange = onSetMapRange,
        onSelectTargetDevice = onSelectTargetDevice,
        onToggleAudioSonar = onToggleAudioSonar,
        onSetFullScreenMapMode = onSetFullScreenMapMode,
        onSetMaxDevices = onSetMaxDevices,
        onToggleFocusMode = onToggleFocusMode,
        onTriggerAiPinpoint = onTriggerAiPinpoint,
        onCycleRadarBoost = onCycleRadarBoost,
        onRunAiDeepAudit = onRunAiDeepAudit
    )
}

@Composable
fun FullScreenRadarMapOverlay(
    uiState: SignalRadarUiState,
    anomalies: List<RfAnomalyEntity> = emptyList(),
    onDismiss: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onSetMapRange: (Float) -> Unit,
    onSelectTargetDevice: (String?) -> Unit,
    onToggleAudioSonar: () -> Unit,
    onSetFullScreenMapMode: (String) -> Unit,
    onSetMaxDevices: (Int) -> Unit = {},
    onToggleFocusMode: () -> Unit = {},
    onTriggerAiPinpoint: (RadarBlip) -> Unit = {},
    onCycleRadarBoost: () -> Unit = {},
    onRunAiDeepAudit: () -> Unit = {},
    onAddSpatialPoint: (String, RfMeasurementPoint) -> Unit = { _, _ -> }
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        FullScreenRadarContent(
            uiState = uiState,
            onDismiss = onDismiss,
            onZoomIn = onZoomIn,
            onZoomOut = onZoomOut,
            onSetMapRange = onSetMapRange,
            onSelectTargetDevice = onSelectTargetDevice,
            onToggleAudioSonar = onToggleAudioSonar,
            onSetFullScreenMapMode = onSetFullScreenMapMode,
            onSetMaxDevices = onSetMaxDevices,
            onToggleFocusMode = onToggleFocusMode,
            onTriggerAiPinpoint = onTriggerAiPinpoint,
            onCycleRadarBoost = onCycleRadarBoost,
            onRunAiDeepAudit = onRunAiDeepAudit,
            onAddSpatialPoint = onAddSpatialPoint
        )
    }
}

@Composable
fun FullScreenRadarContent(
    uiState: SignalRadarUiState,
    onDismiss: (() -> Unit)? = null,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onSetMapRange: (Float) -> Unit,
    onSelectTargetDevice: (String?) -> Unit,
    onToggleAudioSonar: () -> Unit,
    onSetFullScreenMapMode: (String) -> Unit,
    onSetMaxDevices: (Int) -> Unit = {},
    onToggleFocusMode: () -> Unit = {},
    onTriggerAiPinpoint: (RadarBlip) -> Unit = {},
    onCycleRadarBoost: () -> Unit = {},
    onRunAiDeepAudit: () -> Unit = {},
    onAddSpatialPoint: (String, RfMeasurementPoint) -> Unit = { _, _ -> }
) {
    val sensorSuite = uiState.sensorSuite
    val activeMode = uiState.fullScreenMapMode
    val filteredBlips = rememberFilteredBlips(
        blips = uiState.activeBlips,
        filterType = uiState.selectedFilterType,
        maxDevices = uiState.maxVisibleDevices,
        isFocusMode = uiState.isFocusModeEnabled,
        minRssiDbm = uiState.minRssiFilterDbm,
        selectedTargetId = uiState.selectedTargetDeviceId,
        sortBy = uiState.sortByPriority
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030705))
            .testTag("fullscreen_radar_map_container")
    ) {
        // Central Immersive Map Canvas
        when (activeMode) {
            "HEATMAP" -> {
                SpatialD3HeatmapCanvas(
                    blips = filteredBlips,
                    history = uiState.structuredHistory,
                    isKdeMode = true
                )
            }
            "WATERFALL" -> {
                SpectrumWaterfallCanvas(
                    blips = filteredBlips,
                    modifier = Modifier.fillMaxSize()
                )
            }
            "SAT_GRID" -> {
                SatelliteElevationGridCanvas(
                    blips = filteredBlips,
                    headingDegrees = uiState.headingDegrees,
                    modifier = Modifier.fillMaxSize()
                )
            }
            "AR", "AR_CAMERA" -> {
                UwbArCameraScreen(
                    uiState = uiState,
                    mapRangeMeters = uiState.mapRangeMeters,
                    onSelectTargetDevice = onSelectTargetDevice,
                    modifier = Modifier.fillMaxSize(),
                    onAddSpatialPoint = onAddSpatialPoint
                )
            }
            else -> {
                TacticalRadarCanvas(
                    headingDegrees = uiState.headingDegrees,
                    blips = filteredBlips,
                    nearestBlipId = uiState.nearestBlip?.id,
                    selectedTargetDeviceId = uiState.selectedTargetDeviceId,
                    perimeterThresholdMeters = uiState.perimeterThresholdMeters,
                    mapRangeMeters = uiState.mapRangeMeters,
                    isMapMaximized = true,
                    isHudDeclutterEnabled = uiState.isHudDeclutterEnabled,
                    isFocusModeEnabled = uiState.isFocusModeEnabled,
                    maxVisibleDevices = uiState.maxVisibleDevices,
                    radarBoostLevel = uiState.radarBoostLevel,
                    onZoomIn = onZoomIn,
                    onZoomOut = onZoomOut,
                    onSetMapRange = onSetMapRange,
                    onToggleMaximizeMap = {},
                    onOpenFullScreenMap = {},
                    onSelectTargetDevice = onSelectTargetDevice,
                    onCycleRadarBoost = onCycleRadarBoost,
                    onTriggerAiPinpoint = onTriggerAiPinpoint,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Floating Translucent Top Control Overlay
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF05110A).copy(alpha = 0.88f),
            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0xFF00FF66).copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Heading",
                                tint = Color(0xFF00FF66),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "FULL RADAR • ${uiState.headingDegrees.toInt()}° HEADING",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp
                                ),
                                color = Color(0xFF00FF66)
                            )
                            Text(
                                text = "ALT: ${sensorSuite.estimatedAltitudeMeters.toInt()}m • ${filteredBlips.size} EMITTERS IN SCOPE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFFB0BEC5)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Boost Level Button
                        Surface(
                            onClick = onCycleRadarBoost,
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.radarBoostLevel != RadarBoostLevel.NORMAL_1X) uiState.radarBoostLevel.badgeColor.copy(alpha = 0.25f) else Color(0xFF0E2218),
                            border = BorderStroke(1.dp, uiState.radarBoostLevel.badgeColor),
                            modifier = Modifier.testTag("fullscreen_boost_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Radar Boost",
                                    tint = uiState.radarBoostLevel.badgeColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = uiState.radarBoostLevel.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = uiState.radarBoostLevel.badgeColor
                                )
                            }
                        }

                        IconButton(
                            onClick = onRunAiDeepAudit,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF00FF66).copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.7f), CircleShape)
                                .testTag("fullscreen_ai_deep_audit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Run AI Deep Audit",
                                tint = Color(0xFF00FF66),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleAudioSonar,
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (uiState.isAudioSonarActive) Color(0xFFFFCC00).copy(alpha = 0.25f) else Color.DarkGray.copy(alpha = 0.4f),
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    if (uiState.isAudioSonarActive) Color(0xFFFFCC00) else Color.Transparent,
                                    CircleShape
                                )
                                .testTag("fullscreen_map_audio_sonar_toggle")
                        ) {
                            Icon(
                                imageVector = if (uiState.isAudioSonarActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "Audio Sonar",
                                tint = if (uiState.isAudioSonarActive) Color(0xFFFFCC00) else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (onDismiss != null) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFFF3366).copy(alpha = 0.25f), CircleShape)
                                    .border(1.dp, Color(0xFFFF3366).copy(alpha = 0.6f), CircleShape)
                                    .testTag("close_fullscreen_map_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Full Screen Map",
                                    tint = Color(0xFFFF3366),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Map Display Mode Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "TACTICAL" to "VECTOR",
                        "HEATMAP" to "HEATMAP",
                        "WATERFALL" to "WATERFALL",
                        "SAT_GRID" to "SAT GRID",
                        "AR" to "AR 3D"
                    ).forEach { (modeKey, modeTitle) ->
                        val isSel = activeMode == modeKey || (modeKey == "TACTICAL" && activeMode == "VECTOR")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFF00FF66) else Color(0xFF0D2214))
                                .border(
                                    1.dp,
                                    if (isSel) Color.White else Color(0xFF00FF66).copy(alpha = 0.25f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSetFullScreenMapMode(modeKey) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = modeTitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (isSel) Color.Black else Color(0xFF00FF66)
                            )
                        }
                    }
                }
            }
        }

        // Floating Translucent Bottom Control Overlay
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF05110A).copy(alpha = 0.88f),
            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SCALE:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp
                            ),
                            color = Color.Gray
                        )
                        listOf(5f, 15f, 30f, 60f).forEach { scale ->
                            val isCurrentScale = kotlin.math.abs(uiState.mapRangeMeters - scale) < 0.5f
                            val label = "${scale.toInt()}m"
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrentScale) Color(0xFF00FF66) else Color(0xFF0F261B))
                                    .border(
                                        1.dp,
                                        if (isCurrentScale) Color.White else Color(0xFF00FF66).copy(alpha = 0.35f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSetMapRange(scale) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isCurrentScale) Color.Black else Color(0xFF00FF66)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onZoomIn,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .background(Color(0xFF0C2417), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color(0xFF00FF66), modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = onZoomOut,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .background(Color(0xFF0C2417), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color(0xFF00FF66), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Max Targets Limit Slider Component in Fullscreen Mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF06140D), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Limit Slider",
                        tint = if (uiState.isFocusModeEnabled) Color(0xFFFFCC00) else Color(0xFF00FF66),
                        modifier = Modifier.size(14.dp)
                    )

                    Text(
                        text = if (uiState.isFocusModeEnabled) "FOCUS (3)" else if (uiState.maxVisibleDevices == 0) "ALL (${uiState.activeBlips.size})" else "MAX: ${uiState.maxVisibleDevices}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        ),
                        color = if (uiState.isFocusModeEnabled) Color(0xFFFFCC00) else Color.White
                    )

                    Slider(
                        value = if (uiState.isFocusModeEnabled) 3f else if (uiState.maxVisibleDevices == 0) 50f else uiState.maxVisibleDevices.toFloat(),
                        onValueChange = { value ->
                            if (uiState.isFocusModeEnabled) onToggleFocusMode()
                            val intVal = value.toInt()
                            onSetMaxDevices(if (intVal >= 50) 0 else intVal)
                        },
                        valueRange = 1f..50f,
                        steps = 48,
                        colors = SliderDefaults.colors(
                            thumbColor = if (uiState.isFocusModeEnabled) Color(0xFFFFCC00) else Color(0xFF00FF66),
                            activeTrackColor = if (uiState.isFocusModeEnabled) Color(0xFFFFCC00) else Color(0xFF00FF66),
                            inactiveTrackColor = Color(0xFF132B1D)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .testTag("fullscreen_max_devices_slider")
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (uiState.isFocusModeEnabled) Color(0xFFFFCC00) else Color(0xFF0E2218),
                        border = BorderStroke(1.dp, if (uiState.isFocusModeEnabled) Color.White else Color(0xFF00FF66).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clickable { onToggleFocusMode() }
                            .testTag("fullscreen_focus_mode_btn")
                    ) {
                        Text(
                            text = if (uiState.isFocusModeEnabled) "FOCUS: ON" else "FOCUS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (uiState.isFocusModeEnabled) Color.Black else Color(0xFF00FF66),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }

                val selectedBlip = filteredBlips.find { it.id == uiState.selectedTargetDeviceId || it.name == uiState.selectedTargetDeviceId }
                if (selectedBlip != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF071B0F),
                        border = BorderStroke(1.dp, Color(0xFF00FF66)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TARGET: ${selectedBlip.name}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.Yellow
                                )
                                Text(
                                    text = "%.1fm • %ddBm • ALT: %s".format(
                                        selectedBlip.distance,
                                        selectedBlip.rssi,
                                        if (selectedBlip.estimatedZOffsetMeters >= 0) "+%.1fm".format(selectedBlip.estimatedZOffsetMeters) else "%.1fm".format(selectedBlip.estimatedZOffsetMeters)
                                    ),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp
                                    ),
                                    color = Color.LightGray
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    onClick = { onTriggerAiPinpoint(selectedBlip) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF00FF66),
                                    border = BorderStroke(1.dp, Color.White),
                                    modifier = Modifier.testTag("fullscreen_target_ai_pinpoint_btn")
                                ) {
                                    Text(
                                        text = "🎯 AI 3D PINPOINT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.5.sp
                                        ),
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    onClick = { onSelectTargetDevice(null) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF221111),
                                    border = BorderStroke(1.dp, Color.Red)
                                ) {
                                    Text(
                                        text = "CLEAR",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.5.sp
                                        ),
                                        color = Color.Red,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RANGE: ${uiState.mapRangeMeters.toInt()}m • SENSORS: ${sensorSuite.totalActiveSensorsCount} ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF00FF66)
                    )
                    Text(
                        text = "BARO ${sensorSuite.pressureHpa.toInt()}hPa • EMF ${sensorSuite.magnetometerData.totalMicroTesla.toInt()}µT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp
                        ),
                        color = Color(0xFF00E5FF)
                    )
                }
            }
        }
    }
}
