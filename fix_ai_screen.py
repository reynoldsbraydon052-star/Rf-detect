import re

with open('app/src/main/java/com/example/AiThreatIntelScreen.kt', 'r') as f:
    content = f.read()

# Add a banner for GeminiStatus
banner_code = """
        // Gemini Status Banner
        if (uiState.geminiStatus != GeminiStatus.READY) {
            val statusColor = if (uiState.geminiStatus == GeminiStatus.MISSING_KEY) Color(0xFFFFCC00) else Color(0xFFFF3366)
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "API Status", tint = statusColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI System Status: ${uiState.geminiStatus.name}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }
            }
        }
"""

content = content.replace(
    "Column(modifier = modifier.fillMaxSize()) {",
    "Column(modifier = modifier.fillMaxSize()) {\n" + banner_code
)

with open('app/src/main/java/com/example/AiThreatIntelScreen.kt', 'w') as f:
    f.write(content)

