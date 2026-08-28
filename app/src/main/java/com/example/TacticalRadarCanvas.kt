package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Tactical 2.5D Isometric Radar Canvas & Decoupled HUD Pipeline.
 *
 * Key Architecture:
 * 1. 2.5D Isometric Projection: 35° perspective tilt ground plane with Z-stem elevation markers.
 * 2. User Origin Reticle: Distinct tactical chevron rotated by physical phone heading + 60° FOV cone.
 * 3. Spatial Clustering & Micro-Glyphs: Density-deconflicted clusters [ n ] + threat-graded vector glyphs.
 * 4. Zero-Allocation Render Loop: Pre-allocated Path, Paint, and geometry buffers.
 * 5. Decoupled HUD Leader Line: Tapping a node extends a geometric leader line to a floating HUD card.
 * 6. Snail Trails: High-decay phosphor history buffer showing 5-point movement trajectory.
 */
@Composable
fun TacticalRadarCanvas(
    headingDegrees: Float,
    blips: List<RadarBlip>,
    nearestBlipId: String?,
    selectedTargetDeviceId: String? = null,
    perimeterThresholdMeters: Float,
    mapRangeMeters: Float = 30.0f,
    isMapMaximized: Boolean = false,
    isHudDeclutterEnabled: Boolean = true,
    isFocusModeEnabled: Boolean = false,
    maxVisibleDevices: Int = 10,
    radarBoostLevel: RadarBoostLevel = RadarBoostLevel.NORMAL_1X,
    radarGridMode: RadarGridMode = RadarGridMode.POLAR,
    radarGridOpacity: Float = 0.25f,
    showCoverageRings: Boolean = true,
    showDistanceTicks: Boolean = true,
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onSetMapRange: (Float) -> Unit = {},
    onToggleMaximizeMap: () -> Unit = {},
    onOpenFullScreenMap: () -> Unit = {},
    onSelectTargetDevice: (String?) -> Unit = {},
    isFloorplanEnabled: Boolean = false,
    onToggleFloorplan: () -> Unit = {},
    onToggleFocusMode: () -> Unit = {},
    onSetMaxDevices: (Int) -> Unit = {},
    onCycleRadarBoost: () -> Unit = {},
    onTriggerAiPinpoint: (RadarBlip) -> Unit = {},
    onCycleRadarGridMode: () -> Unit = {},
    onOpenRadarGridConfig: () -> Unit = {},
    recenterTriggerCount: Int = 0,
    isGradientVectorEnabled: Boolean = false,
    isInspectionCrosshairEnabled: Boolean = false,
    navigationFollowMode: String = "FOLLOW",
    onSetNavigationFollowMode: (String) -> Unit = {},
    arModeActive: Boolean = false,
    measurementHistory: List<RfMeasurementPoint> = emptyList(),
    isDeveloperModeEnabled: Boolean = false,
    onRecordMeasurementTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Local gesture-driven 3D camera transformation states (Pan, Zoom, Orbit/Yaw, Pitch/Tilt)
    var localZoom by remember { mutableFloatStateOf(1.0f) }
    var localPan by remember { mutableStateOf(Offset.Zero) }
    var localYawRotation by remember { mutableFloatStateOf(0.0f) }
    var pitchAngleDeg by remember { mutableFloatStateOf(35.0f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Isometric / 3D Perspective Transformer & Dynamic Spatial Clusterer engines
    val transformer = remember { IsometricRadarTransformer(pitchAngleDeg = 35.0f, elevationScaleFactor = 1.3f) }
    val clusterer = remember { SpatialClusterer(clusterDistanceThresholdPx = 36.0f, maxTrailPoints = 5) }

    // Recenter effect on demand
    LaunchedEffect(recenterTriggerCount) {
        localZoom = 1.0f
        localPan = Offset.Zero
        localYawRotation = 0.0f
        pitchAngleDeg = 35.0f
    }

    // Continuous 360-degree radar sweep beam animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep25D")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    // Pulse animation for perimeter breach warnings & target reticle
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Pre-allocated Native Geometry Structures (Zero-Allocation Render Loop)
    val userChevronPath = remember { Path() }
    val fovPath = remember { Path() }
    val hexagonPath = remember { Path() }
    val towerPath = remember { Path() }
    val tagPath = remember { Path() }
    val edgeArrowPath = remember { Path() }
    val leaderLinePath = remember { Path() }
    val groundAnchorDiscPath = remember { Path() }
    val reticlePulsePath = remember { Path() }
    val gridRingPath = remember { Path() }
    val scratchMatrix = remember { FloatArray(16) }

    val zStemDashedEffect = remember { PathEffect.dashPathEffect(floatArrayOf(5f, 4f)) }
    val gridDashedEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 6f)) }

    // Pre-allocated Native Paints
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#00FF66")
            textSize = 22f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val clusterCountPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 20f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    val ringDistanceTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#00FF66")
            textSize = 16f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.LEFT
        }
    }

    val density = LocalDensity.current
    val hudAnchorBottomOffsetPx = remember(density) { with(density) { 70.dp.toPx() } }

    val effectiveRange = mapRangeMeters.coerceAtLeast(1.5f)
    val ringDistanceLabels = remember(effectiveRange) {
        (1..4).map { step ->
            val ringDistance = step * 0.25f * effectiveRange
            if (ringDistance >= 10f) "${ringDistance.toInt()}m" else "%.1fm".format(ringDistance)
        }
    }

    val fovGradientColors = remember {
        listOf(
            Color(0xFF00FF66).copy(alpha = 0.16f),
            Color(0xFF00FF66).copy(alpha = 0.04f),
            Color.Transparent
        )
    }

    val containerModifier = if (modifier == Modifier) {
        Modifier
            .fillMaxWidth()
            .height(if (isMapMaximized) 480.dp else 360.dp)
    } else {
        modifier
    }

    Column(
        modifier = containerModifier.testTag("tactical_radar_container"),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Top Radar Tactical Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Range Meter Selector Segmented Chips (5m, 15m, 30m, 60m)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RANGE:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color(0xFF00FF66)
                )

                listOf(5f, 15f, 30f, 60f).forEach { rangePreset ->
                    val isSelected = kotlin.math.abs(mapRangeMeters - rangePreset) < 0.5f
                    val label = "${rangePreset.toInt()}m"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF00FF66) else Color(0xFF0D1D14),
                        border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF00FF66).copy(alpha = 0.35f)),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clickable { onSetMapRange(rangePreset) }
                            .testTag("range_chip_$label")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (isSelected) Color.Black else Color(0xFF00FF66)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Quick Status Chips & Actions Bar
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Radar Grid Mode Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (radarGridMode) {
                        RadarGridMode.POLAR -> Color(0xFF0D281E)
                        RadarGridMode.TACTICAL_MGRS -> Color(0xFF0C2433)
                        RadarGridMode.COVERAGE_ZONES -> Color(0xFF28200C)
                        RadarGridMode.OFF -> Color(0xFF1E1416)
                    },
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.6f)),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onCycleRadarGridMode() }
                        .testTag("radar_hud_grid_mode_pill")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Grid4x4,
                                contentDescription = "Radar Grid Mode",
                                tint = Color(0xFF00FF66),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = radarGridMode.shortLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF00FF66)
                            )
                        }
                    }
                }

                // Grid Settings Gear Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F1E17),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onOpenRadarGridConfig() }
                        .testTag("radar_hud_grid_config_gear_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Configure Grid",
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Radar Boost Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (radarBoostLevel != RadarBoostLevel.NORMAL_1X) radarBoostLevel.badgeColor.copy(alpha = 0.25f) else Color(0xFF0F1E17),
                    border = BorderStroke(1.dp, radarBoostLevel.badgeColor),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onCycleRadarBoost() }
                        .testTag("radar_hud_boost_chip")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = radarBoostLevel.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = radarBoostLevel.badgeColor
                        )
                    }
                }

                // Zoom In
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0D1D14),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onZoomIn() }
                        .testTag("radar_zoom_in_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom In",
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Zoom Out
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0D1D14),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onZoomOut() }
                        .testTag("radar_zoom_out_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom Out",
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Fullscreen Toggle
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF081C26),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onOpenFullScreenMap() }
                        .testTag("open_fullscreen_map_pill_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Full Screen Map",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Radar Canvas Box with Decoupled Floating HUD Layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Decoupled background clustering state (computed on Dispatchers.Default, not in the 60fps render loop)
            var currentClusteredFrame by remember { mutableStateOf(ClusteredRadarFrame.EMPTY) }

            LaunchedEffect(
                blips,
                effectiveRange,
                headingDegrees,
                localYawRotation,
                localZoom,
                localPan,
                pitchAngleDeg,
                selectedTargetDeviceId,
                nearestBlipId,
                perimeterThresholdMeters,
                canvasSize
            ) {
                if (canvasSize.width > 0 && canvasSize.height > 0) {
                    withContext(Dispatchers.Default) {
                        val width = canvasSize.width.toFloat()
                        val height = canvasSize.height.toFloat()
                        val centerX = width / 2f
                        val centerY = height / 2f + 18f
                        val maxRadius = min(centerX, centerY) - 32f

                        transformer.pitchAngleDeg = pitchAngleDeg
                        val frame = clusterer.clusterTargets(
                            blips = blips,
                            transformer = transformer,
                            mapRangeMeters = effectiveRange,
                            centerX = centerX,
                            centerY = centerY,
                            maxRadius = maxRadius,
                            headingRotationDeg = headingDegrees - localYawRotation,
                            zoomFactor = localZoom,
                            panOffset = localPan,
                            selectedTargetId = selectedTargetDeviceId,
                            nearestBlipId = nearestBlipId,
                            perimeterThresholdMeters = perimeterThresholdMeters
                        )
                        currentClusteredFrame = frame
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tactical_radar_canvas")
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            var lastTapTime = 0L
                            var touchStartTime = 0L
                            var touchStartPos = Offset.Zero
                            val maxTapDistancePx = 40f
                            val maxTapTargetDistancePx = 110f

                            while (true) {
                                val event = awaitPointerEvent()
                                val changes = event.changes
                                val downPointers = changes.filter { it.pressed }
                                val count = downPointers.size

                                if (count == 1) {
                                    val pointer = downPointers[0]
                                    if (!pointer.previousPressed) {
                                        touchStartTime = System.currentTimeMillis()
                                        touchStartPos = pointer.position
                                    } else {
                                        val dragDelta = pointer.position - pointer.previousPosition
                                        if (dragDelta != Offset.Zero) {
                                            pointer.consume()
                                            onSetNavigationFollowMode("FREE")
                                            localPan = localPan + dragDelta
                                        }
                                    }
                                } else if (count >= 2) {
                                    val p0 = downPointers[0]
                                    val p1 = downPointers[1]

                                    if (p0.previousPressed && p1.previousPressed) {
                                        val prevPos0 = p0.previousPosition
                                        val prevPos1 = p1.previousPosition
                                        val currPos0 = p0.position
                                        val currPos1 = p1.position

                                        // 1. Two-finger Vertical Drag -> Pitch / Tilt (Clamped 15° to 75°)
                                        val prevCenter = (prevPos0 + prevPos1) / 2f
                                        val currCenter = (currPos0 + currPos1) / 2f
                                        val verticalDelta = currCenter.y - prevCenter.y
                                        if (kotlin.math.abs(verticalDelta) > 0.4f) {
                                            pitchAngleDeg = (pitchAngleDeg - verticalDelta * 0.35f).coerceIn(15f, 75f)
                                            onSetNavigationFollowMode("FREE")
                                        }

                                        // 2. Two-finger Pinch -> Zoom Scale
                                        val prevDist = (prevPos0 - prevPos1).getDistance()
                                        val currDist = (currPos0 - currPos1).getDistance()
                                        if (prevDist > 10f && currDist > 10f) {
                                            val zoomRatio = currDist / prevDist
                                            localZoom = (localZoom * zoomRatio).coerceIn(0.4f, 8.0f)
                                            onSetNavigationFollowMode("FREE")
                                        }

                                        // 3. Two-finger Twist / Angle -> Orbit/Yaw
                                        val prevAngle = Math.toDegrees(atan2((prevPos1.y - prevPos0.y).toDouble(), (prevPos1.x - prevPos0.x).toDouble())).toFloat()
                                        val currAngle = Math.toDegrees(atan2((currPos1.y - currPos0.y).toDouble(), (currPos1.x - currPos0.x).toDouble())).toFloat()
                                        var angleDelta = currAngle - prevAngle
                                        while (angleDelta > 180f) angleDelta -= 360f
                                        while (angleDelta < -180f) angleDelta += 360f
                                        if (kotlin.math.abs(angleDelta) > 0.2f && kotlin.math.abs(angleDelta) < 35f) {
                                            localYawRotation += angleDelta
                                            onSetNavigationFollowMode("FREE")
                                        }

                                        p0.consume()
                                        p1.consume()
                                    }
                                }

                                // Tap & Double-Tap detection on pointer release
                                val upPointers = changes.filter { !it.pressed && it.previousPressed }
                                if (upPointers.isNotEmpty() && downPointers.isEmpty()) {
                                    val releasedPointer = upPointers[0]
                                    val moveDist = (releasedPointer.position - touchStartPos).getDistance()
                                    val totalDuration = System.currentTimeMillis() - touchStartTime
                                    if (totalDuration < 350L && moveDist < maxTapDistancePx) {
                                        val tapOffset = releasedPointer.position
                                        val now = System.currentTimeMillis()
                                        if (now - lastTapTime < 300L) {
                                            // Double tap: toggle zoom level
                                            localZoom = if (localZoom > 1.2f) 1.0f else 2.2f
                                            lastTapTime = 0L
                                        } else {
                                            lastTapTime = now
                                            val frame = currentClusteredFrame
                                            var hitBlipId: String? = null
                                            var minTapDist = maxTapTargetDistancePx

                                            // Check single nodes
                                            for (node in frame.singleNodes) {
                                                val dist = (node.projected.elevatedScreenPos - tapOffset).getDistance()
                                                if (dist < minTapDist) {
                                                    minTapDist = dist
                                                    hitBlipId = node.blip.id
                                                }
                                            }

                                            // Check cluster badges
                                            for (cluster in frame.clusterNodes) {
                                                val dist = (cluster.elevatedPosition - tapOffset).getDistance()
                                                if (dist < minTapDist) {
                                                    minTapDist = dist
                                                    clusterer.toggleClusterExpanded(cluster.clusterId)
                                                    hitBlipId = cluster.targets.firstOrNull()?.blip?.id
                                                }
                                            }

                                            onSelectTargetDevice(hitBlipId)
                                        }
                                        releasedPointer.consume()
                                    }
                                }
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val centerX = width / 2f
                val centerY = height / 2f + 18f // slight downward offset for 2.5D perspective
                val maxRadius = min(centerX, centerY) - 32f
                val maxDistanceRange = effectiveRange

                val cosPitch = cos(Math.toRadians(pitchAngleDeg.toDouble())).toFloat()
                val frame = currentClusteredFrame

                // 1. Draw 2.5D Isometric Ground Grid Plane (Elliptical perspective rings)
                val groundAlpha = (radarGridOpacity.coerceIn(0.08f, 0.8f))
                val radarColor = Color(0xFF00FF66)

                // Concentric distance rings
                if (radarGridMode == RadarGridMode.POLAR || radarGridMode == RadarGridMode.COVERAGE_ZONES) {
                    for (step in 1..4) {
                        val fraction = step * 0.25f
                        val rx = maxRadius * fraction * localZoom
                        val ry = rx * cosPitch

                        drawOval(
                            color = radarColor.copy(alpha = groundAlpha * 0.45f),
                            topLeft = Offset(centerX + localPan.x - rx, centerY + localPan.y - ry),
                            size = Size(rx * 2, ry * 2),
                            style = Stroke(width = 1.2f, pathEffect = if (step < 4) gridDashedEffect else null)
                        )

                        // Distance Annotation on Ring Edge (Pre-allocated String & Paint)
                        val ringLabel = if (step <= ringDistanceLabels.size) ringDistanceLabels[step - 1] else ""
                        ringDistanceTextPaint.alpha = (groundAlpha * 180).toInt().coerceIn(40, 255)
                        drawContext.canvas.nativeCanvas.drawText(
                            ringLabel,
                            centerX + localPan.x + rx - 14f,
                            centerY + localPan.y - 4f,
                            ringDistanceTextPaint
                        )
                    }

                    // Axial spokes (Crosslines tilted in isometric plane)
                    val axisLen = maxRadius * localZoom
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = groundAlpha * 0.6f),
                        start = Offset(centerX + localPan.x - axisLen, centerY + localPan.y),
                        end = Offset(centerX + localPan.x + axisLen, centerY + localPan.y),
                        strokeWidth = 1.2f
                    )
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = groundAlpha * 0.6f),
                        start = Offset(centerX + localPan.x, centerY + localPan.y - axisLen * cosPitch),
                        end = Offset(centerX + localPan.x, centerY + localPan.y + axisLen * cosPitch),
                        strokeWidth = 1.2f
                    )
                }

                // 2. Draw Ground-Plane Perimeter Breach Ring
                val breachFrac = (perimeterThresholdMeters / maxDistanceRange).coerceIn(0.05f, 1.0f)
                val breachRx = maxRadius * breachFrac * localZoom
                val breachRy = breachRx * cosPitch
                drawOval(
                    color = Color(0xFFFF1744).copy(alpha = 0.55f),
                    topLeft = Offset(centerX + localPan.x - breachRx, centerY + localPan.y - breachRy),
                    size = Size(breachRx * 2, breachRy * 2),
                    style = Stroke(width = 2.0f)
                )

                // 3. Draw 60° Forward Camera Field of View (FOV) Cone on Ground Plane (Zero-Allocation)
                transformer.fillFovPath(
                    path = fovPath,
                    fovArcDeg = 60.0f,
                    rangeMeters = (maxDistanceRange * 0.75f),
                    headingDeg = 0f,
                    mapRangeMeters = maxDistanceRange,
                    centerX = centerX,
                    centerY = centerY,
                    maxRadius = maxRadius,
                    zoomFactor = localZoom,
                    panOffset = localPan
                )
                drawPath(
                    path = fovPath,
                    brush = Brush.radialGradient(
                        colors = fovGradientColors,
                        center = Offset(centerX + localPan.x, centerY + localPan.y),
                        radius = maxRadius * 0.75f * localZoom
                    )
                )
                drawPath(
                    path = fovPath,
                    color = Color(0xFF00FF66).copy(alpha = 0.35f),
                    style = Stroke(width = 1.0f, pathEffect = gridDashedEffect)
                )

                // 4. Sweeping Radar Beam (Isometric Perspective Sweep)
                val sweepRad = Math.toRadians(sweepAngle.toDouble())
                val sweepX = (maxRadius * localZoom * sin(sweepRad)).toFloat()
                val sweepY = -(maxRadius * localZoom * cos(sweepRad)).toFloat() * cosPitch
                drawLine(
                    color = Color(0xFF00FF66).copy(alpha = 0.85f),
                    start = Offset(centerX + localPan.x, centerY + localPan.y),
                    end = Offset(centerX + localPan.x + sweepX, centerY + localPan.y + sweepY),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round
                )

                // 5. Draw Phosphor Trails (Snail Trails) with High Alpha Decay
                for (node in frame.singleNodes) {
                    val trail = node.trailHistory
                    if (trail.size > 1) {
                        for (idx in 0 until trail.size - 1) {
                            val alphaFrac = (idx + 1).toFloat() / trail.size.toFloat()
                            drawLine(
                                color = node.threatGrade.color.copy(alpha = alphaFrac * 0.45f),
                                start = trail[idx],
                                end = trail[idx + 1],
                                strokeWidth = 1.5f * alphaFrac,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // 6. THE PAINTER'S ALGORITHM (Z-DEPTH SORTED IN BACKGROUND CLUSTERING)
                // Render Ground Shadow Anchors, Vertical Z-Stems, and Elevated Micro-Glyphs
                for (node in frame.singleNodes) {
                    val p = node.projected
                    val groundPos = p.groundAnchor
                    val elevatedPos = p.elevatedScreenPos

                    // Ground Shadow Anchor Disc at (x, y, 0)
                    drawOval(
                        color = Color.Black.copy(alpha = 0.65f),
                        topLeft = Offset(groundPos.x - 6f, groundPos.y - 3f),
                        size = Size(12f, 6f)
                    )
                    drawOval(
                        color = node.threatGrade.color.copy(alpha = 0.4f),
                        topLeft = Offset(groundPos.x - 4f, groundPos.y - 2f),
                        size = Size(8f, 4f),
                        style = Stroke(width = 1.0f)
                    )

                    // Vertical Z-Stem Drop Line connecting (x,y,0) to (x,y,z)
                    if (p.isElevated) {
                        drawLine(
                            color = node.threatGrade.color.copy(alpha = 0.65f),
                            start = groundPos,
                            end = elevatedPos,
                            strokeWidth = 1.2f,
                            pathEffect = zStemDashedEffect
                        )
                    }

                    // Low-opacity radial ambient uncertainty fill (5-10% alpha)
                    val ambientRadius = (16f * localZoom).coerceIn(12f, 36f)
                    drawCircle(
                        color = node.threatGrade.color.copy(alpha = 0.08f),
                        radius = ambientRadius,
                        center = elevatedPos
                    )

                    // Peripheral Off-Screen Clamping Arrow Indicator
                    if (p.isClamped) {
                        val angle = p.edgeAngleDeg
                        edgeArrowPath.reset()
                        val ax = elevatedPos.x
                        val ay = elevatedPos.y
                        val radA = Math.toRadians(angle.toDouble())
                        val cosA = cos(radA).toFloat()
                        val sinA = sin(radA).toFloat()

                        edgeArrowPath.moveTo(ax + cosA * 8f, ay + sinA * 8f)
                        edgeArrowPath.lineTo(ax - sinA * 5f, ay + cosA * 5f)
                        edgeArrowPath.lineTo(ax + sinA * 5f, ay - cosA * 5f)
                        edgeArrowPath.close()

                        drawPath(
                            path = edgeArrowPath,
                            color = node.threatGrade.color
                        )
                    }

                    // Render Elevated Threat-Graded Vector Micro-Glyph (12-16dp) at (x, y, z)
                    val glyphCenter = elevatedPos
                    when (node.glyphType) {
                        RadarGlyphType.TRACKER_TAG -> {
                            // Compact circular tag glyph
                            drawCircle(
                                color = node.threatGrade.color,
                                radius = 7f,
                                center = glyphCenter,
                                style = Stroke(width = 2.0f)
                            )
                            drawCircle(
                                color = node.threatGrade.color,
                                radius = 2.5f,
                                center = glyphCenter
                            )
                        }
                        RadarGlyphType.BROADCAST_TOWER -> {
                            // Compact broadcast tower glyph
                            towerPath.reset()
                            towerPath.moveTo(glyphCenter.x, glyphCenter.y - 7f)
                            towerPath.lineTo(glyphCenter.x - 5f, glyphCenter.y + 6f)
                            towerPath.lineTo(glyphCenter.x + 5f, glyphCenter.y + 6f)
                            towerPath.close()
                            drawPath(towerPath, color = node.threatGrade.color, style = Stroke(width = 1.6f))
                            drawCircle(color = node.threatGrade.color, radius = 2f, center = Offset(glyphCenter.x, glyphCenter.y - 7f))
                        }
                        RadarGlyphType.BLE_CORE_DOT -> {
                            // 4dp luminous core dot
                            drawCircle(
                                color = node.threatGrade.color.copy(alpha = 0.4f),
                                radius = 5.5f,
                                center = glyphCenter
                            )
                            drawCircle(
                                color = node.threatGrade.color,
                                radius = 3.5f,
                                center = glyphCenter
                            )
                        }
                        RadarGlyphType.MAGNETIC_ANOMALY, RadarGlyphType.AUDIO_ULTRASONIC -> {
                            drawCircle(
                                color = node.threatGrade.color,
                                radius = 6f,
                                center = glyphCenter,
                                style = Stroke(width = 1.5f)
                            )
                            drawLine(
                                color = node.threatGrade.color,
                                start = Offset(glyphCenter.x - 6f, glyphCenter.y),
                                end = Offset(glyphCenter.x + 6f, glyphCenter.y),
                                strokeWidth = 1.5f
                            )
                        }
                    }

                    // Crisp Targeting Reticle for Locked / Selected Target
                    if (node.isSelected) {
                        drawCircle(
                            color = Color(0xFFFFD600),
                            radius = 18f * pulseScale,
                            center = elevatedPos,
                            style = Stroke(width = 1.8f)
                        )
                        drawCircle(
                            color = Color(0xFFFFD600),
                            radius = 11f,
                            center = elevatedPos,
                            style = Stroke(width = 2.2f)
                        )
                        // Reticle ticks
                        drawLine(
                            color = Color(0xFFFFD600),
                            start = Offset(elevatedPos.x - 22f, elevatedPos.y),
                            end = Offset(elevatedPos.x + 22f, elevatedPos.y),
                            strokeWidth = 1.5f
                        )
                        drawLine(
                            color = Color(0xFFFFD600),
                            start = Offset(elevatedPos.x, elevatedPos.y - 22f),
                            end = Offset(elevatedPos.x, elevatedPos.y + 22f),
                            strokeWidth = 1.5f
                        )
                    }
                }

                // 7. Render Cluster Nodes (Hexagonal Badges "[ n ]") sorted by depth
                for (cluster in frame.clusterNodes) {
                    val pos = cluster.elevatedPosition
                    val hexRadius = 14f

                    hexagonPath.reset()
                    for (i in 0..5) {
                        val rad = Math.toRadians((60 * i - 30).toDouble())
                        val hx = pos.x + (hexRadius * cos(rad)).toFloat()
                        val hy = pos.y + (hexRadius * sin(rad)).toFloat()
                        if (i == 0) hexagonPath.moveTo(hx, hy) else hexagonPath.lineTo(hx, hy)
                    }
                    hexagonPath.close()

                    drawPath(
                        path = hexagonPath,
                        color = cluster.dominantThreat.color.copy(alpha = 0.85f)
                    )
                    drawPath(
                        path = hexagonPath,
                        color = Color.White,
                        style = Stroke(width = 1.5f)
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        "${cluster.count}",
                        pos.x,
                        pos.y + 6f,
                        clusterCountPaint
                    )
                }

                // 8. User Origin Reticle (Tactical Chevron at Ground Origin)
                val originX = centerX + localPan.x
                val originY = centerY + localPan.y

                userChevronPath.reset()
                userChevronPath.moveTo(originX, originY - 14f)
                userChevronPath.lineTo(originX + 10f, originY + 8f)
                userChevronPath.lineTo(originX, originY + 3f)
                userChevronPath.lineTo(originX - 10f, originY + 8f)
                userChevronPath.close()

                drawCircle(
                    color = Color.Black.copy(alpha = 0.7f),
                    radius = 16f,
                    center = Offset(originX, originY)
                )
                drawCircle(
                    color = Color(0xFF00FF66),
                    radius = 16f,
                    center = Offset(originX, originY),
                    style = Stroke(width = 1.5f)
                )
                drawPath(
                    path = userChevronPath,
                    color = Color(0xFF00FF66)
                )

                // 9. Target Lock Leader Line (Connecting locked node to floating bottom HUD anchor)
                val selectedNode = frame.selectedTarget
                if (selectedNode != null) {
                    val nodePos = selectedNode.projected.elevatedScreenPos
                    val hudAnchor = Offset(centerX, height - hudAnchorBottomOffsetPx)
                    val inflectionX = (nodePos.x + hudAnchor.x) / 2f
                    val inflectionY = nodePos.y + (hudAnchor.y - nodePos.y) * 0.6f

                    leaderLinePath.reset()
                    leaderLinePath.moveTo(nodePos.x, nodePos.y)
                    leaderLinePath.lineTo(inflectionX, inflectionY)
                    leaderLinePath.lineTo(hudAnchor.x, hudAnchor.y)

                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.75f),
                        start = nodePos,
                        end = Offset(inflectionX, inflectionY),
                        strokeWidth = 1.5f,
                        pathEffect = zStemDashedEffect
                    )
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.75f),
                        start = Offset(inflectionX, inflectionY),
                        end = hudAnchor,
                        strokeWidth = 1.8f
                    )
                    drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = 3.5f,
                        center = Offset(inflectionX, inflectionY)
                    )
                }
            }

            // Decoupled Bottom Floating HUD Card for Locked Target
            val selectedBlip = blips.find { it.id == selectedTargetDeviceId || it.name == selectedTargetDeviceId }
            androidx.compose.animation.AnimatedVisibility(
                visible = selectedBlip != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (selectedBlip != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0B1914).copy(alpha = 0.95f),
                        border = BorderStroke(1.5.dp, Color(0xFF00FF66)),
                        modifier = Modifier
                            .fillMaxWidth(0.96f)
                            .testTag("target_lock_active_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Target Lock",
                                        tint = Color(0xFF00FF66),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = selectedBlip.name.take(18),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.5.sp
                                        ),
                                        color = Color.White
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF00FF66).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = selectedBlip.type,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = Color(0xFF00FF66),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "DIST: ${selectedBlip.distance.toFeetLabel(1)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = Color(0xFF00E5FF)
                                    )
                                    Text(
                                        text = "RSSI: ${selectedBlip.rssi} dBm",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        ),
                                        color = Color.LightGray
                                    )
                                    if (selectedBlip.estimatedZOffsetMeters != 0f) {
                                        Text(
                                            text = "ΔZ: %+.1fm".format(selectedBlip.estimatedZOffsetMeters),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            color = Color(0xFFFFCC00)
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // AI 3D Pinpoint Button
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF00FF66),
                                    modifier = Modifier
                                        .clickable { onTriggerAiPinpoint(selectedBlip) }
                                        .testTag("target_hud_ai_pinpoint_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CenterFocusStrong,
                                            contentDescription = "AI Pinpoint",
                                            tint = Color.Black,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "AI 3D PINPOINT",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = Color.Black
                                        )
                                    }
                                }

                                // Unlock Button
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1B2B23),
                                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clickable { onSelectTargetDevice(null) }
                                ) {
                                    Text(
                                        text = "UNLOCK",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color(0xFF00FF66),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tactical Dialog for configuring Radar Grid Mode, Opacity, Distance Ticks, and Coverage Zones.
 */
@Composable
fun RadarGridConfigDialog(
    currentMode: RadarGridMode,
    currentOpacity: Float,
    showCoverageRings: Boolean,
    showDistanceTicks: Boolean,
    onSelectMode: (RadarGridMode) -> Unit,
    onSetOpacity: (Float) -> Unit,
    onToggleCoverageRings: () -> Unit,
    onToggleDistanceTicks: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("radar_grid_config_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1612)),
            border = BorderStroke(1.5.dp, Color(0xFF00FF66).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Radar Grid",
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "RADAR GRID OVERLAY",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color(0xFF00FF66)
                            )
                            Text(
                                text = "2.5D Isometric Elevation & Ground Grid",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.Gray
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E3A2B))

                // Grid Style Modes
                Text(
                    text = "GRID PATTERN STYLE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = Color.White
                )

                RadarGridMode.entries.forEach { mode ->
                    val isSelected = mode == currentMode
                    Surface(
                        onClick = { onSelectMode(mode) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF132B20) else Color(0xFF0F1B16),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF00FF66) else Color(0xFF1E3A2B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grid_mode_option_${mode.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectMode(mode) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF00FF66),
                                    unselectedColor = Color.Gray
                                )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isSelected) Color(0xFF00FF66) else Color.White
                                )
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF1E3A2B))

                // Grid Opacity Slider
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GRID LINE OPACITY",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "${(currentOpacity * 100).toInt()}% (${if (currentOpacity < 0.2f) "SUBTLE" else if (currentOpacity < 0.4f) "BALANCED" else "HIGH"})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFF00FF66)
                        )
                    }

                    Slider(
                        value = currentOpacity,
                        onValueChange = onSetOpacity,
                        valueRange = 0.08f..0.75f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00FF66),
                            activeTrackColor = Color(0xFF00FF66),
                            inactiveTrackColor = Color(0xFF1E3A2B)
                        ),
                        modifier = Modifier.testTag("radar_grid_opacity_slider")
                    )
                }

                HorizontalDivider(color = Color(0xFF1E3A2B))

                // Toggles for Sub-Ticks & Coverage Bands
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Distance Metric Annotations",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "Render feet distance numerical labels along radar rings",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = showDistanceTicks,
                        onCheckedChange = { onToggleDistanceTicks() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color(0xFF00FF66)
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Wi-Fi & BLE Coverage Envelopes",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "Display expected theoretical RF propagation radiuses",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = showCoverageRings,
                        onCheckedChange = { onToggleCoverageRings() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color(0xFF00FF66)
                        )
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("close_grid_config_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FF66),
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "APPLY & CLOSE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}

fun Offset.rotateDegrees(angleDegrees: Float): Offset {
    val angleRad = Math.toRadians(angleDegrees.toDouble())
    val cosA = kotlin.math.cos(angleRad).toFloat()
    val sinA = kotlin.math.sin(angleRad).toFloat()
    return Offset(
        x = (x * cosA - y * sinA),
        y = (x * sinA + y * cosA)
    )
}
