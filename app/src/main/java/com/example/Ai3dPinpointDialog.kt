package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * AI-Powered 3D Pinpointer HUD Dialog
 * Displays real-time exact 3D location, dual-axis elevation crosshair reticle,
 * barometric altitude delta, floor classification, and Gemini AI tactical search guidance.
 */
@Composable
fun Ai3dPinpointDialog(
    pinpointResult: AiPinpointResult?,
    sensorSuite: HardwareSensorSuiteData,
    compassHeading: Float,
    isAudioSonarActive: Boolean,
    onToggleAudioSonar: () -> Unit,
    onRefreshPinpoint: () -> Unit,
    onDismiss: () -> Unit
) {
    if (pinpointResult == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF030A06))
                .testTag("ai_3d_pinpoint_dialog_surface"),
            color = Color(0xFF030A06)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // Top Header Bar
                PinpointHeaderBar(
                    pinpointResult = pinpointResult,
                    onDismiss = onDismiss,
                    onRefresh = onRefreshPinpoint
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 3D Spatial AimSight Crosshair Scope
                    Tactical3dAimSightScope(
                        pinpointResult = pinpointResult,
                        phoneHeading = compassHeading,
                        phonePitch = sensorSuite.pitchDeg,
                        phoneRoll = sensorSuite.rollDeg
                    )

                    // Real-Time 3D Elevation & Barometric Gauge
                    ElevationAndAltitudeGaugeCard(
                        pinpointResult = pinpointResult,
                        sensorSuite = sensorSuite
                    )

                    // Gemini AI Tactical Guidance & Physical Search Zone
                    AiTacticalGuidanceCard(
                        pinpointResult = pinpointResult,
                        isAudioSonarActive = isAudioSonarActive,
                        onToggleAudioSonar = onToggleAudioSonar
                    )

                    // Step-by-Step Search Checklist
                    SearchChecklistCard(
                        checklist = pinpointResult.searchChecklist
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun PinpointHeaderBar(
    pinpointResult: AiPinpointResult,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF08190E),
        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (pinpointResult.isPinpointingLoading) Color.Yellow else Color(0xFF00FF66))
                )
                Column {
                    Text(
                        text = "AI 3D SPATIAL PINPOINTER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF00FF66)
                    )
                    Text(
                        text = "${pinpointResult.targetName} • ${pinpointResult.signalType} (${pinpointResult.currentRssiDbm} dBm)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (pinpointResult.isPinpointingLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.Yellow
                    )
                } else {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(32.dp).testTag("pinpoint_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recalculate 3D Pinpoint",
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).testTag("pinpoint_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close HUD",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tactical 3D AimSight Scope
 * Renders an augmented reality style crosshair sight tracking both azimuth and elevation pitch.
 */
@Composable
private fun Tactical3dAimSightScope(
    pinpointResult: AiPinpointResult,
    phoneHeading: Float,
    phonePitch: Float,
    phoneRoll: Float
) {
    // Relative Azimuth Angle (-180° to +180°)
    var deltaAzimuth = (pinpointResult.azimuthDegrees - phoneHeading + 360f) % 360f
    if (deltaAzimuth > 180f) deltaAzimuth -= 360f

    // Relative Elevation Pitch Angle (-90° to +90°)
    val deltaPitch = pinpointResult.elevationPitchDeg - phonePitch

    // Check if phone is aimed on-axis in 3D (within ±12° horizontal and ±10° vertical)
    val isOnAxis = abs(deltaAzimuth) <= 12f && abs(deltaPitch) <= 10f

    // Infinite breathing pulse for target lock
    val infiniteTransition = rememberInfiniteTransition(label = "ScopePulse")
    val lockPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LockPulse"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF06150C)),
        border = BorderStroke(
            if (isOnAxis) 2.dp else 1.dp,
            if (isOnAxis) Color(0xFF00FF66) else Color(0xFF00FF66).copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("aimsight_scope_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Scope Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isOnAxis) Color(0xFF00FF66).copy(alpha = 0.2f) else Color(0xFFFFCC00).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (isOnAxis) Color(0xFF00FF66) else Color(0xFFFFCC00))
                ) {
                    Text(
                        text = if (isOnAxis) "🎯 [3D TARGET LOCKED • ON AXIS]" else "SEARCHING 3D VECTOR (ALIGN SIGHT)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = if (isOnAxis) Color(0xFF00FF66) else Color(0xFFFFCC00),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "CONFIDENCE: ${pinpointResult.confidencePercent}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                )
            }

            // Interactive 3D Crosshair Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(Color(0xFF030D07), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f

                    // 1. Draw Scope Radial Concentric Range Rings
                    val ringRadii = listOf(w * 0.15f, w * 0.30f, w * 0.45f)
                    ringRadii.forEachIndexed { idx, r ->
                        drawCircle(
                            color = Color(0xFF00FF66).copy(alpha = 0.12f + idx * 0.05f),
                            radius = r,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.2f)
                        )
                    }

                    // 2. Horizon Pitch / Level Pitch Line
                    val pitchYOffset = (phonePitch / 90f) * (h * 0.4f)
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                        start = Offset(cx - w * 0.35f, cy + pitchYOffset),
                        end = Offset(cx + w * 0.35f, cy + pitchYOffset),
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round
                    )

                    // 3. Center Crosshair Reticle (Phone's bore-sight)
                    val crossLen = 22f
                    drawLine(
                        color = if (isOnAxis) Color(0xFF00FF66) else Color.White.copy(alpha = 0.6f),
                        start = Offset(cx - crossLen, cy),
                        end = Offset(cx + crossLen, cy),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = if (isOnAxis) Color(0xFF00FF66) else Color.White.copy(alpha = 0.6f),
                        start = Offset(cx, cy - crossLen),
                        end = Offset(cx, cy + crossLen),
                        strokeWidth = 2f
                    )
                    drawCircle(
                        color = if (isOnAxis) Color(0xFF00FF66) else Color.White.copy(alpha = 0.4f),
                        radius = 8f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f)
                    )

                    // 4. Calculate Target 3D Reticle Position relative to screen
                    // Max FOV mapped to canvas: ±60° azimuth, ±45° pitch
                    val targetScreenX = (cx + (deltaAzimuth / 60f) * (w * 0.42f)).coerceIn(20f, w - 20f)
                    val targetScreenY = (cy - (deltaPitch / 45f) * (h * 0.42f)).coerceIn(20f, h - 20f)

                    val targetReticleColor = if (isOnAxis) Color(0xFF00FF66) else Color(0xFFFFCC00)
                    val reticleRadius = if (isOnAxis) 18f * lockPulse else 14f

                    // Draw Line connecting Phone Crosshair to Target
                    drawLine(
                        color = targetReticleColor.copy(alpha = 0.45f),
                        start = Offset(cx, cy),
                        end = Offset(targetScreenX, targetScreenY),
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round
                    )

                    // Draw Target 3D Reticle Diamond / Lock Ring
                    drawCircle(
                        color = targetReticleColor,
                        radius = reticleRadius,
                        center = Offset(targetScreenX, targetScreenY),
                        style = Stroke(width = 2.5f)
                    )
                    drawCircle(
                        color = targetReticleColor.copy(alpha = 0.25f),
                        radius = reticleRadius * 0.6f,
                        center = Offset(targetScreenX, targetScreenY)
                    )

                    // Reticle Corner Brackets
                    val bLen = 8f
                    drawLine(
                        color = targetReticleColor,
                        start = Offset(targetScreenX - reticleRadius - bLen, targetScreenY),
                        end = Offset(targetScreenX - reticleRadius, targetScreenY),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = targetReticleColor,
                        start = Offset(targetScreenX + reticleRadius, targetScreenY),
                        end = Offset(targetScreenX + reticleRadius + bLen, targetScreenY),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = targetReticleColor,
                        start = Offset(targetScreenX, targetScreenY - reticleRadius - bLen),
                        end = Offset(targetScreenX, targetScreenY - reticleRadius),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = targetReticleColor,
                        start = Offset(targetScreenX, targetScreenY + reticleRadius),
                        end = Offset(targetScreenX, targetScreenY + reticleRadius + bLen),
                        strokeWidth = 2f
                    )
                }

                // HUD Dynamic Alignment Guidance Callout
                val guidanceText = when {
                    isOnAxis -> "LOCKED ON AXIS • ADVANCE STRAIGHT"
                    deltaAzimuth > 15f -> "PAN RIGHT → ${deltaAzimuth.toInt()}°"
                    deltaAzimuth < -15f -> "PAN LEFT ← ${abs(deltaAzimuth).toInt()}°"
                    deltaPitch > 10f -> "TILT UPWARDS ↑ ${deltaPitch.toInt()}°"
                    deltaPitch < -10f -> "TILT DOWNWARDS ↓ ${abs(deltaPitch).toInt()}°"
                    else -> "FINE ADJUSTING..."
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, if (isOnAxis) Color(0xFF00FF66) else Color(0xFFFFCC00))
                ) {
                    Text(
                        text = guidanceText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        ),
                        color = if (isOnAxis) Color(0xFF00FF66) else Color(0xFFFFCC00),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Exact 3D Metric Triplets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricColumn(
                    title = "EXACT DISTANCE",
                    value = "%.2fm".format(pinpointResult.distanceMeters),
                    sub = "±${"%.2f".format(pinpointResult.accuracyMarginMeters)}m error",
                    accentColor = Color(0xFF00FF66)
                )

                MetricColumn(
                    title = "TRUE BEARING",
                    value = "${pinpointResult.azimuthDegrees.toInt()}°",
                    sub = pinpointResult.relativeClockHeading,
                    accentColor = Color(0xFF00E5FF)
                )

                MetricColumn(
                    title = "ELEVATION PITCH",
                    value = "${if (pinpointResult.elevationPitchDeg >= 0) "+" else ""}${"%.1f".format(pinpointResult.elevationPitchDeg)}°",
                    sub = if (pinpointResult.elevationPitchDeg >= 0) "UPWARD" else "DOWNWARD",
                    accentColor = if (pinpointResult.elevationPitchDeg >= 0) Color(0xFFFFCC00) else Color(0xFF00E5FF)
                )

                MetricColumn(
                    title = "ALTITUDE DELTA",
                    value = "${if (pinpointResult.altitudeOffsetMeters >= 0) "+" else ""}${"%.2f".format(pinpointResult.altitudeOffsetMeters)}m",
                    sub = "Z-Axis offset",
                    accentColor = if (abs(pinpointResult.altitudeOffsetMeters) < 0.6f) Color(0xFF00FF66) else Color(0xFFFFCC00)
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(
    title: String,
    value: String,
    sub: String,
    accentColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp
            ),
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp
            ),
            color = accentColor
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 8.5.sp
            ),
            color = Color.LightGray
        )
    }
}

