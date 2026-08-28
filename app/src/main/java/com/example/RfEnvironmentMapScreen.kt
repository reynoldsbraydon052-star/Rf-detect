package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    
    var scale by remember { mutableStateOf(1f) }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    
    var viewMode by remember { mutableStateOf(MapViewMode.RF_DENSITY) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RF Environment Map",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Estimated Spatial Aggregation - Not a precise physical location map.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Button(
                onClick = { mappingEngine.clearMap() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Map", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset")
            }
        }
        
        // Mode Selector
        ScrollableTabRow(
            selectedTabIndex = viewMode.ordinal,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            MapViewMode.values().forEachIndexed { index, mode ->
                Tab(
                    selected = viewMode.ordinal == index,
                    onClick = { viewMode = mode },
                    text = { 
                        Text(
                            text = mode.title,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                )
            }
        }
        
        // Legend
        MapLegend(viewMode)

        // Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 10f)
                            panX += pan.x
                            panY += pan.y
                        }
                    }
            ) {
                val centerOffset = Offset(size.width / 2f + panX, size.height / 2f + panY)
                val pixelPerMeter = 20f * scale
                
                // Draw Grid
                drawGrid(centerOffset, pixelPerMeter, size)
                
                // Draw Cells (Heatmap)
                val maxObs = max(1, mapState.cells.values.maxOfOrNull { it.observationCount } ?: 1)
                
                mapState.cells.values.forEach { cell ->
                    val cellX = centerOffset.x + (cell.centerX * pixelPerMeter)
                    val cellY = centerOffset.y + (cell.centerY * pixelPerMeter)
                    val cellSizePx = mapState.cellSizeMeters * pixelPerMeter
                    
                    val color = when (viewMode) {
                        MapViewMode.RF_DENSITY -> {
                            val intensity = (cell.observationCount.toFloat() / maxObs).coerceIn(0f, 1f)
                            Color(0f, 1f, 0f, intensity * 0.8f)
                        }
                        MapViewMode.SIGNAL_STRENGTH -> {
                            val intensity = ((cell.averageRssi + 100f) / 60f).coerceIn(0f, 1f)
                            Color(intensity, 1f - intensity, 0f, 0.7f)
                        }
                        MapViewMode.NOISE_FLOOR -> {
                            val intensity = ((cell.estimatedNoiseFloor + 110f) / 30f).coerceIn(0f, 1f)
                            Color(0f, intensity, 1f, 0.6f)
                        }
                        MapViewMode.DEVICES -> {
                            val intensity = (cell.uniqueDevices.size.toFloat() / 10f).coerceIn(0f, 1f)
                            Color(1f, 0f, 1f, intensity * 0.7f)
                        }
                        MapViewMode.ANOMALIES -> {
                            val intensity = (cell.anomalyCount.toFloat() / 5f).coerceIn(0f, 1f)
                            if (intensity > 0) Color(1f, 0f, 0f, intensity * 0.9f) else Color.Transparent
                        }
                    }
                    
                    if (color != Color.Transparent) {
                        drawRect(
                            color = color,
                            topLeft = Offset(cellX - (cellSizePx/2f), cellY - (cellSizePx/2f)),
                            size = Size(cellSizePx, cellSizePx)
                        )
                    }
                }
                
                // Draw Investigation Path
                if (mapState.userPath.size > 1) {
                    val path = Path()
                    mapState.userPath.forEachIndexed { index, point ->
                        val px = centerOffset.x + (point.first * pixelPerMeter)
                        val py = centerOffset.y + (point.second * pixelPerMeter)
                        if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.5f),
                        style = Stroke(width = 2f * scale)
                    )
                }
                
                // Draw User Position
                val userPx = centerOffset.x + (mapState.currentUserX * pixelPerMeter)
                val userPy = centerOffset.y + (mapState.currentUserY * pixelPerMeter)
                drawCircle(
                    color = Color.Blue,
                    radius = 4f * scale,
                    center = Offset(userPx, userPy)
                )
                
                // Draw Selected Target (if any)
                uiState.selectedTargetDeviceId?.let { targetId ->
                    val targetBlip = uiState.activeBlips.find { it.id == targetId }
                    if (targetBlip != null) {
                        val absoluteAngleDeg = (uiState.headingDegrees + targetBlip.targetAngleOffset) % 360f
                        val absoluteAngleRad = Math.toRadians(absoluteAngleDeg.toDouble())
                        val targetX = mapState.currentUserX + (targetBlip.distance * kotlin.math.sin(absoluteAngleRad)).toFloat()
                        val targetY = mapState.currentUserY - (targetBlip.distance * kotlin.math.cos(absoluteAngleRad)).toFloat()
                        
                        drawCircle(
                            color = Color.Red,
                            radius = 6f * scale,
                            center = Offset(centerOffset.x + targetX * pixelPerMeter, centerOffset.y + targetY * pixelPerMeter),
                            style = Stroke(width = 2f * scale)
                        )
                        drawLine(
                            color = Color.Red.copy(alpha = 0.5f),
                            start = Offset(userPx, userPy),
                            end = Offset(centerOffset.x + targetX * pixelPerMeter, centerOffset.y + targetY * pixelPerMeter),
                            strokeWidth = 1f * scale,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(centerOffset: Offset, pixelPerMeter: Float, size: Size) {
    val gridColor = Color.DarkGray.copy(alpha = 0.3f)
    val step = pixelPerMeter * 5f // 5 meter grid lines
    
    // Vertical lines
    var startX = centerOffset.x % step
    while (startX < size.width) {
        drawLine(color = gridColor, start = Offset(startX, 0f), end = Offset(startX, size.height), strokeWidth = 1f)
        startX += step
    }
    
    // Horizontal lines
    var startY = centerOffset.y % step
    while (startY < size.height) {
        drawLine(color = gridColor, start = Offset(0f, startY), end = Offset(size.width, startY), strokeWidth = 1f)
        startY += step
    }
    
    // Origin Crosshair
    drawLine(color = Color.White.copy(alpha = 0.4f), start = Offset(centerOffset.x, 0f), end = Offset(centerOffset.x, size.height), strokeWidth = 1f)
    drawLine(color = Color.White.copy(alpha = 0.4f), start = Offset(0f, centerOffset.y), end = Offset(size.width, centerOffset.y), strokeWidth = 1f)
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
        Text(text = "Legend: ${mode.desc}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}
