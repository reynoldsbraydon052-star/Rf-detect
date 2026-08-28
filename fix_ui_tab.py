import re

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'r') as f:
    content = f.read()

tabs_old = """        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Active") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Past") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Anomalies") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Patterns") })
        }
        
        when (selectedTab) {
            0 -> SessionOverviewTab(sessionEngine)
            1 -> PastSessionsTab(sessionEngine) { sessionId -> viewModel.resumeInvestigationSession(sessionId) }
            2 -> AnomaliesTab(anomalyEngine)
            3 -> PatternsTab(patternEngine)
        }"""

tabs_new = """        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Active") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Past") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Anomalies") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Patterns") })
            Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Cross-Session") })
        }
        
        when (selectedTab) {
            0 -> SessionOverviewTab(sessionEngine)
            1 -> PastSessionsTab(sessionEngine) { sessionId -> viewModel.resumeInvestigationSession(sessionId) }
            2 -> AnomaliesTab(anomalyEngine)
            3 -> PatternsTab(patternEngine)
            4 -> CrossSessionAnalysisTab(viewModel.rfCrossSessionEngine)
        }"""

content = content.replace(tabs_old, tabs_new)

cross_tab = """

@Composable
fun CrossSessionAnalysisTab(engine: RfCrossSessionAnalysisEngine) {
    val result by engine.analysisResult.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        engine.runAnalysis()
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Cross-Session Analysis", style = MaterialTheme.typography.titleLarge)
            Text("Probabilistic recurrence of devices and patterns across multiple isolated investigation sessions.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        
        item {
            Text("Device Recurrence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        if (result.deviceRecurrences.isEmpty()) {
            item {
                Text("No devices found appearing in multiple sessions.", color = Color.Gray)
            }
        } else {
            items(result.deviceRecurrences) { dr ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(dr.primaryMac, fontWeight = FontWeight.Bold)
                            Text("${dr.confidenceScore}% Confidence", color = MaterialTheme.colorScheme.primary)
                        }
                        Text("Seen in ${dr.sessionIds.size} sessions", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(dr.sessionIds.joinToString(", ") { it.take(6) }, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }
        
        item {
            Text("Pattern Recurrence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        if (result.patternRecurrences.isEmpty()) {
            item {
                Text("No temporal or spatial patterns found repeating across sessions.", color = Color.Gray)
            }
        } else {
            items(result.patternRecurrences) { pr ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(pr.type, fontWeight = FontWeight.Bold)
                            Text("${pr.confidenceScore}% Confidence", color = MaterialTheme.colorScheme.primary)
                        }
                        Text("Seen in ${pr.sessionIds.size} sessions", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}
"""

if "fun CrossSessionAnalysisTab" not in content:
    content += cross_tab

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'w') as f:
    f.write(content)
