package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

@Composable
fun RfEventRecorderScreen(
    uiState: SignalRadarUiState,
    recorderEngine: RfEventRecorderEngine,
    onExportJson: (Uri) -> Unit,
    onExportCsv: (Uri) -> Unit,
    onExportCaptures: () -> Unit
) {
    
    val recorderState by recorderEngine.recorderState.collectAsStateWithLifecycle()
    val totalDbCount by recorderEngine.totalDbEventCount.collectAsStateWithLifecycle(initialValue = 0)
    
    // Minimal query functionality just to show events
    val recentEvents by recorderEngine.getRecentEvents().collectAsStateWithLifecycle(initialValue = emptyList())

    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { onExportJson(it) }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { onExportCsv(it) }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Persistent RF Event Recorder",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Control Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val statusText = when {
                            recorderState.isRecording && !recorderState.isPaused -> "RECORDING LIVE"
                            recorderState.isRecording && recorderState.isPaused -> "PAUSED"
                            else -> "STOPPED"
                        }
                        Text(
                            text = statusText,
                            color = if (recorderState.isRecording && !recorderState.isPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Session Events: ${recorderState.totalRecordedEventsSession}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Total DB Events: $totalDbCount",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!recorderState.isRecording) {
                            Button(onClick = { recorderEngine.startRecording() }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start")
                            }
                        } else {
                            if (recorderState.isPaused) {
                                Button(onClick = { recorderEngine.resumeRecording() }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                                }
                            } else {
                                Button(onClick = { recorderEngine.pauseRecording() }) {
                                    Icon(Icons.Default.Pause, contentDescription = "Pause")
                                }
                            }
                            Button(
                                onClick = { recorderEngine.stopRecording() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop")
                            }
                        }
                    }
                }


                if (totalDbCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val sdf = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
                                val timestamp = sdf.format(Date())
                                createJsonLauncher.launch("SignalRadar_Investigation_$timestamp.json")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("JSON Export")
                        }
                        OutlinedButton(
                            onClick = {
                                val sdf = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
                                val timestamp = sdf.format(Date())
                                createCsvLauncher.launch("SignalRadar_Investigation_$timestamp.csv")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV Export")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = { recorderEngine.clearRecording() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear All Events", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        
        Text(
            text = "Recent Events (Latest 50)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(recentEvents, key = { it.eventId }) { event ->
                RfRecordedEventCard(event)
            }
        }
    }
}

@Composable
fun RfRecordedEventCard(event: RfRecordedEventEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${event.signalType} | ${event.deviceId.take(8)}",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
                Text(
                    text = sdf.format(Date(event.timestampMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${event.frequencyMhz}MHz | ${event.rssi}dBm | ${event.distanceMeters ?: "?"}m",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = event.provenance,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (event.classification != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Class: ${event.classification} (Conf: ${event.classificationConfidence})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