/**
 * Real-Time 3D Elevation & Barometric Gauge Card
 */
@Composable
private fun ElevationAndAltitudeGaugeCard(
    pinpointResult: AiPinpointResult,
    sensorSuite: HardwareSensorSuiteData
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF07140B)),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().testTag("elevation_gauge_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Height,
                        contentDescription = "Vertical Elevation",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "VERTICAL ELEVATION & BAROMETER FUSION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
                        ),
                        color = Color(0xFF00E5FF)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F261D),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = pinpointResult.floorClassification,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = Color(0xFF00FF66),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Elevation Level Visual Inclinometer Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PHONE TILT: ${sensorSuite.pitchDeg.toInt()}°",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp
                        ),
                        color = Color.LightGray
                    )
                    Text(
                        text = "TARGET ELEVATION: ${if (pinpointResult.elevationPitchDeg >= 0) "+" else ""}${pinpointResult.elevationPitchDeg.toInt()}°",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp
                        ),
                        color = Color(0xFFFFCC00)
                    )
                }

                // Inclinometer Slider Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(Color(0xFF040E08), RoundedCornerShape(6.dp))
                        .border(1.dp, Color.DarkGray, RoundedCornerShape(6.dp))
                ) {
                    // Center Zero Indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.4f))
                    )

                    // Phone Pitch Marker (Blue)
                    val phoneNorm = ((sensorSuite.pitchDeg + 90f) / 180f).coerceIn(0.05f, 0.95f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(phoneNorm)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF))
                        )
                    }

                    // Target Pitch Marker (Gold)
                    val targetNorm = ((pinpointResult.elevationPitchDeg + 90f) / 180f).coerceIn(0.05f, 0.95f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(targetNorm)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(Color(0xFFFFCC00))
                        )
                    }
                }
            }

            // Barometer and Spatial Matrix Details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B2014), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BAROMETER PRESSURE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp
                        ),
                        color = Color.Gray
                    )
                    Text(
                        text = "%.2f hPa (Alt: %.1fm)".format(sensorSuite.pressureHpa, sensorSuite.estimatedAltitudeMeters),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SPATIAL VECTOR",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp
                        ),
                        color = Color.Gray
                    )
                    Text(
                        text = pinpointResult.spatialVectorXyz,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Color(0xFF00FF66)
                    )
                }
            }
        }
    }
}

