import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target_border = """        border = BorderStroke(
            1.5.dp,
            if (isSelectedTarget) Color(0xFF00FF66)
            else if (blip.distance < perimeterThresholdMeters) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )"""

replacement_border = """        border = BorderStroke(
            1.5.dp,
            when {
                isSelectedTarget -> Color(0xFF00FF66)
                blip.baselineState == BaselineState.ANOMALOUS -> Color(0xFFFF3366)
                blip.baselineState == BaselineState.CHANGED -> Color(0xFFFF8800)
                blip.baselineState == BaselineState.NEW -> Color(0xFFFFCC00)
                blip.distance < perimeterThresholdMeters -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            }
        )"""
content = content.replace(target_border, replacement_border)

target_tags = """                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = blip.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )"""

replacement_tags = target_tags + """
                        if (blip.baselineState != BaselineState.UNKNOWN) {
                            val stateColor = when (blip.baselineState) {
                                BaselineState.KNOWN -> Color(0xFF00E5FF)
                                BaselineState.NEW -> Color(0xFFFFCC00)
                                BaselineState.CHANGED -> Color(0xFFFF8800)
                                BaselineState.ANOMALOUS -> Color(0xFFFF3366)
                                else -> Color.Gray
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = stateColor.copy(alpha = 0.2f), border = BorderStroke(1.dp, stateColor)) {
                                Text(
                                    text = blip.baselineState.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace),
                                    color = stateColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }"""
content = content.replace(target_tags, replacement_tags)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
