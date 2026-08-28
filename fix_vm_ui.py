import re

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "fun IntelligenceDashboardScreen(",
    "fun IntelligenceDashboardScreen(\n    api: IntelligenceApi,"
)

content = content.replace(
    "val activeSession by sessionEngine.activeSession.collectAsStateWithLifecycle()",
    "val activeSession by api.sessionState.collectAsStateWithLifecycle()\n    val summary = api.investigationSummary"
)

content = content.replace(
    "session.eventCount", "summary.eventCount"
)
content = content.replace(
    "session.deviceCount", "summary.deviceCount"
)
content = content.replace(
    "session.anomalyCount", "summary.anomalyCount"
)

content = content.replace("sessionEngine.pauseSession()", "api.pauseSession()")
content = content.replace("sessionEngine.resumeSession(session.id)", "api.resumeSession(session.id)")
content = content.replace("sessionEngine.closeSession()", "api.closeSession()")
content = content.replace("sessionEngine.deleteSession(session.id)", "api.deleteSession(session.id)")
content = content.replace("sessionEngine.archiveSession(session.id)", "api.archiveSession(session.id)")

content = content.replace("viewModel.resumeInvestigationSession(sessionId)", "api.resumeSession(sessionId)")

# Add Provenance UI
provenance_ui = """
@Composable
fun ProvenanceView(api: IntelligenceApi) {
    val evidenceList by api.evidenceList.collectAsStateWithLifecycle()
    val summary = api.investigationSummary
    
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Evidence & Provenance", style = MaterialTheme.typography.titleLarge)
            Text("Traceable chain of evidence supporting analytical conclusions.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        
        if (evidenceList.isEmpty()) {
            item {
                Text("No evidence captured yet in this session.", color = Color.Gray)
            }
        } else {
            val grouped = evidenceList.groupBy { it.relatedDeviceId ?: it.relatedAnomalyId ?: it.relatedPatternId ?: "Unassociated" }
            
            items(grouped.entries.toList()) { (targetId, evidence) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Target: $targetId", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        val supporting = evidence.filter { it.isSupporting }
                        val contradicting = evidence.filter { !it.isSupporting }
                        
                        Text("Supporting Evidence", style = MaterialTheme.typography.labelMedium, color = Color(0xFF81C784))
                        supporting.forEach { ev ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Supporting", tint = Color(0xFF81C784), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${ev.type}: ${ev.measurement} = ${ev.value} ${ev.unit}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        
                        if (contradicting.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Contradicting Evidence", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                            contradicting.forEach { ev ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = "Contradicting", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${ev.type}: ${ev.measurement} = ${ev.value} ${ev.unit}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Source Events: ${evidence.mapNotNull { it.sourceEventId }.distinct().size}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("Analysis: ${evidence.firstOrNull()?.analysisComponent ?: "Unknown"}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("Session: ${evidence.firstOrNull()?.sessionId ?: "Unknown"}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}
"""

if "fun ProvenanceView" not in content:
    content += provenance_ui

# Add Provenance Tab
content = content.replace('Tab(selected = selectedTab == 5, onClick = { selectedTab = 5 }, text = { Text("Cross-Session") })',
"""Tab(selected = selectedTab == 5, onClick = { selectedTab = 5 }, text = { Text("Cross-Session") })
            Tab(selected = selectedTab == 6, onClick = { selectedTab = 6 }, text = { Text("Provenance") })""")

content = content.replace('5 -> CrossSessionAnalysisTab(viewModel.rfCrossSessionEngine)',
"""5 -> CrossSessionAnalysisTab(viewModel.rfCrossSessionEngine)
            6 -> ProvenanceView(api)""")

content = content.replace("fun SessionOverviewTab(sessionEngine: RfInvestigationSessionEngine, viewModel: SignalRadarViewModel)", "fun SessionOverviewTab(api: IntelligenceApi, viewModel: SignalRadarViewModel)")

content = content.replace("0 -> SessionOverviewTab(sessionEngine, viewModel)", "0 -> SessionOverviewTab(api, viewModel)")

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    main_content = f.read()

main_content = main_content.replace(
    "IntelligenceDashboardScreen(viewModel, uiState, viewModel.rfSessionEngine, viewModel.rfAnomalyEngine, viewModel.rfPatternEngine, viewModel.rfIntelligenceEngine)",
    "IntelligenceDashboardScreen(viewModel.intelligenceApi, viewModel, uiState, viewModel.rfSessionEngine, viewModel.rfAnomalyEngine, viewModel.rfPatternEngine, viewModel.rfIntelligenceEngine)"
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(main_content)

