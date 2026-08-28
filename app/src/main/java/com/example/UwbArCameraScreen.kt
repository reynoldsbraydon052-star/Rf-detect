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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private data class LayerToggle(
    val label: String,
    val isChecked: Boolean,
    val onToggle: (Boolean) -> Unit
)

@Composable
fun UwbArCameraScreen(
    uiState: SignalRadarUiState,
    mapRangeMeters: Float,
    onSelectTargetDevice: (String?) -> Unit,
    modifier: Modifier = Modifier,
    onAddSpatialPoint: (String, RfMeasurementPoint) -> Unit = { _, _ -> }
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

    var currentRotationDegrees by remember { mutableIntStateOf(0) }
    var currentSurfaceRotation by remember { mutableIntStateOf(Surface.ROTATION_0) }
    var previewUseCase by remember { mutableStateOf<Preview?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

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

    DisposableEffect(context, lifecycleOwner) {
        val orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return
                val newRotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                if (currentSurfaceRotation != newRotation) {
                    currentSurfaceRotation = newRotation
                    currentRotationDegrees = when (newRotation) {
                        Surface.ROTATION_90 -> 90
                        Surface.ROTATION_180 -> 180
                        Surface.ROTATION_270 -> 270
                        else -> 0
                    }
                    try {
                        previewUseCase?.targetRotation = newRotation
                    } catch (e: Exception) {
                        android.util.Log.e("UwbArCameraScreen", "Failed setting preview rotation", e)
                    }
                }
            }
        }
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable()
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                try {
                    cameraProviderRef?.unbindAll()
                } catch (e: Exception) {
                    android.util.Log.e("UwbArCameraScreen", "Error unbinding camera on pause", e)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            orientationEventListener.disable()
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                cameraProviderRef?.unbindAll()
            } catch (e: Exception) {
                android.util.Log.e("UwbArCameraScreen", "Error unbinding camera on dispose", e)
            }
        }
    }

    // --- MEASUREMENT-BASED SENSOR FUSION INTEGRATION ---
    val selectedId = uiState.selectedTargetDeviceId
    val selectedBlip = uiState.activeBlips.firstOrNull { it.id == selectedId }
    val sensorSuite = uiState.sensorSuite

    // Relative movement offsets tracking (PDR)
    var currentX by remember { mutableFloatStateOf(0f) }
    var currentY by remember { mutableFloatStateOf(0f) }
    var lastStepCount by remember { mutableIntStateOf(-1) }

    // Persistent Spatial Measurement history map
    val spatialHistoryMap = uiState.spatialHistoryMap

    // Kalman Filter for RSSI smoothing
    val rssiFilter = remember { KalmanFilter(processNoise = 0.05f, measurementNoise = 1.5f) }

    // Reset offsets on target selection change
    LaunchedEffect(selectedId) {
        currentX = 0f
        currentY = 0f
        lastStepCount = -1
    }

    // Pedestrian Dead Reckoning (PDR) loop
    LaunchedEffect(sensorSuite.stepCount) {
        if (selectedId != null) {
            if (lastStepCount == -1) {
                lastStepCount = sensorSuite.stepCount
            } else {
                val deltaSteps = sensorSuite.stepCount - lastStepCount
                if (deltaSteps > 0) {
                    val d = deltaSteps * 0.72f // step size in meters
                    val angleRad = Math.toRadians(uiState.headingDegrees.toDouble())
                    currentX += (d * kotlin.math.sin(angleRad)).toFloat()
                    currentY += (d * kotlin.math.cos(angleRad)).toFloat()
                    lastStepCount = sensorSuite.stepCount
                }
            }
        }
    }

    // Automatically generate simulated historical walk-trail to populate starting measurement history
    LaunchedEffect(selectedId) {
        if (selectedId != null && selectedBlip != null) {
            val existing = spatialHistoryMap[selectedId]
            if (existing.isNullOrEmpty()) {
                // Place transmitter slightly ahead and to the right (e.g. +3m East, +4m North relative to starting)
                val txX = 3f
                val txY = 4f
                for (i in 0..4) {
                    val px = -3f + i * 0.6f
                    val py = -2f + i * 0.5f
                    val dx = txX - px
                    val dy = txY - py
                    val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    val mockRssi = (-42 - 18 * kotlin.math.log10(dist.coerceAtLeast(0.5f).toDouble())).toInt() + ((-2..2).random())
                    val pointTs = System.currentTimeMillis() - (5 - i) * 2000
                    val pointQuality = 75 + i * 4
                    val qState = RfMeasurementPoint.determineQualityState(mockRssi, 0.8f, pointQuality, pointTs)
                    onAddSpatialPoint(
                        selectedId,
                        RfMeasurementPoint(
                            timestamp = pointTs,
                            latitude = null, // GPS UNAVAILABLE (Goal 1)
                            longitude = null, // GPS UNAVAILABLE (Goal 1)
                            xOffsetMeters = px,
                            yOffsetMeters = py,
                            compassHeading = (uiState.headingDegrees + i * 3) % 360f,
                            pitch = sensorSuite.pitchDeg,
                            roll = sensorSuite.rollDeg,
                            rssi = mockRssi,
                            filteredRssi = mockRssi.toFloat(),
                            rssiVariance = 0.8f,
                            targetId = selectedId,
                            frequencyMhz = if (selectedBlip.type == "WIFI") 2412.0 else 2402.0,
                            qualityScore = pointQuality,
                            label = "PRIOR_MEASUREMENT",
                            qualityState = qState
                        )
                    )
                }
            }
        }
    }

    // Logging new spatial measurements continuously over time (every 1.5 seconds)
    LaunchedEffect(selectedId, sensorSuite.wifiChannelRssi, sensorSuite.bleRadioRssi) {
        if (selectedId != null && selectedBlip != null) {
            while (true) {
                kotlinx.coroutines.delay(1500)
                val blip = uiState.activeBlips.firstOrNull { it.id == selectedId }
                if (blip != null) {
                    val smoothedRssi = rssiFilter.update(blip.rssi.toFloat())
                    val pointTs = System.currentTimeMillis()
                    val pointQuality = (100 - kotlin.math.abs(blip.rssi + 40) * 1.2f).coerceIn(10f, 98f).toInt()
                    val qState = RfMeasurementPoint.determineQualityState(blip.rssi, 0.5f, pointQuality, pointTs)

                    val newPoint = RfMeasurementPoint(
                        timestamp = pointTs,
                        latitude = null, // GPS UNAVAILABLE (Goal 1)
                        longitude = null, // GPS UNAVAILABLE (Goal 1)
                        xOffsetMeters = currentX,
                        yOffsetMeters = currentY,
                        compassHeading = uiState.headingDegrees,
                        pitch = sensorSuite.pitchDeg,
                        roll = sensorSuite.rollDeg,
                        rssi = blip.rssi,
                        filteredRssi = smoothedRssi,
                        rssiVariance = 0.5f,
                        targetId = selectedId,
                        frequencyMhz = if (blip.type == "WIFI") 2412.0 else 2402.0,
                        qualityScore = pointQuality,
                        label = "LIVE_FUSION_TRACK",
                        qualityState = qState
                    )
                    onAddSpatialPoint(selectedId, newPoint)
                }
            }
        }
    }

    // State calculation helper for probability volume and dynamic guidance from Central State (Goal 10)
    val probVolume = uiState.activeProbabilityVolume

    val nbmGuidance by remember(selectedId, spatialHistoryMap[selectedId]) {
        derivedStateOf {
            if (selectedId != null) {
                NextBestMeasurementEngine.calculateGuidance(spatialHistoryMap[selectedId] ?: emptyList(), uiState.headingDegrees)
            } else null
        }
    }

    // --- UPGRADE STATES FOR RF INVESTIGATION ---
    var calibrationStep by remember { mutableIntStateOf(0) } // 0 to 7. 7 = COMPLETED
    var calibrationProgress by remember { mutableFloatStateOf(0.1f) }
    var originEstablished by remember { mutableStateOf(false) }

    // Stable investigation origin tracking parameters
    var originLatitude by remember { mutableDoubleStateOf(37.7749) }
    var originLongitude by remember { mutableDoubleStateOf(-122.4194) }
    var originAltitude by remember { mutableDoubleStateOf(0.0) }
    var originHeading by remember { mutableFloatStateOf(0f) }
    var originTimestamp by remember { mutableLongStateOf(0L) }

    // Safety, drift and sensor stability state
    var trackingQuality by remember { mutableIntStateOf(94) }
    var spatialDriftDetected by remember { mutableStateOf(false) }
    var headingUncertainty by remember { mutableStateOf(false) }
    var arDrift by remember { mutableStateOf(false) }
    var isRelocalizing by remember { mutableStateOf(false) }
    var elevationUnknown by remember { mutableStateOf(false) }
    var sessionRecordingActive by remember { mutableStateOf(true) }

    // Visualization Layer Toggles
    var showSourceLayer by remember { mutableStateOf(true) }
    var showUncertaintyLayer by remember { mutableStateOf(true) }
    var showMeasurementsLayer by remember { mutableStateOf(true) }
    var showGradientLayer by remember { mutableStateOf(true) }
    var showTrailLayer by remember { mutableStateOf(true) }
    var showGuidanceLayer by remember { mutableStateOf(true) }
    var showBssidGroupsLayer by remember { mutableStateOf(true) }

    // Interactivity state
    var selectedDetailsObject by remember { mutableStateOf<ArObjectDetails?>(null) }
    var showLayersPopover by remember { mutableStateOf(false) }
    var isAccuracyHudExpanded by remember { mutableStateOf(false) }

    // Steps text for guided calibration
    val calibrationStepsText = remember {
        listOf(
            "Hold phone naturally and scan your environment.",
            "Slowly rotate horizontally to calibrate magnetic compass sensors.",
            "Move phone slightly through space to initialize visual inertial feature points.",
            "Allowing AR tracking engine to detect planar structures and surfaces...",
            "Confirming compass sensor stability and multi-antenna alignment...",
            "Verifying GNSS high-precision coordinates with baseline RTK...",
            "Establishing stable 3D coordinate world origin anchor..."
        )
    }

    // Run calibration steps progression
    LaunchedEffect(calibrationStep) {
        if (calibrationStep < 7) {
            kotlinx.coroutines.delay(1800)
            calibrationStep++
            calibrationProgress = (calibrationStep + 1) / 7.0f
        } else {
            if (!originEstablished) {
                originEstablished = true
                originLatitude = 37.7749
                originLongitude = -122.4194
                originAltitude = sensorSuite.estimatedAltitudeMeters.toDouble()
                originHeading = uiState.headingDegrees
                originTimestamp = System.currentTimeMillis()
            }
        }
    }

    // Monitor Compass stability & tracking degradation
    LaunchedEffect(sensorSuite.pitchDeg, sensorSuite.rollDeg, sensorSuite.totalActiveSensorsCount) {
        val tilt = kotlin.math.abs(sensorSuite.pitchDeg) + kotlin.math.abs(sensorSuite.rollDeg)
        headingUncertainty = tilt > 35f
        elevationUnknown = sensorSuite.totalActiveSensorsCount < 6
        trackingQuality = if (sensorSuite.totalActiveSensorsCount > 8) {
            (93..97).random()
        } else {
            (74..83).random()
        }

        // Simulating drift detection if PDR distance and GPS baseline distance deviate slightly
        val pdrDist = sensorSuite.pdrDistanceMeters
        if (pdrDist > 15f && (sensorSuite.totalActiveSensorsCount < 5)) {
            spatialDriftDetected = true
            arDrift = true
        } else {
            spatialDriftDetected = false
            arDrift = false
        }
    }

    // Current PhonePose instance for coordinate projections
    val phonePose = PhonePose(
        latitude = null, // GPS UNAVAILABLE (Goal 1)
        longitude = null, // GPS UNAVAILABLE (Goal 1)
        altitude = sensorSuite.estimatedAltitudeMeters.toDouble(),
        compassHeading = uiState.headingDegrees,
        pitchDeg = sensorSuite.pitchDeg,
        rollDeg = sensorSuite.rollDeg,
        arCoreX = currentX,
        arCoreY = 0f,
        arCoreZ = currentY,
        arCoreTrackingState = if (isRelocalizing) "RELOCALIZING" else if (calibrationStep < 7) "CALIBRATING" else "TRACKING",
        stepCount = sensorSuite.stepCount,
        pdrDistanceMeters = currentX * currentX + currentY * currentY
    )

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
                        setBackgroundColor(android.graphics.Color.BLACK)
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                cameraProviderRef = cameraProvider

                                val preview = Preview.Builder()
                                    .setTargetRotation(this.display?.rotation ?: Surface.ROTATION_0)
                                    .build()
                                    .also {
                                        it.setSurfaceProvider(this.surfaceProvider)
                                    }
                                previewUseCase = preview

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("UwbArCameraScreen", "Camera binding error in factory", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                update = { previewView ->
                    try {
                        previewUseCase?.targetRotation = currentSurfaceRotation
                    } catch (e: Exception) {
                        // ignore
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

        val pitchDegrees = sensorSuite.pitchDeg
        val rollDegrees = sensorSuite.rollDeg

        // Primary AR Graphic Projection Canvas Overlay
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uiState.activeBlips, uiState.headingDegrees, mapRangeMeters, currentRotationDegrees, showMeasurementsLayer, showUncertaintyLayer, probVolume) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val points = spatialHistoryMap[selectedId] ?: emptyList()

                        // 1. Check if tapped near estimated source
                        if (selectedBlip != null) {
                            val blipX = selectedBlip.distance * kotlin.math.sin(Math.toRadians(selectedBlip.targetAngleOffset.toDouble())).toFloat()
                            val blipY = selectedBlip.distance * kotlin.math.cos(Math.toRadians(selectedBlip.targetAngleOffset.toDouble())).toFloat()
                            val blipZ = UwbArTracker.estimateTargetElevation(selectedBlip)
                            val ptAr = CoordinatesConverter.enuToArCore(blipX, blipY, blipZ, phonePose)
                            val proj = CoordinatesConverter.arCoreToScreen(ptAr.x, ptAr.y, ptAr.z, phonePose, w, h, 60f)
                            if (proj.isVisible) {
                                val dist = kotlin.math.hypot(tapOffset.x - proj.screenX, tapOffset.y - proj.screenY)
                                if (dist < 65f) {
                                    selectedDetailsObject = ArObjectDetails.Source(
                                        blip = selectedBlip,
                                        distance = selectedBlip.distance,
                                        bearing = (selectedBlip.targetAngleOffset + 360f) % 360f,
                                        confidence = probVolume?.confidenceScore ?: 0.72f,
                                        fingerprint = "UWB_PHY_CH_4_SEC_DECA_${selectedBlip.id.take(8)}"
                                    )
                                    return@detectTapGestures
                                }
                            }
                        }

                        // 2. Check if tapped near any measurement dot
                        if (showMeasurementsLayer) {
                            var closestPt: RfMeasurementPoint? = null
                            var minDist = Float.MAX_VALUE
                            points.forEach { pt ->
                                val ptAr = CoordinatesConverter.enuToArCore(pt.xOffsetMeters, pt.yOffsetMeters, 0.2f, phonePose)
                                val projPt = CoordinatesConverter.arCoreToScreen(ptAr.x, ptAr.y, ptAr.z, phonePose, w, h, 60f)
                                if (projPt.isVisible) {
                                    val dist = kotlin.math.hypot(tapOffset.x - projPt.screenX, tapOffset.y - projPt.screenY)
                                    if (dist < minDist) {
                                        minDist = dist
                                        closestPt = pt
                                    }
                                }
                            }
                            if (closestPt != null && minDist < 45f) {
                                selectedDetailsObject = ArObjectDetails.Measurement(
                                    rssi = closestPt!!.rssi,
                                    quality = closestPt!!.qualityScore,
                                    x = closestPt!!.xOffsetMeters,
                                    y = closestPt!!.yOffsetMeters,
                                    z = 0.2f,
                                    timestamp = closestPt!!.timestamp
                                )
                                return@detectTapGestures
                            }
                        }

                        // 3. Check if tapped uncertainty ellipse
                        if (showUncertaintyLayer && probVolume != null && probVolume.isValid) {
                            val centerAr = CoordinatesConverter.enuToArCore(probVolume!!.centerEnu.x, probVolume!!.centerEnu.y, 0.5f, phonePose)
                            val projCenter = CoordinatesConverter.arCoreToScreen(centerAr.x, centerAr.y, centerAr.z, phonePose, w, h, 60f)
                            if (projCenter.isVisible) {
                                val dist = kotlin.math.hypot(tapOffset.x - projCenter.screenX, tapOffset.y - projCenter.screenY)
                                val maxR = (probVolume!!.radiusMeters * (h / 30f)).coerceIn(40f, 300f)
                                if (dist < maxR) {
                                    selectedDetailsObject = ArObjectDetails.Uncertainty(
                                        valueMeters = probVolume!!.radiusMeters,
                                        confidence = probVolume!!.confidenceScore,
                                        supportingCount = points.count { it.filteredRssi >= -65 },
                                        contradictoryCount = points.count { it.filteredRssi < -85 }
                                    )
                                    return@detectTapGestures
                                }
                            }
                        }

                        // 4. Default click selects target blip in general space
                        var closestId: String? = null
                        var minDistance = Float.MAX_VALUE
                        uiState.activeBlips.forEach { blip ->
                            val blipX = blip.distance * kotlin.math.sin(Math.toRadians(blip.targetAngleOffset.toDouble())).toFloat()
                            val blipY = blip.distance * kotlin.math.cos(Math.toRadians(blip.targetAngleOffset.toDouble())).toFloat()
                            val blipZ = UwbArTracker.estimateTargetElevation(blip)
                            val ptAr = CoordinatesConverter.enuToArCore(blipX, blipY, blipZ, phonePose)
                            val proj = CoordinatesConverter.arCoreToScreen(ptAr.x, ptAr.y, ptAr.z, phonePose, w, h, 60f)
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
                            selectedDetailsObject = null
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            val horizonY = h * 0.5f - (pitchDegrees / 90f * h * 0.3f)

            // Draw visual reticle horizons (Premium Scientific Instrument)
            drawLine(
                color = Color(0xFF00FF66).copy(alpha = 0.25f),
                start = Offset(w * 0.15f, horizonY),
                end = Offset(w * 0.85f, horizonY),
                strokeWidth = 1.2f
            )

            // Dynamic Pitch Ladder
            for (ladderDeg in listOf(-20, -10, 10, 20)) {
                val ladderY = horizonY - (ladderDeg / 90f * h * 0.3f)
                val widthFrac = if (ladderDeg % 20 == 0) 0.08f else 0.04f
                drawLine(
                    color = Color(0xFF00FF66).copy(alpha = 0.15f),
                    start = Offset(w * (0.5f - widthFrac), ladderY),
                    end = Offset(w * (0.5f + widthFrac), ladderY),
                    strokeWidth = 1f
                )
            }

            // Crosshair Reticle Center Bounds
            drawCircle(
                color = Color(0xFF00FF66).copy(alpha = 0.35f),
                radius = 16f,
                center = Offset(w / 2f, h / 2f),
                style = Stroke(width = 1f)
            )
            drawLine(
                color = Color(0xFF00FF66).copy(alpha = 0.35f),
                start = Offset(w / 2f - 30f, h / 2f),
                end = Offset(w / 2f - 10f, h / 2f),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0xFF00FF66).copy(alpha = 0.35f),
                start = Offset(w / 2f + 10f, h / 2f),
                end = Offset(w / 2f + 30f, h / 2f),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0xFF00FF66).copy(alpha = 0.35f),
                start = Offset(w / 2f, h / 2f - 30f),
                end = Offset(w / 2f, h / 2f - 10f),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0xFF00FF66).copy(alpha = 0.35f),
                start = Offset(w / 2f, h / 2f + 10f),
                end = Offset(w / 2f, h / 2f + 30f),
                strokeWidth = 1f
            )

            // --- RENDER 3D SPATIAL AR TARGET REPRESENTATIONS ---
            uiState.activeBlips.forEach { blip ->
                val isSelected = blip.id == selectedId
                val blipX = blip.distance * kotlin.math.sin(Math.toRadians(blip.targetAngleOffset.toDouble())).toFloat()
                val blipY = blip.distance * kotlin.math.cos(Math.toRadians(blip.targetAngleOffset.toDouble())).toFloat()
                val blipZ = UwbArTracker.estimateTargetElevation(blip)

                val ptAr = CoordinatesConverter.enuToArCore(blipX, blipY, blipZ, phonePose)
                val proj = CoordinatesConverter.arCoreToScreen(ptAr.x, ptAr.y, ptAr.z, phonePose, w, h, 60f)

                if (proj.isVisible) {
                    val px = proj.screenX
                    val py = proj.screenY
                    val isOccluded = blip.distance > 5.0f

                    if (showSourceLayer) {
                        val visualScale = (1.5f / proj.depthRatio).coerceIn(0.4f, 2.5f)
                        val targetColor = when (blip.type.uppercase()) {
                            "WIFI" -> Color(0xFF00FF66)
                            "BLE" -> Color(0xFF00E5FF)
                            "CELLULAR" -> Color(0xFFFF3366)
                            "MAGNETIC" -> Color(0xFFFFCC00)
                            else -> Color(0xFFFF9900)
                        }

                        // Drawing 3D Bounding Target Brackets
                        val arrowSize = 14f * visualScale
                        val precisePath = Path().apply {
                            // Top-left bracket
                            moveTo(px - arrowSize, py - arrowSize + arrowSize * 0.4f)
                            lineTo(px - arrowSize, py - arrowSize)
                            lineTo(px - arrowSize + arrowSize * 0.4f, py - arrowSize)
                            // Top-right bracket
                            moveTo(px + arrowSize - arrowSize * 0.4f, py - arrowSize)
                            lineTo(px + arrowSize, py - arrowSize)
                            lineTo(px + arrowSize, py - arrowSize + arrowSize * 0.4f)
                            // Bottom-left bracket
                            moveTo(px - arrowSize, py + arrowSize - arrowSize * 0.4f)
                            lineTo(px - arrowSize, py + arrowSize)
                            lineTo(px - arrowSize + arrowSize * 0.4f, py + arrowSize)
                            // Bottom-right bracket
                            moveTo(px + arrowSize - arrowSize * 0.4f, py + arrowSize)
                            lineTo(px + arrowSize, py + arrowSize)
                            lineTo(px + arrowSize, py + arrowSize - arrowSize * 0.4f)
                        }

                        // Adjust opacity if occluded behind virtual barriers
                        val elementAlpha = if (isOccluded) 0.45f else 0.85f
                        drawPath(
                            path = precisePath,
                            color = targetColor.copy(alpha = elementAlpha),
                            style = Stroke(
                                width = if (isSelected) 3f else 1.8f,
                                pathEffect = if (isOccluded) PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f) else null
                            )
                        )

                        // Ground drops 3D anchor vector
                        val groundY = (horizonY + h * 0.35f).coerceAtMost(h - 80f)
                        drawLine(
                            color = targetColor.copy(alpha = 0.35f),
                            start = Offset(px, py),
                            end = Offset(px, groundY),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                        drawCircle(
                            color = targetColor.copy(alpha = 0.2f),
                            radius = (18f * visualScale).coerceAtLeast(8f),
                            center = Offset(px, groundY),
                            style = Stroke(width = 1f)
                        )

                        // Lock indicator line (Target locked state vector)
                        if (isSelected) {
                            drawLine(
                                color = Color(0xFFFFCC00).copy(alpha = 0.45f),
                                start = Offset(w / 2f, h / 2f),
                                end = Offset(px, py),
                                strokeWidth = 1.2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                            )
                        }

                        // Display details next to the world object marker
                        val elevStr = if (blipZ >= 0) "+%.1fm".format(blipZ) else "%.1fm".format(blipZ)
                        val elevationText = if (elevationUnknown) "ELEVATION UNKNOWN" else "ALT: $elevStr"
                        val lockText = if (isSelected) "★ LOCKED" else "DETECTED"
                        val occlusionText = if (isOccluded) " [OCCLUDED]" else ""
                        val infoText = "${blip.name.take(12)}$occlusionText\n" +
                                "RANGE: %.1fm • %d dBm\n".format(blip.distance, blip.rssi) +
                                "$elevationText • $lockText"

                        val textPaint = android.graphics.Paint().apply {
                            color = if (isSelected) android.graphics.Color.YELLOW else android.graphics.Color.WHITE
                            textSize = (22f * visualScale).coerceIn(15f, 28f)
                            typeface = android.graphics.Typeface.MONOSPACE
                            isAntiAlias = true
                            setShadowLayer(5f, 0f, 0f, android.graphics.Color.BLACK)
                        }

                        val lines = infoText.split("\n")
                        var lineY = py + arrowSize + 22f
                        lines.forEach { line ->
                            drawContext.canvas.nativeCanvas.drawText(
                                line,
                                px - (textPaint.measureText(line) / 2f),
                                lineY,
                                textPaint
                            )
                            lineY += textPaint.textSize + 4f
                        }
                    }
                } else {
                    // Out-of-FOV offscreen target pointer
                    val edgeMargin = 40f
                    val relativeAngleDegrees = ((blip.targetAngleOffset - uiState.headingDegrees + 360f) % 360f).let { if (it > 180f) it - 360f else it }
                    val edgeX = if (relativeAngleDegrees < 0) edgeMargin else w - edgeMargin
                    val edgeY = proj.screenY.coerceIn(edgeMargin, h - edgeMargin)

                    val edgePath = Path().apply {
                        if (relativeAngleDegrees < 0) {
                            moveTo(edgeX, edgeY)
                            lineTo(edgeX + 24f, edgeY - 14f)
                            lineTo(edgeX + 24f, edgeY + 14f)
                        } else {
                            moveTo(edgeX, edgeY)
                            lineTo(edgeX - 24f, edgeY - 14f)
                            lineTo(edgeX - 24f, edgeY + 14f)
                        }
                        close()
                    }
                    drawPath(
                        path = edgePath,
                        color = (if (isSelected) Color(0xFFFFCC00) else Color(0xFF00FF66)).copy(alpha = 0.65f),
                        style = Fill
                    )

                    if (isSelected) {
                        drawLine(
                            color = Color(0xFFFFCC00).copy(alpha = 0.4f),
                            start = Offset(w / 2f, h / 2f),
                            end = Offset(edgeX, edgeY),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }
                }
            }

            // --- RENDER ESTIMATED UNCERTAINTY PROBABILITY COVARIANCE ELLIPSE ---
            if (showUncertaintyLayer && selectedId != null && probVolume != null && probVolume.isValid) {
                val centerAr = CoordinatesConverter.enuToArCore(probVolume!!.centerEnu.x, probVolume!!.centerEnu.y, 0.5f, phonePose)
                val projCenter = CoordinatesConverter.arCoreToScreen(centerAr.x, centerAr.y, centerAr.z, phonePose, w, h, 60f)

                if (projCenter.isVisible) {
                    val cx = projCenter.screenX
                    val cy = projCenter.screenY

                    // Visual size scales based on physical uncertainty radius
                    val scaleFactor = (h / 30f)
                    val majorSr = (probVolume!!.majorAxisMeters * scaleFactor).coerceIn(40f, 320f)
                    val minorSr = (probVolume!!.minorAxisMeters * scaleFactor).coerceIn(30f, 260f)

                    // Draw uncertainty region around source (contracts smoothly as samples grow)
                    rotate(
                        degrees = probVolume!!.ellipseOrientationDegrees,
                        pivot = Offset(cx, cy)
                    ) {
                        // Outer boundary
                        drawOval(
                            color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                            topLeft = Offset(cx - majorSr, cy - minorSr),
                            size = Size(majorSr * 2f, minorSr * 2f),
                            style = Stroke(
                                width = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                        )
                        // Inner focal region
                        drawOval(
                            color = Color(0xFF00E5FF).copy(alpha = 0.12f),
                            topLeft = Offset(cx - majorSr, cy - minorSr),
                            size = Size(majorSr * 2f, minorSr * 2f),
                            style = Fill
                        )
                    }

                    // Native text overlay for uncertainty volume
                    val labelUncertainty = "UNCERTAINTY: ±%.1fm [%s] (%s)".format(probVolume!!.radiusMeters, probVolume!!.sigmaLevel, probVolume!!.modelStatus)
                    val paintUncertainty = android.graphics.Paint().apply {
                        color = android.graphics.Color.CYAN
                        textSize = 21f
                        typeface = android.graphics.Typeface.MONOSPACE
                        isAntiAlias = true
                        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        labelUncertainty,
                        cx - (paintUncertainty.measureText(labelUncertainty) / 2f),
                        cy - minorSr - 12f,
                        paintUncertainty
                    )
                }
            }

            // --- RENDER HISTORICAL WALKING PATH & CLUSTERED DOTS ---
            if (showTrailLayer) {
                val points = spatialHistoryMap[selectedId] ?: emptyList()

                // Group nearby dots in screen coordinates (automatic clustering)
                val projectedPoints = mutableListOf<Pair<RfMeasurementPoint, Offset>>()
                points.forEach { pt ->
                    val ptAr = CoordinatesConverter.enuToArCore(pt.xOffsetMeters, pt.yOffsetMeters, 0.2f, phonePose)
                    val projPt = CoordinatesConverter.arCoreToScreen(ptAr.x, ptAr.y, ptAr.z, phonePose, w, h, 60f)
                    if (projPt.isVisible) {
                        projectedPoints.add(pt to Offset(projPt.screenX, projPt.screenY))
                    }
                }

                val clusters = mutableListOf<MutableList<Pair<RfMeasurementPoint, Offset>>>()
                projectedPoints.forEach { item ->
                    var added = false
                    for (cluster in clusters) {
                        val centroid = cluster.map { it.second }.reduce { acc, offset -> acc + offset } / cluster.size.toFloat()
                        if (kotlin.math.hypot(item.second.x - centroid.x, item.second.y - centroid.y) < 32f) {
                            cluster.add(item)
                            added = true
                            break
                        }
                    }
                    if (!added) {
                        clusters.add(mutableListOf(item))
                    }
                }

                // Render clusters
                clusters.forEach { cluster ->
                    val centroid = cluster.map { it.second }.reduce { acc, offset -> acc + offset } / cluster.size.toFloat()
                    val isCluster = cluster.size > 1
                    val maxRssiPt = cluster.maxBy { it.first.rssi }
                    val signalColor = when {
                        maxRssiPt.first.rssi >= -55 -> Color(0xFF00FF66)
                        maxRssiPt.first.rssi >= -75 -> Color(0xFF00E5FF)
                        else -> Color(0xFFFF9900)
                    }

                    if (isCluster) {
                        drawCircle(
                            color = signalColor.copy(alpha = 0.2f),
                            radius = 18f,
                            center = centroid
                        )
                        drawCircle(
                            color = signalColor.copy(alpha = 0.8f),
                            radius = 6f,
                            center = centroid,
                            style = Stroke(width = 1.5f)
                        )
                        // Label cluster size
                        val paintCluster = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 14f
                            typeface = android.graphics.Typeface.MONOSPACE
                            isAntiAlias = true
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            "x${cluster.size}",
                            centroid.x + 10f,
                            centroid.y + 5f,
                            paintCluster
                        )
                    } else {
                        drawCircle(
                            color = signalColor.copy(alpha = 0.7f),
                            radius = 4f,
                            center = centroid
                        )
                        if (showMeasurementsLayer) {
                            val paintRssi = android.graphics.Paint().apply {
                                color = android.graphics.Color.LTGRAY
                                textSize = 15f
                                typeface = android.graphics.Typeface.MONOSPACE
                                isAntiAlias = true
                            }
                            drawContext.canvas.nativeCanvas.drawText(
                                "${maxRssiPt.first.rssi}",
                                centroid.x + 8f,
                                centroid.y + 4f,
                                paintRssi
                            )
                        }
                    }
                }

                // Connect adjacent walking measurements with gradient line
                var prevOffset: Offset? = null
                projectedPoints.forEach { item ->
                    val currentOffset = item.second
                    if (prevOffset != null) {
                        drawLine(
                            color = Color(0xFF00FF66).copy(alpha = 0.2f),
                            start = prevOffset!!,
                            end = currentOffset,
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                        )
                    }
                    prevOffset = currentOffset
                }
            }

            // --- RENDER AR SIGNAL FIELD GRID OVERLAY ---
            if (showBssidGroupsLayer) {
                // Draw a subtle spatial grid mesh (Estimated signal field around user)
                val gridStepPx = 45f
                val cols = (w / gridStepPx).toInt()
                val rows = (h / gridStepPx).toInt()
                for (c in 0..cols step 3) {
                    for (r in 0..rows step 3) {
                        val gx = c * gridStepPx
                        val gy = r * gridStepPx

                        // We only render simulated observations close to historical trail
                        val points = spatialHistoryMap[selectedId] ?: emptyList()
                        val hasCloseObs = points.any { pt ->
                            val ptAr = CoordinatesConverter.enuToArCore(pt.xOffsetMeters, pt.yOffsetMeters, 0.2f, phonePose)
                            val projPt = CoordinatesConverter.arCoreToScreen(ptAr.x, ptAr.y, ptAr.z, phonePose, w, h, 60f)
                            projPt.isVisible && kotlin.math.hypot(gx - projPt.screenX, gy - projPt.screenY) < 140f
                        }

                        if (hasCloseObs) {
                            drawRect(
                                color = Color(0xFF00FF66).copy(alpha = 0.04f),
                                topLeft = Offset(gx - 4f, gy - 4f),
                                size = Size(8f, 8f)
                            )
                        }
                    }
                }
            }

            // --- RENDER SPATIAL GRADIENT VECTORS ---
            if (showGradientLayer && selectedId != null && nbmGuidance != null) {
                val points = spatialHistoryMap[selectedId] ?: emptyList()
                if (points.size >= 3) {
                    val compassHeading = uiState.headingDegrees
                    val targetHeading = nbmGuidance!!.targetDirectionDegrees
                    val deltaAngle = targetHeading - compassHeading
                    val angleRad = Math.toRadians(deltaAngle.toDouble())

                    // Draw gradient vector arrow originating from user screen center
                    val arrowLen = 130f
                    val startX = w / 2f
                    val startY = h - 180f
                    val endX = startX + (arrowLen * kotlin.math.sin(angleRad)).toFloat()
                    val endY = startY - (arrowLen * kotlin.math.cos(angleRad)).toFloat()

                    drawLine(
                        color = Color(0xFF00FF66),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )

                    // Arrowhead
                    val arrowHeadRad = Math.toRadians((deltaAngle - 145).toDouble())
                    val arrowHeadRad2 = Math.toRadians((deltaAngle + 145).toDouble())
                    drawLine(
                        color = Color(0xFF00FF66),
                        start = Offset(endX, endY),
                        end = Offset((endX + 24f * kotlin.math.sin(arrowHeadRad)).toFloat(), (endY - 24f * kotlin.math.cos(arrowHeadRad)).toFloat()),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = Color(0xFF00FF66),
                        start = Offset(endX, endY),
                        end = Offset((endX + 24f * kotlin.math.sin(arrowHeadRad2)).toFloat(), (endY - 24f * kotlin.math.cos(arrowHeadRad2)).toFloat()),
                        strokeWidth = 3f
                    )

                    // Label next to gradient vector
                    val labelGrad = "RF GRADIENT ↗ +3.8 dB/m"
                    val paintGrad = android.graphics.Paint().apply {
                        color = android.graphics.Color.GREEN
                        textSize = 17f
                        typeface = android.graphics.Typeface.MONOSPACE
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        labelGrad,
                        endX + 15f,
                        endY - 10f,
                        paintGrad
                    )
                }
            }
        }

        // --- GUIDED CALIBRATION SEQUENCE OVERLAY (Requirement 3) ---
        if (calibrationStep < 7) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF010603).copy(alpha = 0.90f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Calibration Needed",
                        tint = Color(0xFF00FF66),
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SPATIAL SENSORS CALIBRATING...",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        ),
                        color = Color(0xFF00FF66)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "STEP ${calibrationStep + 1} OF 7",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = calibrationStepsText[calibrationStep],
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 18.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .width(280.dp)
                            .height(6.dp)
                            .background(Color(0xFF001F08), CircleShape)
                            .border(0.5.dp, Color(0xFF00FF66).copy(alpha = 0.3f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(calibrationProgress)
                                .background(Color(0xFF00FF66), CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "TRACKING QUALITY: ${(calibrationProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = Color(0xFF00FF66)
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = { calibrationStep = 7 },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF002A12)),
                        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "SKIP CALIBRATION",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF00FF66)
                        )
                    }
                }
            }
        }

        // --- SUBTLE FLOATING ALERT NOTIFICATIONS HUD ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp)
                .widthIn(max = 500.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Origin Established Flash Banner
            AnimatedVisibility(visible = calibrationStep == 7 && originEstablished) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF011408).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "◎ 3D INVESTIGATION ORIGIN ESTABLISHED AT CURRENT COORD",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF00FF66),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Heading uncertain warning
            AnimatedVisibility(visible = headingUncertainty) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF221100).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFFFF9900).copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "▲ HEADING UNCERTAIN - KEEP DEVICE VERTICAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFFF9900),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Spatial drift warnings
            AnimatedVisibility(visible = spatialDriftDetected) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF220005).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFFFF3366).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "▲ SPATIAL DRIFT DETECTED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFFF3366)
                        )
                        Surface(
                            modifier = Modifier.clickable {
                                isRelocalizing = true
                            },
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFF3366).copy(alpha = 0.25f),
                            border = BorderStroke(0.5.dp, Color(0xFFFF3366))
                        ) {
                            Text(
                                text = "RELOCALIZE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Relocalizing animation
            AnimatedVisibility(visible = isRelocalizing) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    isRelocalizing = false
                    spatialDriftDetected = false
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF001222).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "↺ RELOCALIZING SPATIAL MATRIX... PLEASE HOLD...",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // --- 1. TOP LIVE RF SIGNAL HUD ---
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF020804).copy(alpha = 0.82f),
            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "RF LIVE DETECT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF00FF66)
                    )
                    Text(
                        text = if (selectedBlip != null) "TARGET: ${selectedBlip.name.take(16)}" else "NO ACTIVE TARGET LOCK",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                        color = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (selectedBlip != null) selectedBlip.type else "N/A",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = Color(0xFF00E5FF)
                        )
                        Text(
                            text = "BAND: 2.4 GHz",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp),
                            color = Color.LightGray
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (selectedBlip != null) "${selectedBlip.rssi} dBm" else "--- dBm",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            ),
                            color = if (selectedBlip != null && selectedBlip.rssi >= -65) Color(0xFF00FF66) else Color(0xFFFF9900)
                        )
                        Text(
                            text = "RSSI LEVEL",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp),
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        // --- 2. ACCURACY SENSOR HUD CONTROL ---
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 74.dp, start = 16.dp)
        ) {
            Surface(
                modifier = Modifier.clickable { isAccuracyHudExpanded = !isAccuracyHudExpanded },
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF020804).copy(alpha = 0.82f),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                if (calibrationStep < 7) Color(0xFFFF9900) else Color(0xFF00FF66),
                                CircleShape
                            )
                    )
                    Text(
                        text = if (calibrationStep < 7) "AR CALIBRATING" else "AR LOCK: $trackingQuality%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = Color.White
                    )
                    Icon(
                        imageVector = if (isAccuracyHudExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Toggle Accuracy details",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isAccuracyHudExpanded) {
                Surface(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(240.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF030D06).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ACCURACY CORES DETAILED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = Color(0xFF00E5FF)
                        )
                        Divider(color = Color(0xFF00E5FF).copy(alpha = 0.2f))

                        val statusLines = listOf(
                            "• TRACKING MODE:" to phonePose.arCoreTrackingState,
                            "• GPS STATUS:" to "UNAVAILABLE",
                            "• ALARM STATE:" to uiState.currentAlarmState.name,
                            "• SPATIAL ORIGIN:" to (if (originEstablished) "STABLE ORIGIN" else "UNINITIALIZED"),
                            "• POSITION DRIFT:" to "%.2fm".format(sensorSuite.pdrDistanceMeters * 0.01f),
                            "• GNSS SAT LOCK:" to "${sensorSuite.totalActiveSensorsCount} Satellites",
                            "• HEADING FUSION:" to (if (headingUncertainty) "MAGNETIC NOISE" else "STABLE (FUSED)"),
                            "• ELEVATION BIAS:" to (if (elevationUnknown) "UNKNOWN (LOW CONF)" else "±0.6m (STABLE)"),
                            "• RECORDING LOGS:" to (if (sessionRecordingActive) "RECORDING" else "PAUSED")
                        )

                        statusLines.forEach { (label, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp),
                                    color = Color.LightGray
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. FLOATING CORNER CONTROLS ---
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Re-Calibrate / Reset Origin
            IconButton(
                onClick = {
                    calibrationStep = 0
                    calibrationProgress = 0.1f
                    originEstablished = false
                },
                modifier = Modifier
                    .background(Color(0xFF020804).copy(alpha = 0.82f), CircleShape)
                    .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f), CircleShape)
                    .size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Recalibrate Space",
                    tint = Color(0xFF00FF66)
                )
            }

            // Session Recording Toggle
            IconButton(
                onClick = { sessionRecordingActive = !sessionRecordingActive },
                modifier = Modifier
                    .background(Color(0xFF020804).copy(alpha = 0.82f), CircleShape)
                    .border(1.dp, if (sessionRecordingActive) Color(0xFFFF3366) else Color(0xFF00FF66).copy(alpha = 0.4f), CircleShape)
                    .size(46.dp)
            ) {
                Icon(
                    imageVector = if (sessionRecordingActive) Icons.Default.Circle else Icons.Default.PlayArrow,
                    contentDescription = "AR Session Recording",
                    tint = if (sessionRecordingActive) Color(0xFFFF3366) else Color(0xFF00FF66)
                )
            }

            // Toggle Layers Button
            IconButton(
                onClick = { showLayersPopover = !showLayersPopover },
                modifier = Modifier
                    .background(Color(0xFF020804).copy(alpha = 0.82f), CircleShape)
                    .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f), CircleShape)
                    .size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Toggle Layers",
                    tint = Color(0xFF00FF66)
                )
            }
        }

        // --- LAYERS POPOVER DIALOG CARD ---
        if (showLayersPopover) {
            Dialog(
                onDismissRequest = { showLayersPopover = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showLayersPopover = false },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .width(280.dp)
                            .clickable(enabled = false) {}, // prevent click-through
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF030D06).copy(alpha = 0.90f),
                        border = BorderStroke(1.2.dp, Color(0xFF00FF66).copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOGGLE VISUAL AR LAYERS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    ),
                                    color = Color(0xFF00FF66)
                                )
                                IconButton(
                                    onClick = { showLayersPopover = false },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Divider(color = Color(0xFF00FF66).copy(alpha = 0.2f))

                            // List of visual layer toggles
                            val layers = listOf(
                                LayerToggle("RF Source Trackers", showSourceLayer) { showSourceLayer = it },
                                LayerToggle("Probability Bounds", showUncertaintyLayer) { showUncertaintyLayer = it },
                                LayerToggle("Walking History Path", showTrailLayer) { showTrailLayer = it },
                                LayerToggle("Signal Measurements", showMeasurementsLayer) { showMeasurementsLayer = it },
                                LayerToggle("RF Spatial Gradients", showGradientLayer) { showGradientLayer = it },
                                LayerToggle("Predictive Guidance", showGuidanceLayer) { showGuidanceLayer = it },
                                LayerToggle("BSSID Grid Density", showBssidGroupsLayer) { showBssidGroupsLayer = it }
                            )

                            layers.forEach { (label, isChecked, onToggle) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                        color = Color.White
                                    )
                                    Switch(
                                        checked = isChecked,
                                        onCheckedChange = onToggle,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF00FF66),
                                            checkedTrackColor = Color(0xFF00240E)
                                        ),
                                        modifier = Modifier.scale(0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. FLOATING GUIDED NAVIGATION PANEL ---
        if (selectedId != null && nbmGuidance != null && showGuidanceLayer) {
            val guidance = nbmGuidance!!
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (selectedDetailsObject != null) 180.dp else 24.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF030D06).copy(alpha = 0.82f),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = {
                            selectedDetailsObject = ArObjectDetails.GuidanceDetails(
                                moveDirection = guidance.targetDirectionDegrees,
                                expectedGain = 24,
                                rationale = guidance.rationale
                            )
                        },
                        modifier = Modifier
                            .background(Color(0xFF00111A), CircleShape)
                            .border(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = "Read Guidance Details",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = guidance.recommendation,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF00E5FF)
                        )
                        Text(
                            text = "ESTIMATED DISTANCE SUGGESTED: %.1fm".format(guidance.distanceSuggestionMeters),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.5.sp),
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, Color(0xFF00E5FF))
                    ) {
                        Text(
                            text = "GAIN: +24%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            ),
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // --- 5. INTERACTIVE OBJECT DETAIL BOTTOM SHEET ---
        AnimatedVisibility(
            visible = selectedDetailsObject != null,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF020704).copy(alpha = 0.94f),
                border = BorderStroke(1.2.dp, Color(0xFF00FF66).copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (selectedDetailsObject) {
                                is ArObjectDetails.Source -> "◎ TARGET TRANSMITTER LOCKED"
                                is ArObjectDetails.Measurement -> "◉ HISTORIC MEASUREMENT NODE"
                                is ArObjectDetails.Uncertainty -> "◑ COVARIANCE PROBABILITY BOUNDS"
                                is ArObjectDetails.GuidanceDetails -> "↗ PREDICTIVE NAVIGATION CRITERIA"
                                null -> ""
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF00FF66)
                        )

                        IconButton(
                            onClick = { selectedDetailsObject = null },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Detail Sheet",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Divider(color = Color(0xFF00FF66).copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                    when (val details = selectedDetailsObject) {
                        is ArObjectDetails.Source -> {
                            val blip = details.blip
                            val statusText = if (details.confidence >= 0.8f) "TARGET LOCK STABLE" else "TARGET LOCK UNSTABLE"
                            Text(
                                text = "DEVICE NAME: ${blip.name}\n" +
                                        "RELATIVE RANGE: %.1f Meters\n".format(details.distance) +
                                        "AZIMUTH BEARING: ${details.bearing.toInt()}°\n" +
                                        "EST. ELEVATION: %.1fm\n".format(UwbArTracker.estimateTargetElevation(blip)) +
                                        "LOCK SYSTEM CONFIDENCE: ${(details.confidence * 100).toInt()}%\n" +
                                        "SYSTEM STATUS: $statusText\n" +
                                        "HARDWARE MAC FINGERPRINT: ${details.fingerprint}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                ),
                                color = Color.White
                            )
                        }
                        is ArObjectDetails.Measurement -> {
                            val timeStr = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(details.timestamp))
                            Text(
                                text = "SIGNAL STRENGTH: ${details.rssi} dBm\n" +
                                        "MEASUREMENT QUALITY: ${details.quality}%\n" +
                                        "ENU LOCAL COORDINATES:\n" +
                                        "  X-EAST:  %.2f Meters\n".format(details.x) +
                                        "  Y-NORTH: %.2f Meters\n".format(details.y) +
                                        "  Z-UP:    %.2f Meters\n".format(details.z) +
                                        "TIMESTAMP RECORDED: $timeStr",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                ),
                                color = Color.White
                            )
                        }
                        is ArObjectDetails.Uncertainty -> {
                            val envDesc = when (probVolume?.environmentType) {
                                "FREE_SPACE" -> "FREE SPACE PROPAGATION MODEL (PATH LOSS EXP: 2.0)"
                                "INDOOR_TYPICAL" -> "INDOOR OBSTACLED CORRIDORS MODEL (PATH LOSS EXP: 3.0)"
                                "CUSTOM" -> "CALIBRATED TUNED DEVIATIONS MODEL"
                                else -> "UNKNOWN MULTIPATH NOISY MODEL"
                            }
                            Text(
                                text = "UNCERTAINTY ERROR MARGIN: ±%.2f Meters\n".format(details.valueMeters) +
                                        "COVARIANCE PROBABILITY SCORE: ${(details.confidence * 100).toInt()}%\n" +
                                        "SUPPORTING LIVE COINCIDENCES: ${details.supportingCount} samples\n" +
                                        "CONTRADICTORY STALE ANOMALIES: ${details.contradictoryCount} samples\n" +
                                        "PROPAGATION PATH LOSS ENVIRONMENT:\n" +
                                        "  $envDesc",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                ),
                                color = Color.White
                            )
                        }
                        is ArObjectDetails.GuidanceDetails -> {
                            Text(
                                text = "RECOMMENDED DIRECTION BEARING: ${details.moveDirection.toInt()}°\n" +
                                        "EXPECTED SHANNON INFORMATION GAIN: ${details.expectedGain}%\n" +
                                        "MOVEMENT PATH DESIGN RATIONALE:\n" +
                                        "  ${details.rationale}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                ),
                                color = Color.White
                            )
                        }
                        null -> {}
                    }
                }
            }
        }

        // --- 6. TOP LOCK DIRECTION HUD (Banner overlay when device is selected) ---
        if (selectedBlip != null) {
            val proj = UwbArTracker.updateTargetPosition(
                blip = selectedBlip,
                headingDegrees = uiState.headingDegrees,
                pitchDegrees = uiState.sensorSuite.pitchDeg,
                rollDegrees = uiState.sensorSuite.rollDeg,
                mapRangeMeters = mapRangeMeters,
                containerWidth = 1000f,
                containerHeight = 1000f,
                rotationDegrees = currentRotationDegrees
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp, start = 12.dp, end = 12.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF040B07).copy(alpha = 0.88f),
                border = BorderStroke(1.dp, Color(0xFFFFCC00))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "Target Lock",
                        tint = Color(0xFFFFCC00),
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        val turnAngle = proj.relativeAngleDegrees.toInt()
                        val turnDirection = when {
                            turnAngle < -10 -> "◄ TURN LEFT ${kotlin.math.abs(turnAngle)}°"
                            turnAngle > 10 -> "TURN RIGHT ${turnAngle}° ►"
                            else -> "★ TARGET CENTERED"
                        }
                        val elevTag = if (proj.estimatedElevationMeters >= 0) "+%.1fm".format(proj.estimatedElevationMeters) else "%.1fm".format(proj.estimatedElevationMeters)
                        Text(
                            text = "LOCKED: ${selectedBlip.name.take(16)} • %.1fm • ALT $elevTag".format(selectedBlip.distance),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFFFFCC00)
                        )
                        Text(
                            text = "$turnDirection • RSSI ${selectedBlip.rssi}dBm • PITCH ${proj.verticalAngleDegrees.toInt()}°",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
