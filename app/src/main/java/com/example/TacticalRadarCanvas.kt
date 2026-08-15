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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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
            textSize = 26f
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
    val gridDark = Color(0xFF00FF66).copy(alpha = 0.25f)
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

    Box(
        modifier = containerModifier
            .testTag("tactical_radar_container")
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
                    val maxRadius = min(centerX, centerY) - 44f
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

                        // Concentric Range Rings (75%, 50%, 25%)
                        drawCircle(color = gridDark, radius = maxRadius * 0.75f, style = Stroke(width = 1.5f))
                        drawCircle(color = gridDark, radius = maxRadius * 0.50f, style = Stroke(width = 1.5f))
                        drawCircle(color = gridDark, radius = maxRadius * 0.25f, style = Stroke(width = 1.5f))

                        // Micro-Perimeter Danger Ring
                        val perimeterRadius = (perimeterThresholdMeters / maxDistanceRange) * maxRadius
                        if (perimeterRadius.toDouble() <= maxRadius.toDouble()) {
                            drawCircle(
                                color = breachRed.copy(alpha = 0.8f),
                                radius = perimeterRadius,
                                style = Stroke(width = 2.5f)
                            )
                        }

                        // Polar Crosshair Axes
                        drawLine(
                            color = gridDark,
                            start = Offset(centerX, centerY - maxRadius),
                            end = Offset(centerX, centerY + maxRadius),
                            strokeWidth = 1.5f
                        )
                        drawLine(
                            color = gridDark,
                            start = Offset(centerX - maxRadius, centerY),
                            end = Offset(centerX + maxRadius, centerY),
                            strokeWidth = 1.5f
                        )

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
                        drawContext.canvas.nativeCanvas.drawText(
                            "${maxDistanceRange.toInt()}m",
                            centerX,
                            centerY - maxRadius + 22f,
                            textPaint
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "${(maxDistanceRange * 0.5f).toInt()}m",
                            centerX,
                            centerY - (maxRadius * 0.5f) + 18f,
                            textPaint
                        )
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

        // Overlay Controls Top Right: Zoom In, Zoom Out, Expand Map
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.6f)),
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onZoomIn() }
                    .testTag("radar_zoom_in_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = Color(0xFF00FF66),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.6f)),
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onZoomOut() }
                    .testTag("radar_zoom_out_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = Color(0xFF00FF66),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onToggleMaximizeMap() }
                    .testTag("radar_expand_map_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isMapMaximized) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Resize Map View",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Overlay Range Presets Chips & Quick HUD Pills Top Left
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Radar Boost",
                        tint = radarBoostLevel.badgeColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "BOOST: ${radarBoostLevel.label}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
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
                border = BorderStroke(1.dp, if (isFocusModeEnabled) Color(0xFFFFE066) else Color(0xFF00FF66).copy(alpha = 0.5f)),
                modifier = Modifier
                    .clickable { onToggleFocusMode() }
                    .testTag("radar_hud_focus_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Focus",
                        tint = if (isFocusModeEnabled) Color.Black else Color(0xFF00FF66),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isFocusModeEnabled) "FOCUS: ON" else "FOCUS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (isFocusModeEnabled) Color.Black else Color(0xFF00FF66)
                    )
                }
            }

            // Quick Limit cycle button (e.g. 5 -> 10 -> 25 -> ALL -> 3)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF101C16),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
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
                    text = if (maxVisibleDevices == 0) "LIM: ALL" else "LIM: $maxVisibleDevices",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color(0xFF00E5FF),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            listOf(5f, 15f, 30f, 60f).forEach { rangePreset ->
                val isSelected = mapRangeMeters.toInt() == rangePreset.toInt()
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) Color(0xFF00FF66) else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clickable { onSetMapRange(rangePreset) }
                        .testTag("range_chip_${rangePreset.toInt()}m")
                ) {
                    Text(
                        text = "${rangePreset.toInt()}m",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (isSelected) Color.Black else Color(0xFF00FF66),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isFloorplanEnabled) Color(0xFFFFCC00) else Color(0xFF122230),
                border = BorderStroke(1.dp, if (isFloorplanEnabled) Color.Yellow else Color(0xFF00E5FF).copy(alpha = 0.5f)),
                modifier = Modifier
                    .clickable { onToggleFloorplan() }
                    .testTag("toggle_floorplan_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Toggle Floorplan",
                        tint = if (isFloorplanEnabled) Color.Black else Color(0xFF00E5FF),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isFloorplanEnabled) "FLOORPLAN: ON" else "FLOORPLAN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (isFloorplanEnabled) Color.Black else Color(0xFF00E5FF)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF00E5FF),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                modifier = Modifier
                    .clickable { onOpenFullScreenMap() }
                    .testTag("open_fullscreen_map_pill_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Full Screen Map",
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "FULL SCREEN MAP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.Black
                    )
                }
            }
        }

        // Target Lock Info Status Banner Bottom Center
        if (selectedTargetDeviceId != null) {
            val lockedBlip = blips.find { it.id == selectedTargetDeviceId || it.name == selectedTargetDeviceId }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
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
