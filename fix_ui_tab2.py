import re

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'r') as f:
    content = f.read()

tabs_old = """            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Patterns") })
            Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Cross-Session") })
        }
        
        when (selectedTab) {
            0 -> SessionOverviewTab(sessionEngine, viewModel)
            1 -> PastSessionsTab(sessionEngine) { sessionId -> viewModel.resumeInvestigationSession(sessionId) }
            2 -> AnomaliesTab(anomalyEngine)
            3 -> PatternsTab(patternEngine)
            4 -> CrossSessionAnalysisTab(viewModel.rfCrossSessionEngine)
        }"""

tabs_new = """            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Patterns") })
            Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Graph") })
            Tab(selected = selectedTab == 5, onClick = { selectedTab = 5 }, text = { Text("Cross-Session") })
        }
        
        when (selectedTab) {
            0 -> SessionOverviewTab(sessionEngine, viewModel)
            1 -> PastSessionsTab(sessionEngine) { sessionId -> viewModel.resumeInvestigationSession(sessionId) }
            2 -> AnomaliesTab(anomalyEngine)
            3 -> PatternsTab(patternEngine)
            4 -> IntelligenceGraphTab(intelligenceEngine)
            5 -> CrossSessionAnalysisTab(viewModel.rfCrossSessionEngine)
        }"""

content = content.replace(tabs_old, tabs_new)

graph_tab = """

@Composable
fun IntelligenceGraphTab(engine: RfIntelligenceCorrelationEngine) {
    val graph by engine.graph.collectAsStateWithLifecycle()
    
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Intelligence Relationships", style = MaterialTheme.typography.titleLarge)
            Text("Correlated observations between devices, anomalies, and temporal patterns within this session.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        
        val displayEdges = graph.edges.filter { it.relationshipType != "BELONGS_TO_SESSION" }
        
        if (displayEdges.isEmpty()) {
            item {
                Text("Insufficient evidence to form meaningful intelligence correlations yet.", color = Color.Gray)
            }
        } else {
            items(displayEdges) { edge ->
                var expanded by remember { mutableStateOf(false) }
                val sourceNode = graph.nodes.find { it.id == edge.sourceId }
                val targetNode = graph.nodes.find { it.id == edge.targetId }
                
                if (sourceNode != null && targetNode != null) {
                    val confidenceText = when {
                        edge.confidenceScore >= 90 -> "Strong correlation"
                        edge.confidenceScore >= 70 -> "Probable"
                        edge.confidenceScore >= 40 -> "Likely"
                        edge.confidenceScore >= 20 -> "Possible"
                        else -> "Weak correlation"
                    }
                    
                    val color = when {
                        edge.confidenceScore >= 90 -> Color.Green
                        edge.confidenceScore >= 70 -> Color(0xFF81C784)
                        edge.confidenceScore >= 40 -> Color(0xFFFFF176)
                        edge.confidenceScore >= 20 -> Color(0xFFFFB74D)
                        else -> Color.Gray
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${sourceNode.type.name}: ${sourceNode.label}", fontWeight = FontWeight.Bold)
                                    Text("relates to", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("${targetNode.type.name}: ${targetNode.label}", fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(confidenceText, color = color, fontWeight = FontWeight.Bold)
                                    TextButton(onClick = { expanded = !expanded }) {
                                        Text(if (expanded) "Hide Evidence" else "Show Evidence")
                                    }
                                }
                            }
                            
                            if (expanded) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text("Provenance & Explainability", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(edge.explainabilityReasoning, style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Confidence Score: ${edge.confidenceScore}/100", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("Independent Verification Required for definitive claims.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
"""

if "fun IntelligenceGraphTab" not in content:
    content += graph_tab

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'w') as f:
    f.write(content)
