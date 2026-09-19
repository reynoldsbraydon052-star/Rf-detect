package com.example

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceLocatorScreen(
    uiState: SignalRadarUiState,
    onSelectTargetDevice: (String?) -> Unit,
    onBackToRadar: () -> Unit,
    viewModel: SignalRadarViewModel? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val activeBlips = uiState.activeBlips
    val selectedTargetId = uiState.selectedTargetDeviceId ?: uiState.selectedDeviceId

    val targetBlip = remember(activeBlips, selectedTargetId) {
        activeBlips.find { it.id == selectedTargetId || it.name == selectedTargetId }
    }

    val effectiveRssi = remember(targetBlip) {
        targetBlip?.rssi ?: -100
    }

    val effectiveDistance = remember(targetBlip) {
        targetBlip?.distance ?: 15.0f
    }

    val bearingScanState by (viewModel?.bearingScanState?.collectAsState(ScanState.IDLE) ?: remember { mutableStateOf(ScanState.IDLE) })
    val bearingSweepResult by (viewModel?.bearingSweepResult?.collectAsState(null) ?: remember { mutableStateOf(null) })
    val bearingAccumulatedRotation by (viewModel?.bearingAccumulatedRotation?.collectAsState(0f) ?: remember { mutableStateOf(0f) })

    val relativeBearingOffset = remember(bearingScanState, uiState.headingDegrees, bearingSweepResult) {
        if (bearingScanState == ScanState.LOCKED) {
            viewModel?.getRelativeTargetOffset(uiState.headingDegrees)
        } else {
            null
        }
    }

    LaunchedEffect(uiState.headingDegrees, targetBlip, bearingScanState) {
        if (bearingScanState == ScanState.CALIBRATING_ROTATION && targetBlip != null) {
            viewModel?.feedBearingSample(
                currentAzimuth = uiState.headingDegrees,
                incomingMac = targetBlip.id,
                smoothedRssi = effectiveRssi.toDouble()
            )
        }
    }

    var relativeTargetAngle by remember { mutableStateOf(45f) }
    LaunchedEffect(targetBlip, uiState.headingDegrees, relativeBearingOffset) {
        if (relativeBearingOffset != null) {
            relativeTargetAngle = (relativeBearingOffset + 360f) % 360f
        } else if (targetBlip != null) {
            val diff = (targetBlip.targetAngleOffset - uiState.headingDegrees + 360f) % 360f
            relativeTargetAngle = diff
        }
    }

    var lastRssi by remember { mutableStateOf(effectiveRssi) }
    var signalTrend by remember { mutableStateOf("STABLE") }
    LaunchedEffect(effectiveRssi) {
        if (effectiveRssi > lastRssi) {
            signalTrend = "HOTTER"
        } else if (effectiveRssi < lastRssi) {
            signalTrend = "COLDER"
        }
        lastRssi = effectiveRssi
    }

    var isAudioLocatorEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(isAudioLocatorEnabled, effectiveRssi, bearingScanState, relativeBearingOffset) {
        if (isAudioLocatorEnabled) {
            val toneGen = try {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
            } catch (e: Exception) {
                null
            }
            if (toneGen != null) {
                try {
                    while (isAudioLocatorEnabled) {
                        val isLockedAligned = bearingScanState == ScanState.LOCKED && relativeBearingOffset != null && abs(relativeBearingOffset) <= 12f
                        val delayMs = if (isLockedAligned) {
                            75L
                        } else {
                            val clampedRssi = effectiveRssi.coerceIn(-95, -45)
                            val ratio = (clampedRssi + 95) / 50f
                            (1300 - (ratio * 1150)).toLong().coerceIn(100L, 1400L)
                        }

                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
                        delay(delayMs)
                    }
                } catch (e: Exception) {
                } finally {
                    toneGen.release()
                }
            }
        }
    }

    var isHapticLocatorEnabled by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(isHapticLocatorEnabled, effectiveRssi, bearingScanState, relativeBearingOffset) {
        if (isHapticLocatorEnabled) {
            while (isHapticLocatorEnabled) {
                val isLockedAligned = bearingScanState == ScanState.LOCKED && relativeBearingOffset != null && abs(relativeBearingOffset) <= 12f
                val delayMs = if (isLockedAligned) {
                    75L
                } else {
                    val clampedRssi = effectiveRssi.coerceIn(-95, -45)
                    val ratio = (clampedRssi + 95) / 50f
                    (1300 - (ratio * 1150)).toLong().coerceIn(100L, 1400L)
                }

                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(delayMs)
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = remember(effectiveRssi) {
                    val clamped = effectiveRssi.coerceIn(-100, -40)
                    val ratio = (clamped + 100) / 60f
                    (2000 - (ratio * 1500)).toInt().coerceIn(400, 2200)
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "REAL-TIME DEVICE LOCATOR",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            color = Color(0xFF00FF66)
                        )
                        Text(
                            text = "High-precision tactical signal strength tracking",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToRadar,
                        modifier = Modifier.testTag("locator_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF00FF66)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF07120B)
                )
            )
        },
        containerColor = Color(0xFF030705)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F13)),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TRACKING TARGET CONFIGURATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF00FF66)
                    )

                    var dropdownExpanded by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("locator_device_dropdown"),
                            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Radar,
                                        contentDescription = null,
                                        tint = Color(0xFF00FF66),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = targetBlip?.let { "${it.name} [${it.type}]" } ?: "SELECT EMITTER TO LOCATE...",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color(0xFF00FF66)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(Color(0xFF0C1F13))
                                .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f))
                        ) {
                            if (activeBlips.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No active emitters detected", color = Color.Gray, fontFamily = FontFamily.Monospace) },
                                    onClick = { dropdownExpanded = false }
                                )
                            } else {
                                activeBlips.forEach { blip ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${blip.name} (${blip.rssi} dBm)",
                                                    color = Color.White,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = blip.type,
                                                    color = Color(0xFF00FF66),
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        },
                                        onClick = {
                                            onSelectTargetDevice(blip.id)
                                            dropdownExpanded = false
                                        }
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
                        .height(300.dp)
                        .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .background(Color(0xFF050D08)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = Color(0xFF00FF66).copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "NO DEVICE SELECTED FOR TARGET TRACKING",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Select an emitter from the dropdown menu above\nto begin high-precision real-time proximity guidance.",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF050D08)),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val isAligned = if (bearingScanState == ScanState.LOCKED && relativeBearingOffset != null) {
                            abs(relativeBearingOffset) <= 12f
                        } else {
                            relativeTargetAngle in 340f..360f || relativeTargetAngle in 0f..20f
                        }

                        val alignmentColor = if (isAligned) Color(0xFF00FF66) else {
                            if (bearingScanState == ScanState.CALIBRATING_ROTATION) Color(0xFFFFFF00) else Color.Red
                        }

                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                when (bearingScanState) {
                                    ScanState.IDLE, ScanState.FAILED_INSUFFICIENT_DATA -> {
                                        if (bearingScanState == ScanState.FAILED_INSUFFICIENT_DATA) {
                                            Text(
                                                text = "CALIBRATION FAILED: INSUFFICIENT DATA",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = Color.Red,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                targetBlip.let {
                                                    viewModel?.startBearingCalibration(it.id, uiState.headingDegrees)
                                                }
                                            },
                                            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.6f)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = null,
                                                    tint = Color(0xFF00FF66),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "CALIBRATE BEARING (360°)",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF00FF66)
                                                )
                                            }
                                        }
                                    }
                                    ScanState.CALIBRATING_ROTATION -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFFFF00).copy(alpha = 0.15f))
                                                .border(1.dp, Color(0xFFFFFF00).copy(alpha = 0.4f))
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "HOLD DEVICE CLOSE TO CHEST & ROTATE 360° SLOWLY",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFFFFFF00),
                                                    textAlign = TextAlign.Center
                                                )
                                                val progress = (bearingAccumulatedRotation / 340f).coerceIn(0f, 1f)
                                                LinearProgressIndicator(
                                                    progress = progress,
                                                    color = Color(0xFFFFFF00),
                                                    trackColor = Color(0xFFFFFF00).copy(alpha = 0.2f),
                                                    modifier = Modifier.fillMaxWidth().height(4.dp)
                                                )
                                                Text(
                                                    text = "CALIBRATING ROTATION: ${(progress * 100).toInt()}%",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    color = Color.LightGray
                                                )
                                                TextButton(onClick = { viewModel?.resetBearingCalibration() }) {
                                                    Text(
                                                        text = "CANCEL",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.sp,
                                                        color = Color.Red
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    ScanState.LOCKED -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(alignmentColor.copy(alpha = 0.15f))
                                                .border(1.dp, alignmentColor.copy(alpha = 0.4f))
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isAligned) Icons.Default.CheckCircle else Icons.Default.Navigation,
                                                    contentDescription = null,
                                                    tint = alignmentColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = if (isAligned) {
                                                        "ALIGNMENT LOCKED: TARGET LOCK"
                                                    } else {
                                                        val directionText = if ((relativeBearingOffset ?: 0f) > 0f) "STEER RIGHT →" else "STEER LEFT ←"
                                                        "LOCK ACTIVE: $directionText"
                                                    },
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 11.sp,
                                                    color = alignmentColor
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val scorePct = ((bearingSweepResult?.confidenceScore ?: 0f) * 100).toInt()
                                            val sigDelta = String.format("%.1f", bearingSweepResult?.signalDelta ?: 0.0)
                                            Text(
                                                text = "CONFIDENCE: $scorePct% (Δ $sigDelta dBm)",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                            TextButton(
                                                onClick = { viewModel?.resetBearingCalibration() },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(
                                                    text = "RE-CALIBRATE",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF00FF66)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .drawBehind {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val radius = size.width / 2f
                                    drawCircle(
                                        color = Color(0xFF00FF66).copy(alpha = 0.1f),
                                        radius = radius,
                                        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                                    )

                                    for (i in 1..4) {
                                        drawCircle(
                                            color = Color(0xFF00FF66).copy(alpha = 0.05f * i),
                                            radius = radius * (i / 4f),
                                            style = Stroke(width = 1f)
                                        )
                                    }

                                    drawCircle(
                                        color = Color(0xFF00FF66).copy(alpha = (1f - pulseProgress) * 0.45f),
                                        radius = radius * pulseProgress,
                                        style = Stroke(width = 3f)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                rotate(degrees = relativeTargetAngle, pivot = center) {
                                    drawLine(
                                        color = if (isAligned) Color(0xFF00FF66) else Color(0xFF00FF66).copy(alpha = 0.4f),
                                        start = center,
                                        end = Offset(center.x, 24.dp.toPx()),
                                        strokeWidth = 3f
                                    )

                                    val path = Path().apply {
                                        moveTo(center.x, 12.dp.toPx())
                                        lineTo(center.x - 8.dp.toPx(), 28.dp.toPx())
                                        lineTo(center.x + 8.dp.toPx(), 28.dp.toPx())
                                        close()
                                    }
                                    drawPath(
                                        path = path,
                                        color = if (isAligned) Color(0xFF00FF66) else Color(0xFF00FF66).copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF030705))
                                    .border(1.5.dp, Color(0xFF00FF66).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "DISTANCE",
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format("%.1f m", effectiveDistance),
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when (signalTrend) {
                                                "HOTTER" -> Color(0xFF00FF66).copy(alpha = 0.2f)
                                                "COLDER" -> Color.Red.copy(alpha = 0.2f)
                                                else -> Color.Gray.copy(alpha = 0.2f)
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (signalTrend) {
                                                "HOTTER" -> Icons.Default.TrendingUp
                                                "COLDER" -> Icons.Default.TrendingDown
                                                else -> Icons.Default.TrendingFlat
                                            },
                                            contentDescription = null,
                                            tint = when (signalTrend) {
                                                "HOTTER" -> Color(0xFF00FF66)
                                                "COLDER" -> Color.Red
                                                else -> Color.Gray
                                            },
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = signalTrend,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = when (signalTrend) {
                                                "HOTTER" -> Color(0xFF00FF66)
                                                "COLDER" -> Color.Red
                                                else -> Color.Gray
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F13)),
                                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("SIGNAL RATIO", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$effectiveRssi dBm",
                                        color = Color(0xFF00FF66),
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F13)),
                                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("BAND GAP", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${targetBlip.frequencyMhz.toInt()} MHz",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { isAudioLocatorEnabled = !isAudioLocatorEnabled },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("locator_sound_toggle"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAudioLocatorEnabled) Color(0xFF00FF66) else Color(0xFF0B2114),
                                    contentColor = if (isAudioLocatorEnabled) Color.Black else Color.White
                                ),
                                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    imageVector = if (isAudioLocatorEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Audio Sonar",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { isHapticLocatorEnabled = !isHapticLocatorEnabled },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("locator_haptic_toggle"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isHapticLocatorEnabled) Color(0xFF00FF66) else Color(0xFF0B2114),
                                    contentColor = if (isHapticLocatorEnabled) Color.Black else Color.White
                                ),
                                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Haptic Pulse",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
