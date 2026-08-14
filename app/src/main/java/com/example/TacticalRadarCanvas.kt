package com.example

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun TacticalRadarCanvas(
    headingDegrees: Float,
    blips: List<RadarBlip>,
    nearestBlipId: String?,
    selectedTargetDeviceId: String? = null,
    perimeterThresholdMeters: Float,
    mapRangeMeters: Float = 30.0f,
    isMapMaximized: Boolean = false,
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onSetMapRange: (Float) -> Unit = {},
    onToggleMaximizeMap: () -> Unit = {},
    onOpenFullScreenMap: () -> Unit = {},
    onSelectTargetDevice: (String?) -> Unit = {},
    isFloorplanEnabled: Boolean = false,
    onToggleFloorplan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Infinite transition for continuous 360 radar sweep line animation
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

    // Pulse animation for perimeter breach warnings & target locking reticle
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Pre-allocated Paint and Path objects
    val arrowPath = remember { Path() }
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
            android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    val radarGreen = Color(0xFF00FF66)
    val gridDark = Color(0xFF00FF66).copy(alpha = 0.25f)
    val breachRed = Color(0xFFFF3366)
    val wifiGreen = Color(0xFF00FF66)
    val cellRed = Color(0xFFFF3366)
    val bleCyan = Color(0xFF00E5FF)
    val audioYellow = Color(0xFFFFCC00)

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
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("tactical_radar_canvas")
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

            if (isFloorplanEnabled) {
                drawFloorplanOverlay(
                    alpha = 0.5f,
                    radarRangeMeters = maxDistanceRange
                )
            }

            // 1. Outer Bezel & Rotating Compass Grid locked to heading
            rotate(-headingDegrees, pivot = Offset(centerX, centerY)) {
                // Concentric Range Rings (100%, 75%, 50%, 25%)
                drawCircle(color = radarGreen, radius = maxRadius, style = Stroke(width = 3f))
                drawCircle(color = gridDark, radius = maxRadius * 0.75f, style = Stroke(width = 1.5f))
                drawCircle(color = gridDark, radius = maxRadius * 0.50f, style = Stroke(width = 1.5f))
                drawCircle(color = gridDark, radius = maxRadius * 0.25f, style = Stroke(width = 1.5f))

                // Micro-Perimeter Danger Ring
                val perimeterRadius = (perimeterThresholdMeters / maxDistanceRange) * maxRadius
                if (perimeterRadius <= maxRadius) {
                    drawCircle(
                        color = breachRed.copy(alpha = 0.8f),
                        radius = perimeterRadius,
                        style = Stroke(width = 2.5f)
                    )
                }

                // Crosshair lines
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

                // Range ring labels
                if (perimeterRadius <= maxRadius - 20f) {
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

            // 2. Heading Pointer Arrow
            arrowPath.reset()
            arrowPath.moveTo(centerX, centerY - maxRadius - 8f)
            arrowPath.lineTo(centerX - 12f, centerY - maxRadius + 16f)
            arrowPath.lineTo(centerX + 12f, centerY - maxRadius + 16f)
            arrowPath.close()
            drawPath(arrowPath, color = Color.Cyan)

            // 3. Sweeping Radar Arc & Line
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

            // 4. Render Discovered Signal Blips & Target Locking Vector with Deconfliction Engine
            data class PositionedBlip(
                val blip: RadarBlip,
                val rawX: Float,
                val rawY: Float,
                var currentX: Float,
                var currentY: Float,
                val isSelectedTarget: Boolean,
                val isNearestTarget: Boolean
            )

            val positionedList = blips.map { blip ->
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

                PositionedBlip(
                    blip = blip,
                    rawX = rawX,
                    rawY = rawY,
                    currentX = rawX,
                    currentY = rawY,
                    isSelectedTarget = isSelectedTarget,
                    isNearestTarget = isNearestTarget
                )
            }.toMutableList()

            // Iterative Repulsion Deconfliction Pass (Separates overlapping blip dots)
            val minDotDist = 32f
            repeat(4) {
                for (i in positionedList.indices) {
                    for (j in i + 1 until positionedList.size) {
                        val p1 = positionedList[i]
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

                // Clamp to radar display boundary
                for (p in positionedList) {
                    val distFromCenter = kotlin.math.hypot(p.currentX - centerX, p.currentY - centerY)
                    val maxAllowed = maxRadius - 12f
                    if (distFromCenter > maxAllowed && distFromCenter > 0f) {
                        p.currentX = centerX + (p.currentX - centerX) / distFromCenter * maxAllowed
                        p.currentY = centerY + (p.currentY - centerY) / distFromCenter * maxAllowed
                    }
                }
            }

            // Determine top closest blip IDs for clean label decluttering when total node count is large
            val sortedByDist = blips.sortedBy { it.distance }
            val topLabeledIds = if (blips.size > 10) {
                sortedByDist.take(8).map { it.id }.toSet()
            } else {
                blips.map { it.id }.toSet()
            }

            // Draw non-selected blips first, then selected target blip last on top
            val sortedList = positionedList.sortedBy { if (it.isSelectedTarget) 1 else 0 }

            for (p in sortedList) {
                val blip = p.blip
                val blipX = p.currentX
                val blipY = p.currentY
                val isSelectedTarget = p.isSelectedTarget
                val isNearestTarget = p.isNearestTarget

                val nodeColor = when (blip.type.uppercase()) {
                    "WIFI" -> wifiGreen
                    "CELLULAR" -> cellRed
                    "BLE" -> bleCyan
                    "MAGNETIC" -> Color(0xFFFF00FF)
                    else -> audioYellow
                }

                val isBreach = blip.distance < perimeterThresholdMeters

                // If blip was displaced by deconfliction engine, draw connecting tether line to exact raw coordinate
                val shiftDist = kotlin.math.hypot(p.currentX - p.rawX, p.currentY - p.rawY)
                if (shiftDist > 8f) {
                    drawLine(
                        color = nodeColor.copy(alpha = 0.4f),
                        start = Offset(p.rawX, p.rawY),
                        end = Offset(p.currentX, p.currentY),
                        strokeWidth = 1.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                }

                // Breach warning pulse ring
                if (isBreach) {
                    drawCircle(
                        color = breachRed.copy(alpha = 0.6f),
                        radius = 22f * pulseScale,
                        center = Offset(blipX, blipY),
                        style = Stroke(width = 2f)
                    )
                }

                // Target Lock Vector Line from origin (user center) to selected device
                if (isSelectedTarget) {
                    drawLine(
                        color = Color.Yellow,
                        start = Offset(centerX, centerY),
                        end = Offset(blipX, blipY),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )

                    // Target Reticle Box / Circle
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

                    // Reticle Crosshair
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

                    // Locked Target Label Callout
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

                // Bluetooth 6.0 Channel Sounding (CS) Sub-Meter Precision Halo & Reticle
                if (blip.isChannelSoundingCapable && !isSelectedTarget) {
                    val csHaloRadius = ((blip.csEstimatedAccuracyMeters / maxDistanceRange) * maxRadius).coerceIn(8f, 35f)
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.55f),
                        radius = csHaloRadius,
                        center = Offset(blipX, blipY),
                        style = Stroke(width = 1.8f)
                    )
                    val diamondPath = Path().apply {
                        moveTo(blipX, blipY - 12f)
                        lineTo(blipX + 12f, blipY)
                        lineTo(blipX, blipY + 12f)
                        lineTo(blipX - 12f, blipY)
                        close()
                    }
                    drawPath(
                        path = diamondPath,
                        color = Color(0xFF00E5FF),
                        style = Stroke(width = 2f)
                    )
                }

                // Blip Dot
                drawCircle(
                    color = if (isSelectedTarget) Color.Yellow else if (blip.isChannelSoundingCapable) Color(0xFF00E5FF) else nodeColor,
                    radius = if (isSelectedTarget || isNearestTarget) 11f else if (blip.isChannelSoundingCapable) 9f else 8f,
                    center = Offset(blipX, blipY)
                )

                // Smart Label Placement with Directional Text Alignment (Decluttered)
                val shouldDrawLabel = !isSelectedTarget && (isNearestTarget || blip.isChannelSoundingCapable || isBreach || topLabeledIds.contains(blip.id))
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

        // Overlay Range Presets Chips & Full Screen Map Button Top Left
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        imageVector = Icons.Default.TrackChanges,
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
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141A06),
                border = BorderStroke(1.dp, Color.Yellow),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .clickable { onSelectTargetDevice(null) }
                    .testTag("target_lock_active_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = "Target Lock",
                        tint = Color.Yellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "AUDIO SONAR LOCKED TO DEVICE • CLICK TO UNLOCK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Color.Yellow
                    )
                }
            }
        }
    }
}
