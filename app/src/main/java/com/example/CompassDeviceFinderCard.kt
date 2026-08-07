package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Real-time Compass Arrow Finder Composable
 * Calculates relative angle from phone's geomagnetic compass heading to target device angle offset,
 * displaying a rotating tactical arrow pointer with turn-by-turn proximity guidance.
 */
@Composable
fun CompassDeviceFinderCard(
    uiState: SignalRadarUiState,
    onSelectTargetDevice: (String?) -> Unit,
    onToggleAudioSonar: () -> Unit,
    onOpenCalibration: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpandedDeviceList by remember { mutableStateOf(false) }

    val activeBlips = uiState.activeBlips
    val selectedTargetId = uiState.selectedTargetDeviceId

    val targetBlip = remember(activeBlips, selectedTargetId, uiState.nearestBlip) {
        activeBlips.find { it.id == selectedTargetId || it.name == selectedTargetId }
            ?: uiState.nearestBlip
            ?: activeBlips.firstOrNull()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("compass_device_finder_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF06130B)),
        border = BorderStroke(1.5.dp, Color(0xFF00FF66))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Real-Time Compass Finder Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .background(Color(0xFF00FF66))
                    )
                    Text(
                        text = "REAL-TIME COMPASS FINDER",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF00FF66)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Automated Figure-Eight Calibration Routine Trigger
                    Surface(
                        onClick = onOpenCalibration,
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F2618),
                        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("compass_open_calibration_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AllInclusive,
                                contentDescription = "Figure Eight Calibrate",
                                tint = Color(0xFF00FF66),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "CALIBRATE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = Color(0xFF00FF66)
                            )
                        }
                    }

                    // Audio Sonar Pulse Toggle Button
                    IconButton(
                        onClick = onToggleAudioSonar,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("compass_toggle_sonar_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isAudioSonarActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Audio Sonar",
                            tint = if (uiState.isAudioSonarActive) Color(0xFF00FF66) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Device Selector Dropdown Toggle
                    Surface(
                        onClick = { isExpandedDeviceList = !isExpandedDeviceList },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F2618),
                        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("compass_select_device_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Select Target",
                                tint = Color(0xFF00FF66),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (selectedTargetId != null) "LOCKED" else "AUTO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = Color(0xFF00FF66)
                            )
                        }
                    }
                }
            }

            // Expandable Target Selector List
            AnimatedVisibility(visible = isExpandedDeviceList) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0A1F13),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SELECT TARGET DEVICE TO TRACK:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.Gray
                            )

                            if (selectedTargetId != null) {
                                TextTextButton(
                                    onClick = {
                                        onSelectTargetDevice(null)
                                        isExpandedDeviceList = false
                                    }
                                ) {
                                    Text(
                                        text = "CLEAR LOCK",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Yellow
                                        )
                                    )
                                }
                            }
                        }

                        if (activeBlips.isEmpty()) {
                            Text(
                                text = "No active signal targets detected yet.",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.Gray
                            )
                        } else {
                            activeBlips.take(6).forEach { blip ->
                                val isSelected = blip.id == selectedTargetId || blip.name == selectedTargetId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF1B3D28) else Color.Transparent)
                                        .clickable {
                                            onSelectTargetDevice(if (isSelected) null else blip.id)
                                            isExpandedDeviceList = false
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFF00FF66) else Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = blip.name,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = if (isSelected) Color.Yellow else Color.White
                                        )
                                    }

                                    Text(
                                        text = "%.1fm • ${blip.rssi}dBm".format(blip.distance),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.5.sp
                                        ),
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (targetBlip == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFF0B1B11), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Searching for signal compass vectors...",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Gray
                    )
                }
            } else {
                val heading = uiState.headingDegrees
                val targetAngle = targetBlip.targetAngleOffset

                // Calculate real-time relative angle (0° = straight ahead, 90° = right, 180° = behind, 270° = left)
                val relativeAngle = (targetAngle - heading + 360f) % 360f

                // Continuous shortest-path angle tracking for smooth animation without 360° spin jumps
                var currentContinuousAngle by remember { mutableFloatStateOf(relativeAngle) }
                val targetContinuousAngle = remember(relativeAngle) {
                    var delta = relativeAngle - (currentContinuousAngle % 360f)
                    if (delta < 0f) delta += 360f
                    if (delta > 180f) delta -= 360f
                    val next = currentContinuousAngle + delta
                    currentContinuousAngle = next
                    next
                }

                // Smooth rotation animation logic
                val animAngle by animateFloatAsState(
                    targetValue = targetContinuousAngle,
                    animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing),
                    label = "compassArrowRotation"
                )

                // Directional status calculations
                val diffFromFront = if (relativeAngle > 180f) 360f - relativeAngle else relativeAngle
                val isFacingTarget = diffFromFront <= 18f
                val isTargetBehind = diffFromFront >= 150f

                val turnGuidanceText = when {
                    targetBlip.distance < 0.8f -> "TARGET LOCATED DIRECTLY HERE"
                    isFacingTarget -> "FACE STRAIGHT AHEAD • WALK FORWARD"
                    isTargetBehind -> "TURN 180° AROUND"
                    relativeAngle in 18.0f..180.0f -> "TURN RIGHT ${relativeAngle.toInt()}°"
                    else -> "TURN LEFT ${(360f - relativeAngle).toInt()}°"
                }

                val arrowColor = when {
                    targetBlip.distance < 0.8f -> Color(0xFF00FF66)
                    isFacingTarget -> Color(0xFF00FF66)
                    isTargetBehind -> Color(0xFFFF3366)
                    else -> Color(0xFFFFCC00)
                }

                // Main Compass Dial & Arrow Canvas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Device Info & Turn Guidance
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = arrowColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, arrowColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = turnGuidanceText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = arrowColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = targetBlip.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )
                        
                        // Z-Axis Vertical Elevation Badge
                        val elevationDiff = targetBlip.estimatedZOffsetMeters
                        val elevationText = when {
                            abs(elevationDiff) < 1.0f -> "[= SAME LEVEL]"
                            elevationDiff > 0f -> "[↑ FLOOR ABOVE (+%.1fm)]".format(elevationDiff)
                            else -> "[↓ FLOOR BELOW (%.1fm)]".format(elevationDiff)
                        }
                        val elevationColor = when {
                            abs(elevationDiff) < 1.0f -> Color(0xFF00FF66)
                            elevationDiff > 0f -> Color(0xFFFFCC00)
                            else -> Color(0xFF00E5FF)
                        }
                        Text(
                            text = elevationText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = elevationColor
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "DISTANCE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.5.sp
                                    ),
                                    color = Color.Gray
                                )
                                Text(
                                    text = "%.1fm".format(targetBlip.distance),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (targetBlip.distance < 2.0f) Color(0xFF00FF66) else Color.White
                                )
                            }

                            Column {
                                Text(
                                    text = "SIGNAL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.5.sp
                                    ),
                                    color = Color.Gray
                                )
                                Text(
                                    text = "${targetBlip.rssi} dBm",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF00E5FF)
                                )
                            }

                            Column {
                                Text(
                                    text = "BEARING",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.5.sp
                                    ),
                                    color = Color.Gray
                                )
                                Text(
                                    text = "${relativeAngle.toInt()}°",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.Yellow
                                )
                            }
                        }

                        Text(
                            text = "Phone Heading: ${heading.toInt()}° • Target Absolute: ${targetAngle.toInt()}°",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            ),
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right Column: Rotating Tactical Compass Needle Canvas
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .testTag("compass_directional_arrow_canvas"),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val radius = size.minDimension / 2f - 8f

                            // Outer Compass Dial Ring
                            drawCircle(
                                color = Color(0xFF142E1F),
                                radius = radius,
                                center = Offset(centerX, centerY),
                                style = Fill
                            )

                            drawCircle(
                                color = Color(0xFF00FF66).copy(alpha = 0.4f),
                                radius = radius,
                                center = Offset(centerX, centerY),
                                style = Stroke(width = 2.5f)
                            )

                            // Cardinal Tick Marks (N, E, S, W)
                            val cardinalTicks = listOf(
                                0f to "N", 90f to "E", 180f to "S", 270f to "W"
                            )

                            for (angle in 0 until 360 step 30) {
                                val rad = Math.toRadians((angle - heading).toDouble())
                                val innerR = if (angle % 90 == 0) radius - 12f else radius - 6f
                                val outerR = radius - 2f

                                val startX = centerX + (innerR * sin(rad)).toFloat()
                                val startY = centerY - (innerR * cos(rad)).toFloat()
                                val endX = centerX + (outerR * sin(rad)).toFloat()
                                val endY = centerY - (outerR * cos(rad)).toFloat()

                                drawLine(
                                    color = if (angle == 0) Color.Red else Color(0xFF00FF66).copy(alpha = 0.5f),
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = if (angle % 90 == 0) 2.5f else 1f
                                )
                            }

                            // Rotating Directional Arrow pointing to Target
                            rotate(animAngle, pivot = Offset(centerX, centerY)) {
                                val arrowPath = Path().apply {
                                    // Arrow Tip at top
                                    moveTo(centerX, centerY - radius + 12f)
                                    // Right Wing
                                    lineTo(centerX + 16f, centerY + 24f)
                                    // Notch back
                                    lineTo(centerX, centerY + 12f)
                                    // Left Wing
                                    lineTo(centerX - 16f, centerY + 24f)
                                    close()
                                }

                                // Arrow Body Fill
                                drawPath(
                                    path = arrowPath,
                                    color = arrowColor
                                )

                                // Arrow Border
                                drawPath(
                                    path = arrowPath,
                                    color = Color.White,
                                    style = Stroke(width = 1.5f)
                                )

                                // Tail Line (pointing backward)
                                drawLine(
                                    color = Color.Red.copy(alpha = 0.7f),
                                    start = Offset(centerX, centerY + 12f),
                                    end = Offset(centerX, centerY + radius - 16f),
                                    strokeWidth = 2f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                )
                            }

                            // Center Pivot Dot
                            drawCircle(
                                color = Color.Black,
                                radius = 10f,
                                center = Offset(centerX, centerY)
                            )
                            drawCircle(
                                color = arrowColor,
                                radius = 6f,
                                center = Offset(centerX, centerY)
                            )
                        }

                        // Top Forward Marker "AHEAD"
                        Text(
                            text = "AHEAD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp
                            ),
                            color = Color(0xFF00FF66),
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextTextButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        content()
    }
}
