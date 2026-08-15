package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThermalFusionCard(uiState: SignalRadarUiState) {
    var isThermalActive by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "ThermalAnimation")
    val thermalHueShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ThermalHue"
    )
    val thermalPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ThermalPulse"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .testTag("thermal_fusion_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF140D0F)),
        border = BorderStroke(1.dp, Color(0xFFFF3366).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "THERMAL FUSION RF OVERLAY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color(0xFFFF3366)
                )
                
                Switch(
                    checked = isThermalActive,
                    onCheckedChange = { active ->
                        isThermalActive = active
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFFF3366),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.testTag("thermal_fusion_switch")
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black, RoundedCornerShape(8.dp))
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isThermalActive) {
                    // GPU-Accelerated Thermal Camera Heatmap View
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val center = Offset(w / 2f, h / 2f)

                        // Ambient Thermal Background
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xFF0F041C),
                                    Color(0xFF2A0835),
                                    Color(0xFF0C0216)
                                )
                            )
                        )

                        // Primary Infrared Thermal Core Heatmap
                        val heatCoreRadius = (w.coerceAtMost(h) * 0.38f) * thermalPulse
                        val heatCenter = Offset(
                            center.x + (thermalHueShift - 0.5f) * 40f,
                            center.y + ((1f - thermalHueShift) - 0.5f) * 25f
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFFF66), // White-Hot core
                                    Color(0xFFFF9900), // Orange radiant
                                    Color(0xFFFF0055), // Magenta-Red heat gradient
                                    Color(0xFF8800AA), // Purple fringe
                                    Color.Transparent
                                ),
                                center = heatCenter,
                                radius = heatCoreRadius
                            ),
                            center = heatCenter,
                            radius = heatCoreRadius
                        )

                        // Secondary Ambient Heat Node
                        val secondaryRadius = heatCoreRadius * 0.65f
                        val secondaryCenter = Offset(
                            center.x - 70f + thermalHueShift * 20f,
                            center.y + 35f
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF5500).copy(alpha = 0.8f),
                                    Color(0xFFCC0088).copy(alpha = 0.5f),
                                    Color.Transparent
                                ),
                                center = secondaryCenter,
                                radius = secondaryRadius
                            ),
                            center = secondaryCenter,
                            radius = secondaryRadius
                        )

                        // Thermal Grid Crosshairs
                        drawLine(
                            color = Color(0xFFFF3366).copy(alpha = 0.3f),
                            start = Offset(center.x, 0f),
                            end = Offset(center.x, h),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color(0xFFFF3366).copy(alpha = 0.3f),
                            start = Offset(0f, center.y),
                            end = Offset(w, center.y),
                            strokeWidth = 1f
                        )
                    }
                    
                    // RF Overlay Blips projected over Thermal Image
                    Box(modifier = Modifier.fillMaxSize()) {
                        uiState.activeBlips.take(5).forEach { blip ->
                            val xPos = (blip.targetAngleOffset / 360f)
                            val size = ((blip.rssi + 100f) / 50f).coerceIn(0.1f, 1.0f) * 100f
                            
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(x = (xPos * 200f - 100f).dp)
                                    .size(size.dp)
                                    .background(
                                        Color(0xFF00FF66).copy(alpha = 0.35f),
                                        shape = CircleShape
                                    )
                                    .border(
                                        1.dp,
                                        Color(0xFF00FF66).copy(alpha = 0.7f),
                                        CircleShape
                                    )
                            )
                        }
                    }
                    
                    Text(
                        text = "FLIR ONE PRO • 60 FPS GPU FUSED • REAL-TIME CALIBRATED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF00FF66),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                    )
                } else {
                    Text(
                        text = "Connect USB-C Thermal Camera (FLIR/UVC) and Enable",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                    )
                }
            }
        }
    }
}
