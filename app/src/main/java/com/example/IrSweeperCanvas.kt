package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Jetpack Compose Renderer for the Passive IR Biometric Camera Sweeper.
 *
 * Uses an [AndroidView] wrapping a [TextureView] to render false-color IR bitmaps at 30 FPS
 * off the main Compose recomposition thread, preventing frame drops and UI freezes.
 */
@Composable
fun IrSweeperCanvas(
    engine: InfraredSweeperEngine,
    modifier: Modifier = Modifier,
    showHudOverlay: Boolean = true,
    onTargetClick: ((IrBloomTarget) -> Unit)? = null
) {
    val telemetry by engine.telemetry.collectAsState()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var currentThreshold by remember { mutableStateOf(engine.processor.luminanceThreshold.toFloat()) }

    // Synchronize lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                engine.stopSweeper()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            engine.stopSweeper()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFF070B10))
    ) {
        // High-Performance Hardware TextureView
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    isOpaque = true
                    textureViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { textureView ->
                textureViewRef = textureView
            }
        )

        // Continuous Render Loop off Compose Recomposition
        LaunchedEffect(textureViewRef) {
            val tv = textureViewRef ?: return@LaunchedEffect
            val dstRect = Rect()
            val paint = Paint().apply {
                isFilterBitmap = true
                isDither = true
            }

            scope.launch(Dispatchers.Default) {
                engine.latestFrame.collectLatest { bitmap ->
                    if (bitmap != null && !bitmap.isRecycled && tv.isAvailable) {
                        var canvas: Canvas? = null
                        try {
                            canvas = tv.lockCanvas()
                            if (canvas != null) {
                                dstRect.set(0, 0, canvas.width, canvas.height)
                                canvas.drawBitmap(bitmap, null, dstRect, paint)
                            }
                        } catch (_: Throwable) {
                        } finally {
                            if (canvas != null) {
                                try {
                                    tv.unlockCanvasAndPost(canvas)
                                } catch (_: Throwable) {}
                            }
                        }
                    }
                }
            }
        }

        // Tactical HUD Overlay
        if (showHudOverlay) {
            IrSweeperTacticalHud(
                telemetry = telemetry,
                currentThreshold = currentThreshold,
                onThresholdChange = { newThresh ->
                    currentThreshold = newThresh
                    engine.processor.luminanceThreshold = newThresh.toInt()
                },
                onStartClick = { engine.startSweeper() },
                onStopClick = { engine.stopSweeper() },
                onTargetClick = onTargetClick
            )
        }
    }
}

/**
 * Tactical Night-Vision HUD Overlay for the IR Sweeper Canvas.
 */
@Composable
fun BoxScope.IrSweeperTacticalHud(
    telemetry: IrSweeperTelemetry,
    currentThreshold: Float,
    onThresholdChange: (Float) -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onTargetClick: ((IrBloomTarget) -> Unit)? = null
) {
    // Header telemetry banner
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .align(Alignment.TopCenter),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sensor mode badge
        Surface(
            color = androidx.compose.ui.graphics.Color(0xCC111927),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (telemetry.isMonochrome) androidx.compose.ui.graphics.Color(0xFF00E676)
                else if (telemetry.isSecureLocked) androidx.compose.ui.graphics.Color(0xFFFF1744)
                else androidx.compose.ui.graphics.Color(0xFFFFB300)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        telemetry.isMonochrome -> Icons.Default.Sensors
                        telemetry.isSecureLocked -> Icons.Default.Lock
                        else -> Icons.Default.CameraAlt
                    },
                    contentDescription = null,
                    tint = if (telemetry.isMonochrome) androidx.compose.ui.graphics.Color(0xFF00E676)
                    else if (telemetry.isSecureLocked) androidx.compose.ui.graphics.Color(0xFFFF1744)
                    else androidx.compose.ui.graphics.Color(0xFFFFB300),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        telemetry.isMonochrome -> "BIOMETRIC NIR (PHYSICAL)"
                        telemetry.isSecureLocked -> "HARDWARE SECURE LOCKED"
                        telemetry.sensorState == IrSensorState.STREAMING -> "FRONT OPTICAL (NIR PASS)"
                        else -> "IR SENSOR STANDBY"
                    },
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // FPS & frame counter
        Surface(
            color = androidx.compose.ui.graphics.Color(0xCC111927),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF37474F))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FPS: %.1f".format(telemetry.frameFps),
                    color = androidx.compose.ui.graphics.Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    // Active Bloom Emitter Tag / Reticle List
    if (telemetry.bloomTargets.isNotEmpty()) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        ) {
            telemetry.bloomTargets.take(3).forEach { target ->
                Surface(
                    color = androidx.compose.ui.graphics.Color(0xE62A0822),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFFF007F)),
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color(0xFFFF007F),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EMITTER LOCK: LUM ${target.peakLuminance} [${(target.confidence * 100).toInt()}%]",
                            color = androidx.compose.ui.graphics.Color(0xFFFF80DF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }

    // Bottom control panel: Sensitivity slider & toggle
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(16.dp),
        color = androidx.compose.ui.graphics.Color(0xEE0D131F),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "IR HIGH-PASS THRESHOLD: ${currentThreshold.toInt()}",
                    color = androidx.compose.ui.graphics.Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "TARGETS: ${telemetry.bloomTargets.size}",
                    color = if (telemetry.bloomTargets.isNotEmpty()) androidx.compose.ui.graphics.Color(0xFFFF007F) else androidx.compose.ui.graphics.Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Slider(
                value = currentThreshold,
                onValueChange = onThresholdChange,
                valueRange = 100f..250f,
                colors = SliderDefaults.colors(
                    thumbColor = androidx.compose.ui.graphics.Color(0xFFFF007F),
                    activeTrackColor = androidx.compose.ui.graphics.Color(0xFFFF007F),
                    inactiveTrackColor = androidx.compose.ui.graphics.Color(0xFF334155)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (telemetry.sensorState == IrSensorState.STREAMING) {
                    Button(
                        onClick = onStopClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFDC2626)
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("STOP SWEEPER", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onStartClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFF00E676)
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ENGAGE IR SWEEPER", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.Black)
                    }
                }
            }
        }
    }
}
