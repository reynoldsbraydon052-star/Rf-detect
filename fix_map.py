import re

with open('app/src/main/java/com/example/FullScreenRadarMapOverlay.kt', 'r') as f:
    content = f.read()

# Add anomalies param
content = content.replace(
    "fun FullScreenRadarMapOverlay(\n    uiState: SignalRadarUiState,",
    "fun FullScreenRadarMapOverlay(\n    uiState: SignalRadarUiState,\n    anomalies: List<RfAnomalyEntity> = emptyList(),"
)

# Render anomalies on the map
# In HighFidelityRadarMap, add rendering for anomalies
content = content.replace(
    "fun HighFidelityRadarMap(",
    "fun HighFidelityRadarMap(\n    anomalies: List<RfAnomalyEntity> = emptyList(),"
)

# Inside HighFidelityRadarMap canvas block
anomaly_render_code = """
        // 5. Draw Anomalies
        anomalies.filter { it.spatialX != null && it.spatialY != null }.forEach { anomaly ->
            val ax = cx + (anomaly.spatialX!! / mapRangeMeters) * radius
            val ay = cy - (anomaly.spatialY!! / mapRangeMeters) * radius
            
            // Draw red pulsing triangle or diamond
            val severityColor = when (anomaly.severity) {
                "CRITICAL" -> Color.Red
                "HIGH" -> Color(0xFFFF4400)
                "MEDIUM" -> Color(0xFFFF9900)
                else -> Color.Yellow
            }
            
            drawCircle(
                color = severityColor.copy(alpha = 0.3f),
                radius = 12.dp.toPx(),
                center = Offset(ax, ay)
            )
            drawCircle(
                color = severityColor,
                radius = 6.dp.toPx(),
                center = Offset(ax, ay),
                style = Stroke(width = 2f)
            )
        }
        
        // 6. Draw Blips
"""

content = content.replace("        // 5. Draw Blips", anomaly_render_code)

# Pass anomalies to HighFidelityRadarMap
content = content.replace(
    "HighFidelityRadarMap(\n                        uiState = uiState,",
    "HighFidelityRadarMap(\n                        anomalies = anomalies,\n                        uiState = uiState,"
)

with open('app/src/main/java/com/example/FullScreenRadarMapOverlay.kt', 'w') as f:
    f.write(content)
