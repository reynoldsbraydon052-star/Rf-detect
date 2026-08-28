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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

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
    var chartMode by remember { mutableStateOf("FLUCTUATION") } // "FLUCTUATION", "MULTI_CHANNEL", "WATERFALL", "SPECTRUM"
    var selectedFilter by remember { mutableStateOf("ALL") }
    var isScrubberActive by remember { mutableStateOf(false) }
    var scrubberXRatio by remember { mutableStateOf(0.85f) }

    // Dedicated decoupled telemetry engine with circular ring buffers
    val engine = remember { RfFluctuationEngine() }
    val viewState by engine.viewState.collectAsState()

    DisposableEffect(engine) {
        onDispose {
            engine.destroy()
        }
    }

    LaunchedEffect(selectedFilter) {
        engine.setFilter(selectedFilter)
    }

    LaunchedEffect(selectedTargetDeviceId) {
        engine.setSelectedTarget(selectedTargetDeviceId)
    }

    LaunchedEffect(activeBlips, selectedTargetDeviceId) {
        engine.ingestBlips(activeBlips, selectedTargetDeviceId)
    }

    val filteredBlips = remember(activeBlips, selectedFilter) {
        if (selectedFilter == "ALL") activeBlips
        else activeBlips.filter { it.type.equals(selectedFilter, ignoreCase = true) }
    }

    val selectedBlip = remember(activeBlips, selectedTargetDeviceId) {
        activeBlips.firstOrNull { it.id == selectedTargetDeviceId || it.name == selectedTargetDeviceId }
    }

    val primaryBlip = selectedBlip ?: filteredBlips.minByOrNull { it.distance }
    val metrics = viewState.primaryMetrics
    val yMin = viewState.yAxisBounds.minY
    val yMax = viewState.yAxisBounds.maxY

    // Compatibility history list for target banner
    val primaryHistory = remember(viewState.activeSamplesToDraw) {
        viewState.activeSamplesToDraw.map { SignalTimePoint(it.timestampMs, it.rssiDbm.toInt(), 1.0f) }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_signal_strength_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Bar with Title and Mode Selector Tabs
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
                            .background(Color(0xFF00FF66).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Signal Fluctuation Chart",
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "RF SIGNAL FLUCTUATIONS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp,
                                fontSize = 13.sp
                            ),
                            color = Color(0xFF00FF66)
                        )
                        Text(
                            text = "Live RSSI Drift, Jitter & Spectrum Density",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp
                            ),
                            color = Color.Gray
                        )
                    }
                }

                // Segmented Mode Selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "FLUCTUATION" to "WAVE",
                        "MULTI_CHANNEL" to "MULTI",
                        "WATERFALL" to "WATERFALL",
                        "SPECTRUM" to "FFT"
                    ).forEach { (modeKey, label) ->
                        val isSel = chartMode == modeKey
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Color(0xFF00FF66) else Color(0xFF0E2417),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFF00FF66) else Color(0xFF00FF66).copy(alpha = 0.25f)),
                            modifier = Modifier
                                .clickable { chartMode = modeKey }
                                .testTag("chart_mode_$modeKey")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (isSel) Color.Black else Color(0xFF00FF66),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Live Telemetry Metrics Strip (Calculated Real-Time Values)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF06150D), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryMetricItem("CURRENT", if (metrics.sampleCount > 0) "${metrics.currentRssi.toInt()} dBm" else "-- dBm", Color(0xFF00FF66))
                TelemetryMetricItem("PEAK", if (metrics.sampleCount > 0) "${metrics.peakRssi.toInt()} dBm" else "-- dBm", Color(0xFFFFCC00))
                TelemetryMetricItem("AVG", if (metrics.sampleCount > 0) "%.0f dBm".format(metrics.avgRssi) else "-- dBm", Color(0xFF00E5FF))
                TelemetryMetricItem("JITTER", "±%.1f dBm".format(metrics.jitterDbm), if (metrics.jitterDbm > 3.0) Color(0xFFFF3366) else Color(0xFF00FF66))
                TelemetryMetricItem("STABILITY", "%.0f%%".format(metrics.stabilityPercent), if (metrics.stabilityPercent < 50.0) Color(0xFFFF3366) else Color.White)
            }

            // Filter Chips Bar
            FilterChipsRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            // Selected Target Callout Banner if locked
            if (selectedBlip != null) {
                SelectedTargetPinpointBanner(
                    selectedBlip = selectedBlip,
                    history = primaryHistory,
                    driftVelocity = metrics.driftVelocityDbmPerSec.toFloat(),
                    onClearSelection = { onSelectTargetDevice(null) },
                    onOpenArCamera = { onOpenArCameraForTarget?.invoke(selectedBlip.id) }
                )
            }

            // Canvas Chart Area with Dynamic Animated Glow, Curves and Dynamic Y-Axis
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseGlowRadius by infiniteTransition.animateFloat(
                initialValue = 8f,
                targetValue = 22f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseGlowRadius"
            )

            val sweepPhase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "sweepPhase"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF020905))
                    .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
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
                                val paddingTop = 26f
                                val paddingBottom = 26f
                                val graphW = w - paddingLeft - paddingRight
                                val graphH = h - paddingTop - paddingBottom

                                var closestBlipId: String? = null
                                var minDistance = Float.MAX_VALUE

                                filteredBlips.forEach { blip ->
                                    val samples = viewState.channelSnapshots[blip.type.uppercase()] ?: emptyList()
                                    if (samples.isNotEmpty()) {
                                        val lastPt = samples.last()
                                        val headX = paddingLeft + graphW
                                        val normY = 1.0f - ((lastPt.rssiDbm.toFloat() - yMin) / (yMax - yMin)).coerceIn(0f, 1f)
                                        val headY = paddingTop + (normY * graphH)
                                        val dist = kotlin.math.hypot((headX - tapOffset.x).toDouble(), (headY - tapOffset.y).toDouble()).toFloat()
                                        if (dist < minDistance) {
                                            minDistance = dist
                                            closestBlipId = blip.id
                                        }
                                    }
                                }

                                if (closestBlipId != null && minDistance <= 65f) {
                                    onSelectTargetDevice(if (closestBlipId == selectedTargetDeviceId) null else closestBlipId)
                                } else {
                                    isScrubberActive = true
                                    scrubberXRatio = ((tapOffset.x - paddingLeft) / graphW).coerceIn(0.02f, 0.98f)
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                isScrubberActive = true
                                val paddingLeft = 45f
                                val paddingRight = 15f
                                val graphW = size.width - paddingLeft - paddingRight
                                scrubberXRatio = ((change.position.x - paddingLeft) / graphW).coerceIn(0.02f, 0.98f)
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    val paddingTop = 26f
                    val paddingBottom = 26f
                    val paddingLeft = 48f
                    val paddingRight = 15f

                    val graphW = w - paddingLeft - paddingRight
                    val graphH = h - paddingTop - paddingBottom

                    // Draw Horizontal Grid Lines with Dynamic Y-Axis Scaling
                    val span = yMax - yMin
                    val stepCount = 4
                    val step = span / stepCount

                    for (i in 0..stepCount) {
                        val level = yMin + (i * step)
                        val normY = 1.0f - ((level - yMin) / span).coerceIn(0f, 1f)
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
                            textSize = 19f
                            typeface = android.graphics.Typeface.MONOSPACE
                            isAntiAlias = true
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            "${level.toInt()}dBm",
                            6f,
                            y + 6f,
                            textPaint
                        )
                    }

                    // Vertical Time Grid Lines (-60s, -45s, -30s, -15s, NOW)
                    val timeTicks = listOf("-60s", "-45s", "-30s", "-15s", "NOW")
                    timeTicks.forEachIndexed { idx, label ->
                        val xRatio = idx / (timeTicks.size - 1).toFloat()
                        val x = paddingLeft + (xRatio * graphW)

                        drawLine(
                            color = Color(0xFF00FF66).copy(alpha = 0.12f),
                            start = Offset(x, paddingTop),
                            end = Offset(x, h - paddingBottom),
                            strokeWidth = 1f
                        )

                        val textPaint = android.graphics.Paint().apply {
                            color = if (label == "NOW") android.graphics.Color.GREEN else android.graphics.Color.DKGRAY
                            textSize = 18f
                            typeface = android.graphics.Typeface.MONOSPACE
                            isAntiAlias = true
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            x - 14f,
                            h - 8f,
                            textPaint
                        )
                    }

                    when (chartMode) {
                        "FLUCTUATION" -> {
                            // High-Fidelity Smooth Cubic Spline Wave with Dynamic Gradient Glow Fill
                            val samples = viewState.activeSamplesToDraw
                            if (samples.size >= 2) {
                                val recentSamples = samples.takeLast(60)
                                val points = recentSamples.mapIndexed { i, pt ->
                                    val xRatio = i / (recentSamples.size - 1).toFloat()
                                    val x = paddingLeft + (xRatio * graphW)
                                    val normY = 1.0f - ((pt.rssiDbm.toFloat() - yMin) / (yMax - yMin)).coerceIn(0f, 1f)
                                    val y = paddingTop + (normY * graphH)
                                    Offset(x, y)
                                }

                                // Construct Smooth Cubic Bezier Path
                                val splinePath = Path()
                                splinePath.moveTo(points.first().x, points.first().y)
                                for (i in 0 until points.size - 1) {
                                    val p0 = points[i]
                                    val p1 = points[i + 1]
                                    val cx = (p0.x + p1.x) / 2f
                                    splinePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                                }

                                // Fill Area Underneath with Neon Gradient Glow
                                val fillPath = Path()
                                fillPath.addPath(splinePath)
                                fillPath.lineTo(points.last().x, h - paddingBottom)
                                fillPath.lineTo(points.first().x, h - paddingBottom)
                                fillPath.close()

                                val waveColor = when (primaryBlip?.type) {
                                    "WIFI" -> Color(0xFF00FF66)
                                    "BLE" -> Color(0xFF00E5FF)
                                    "CELLULAR" -> Color(0xFFFF3366)
                                    "MAGNETIC" -> Color(0xFFFFCC00)
                                    else -> Color(0xFF00FF66)
                                }

                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            waveColor.copy(alpha = 0.35f),
                                            waveColor.copy(alpha = 0.08f),
                                            Color.Transparent
                                        ),
                                        startY = paddingTop,
                                        endY = h - paddingBottom
                                    ),
                                    style = Fill
                                )

                                // Outer Neon Halo
                                drawPath(
                                    path = splinePath,
                                    color = waveColor.copy(alpha = 0.3f),
                                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                                )

                                // Primary Sharp Trace
                                drawPath(
                                    path = splinePath,
                                    color = waveColor,
                                    style = Stroke(width = 2.8f, cap = StrokeCap.Round)
                                )

                                // Peak-Hold Line (Dashed)
                                val maxPt = recentSamples.maxByOrNull { it.rssiDbm }
                                if (maxPt != null) {
                                    val peakNormY = 1.0f - ((maxPt.rssiDbm.toFloat() - yMin) / (yMax - yMin)).coerceIn(0f, 1f)
                                    val peakY = paddingTop + (peakNormY * graphH)
                                    drawLine(
                                        color = Color(0xFFFFCC00).copy(alpha = 0.6f),
                                        start = Offset(paddingLeft, peakY),
                                        end = Offset(w - paddingRight, peakY),
                                        strokeWidth = 1.2f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                                    )

                                    val textPaint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.YELLOW
                                        textSize = 18f
                                        typeface = android.graphics.Typeface.MONOSPACE
                                        isAntiAlias = true
                                    }
                                    drawContext.canvas.nativeCanvas.drawText(
                                        "PEAK: ${maxPt.rssiDbm.toInt()}dBm",
                                        w - paddingRight - 115f,
                                        peakY - 4f,
                                        textPaint
                                    )
                                }

                                // Current Head Point
                                val headPt = points.last()
                                drawCircle(color = waveColor, radius = 6f, center = headPt)
                                drawCircle(
                                    color = waveColor.copy(alpha = 0.5f),
                                    radius = pulseGlowRadius,
                                    center = headPt,
                                    style = Stroke(width = 2f)
                                )
                            }
                        }

                        "MULTI_CHANNEL" -> {
                            // Plot Multi-Channel Simultaneous Overlay Traces for all active protocols
                            val protocols = listOf("WIFI", "BLE", "CELLULAR", "MAGNETIC")
                            protocols.forEach { proto ->
                                val channelSamples = viewState.channelSnapshots[proto] ?: emptyList()
                                val signalColor = when (proto) {
                                    "WIFI" -> Color(0xFF00FF66)
                                    "BLE" -> Color(0xFF00E5FF)
                                    "CELLULAR" -> Color(0xFFFF3366)
                                    "MAGNETIC" -> Color(0xFFFFCC00)
                                    else -> Color(0xFFFF9900)
                                }

                                if (channelSamples.size >= 2) {
                                    val samples = channelSamples.takeLast(40)
                                    val path = Path()
                                    samples.forEachIndexed { i, pt ->
                                        val xRatio = i / (samples.size - 1).toFloat()
                                        val x = paddingLeft + (xRatio * graphW)
                                        val normY = 1.0f - ((pt.rssiDbm.toFloat() - yMin) / (yMax - yMin)).coerceIn(0f, 1f)
                                        val y = paddingTop + (normY * graphH)

                                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                    }

                                    drawPath(
                                        path = path,
                                        color = signalColor.copy(alpha = 0.85f),
                                        style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                                    )

                                    val lastPt = samples.last()
                                    val headX = paddingLeft + graphW
                                    val normY = 1.0f - ((lastPt.rssiDbm.toFloat() - yMin) / (yMax - yMin)).coerceIn(0f, 1f)
                                    val headY = paddingTop + (normY * graphH)

                                    drawCircle(
                                        color = signalColor,
                                        radius = 4f,
                                        center = Offset(headX, headY)
                                    )
                                }
                            }
                        }

                        "WATERFALL" -> {
                            // Rolling Spectrogram Waterfall Heatmap
                            val numSlices = 20
                            val sliceH = graphH / numSlices
                            for (s in 0 until numSlices) {
                                val yTop = paddingTop + (s * sliceH)
                                val timeRatio = s / numSlices.toFloat()

                                val numFreqBins = 30
                                val binW = graphW / numFreqBins
                                for (b in 0 until numFreqBins) {
                                    val xLeft = paddingLeft + (b * binW)
                                    val freqRatio = b / numFreqBins.toFloat()

                                    val angle = (freqRatio * 6.28 + timeRatio * 4.0 + sweepPhase * 6.28).toDouble()
                                    val syntheticEnergy = (abs(sin(angle)).toFloat() * 0.7f).coerceIn(0f, 1f)
                                    val heatColor = when {
                                        syntheticEnergy > 0.8f -> Color(0xFFFF3366).copy(alpha = 0.8f)
                                        syntheticEnergy > 0.55f -> Color(0xFFFFCC00).copy(alpha = 0.65f)
                                        syntheticEnergy > 0.3f -> Color(0xFF00FF66).copy(alpha = 0.45f)
                                        else -> Color(0xFF00E5FF).copy(alpha = 0.2f)
                                    }

                                    drawRect(
                                        color = heatColor,
                                        topLeft = Offset(xLeft, yTop),
                                        size = Size(binW - 1f, sliceH - 1f)
                                    )
                                }
                            }
                        }

                        "SPECTRUM" -> {
                            // RF FFT Power Density Curve across 2.4GHz, 5GHz, 6GHz Bands
                            val spectrumPath = Path()
                            val numBins = 70
                            for (bin in 0..numBins) {
                                val ratio = bin / numBins.toFloat()
                                val freqMhz = 10.0 + (ratio * 6000.0)
                                val x = paddingLeft + (ratio * graphW)

                                var totalPower = -95.0
                                filteredBlips.forEach { blip ->
                                    val freqDiff = abs(blip.frequencyMhz - freqMhz)
                                    if (freqDiff < 350.0) {
                                        val contrib = blip.rssi + (350.0 - freqDiff) / 350.0 * 25.0
                                        totalPower = max(totalPower, contrib)
                                    }
                                }

                                val normY = 1.0f - ((totalPower.toFloat() - yMin) / (yMax - yMin)).coerceIn(0f, 1f)
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
                                        Color(0xFF00FF66).copy(alpha = 0.5f),
                                        Color(0xFF00FF66).copy(alpha = 0.08f),
                                        Color.Transparent
                                    ),
                                    startY = paddingTop,
                                    endY = h - paddingBottom
                                ),
                                style = Fill
                            )

                            drawPath(
                                path = spectrumPath,
                                color = Color(0xFF00FF66),
                                style = Stroke(width = 2.2f)
                            )
                        }
                    }

                    // Interactive Scrubber Cursor Line
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

            // Compact Spectrum Density Breakdown Sub-Graph
            SpectrumDensityHistogramStrip(
                densityMap = viewState.spectrumDensity,
                modifier = Modifier.fillMaxWidth()
            )

            // Bottom Legend & Quick Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendIndicator("WiFi", Color(0xFF00FF66))
                    LegendIndicator("BLE", Color(0xFF00E5FF))
                    LegendIndicator("Cellular", Color(0xFFFF3366))
                    LegendIndicator("Magnetic", Color(0xFFFFCC00))
                    if (selectedTargetDeviceId != null) {
                        LegendIndicator("Locked", Color(0xFFFFCC00))
                    }
                }

                Text(
                    text = "${filteredBlips.size} SIGNALS TRACKED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    ),
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Compact Spectrum Density Histogram Strip showing relative channel occupancy and activity levels.
 */
@Composable
fun SpectrumDensityHistogramStrip(
    densityMap: Map<String, Float>,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF06150D),
        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.2f)),
        modifier = modifier.testTag("spectrum_density_histogram_strip")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPECTRUM DENSITY BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF00FF66)
                )
                Text(
                    text = "RELATIVE OCCUPANCY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 7.5.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.Gray
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Triple("WIFI", Color(0xFF00FF66), densityMap["WIFI"] ?: 0f),
                    Triple("BLE", Color(0xFF00E5FF), densityMap["BLE"] ?: 0f),
                    Triple("CELL", Color(0xFFFF3366), densityMap["CELLULAR"] ?: 0f),
                    Triple("MAG", Color(0xFFFFCC00), densityMap["MAGNETIC"] ?: 0f)
                ).forEach { (label, color, ratio) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = color
                            )
                            Text(
                                text = "%.0f%%".format(ratio * 100f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                        }

                        // Activity gauge bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF0E2417))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(ratio.coerceIn(0.04f, 1f))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryMetricItem(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            ),
            color = valueColor
        )
    }
}

@Composable
private fun SelectedTargetPinpointBanner(
    selectedBlip: RadarBlip,
    history: List<SignalTimePoint>,
    driftVelocity: Float,
    onClearSelection: () -> Unit,
    onOpenArCamera: () -> Unit
) {
    val currentRssi = selectedBlip.rssi
    val trendLabel = when {
        driftVelocity > 0.4f -> "APPROACHING (+%.1f dB/s)".format(driftVelocity)
        driftVelocity < -0.4f -> "RECEDING (%.1f dB/s)".format(driftVelocity)
        else -> "STABLE SIGNAL (%.1f dB/s)".format(driftVelocity)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFFFCC00).copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color(0xFFFFCC00).copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
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
                        .background(Color(0xFFFFCC00).copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "Target Lock",
                        tint = Color(0xFFFFCC00),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = selectedBlip.name.take(16),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
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
                            fontSize = 8.5.sp
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
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "AR Track",
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "AR TRACK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                IconButton(
                    onClick = onClearSelection,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Unlock Target",
                        tint = Color.Gray,
                        modifier = Modifier.size(15.dp)
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
                .size(7.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace
            ),
            color = Color.Gray
        )
    }
}

