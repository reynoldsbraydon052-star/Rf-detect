package com.example

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
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
    val relativeAngleDegrees: Float
)

object UwbArTracker {
    fun updateTargetPosition(
        blip: RadarBlip,
        headingDegrees: Float,
        pitchDegrees: Float,
        rollDegrees: Float,
        mapRangeMeters: Float,
        containerWidth: Float,
        containerHeight: Float,
        fovDegrees: Float = 60f
    ): ArTargetProjection {
        var rawRelAngle = (blip.targetAngleOffset - headingDegrees + 360f) % 360f
        if (rawRelAngle > 180f) rawRelAngle -= 360f

        val halfFov = fovDegrees / 2f
        val isVisibleHorizontally = kotlin.math.abs(rawRelAngle) <= halfFov

        val xFraction = 0.5f + (rawRelAngle / fovDegrees)
        val screenX = (xFraction * containerWidth).coerceIn(-containerWidth * 0.5f, containerWidth * 1.5f)

        val effectiveRange = mapRangeMeters.coerceAtLeast(1.0f)
        val depthRatio = (blip.distance / effectiveRange).coerceIn(0.05f, 3.0f)

        val baseVerticalRatio = 0.45f + (depthRatio * 0.25f) - (pitchDegrees / 120f)
        val screenY = (baseVerticalRatio.coerceIn(0.15f, 0.85f)) * containerHeight

        return ArTargetProjection(
            id = blip.id,
            name = blip.name,
            distanceMeters = blip.distance,
            rssiDbm = blip.rssi,
            type = blip.type,
            screenX = screenX,
            screenY = screenY,
            depthRatio = depthRatio,
            isVisibleInFov = isVisibleHorizontally,
            relativeAngleDegrees = rawRelAngle
        )
    }
}

@Composable
fun UwbArCameraScreen(
    uiState: SignalRadarUiState,
    mapRangeMeters: Float,
    onSelectTargetDevice: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("uwb_ar_camera_screen")
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder()
                                    .setTargetRotation(this.display?.rotation ?: Surface.ROTATION_0)
                                    .build()
                                    .also {
                                        it.setSurfaceProvider(this.surfaceProvider)
                                    }
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("UwbArCameraScreen", "Camera binding error", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF0C1F15), Color(0xFF030704))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AR CAMERA FEED (CAMERA PERMISSION REQUIRED)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Gray
                )
            }
        }

        val sensorSuite = uiState.sensorSuite
        val pitchDegrees = sensorSuite.pitchDeg
        val rollDegrees = sensorSuite.rollDeg

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uiState.activeBlips, uiState.headingDegrees, mapRangeMeters) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        var closestId: String? = null
                        var minDistance = Float.MAX_VALUE

                        uiState.activeBlips.forEach { blip ->
                            val proj = UwbArTracker.updateTargetPosition(
                                blip = blip,
                                headingDegrees = uiState.headingDegrees,
                                pitchDegrees = pitchDegrees,
                                rollDegrees = rollDegrees,
                                mapRangeMeters = mapRangeMeters,
                                containerWidth = w,
                                containerHeight = h
                            )
                            val dist = kotlin.math.hypot(
                                (proj.screenX - tapOffset.x).toDouble(),
                                (proj.screenY - tapOffset.y).toDouble()
                            ).toFloat()
                            if (dist < minDistance) {
                                minDistance = dist
                                closestId = blip.id
                            }
                        }

                        if (closestId != null && minDistance <= 65f) {
                            onSelectTargetDevice(closestId)
                        } else {
                            onSelectTargetDevice(null)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            val horizonY = h * 0.5f - (pitchDegrees / 90f * h * 0.3f)
            drawLine(
                color = Color(0xFF00FF66).copy(alpha = 0.25f),
                start = Offset(w * 0.2f, horizonY),
                end = Offset(w * 0.8f, horizonY),
                strokeWidth = 1.5f
            )

            drawCircle(
                color = Color(0xFF00FF66).copy(alpha = 0.4f),
                radius = 24f,
                center = Offset(w / 2f, h / 2f),
                style = Stroke(width = 1.5f)
            )
            drawLine(
                color = Color(0xFF00FF66).copy(alpha = 0.5f),
                start = Offset(w / 2f - 12f, h / 2f),
                end = Offset(w / 2f + 12f, h / 2f),
                strokeWidth = 1.5f
            )
            drawLine(
                color = Color(0xFF00FF66).copy(alpha = 0.5f),
                start = Offset(w / 2f, h / 2f - 12f),
                end = Offset(w / 2f, h / 2f + 12f),
                strokeWidth = 1.5f
            )

            uiState.activeBlips.forEach { blip ->
                val proj = UwbArTracker.updateTargetPosition(
                    blip = blip,
                    headingDegrees = uiState.headingDegrees,
                    pitchDegrees = pitchDegrees,
                    rollDegrees = rollDegrees,
                    mapRangeMeters = mapRangeMeters,
                    containerWidth = w,
                    containerHeight = h
                )

                val isSelected = blip.id == uiState.selectedTargetDeviceId
                val targetColor = when {
                    isSelected -> Color(0xFFFFCC00)
                    blip.distance < uiState.perimeterThresholdMeters -> Color(0xFFFF3366)
                    blip.type == "WIFI" -> Color(0xFF00FF66)
                    blip.type == "BLE" -> Color(0xFF00E5FF)
                    else -> Color(0xFFFF9900)
                }

                val visualScale = (1.8f - (proj.depthRatio * 0.8f)).coerceIn(0.4f, 2.2f)
                val arrowSize = 34f * visualScale
                val px = proj.screenX
                val py = proj.screenY

                if (proj.isVisibleInFov) {
                    val path = Path().apply {
                        moveTo(px, py - arrowSize)
                        lineTo(px + arrowSize * 0.7f, py)
                        lineTo(px, py + arrowSize * 0.6f)
                        lineTo(px - arrowSize * 0.7f, py)
                        close()
                    }

                    drawPath(
                        path = path,
                        color = targetColor.copy(alpha = if (isSelected) 0.85f else 0.55f),
                        style = Fill
                    )
                    drawPath(
                        path = path,
                        color = targetColor,
                        style = Stroke(width = if (isSelected) 3f else 1.8f)
                    )

                    drawLine(
                        color = targetColor.copy(alpha = 0.4f),
                        start = Offset(px, py + arrowSize * 0.6f),
                        end = Offset(px, py + arrowSize * 1.8f),
                        strokeWidth = 1.5f
                    )
                    drawCircle(
                        color = targetColor.copy(alpha = 0.6f),
                        radius = 5f * visualScale,
                        center = Offset(px, py + arrowSize * 1.8f),
                        style = Stroke(width = 1.5f)
                    )

                    val labelText = "${blip.name.take(12)}\n%.1fm • %ddBm".format(blip.distance, blip.rssi)
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = (26f * visualScale).coerceIn(18f, 34f)
                        typeface = android.graphics.Typeface.MONOSPACE
                        isAntiAlias = true
                        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                    }

                    val lines = labelText.split("\n")
                    var lineY = py - arrowSize - 10f
                    lines.reversed().forEach { line ->
                        drawContext.canvas.nativeCanvas.drawText(
                            line,
                            px - (textPaint.measureText(line) / 2f),
                            lineY,
                            textPaint
                        )
                        lineY -= textPaint.textSize + 4f
                    }
                } else {
                    val edgeMargin = 36f
                    val edgeX = if (proj.relativeAngleDegrees < 0) edgeMargin else w - edgeMargin
                    val edgeY = py.coerceIn(edgeMargin, h - edgeMargin)

                    val edgePath = Path().apply {
                        if (proj.relativeAngleDegrees < 0) {
                            moveTo(edgeX, edgeY)
                            lineTo(edgeX + 22f, edgeY - 12f)
                            lineTo(edgeX + 22f, edgeY + 12f)
                        } else {
                            moveTo(edgeX, edgeY)
                            lineTo(edgeX - 22f, edgeY - 12f)
                            lineTo(edgeX - 22f, edgeY + 12f)
                        }
                        close()
                    }

                    drawPath(path = edgePath, color = targetColor.copy(alpha = 0.7f), style = Fill)
                }
            }
        }
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
    onSetFullScreenMapMode: (String) -> Unit
) {
    FullScreenRadarContent(
        uiState = uiState,
        onDismiss = null,
        onZoomIn = onZoomIn,
        onZoomOut = onZoomOut,
        onSetMapRange = onSetMapRange,
        onSelectTargetDevice = onSelectTargetDevice,
        onToggleAudioSonar = onToggleAudioSonar,
        onSetFullScreenMapMode = onSetFullScreenMapMode
    )
}

