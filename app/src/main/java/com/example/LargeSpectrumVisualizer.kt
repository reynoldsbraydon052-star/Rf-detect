package com.example

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SettingsInputAntenna
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.log10
import kotlin.math.sin

data class PhoneFrequencyBand(
    val id: String,
    val bandName: String,
    val antennaHarness: String,
    val startFreqMhz: Double,
    val endFreqMhz: Double,
    val freqDisplayLabel: String,
    val color: Color,
    val protocols: List<String>,
    val channelSpacing: String,
    val maxTxPower: String,
    val description: String,
    val isHardwareCapable: Boolean = true
)

object PhoneSpectrumDatabase {
    val ALL_PHONE_BANDS = listOf(
        PhoneFrequencyBand(
            id = "nfc_hf",
            bandName = "NFC / HF Inductive Band",
            antennaHarness = "NFC Flat Coil Antenna",
            startFreqMhz = 13.56,
            endFreqMhz = 13.56,
            freqDisplayLabel = "13.56 MHz HF",
            color = Color(0xFFFF9800),
            protocols = listOf("ISO/IEC 14443-A/B", "NFC-A", "NFC-F (FeliCa)", "NDEF"),
            channelSpacing = "Single 13.56 MHz Carrier",
            maxTxPower = "+20 dBm (Inductive Field)",
            description = "Near-Field Communication coil integrated into the rear glass/battery housing for contactless proximity and tag reading."
        ),
        PhoneFrequencyBand(
            id = "fm_radio",
            bandName = "VHF FM Broadcast Receiver",
            antennaHarness = "3.5mm Headset Wire / Internal Parasitic",
            startFreqMhz = 87.5,
            endFreqMhz = 108.0,
            freqDisplayLabel = "87.5 - 108 MHz VHF",
            color = Color(0xFFE91E63),
            protocols = listOf("FM Stereo Analog", "RDS Data System", "RBDS"),
            channelSpacing = "200 kHz Channels",
            maxTxPower = "Rx Only (Pass-through)",
            description = "VHF broadcast radio receiver utilizing connected wired headsets or parasitic internal traces as antenna elements."
        ),
        PhoneFrequencyBand(
            id = "low_band_cell",
            bandName = "Low-Band Cellular & Sub-1GHz IoT",
            antennaHarness = "Primary Sub-1GHz Bottom Antenna",
            startFreqMhz = 600.0,
            endFreqMhz = 960.0,
            freqDisplayLabel = "600 - 960 MHz Sub-1GHz",
            color = Color(0xFF00E5FF),
            protocols = listOf("5G NR n71/n12/n28", "LTE Band 12/13/20/71", "FirstNet B14", "GSM 850/900"),
            channelSpacing = "5 MHz - 20 MHz Carriers",
            maxTxPower = "+23 dBm (Power Class 3)",
            description = "Deep penetration long-range cellular coverage and public safety first responder frequencies."
        ),
        PhoneFrequencyBand(
            id = "gnss_sat",
            bandName = "GNSS / GPS Satellite Navigation",
            antennaHarness = "Dual-Band Patch Antenna (Top Frame)",
            startFreqMhz = 1176.45,
            endFreqMhz = 1602.0,
            freqDisplayLabel = "1.176 - 1.602 GHz Satellite",
            color = Color(0xFFFFEB3B),
            protocols = listOf("GPS L1/L5", "Galileo E1/E5a", "GLONASS L1/L2", "BeiDou B1/B2a", "NavIC"),
            channelSpacing = "1.023 MHz / 10.23 MHz Code",
            maxTxPower = "Rx Only (-130 dBm Sensitivity)",
            description = "Multi-constellation dual-frequency satellite navigation array for sub-meter location fixes."
        ),
        PhoneFrequencyBand(
            id = "mid_band_cell",
            bandName = "Mid-Band LTE & 5G Sub-6",
            antennaHarness = "Mid-Frame Multi-MIMO Dipoles",
            startFreqMhz = 1700.0,
            endFreqMhz = 2700.0,
            freqDisplayLabel = "1.7 - 2.7 GHz Cellular",
            color = Color(0xFF00FF66),
            protocols = listOf("5G NR n1/n3/n7/n25/n41", "LTE B1/B2/B3/B4/B7/B38/B41", "AWS-1/AWS-3"),
            channelSpacing = "10 MHz - 100 MHz Carriers",
            maxTxPower = "+23 dBm - +26 dBm (HPUE)",
            description = "High capacity mid-band cellular backbone supporting 4x4 MIMO spatial multiplexing."
        ),
        PhoneFrequencyBand(
            id = "ism_2g4",
            bandName = "2.4 GHz ISM (Wi-Fi & Bluetooth)",
            antennaHarness = "Combo Wi-Fi/BT Upper Frame Antenna",
            startFreqMhz = 2400.0,
            endFreqMhz = 2483.5,
            freqDisplayLabel = "2.400 - 2.4835 GHz ISM",
            color = Color(0xFF76FF03),
            protocols = listOf("Wi-Fi 1/2/3/4/6 (802.11b/g/n/ax)", "Bluetooth 5.4 / BLE 6.0", "Thread / Zigbee"),
            channelSpacing = "20 MHz Wi-Fi / 2 MHz BLE",
            maxTxPower = "+20 dBm (100mW EIRP)",
            description = "Short range industrial/scientific/medical band for local networking, Bluetooth accessories, and BLE Channel Sounding."
        ),
        PhoneFrequencyBand(
            id = "c_band_5g",
            bandName = "C-Band & CBRS 5G NR",
            antennaHarness = "Dedicated C-Band Antenna Array",
            startFreqMhz = 3300.0,
            endFreqMhz = 4200.0,
            freqDisplayLabel = "3.3 - 4.2 GHz C-Band",
            color = Color(0xFFFF5722),
            protocols = listOf("5G NR n77 / n78 / n79", "CBRS Band 48 Private 5G", "Massive MIMO"),
            channelSpacing = "40 MHz - 100 MHz Carriers",
            maxTxPower = "+26 dBm (PC2 High Power UE)",
            description = "Ultra-fast mid-band 5G spectrum providing gigabit download speeds across suburban and urban areas."
        ),
        PhoneFrequencyBand(
            id = "wifi_5g",
            bandName = "5.0 GHz Wi-Fi (UNII-1/2/3)",
            antennaHarness = "Dual-Polarized 5GHz Wi-Fi Array",
            startFreqMhz = 5150.0,
            endFreqMhz = 5895.0,
            freqDisplayLabel = "5.150 - 5.895 GHz UNII",
            color = Color(0xFF18FFFF),
            protocols = listOf("Wi-Fi 5 (802.11ac)", "Wi-Fi 6 (802.11ax)", "DFS Radar Sensing"),
            channelSpacing = "20 / 40 / 80 / 160 MHz",
            maxTxPower = "+23 dBm (200mW EIRP)",
            description = "High-throughput local wireless band operating across UNII-1 through UNII-3 channels."
        ),
        PhoneFrequencyBand(
            id = "wifi_6e_7",
            bandName = "6 GHz Wi-Fi 6E / Wi-Fi 7",
            antennaHarness = "Tri-Band Wi-Fi 7 Module (Top Corner)",
            startFreqMhz = 5925.0,
            endFreqMhz = 7125.0,
            freqDisplayLabel = "5.925 - 7.125 GHz 6GHz",
            color = Color(0xFFD500F9),
            protocols = listOf("Wi-Fi 6E", "Wi-Fi 7 (802.11be)", "320 MHz Multi-Link (MLO)"),
            channelSpacing = "160 MHz / 320 MHz Wide Channels",
            maxTxPower = "+24 dBm LPI (Low Power Indoor)",
            description = "Next-generation pristine spectrum offering 320 MHz wide channels and multi-gigabit wireless throughput."
        ),
        PhoneFrequencyBand(
            id = "uwb_radar",
            bandName = "Ultra-Wideband (UWB) Spatial Radar",
            antennaHarness = "UWB Patch Antenna Array (Back/Edge)",
            startFreqMhz = 6489.6,
            endFreqMhz = 7987.2,
            freqDisplayLabel = "6.5 GHz / 8.0 GHz UWB",
            color = Color(0xFFFF4081),
            protocols = listOf("IEEE 802.15.4z Fine Ranging", "FiRa Consortium", "Apple/Android Precision Tag"),
            channelSpacing = "500 MHz Bandwidth Channels",
            maxTxPower = "-41.3 dBm/MHz Peak EIRP",
            description = "Centimeter-accurate pulse radar spatial positioning, direction finding (AoA), and secure digital key ranging."
        ),
        PhoneFrequencyBand(
            id = "mmwave_5g",
            bandName = "5G mmWave FR2 High-Band",
            antennaHarness = "Dual-Axis mmWave Phased Array Modules (Side/Top)",
            startFreqMhz = 24250.0,
            endFreqMhz = 39500.0,
            freqDisplayLabel = "24.25 - 39.5 GHz mmWave",
            color = Color(0xFF651FFF),
            protocols = listOf("5G NR n257 / n258 / n260 / n261", "Beamforming Phased Array"),
            channelSpacing = "100 MHz - 400 MHz Carriers",
            maxTxPower = "+43 dBm Total Radiated Power (TRP)",
            description = "Extreme multi-gigabit millimetric wave spectrum utilizing 64-element beamforming phased array modules."
        )
    )
}

