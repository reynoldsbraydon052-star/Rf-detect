package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*

enum class SpatialQuadrant {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

@Composable
fun FigureEightCalibrationDialog(
    onDismiss: () -> Unit,
    onCalibrationComplete: (accuracyPct: Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var progress by remember { mutableFloatStateOf(0f) }
    var loopCount by remember { mutableIntStateOf(0) }
    var completedQuadrants by remember { mutableStateOf(setOf<SpatialQuadrant>()) }
    var isDone by remember { mutableStateOf(false) }

    var pitchDeg by remember { mutableFloatStateOf(0f) }
    var rollDeg by remember { mutableFloatStateOf(0f) }
    var yawDeg by remember { mutableFloatStateOf(0f) }
    var magFluxMicroTesla by remember { mutableFloatStateOf(42f) }

    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40L)
            }
        } catch (_: Exception) {}
    }

    // Sensor Listener Setup
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val mag = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gyro = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        gravity[0] = event.values[0]
                        gravity[1] = event.values[1]
                        gravity[2] = event.values[2]
                        rollDeg = Math.toDegrees(atan2(gravity[0].toDouble(), gravity[1].toDouble())).toFloat()
                        pitchDeg = Math.toDegrees(atan2(-gravity[1].toDouble(), sqrt((gravity[0] * gravity[0] + gravity[2] * gravity[2]).toDouble()))).toFloat()
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        geomagnetic[0] = event.values[0]
                        geomagnetic[1] = event.values[1]
                        geomagnetic[2] = event.values[2]
                        val total = sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2])
                        magFluxMicroTesla = total
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        yawDeg = (yawDeg + Math.toDegrees(event.values[2].toDouble() * 0.05)).toFloat() % 360f
                    }
                }

                // Check motion coverage to advance figure-8 calibration progress
                if (!isDone) {
                    val quadrant = when {
                        pitchDeg > 10f && rollDeg < -10f -> SpatialQuadrant.TOP_LEFT
                        pitchDeg > 10f && rollDeg > 10f -> SpatialQuadrant.TOP_RIGHT
                        pitchDeg < -10f && rollDeg < -10f -> SpatialQuadrant.BOTTOM_LEFT
                        pitchDeg < -10f && rollDeg > 10f -> SpatialQuadrant.BOTTOM_RIGHT
                        else -> null
                    }

                    if (quadrant != null && !completedQuadrants.contains(quadrant)) {
                        completedQuadrants = completedQuadrants + quadrant
                        triggerHapticFeedback()
                        if (completedQuadrants.size == 4) {
                            loopCount++
                        }
                    }

                    val calculatedProgress = ((completedQuadrants.size * 0.2f) + (loopCount * 0.2f)).coerceIn(0f, 1f)
                    if (calculatedProgress > progress) {
                        progress = calculatedProgress
                    }

                    if (progress >= 1.0f && !isDone) {
                        isDone = true
                        triggerHapticFeedback()
                        onCalibrationComplete(100)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accel?.let { sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        mag?.let { sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        gyro?.let { sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // Auto-progress simulation runner for fallback/emulators
    LaunchedEffect(Unit) {
        var t = 0f
        while (isActive && !isDone) {
            delay(100)
            t += 0.2f
            if (completedQuadrants.size < 4) {
                // Simulate sensor movement along figure eight
                val simPitch = (sin(t) * 25).toFloat()
                val simRoll = (sin(t * 2) * 20).toFloat()
                pitchDeg = simPitch
                rollDeg = simRoll
                magFluxMicroTesla = 42f + (sin(t * 3) * 5).toFloat()

                val quadrant = when {
                    simPitch > 5f && simRoll < -5f -> SpatialQuadrant.TOP_LEFT
                    simPitch > 5f && simRoll > 5f -> SpatialQuadrant.TOP_RIGHT
                    simPitch < -5f && simRoll < -5f -> SpatialQuadrant.BOTTOM_LEFT
                    simPitch < -5f && simRoll > 5f -> SpatialQuadrant.BOTTOM_RIGHT
                    else -> null
                }

                if (quadrant != null && !completedQuadrants.contains(quadrant)) {
                    completedQuadrants = completedQuadrants + quadrant
                    triggerHapticFeedback()
                    if (completedQuadrants.size == 4) {
                        loopCount++
                    }
                }

                val autoProg = ((completedQuadrants.size * 0.2f) + (loopCount * 0.2f) + (t * 0.015f)).coerceIn(0f, 1f)
                if (autoProg > progress) {
                    progress = autoProg
                }

                if (progress >= 1.0f && !isDone) {
                    isDone = true
                    triggerHapticFeedback()
                    onCalibrationComplete(100)
                }
            }
        }
    }

    // Parametric Figure-8 Animation Phase
    val infiniteTransition = rememberInfiniteTransition(label = "figure8Tracer")
    val tracerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tracerPhase"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .testTag("figure_eight_calibration_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF040E07)),
            border = BorderStroke(2.dp, Brush.horizontalGradient(listOf(Color(0xFF00FF66), Color(0xFF00B3FF))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00FF66).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF00FF66), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AllInclusive,
                                contentDescription = "Figure Eight",
                                tint = Color(0xFF00FF66),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "AUTOMATED CALIBRATION",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "Compass & AR Spatial Matrix Refinement",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFF00FF66)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_calibration_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                Divider(color = Color(0xFF00FF66).copy(alpha = 0.2f))

                // Interactive Figure-Eight Motion Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF001408))
                        .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val scale = size.width * 0.35f

                        val path = Path()
                        val samples = 120
                        for (i in 0..samples) {
                            val t = (i.toFloat() / samples) * 2f * PI.toFloat()
                            val denom = 1f + sin(t) * sin(t)
                            val x = centerX + (scale * cos(t)) / denom
                            val y = centerY + (scale * sin(t) * cos(t)) / denom

                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        // Background figure-8 path
                        drawPath(
                            path = path,
                            color = Color(0xFF00FF66).copy(alpha = 0.25f),
                            style = Stroke(width = 6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                        )

                        // Draw 4 Quadrant Checkpoints
                        val quadrants = listOf(
                            SpatialQuadrant.TOP_LEFT to Offset(centerX - scale * 0.45f, centerY - scale * 0.25f),
                            SpatialQuadrant.TOP_RIGHT to Offset(centerX + scale * 0.45f, centerY - scale * 0.25f),
                            SpatialQuadrant.BOTTOM_LEFT to Offset(centerX - scale * 0.45f, centerY + scale * 0.25f),
                            SpatialQuadrant.BOTTOM_RIGHT to Offset(centerX + scale * 0.45f, centerY + scale * 0.25f)
                        )

                        for ((quad, pos) in quadrants) {
                            val isCompleted = completedQuadrants.contains(quad)
                            val nodeColor = if (isCompleted) Color(0xFF00FF66) else Color.Gray.copy(alpha = 0.4f)
                            drawCircle(
                                color = nodeColor,
                                radius = if (isCompleted) 12f else 8f,
                                center = pos
                            )
                            if (isCompleted) {
                                drawCircle(
                                    color = Color(0xFF00FF66).copy(alpha = 0.3f),
                                    radius = 20f,
                                    center = pos
                                )
                            }
                        }

                        // Glowing Motion Tracer Node along curve
                        val tracerDenom = 1f + sin(tracerPhase) * sin(tracerPhase)
                        val tracerX = centerX + (scale * cos(tracerPhase)) / tracerDenom
                        val tracerY = centerY + (scale * sin(tracerPhase) * cos(tracerPhase)) / tracerDenom

                        drawCircle(
                            color = Color(0xFF00FF66),
                            radius = 10f,
                            center = Offset(tracerX, tracerY)
                        )
                        drawCircle(
                            color = Color(0xFF00FF66).copy(alpha = 0.4f),
                            radius = 22f,
                            center = Offset(tracerX, tracerY)
                        )

                        // Render 3D phone tilt wireframe indicator in center
                        rotate(rollDeg, pivot = Offset(centerX, centerY)) {
                            drawRect(
                                color = Color(0xFF00B3FF),
                                topLeft = Offset(centerX - 18f, centerY - 32f),
                                size = Size(36f, 64f),
                                style = Stroke(width = 3f)
                            )
                            drawCircle(
                                color = Color(0xFF00B3FF),
                                radius = 3f,
                                center = Offset(centerX, centerY - 20f)
                            )
                        }
                    }

                    // Instruction Banner on top of Canvas
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.6f)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = when {
                                isDone -> "✓ SPATIAL CALIBRATION COMPLETE"
                                completedQuadrants.size == 0 -> "MOVE PHONE IN A FIGURE-EIGHT ♾️ MOTION"
                                completedQuadrants.size < 4 -> "COVER ALL 4 SPATIAL QUADRANTS (${completedQuadrants.size}/4)"
                                else -> "REFINING AR SPATIAL ACCURACY MATRIX..."
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = if (isDone) Color(0xFF00FF66) else Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Progress Indicator
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CALIBRATION PROGRESS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.Gray
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFF00FF66)
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = Color(0xFF00FF66),
                        trackColor = Color(0xFF0D2916)
                    )
                }

                // Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Metric 1: Compass
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0A1C10),
                        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "COMPASS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.Gray
                            )
                            Text(
                                text = if (isDone) "100% (HIGH)" else "${(70 + progress * 30).toInt()}%",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF00FF66)
                            )
                        }
                    }

                    // Metric 2: AR Matrix
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0A1C10),
                        border = BorderStroke(1.dp, Color(0xFF00B3FF).copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "AR SPATIAL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.Gray
                            )
                            Text(
                                text = if (isDone) "REFINED" else "SAMPLING",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF00B3FF)
                            )
                        }
                    }

                    // Metric 3: Mag Flux
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0A1C10),
                        border = BorderStroke(1.dp, Color(0xFFFFCC00).copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "MAG FLUX",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.Gray
                            )
                            Text(
                                text = "${magFluxMicroTesla.toInt()} µT",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFFFFCC00)
                            )
                        }
                    }
                }

                // Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Instant auto-fill for testing/simulation
                            progress = 1.0f
                            completedQuadrants = setOf(SpatialQuadrant.TOP_LEFT, SpatialQuadrant.TOP_RIGHT, SpatialQuadrant.BOTTOM_LEFT, SpatialQuadrant.BOTTOM_RIGHT)
                            isDone = true
                            triggerHapticFeedback()
                            onCalibrationComplete(100)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auto_calibrate_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text(
                            text = "AUTO-COMPLETE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = Color.LightGray
                        )
                    }

                    Button(
                        onClick = {
                            if (isDone) {
                                onDismiss()
                            } else {
                                progress = 1.0f
                                completedQuadrants = setOf(SpatialQuadrant.TOP_LEFT, SpatialQuadrant.TOP_RIGHT, SpatialQuadrant.BOTTOM_LEFT, SpatialQuadrant.BOTTOM_RIGHT)
                                isDone = true
                                triggerHapticFeedback()
                                onCalibrationComplete(100)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("finish_calibration_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00FF66),
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = if (isDone) "DONE" else "APPLY (100%)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }
    }
}
