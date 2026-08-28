package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SimulationLabScreen(
    uiState: SignalRadarUiState,
    replayState: ReplayState,
    simulationScenario: SimulationScenario?,
    activeSession: RfSessionEntity?,
    savedSessions: List<RfSessionEntity>,
    currentPositionMs: Long,
    playbackSpeed: Float,
    onStartSimulation: (SimulationScenario) -> Unit,
    onStopSimulation: () -> Unit,
    onLoadReplay: (RfSessionEntity) -> Unit,
    onPlayReplay: () -> Unit,
    onPauseReplay: () -> Unit,
    onStopReplay: () -> Unit,
    onSeekReplay: (Long) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onReturnToLive: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030A06))
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF071C11))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EVIDENCE LAB",
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                
                if (uiState.operatingMode != OperatingMode.LIVE) {
                    Button(
                        onClick = onReturnToLive,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(
                            text = "RETURN TO LIVE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            val statusColor = when (uiState.operatingMode) {
                OperatingMode.LIVE -> Color.Green
                OperatingMode.SIMULATION -> Color.Yellow
                OperatingMode.REPLAY -> Color.Cyan
            }
            Text(
                text = "ACTIVE MODE: ${uiState.operatingMode.name}",
                color = statusColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF0A1F13),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("SIMULATION", fontFamily = FontFamily.Monospace) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("REPLAY", fontFamily = FontFamily.Monospace) }
            )
        }

        if (selectedTab == 0) {
            SimulationControlPanel(
                simulationScenario = simulationScenario,
                onStartSimulation = onStartSimulation,
                onStopSimulation = onStopSimulation
            )
        } else {
            ReplayControlPanel(
                replayState = replayState,
                activeSession = activeSession,
                savedSessions = savedSessions,
                currentPositionMs = currentPositionMs,
                playbackSpeed = playbackSpeed,
                onLoadReplay = onLoadReplay,
                onPlayReplay = onPlayReplay,
                onPauseReplay = onPauseReplay,
                onStopReplay = onStopReplay,
                onSeekReplay = onSeekReplay,
                onSetPlaybackSpeed = onSetPlaybackSpeed
            )
        }
    }
}

@Composable
fun SimulationControlPanel(
    simulationScenario: SimulationScenario?,
    onStartSimulation: (SimulationScenario) -> Unit,
    onStopSimulation: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (simulationScenario != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ACTIVE SIMULATION",
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = simulationScenario.name,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onStopSimulation,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("STOP SIMULATION", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
        
        item {
            Text(
                text = "AVAILABLE SCENARIOS",
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        items(SimulationScenario.entries.toTypedArray()) { scenario ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF112B1B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = scenario.name,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = { onStartSimulation(scenario) },
                        enabled = simulationScenario != scenario
                    ) {
                        Text("RUN", fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun ReplayControlPanel(
    replayState: ReplayState,
    activeSession: RfSessionEntity?,
    savedSessions: List<RfSessionEntity>,
    currentPositionMs: Long,
    playbackSpeed: Float,
    onLoadReplay: (RfSessionEntity) -> Unit,
    onPlayReplay: () -> Unit,
    onPauseReplay: () -> Unit,
    onStopReplay: () -> Unit,
    onSeekReplay: (Long) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (activeSession != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ACTIVE REPLAY",
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activeSession.name,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${currentPositionMs / 1000}s / ${((activeSession.endTimeMs ?: System.currentTimeMillis()) - activeSession.startTimeMs) / 1000}s",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Slider(
                        value = currentPositionMs.toFloat(),
                        onValueChange = { onSeekReplay(it.toLong()) },
                        valueRange = 0f..(((activeSession.endTimeMs ?: System.currentTimeMillis()) - activeSession.startTimeMs).toFloat().coerceAtLeast(1f))
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = onStopReplay) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
                        }
                        if (replayState == ReplayState.PLAYING) {
                            IconButton(onClick = onPauseReplay) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            IconButton(onClick = onPlayReplay) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Speed:", color = Color.White, fontFamily = FontFamily.Monospace)
                        listOf(0.5f, 1f, 2f, 5f).forEach { speed ->
                            FilterChip(
                                selected = playbackSpeed == speed,
                                onClick = { onSetPlaybackSpeed(speed) },
                                label = { Text("${speed}x", fontFamily = FontFamily.Monospace) }
                            )
                        }
                    }
                }
            }
        }
        
        Text(
            text = "SAVED SESSIONS",
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        
        if (savedSessions.isEmpty()) {
            Text(
                text = "No recorded sessions available. Use LIVE mode to record.",
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )
        } else {
            LazyColumn {
                items(savedSessions) { session ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF112B1B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = session.name,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${session.eventCount} events • ${((session.endTimeMs ?: System.currentTimeMillis()) - session.startTimeMs) / 1000}s",
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                            Button(onClick = { onLoadReplay(session) }) {
                                Text("LOAD", fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}
