import re

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'r') as f:
    content = f.read()

replacement = """
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable

@Composable
fun AnomaliesTab(anomalyEngine: RfAnomalyCorrelationEngine) {
    val anomalies by anomalyEngine.anomalies.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedSeverityFilter by remember { mutableStateOf<String?>(null) }
    var expandedAnomalyId by remember { mutableStateOf<String?>(null) }
    
    val filteredAnomalies = anomalies.filter {
        (searchQuery.isEmpty() || it.type.contains(searchQuery, ignoreCase = true) || (it.deviceId?.contains(searchQuery, ignoreCase = true) == true)) &&
        (selectedSeverityFilter == null || it.severity == selectedSeverityFilter)
    }.reversed()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            placeholder = { Text("Search anomalies...") },
            singleLine = true
        )
        
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val severities = listOf(null, "CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO")
            severities.forEach { sev ->
                FilterChip(
                    selected = selectedSeverityFilter == sev,
                    onClick = { selectedSeverityFilter = sev },
                    label = { Text(sev ?: "ALL") }
                )
            }
        }

        if (filteredAnomalies.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No anomalies match criteria.", color = Color.Gray)
            }
        } else {
            val df = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredAnomalies, key = { it.id }) { anomaly ->
                    val isExpanded = expandedAnomalyId == anomaly.id
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { expandedAnomalyId = if (isExpanded) null else anomaly.id },
                        colors = CardDefaults.cardColors(
                            containerColor = when(anomaly.severity) {
                                "CRITICAL" -> MaterialTheme.colorScheme.errorContainer
                                "HIGH" -> MaterialTheme.colorScheme.errorContainer.copy(alpha=0.8f)
                                "MEDIUM" -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = anomaly.type.replace("_", " "),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = df.format(Date(anomaly.timestampMs)),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Severity: ${anomaly.severity} | Confidence: ${anomaly.confidenceScore}% | Status: ${anomaly.status}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (anomaly.deviceId != null) {
                                Text(
                                    text = "Device: ${anomaly.deviceId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    Text("Detection: ${anomaly.detectionAlgorithm} v${anomaly.algorithmVersion}", style = MaterialTheme.typography.labelSmall)
                                    Text("Evidence: ${anomaly.supportingEvidenceJson}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { anomalyEngine.updateAnomalyStatus(anomaly.id, AnomalyStatus.DISMISSED) }) {
                                            Text("Dismiss")
                                        }
                                        Button(onClick = { anomalyEngine.updateAnomalyStatus(anomaly.id, AnomalyStatus.RESOLVED) }) {
                                            Text("Resolve")
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
"""

content = re.sub(r'fun AnomaliesTab.*?fun PatternsTab', replacement + '\n@Composable\nfun PatternsTab', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'w') as f:
    f.write(content)