@Composable
fun FullScreenRadarMapOverlay(
    uiState: SignalRadarUiState,
    onDismiss: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onSetMapRange: (Float) -> Unit,
    onSelectTargetDevice: (String?) -> Unit,
    onToggleAudioSonar: () -> Unit,
    onSetFullScreenMapMode: (String) -> Unit
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
            onSetFullScreenMapMode = onSetFullScreenMapMode
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
    onSetFullScreenMapMode: (String) -> Unit
) {
    val sensorSuite = uiState.sensorSuite
    val activeMode = uiState.fullScreenMapMode
    val filteredBlips = rememberFilteredBlips(uiState.activeBlips, uiState.selectedFilterType)

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
                    modifier = Modifier.fillMaxSize()
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
                    onZoomIn = onZoomIn,
                    onZoomOut = onZoomOut,
                    onSetMapRange = onSetMapRange,
                    onToggleMaximizeMap = {},
                    onOpenFullScreenMap = {},
                    onSelectTargetDevice = onSelectTargetDevice,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Floating Translucent Top Control Overlay
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF07120B).copy(alpha = 0.85f),
            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF00FF66).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Heading",
                                tint = Color(0xFF00FF66),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "FULL RADAR • ${uiState.headingDegrees.toInt()}° HEADING",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFF00FF66)
                            )
                            Text(
                                text = "GPS 37.7749° N, 122.4194° W • ALT ${sensorSuite.estimatedAltitudeMeters.toInt()}m",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.LightGray
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onToggleAudioSonar,
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (uiState.isAudioSonarActive) Color(0xFFFFCC00).copy(alpha = 0.25f) else Color.DarkGray.copy(alpha = 0.4f),
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                                    .background(if (isSel) Color(0xFF00FF66) else Color(0xFF0C1F13))
                                    .clickable { onSetFullScreenMapMode(modeKey) }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = modeTitle,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isSel) Color.Black else Color(0xFF00FF66)
                                )
                            }
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
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF050E07).copy(alpha = 0.85f),
            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.35f))
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
                            val isCurrentScale = uiState.mapRangeMeters.toInt() == scale.toInt()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isCurrentScale) Color(0xFF00E5FF) else Color(0xFF0F261B))
                                    .clickable { onSetMapRange(scale) }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${scale.toInt()}m",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isCurrentScale) Color.Black else Color(0xFF00E5FF)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onZoomIn,
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF0C2417), CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color(0xFF00FF66), modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = onZoomOut,
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF0C2417), CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color(0xFF00FF66), modifier = Modifier.size(16.dp))
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
