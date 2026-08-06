package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

enum class ScannerViewMode {
    RF_BLE_SPECTRUM,
    OPTICAL_CAMERA,
    MATRIX_CODE
}

@Composable
fun TacticalScannerScreen(
    uiState: SignalRadarUiState,
    onToggleBleScanner: () -> Unit,
    onToggleScanning: () -> Unit,
    onFilterSelected: (String) -> Unit,
    onSelectTargetDevice: (String?) -> Unit,
    onOpenArCameraForTarget: (String) -> Unit,
    onPlayTestPing: (Double) -> Unit,
    onClearScanHistory: () -> Unit = {}
) {
    var viewMode by remember { mutableStateOf(ScannerViewMode.RF_BLE_SPECTRUM) }
    var isDeepSweepRunning by remember { mutableStateOf(false) }
    var showMaskedAddresses by remember { mutableStateOf(true) }

    val filteredBlips = rememberFilteredBlips(uiState.activeBlips, uiState.selectedFilterType)

    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserPosition"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B09))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Tactical Control Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tactical_scanner_header_card"),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0F1B15),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                                .background(if (uiState.isBleScannerServiceActive) Color(0xFF00FF66) else Color.Red)
                        )
                        Text(
                            text = "TACTICAL MULTI-SCANNER",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Toggle Active Scan Service
                        IconButton(
                            onClick = onToggleBleScanner,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("scanner_toggle_service_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isBleScannerServiceActive) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Toggle Scan Engine",
                                tint = if (uiState.isBleScannerServiceActive) Color(0xFF00FF66) else Color.Yellow,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Deep Sweep Button
                        Button(
                            onClick = {
                                isDeepSweepRunning = true
                                onToggleScanning()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDeepSweepRunning) Color(0xFFFF9900) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("scanner_deep_sweep_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = "Deep Sweep",
                                tint = if (isDeepSweepRunning) Color.Black else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SWEEP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = if (isDeepSweepRunning) Color.Black else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Mode Tabs (RF & BLE, OPTICAL AR, MATRIX & CODE)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ScannerViewMode.entries.forEach { mode ->
                        val isSelected = mode == viewMode
                        val label = when (mode) {
                            ScannerViewMode.RF_BLE_SPECTRUM -> "RF & BLE"
                            ScannerViewMode.OPTICAL_CAMERA -> "OPTICAL AR"
                            ScannerViewMode.MATRIX_CODE -> "QR & MATRIX"
                        }
                        val icon = when (mode) {
                            ScannerViewMode.RF_BLE_SPECTRUM -> Icons.Default.Sensors
                            ScannerViewMode.OPTICAL_CAMERA -> Icons.Default.Camera
                            ScannerViewMode.MATRIX_CODE -> Icons.Default.Search
                        }

                        Surface(
                            onClick = { viewMode = mode },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scanner_mode_${mode.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Viewport / Reticle / Visualizer Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFF040A07), RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .testTag("tactical_scanner_viewport_box")
        ) {
            when (viewMode) {
                ScannerViewMode.OPTICAL_CAMERA, ScannerViewMode.MATRIX_CODE -> {
                    // Live AR Camera View with Reticle Overlay
                    UwbArCameraScreen(
                        uiState = uiState,
                        mapRangeMeters = uiState.mapRangeMeters,
                        onSelectTargetDevice = onSelectTargetDevice,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Laser Scanning Line Overlay
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val lineY = h * laserPosition

                        // Reticle Corners
                        val cornerLen = 24f
                        val strokeW = 3f
                        val reticleColor = Color(0xFF00FF66).copy(alpha = pulseGlow)

                        // Top Left Corner
                        drawLine(reticleColor, Offset(16f, 16f), Offset(16f + cornerLen, 16f), strokeW)
                        drawLine(reticleColor, Offset(16f, 16f), Offset(16f, 16f + cornerLen), strokeW)

                        // Top Right Corner
                        drawLine(reticleColor, Offset(w - 16f, 16f), Offset(w - 16f - cornerLen, 16f), strokeW)
                        drawLine(reticleColor, Offset(w - 16f, 16f), Offset(w - 16f, 16f + cornerLen), strokeW)

                        // Bottom Left Corner
                        drawLine(reticleColor, Offset(16f, h - 16f), Offset(16f + cornerLen, h - 16f), strokeW)
                        drawLine(reticleColor, Offset(16f, h - 16f), Offset(16f, h - 16f - cornerLen), strokeW)

                        // Bottom Right Corner
                        drawLine(reticleColor, Offset(w - 16f, h - 16f), Offset(w - 16f - cornerLen, h - 16f), strokeW)
                        drawLine(reticleColor, Offset(w - 16f, h - 16f), Offset(w - 16f, h - 16f - cornerLen), strokeW)

                        // Laser Sweep Line
                        drawLine(
                            color = Color(0xFFFF0055).copy(alpha = 0.85f),
                            start = Offset(0f, lineY),
                            end = Offset(w, lineY),
                            strokeWidth = 2.5f
                        )
                        drawRect(
                            color = Color(0xFFFF0055).copy(alpha = 0.15f),
                            topLeft = Offset(0f, (lineY - 12f).coerceAtLeast(0f)),
                            size = Size(w, 24f)
                        )
                    }

                    // Optical HUD Overlay Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = if (viewMode == ScannerViewMode.MATRIX_CODE) "MATRIX QR DECODER • 60 FPS" else "OPTICAL RETICLE • FOV 60°",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF00FF66),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                ScannerViewMode.RF_BLE_SPECTRUM -> {
                    // RF Spectrum Wave distribution visualizer
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val pad = 20f
                        val graphW = w - (pad * 2)
                        val graphH = h - (pad * 2)

                        // Grid lines
                        for (i in 1..4) {
                            val y = pad + (graphH * (i / 5f))
                            drawLine(
                                color = Color(0xFF00FF66).copy(alpha = 0.15f),
                                start = Offset(pad, y),
                                end = Offset(w - pad, y),
                                strokeWidth = 1f
                            )
                        }

                        // Spectrum Peaks Path
                        val path = Path()
                        path.moveTo(pad, pad + graphH)

                        filteredBlips.forEachIndexed { idx, blip ->
                            val xRatio = (idx + 1).toFloat() / (filteredBlips.size + 1)
                            val x = pad + (xRatio * graphW)
                            val normRssi = ((blip.rssi + 100f) / 80f).coerceIn(0.1f, 1.0f)
                            val y = pad + graphH - (normRssi * graphH)

                            path.lineTo(x - 15f, pad + graphH)
                            path.lineTo(x, y)
                            path.lineTo(x + 15f, pad + graphH)

                            // Draw beacon dot at peak
                            drawCircle(
                                color = if (blip.id == uiState.selectedTargetDeviceId) Color.Yellow else Color(0xFF00FF66),
                                radius = if (blip.id == uiState.selectedTargetDeviceId) 6f else 4f,
                                center = Offset(x, y)
                            )
                        }
                        path.lineTo(w - pad, pad + graphH)

                        drawPath(
                            path = path,
                            color = Color(0xFF00FF66).copy(alpha = 0.8f),
                            style = Stroke(width = 2f, cap = StrokeCap.Round)
                        )
                    }

                    // Spectrum Status Header
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "BLIPS: ${filteredBlips.size} • 2.4GHz / BLE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Filter Bar & Unmask Addresses Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChipsRow(
                selectedFilter = uiState.selectedFilterType,
                onFilterSelected = onFilterSelected
            )

            IconButton(
                onClick = { showMaskedAddresses = !showMaskedAddresses },
                modifier = Modifier
                    .size(28.dp)
                    .testTag("scanner_toggle_mask_button")
            ) {
                Icon(
                    imageVector = if (showMaskedAddresses) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle MAC Masking",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Live Discovered Devices Scanned List
        Text(
            text = "DISCOVERED SIGNALS (${filteredBlips.size})",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )

        if (filteredBlips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0F1B15), RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = "Scanning",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Scanning RF spectrum...",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("tactical_scanner_device_list"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredBlips, key = { it.id }) { blip ->
                    val isSelected = blip.id == uiState.selectedTargetDeviceId || blip.name == uiState.selectedTargetDeviceId

                    val signalColor = when {
                        blip.rssi >= -65 -> Color(0xFF00FF66)
                        blip.rssi >= -85 -> Color(0xFFFFCC00)
                        else -> Color(0xFFFF3366)
                    }

                    val typeIcon = when (blip.type) {
                        "WIFI" -> Icons.Default.Wifi
                        "BLE" -> Icons.Default.Bluetooth
                        "ULTRASONIC" -> Icons.Default.GraphicEq
                        "CELLULAR" -> Icons.Default.CellTower
                        "MAGNETIC" -> Icons.Default.Equalizer
                        else -> Icons.Default.Sensors
                    }

                    val displayAddress = if (showMaskedAddresses && blip.id.length > 8) {
                        "${blip.id.take(4)}:***:${blip.id.takeLast(4)}"
                    } else blip.id

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF1B3828) else Color(0xFF0F1B15),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color.Yellow else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTargetDevice(if (isSelected) null else blip.id) }
                            .testTag("scanner_device_item_${blip.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(signalColor.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = typeIcon,
                                        contentDescription = blip.type,
                                        tint = signalColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = blip.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = if (isSelected) Color.Yellow else Color.White
                                    )
                                    Text(
                                        text = "$displayAddress • ${blip.type}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp
                                        ),
                                        color = Color.Gray
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${blip.rssi} dBm",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = signalColor
                                    )
                                    Text(
                                        text = "%.1fm".format(blip.distance),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp
                                        ),
                                        color = Color.LightGray
                                    )
                                }

                                // Quick Action Buttons for Device
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(
                                        onClick = { onOpenArCameraForTarget(blip.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Camera,
                                            contentDescription = "View AR",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onPlayTestPing(1200.0) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Ping",
                                            tint = Color.Yellow,
                                            modifier = Modifier.size(14.dp)
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
}
