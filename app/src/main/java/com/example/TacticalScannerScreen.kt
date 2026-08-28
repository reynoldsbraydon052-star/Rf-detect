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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

enum class ScannerTab {
    SCAN,
    TRACK,
    LOCALIZE,
    AR
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
    onClearScanHistory: () -> Unit = {},
    onAddSpatialPoint: (String, RfMeasurementPoint) -> Unit = { _, _ -> },
    onSetMapRange: (Float) -> Unit = {}
) {
    // Current local view/tab selection
    var activeTab by remember { mutableStateOf(ScannerTab.SCAN) }

    // Advanced sliders & settings panel expansion state
    var isControlsExpanded by remember { mutableStateOf(false) }

    // Collapsible Device List sheet state
    var isDeviceListExpanded by remember { mutableStateOf(false) }

    // Radar zoom range in meters from ViewModel state
    val currentZoomMeters = uiState.currentRadarRangeMeters

    // Local Search & MAC mask state
    var searchQuery by remember { mutableStateOf("") }
    var showMaskedAddresses by remember { mutableStateOf(true) }

    // Alarm state transition tracker to prevent screen flashing/spamming
    var previousAlarmState by remember { mutableStateOf(uiState.currentAlarmState) }
    var lastAlertBannerMessage by remember { mutableStateOf<String?>(null) }
    var showAlertBanner by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.currentAlarmState) {
        if (uiState.currentAlarmState != previousAlarmState) {
            val msg = when (uiState.currentAlarmState) {
                AlarmState.TRIGGERED -> "CRITICAL BREACH WARNING: Device threshold exceeded!"
                AlarmState.APPROACHING -> "PROXIMITY WARNING: Source approaching inner security sector!"
                AlarmState.COOLDOWN -> "ALERT CLEARED: Source signal returning to background baseline."
                else -> null
            }
            if (msg != null) {
                lastAlertBannerMessage = msg
                showAlertBanner = true
            }
            previousAlarmState = uiState.currentAlarmState
        }
    }

    // Filter, Sort and Limit devices without recreating list references
    val filteredBlips = rememberFilteredBlips(
        blips = uiState.activeBlips,
        filterType = uiState.selectedFilterType,
        maxDevices = uiState.maxVisibleDevices,
        isFocusMode = uiState.isFocusModeEnabled,
        minRssiDbm = uiState.minRssiFilterDbm,
        selectedTargetId = uiState.selectedTargetDeviceId,
        sortBy = uiState.sortByPriority
    )

    val searchedBlips = remember(filteredBlips, searchQuery) {
        if (searchQuery.isBlank()) {
            filteredBlips
        } else {
            filteredBlips.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.id.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Find the currently locked target if any
    val lockedTarget = remember(uiState.activeBlips, uiState.selectedTargetDeviceId) {
        uiState.activeBlips.find { it.id == uiState.selectedTargetDeviceId || it.name == uiState.selectedTargetDeviceId }
    }

    // Mathematical rolling filtered RSSI for locked target
    var filteredRssi by remember(uiState.selectedTargetDeviceId) { mutableStateOf(0f) }
    LaunchedEffect(lockedTarget?.rssi) {
        lockedTarget?.let {
            if (filteredRssi == 0f) {
                filteredRssi = it.rssi.toFloat()
            } else {
                filteredRssi = filteredRssi * 0.82f + it.rssi.toFloat() * 0.18f
            }
        }
    }

    // Math solver logs & quality stats
    val targetMeasurements = remember(uiState.measurementHistory, uiState.selectedTargetDeviceId) {
        uiState.measurementHistory.filter { it.targetId == uiState.selectedTargetDeviceId }
    }
    val samplesCount = targetMeasurements.size

    val volume = remember(targetMeasurements, uiState.selectedTargetDeviceId) {
        NextBestMeasurementEngine.estimateProbabilityVolume(targetMeasurements, uiState.selectedTargetDeviceId)
    }

    val finalQualityScore = remember(lockedTarget, targetMeasurements, volume) {
        if (volume != null && volume.isValid) {
            (volume.measurementQuality * 100).toInt().coerceIn(10, 99)
        } else if (targetMeasurements.isNotEmpty()) {
            targetMeasurements.map { it.qualityScore }.average().toInt().coerceIn(10, 99)
        } else if (lockedTarget != null) {
            (100 - abs(lockedTarget.rssi + 40) * 1.1f).toInt().coerceIn(10, 95)
        } else {
            0
        }
    }

    // Outer Container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040805))
            .testTag("tactical_scanner_screen_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp), // Space for bottom navigation
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ==================================================
            // 1. TOP HEADER & METRICS
            // ==================================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .testTag("tactical_scanner_header_card"),
                color = Color(0xFF0A140E),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.25f)),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "RF DETECT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color(0xFF00FF66)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isBleScannerServiceActive) Color(0xFF00FF66) else Color.Red)
                            )
                            Text(
                                text = if (uiState.isBleScannerServiceActive) "SCANNING ACTIVE • %.1f Hz".format(1.2f) else "SCANNER OFFLINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.Gray
                            )
                        }
                    }

                    // Compact state indicator & alarm light
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val alarmColor = when (uiState.currentAlarmState) {
                            AlarmState.TRIGGERED -> Color(0xFFFF3366)
                            AlarmState.APPROACHING -> Color(0xFFFF9900)
                            AlarmState.COOLDOWN -> Color(0xFF00E5FF)
                            else -> Color(0xFF00FF66).copy(alpha = 0.5f)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = alarmColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, alarmColor)
                        ) {
                            Text(
                                text = uiState.currentAlarmState.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = alarmColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        // Gemini Threat analyzer status badge
                        val geminiColor = if (uiState.isAiAnalyzingThreats) Color(0xFFD066FF) else Color(0xFF00E5FF)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = geminiColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, geminiColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = if (uiState.isAiAnalyzingThreats) "AI ANALYSIS..." else "AI READY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = geminiColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // ==================================================
            // 2. LOCKED TARGET CARD
            // ==================================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .testTag("locked_target_display_card"),
                shape = RoundedCornerShape(12.dp),
                color = if (lockedTarget != null) Color(0xFF101B14) else Color(0xFF0A0F0B),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (lockedTarget != null) Color(0xFFFFD700).copy(alpha = 0.4f) else Color(0xFF00FF66).copy(alpha = 0.15f)
                )
            ) {
                if (lockedTarget == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "NO TARGET LOCKED",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.8.sp
                            ),
                            color = Color.Gray
                        )
                        Text(
                            text = "Select a BLE or Wi-Fi device to begin tracking.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFD700))
                                )
                                Text(
                                    text = "LOCKED TARGET",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color(0xFFFFD700)
                                )
                            }
                            Text(
                                text = lockedTarget.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (showMaskedAddresses && lockedTarget.id.length > 8) {
                                    "${lockedTarget.id.take(4)}:***:${lockedTarget.id.takeLast(4)} • ${lockedTarget.type}"
                                } else "${lockedTarget.id} • ${lockedTarget.type}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                ),
                                color = Color.Gray
                            )
                        }

                        // Locked target metrics
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "${lockedTarget.rssi} dBm",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (lockedTarget.rssi >= -65) Color(0xFF00FF66) else Color(0xFFFFCC00)
                                )
                                Text(
                                    text = "FLTR: %d dBm".format(filteredRssi.toInt()),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "QLTY: %d%% • SMPL: %d".format(finalQualityScore, samplesCount),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp
                                    ),
                                    color = Color.Gray
                                )
                            }

                            // Unlock button (explicit unlock only, no auto-replaces)
                            Button(
                                onClick = { onSelectTargetDevice(null) },
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .testTag("unlock_target_button"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF3366).copy(alpha = 0.15f),
                                    contentColor = Color(0xFFFF3366)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFFF3366).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "UNLOCK",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ==================================================
            // 3. CENTER: LARGE RADAR / MAP VIEWPORT
            // ==================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF020703))
                    .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                when (activeTab) {
                    ScannerTab.SCAN, ScannerTab.TRACK -> {
                        // Render standard Tactical Radar Canvas
                        TacticalRadarCanvas(
                            headingDegrees = uiState.headingDegrees,
                            blips = searchedBlips,
                            nearestBlipId = uiState.nearestBlip?.id,
                            selectedTargetDeviceId = uiState.selectedTargetDeviceId,
                            perimeterThresholdMeters = uiState.perimeterThresholdMeters,
                            mapRangeMeters = currentZoomMeters,
                            isHudDeclutterEnabled = uiState.isHudDeclutterEnabled,
                            isFocusModeEnabled = activeTab == ScannerTab.TRACK,
                            onSelectTargetDevice = onSelectTargetDevice,
                            measurementHistory = targetMeasurements,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    ScannerTab.LOCALIZE -> {
                        // Render Beautiful Cartesian Grid localization map
                        Box(modifier = Modifier.fillMaxSize()) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                // Convert 1 meter to pixels based on scale and zoom preset
                                val scale = (size.width / 2f) / currentZoomMeters

                                // A. Draw coordinate grid lines
                                for (i in -10..10) {
                                    val offset = i * scale * 2f // Every 2 meters
                                    drawLine(
                                        color = Color(0xFF00FF66).copy(alpha = 0.06f),
                                        start = Offset(center.x + offset, 0f),
                                        end = Offset(center.x + offset, size.height),
                                        strokeWidth = 1f
                                    )
                                    drawLine(
                                        color = Color(0xFF00FF66).copy(alpha = 0.06f),
                                        start = Offset(0f, center.y + offset),
                                        end = Offset(size.width, center.y + offset),
                                        strokeWidth = 1f
                                    )
                                }

                                // B. Concentric circles & distance tick markers
                                for (r in listOf(2f, 5f, 10f, 20f, 30f)) {
                                    drawCircle(
                                        color = Color(0xFF00FF66).copy(alpha = 0.08f),
                                        radius = r * scale,
                                        center = center,
                                        style = Stroke(
                                            width = 1f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)
                                        )
                                    )
                                }

                                // C. Draw DIRECT MEASUREMENTS (solid history points)
                                targetMeasurements.forEach { pt ->
                                    val ptX = center.x + pt.xOffsetMeters * scale
                                    val ptY = center.y - pt.yOffsetMeters * scale

                                    // Outer ring indicating high-quality direct measurement
                                    drawCircle(
                                        color = Color(0xFF00FF66).copy(alpha = 0.5f),
                                        radius = 6f,
                                        center = Offset(ptX, ptY)
                                    )
                                    drawCircle(
                                        color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                        radius = 12f,
                                        center = Offset(ptX, ptY),
                                        style = Stroke(width = 1f)
                                    )
                                }

                                // D. Draw ESTIMATED source probability volume & uncertainty bounds
                                if (volume != null && volume.isValid && targetMeasurements.size >= 4) {
                                    val volX = center.x + volume.centerEnu.x * scale
                                    val volY = center.y - volume.centerEnu.y * scale
                                    val rMajor = volume.majorAxisMeters * scale
                                    val rMinor = volume.minorAxisMeters * scale

                                    // Draw Translucent Ellipse Aura
                                    rotate(degrees = volume.ellipseOrientationDegrees, pivot = Offset(volX, volY)) {
                                        drawOval(
                                            color = Color(0xFFD066FF).copy(alpha = 0.08f),
                                            topLeft = Offset(volX - rMajor, volY - rMinor),
                                            size = Size(rMajor * 2f, rMinor * 2f)
                                        )
                                        drawOval(
                                            color = Color(0xFFD066FF).copy(alpha = 0.6f),
                                            topLeft = Offset(volX - rMajor, volY - rMinor),
                                            size = Size(rMajor * 2f, rMinor * 2f),
                                            style = Stroke(
                                                width = 2f,
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                            )
                                        )
                                    }

                                    // Draw inferred gradient center point
                                    drawCircle(
                                        color = Color(0xFFD066FF),
                                        radius = 8f,
                                        center = Offset(volX, volY)
                                    )
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.3f),
                                        radius = 14f,
                                        center = Offset(volX, volY),
                                        style = Stroke(width = 1f)
                                    )
                                }

                                // E. User position reticle with heading
                                drawCircle(
                                    color = Color.White,
                                    radius = 7f,
                                    center = center
                                )
                                drawCircle(
                                    color = Color(0xFF00FF66).copy(alpha = 0.4f),
                                    radius = 14f,
                                    center = center,
                                    style = Stroke(width = 1.5f)
                                )

                                // Heading pointer vector
                                val headX = center.x + 18f * sin(uiState.headingDegrees * PI.toFloat() / 180f)
                                val headY = center.y - 18f * cos(uiState.headingDegrees * PI.toFloat() / 180f)
                                drawLine(
                                    color = Color(0xFF00FF66),
                                    start = center,
                                    end = Offset(headX, headY),
                                    strokeWidth = 3f
                                )
                            }

                            // Coordinate Labels overlay
                            Text(
                                text = "SOLVER SYSTEM • BAYESIAN MULTILATERATION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF00FF66),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                            )

                            // Status tags for current evidence visualization state
                            val stateLabel = when {
                                volume?.isValid == true && targetMeasurements.size >= 4 -> "ESTIMATED & CONVERGED"
                                targetMeasurements.isNotEmpty() -> "DIRECT MEASUREMENTS ONLY"
                                lockedTarget != null -> "INFERRED DIRECTION ONLY"
                                else -> "NO DATA ACQUIRED"
                            }
                            val stateColor = when {
                                volume?.isValid == true && targetMeasurements.size >= 4 -> Color(0xFFD066FF)
                                targetMeasurements.isNotEmpty() -> Color(0xFF00FF66)
                                lockedTarget != null -> Color(0xFFFF9900)
                                else -> Color.Gray
                            }

                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Black.copy(alpha = 0.8f),
                                border = BorderStroke(1.dp, stateColor.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "STATE: $stateLabel",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = stateColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    ScannerTab.AR -> {
                        // Re-use full live AR camera view
                        UwbArCameraScreen(
                            uiState = uiState,
                            mapRangeMeters = uiState.mapRangeMeters,
                            onSelectTargetDevice = onSelectTargetDevice,
                            modifier = Modifier.fillMaxSize(),
                            onAddSpatialPoint = onAddSpatialPoint
                        )
                    }
                }

                // ==================================================
                // 5. RADAR ZOOM CONTROLS (Layered gracefully over corner)
                // ==================================================
                if (activeTab != ScannerTab.AR) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "RADAR RANGE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.LightGray
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(5f, 15f, 30f, 60f).forEach { rangeMeters ->
                                val isSelected = kotlin.math.abs(uiState.currentRadarRangeMeters - rangeMeters) < 0.5f
                                Surface(
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .clickable { onSetMapRange(rangeMeters) }
                                        .testTag("tactical_range_chip_${rangeMeters.toInt()}m"),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF00FF66) else Color(0xFF06150C),
                                    border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF00FF66).copy(alpha = 0.35f))
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${rangeMeters.toInt()}m",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            ),
                                            color = if (isSelected) Color.Black else Color(0xFF00FF66)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================================================
            // 6. FILTER & SEARCH BAR (Compact layout)
            // ==================================================
            if (activeTab == ScannerTab.SCAN) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "Search signal...",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color.Gray
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("scanner_search_input"),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.White
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00FF66),
                                unfocusedBorderColor = Color(0xFF00FF66).copy(alpha = 0.3f),
                                focusedContainerColor = Color(0xFF09140D),
                                unfocusedContainerColor = Color(0xFF050A06)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear Search",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        )

                        // Collapsible Device List Sheet Trigger button
                        Button(
                            onClick = { isDeviceListExpanded = !isDeviceListExpanded },
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("toggle_device_list_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDeviceListExpanded) Color(0xFF00FF66) else Color(0xFF0E1F14),
                                contentColor = if (isDeviceListExpanded) Color.Black else Color(0xFF00FF66)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = if (isDeviceListExpanded) Icons.Default.ExpandMore else Icons.Default.Menu,
                                contentDescription = "Toggle Device Drawer",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isDeviceListExpanded) "COLLAPSE" else "BROWSE DEVICES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }

                    // Compact filter chips row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("ALL", "WIFI", "BLE", "CELLULAR", "OTHER").forEach { type ->
                            val isSelected = uiState.selectedFilterType == type
                            Surface(
                                modifier = Modifier
                                    .clickable { onFilterSelected(type) }
                                    .testTag("filter_chip_$type"),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFF00FF66).copy(alpha = 0.25f) else Color.Transparent,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF00FF66) else Color(0xFF00FF66).copy(alpha = 0.2f)
                                )
                            ) {
                                Text(
                                    text = type,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal
                                    ),
                                    color = if (isSelected) Color(0xFF00FF66) else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Toggle Mac address mask icon
                        IconButton(
                            onClick = { showMaskedAddresses = !showMaskedAddresses },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("scanner_toggle_mask_button")
                        ) {
                            Icon(
                                imageVector = if (showMaskedAddresses) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle MAC masking",
                                tint = Color(0xFF00FF66).copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // ==================================================
            // 7. EXPANDABLE DEVICE LIST DRAWER
            // ==================================================
            if (activeTab == ScannerTab.SCAN) {
                AnimatedVisibility(
                    visible = isDeviceListExpanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(horizontal = 12.dp)
                            .testTag("collapsible_device_list_drawer"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF08100B),
                        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "DISCOVERED RF SIGNALS (%d)".format(searchedBlips.size),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF00FF66).copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            if (searchedBlips.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No matching signals in current environment.",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = Color.DarkGray
                                    )
                                }
                            } else {
                                val listState = rememberLazyListState()

                                // Partition list to pin locked target on top
                                val pinnedDevicesList = remember(searchedBlips, uiState.selectedTargetDeviceId) {
                                    val pinned = searchedBlips.filter { it.id == uiState.selectedTargetDeviceId }
                                    val remaining = searchedBlips.filter { it.id != uiState.selectedTargetDeviceId }
                                    pinned + remaining
                                }

                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .testTag("tactical_scanner_device_list"),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(pinnedDevicesList, key = { "${it.type}_${it.id}" }) { blip ->
                                        var isExpanded by remember { mutableStateOf(false) }
                                        val isSelected = blip.id == uiState.selectedTargetDeviceId

                                        val rssiColor = when {
                                            blip.rssi >= -65 -> Color(0xFF00FF66)
                                            blip.rssi >= -85 -> Color(0xFFFFCC00)
                                            else -> Color(0xFFFF3366)
                                        }

                                        val signalIcon = when (blip.type) {
                                            "WIFI" -> Icons.Default.Wifi
                                            "BLE" -> Icons.Default.Bluetooth
                                            "CELLULAR" -> Icons.Default.CellTower
                                            else -> Icons.Default.Sensors
                                        }

                                        val displayAddr = if (showMaskedAddresses && blip.id.length > 8) {
                                            "${blip.id.take(4)}:***:${blip.id.takeLast(4)}"
                                        } else blip.id

                                        val timeDeltaSeconds = (System.currentTimeMillis() - blip.timestampMs) / 1000f

                                        Surface(
                                            onClick = { isExpanded = !isExpanded },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) Color(0xFF102418) else Color(0xFF050A06),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) Color(0xFFFFD700).copy(alpha = 0.5f) else Color(0xFF00FF66).copy(alpha = 0.15f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("scanner_device_item_${blip.id}")
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            imageVector = signalIcon,
                                                            contentDescription = blip.type,
                                                            tint = rssiColor,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Column {
                                                            Text(
                                                                text = blip.name,
                                                                style = MaterialTheme.typography.bodySmall.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontFamily = FontFamily.Monospace
                                                                ),
                                                                color = if (isSelected) Color(0xFFFFD700) else Color.White,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "$displayAddr • ${blip.type}",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontFamily = FontFamily.Monospace,
                                                                    fontSize = 8.sp
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
                                                                color = rssiColor
                                                            )
                                                            Text(
                                                                text = "seen %.1f s".format(timeDeltaSeconds),
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontFamily = FontFamily.Monospace,
                                                                    fontSize = 8.sp
                                                                ),
                                                                color = Color.LightGray
                                                            )
                                                        }

                                                        // Compact LOCK action
                                                        Button(
                                                            onClick = { onSelectTargetDevice(if (isSelected) null else blip.id) },
                                                            modifier = Modifier
                                                                .height(24.dp)
                                                                .testTag("lock_action_${blip.id}"),
                                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                            shape = RoundedCornerShape(4.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = if (isSelected) Color(0xFFFFD700).copy(alpha = 0.2f) else Color(0xFF00FF66).copy(alpha = 0.1f),
                                                                contentColor = if (isSelected) Color(0xFFFFD700) else Color(0xFF00FF66)
                                                            ),
                                                            border = BorderStroke(
                                                                1.dp,
                                                                if (isSelected) Color(0xFFFFD700).copy(alpha = 0.5f) else Color(0xFF00FF66).copy(alpha = 0.3f)
                                                            )
                                                        ) {
                                                            Text(
                                                                text = if (isSelected) "LOCKED" else "LOCK",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 8.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontFamily = FontFamily.Monospace
                                                                )
                                                            )
                                                        }
                                                    }
                                                }

                                                // Clean expandable detailed parameters view
                                                if (isExpanded) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(vertical = 6.dp),
                                                        color = Color(0xFF00FF66).copy(alpha = 0.15f)
                                                    )
                                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                        Text(
                                                            text = "OUI Vendor: ${blip.ouiVendor ?: "Unidentified Broadcast"}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp),
                                                            color = if (blip.isHighRiskVendor) Color.Red else Color.LightGray
                                                        )
                                                        Text(
                                                            text = "Baseline: ${blip.baselineState} • Ranging: ${blip.csRangingMethod}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp),
                                                            color = Color.Gray
                                                        )
                                                        Text(
                                                            text = "Freq: %.1f MHz • Accuracy: %.2f m".format(blip.frequencyMhz, blip.csEstimatedAccuracyMeters),
                                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp),
                                                            color = Color.Gray
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
            }

            // ==================================================
            // 9. EXPANDABLE ADVANCED CONTROL PANEL
            // ==================================================
            if (activeTab == ScannerTab.TRACK) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { isControlsExpanded = !isControlsExpanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("toggle_advanced_controls_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0E1F14),
                            contentColor = Color(0xFF00FF66)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f))
                    ) {
                        Icon(
                            imageVector = if (isControlsExpanded) Icons.Default.ExpandLess else Icons.Default.Settings,
                            contentDescription = "Toggle controls drawer",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isControlsExpanded) "HIDE ADVANCED CONTROLS" else "SHOW ADVANCED CONTROLS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            )
                        )
                    }

                    AnimatedVisibility(
                        visible = isControlsExpanded,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF08120A),
                            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "TACTICAL RECEIVER PARAMETERS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 8.sp
                                    ),
                                    color = Color(0xFF00FF66)
                                )

                                // Scan speed slider simulation
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Scan Speed: 1.2s sweep",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = Color.LightGray
                                    )
                                    Switch(
                                        checked = uiState.isScanningActive,
                                        onCheckedChange = { onToggleScanning() },
                                        modifier = Modifier.scale(0.7f)
                                    )
                                }

                                // Audio Alert trigger
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Audio Proximity Sonar",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = Color.LightGray
                                    )
                                    IconButton(
                                        onClick = { onPlayTestPing(1200.0) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Test sound",
                                            tint = Color(0xFF00FF66),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Max targets selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Max Targets Shown",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = "${uiState.maxVisibleDevices}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                        color = Color(0xFF00FF66)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==================================================
            // 9B. LOCALIZE SOLVER DETAILS PANEL
            // ==================================================
            if (activeTab == ScannerTab.LOCALIZE) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .testTag("localize_solver_telemetry_card"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF070D09),
                    border = BorderStroke(1.dp, Color(0xFFD066FF).copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "SOLVER METRICS & MODEL COVARIANCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 8.sp,
                                letterSpacing = 1.sp
                            ),
                            color = Color(0xFFD066FF)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Residual RMSE: %.2f dB".format(volume?.rmse ?: 0f),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp),
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Consistency Score: ${volume?.modelConsistency ?: "UNKNOWN"}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp),
                                    color = Color.LightGray
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Eigen Major: %.2fm".format(volume?.majorAxisMeters ?: 0f),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp),
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Total Samples: %d".format(samplesCount),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp),
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==================================================
        // 10. ALARM STATE TRANSITION WARNING POPUP
        // ==================================================
        AnimatedVisibility(
            visible = showAlertBanner,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E0C0C),
                border = BorderStroke(2.dp, Color(0xFFFF3366)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alarm_transition_popup_banner")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alarm trigger",
                        tint = Color(0xFFFF3366),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "TACTICAL THRESHOLD BREACH",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFFFF3366)
                    )
                    Text(
                        text = lastAlertBannerMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { showAlertBanner = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF3366),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ACKNOWLEDGE SECURE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // ==================================================
        // 4. BOTTOM COMPACT NAVIGATION
        // ==================================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            color = Color(0xFF060D08),
            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScannerTab.entries.forEach { tab ->
                    val isSelected = activeTab == tab
                    val (icon, label) = when (tab) {
                        ScannerTab.SCAN -> Icons.Default.Radar to "SCAN"
                        ScannerTab.TRACK -> Icons.Default.MyLocation to "TRACK"
                        ScannerTab.LOCALIZE -> Icons.Default.GridOn to "LOCALIZE"
                        ScannerTab.AR -> Icons.Default.QrCodeScanner to "AR"
                    }

                    Surface(
                        onClick = {
                            activeTab = tab
                            // Sync tab with original optical/view state if needed
                            if (tab == ScannerTab.AR) {
                                // Keep AR mode integrated
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF00FF66).copy(alpha = 0.15f) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scanner_bottom_tab_${tab.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) Color(0xFF00FF66) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = if (isSelected) Color(0xFF00FF66) else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
