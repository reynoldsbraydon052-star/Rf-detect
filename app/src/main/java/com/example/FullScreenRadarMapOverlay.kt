package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030705))
            .testTag("fullscreen_radar_map_container")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF07120B),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Heading & GPS Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF00FF66).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Heading",
                                tint = Color(0xFF00FF66),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "FULL SCREEN RADAR • ${uiState.headingDegrees.toInt()}° HEADING",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                ),
                                color = Color(0xFF00FF66)
                            )
                            Text(
                                text = "GPS 37.7749° N, 122.4194° W • ALT ${sensorSuite.estimatedAltitudeMeters.toInt()}m",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.LightGray
                            )
                        }
                    }

                    // Right Controls: Audio Sonar & Dismiss (if present)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Audio Sonar Quick Toggle
                        IconButton(
                            onClick = onToggleAudioSonar,
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (uiState.isAudioSonarActive) Color(0xFFFFCC00).copy(alpha = 0.2f) else Color.DarkGray.copy(alpha = 0.4f),
                                    CircleShape
                                )
                                .testTag("fullscreen_map_audio_sonar_toggle")
                        ) {
                            Icon(
                                imageVector = if (uiState.isAudioSonarActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "Audio Sonar",
                                tint = if (uiState.isAudioSonarActive) Color(0xFFFFCC00) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (onDismiss != null) {
                            // Close Dialog Button
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFFF3366).copy(alpha = 0.2f), CircleShape)
                                    .testTag("close_fullscreen_map_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Full Screen Map",
                                    tint = Color(0xFFFF3366),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Map View Mode Selector Bar (Vector / Heatmap / Sat-Grid)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF050B07))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VIEW MODE:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp
                    ),
                    color = Color.Gray
                )

                listOf(
                    "TACTICAL" to "TACTICAL VECTOR",
                    "HEATMAP" to "IR HEATMAP",
                    "SAT_GRID" to "SAT GRID"
                ).forEach { (modeKey, modeTitle) ->
                    val isSel = uiState.fullScreenMapMode == modeKey
                    FilterChip(
                        selected = isSel,
                        onClick = { onSetFullScreenMapMode(modeKey) },
                        label = {
                            Text(
                                modeTitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00FF66),
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF0C1F13),
                            labelColor = Color(0xFF00FF66)
                        )
                    )
                }
            }

            // Main Radar Map Canvas Area (Fills max space)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                TacticalRadarCanvas(
                    headingDegrees = uiState.headingDegrees,
                    blips = uiState.activeBlips,
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

            // Bottom Bar: PHONE HARDWARE SENSORS LIVE TELEMETRY STREAM
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF060E08),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GOOGLE PIXEL ALL-HARDWARE SENSORS TELEMETRY STREAM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF00FF66)
                        )
                        Text(
                            text = "${sensorSuite.totalActiveSensorsCount} Pixel Sensors Active",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            ),
                            color = Color(0xFF00E5FF)
                        )
                    }

                    // Scrollable Telemetry Readouts Row across all hardware sensors
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Magnetometer
                        item {
                            SensorMetricPill(
                                label = "3D MAGNETOMETER",
                                value = "${sensorSuite.magnetometerData.totalMicroTesla.toInt()} µT",
                                subValue = "Bx:%.0f By:%.0f Bz:%.0f".format(sensorSuite.magnetometerData.x, sensorSuite.magnetometerData.y, sensorSuite.magnetometerData.z),
                                accentColor = Color(0xFF00FF66)
                            )
                        }
                        // 2. Accelerometer / G-Force
                        item {
                            SensorMetricPill(
                                label = "ACCELEROMETER",
                                value = "%.2f G".format(sensorSuite.totalGForce),
                                subValue = if (sensorSuite.isMotionDetected) "MOTION ACTIVE" else "STATIONARY",
                                accentColor = if (sensorSuite.isMotionDetected) Color(0xFFFFCC00) else Color(0xFF00FF66)
                            )
                        }
                        // 3. Gyroscope Rotational Speed
                        item {
                            SensorMetricPill(
                                label = "GYROSCOPE",
                                value = "%.1f °/s".format(sensorSuite.rotationalSpeedDegPerSec),
                                subValue = "Vib: %.1f Hz".format(sensorSuite.vibrationHz),
                                accentColor = Color(0xFF00E5FF)
                            )
                        }
                        // 4. Barometer & Pressure Altitude
                        item {
                            SensorMetricPill(
                                label = "BAROMETER",
                                value = "${sensorSuite.pressureHpa.toInt()} hPa",
                                subValue = "Alt: ${sensorSuite.estimatedAltitudeMeters.toInt()}m",
                                accentColor = Color(0xFFFF8800)
                            )
                        }
                        // 5. Ambient Light Lux Meter
                        item {
                            SensorMetricPill(
                                label = "AMBIENT LIGHT",
                                value = "${sensorSuite.lightLux.toInt()} Lux",
                                subValue = sensorSuite.lightCondition,
                                accentColor = Color.Yellow
                            )
                        }
                        // 6. Infrared Proximity
                        item {
                            SensorMetricPill(
                                label = "IR PROXIMITY",
                                value = if (sensorSuite.isProximityNear) "NEAR (<1cm)" else "FAR (5cm)",
                                subValue = "Obstruct Sensing",
                                accentColor = if (sensorSuite.isProximityNear) Color(0xFFFF3366) else Color.Gray
                            )
                        }
                        // 7. Orientation Pitch & Roll
                        item {
                            SensorMetricPill(
                                label = "ROTATION VECTOR",
                                value = "${uiState.headingDegrees.toInt()}° Compass",
                                subValue = "P:${sensorSuite.pitchDeg.toInt()}° R:${sensorSuite.rollDeg.toInt()}°",
                                accentColor = Color(0xFF00FF66)
                            )
                        }
                        // 8. Step Counter / PDR
                        item {
                            SensorMetricPill(
                                label = "STEP DEAD RECKONING",
                                value = "${sensorSuite.stepCount} Steps",
                                subValue = "Dist: %.1fm".format(sensorSuite.pdrDistanceMeters),
                                accentColor = Color(0xFF00E5FF)
                            )
                        }
                        // 9. Gravity Sensor
                        item {
                            SensorMetricPill(
                                label = "GRAVITY VECTOR",
                                value = "Gx:%.1f Gy:%.1f".format(sensorSuite.gravityX, sensorSuite.gravityY),
                                subValue = "Gz:%.1f m/s²".format(sensorSuite.gravityZ),
                                accentColor = Color(0xFFFFCC00)
                            )
                        }
                        // 10. Linear Acceleration
                        item {
                            SensorMetricPill(
                                label = "LINEAR ACCEL",
                                value = "Lx:%.1f Ly:%.1f".format(sensorSuite.linearAccelX, sensorSuite.linearAccelY),
                                subValue = "Impulse Motion",
                                accentColor = Color(0xFF00FF66)
                            )
                        }
                        // 11. Uncalibrated EMF
                        item {
                            SensorMetricPill(
                                label = "RAW UNCALIB EMF",
                                value = "Ux:%.0f Uy:%.0f".format(sensorSuite.uncalibratedMagX, sensorSuite.uncalibratedMagY),
                                subValue = "Soft-Iron Bias",
                                accentColor = Color(0xFFFF00FF)
                            )
                        }
                        // 12. Thermal Sensor
                        item {
                            SensorMetricPill(
                                label = "THERMAL SPECTRUM",
                                value = "%.1f °C".format(sensorSuite.ambientTempCelsius),
                                subValue = "Humidity: %.0f%%".format(sensorSuite.relativeHumidityPct),
                                accentColor = Color(0xFFFF5500)
                            )
                        }
                        // 13. Game Rotation
                        item {
                            SensorMetricPill(
                                label = "GAME ROTATION",
                                value = "${sensorSuite.gameRotationHeading.toInt()}° Kinematics",
                                subValue = "Low-Latency Tracking",
                                accentColor = Color(0xFF00E5FF)
                            )
                        }
                        // 14. Stationarity
                        item {
                            SensorMetricPill(
                                label = "PIXEL MOTION STATE",
                                value = sensorSuite.motionState,
                                subValue = if (sensorSuite.isStationary) "MOUNTED STABLE" else "HANDHELD SWEEP",
                                accentColor = Color(0xFF00FF66)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorMetricPill(
    label: String,
    value: String,
    subValue: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0B1B10),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = Modifier.height(52.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                ),
                color = accentColor
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.LightGray
            )
        }
    }
}
