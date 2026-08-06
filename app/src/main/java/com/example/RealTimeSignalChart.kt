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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class SignalTimePoint(
    val timestampMs: Long,
    val rssiDbm: Int,
    val distanceMeters: Float
)

@Composable
fun RealTimeSignalStrengthChartCard(
    activeBlips: List<RadarBlip>,
    selectedTargetDeviceId: String?,
    onSelectTargetDevice: (String?) -> Unit,
    onOpenArCameraForTarget: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var chartMode by remember { mutableStateOf("TIME_SERIES") } // "TIME_SERIES", "RF_SPECTRUM", "DRIFT_DELTA"
    var selectedFilter by remember { mutableStateOf("ALL") }
    var isScrubberActive by remember { mutableStateOf(false) }
    var scrubberXRatio by remember { mutableStateOf(0.8f) }

    // Rolling signal strength history map in memory (deviceId -> List<SignalTimePoint>)
    val signalHistoryMap = remember { mutableStateMapOf<String, MutableList<SignalTimePoint>>() }
    val currentTimeMs = System.currentTimeMillis()

    LaunchedEffect(activeBlips) {
        val now = System.currentTimeMillis()
        activeBlips.forEach { blip ->
            val list = signalHistoryMap.getOrPut(blip.id) { mutableListOf() }
            list.add(SignalTimePoint(now, blip.rssi, blip.distance))
            // Keep last 60 samples (~60 seconds)
            if (list.size > 60) {
                list.removeAt(0)
            }
        }
    }

    val filteredBlips = remember(activeBlips, selectedFilter) {
        if (selectedFilter == "ALL") activeBlips
        else activeBlips.filter { it.type.equals(selectedFilter, ignoreCase = true) }
    }

    val selectedBlip = remember(activeBlips, selectedTargetDeviceId) {
        activeBlips.firstOrNull { it.id == selectedTargetDeviceId || it.name == selectedTargetDeviceId }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_signal_strength_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                            .size(32.dp)
                            .background(Color(0xFF00FF66).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Signal Chart",
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "REAL-TIME RF SIGNAL STRENGTH CHART",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF00FF66)
                        )
                        Text(
                            text = "Live RSSI Time-Series & RF Spectrum Density Changes",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Gray
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "TIME_SERIES" to "TIME",
                        "RF_SPECTRUM" to "SPECTRUM",
                        "DRIFT_DELTA" to "DELTA"
                    ).forEach { (modeKey, label) ->
                        val isSel = chartMode == modeKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) Color(0xFF00FF66) else Color(0xFF0E2417))
                                .clickable { chartMode = modeKey }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (isSel) Color.Black else Color(0xFF00FF66)
                            )
                        }
                    }
                }
            }

            // Filter Chips Bar
            FilterChipsRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            // Selected Target Callout Banner if present
            if (selectedBlip != null) {
                SelectedTargetPinpointBanner(
                    selectedBlip = selectedBlip,
                    history = signalHistoryMap[selectedBlip.id] ?: emptyList(),
                    onClearSelection = { onSelectTargetDevice(null) },
                    onOpenArCamera = { onOpenArCameraForTarget?.invoke(selectedBlip.id) }
                )
            }

            // Canvas Chart Area
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseGlowRadius by infiniteTransition.animateFloat(
                initialValue = 10f,
                targetValue = 24f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseGlowRadius"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF020905))
                    .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(filteredBlips, selectedTargetDeviceId) {
                            detectTapGestures { tapOffset ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val paddingLeft = 45f
                                val paddingRight = 15f
                                val paddingTop = 28f
                                val paddingBottom = 28f
                                val graphW = w - paddingLeft - paddingRight
                                val graphH = h - paddingTop - paddingBottom
                                val minRssi = -100f
                                val maxRssi = -20f

                                var closestBlipId: String? = null
                                var minDistance = Float.MAX_VALUE

                                filteredBlips.forEach { blip ->
                                    val history = signalHistoryMap[blip.id] ?: emptyList()
                                    if (history.isNotEmpty()) {
                                        val lastPt = history.last()
                                        val headX = paddingLeft + graphW
                                        val normY = 1.0f - ((lastPt.rssiDbm.toFloat() - minRssi) / (maxRssi - minRssi)).coerceIn(0f, 1f)
                                        val headY = paddingTop + (normY * graphH)
                                        val dist = kotlin.math.hypot((headX - tapOffset.x).toDouble(), (headY - tapOffset.y).toDouble()).toFloat()
                                        if (dist < minDistance) {
                                            minDistance = dist
                                            closestBlipId = blip.id
                                        }
                                    }
                                }

                                if (closestBlipId != null && minDistance <= 80f) {
                                    onSelectTargetDevice(if (closestBlipId == selectedTargetDeviceId) null else closestBlipId)
                                } else {
                                    isScrubberActive = true
                                    scrubberXRatio = (tapOffset.x / size.width).coerceIn(0.05f, 0.95f)
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, _, _ ->
                                isScrubberActive = true
                                scrubberXRatio = ((scrubberXRatio * size.width + pan.x) / size.width).coerceIn(0.05f, 0.95f)
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    val paddingTop = 28f
                    val paddingBottom = 28f
                    val paddingLeft = 45f
                    val paddingRight = 15f

                    val graphW = w - paddingLeft - paddingRight
                    val graphH = h - paddingTop - paddingBottom

                    // Draw Horizontal Grid Lines (RSSI dBm: -100 to -20)
                    val minRssi = -100f
                    val maxRssi = -20f
                    val rssiStep = 20f

                    var currRssi = minRssi
                    while (currRssi <= maxRssi) {
                        val normY = 1.0f - ((currRssi - minRssi) / (maxRssi - minRssi))
                        val y = paddingTop + (normY * graphH)

                        drawLine(
                            color = Color(0xFF00FF66).copy(alpha = 0.12f),
                            start = Offset(paddingLeft, y),
                            end = Offset(w - paddingRight, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )

                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 20f
                            typeface = android.graphics.Typeface.MONOSPACE
                            isAntiAlias = true
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            "${currRssi.toInt()}dBm",
                            10f,
                            y + 6f,
                            textPaint
                        )

                        currRssi += rssiStep
                    }

                    // Vertical Time Grid Lines (-60s, -45s, -30s, -15s, NOW)
                    val timeTicks = listOf("-60s", "-45s", "-30s", "-15s", "NOW")
                    timeTicks.forEachIndexed { idx, label ->
                        val xRatio = idx / (timeTicks.size - 1).toFloat()
                        val x = paddingLeft + (xRatio * graphW)

                        drawLine(
                            color = Color(0xFF00FF66).copy(alpha = 0.15f),
                            start = Offset(x, paddingTop),
                            end = Offset(x, h - paddingBottom),
                            strokeWidth = 1f
                        )

                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 18f
                            typeface = android.graphics.Typeface.MONOSPACE
                            isAntiAlias = true
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            x - 15f,
                            h - 8f,
                            textPaint
                        )
                    }

                    if (chartMode == "TIME_SERIES" || chartMode == "DRIFT_DELTA") {
                        // Plot Time-Series Lines for active blips
                        filteredBlips.forEach { blip ->
                            val history = signalHistoryMap[blip.id] ?: emptyList()
                            val isSelected = blip.id == selectedTargetDeviceId || blip.name == selectedTargetDeviceId

                            val lineAlpha = if (isSelected) 1.0f else if (selectedTargetDeviceId != null) 0.22f else 0.8f
                            val strokeW = if (isSelected) 4.2f else if (selectedTargetDeviceId != null) 1.5f else 2.0f

                            val signalColor = when {
                                isSelected -> Color(0xFFFFCC00)
                                blip.type == "WIFI" -> Color(0xFF00FF66)
                                blip.type == "BLE" -> Color(0xFF00E5FF)
                                blip.type == "CELLULAR" -> Color(0xFFFF3366)
                                blip.type == "MAGNETIC" -> Color(0xFFFFCC00)
                                else -> Color(0xFFFF9900)
                            }

                            if (history.size >= 2) {
                                val path = Path()
                                val samples = history.takeLast(30)
                                samples.forEachIndexed { i, pt ->
                                    val xRatio = i / (samples.size - 1).toFloat()
                                    val x = paddingLeft + (xRatio * graphW)

                                    val valRssi = if (chartMode == "DRIFT_DELTA" && i > 0) {
                                        val prev = samples[i - 1].rssiDbm
                                        -60f + (pt.rssiDbm - prev) * 5f
                                    } else {
                                        pt.rssiDbm.toFloat()
                                    }

                                    val normY = 1.0f - ((valRssi - minRssi) / (maxRssi - minRssi)).coerceIn(0f, 1f)
                                    val y = paddingTop + (normY * graphH)

                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }

                                if (isSelected) {
                                    // Glowing animated thick halo line
                                    drawPath(
                                        path = path,
                                        color = signalColor.copy(alpha = 0.4f),
                                        style = Stroke(width = 10f, cap = StrokeCap.Round)
                                    )
                                }

                                drawPath(
                                    path = path,
                                    color = signalColor.copy(alpha = lineAlpha),
                                    style = Stroke(
                                        width = strokeW,
                                        cap = StrokeCap.Round
                                    )
                                )

                                // Current Head Point
                                val lastPt = samples.last()
                                val headX = paddingLeft + graphW
                                val normY = 1.0f - ((lastPt.rssiDbm.toFloat() - minRssi) / (maxRssi - minRssi)).coerceIn(0f, 1f)
                                val headY = paddingTop + (normY * graphH)

                                drawCircle(
                                    color = signalColor.copy(alpha = lineAlpha),
                                    radius = if (isSelected) 8f else 4f,
                                    center = Offset(headX, headY)
                                )

                                if (isSelected) {
                                    drawCircle(
                                        color = signalColor.copy(alpha = 0.5f),
                                        radius = pulseGlowRadius,
                                        center = Offset(headX, headY),
                                        style = Stroke(width = 2f)
                                    )
                                }
                            }
                        }
                    } else if (chartMode == "RF_SPECTRUM") {
                        // Plot RF Spectrum Power Density curve across frequencies
                        val spectrumPath = Path()
                        val numBins = 60
                        for (bin in 0..numBins) {
                            val ratio = bin / numBins.toFloat()
                            val freqMhz = 10.0 + (ratio * 6000.0)
                            val x = paddingLeft + (ratio * graphW)

                            // Aggregate power from blips near this frequency
                            var totalPower = -95.0
                            filteredBlips.forEach { blip ->
                                val freqDiff = abs(blip.frequencyMhz - freqMhz)
                                if (freqDiff < 300.0) {
                                    val contrib = blip.rssi + (300.0 - freqDiff) / 300.0 * 25.0
                                    totalPower = max(totalPower, contrib)
                                }
                            }

                            val normY = 1.0f - ((totalPower.toFloat() - minRssi) / (maxRssi - minRssi)).coerceIn(0f, 1f)
                            val y = paddingTop + (normY * graphH)

                            if (bin == 0) {
                                spectrumPath.moveTo(x, h - paddingBottom)
                                spectrumPath.lineTo(x, y)
                            } else {
                                spectrumPath.lineTo(x, y)
                            }
                        }
                        spectrumPath.lineTo(paddingLeft + graphW, h - paddingBottom)
                        spectrumPath.close()

                        drawPath(
                            path = spectrumPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF00FF66).copy(alpha = 0.6f),
                                    Color(0xFF00FF66).copy(alpha = 0.05f)
                                )
                            ),
                            style = Fill
                        )

                        drawPath(
                            path = spectrumPath,
                            color = Color(0xFF00FF66),
                            style = Stroke(width = 2f)
                        )
                    }

                    // Interactive Scrubber Line if active
                    if (isScrubberActive) {
                        val sx = paddingLeft + (scrubberXRatio * graphW)
                        drawLine(
                            color = Color(0xFFFFCC00),
                            start = Offset(sx, paddingTop),
                            end = Offset(sx, h - paddingBottom),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )

                        drawCircle(
                            color = Color(0xFFFFCC00),
                            radius = 4f,
                            center = Offset(sx, paddingTop)
                        )

                        // Draw Scrubber Value Callout Box
                        val scrubberTimeSec = ((1f - scrubberXRatio) * 60).toInt()
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.YELLOW
                            textSize = 20f
                            typeface = android.graphics.Typeface.MONOSPACE
                            isAntiAlias = true
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            "-${scrubberTimeSec}s",
                            (sx - 20f).coerceIn(paddingLeft, w - 60f),
                            paddingTop - 6f,
                            textPaint
                        )
                    }
                }
            }

            // Bottom Legend & Quick Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendIndicator("WiFi", Color(0xFF00FF66))
                    LegendIndicator("BLE", Color(0xFF00E5FF))
                    LegendIndicator("Cellular", Color(0xFFFF3366))
                    if (selectedTargetDeviceId != null) {
                        LegendIndicator("Locked Target", Color(0xFFFFCC00))
                    }
                }

                Text(
                    text = "${filteredBlips.size} SIGNALS TRACKED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun SelectedTargetPinpointBanner(
    selectedBlip: RadarBlip,
    history: List<SignalTimePoint>,
    onClearSelection: () -> Unit,
    onOpenArCamera: () -> Unit
) {
    val currentRssi = selectedBlip.rssi
    val avgRssi = if (history.isNotEmpty()) history.map { it.rssiDbm }.average() else currentRssi.toDouble()
    val driftTrendDbm = if (history.size >= 5) {
        val recent = history.takeLast(5).map { it.rssiDbm }
        (recent.last() - recent.first()).toFloat() / 5f
    } else 0f

    val trendLabel = when {
        driftTrendDbm > 0.5f -> "APPROACHING (+%.1f dB/s)".format(driftTrendDbm)
        driftTrendDbm < -0.5f -> "RECEDING (%.1f dB/s)".format(driftTrendDbm)
        else -> "STABLE SIGNAL"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFCC00).copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color(0xFFFFCC00).copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFFCC00).copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "Target Lock",
                        tint = Color(0xFFFFCC00),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = selectedBlip.name.take(18),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFFFFCC00)
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFCC00).copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "LOCKED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFFFFCC00),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "RSSI: ${currentRssi}dBm • DIST: %.1fm • %s".format(selectedBlip.distance, trendLabel),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        ),
                        color = Color.LightGray
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenArCamera,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFCC00),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "AR Track",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AR TRACK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                IconButton(
                    onClick = onClearSelection,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Unlock Target",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendIndicator(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            ),
            color = Color.Gray
        )
    }
}
