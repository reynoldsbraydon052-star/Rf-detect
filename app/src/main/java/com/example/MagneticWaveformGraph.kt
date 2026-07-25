package com.example

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MagneticWaveformGraph(
    magnetometerData: MagnetometerData,
    modifier: Modifier = Modifier,
    maxHistorySamples: Int = 60
) {
    val sampleHistory = remember { mutableStateListOf<Float>() }

    LaunchedEffect(magnetometerData.totalMicroTesla) {
        val nextVal = if (magnetometerData.isCalibrated) {
            magnetometerData.netCalibratedMicroTesla
        } else {
            magnetometerData.totalMicroTesla
        }

        sampleHistory.add(nextVal)
        if (sampleHistory.size > maxHistorySamples) {
            sampleHistory.removeAt(0)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val neonMagenta = Color(0xFFFF00FF)
    val warningRed = MaterialTheme.colorScheme.error
    val isInterference = magnetometerData.isRfInterferenceDetected
    val strokeColor = if (isInterference) warningRed else neonMagenta
    val gridColor = Color(0xFF1E3A2B)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF08120E))
            .border(
                width = if (isInterference) 2.dp else 1.dp,
                color = if (isInterference) warningRed else neonMagenta.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
            .testTag("magnetic_waveform_graph_card")
    ) {
        // Graph Header & Real-time Readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "EMF FLUX WAVEFORM",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = strokeColor
                    )
                    if (isInterference) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = warningRed
                        ) {
                            Text(
                                text = "RF INTERFERENCE SPIKE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = if (magnetometerData.isCalibrated) "Net Calibrated Field Δ (µT)" else "Raw Magnetic Field (µT)",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Gray
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = strokeColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${sampleHistory.lastOrNull()?.toInt() ?: 0} µT",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = strokeColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Real-Time Canvas Waveform Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF040A07))
                .border(1.dp, Color(0xFF142B20), RoundedCornerShape(10.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("magnetic_waveform_canvas")
            ) {
                val width = size.width
                val height = size.height

                // Draw Background Oscilloscope Grid Lines
                val gridCols = 6
                val gridRows = 4
                val colSpacing = width / gridCols
                val rowSpacing = height / gridRows

                for (i in 1 until gridCols) {
                    drawLine(
                        color = gridColor,
                        start = Offset(i * colSpacing, 0f),
                        end = Offset(i * colSpacing, height),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                for (i in 1 until gridRows) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, i * rowSpacing),
                        end = Offset(width, i * rowSpacing),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                if (sampleHistory.size < 2) return@Canvas

                val minVal = 0f
                val maxVal = (sampleHistory.maxOrNull() ?: 100f).coerceAtLeast(100f)

                val xStep = width / (maxHistorySamples - 1).toFloat()

                val path = Path()
                val fillPath = Path()

                fillPath.moveTo(0f, height)

                sampleHistory.forEachIndexed { index, sample ->
                    val normalized = ((sample - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                    val x = index * xStep
                    val y = height - (normalized * (height - 20f)) - 10f

                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.lineTo(x, y)
                    } else {
                        val prevSample = sampleHistory[index - 1]
                        val prevNormalized = ((prevSample - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                        val prevX = (index - 1) * xStep
                        val prevY = height - (prevNormalized * (height - 20f)) - 10f

                        val controlX1 = prevX + (xStep / 2f)
                        val controlY1 = prevY
                        val controlX2 = prevX + (xStep / 2f)
                        val controlY2 = y

                        path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                        fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                    }
                }

                val lastIndex = sampleHistory.size - 1
                val lastX = lastIndex * xStep
                fillPath.lineTo(lastX, height)
                fillPath.close()

                // Draw Gradient Area Under Waveform
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            strokeColor.copy(alpha = 0.35f),
                            strokeColor.copy(alpha = 0.02f)
                        ),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw Glowing Waveform Stroke
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Draw Latest Point Pulsing Dot
                val latestSample = sampleHistory.last()
                val latestNormalized = ((latestSample - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                val latestY = height - (latestNormalized * (height - 20f)) - 10f

                drawCircle(
                    color = strokeColor.copy(alpha = 0.4f),
                    radius = 8.dp.toPx(),
                    center = Offset(lastX, latestY)
                )
                drawCircle(
                    color = strokeColor,
                    radius = 4.dp.toPx(),
                    center = Offset(lastX, latestY)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend & Scale Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text(
                text = "0 µT Baseline",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = Color.Gray
            )
            Text(
                text = "Peak: ${(sampleHistory.maxOrNull() ?: 0f).toInt()} µT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = neonMagenta
            )
        }
    }
}
