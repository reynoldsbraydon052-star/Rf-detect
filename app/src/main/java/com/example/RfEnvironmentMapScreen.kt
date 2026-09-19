package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.max

@Composable
fun RfEnvironmentMapScreen(
    uiState: SignalRadarUiState,
    mappingEngine: RfEnvironmentMappingEngine
) {
    val mapState by mappingEngine.mapState.collectAsStateWithLifecycle()
    
    var scale by remember { mutableStateOf(1.2f) }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    
    var viewMode by remember { mutableStateOf(MapViewMode.RF_DENSITY) }
    
    // Additional Tactical Overlay Layer States
    var isHeatmapOverlayEnabled by remember { mutableStateOf(true) }
    var is25DEnabled by remember { mutableStateOf(true) }

    // Keep pulse animations updating in real-time
    var frameTicker by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(50L)
            frameTicker = System.currentTimeMillis()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // HUD Control Header Area
        Surface(
            color = Color(0xFF040B06),
            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TACTICAL 2.5D SPATIAL MAP",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        ),
                        color = Color(0xFF00FF66)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Active Emitters: ${uiState.activeBlips.size} | Track Nodes: ${mapState.userPath.size}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.Gray
                    )
                }
                
                // Reset/Clear Trail
                Button(
                    onClick = { mappingEngine.clearMap() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3366).copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, Color(0xFFFF3366)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Reset map data",
                        tint = Color(0xFFFF3366),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RESET DATA",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color(0xFFFF3366)
                    )
                }
            }
        }
        
        // Horizontal Mode Switcher
        ScrollableTabRow(
            selectedTabIndex = viewMode.ordinal,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth().height(42.dp),
            containerColor = Color(0xFF020603),
            contentColor = Color(0xFF00FF66)
        ) {
            MapViewMode.entries.forEachIndexed { index, mode ->
                Tab(
                    selected = viewMode.ordinal == index,
                    onClick = { viewMode = mode },
                    text = { 
                        Text(
                            text = mode.title.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                )
            }
        }
        
        // Dynamic Legend Panel
        MapLegend(viewMode)

        // Upgraded Map Canvas Viewport Box Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF010402))
                .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.25f))
        ) {
            // MAIN 2.5D GEOMETRY CANVAS
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.4f, 8f)
                            panX += pan.x
                            panY += pan.y
                        }
                    }
            ) {
                val centerOffset = Offset(size.width / 2f + panX, size.height / 2f + panY)
                val pixelPerMeter = 20f * scale
                
                // ==========================================
                // 1. ADVANCED METRIC COORDINATE GRID SYSTEM
                // ==========================================
                // Calculate grid ranges in world meters relative to center offset
                val minX_m = (((0f - centerOffset.x) / pixelPerMeter).toInt() - 2)
                val maxX_m = (((size.width - centerOffset.x) / pixelPerMeter).toInt() + 2)
                val minY_m = (((0f - centerOffset.y) / pixelPerMeter).toInt() - 2)
                val maxY_m = (((size.height - centerOffset.y) / pixelPerMeter).toInt() + 2)

                val gridLabelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(120, 0, 255, 102) // Green phosphor
                    textSize = 10.dp.toPx()
                    typeface = android.graphics.Typeface.MONOSPACE
                    isAntiAlias = true
                }

                // Draw vertical grid lines and dynamic labels
                for (x in minX_m..maxX_m) {
                    val isMajor10 = x % 10 == 0
                    val isMajor5 = x % 5 == 0
                    
                    val alpha = if (isMajor10) {
                        0.32f
                    } else if (isMajor5) {
                        0.15f
                    } else {
                        if (scale < 1.3f) continue
                        0.05f * (scale / 2f).coerceAtMost(1f)
                    }
                    
                    val screenX = centerOffset.x + x * pixelPerMeter
                    drawLine(
                        color = Color(0xFF00FF66).copy(alpha = alpha),
                        start = Offset(screenX, 0f),
                        end = Offset(screenX, size.height),
                        strokeWidth = if (isMajor10) 1.5f else 1f
                    )
                    
                    // Dynamic metric distance indicator labels
                    if (x != 0 && (isMajor10 || (isMajor5 && scale >= 1.3f))) {
                        val label = if (x > 0) "+${x}m" else "${x}m"
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            screenX + 4f,
                            centerOffset.y - 6f,
                            gridLabelPaint
                        )
                    }
                }

                // Draw horizontal grid lines and dynamic labels
                for (y in minY_m..maxY_m) {
                    val isMajor10 = y % 10 == 0
                    val isMajor5 = y % 5 == 0
                    
                    val alpha = if (isMajor10) {
                        0.32f
                    } else if (isMajor5) {
                        0.15f
                    } else {
                        if (scale < 1.3f) continue
                        0.05f * (scale / 2f).coerceAtMost(1f)
                    }
                    
                    val screenY = centerOffset.y + y * pixelPerMeter
                    drawLine(
                        color = Color(0xFF00FF66).copy(alpha = alpha),
                        start = Offset(0f, screenY),
                        end = Offset(size.width, screenY),
                        strokeWidth = if (isMajor10) 1.5f else 1f
                    )
                    
                    // Dynamic metric distance indicator labels along Y axis
                    if (y != 0 && (isMajor10 || (isMajor5 && scale >= 1.3f))) {
                        val label = if (-y > 0) "+${-y}m" else "${-y}m"
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            centerOffset.x + 6f,
                            screenY - 4f,
                            gridLabelPaint
                        )
                    }
                }

                // Core Reference Crosshairs (0,0)
                drawLine(
                    color = Color(0xFF00FF66).copy(alpha = 0.55f),
                    start = Offset(centerOffset.x, 0f),
                    end = Offset(centerOffset.x, size.height),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color(0xFF00FF66).copy(alpha = 0.55f),
                    start = Offset(0f, centerOffset.y),
                    end = Offset(size.width, centerOffset.y),
                    strokeWidth = 2f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "ORIGIN [0,0]",
                    centerOffset.x + 6f,
                    centerOffset.y - 6f,
                    gridLabelPaint
                )

                // ==========================================
                // 2. SPATIAL GEOMETRY MAP CELL OVERLAYS
                // ==========================================
                val maxObs = max(1, mapState.cells.values.maxOfOrNull { it.observationCount } ?: 1)
                
                mapState.cells.values.forEach { cell ->
                    val cellX = centerOffset.x + (cell.centerX * pixelPerMeter)
                    val cellY = centerOffset.y + (cell.centerY * pixelPerMeter)
                    val cellSizePx = mapState.cellSizeMeters * pixelPerMeter
                    
                    val cellColor = when (viewMode) {
                        MapViewMode.RF_DENSITY -> {
                            val intensity = (cell.observationCount.toFloat() / maxObs).coerceIn(0f, 1f)
                            Color(0f, 1f, 0.4f, intensity * 0.45f)
                        }
                        MapViewMode.SIGNAL_STRENGTH -> {
                            val intensity = ((cell.averageRssi + 100f) / 60f).coerceIn(0f, 1f)
                            Color(intensity, 1f - intensity, 0f, 0.4f)
                        }
                        MapViewMode.NOISE_FLOOR -> {
                            val intensity = ((cell.estimatedNoiseFloor + 110f) / 30f).coerceIn(0f, 1f)
                            Color(0f, intensity, 1f, 0.35f)
                        }
                        MapViewMode.DEVICES -> {
                            val intensity = (cell.uniqueDevices.size.toFloat() / 10f).coerceIn(0f, 1f)
                            Color(1f, 0f, 1f, intensity * 0.4f)
                        }
                        MapViewMode.ANOMALIES -> {
                            val intensity = (cell.anomalyCount.toFloat() / 5f).coerceIn(0f, 1f)
                            if (intensity > 0) Color(1f, 0.2f, 0.2f, intensity * 0.55f) else Color.Transparent
                        }
                    }
                    
                    if (cellColor != Color.Transparent) {
                        drawRect(
                            color = cellColor,
                            topLeft = Offset(cellX - (cellSizePx / 2f), cellY - (cellSizePx / 2f)),
                            size = Size(cellSizePx, cellSizePx)
                        )
                    }
                }

                // ==========================================
                // 3. OPERATOR BREADCRUMB TRAILS & HEADING ARROWS
                // ==========================================
                if (mapState.userPath.size > 1) {
                    val trailPath = Path()
                    mapState.userPath.forEachIndexed { index, point ->
                        val px = centerOffset.x + (point.first * pixelPerMeter)
                        val py = centerOffset.y + (point.second * pixelPerMeter)
                        if (index == 0) trailPath.moveTo(px, py) else trailPath.lineTo(px, py)
                    }
                    drawPath(
                        path = trailPath,
                        color = Color(0xFF00FF66).copy(alpha = 0.7f),
                        style = Stroke(
                            width = 2.5f * scale,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                        )
                    )

                    // Draw traversal heading arrows along each line segment
                    for (i in 0 until mapState.userPath.size - 1) {
                        val p1 = mapState.userPath[i]
                        val p2 = mapState.userPath[i + 1]
                        val p1x = centerOffset.x + (p1.first * pixelPerMeter)
                        val p1y = centerOffset.y + (p1.second * pixelPerMeter)
                        val p2x = centerOffset.x + (p2.first * pixelPerMeter)
                        val p2y = centerOffset.y + (p2.second * pixelPerMeter)

                        val midX = (p1x + p2x) / 2f
                        val midY = (p1y + p2y) / 2f
                        val dx = p2x - p1x
                        val dy = p2y - p1y
                        val dist = kotlin.math.hypot(dx, dy)
                        if (dist > 15f) {
                            val angleRad = kotlin.math.atan2(dy, dx)
                            val arrSize = 6f * scale
                            val arrPath = Path().apply {
                                moveTo(midX, midY)
                                lineTo(
                                    midX - arrSize * kotlin.math.cos(angleRad - Math.PI / 6).toFloat(),
                                    midY - arrSize * kotlin.math.sin(angleRad - Math.PI / 6).toFloat()
                                )
                                lineTo(
                                    midX - arrSize * kotlin.math.cos(angleRad + Math.PI / 6).toFloat(),
                                    midY - arrSize * kotlin.math.sin(angleRad + Math.PI / 6).toFloat()
                                )
                                close()
                            }
                            drawPath(arrPath, Color(0xFF00FF66).copy(alpha = 0.85f))
                        }
                    }
                }

                // Active User Position Indicator
                val userPx = centerOffset.x + (mapState.currentUserX * pixelPerMeter)
                val userPy = centerOffset.y + (mapState.currentUserY * pixelPerMeter)
                
                // Draw rotating compass crosshair around user position
                val userAngleRad = Math.toRadians(uiState.headingDegrees.toDouble())
                val beamLength = 16f * scale
                drawLine(
                    color = Color(0xFF00FF66),
                    start = Offset(userPx, userPy),
                    end = Offset(
                        userPx + (beamLength * kotlin.math.sin(userAngleRad)).toFloat(),
                        userPy - (beamLength * kotlin.math.cos(userAngleRad)).toFloat()
                    ),
                    strokeWidth = 2.5f * scale
                )
                drawCircle(
                    color = Color(0xFF00FF66).copy(alpha = 0.2f),
                    radius = 12f * scale,
                    center = Offset(userPx, userPy)
                )
                drawCircle(
                    color = Color(0xFF00FF66),
                    radius = 5f * scale,
                    center = Offset(userPx, userPy)
                )
                
                // ==========================================
                // 4. ACTIVE EMITTERS & 2.5D ALTITUDE PROJECTIONS
                // ==========================================
                uiState.activeBlips.forEach { blip ->
                    val absoluteAngleDeg = (uiState.headingDegrees + blip.targetAngleOffset) % 360f
                    val absoluteAngleRad = Math.toRadians(absoluteAngleDeg.toDouble())
                    
                    // Approximate spatial location in Cartesian coordinates
                    val targetX = mapState.currentUserX + (blip.distance * kotlin.math.sin(absoluteAngleRad)).toFloat()
                    val targetY = mapState.currentUserY - (blip.distance * kotlin.math.cos(absoluteAngleRad)).toFloat()
                    
                    val txPx = centerOffset.x + targetX * pixelPerMeter
                    val tyPx = centerOffset.y + targetY * pixelPerMeter

                    val emitterColor = when (blip.type) {
                        "WIFI" -> Color(0xFF00FF66) // Neon AP
                        "BLE" -> Color(0xFF00E5FF)  // Cyan Diamond BLE
                        else -> Color(0xFFFF9900)   // Amber Other
                    }

                    // 4A. Heatmap Overlap Density Render (with BlendMode.Plus)
                    if (isHeatmapOverlayEnabled) {
                        val rssiClamped = blip.rssi.coerceIn(-95, -40)
                        val densityIntensity = (rssiClamped + 95) / 55f
                        val heatRadiusPx = (6f + densityIntensity * 12f) * pixelPerMeter
                        
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(emitterColor.copy(alpha = densityIntensity * 0.45f), Color.Transparent),
                                center = Offset(txPx, tyPx),
                                radius = heatRadiusPx
                            ),
                            center = Offset(txPx, tyPx),
                            radius = heatRadiusPx,
                            blendMode = BlendMode.Plus
                        )
                    }

                    // 4B. 2.5D Perspective Stem / Projection Math
                    val stemHeightPx = if (is25DEnabled) {
                        if (blip.estimatedZOffsetMeters != 0f) {
                            blip.estimatedZOffsetMeters * 3.5f * pixelPerMeter
                        } else {
                            // Compute signal level (strength mapping) as vertical height
                            val strength = (blip.rssi + 95f).coerceAtLeast(0f)
                            (strength * 0.12f) * pixelPerMeter
                        }
                    } else {
                        0f
                    }

                    val groundPx = Offset(txPx, tyPx)
                    val skyPx = Offset(txPx, tyPx - stemHeightPx)

                    // Draw Projection Footprint on ground
                    drawCircle(
                        color = emitterColor.copy(alpha = 0.35f),
                        radius = 4.5f * scale,
                        center = groundPx,
                        style = Stroke(width = 1f)
                    )

                    if (is25DEnabled && stemHeightPx > 0f) {
                        // Draw Vertical Stem
                        drawLine(
                            color = emitterColor.copy(alpha = 0.5f),
                            start = groundPx,
                            end = skyPx,
                            strokeWidth = 1.5f * scale,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    // 4C. Real-time Uncertainty Variance Rings (Pulsating)
                    val sensorAccuracy = if (blip.csEstimatedAccuracyMeters > 0f) blip.csEstimatedAccuracyMeters else 3.5f
                    val timePulse = (frameTicker % 1500) / 1500f
                    val varianceRadiusPx = (sensorAccuracy * (1f + timePulse * 0.35f)) * pixelPerMeter
                    val ringAlpha = (1f - timePulse) * 0.22f
                    
                    drawCircle(
                        color = emitterColor.copy(alpha = ringAlpha),
                        radius = varianceRadiusPx,
                        center = groundPx,
                        style = Stroke(width = 1.5f * scale)
                    )

                    // 4D. Drawing Emitter Icon Shape (Hexagon vs Diamond)
                    val markerCenter = if (is25DEnabled) skyPx else groundPx
                    val markerSize = 8f * scale

                    if (blip.type == "WIFI") {
                        // Draw Hexagon for Access Points
                        val hexagonPath = Path().apply {
                            for (step in 0 until 6) {
                                val deg = Math.toRadians((step * 60).toDouble())
                                val hx = markerCenter.x + markerSize * kotlin.math.sin(deg).toFloat()
                                val hy = markerCenter.y - markerSize * kotlin.math.cos(deg).toFloat()
                                if (step == 0) moveTo(hx, hy) else lineTo(hx, hy)
                            }
                            close()
                        }
                        drawPath(path = hexagonPath, color = emitterColor)
                        drawPath(path = hexagonPath, color = Color.White, style = Stroke(width = 1f * scale))
                    } else {
                        // Draw Diamond for BLE tracking nodes
                        val diamondPath = Path().apply {
                            moveTo(markerCenter.x, markerCenter.y - markerSize)
                            lineTo(markerCenter.x + markerSize, markerCenter.y)
                            lineTo(markerCenter.x, markerCenter.y + markerSize)
                            lineTo(markerCenter.x - markerSize, markerCenter.y)
                            close()
                        }
                        drawPath(path = diamondPath, color = emitterColor)
                        drawPath(path = diamondPath, color = Color.White, style = Stroke(width = 1f * scale))
                    }

                    // 4E. Draw Text Metadata next to Emitter blip
                    val emitterPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 9.dp.toPx()
                        typeface = android.graphics.Typeface.MONOSPACE
                        isAntiAlias = true
                    }
                    val heightText = if (blip.estimatedZOffsetMeters != 0f) {
                        " H:${String.format("%.1f", blip.estimatedZOffsetMeters)}m"
                    } else {
                        ""
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "${blip.name} [${blip.rssi}dBm]$heightText",
                        markerCenter.x + markerSize + 4f,
                        markerCenter.y + 3f,
                        emitterPaint
                    )
                }
            }

            // ==========================================
            // 5. LAYER / GESTURES CONTROL BUTTON PANELS
            // ==========================================
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // TOGGLE HEAT DENSITY BUTTON
                FloatingActionButton(
                    onClick = { isHeatmapOverlayEnabled = !isHeatmapOverlayEnabled },
                    containerColor = if (isHeatmapOverlayEnabled) Color(0xFF00FF66) else Color(0xFF06150C),
                    contentColor = if (isHeatmapOverlayEnabled) Color.Black else Color(0xFF00FF66),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(42.dp),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Toggle Heat Density View",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // TOGGLE 2.5D STEM TRANSITIONS
                FloatingActionButton(
                    onClick = { is25DEnabled = !is25DEnabled },
                    containerColor = if (is25DEnabled) Color(0xFF00FF66) else Color(0xFF06150C),
                    contentColor = if (is25DEnabled) Color.Black else Color(0xFF00FF66),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(42.dp),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "Toggle 2.5D Perspective Projection",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // RE-CENTER ON OPERATOR POSITION
                FloatingActionButton(
                    onClick = {
                        scale = 1.2f
                        panX = -mapState.currentUserX * 24f
                        panY = -mapState.currentUserY * 24f
                    },
                    containerColor = Color(0xFF06150C),
                    contentColor = Color(0xFF00FF66),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .size(42.dp)
                        .border(1.dp, Color(0xFF00FF66), RoundedCornerShape(8.dp)),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Re-center on operator",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // TOP-LEFT FLOATING STATUS FEEDOVERLAY
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "HUD MAP STATS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = Color(0xFF00FF66)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ZOOM LEVEL : ${String.format("%.1f", scale)}x\nGRID MATRIX: 1m/5m/10m\nHEAT OVERLAY: ${if (isHeatmapOverlayEnabled) "ACTIVE" else "OFF"}\nPROJECTION : ${if (is25DEnabled) "2.5D STEM" else "2D PLANE"}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        color = Color.LightGray,
                        lineHeight = 11.sp
                    )
                }
            }
        }
    }
}

enum class MapViewMode(val title: String, val desc: String) {
    RF_DENSITY("Density", "High density (Green) vs Low (Transparent)"),
    SIGNAL_STRENGTH("Strength", "Strong (-40dBm Red) to Weak (-100dBm Green)"),
    NOISE_FLOOR("Noise", "High Noise Floor (Cyan)"),
    DEVICES("Devices", "Many devices (Magenta)"),
    ANOMALIES("Anomalies", "High anomaly concentration (Red)")
}

@Composable
fun MapLegend(mode: MapViewMode) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Legend: ${mode.desc}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            ),
            color = Color.Gray
        )
    }
}