/**
 * Gemini AI Tactical Guidance & Physical Zone Card
 */
@Composable
private fun AiTacticalGuidanceCard(
    pinpointResult: AiPinpointResult,
    isAudioSonarActive: Boolean,
    onToggleAudioSonar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF091B10)),
        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().testTag("ai_guidance_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Gemini AI",
                        tint = Color(0xFF00FF66),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "GEMINI AI TACTICAL PINPOINT GUIDANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
                        ),
                        color = Color(0xFF00FF66)
                    )
                }

                Surface(
                    onClick = onToggleAudioSonar,
                    shape = RoundedCornerShape(6.dp),
                    color = if (isAudioSonarActive) Color(0xFF00FF66) else Color(0xFF142B1D),
                    border = BorderStroke(1.dp, Color(0xFF00FF66))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isAudioSonarActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = null,
                            tint = if (isAudioSonarActive) Color.Black else Color(0xFF00FF66),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isAudioSonarActive) "SONAR: ON" else "SONAR: OFF",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            ),
                            color = if (isAudioSonarActive) Color.Black else Color(0xFF00FF66)
                        )
                    }
                }
            }

            // Natural Language Tactical Guidance
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF040E07),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = pinpointResult.aiTacticalGuidance,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(10.dp)
                )
            }

            // Physical Zone Estimation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFFFCC00),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "ESTIMATED HIDING ZONE: ${pinpointResult.physicalZoneEstimation}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color(0xFFFFCC00)
                )
            }
        }
    }
}

/**
 * Step-by-Step Search Checklist Card
 */
@Composable
private fun SearchChecklistCard(
    checklist: List<String>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF06140C)),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "ACTIONABLE PHYSICAL SEARCH CHECKLIST:",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                ),
                color = Color.Gray
            )

            checklist.forEach { step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = null,
                        tint = Color(0xFF00FF66),
                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                    )
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}
