import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

card_code = """
@Composable
fun EnvironmentalBaselineCard(
    summary: BaselineSummary,
    onToggleLearning: () -> Unit,
    onResetBaseline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ENVIRONMENT BASELINE",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Controls
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (summary.isLearning) Color(0xFF00FF66).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (summary.isLearning) Color(0xFF00FF66) else MaterialTheme.colorScheme.outline),
                        modifier = Modifier.clickable { onToggleLearning() }
                    ) {
                        Text(
                            text = if (summary.isLearning) "LEARNING ACTIVE" else "PAUSED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                            color = if (summary.isLearning) Color(0xFF00FF66) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { onResetBaseline() }
                    ) {
                        Text(
                            text = "RESET",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Known Fingerprints: ${summary.knownFingerprints}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                    Text(text = "New Fingerprints: ${summary.newFingerprints}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                    Text(text = "Missing Fingerprints: ${summary.missingFingerprints}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                    val actSign = if (summary.rfActivityDeltaPercent >= 0) "+" else ""
                    val freqSign = if (summary.freqOccupancyDeltaPercent >= 0) "+" else ""
                    Text(text = "RF Activity: $actSign${String.format("%.1f", summary.rfActivityDeltaPercent * 100)}%", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                    Text(text = "Frequency Occupancy: $freqSign${String.format("%.1f", summary.freqOccupancyDeltaPercent * 100)}%", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                    Text(text = "Baseline Confidence: ${String.format("%.2f", summary.baselineConfidence)}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                }
            }
            
            Text(
                text = "Age: ${summary.baselineAgeMs / 1000 / 60} min | Data Points: ${summary.observationsCollected}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray)
            )
        }
    }
}
"""

if "fun EnvironmentalBaselineCard(" not in content:
    content += card_code
    
with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
