package com.example

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Mutable internal tracker representation for 60fps deconfliction calculations without allocations.
 */
private class TrackNode(
    val blip: RadarBlip,
    val rawX: Float,
    val rawY: Float,
    var currentX: Float,
    var currentY: Float,
    val isSelectedTarget: Boolean,
    val isNearestTarget: Boolean
)

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
    modifier: Modifier = Modifier
) {
    // Continuous 360-degree radar sweep beam animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    // Pulse animation for perimeter breach warnings & target reticle
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Pre-allocated Native Paint & Path structures to ensure 0 GC pressure during drawing passes
    val arrowPath = remember { Path() }
    val csDiamondPath = remember { Path() }
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(4f, 4f)) }

    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#00FF66")
            textSize = 24f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val subTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#9900FF66")
            textSize = 19f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val warningTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FF3366")
            textSize = 22f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val blipTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 20f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }

    val targetTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FFCC00")
            textSize = 22f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            isFakeBoldText = true
        }
    }

    val radarGreen = Color(0xFF00FF66)
    val breachRed = Color(0xFFFF3366)
    val wifiGreen = Color(0xFF00FF66)
    val cellRed = Color(0xFFFF3366)
    val bleCyan = Color(0xFF00E5FF)
    val audioYellow = Color(0xFFFFCC00)
    val magneticMagenta = Color(0xFFFF00FF)

    val containerModifier = if (modifier == Modifier) {
        Modifier
            .fillMaxWidth()
            .height(if (isMapMaximized) 460.dp else 340.dp)
    } else {
        modifier
    }

    Column(
        modifier = containerModifier
            .testTag("tactical_radar_container"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Dedicated Top Radar HUD Control Bar (Outside the canvas so it never blocks the radar circle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Range Meter Selector Segmented Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RANGE:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.Gray
                )

                listOf(5f, 15f, 30f, 60f).forEach { rangePreset ->
                    val isSelected = mapRangeMeters.toInt() == rangePreset.toInt()
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) Color(0xFF00FF66) else Color(0xFF0D1D14),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00FF66) else Color(0xFF00FF66).copy(alpha = 0.35f)),
                        modifier = Modifier
                            .clickable { onSetMapRange(rangePreset) }
                            .testTag("range_chip_${rangePreset.toInt()}m")
                    ) {
                        Text(
                            text = "${rangePreset.toInt()}m",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (isSelected) Color.Black else Color(0xFF00FF66),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Quick Status Chips & Actions Bar
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Radar Grid Mode Pill & Config Button
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (radarGridMode) {
                        RadarGridMode.POLAR -> Color(0xFF0D281E)
                        RadarGridMode.TACTICAL_MGRS -> Color(0xFF0C2433)
                        RadarGridMode.COVERAGE_ZONES -> Color(0xFF28200C)
                        RadarGridMode.OFF -> Color(0xFF1E1416)
                    },
                    border = BorderStroke(
                        1.dp,
                        when (radarGridMode) {
                            RadarGridMode.POLAR -> Color(0xFF00FF66).copy(alpha = 0.6f)
                            RadarGridMode.TACTICAL_MGRS -> Color(0xFF00E5FF).copy(alpha = 0.7f)
                            RadarGridMode.COVERAGE_ZONES -> Color(0xFFFFCC00).copy(alpha = 0.7f)
                            RadarGridMode.OFF -> Color(0xFFFF3366).copy(alpha = 0.5f)
                        }
                    ),
                    modifier = Modifier
                        .clickable { onCycleRadarGridMode() }
                        .testTag("radar_hud_grid_mode_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Grid4x4,
                            contentDescription = "Radar Grid Mode",
                            tint = when (radarGridMode) {
                                RadarGridMode.POLAR -> Color(0xFF00FF66)
                                RadarGridMode.TACTICAL_MGRS -> Color(0xFF00E5FF)
                                RadarGridMode.COVERAGE_ZONES -> Color(0xFFFFCC00)
                                RadarGridMode.OFF -> Color(0xFFFF3366)
                            },
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = radarGridMode.shortLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = when (radarGridMode) {
                                RadarGridMode.POLAR -> Color(0xFF00FF66)
                                RadarGridMode.TACTICAL_MGRS -> Color(0xFF00E5FF)
                                RadarGridMode.COVERAGE_ZONES -> Color(0xFFFFCC00)
                                RadarGridMode.OFF -> Color(0xFFFF3366)
                            }
                        )
                    }
                }

                // Grid Settings Gear Icon
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F1E17),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clickable { onOpenRadarGridConfig() }
                        .testTag("radar_hud_grid_config_gear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Configure Grid",
                        tint = Color(0xFF00FF66),
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 3.dp)
                            .size(12.dp)
                    )
                }

                // Radar Gain / Sensitivity Boost Chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (radarBoostLevel != RadarBoostLevel.NORMAL_1X) radarBoostLevel.badgeColor.copy(alpha = 0.25f) else Color(0xFF0F1E17),
                    border = BorderStroke(1.dp, radarBoostLevel.badgeColor),
                    modifier = Modifier
                        .clickable { onCycleRadarBoost() }
                        .testTag("radar_hud_boost_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Radar Boost",
                            tint = radarBoostLevel.badgeColor,
                            modifier = Modifier.size(12.dp)
                        )
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

                // Quick HUD Focus Mode Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isFocusModeEnabled) Color(0xFFFFCC00) else Color(0xFF0F1E17),
                    border = BorderStroke(1.dp, if (isFocusModeEnabled) Color(0xFFFFE066) else Color(0xFF00FF66).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clickable { onToggleFocusMode() }
                        .testTag("radar_hud_focus_pill")
                ) {
                    Text(
                        text = if (isFocusModeEnabled) "FOCUS" else "FOC",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (isFocusModeEnabled) Color.Black else Color(0xFF00FF66),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                    )
                }

                // Quick Limit cycle button
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF101C16),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clickable {
                            val next = when (maxVisibleDevices) {
                                3 -> 5
                                5 -> 10
                                10 -> 25
                                25 -> 0
                                else -> 3
                            }
                            onSetMaxDevices(next)
                        }
                        .testTag("radar_hud_limit_cycle_button")
                ) {
                    Text(
                        text = if (maxVisibleDevices == 0) "ALL" else "L:$maxVisibleDevices",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                    )
                }

                // Floorplan toggle button
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isFloorplanEnabled) Color(0xFFFFCC00) else Color(0xFF122230),
                    border = BorderStroke(1.dp, if (isFloorplanEnabled) Color.Yellow else Color(0xFF00E5FF).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clickable { onToggleFloorplan() }
                        .testTag("toggle_floorplan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Toggle Floorplan",
                        tint = if (isFloorplanEnabled) Color.Black else Color(0xFF00E5FF),
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 3.dp)
                            .size(13.dp)
                    )
                }

                // Zoom In
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0D1D14),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .size(26.dp)
                        .clickable { onZoomIn() }
                        .testTag("radar_zoom_in_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom In",
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Zoom Out
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0D1D14),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .size(26.dp)
                        .clickable { onZoomOut() }
                        .testTag("radar_zoom_out_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom Out",
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Full Screen Map / Maximize
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF081C26),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                    modifier = Modifier
                        .size(26.dp)
                        .clickable { onOpenFullScreenMap() }
                        .testTag("open_fullscreen_map_pill_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Full Screen Map",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // Radar Canvas Box - 100% Unobstructed Scope Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // High-Performance Dual-Layer Canvas Architecture:
            // 1. drawBehind handles static/cached background grid, compass rings, and floorplan
            // 2. Canvas handles dynamic 60fps sweep rays, active device tracks, and target vectors
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tactical_radar_canvas")
                    .drawBehind {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val maxRadius = min(centerX, centerY) - 28f
                        val maxDistanceRange = mapRangeMeters.coerceAtLeast(2.0f)

                    // Optional GPU-accelerated Tactical Floorplan Blueprint Layer
                    if (isFloorplanEnabled) {
                        drawFloorplanOverlay(
                            alpha = 0.5f,
                            radarRangeMeters = maxDistanceRange
                        )
                    }

                    // Background Radar Concentric Rings & Cardinal Grid (Heading-Locked)
                    rotate(-headingDegrees, pivot = Offset(centerX, centerY)) {
                        // Outer Boundary Bezel
                        drawCircle(color = radarGreen, radius = maxRadius, style = Stroke(width = 3f))

                        val gridColor = radarGreen.copy(alpha = radarGridOpacity.coerceIn(0.05f, 0.9f))
                        val subGridColor = radarGreen.copy(alpha = (radarGridOpacity * 0.5f).coerceIn(0.02f, 0.45f))
                        val axisColor = Color(0xFF00E5FF).copy(alpha = (radarGridOpacity * 1.5f).coerceIn(0.15f, 0.95f))

                        when (radarGridMode) {
                            RadarGridMode.POLAR -> {
                                // Concentric Distance Range Rings (100%, 75%, 50%, 25%, and subtle intermediate sub-rings)
                                drawCircle(color = gridColor, radius = maxRadius * 0.75f, style = Stroke(width = 1.5f))
                                drawCircle(color = gridColor, radius = maxRadius * 0.50f, style = Stroke(width = 1.5f))
                                drawCircle(color = gridColor, radius = maxRadius * 0.25f, style = Stroke(width = 1.5f))

                                // Subtle intermediate range rings
                                drawCircle(color = subGridColor, radius = maxRadius * 0.875f, style = Stroke(width = 1f, pathEffect = dashEffect))
                                drawCircle(color = subGridColor, radius = maxRadius * 0.625f, style = Stroke(width = 1f, pathEffect = dashEffect))
                                drawCircle(color = subGridColor, radius = maxRadius * 0.375f, style = Stroke(width = 1f, pathEffect = dashEffect))
                                drawCircle(color = subGridColor, radius = maxRadius * 0.125f, style = Stroke(width = 1f, pathEffect = dashEffect))

                                // 8-Point Radial Spokes (45, 135, 225, 315)
                                for (angle in listOf(45.0, 135.0, 225.0, 315.0)) {
                                    val rad = Math.toRadians(angle)
                                    val cosA = cos(rad).toFloat()
                                    val sinA = sin(rad).toFloat()
                                    drawLine(
                                        color = subGridColor,
                                        start = Offset(centerX - maxRadius * sinA, centerY + maxRadius * cosA),
                                        end = Offset(centerX + maxRadius * sinA, centerY - maxRadius * cosA),
                                        strokeWidth = 1f,
                                        pathEffect = dashEffect
                                    )
                                }

                                // Polar Primary Crosshair Axes
                                drawLine(
                                    color = axisColor,
                                    start = Offset(centerX, centerY - maxRadius),
                                    end = Offset(centerX, centerY + maxRadius),
                                    strokeWidth = 1.5f
                                )
                                drawLine(
                                    color = axisColor,
                                    start = Offset(centerX - maxRadius, centerY),
                                    end = Offset(centerX + maxRadius, centerY),
                                    strokeWidth = 1.5f
                                )

                                // Polar Distance Tick Marks along axes
                                if (showDistanceTicks) {
                                    for (step in 1..4) {
                                        val frac = step * 0.25f
                                        val distLabel = "${(maxDistanceRange * frac).toInt()}m"
                                        val r = maxRadius * frac
                                        drawLine(
                                            color = gridColor,
                                            start = Offset(centerX - 6f, centerY - r),
                                            end = Offset(centerX + 6f, centerY - r),
                                            strokeWidth = 2f
                                        )
                                        drawContext.canvas.nativeCanvas.drawText(
                                            distLabel,
                                            centerX + 22f,
                                            centerY - r + 6f,
                                            subTextPaint
                                        )
                                    }
                                }
                            }

                            RadarGridMode.TACTICAL_MGRS -> {
                                // Rectangular / Cartesian MGRS Metric Grid Mesh
                                val gridDivisions = 8
                                val stepPx = (maxRadius * 2f) / gridDivisions
                                val mgrsColor = Color(0xFF00E5FF).copy(alpha = radarGridOpacity.coerceIn(0.08f, 0.8f))
                                val mgrsSubColor = Color(0xFF00E5FF).copy(alpha = (radarGridOpacity * 0.45f).coerceIn(0.03f, 0.4f))

                                for (i in 1 until gridDivisions) {
                                    val offsetFromLeft = (centerX - maxRadius) + i * stepPx
                                    val offsetFromTop = (centerY - maxRadius) + i * stepPx

                                    val dx = offsetFromLeft - centerX
                                    val chordHalfY = sqrt((maxRadius * maxRadius - dx * dx).coerceAtLeast(0f))
                                    if (chordHalfY > 2f) {
                                        drawLine(
                                            color = if (i == gridDivisions / 2) axisColor else mgrsSubColor,
                                            start = Offset(offsetFromLeft, centerY - chordHalfY),
                                            end = Offset(offsetFromLeft, centerY + chordHalfY),
                                            strokeWidth = if (i == gridDivisions / 2) 1.5f else 1f,
                                            pathEffect = if (i == gridDivisions / 2) null else dashEffect
                                        )
                                    }

                                    val dy = offsetFromTop - centerY
                                    val chordHalfX = sqrt((maxRadius * maxRadius - dy * dy).coerceAtLeast(0f))
                                    if (chordHalfX > 2f) {
                                        drawLine(
                                            color = if (i == gridDivisions / 2) axisColor else mgrsSubColor,
                                            start = Offset(centerX - chordHalfX, offsetFromTop),
                                            end = Offset(centerX + chordHalfX, offsetFromTop),
                                            strokeWidth = if (i == gridDivisions / 2) 1.5f else 1f,
                                            pathEffect = if (i == gridDivisions / 2) null else dashEffect
                                        )
                                    }
                                }

                                // Concentric reference rings
                                drawCircle(color = mgrsColor, radius = maxRadius * 0.50f, style = Stroke(width = 1f, pathEffect = dashEffect))

                                if (showDistanceTicks) {
                                    val meterPerGrid = (maxDistanceRange * 2f / gridDivisions)
                                    drawContext.canvas.nativeCanvas.drawText(
                                        "GRID: ${meterPerGrid.toInt()}m/div",
                                        centerX,
                                        centerY + maxRadius - 16f,
                                        subTextPaint
                                    )
                                }
                            }

                            RadarGridMode.COVERAGE_ZONES -> {
                                // Theoretical RF Propagation Zones & Signal Envelopes
                                val immediateFrac = (3.0f / maxDistanceRange).coerceIn(0.10f, 0.40f)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFF00FF66).copy(alpha = radarGridOpacity * 0.35f), Color.Transparent),
                                        center = Offset(centerX, centerY),
                                        radius = maxRadius * immediateFrac
                                    ),
                                    radius = maxRadius * immediateFrac,
                                    style = Fill
                                )
                                drawCircle(
                                    color = Color(0xFF00FF66).copy(alpha = radarGridOpacity * 0.8f),
                                    radius = maxRadius * immediateFrac,
                                    style = Stroke(width = 1.5f, pathEffect = dashEffect)
                                )

                                val losFrac = (12.0f / maxDistanceRange).coerceIn(0.35f, 0.70f)
                                drawCircle(
                                    color = Color(0xFF00E5FF).copy(alpha = radarGridOpacity * 0.6f),
                                    radius = maxRadius * losFrac,
                                    style = Stroke(width = 1.5f, pathEffect = dashEffect)
                                )

                                drawCircle(
                                    color = Color(0xFFFFCC00).copy(alpha = radarGridOpacity * 0.5f),
                                    radius = maxRadius * 0.85f,
                                    style = Stroke(width = 1.5f, pathEffect = dashEffect)
                                )

                                drawLine(color = gridColor, start = Offset(centerX, centerY - maxRadius), end = Offset(centerX, centerY + maxRadius), strokeWidth = 1f)
                                drawLine(color = gridColor, start = Offset(centerX - maxRadius, centerY), end = Offset(centerX + maxRadius, centerY), strokeWidth = 1f)

                                if (showDistanceTicks) {
                                    drawContext.canvas.nativeCanvas.drawText("BLE PROXIMITY (<3m)", centerX, centerY - maxRadius * immediateFrac + 14f, subTextPaint)
                                    drawContext.canvas.nativeCanvas.drawText("RF LOS ZONE", centerX, centerY - maxRadius * losFrac + 14f, subTextPaint)
                                }
                            }

                            RadarGridMode.OFF -> {
                                drawLine(
                                    color = gridColor.copy(alpha = 0.15f),
                                    start = Offset(centerX, centerY - 15f),
                                    end = Offset(centerX, centerY + 15f),
                                    strokeWidth = 1.5f
                                )
                                drawLine(
                                    color = gridColor.copy(alpha = 0.15f),
                                    start = Offset(centerX - 15f, centerY),
                                    end = Offset(centerX + 15f, centerY),
                                    strokeWidth = 1.5f
                                )
                            }
                        }

                        // Micro-Perimeter Danger Ring
                        val perimeterRadius = (perimeterThresholdMeters / maxDistanceRange) * maxRadius
                        if (perimeterRadius.toDouble() <= maxRadius.toDouble()) {
                            drawCircle(
                                color = breachRed.copy(alpha = 0.85f),
                                radius = perimeterRadius,
                                style = Stroke(width = 2.5f)
                            )
                        }

                        // Optional Coverage Rings for active Blip Types
                        if (showCoverageRings && radarGridMode != RadarGridMode.OFF) {
                            val bleMaxRadius = (15.0f / maxDistanceRange).coerceIn(0.15f, 0.95f) * maxRadius
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = (radarGridOpacity * 0.35f).coerceIn(0.02f, 0.25f)),
                                radius = bleMaxRadius,
                                style = Stroke(width = 1f, pathEffect = dashEffect)
                            )
                        }

                        // Compass Cardinal Direction Labels (N, E, S, W)
                        drawContext.canvas.nativeCanvas.drawText("N", centerX, centerY - maxRadius - 12f, textPaint)
                        drawContext.canvas.nativeCanvas.drawText("S", centerX, centerY + maxRadius + 30f, textPaint)
                        drawContext.canvas.nativeCanvas.drawText("E", centerX + maxRadius + 24f, centerY + 8f, textPaint)
                        drawContext.canvas.nativeCanvas.drawText("W", centerX - maxRadius - 24f, centerY + 8f, textPaint)

                        // Distance Scale Annotations
                        if (perimeterRadius.toDouble() <= (maxRadius - 20f).toDouble()) {
                            drawContext.canvas.nativeCanvas.drawText(
                                "${perimeterThresholdMeters.toInt()}m BREACH",
                                centerX,
                                centerY - perimeterRadius - 6f,
                                warningTextPaint
                            )
                        }
                        if (showDistanceTicks && radarGridMode != RadarGridMode.OFF) {
                            drawContext.canvas.nativeCanvas.drawText(
                                "${maxDistanceRange.toInt()}m",
                                centerX,
                                centerY - maxRadius + 22f,
                                textPaint
                            )
                        }
                    }

                    // Static Heading Orientation Arrow (Cyan)
                    arrowPath.reset()
                    arrowPath.moveTo(centerX, centerY - maxRadius - 8f)
                    arrowPath.lineTo(centerX - 12f, centerY - maxRadius + 16f)
                    arrowPath.lineTo(centerX + 12f, centerY - maxRadius + 16f)
                    arrowPath.close()
                    drawPath(arrowPath, color = Color.Cyan)
                }
                .pointerInput(mapRangeMeters) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom > 1.05f) {
                            onZoomIn()
                        } else if (zoom < 0.95f) {
                            onZoomOut()
                        }
                    }
                }
                .pointerInput(blips, headingDegrees, mapRangeMeters) {
                    detectTapGestures { tapOffset ->
                        val maxRadius = min(size.width / 2f, size.height / 2f) - 44f
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val effectiveMaxRange = mapRangeMeters.coerceAtLeast(2.0f)

                        var closestBlipId: String? = null
                        var minDistanceToTap = Float.MAX_VALUE

                        for (blip in blips) {
                            val rawRatio = (blip.distance / effectiveMaxRange).coerceIn(0.01f, 4.0f)
                            val radialFraction = if (rawRatio <= 1.0f) {
                                Math.pow(rawRatio.toDouble(), 0.70).toFloat()
                            } else {
                                0.88f + (0.09f * (1.0f - kotlin.math.exp(-0.7f * (rawRatio - 1.0f))))
                            }
                            val cappedRadius = (radialFraction * maxRadius).coerceAtMost(maxRadius - 10f)
                            val trueAngleRad = Math.toRadians((blip.targetAngleOffset - headingDegrees).toDouble())
                            val bx = centerX + (cappedRadius * sin(trueAngleRad)).toFloat()
                            val by = centerY - (cappedRadius * cos(trueAngleRad)).toFloat()

                            val dist = kotlin.math.hypot((bx - tapOffset.x).toDouble(), (by - tapOffset.y).toDouble()).toFloat()
                            if (dist < minDistanceToTap) {
                                minDistanceToTap = dist
                                closestBlipId = blip.id
                            }
                        }

                        if (closestBlipId != null && minDistanceToTap <= 54f) {
                            onSelectTargetDevice(closestBlipId)
                        } else {
                            onSelectTargetDevice(null)
                        }
                    }
                }
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val maxRadius = min(centerX, centerY) - 44f
            val maxDistanceRange = mapRangeMeters.coerceAtLeast(2.0f)

            // Dynamic Layer 1: Sweeping Radar Arc & Line (60 FPS Rotation)
            rotate(sweepAngle, pivot = Offset(centerX, centerY)) {
                val sweepRad = Math.toRadians(0.0)
                val sweepEndX = centerX + (maxRadius * sin(sweepRad)).toFloat()
                val sweepEndY = centerY - (maxRadius * cos(sweepRad)).toFloat()

                drawLine(
                    color = radarGreen,
                    start = Offset(centerX, centerY),
                    end = Offset(sweepEndX, sweepEndY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )

                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.85f to Color.Transparent,
                        1.0f to radarGreen.copy(alpha = 0.28f),
                        center = Offset(centerX, centerY)
                    ),
                    startAngle = -90f,
                    sweepAngle = 60f,
                    useCenter = true,
                    topLeft = Offset(centerX - maxRadius, centerY - maxRadius),
                    size = Size(maxRadius * 2, maxRadius * 2)
                )
            }

            // Dynamic Layer 2: Real-time Multi-Device Track Projection with Deconfliction
            val positionedList = ArrayList<TrackNode>(blips.size)
            for (blip in blips) {
                val rawRatio = (blip.distance / maxDistanceRange).coerceIn(0.01f, 4.0f)
                val radialFraction = if (rawRatio <= 1.0f) {
                    Math.pow(rawRatio.toDouble(), 0.70).toFloat()
                } else {
                    0.88f + (0.09f * (1.0f - kotlin.math.exp(-0.7f * (rawRatio - 1.0f))))
                }
                val cappedRadius = (radialFraction * maxRadius).coerceAtMost(maxRadius - 10f)
                val trueAngleRad = Math.toRadians((blip.targetAngleOffset - headingDegrees).toDouble())

                val rawX = centerX + (cappedRadius * sin(trueAngleRad)).toFloat()
                val rawY = centerY - (cappedRadius * cos(trueAngleRad)).toFloat()

                val isSelectedTarget = selectedTargetDeviceId != null &&
                        (blip.id == selectedTargetDeviceId || blip.name == selectedTargetDeviceId)
                val isNearestTarget = selectedTargetDeviceId == null && blip.id == nearestBlipId

                positionedList.add(
                    TrackNode(
                        blip = blip,
                        rawX = rawX,
                        rawY = rawY,
                        currentX = rawX,
                        currentY = rawY,
                        isSelectedTarget = isSelectedTarget,
                        isNearestTarget = isNearestTarget
                    )
                )
            }

            // High-Performance Repulsion Pass (Separates overlapping dot clusters)
            val minDotDist = 32f
            val nodeCount = positionedList.size
            if (nodeCount in 2..60) {
                repeat(3) {
                    for (i in 0 until nodeCount) {
                        val p1 = positionedList[i]
                        for (j in i + 1 until nodeCount) {
                            val p2 = positionedList[j]
                            var dx = p2.currentX - p1.currentX
                            var dy = p2.currentY - p1.currentY
                            var dist = kotlin.math.hypot(dx, dy)

                            if (dist < minDotDist) {
                                if (dist < 0.1f) {
                                    dx = 1.0f
                                    dy = 1.0f
                                    dist = 1.414f
                                }
                                val overlap = (minDotDist - dist) / 2f
                                val nx = dx / dist
                                val ny = dy / dist

                                if (!p1.isSelectedTarget) {
                                    p1.currentX -= nx * overlap
                                    p1.currentY -= ny * overlap
                                }
                                if (!p2.isSelectedTarget) {
                                    p2.currentX += nx * overlap
                                    p2.currentY += ny * overlap
                                }
                            }
                        }
                    }

                    // Boundary containment
                    val maxAllowed = maxRadius - 12f
                    for (k in 0 until nodeCount) {
                        val p = positionedList[k]
                        val distFromCenter = kotlin.math.hypot(p.currentX - centerX, p.currentY - centerY)
                        if (distFromCenter > maxAllowed && distFromCenter > 0f) {
                            val scale = maxAllowed / distFromCenter
                            p.currentX = centerX + (p.currentX - centerX) * scale
                            p.currentY = centerY + (p.currentY - centerY) * scale
                        }
                    }
                }
            }

            // Decluttering Label Selection
            val sortedByDist = blips.sortedBy { it.distance }
            val topLabeledIds = if (isHudDeclutterEnabled) {
                if (blips.size > 5) sortedByDist.take(3).map { it.id }.toSet() else blips.map { it.id }.toSet()
            } else {
                if (blips.size > 10) sortedByDist.take(8).map { it.id }.toSet() else blips.map { it.id }.toSet()
            }

            // Render Device Tracks: non-selected tracks first, locked target track on top
            positionedList.sortWith(compareBy { if (it.isSelectedTarget) 1 else 0 })

            for (p in positionedList) {
                val blip = p.blip
                val blipX = p.currentX
                val blipY = p.currentY
                val isSelectedTarget = p.isSelectedTarget
                val isNearestTarget = p.isNearestTarget

                val nodeColor = when (blip.type.uppercase()) {
                    "WIFI" -> wifiGreen
                    "CELLULAR" -> cellRed
                    "BLE" -> bleCyan
                    "MAGNETIC" -> magneticMagenta
                    else -> audioYellow
                }

                val isBreach = blip.distance < perimeterThresholdMeters

                // Deconfliction displacement tether dashed line
                val shiftDist = kotlin.math.hypot(p.currentX - p.rawX, p.currentY - p.rawY)
                if (shiftDist > 8f) {
                    drawLine(
                        color = nodeColor.copy(alpha = 0.4f),
                        start = Offset(p.rawX, p.rawY),
                        end = Offset(p.currentX, p.currentY),
                        strokeWidth = 1.2f,
                        pathEffect = dashEffect
                    )
                }

                // Perimeter breach warning ring
                if (isBreach) {
                    drawCircle(
                        color = breachRed.copy(alpha = 0.6f),
                        radius = 22f * pulseScale,
                        center = Offset(blipX, blipY),
                        style = Stroke(width = 2f)
                    )
                }

                // Target Lock Vector & Reticle
                if (isSelectedTarget) {
                    // Vector line from radar center (user) to selected device
                    drawLine(
                        color = Color.Yellow,
                        start = Offset(centerX, centerY),
                        end = Offset(blipX, blipY),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )

                    // Target Reticle
                    drawCircle(
                        color = Color.Yellow,
                        radius = 26f * pulseScale,
                        center = Offset(blipX, blipY),
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = Color.Yellow,
                        radius = 16f,
                        center = Offset(blipX, blipY),
                        style = Stroke(width = 2.5f)
                    )

                    // Reticle Crosshairs
                    drawLine(
                        color = Color.Yellow,
                        start = Offset(blipX - 32f, blipY),
                        end = Offset(blipX + 32f, blipY),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.Yellow,
                        start = Offset(blipX, blipY - 32f),
                        end = Offset(blipX, blipY + 32f),
                        strokeWidth = 2f
                    )

                    // Locked Target Distance & Name Label
                    targetTextPaint.textAlign = android.graphics.Paint.Align.LEFT
                    drawContext.canvas.nativeCanvas.drawText(
                        "LOCKED TARGET: ${blip.name} (%.1fm)".format(blip.distance),
                        blipX + 18f,
                        blipY - 10f,
                        targetTextPaint
                    )
                } else if (isNearestTarget) {
                    drawCircle(
                        color = Color.Yellow.copy(alpha = 0.8f),
                        radius = 20f,
                        center = Offset(blipX, blipY),
                        style = Stroke(width = 2f)
                    )
                }

                // Bluetooth 6.0 Channel Sounding Precision Diamond & Accuracy Halo
                if (blip.isChannelSoundingCapable && !isSelectedTarget) {
                    val csHaloRadius = ((blip.csEstimatedAccuracyMeters / maxDistanceRange) * maxRadius).coerceIn(8f, 35f)
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.55f),
                        radius = csHaloRadius,
                        center = Offset(blipX, blipY),
                        style = Stroke(width = 1.8f)
                    )

                    csDiamondPath.reset()
                    csDiamondPath.moveTo(blipX, blipY - 12f)
                    csDiamondPath.lineTo(blipX + 12f, blipY)
                    csDiamondPath.lineTo(blipX, blipY + 12f)
                    csDiamondPath.lineTo(blipX - 12f, blipY)
                    csDiamondPath.close()
                    drawPath(
                        path = csDiamondPath,
                        color = Color(0xFF00E5FF),
                        style = Stroke(width = 2f)
                    )
                }

                // Track Node Dot
                drawCircle(
                    color = if (isSelectedTarget) Color.Yellow else if (blip.isChannelSoundingCapable) Color(0xFF00E5FF) else nodeColor,
                    radius = if (isSelectedTarget || isNearestTarget) 11f else if (blip.isChannelSoundingCapable) 9f else 8f,
                    center = Offset(blipX, blipY)
                )

                // Direction-Aware Decluttered Label Rendering
                val shouldDrawLabel = if (isHudDeclutterEnabled) {
                    !isSelectedTarget && (isNearestTarget || blip.isChannelSoundingCapable || isBreach || topLabeledIds.contains(blip.id))
                } else {
                    !isSelectedTarget && (isNearestTarget || blip.isChannelSoundingCapable || isBreach || topLabeledIds.contains(blip.id))
                }

                if (shouldDrawLabel) {
                    val floorBadge = when {
                        blip.estimatedZOffsetMeters > 1.2f -> " [↑ FLOOR ABOVE]"
                        blip.estimatedZOffsetMeters < -1.2f -> " [↓ FLOOR BELOW]"
                        else -> ""
                    }
                    val labelText = if (blip.isChannelSoundingCapable) {
                        "${blip.name.take(10)} (%.1fm ±%.2fm CS)%s".format(blip.distance, blip.csEstimatedAccuracyMeters, floorBadge)
                    } else {
                        "${blip.name.take(12)} (%.1fm)%s".format(blip.distance, floorBadge)
                    }

                    val paint = if (blip.isChannelSoundingCapable) targetTextPaint else blipTextPaint
                    val isRightHalf = blipX >= centerX

                    if (isRightHalf) {
                        paint.textAlign = android.graphics.Paint.Align.LEFT
                        drawContext.canvas.nativeCanvas.drawText(
                            labelText,
                            blipX + 14f,
                            blipY + 5f,
                            paint
                        )
                    } else {
                        paint.textAlign = android.graphics.Paint.Align.RIGHT
                        drawContext.canvas.nativeCanvas.drawText(
                            labelText,
                            blipX - 14f,
                            blipY + 5f,
                            paint
                        )
                    }
                }
            }
        }

        // Target Lock Info Status Banner Bottom Center (Unobtrusive floating pill)
        if (selectedTargetDeviceId != null) {
            val lockedBlip = blips.find { it.id == selectedTargetDeviceId || it.name == selectedTargetDeviceId }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF141A06),
                    border = BorderStroke(1.dp, Color.Yellow),
                    modifier = Modifier
                        .clickable { onSelectTargetDevice(null) }
                        .testTag("target_lock_active_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Target Lock",
                            tint = Color.Yellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "LOCKED • UNLOCK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = Color.Yellow
                        )
                    }
                }

                if (lockedBlip != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF00FF66),
                        border = BorderStroke(1.dp, Color.White),
                        modifier = Modifier
                            .clickable { onTriggerAiPinpoint(lockedBlip) }
                            .testTag("target_hud_ai_pinpoint_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = "AI 3D Pinpoint",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "🎯 AI 3D PINPOINT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                ),
                                color = Color.Black
                            )
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
                                text = "Distance Estimation & Signal Envelopes",
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

                RadarGridMode.values().forEach { mode ->
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
                            text = "Render meter distance numerical labels along radar rings & axes",
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
