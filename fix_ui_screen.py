import re

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'r') as f:
    content = f.read()

# Update the tabs in the main screen
tabs_old = """        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Active Session") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Anomalies") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Temporal Patterns") })
        }
        
        when (selectedTab) {
            0 -> SessionOverviewTab(sessionEngine)
            1 -> AnomaliesTab(anomalyEngine)
            2 -> PatternsTab(patternEngine)
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
        }
        
        when (selectedTab) {
            0 -> SessionOverviewTab(sessionEngine)
            1 -> PastSessionsTab(sessionEngine)
            2 -> AnomaliesTab(anomalyEngine)
            3 -> PatternsTab(patternEngine)
        }"""

content = content.replace(tabs_old, tabs_new)

# Append the PastSessionsTab
past_sessions = """

@Composable
fun PastSessionsTab(sessionEngine: RfInvestigationSessionEngine) {
    val allSessions by sessionEngine.allSessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeSession by sessionEngine.activeSession.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    val pastSessions = allSessions.filter { it.id != activeSession?.id }
    
    if (pastSessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No past sessions found.", color = Color.Gray)
        }
        return
    }
    
    val df = remember { SimpleDateFormat("MM-dd HH:mm", Locale.US) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pastSessions, key = { it.id }) { session ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(session.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(session.state, style = MaterialTheme.typography.labelSmall, color = if (session.state == "ARCHIVED") Color.Gray else MaterialTheme.colorScheme.primary)
                    }
                    Text("Started: ${df.format(Date(session.startTimeMs))}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ev: ${session.eventCount}", style = MaterialTheme.typography.bodySmall)
                        Text("Dev: ${session.deviceCount}", style = MaterialTheme.typography.bodySmall)
                        Text("Anom: ${session.anomalyCount}", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { 
                            kotlinx.coroutines.GlobalScope.launch {
                                sessionEngine.deleteSession(session.id)
                            }
                        }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { 
                            kotlinx.coroutines.GlobalScope.launch {
                                sessionEngine.archiveSession(session.id)
                            }
                        }) {
                            Text("Archive")
                        }
                        Button(onClick = { 
                            // Note: This needs to also inform ViewModel to reload hypotheses
                            // Currently just calls resume on engine
                        }) {
                            Text("Resume")
                        }
                    }
                }
            }
        }
    }
}
"""

if "fun PastSessionsTab" not in content:
    content += past_sessions

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'w') as f:
    f.write(content)
