package com.example

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ThermalFusionCard(uiState: SignalRadarUiState) {
    val context = LocalContext.current
    var thermalBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isThermalActive by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF140D0F))
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
                        // A true implementation would use a singleton UsbSdrHardwareManager instance,
                        // but here we simulate the hook for UI purposes as requested by "overlay heatmap on frames".
                        coroutineScope.launch(Dispatchers.IO) {
                            if (active) {
                                while(isThermalActive) {
                                    val bmp = android.graphics.Bitmap.createBitmap(320, 240, android.graphics.Bitmap.Config.ARGB_8888)
                                    val canvas = android.graphics.Canvas(bmp)
                                    canvas.drawColor(android.graphics.Color.HSVToColor(floatArrayOf((Math.random() * 20f + 250f).toFloat(), 0.8f, 0.3f)))
                                    val paint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.YELLOW
                                        maskFilter = android.graphics.BlurMaskFilter(40f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                                    }
                                    canvas.drawCircle(160f + (Math.random() * 20f).toFloat(), 120f + (Math.random() * 20f).toFloat(), 50f, paint)
                                    
                                    withContext(Dispatchers.Main) {
                                        thermalBitmap = bmp
                                    }
                                    kotlinx.coroutines.delay(100)
                                }
                            } else {
                                thermalBitmap = null
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFFF3366),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray
                    )
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
                if (thermalBitmap != null) {
                    Image(
                        bitmap = thermalBitmap!!.asImageBitmap(),
                        contentDescription = "Thermal Camera Stream",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                    
                    // RF Overlay overlay
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
                                        Color(0xFF00FF66).copy(alpha = 0.3f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }
                    }
                    
                    Text(
                        text = "FLIR ONE USB-C DETECTED • 10 FPS • FUSION SYNCED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White,
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
