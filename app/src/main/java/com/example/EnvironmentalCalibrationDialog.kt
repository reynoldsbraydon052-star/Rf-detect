package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun EnvironmentalCalibrationDialog(
    uiState: SignalRadarUiState,
    onDismiss: () -> Unit,
    onApplyCalibration: (rssiCutoff: Int, emfThreshold: Float) -> Unit
) {
    var isCalibrating by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    // Ambient readings
    var avgRssi by remember { mutableStateOf(-95) }
    var avgEmf by remember { mutableStateOf(0f) }

    // Result thresholds
    var recommendedRssi by remember { mutableStateOf(-90) }
    var recommendedEmf by remember { mutableStateOf(45f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LaunchedEffect(isCalibrating) {
        if (isCalibrating) {
            progress = 0f
            var accumulatedRssi = 0
            var accumulatedEmf = 0f
            val samples = 30
            for (i in 1..samples) {
                delay(100)
                progress = i.toFloat() / samples
                
                // Sample from current UI state
                val currentRssi = if (uiState.activeBlips.isNotEmpty()) {
                    uiState.activeBlips.map { it.rssi }.average().toInt()
                } else {
                    -95
                }
                val currentEmf = uiState.magnetometerData.totalMicroTesla

                accumulatedRssi += currentRssi
                accumulatedEmf += currentEmf

                avgRssi = accumulatedRssi / i
                avgEmf = accumulatedEmf / i
            }

            // Set recommended thresholds based on ambient noise
            recommendedRssi = minOf(-50, avgRssi + 15) // +15 dBm margin
            recommendedEmf = maxOf(45f, avgEmf + 25f) // +25 µT margin
            
            isCalibrating = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F1A14),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFFFF9800))
                Text(
                    "ENVIRONMENTAL CALIBRATION",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    ),
                    color = Color(0xFFFF9800)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Scan local RF and EMF noise floors to auto-tune radar sensitivity thresholds. This helps filter out persistent ambient interference.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.LightGray
                )

                if (isCalibrating) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2622))
                            .border(1.dp, Color(0xFFFF9800).copy(alpha = pulseAlpha), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFFFF9800).copy(alpha = pulseAlpha))
                        Text("SAMPLING NOISE FLOOR...", style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = Color(0xFFFF9800))
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = Color(0xFFFF9800),
                            trackColor = Color(0xFF333333)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("RF: $avgRssi dBm", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("EMF: %.1f µT".format(avgEmf), color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                } else if (progress >= 1f) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF14241B))
                            .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("CALIBRATION COMPLETE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = Color(0xFF00FF66))
                        
                        Divider(color = Color.DarkGray)
                        
                        Text("AMBIENT NOISE FLOOR:", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                        Text("Average RF: $avgRssi dBm", fontSize = 12.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                        Text("Average EMF: %.1f µT".format(avgEmf), fontSize = 12.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text("RECOMMENDED THRESHOLDS:", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                        Text("RSSI Cutoff: $recommendedRssi dBm", fontSize = 12.sp, color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace)
                        Text("EMF Warning: %.1f µT".format(recommendedEmf), fontSize = 12.sp, color = Color(0xFFFFCC00), fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        confirmButton = {
            if (progress >= 1f && !isCalibrating) {
                Button(
                    onClick = { onApplyCalibration(recommendedRssi, recommendedEmf) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66), contentColor = Color.Black)
                ) {
                    Text("APPLY SETTINGS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                }
            } else {
                Button(
                    onClick = { isCalibrating = true },
                    enabled = !isCalibrating,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.Black)
                ) {
                    Text(if (progress == 0f) "START SCAN" else "RE-SCAN", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCalibrating) {
                Text("CANCEL", color = Color.Gray)
            }
        }
    )
}
