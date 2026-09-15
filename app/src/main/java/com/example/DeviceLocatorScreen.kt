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

    // Find our currently selected target
    val targetBlip = remember(activeBlips, selectedTargetId) {
        activeBlips.find { it.id == selectedTargetId || it.name == selectedTargetId }
    }

    // Interactive simulator walk offset to let the user "walk" on the emulator
    var simulatorWalkOffset by remember { mutableStateOf(0f) } // -10m to +10m offset
    var isSimulationModeEnabled by remember { mutableStateOf(false) }

    // Computes effective distance and RSSI, blending live values with simulation tweaks if enabled
    val effectiveRssi = remember(targetBlip, isSimulationModeEnabled, simulatorWalkOffset) {
        if (targetBlip == null) -100 else {
            if (isSimulationModeEnabled) {
                // Closer walk offset = stronger RSSI
                val baseRssi = -60f
                val walkFactor = simulatorWalkOffset * 3.5f // 3.5 dB per meter walk
                (baseRssi + walkFactor).coerceIn(-100f, -40f).toInt()
            } else {
                targetBlip.rssi
            }
        }
    }

    val effectiveDistance = remember(targetBlip, isSimulationModeEnabled, simulatorWalkOffset) {
        if (targetBlip == null) 15.0f else {
            if (isSimulationModeEnabled) {
                // Base 5 meters, adjusted by walk offset
                (5.0f - simulatorWalkOffset).coerceIn(0.5f, 25.0f)
            } else {
                targetBlip.distance
            }
        }
    }

    // Directional indicator tracking (simulated angular deviation)
    var relativeTargetAngle by remember { mutableStateOf(45f) } // relative to user's face-heading
    LaunchedEffect(targetBlip, uiState.headingDegrees) {
        if (targetBlip != null) {
            // Target angle relative to the phone's heading
            val diff = (targetBlip.targetAngleOffset - uiState.headingDegrees + 360f) % 360f
            relativeTargetAngle = diff
        }
    }

    // Signal Strength Trend tracking (Hot / Cold / Stable)
    var lastRssi by remember { mutableStateOf(effectiveRssi) }
    var signalTrend by remember { mutableStateOf("STABLE") } // "HOTTER", "COLDER", "STABLE"
    LaunchedEffect(effectiveRssi) {
        if (effectiveRssi > lastRssi) {
            signalTrend = "HOTTER"
        } else if (effectiveRssi < lastRssi) {
            signalTrend = "COLDER"
        }
        lastRssi = effectiveRssi
    }

    // Real-Time Audio Sonar Ticker
    var isAudioLocatorEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(isAudioLocatorEnabled, effectiveRssi) {
        if (isAudioLocatorEnabled) {
            val toneGen = try {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
            } catch (e: Exception) {
                null
            }
            if (toneGen != null) {
                try {
                    while (isAudioLocatorEnabled) {
                        // Stronger signal (e.g. -45 dBm) = shorter interval between ticks (e.g. 150ms)
                        // Weaker signal (e.g. -95 dBm) = longer interval between ticks (e.g. 1300ms)
                        val clampedRssi = effectiveRssi.coerceIn(-95, -45)
                        val ratio = (clampedRssi + 95) / 50f // 0.0 to 1.0
                        val delayMs = (1300 - (ratio * 1150)).toLong().coerceIn(100L, 1400L)

                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
                        delay(delayMs)
                    }
                } catch (e: Exception) {
                    // Ignore
                } finally {
                    toneGen.release()
                }
            }
        }
    }

    // Real-Time Haptic Pulse Loop
    var isHapticLocatorEnabled by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(isHapticLocatorEnabled, effectiveRssi) {
        if (isHapticLocatorEnabled) {
            while (isHapticLocatorEnabled) {
                val clampedRssi = effectiveRssi.coerceIn(-95, -45)
                val ratio = (clampedRssi + 95) / 50f
                val delayMs = (1300 - (ratio * 1150)).toLong().coerceIn(100L, 1400L)

                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(delayMs)
            }
        }
    }

    // Radar Concentric Pulsing Animation
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
            // Target Selector Dropdown / Selection Bar
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
                // Empty tracking state
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
                // active locator display
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
                        // Direction Alignment Banner
                        val isAligned = relativeTargetAngle in 340f..360f || relativeTargetAngle in 0f..20f
                        val alignmentColor = if (isAligned) Color(0xFF00FF66) else Color.Red

                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
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
                                        text = if (isAligned) "ALIGNMENT LOCK: EMITTER DIRECTLY AHEAD" else "ORIENTATION DRIFT: ROTATE TO ALIGN",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = alignmentColor
                                    )
                                }
                            }
                        }

                        // Giant Canvas Pulse Radar & Compass pointer
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .drawBehind {
                                    // Base grid layout
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val radius = size.width / 2f
                                    drawCircle(
                                        color = Color(0xFF00FF66).copy(alpha = 0.1f),
                                        radius = radius,
                                        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                                    )

                                    // Drawing concentric distance lines
                                    for (i in 1..4) {
                                        drawCircle(
                                            color = Color(0xFF00FF66).copy(alpha = 0.05f * i),
                                            radius = radius * (i / 4f),
                                            style = Stroke(width = 1f)
                                        )
                                    }

                                    // Pulsing Sonar waves based on RSSI strength
                                    drawCircle(
                                        color = Color(0xFF00FF66).copy(alpha = (1f - pulseProgress) * 0.45f),
                                        radius = radius * pulseProgress,
                                        style = Stroke(width = 3f)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Compass / Relative bearing arrow
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                rotate(degrees = relativeTargetAngle, pivot = center) {
                                    // Pointer Line
                                    drawLine(
                                        color = if (isAligned) Color(0xFF00FF66) else Color(0xFF00FF66).copy(alpha = 0.4f),
                                        start = center,
                                        end = Offset(center.x, 24.dp.toPx()),
                                        strokeWidth = 3f
                                    )

                                    // Arrow head
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

                            // Center Digital Readout
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

                        // Digital Tactical Indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // RSSI readouts
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

                            // Emitter ID
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

                        // Toggle Buttons for Audio Sonar Guidance & Haptics
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

                        // Simulation / Walking Locomotion Helper Box to verify Hot / Cold in emulator
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1710)),
                            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsWalk,
                                            contentDescription = null,
                                            tint = Color(0xFF00FF66),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "WALK LOCOMOTION SIMULATOR",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Switch(
                                        checked = isSimulationModeEnabled,
                                        onCheckedChange = { isSimulationModeEnabled = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = Color(0xFF00FF66)
                                        ),
                                        modifier = Modifier.testTag("locator_simulation_toggle")
                                    )
                                }

                                Text(
                                    text = "Simulates physical walking actions in the browser emulator environment to verify Hot/Cold signal gradient changes.",
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace
                                )

                                AnimatedVisibility(visible = isSimulationModeEnabled) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Divider(color = Color(0xFF00FF66).copy(alpha = 0.15f))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Position: " + if (simulatorWalkOffset >= 0f) String.format("%.1f m closer", simulatorWalkOffset) else String.format("%.1f m further", -simulatorWalkOffset),
                                                color = Color(0xFF00FF66),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "Max proximity limit: 5m",
                                                color = Color.Gray,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Slider(
                                            value = simulatorWalkOffset,
                                            onValueChange = { simulatorWalkOffset = it },
                                            valueRange = -4f..4.5f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFF00FF66),
                                                activeTrackColor = Color(0xFF00FF66),
                                                inactiveTrackColor = Color(0xFF0B2114)
                                            ),
                                            modifier = Modifier.testTag("locator_slider")
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    simulatorWalkOffset = (simulatorWalkOffset + 0.5f).coerceIn(-4f, 4.5f)
                                                },
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f))
                                            ) {
                                                Text("Step Closer", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    simulatorWalkOffset = (simulatorWalkOffset - 0.5f).coerceIn(-4f, 4.5f)
                                                },
                                                modifier = Modifier.weight(1f),
                                                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f))
                                            ) {
                                                Text("Step Away", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
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
}
