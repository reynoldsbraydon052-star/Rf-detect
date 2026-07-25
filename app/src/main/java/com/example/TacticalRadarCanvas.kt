package com.example

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun TacticalRadarCanvas(
    headingDegrees: Float,
    blips: List<RadarBlip>,
    nearestBlipId: String?,
    perimeterThresholdMeters: Float,
    modifier: Modifier = Modifier
) {
    // Infinite transition for continuous 360 radar sweep line animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    // Pulse animation for perimeter breach warnings
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Pre-allocated Paint and Path objects to avoid GC frame drops
    val arrowPath = remember { Path() }
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#00FF66")
            textSize = 28f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val warningTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FF3366")
            textSize = 24f
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

    val radarGreen = Color(0xFF00FF66)
    val gridDark = Color(0xFF00FF66).copy(alpha = 0.25f)
    val breachRed = Color(0xFFFF3366)
    val wifiGreen = Color(0xFF00FF66)
    val cellRed = Color(0xFFFF3366)
    val bleCyan = Color(0xFF00E5FF)
    val audioYellow = Color(0xFFFFCC00)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("tactical_radar_canvas")
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = min(centerX, centerY) - 40f
        val maxDistanceRange = 30f // 30 meters scaling

        // 1. Draw Outer Bezel & Rotating Compass Grid locked to heading
        rotate(-headingDegrees, pivot = Offset(centerX, centerY)) {
            // Concentric Range Rings (50m, 30m, 15m, 5m perimeter, 1.5m)
            drawCircle(color = radarGreen, radius = maxRadius, style = Stroke(width = 3f))
            drawCircle(color = gridDark, radius = maxRadius * 0.75f, style = Stroke(width = 1.5f))
            drawCircle(color = gridDark, radius = maxRadius * 0.50f, style = Stroke(width = 1.5f))

            // Micro-Perimeter Danger Ring (5m threshold proportional to maxDistanceRange)
            val perimeterRadius = (perimeterThresholdMeters / maxDistanceRange) * maxRadius
            drawCircle(
                color = breachRed.copy(alpha = 0.7f),
                radius = perimeterRadius.coerceAtMost(maxRadius),
                style = Stroke(width = 2.5f)
            )

            drawCircle(color = gridDark, radius = maxRadius * 0.10f, style = Stroke(width = 1.5f))

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

            // Distance ring labels
            drawContext.canvas.nativeCanvas.drawText("${perimeterThresholdMeters.toInt()}m PERIMETER", centerX, centerY - perimeterRadius - 6f, warningTextPaint)
            drawContext.canvas.nativeCanvas.drawText("30m", centerX, centerY - maxRadius + 22f, textPaint)
        }

        // 2. Static Top Pointer Arrow for Device Heading
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

            // Fading sweep sector gradient
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.85f to Color.Transparent,
                    1.0f to radarGreen.copy(alpha = 0.25f),
                    center = Offset(centerX, centerY)
                ),
                startAngle = -90f,
                sweepAngle = 60f,
                useCenter = true,
                topLeft = Offset(centerX - maxRadius, centerY - maxRadius),
                size = Size(maxRadius * 2, maxRadius * 2)
            )
        }

        // 4. Render Discovered Signal Blips
        for (blip in blips) {
            val radius = (blip.distance / maxDistanceRange) * maxRadius
            val cappedRadius = radius.coerceAtMost(maxRadius)
            val trueAngleRad = Math.toRadians((blip.targetAngleOffset - headingDegrees).toDouble())

            val blipX = centerX + (cappedRadius * sin(trueAngleRad)).toFloat()
            val blipY = centerY - (cappedRadius * cos(trueAngleRad)).toFloat()

            val nodeColor = when (blip.type.uppercase()) {
                "WIFI" -> wifiGreen
                "CELLULAR" -> cellRed
                "BLE" -> bleCyan
                "MAGNETIC" -> Color(0xFFFF00FF)
                else -> audioYellow
            }

            val isBreach = blip.distance < perimeterThresholdMeters

            // If inside perimeter, draw animated pulsing warning ring
            if (isBreach) {
                drawCircle(
                    color = breachRed.copy(alpha = 0.5f),
                    radius = 20f * pulseScale,
                    center = Offset(blipX, blipY),
                    style = Stroke(width = 2f)
                )
            }

            // Target Blip Dot
            drawCircle(
                color = nodeColor,
                radius = if (blip.id == nearestBlipId) 12f else 9f,
                center = Offset(blipX, blipY)
            )

            // Target Lock Crosshair if nearest target
            if (blip.id == nearestBlipId) {
                drawCircle(
                    color = Color.Yellow,
                    radius = 22f,
                    center = Offset(blipX, blipY),
                    style = Stroke(width = 2.5f)
                )
                drawLine(
                    color = Color.Yellow,
                    start = Offset(blipX - 28f, blipY),
                    end = Offset(blipX + 28f, blipY),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color.Yellow,
                    start = Offset(blipX, blipY - 28f),
                    end = Offset(blipX, blipY + 28f),
                    strokeWidth = 1.5f
                )
            }

            // Blip Name & Distance Tag
            drawContext.canvas.nativeCanvas.drawText(
                "${blip.name.take(12)} (${String.format("%.1f", blip.distance)}m)",
                blipX + 14f,
                blipY + 6f,
                blipTextPaint
            )
        }
    }
}