@Composable
fun LargeSpectrumVisualizerCard(
    activeBlips: List<RadarBlip>,
    modifier: Modifier = Modifier
) {
    var selectedBandFilter by remember { mutableStateOf("ALL") }
    var isLogarithmicScale by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "spectrum_sweep")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_line"
    )

    val displayedBands = remember(selectedBandFilter) {
        if (selectedBandFilter == "ALL") {
            PhoneSpectrumDatabase.ALL_PHONE_BANDS
        } else {
            PhoneSpectrumDatabase.ALL_PHONE_BANDS.filter { it.id == selectedBandFilter }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("large_spectrum_visualizer_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Full Radio Spectrum",
                        tint = Color(0xFF00FF66),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "FULL PHONE ANTENNA SPECTRUM (13.5MHz - 39.5GHz)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF00FF66)
                        )
                        Text(
                            text = "10 All-Band Phone Hardware Radio Receptors Active",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Gray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = isLogarithmicScale,
                        onClick = { isLogarithmicScale = !isLogarithmicScale },
                        label = {
                            Text(
                                if (isLogarithmicScale) "LOG SCALE" else "LIN SCALE",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00FF66).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF00FF66)
                        )
                    )
                }
            }

            // Band Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedBandFilter == "ALL",
                        onClick = { selectedBandFilter = "ALL" },
                        label = {
                            Text(
                                "ALL SPECTRUM (10 BANDS)",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00FF66),
                            selectedLabelColor = Color.Black
                        )
                    )
                }

                items(PhoneSpectrumDatabase.ALL_PHONE_BANDS, key = { it.id }) { band ->
                    FilterChip(
                        selected = selectedBandFilter == band.id,
                        onClick = { selectedBandFilter = band.id },
                        label = {
                            Text(
                                band.freqDisplayLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = band.color.copy(alpha = 0.3f),
                            selectedLabelColor = band.color
                        )
                    )
                }
            }

            // Spectrum Waterfall Canvas Graph
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF060D0A))
                    .border(1.dp, Color(0xFF1B3D2B), RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Grid Lines
                    val gridLines = 8
                    for (i in 0..gridLines) {
                        val y = h * (i / gridLines.toFloat())
                        drawLine(
                            color = Color(0xFF122E20),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    val xGridCount = 10
                    for (i in 0..xGridCount) {
                        val x = w * (i / xGridCount.toFloat())
                        drawLine(
                            color = Color(0xFF122E20),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1f
                        )
                    }

                    val minFreqLog = log10(10.0) // ~10 MHz
                    val maxFreqLog = log10(40000.0) // 40,000 MHz (40 GHz)

                    fun getXForFreq(freqMhz: Double): Float {
                        return if (isLogarithmicScale) {
                            val freqLog = log10(freqMhz.coerceIn(10.0, 40000.0))
                            val norm = ((freqLog - minFreqLog) / (maxFreqLog - minFreqLog)).toFloat()
                            norm * w
                        } else {
                            (freqMhz / 40000.0 * w).toFloat()
                        }
                    }

                    // Render Frequency Band Coverage Blocks
                    PhoneSpectrumDatabase.ALL_PHONE_BANDS.forEach { band ->
                        val startX = getXForFreq(band.startFreqMhz)
                        val endX = getXForFreq(band.endFreqMhz).coerceAtLeast(startX + 4f)
                        val blockWidth = (endX - startX).coerceAtLeast(3f)

                        val isFilteredIn = selectedBandFilter == "ALL" || selectedBandFilter == band.id
                        val alpha = if (isFilteredIn) 0.35f else 0.08f

                        drawRect(
                            color = band.color.copy(alpha = alpha),
                            topLeft = Offset(startX, 0f),
                            size = Size(blockWidth, h)
                        )

                        // Top indicator cap line
                        drawLine(
                            color = band.color,
                            start = Offset(startX, 0f),
                            end = Offset(startX + blockWidth, 0f),
                            strokeWidth = 3f
                        )
                    }

                    // Draw Simulated/Live Frequency Power Spectrum Line
                    val path = Path()
                    val steps = 200
                    for (i in 0..steps) {
                        val normX = i / steps.toFloat()
                        val freqMhz = if (isLogarithmicScale) {
                            Math.pow(10.0, minFreqLog + normX * (maxFreqLog - minFreqLog))
                        } else {
                            normX * 40000.0
                        }

                        val currentX = normX * w
                        // Noise baseline + carrier peaks
                        var powerNorm = 0.15f + (sin(normX * 30f + sweepProgress * 6.28f) * 0.05f).toFloat()

                        // Add peaks for active bands
                        PhoneSpectrumDatabase.ALL_PHONE_BANDS.forEach { band ->
                            if (freqMhz >= band.startFreqMhz * 0.95 && freqMhz <= band.endFreqMhz * 1.05) {
                                val centerFreq = (band.startFreqMhz + band.endFreqMhz) / 2.0
                                val delta = Math.abs(freqMhz - centerFreq) / (band.endFreqMhz - band.startFreqMhz + 1.0)
                                val peakFactor = Math.max(0.0, 1.0 - delta * 2.0).toFloat()
                                powerNorm += peakFactor * 0.65f
                            }
                        }

                        val y = h * (1.0f - powerNorm.coerceIn(0.05f, 0.95f))

                        if (i == 0) {
                            path.moveTo(currentX, y)
                        } else {
                            path.lineTo(currentX, y)
                        }
                    }

                    // Fill under path
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF00FF66).copy(alpha = 0.35f), Color.Transparent),
                            startY = 0f,
                            endY = h
                        )
                    )

                    drawPath(
                        path = path,
                        color = Color(0xFF00FF66),
                        style = Stroke(width = 2.5f)
                    )

                    // Draw Live Intercept Blip Markers along spectrum
                    activeBlips.forEach { blip ->
                        val blipX = getXForFreq(blip.frequencyMhz)
                        val blipY = h * 0.35f

                        drawCircle(
                            color = Color.Yellow,
                            radius = 6f,
                            center = Offset(blipX, blipY)
                        )
                        drawCircle(
                            color = Color.Yellow.copy(alpha = 0.4f),
                            radius = 12f,
                            center = Offset(blipX, blipY),
                            style = Stroke(width = 1.5f)
                        )
                    }

                    // Animated Sweep Beam
                    val sweepX = sweepProgress * w
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(sweepX, 0f),
                        end = Offset(sweepX, h),
                        strokeWidth = 2f
                    )
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.25f), Color.Transparent),
                            startX = sweepX,
                            endX = (sweepX + 40f).coerceAtMost(w)
                        ),
                        topLeft = Offset(sweepX, 0f),
                        size = Size(40f, h)
                    )
                }

                // Frequency Axis Labels Overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("13.5MHz (NFC)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                    Text("1.5GHz (GNSS)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                    Text("2.4GHz (ISM)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                    Text("5.8GHz (Wi-Fi)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                    Text("39.5GHz (mmWave)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                }
            }

            // Detailed List of Displayed Frequency Bands
            Text(
                text = "PHONE ANTENNA BAND REGISTRATION (${displayedBands.size})",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayedBands.forEach { band ->
                    PhoneBandDetailTile(band = band)
                }
            }
        }
    }
}

@Composable
fun PhoneBandDetailTile(band: PhoneFrequencyBand) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0B1410),
        border = BorderStroke(1.dp, band.color.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("band_tile_${band.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(band.color)
                    )
                    Text(
                        text = band.bandName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = band.color.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = band.freqDisplayLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = band.color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Harness: ${band.antennaHarness} • Max Power: ${band.maxTxPower}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = Color.LightGray
            )

            Text(
                text = band.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color.Gray
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                band.protocols.forEach { proto ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF182E23)
                    ) {
                        Text(
                            text = proto,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFF00FF66),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
