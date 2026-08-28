package com.example

import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IntelligenceDashboardScreen(
    viewModel: SignalRadarViewModel,
    uiState: SignalRadarUiState,
    sessionEngine: RfInvestigationSessionEngine,
    anomalyEngine: RfAnomalyCorrelationEngine,
    patternEngine: RfTemporalPatternEngine,
    intelligenceEngine: RfIntelligenceCorrelationEngine
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = "Intelligence",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "RF Intelligence Correlation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Unified anomalies, patterns, and session data.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        ScrollableTabRow(
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
        }
    }
}

@Composable
fun SessionOverviewTab(sessionEngine: RfInvestigationSessionEngine, viewModel: SignalRadarViewModel) {
    val activeSession by sessionEngine.activeSession.collectAsStateWithLifecycle()
    
    if (activeSession == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active session.", color = Color.Gray)
        }
        return
    }
    
    val session = activeSession!!
    val df = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.US) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Session ID: ${session.id.take(8)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(session.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("State", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(session.state, style = MaterialTheme.typography.bodyMedium, color = if (session.state == "ACTIVE") Color.Green else Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Started", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(df.format(Date(session.startTimeMs)), style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Devices Tracked", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${session.deviceCount}", style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Anomalies", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${session.anomalyCount}", style = MaterialTheme.typography.bodyMedium, color = if (session.anomalyCount > 0) Color.Red else Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Events", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${session.eventCount}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            Text("Session Annotations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            val annotations by sessionEngine.activeSessionAnnotations.collectAsStateWithLifecycle()
            
            if (annotations.isEmpty()) {
                Text("No annotations added yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                annotations.forEach { annot ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(annot.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                val tf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                                Text(tf.format(java.util.Date(annot.timestampMs)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Text(annot.text, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            
            var showAddAnnotDialog by remember { mutableStateOf(false) }
            if (showAddAnnotDialog) {
                var annotText by remember { mutableStateOf("") }
                var annotCategory by remember { mutableStateOf("NOTE") }
                AlertDialog(
                    onDismissRequest = { showAddAnnotDialog = false },
                    title = { Text("Add Annotation") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = annotText,
                                onValueChange = { annotText = it },
                                label = { Text("Note") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            kotlinx.coroutines.GlobalScope.launch {
                                sessionEngine.addAnnotation(annotText, annotCategory)
                            }
                            showAddAnnotDialog = false
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddAnnotDialog = false }) { Text("Cancel") }
                    }
                )
            }
            
            Button(onClick = { showAddAnnotDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add Annotation")
            }

            Text("Export & Sharing", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            val context = LocalContext.current
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.exportRfEventsJson(context, android.net.Uri.EMPTY) }, modifier = Modifier.weight(1f)) {
                    Text("Export JSON")
                }
                Button(onClick = { viewModel.exportRfEventsCsv(context, android.net.Uri.EMPTY) }, modifier = Modifier.weight(1f)) {
                    Text("Export CSV")
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { 
                    kotlinx.coroutines.GlobalScope.launch {
                        if (session.state == "ACTIVE") {
                            sessionEngine.pauseSession()
                        } else {
                            sessionEngine.resumeSession(session.id)
                        }
                    }
                }) {
                    Text(if (session.state == "ACTIVE") "Pause Session" else "Resume Session")
                }
                TextButton(onClick = { 
                    kotlinx.coroutines.GlobalScope.launch {
                        sessionEngine.closeSession()
                    }
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Close Session")
                }
            }
        }
    }
}

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

@Composable
fun PatternsTab(patternEngine: RfTemporalPatternEngine) {
    val patterns by patternEngine.patterns.collectAsStateWithLifecycle()
    
    if (patterns.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No temporal patterns identified yet.", color = Color.Gray)
        }
        return
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(patterns.reversed(), key = { it.id }) { pattern ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = pattern.type.replace("_", " "),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = pattern.stability,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Observed ${pattern.observationCount} times. Confidence: ${pattern.confidenceScore}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    if (pattern.deviceHypothesisId != null) {
                        Text(
                            text = "Device: ${pattern.deviceHypothesisId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun PastSessionsTab(sessionEngine: RfInvestigationSessionEngine, onResume: (String) -> Unit) {
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
                            scope.launch {
                                sessionEngine.deleteSession(session.id)
                            }
                        }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { 
                            scope.launch {
                                sessionEngine.archiveSession(session.id)
                            }
                        }) {
                            Text("Archive")
                        }
                        Button(onClick = { onResume(session.id) }) {
                            Text("Resume")
                        }
                    }
                }
            }
        }
    }
}


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
                            
                            val drConfText = when {
                                dr.confidenceScore >= 90 -> "Strong correlation"
                                dr.confidenceScore >= 70 -> "Probable"
                                dr.confidenceScore >= 40 -> "Likely"
                                dr.confidenceScore >= 20 -> "Possible"
                                else -> "Weak correlation"
                            }
                            Text(drConfText, color = MaterialTheme.colorScheme.primary)

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
                            
                            val prConfText = when {
                                pr.confidenceScore >= 90 -> "Strong correlation"
                                pr.confidenceScore >= 70 -> "Probable"
                                pr.confidenceScore >= 40 -> "Likely"
                                pr.confidenceScore >= 20 -> "Possible"
                                else -> "Weak correlation"
                            }
                            Text(prConfText, color = MaterialTheme.colorScheme.primary)

                        }
                        Text("Seen in ${pr.sessionIds.size} sessions", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}


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
                        edge.weight >= 90 -> "Strong correlation"
                        edge.weight >= 70 -> "Probable"
                        edge.weight >= 40 -> "Likely"
                        edge.weight >= 20 -> "Possible"
                        else -> "Weak correlation"
                    }
                    
                    val color = when {
                        edge.weight >= 90 -> Color.Green
                        edge.weight >= 70 -> Color(0xFF81C784)
                        edge.weight >= 40 -> Color(0xFFFFF176)
                        edge.weight >= 20 -> Color(0xFFFFB74D)
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
                                Text(edge.explanation, style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Confidence Score: ${edge.weight}/100", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("Independent Verification Required for definitive claims.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
