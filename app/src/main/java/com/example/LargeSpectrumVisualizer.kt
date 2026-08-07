package com.example

import kotlin.math.*
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
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

data class SweepChannel(
    val channelName: String,
    val bandName: String,
    val centerFreqMhz: Double,
    val bandwidthMhz: Double,
    val category: String, // "WIFI_2G4", "BLE", "WIFI_5G"
    val color: Color
)

object CommonFrequencyChannels {
    val CHANNELS = listOf(
        // Wi-Fi 2.4 GHz Channels
        SweepChannel("Wi-Fi Ch 1", "2.4 GHz ISM", 2412.0, 22.0, "WIFI_2G4", Color(0xFF76FF03)),
        SweepChannel("Wi-Fi Ch 3", "2.4 GHz ISM", 2422.0, 22.0, "WIFI_2G4", Color(0xFF76FF03)),
        SweepChannel("Wi-Fi Ch 6", "2.4 GHz ISM", 2437.0, 22.0, "WIFI_2G4", Color(0xFF76FF03)),
        SweepChannel("Wi-Fi Ch 9", "2.4 GHz ISM", 2452.0, 22.0, "WIFI_2G4", Color(0xFF76FF03)),
        SweepChannel("Wi-Fi Ch 11", "2.4 GHz ISM", 2462.0, 22.0, "WIFI_2G4", Color(0xFF76FF03)),
        SweepChannel("Wi-Fi Ch 13", "2.4 GHz ISM", 2472.0, 22.0, "WIFI_2G4", Color(0xFF76FF03)),

        // Bluetooth LE Channels
        SweepChannel("BLE Adv Ch 37", "Bluetooth LE", 2402.0, 4.0, "BLE", Color(0xFF00E5FF)),
        SweepChannel("BLE Adv Ch 38", "Bluetooth LE", 2426.0, 4.0, "BLE", Color(0xFF00E5FF)),
        SweepChannel("BLE Adv Ch 39", "Bluetooth LE", 2480.0, 4.0, "BLE", Color(0xFF00E5FF)),
        SweepChannel("BLE Data Ch 10", "Bluetooth LE", 2422.0, 4.0, "BLE", Color(0xFF00E5FF)),
        SweepChannel("BLE Data Ch 20", "Bluetooth LE", 2442.0, 4.0, "BLE", Color(0xFF00E5FF)),
        SweepChannel("BLE Data Ch 30", "Bluetooth LE", 2462.0, 4.0, "BLE", Color(0xFF00E5FF)),

        // Wi-Fi 5 GHz UNII Channels
        SweepChannel("Wi-Fi Ch 36", "5.0 GHz UNII-1", 5180.0, 40.0, "WIFI_5G", Color(0xFF18FFFF)),
        SweepChannel("Wi-Fi Ch 40", "5.0 GHz UNII-1", 5200.0, 40.0, "WIFI_5G", Color(0xFF18FFFF)),
        SweepChannel("Wi-Fi Ch 44", "5.0 GHz UNII-1", 5220.0, 40.0, "WIFI_5G", Color(0xFF18FFFF)),
        SweepChannel("Wi-Fi Ch 48", "5.0 GHz UNII-1", 5240.0, 40.0, "WIFI_5G", Color(0xFF18FFFF)),
        SweepChannel("Wi-Fi Ch 149", "5.0 GHz UNII-3", 5745.0, 40.0, "WIFI_5G", Color(0xFF18FFFF)),
        SweepChannel("Wi-Fi Ch 157", "5.0 GHz UNII-3", 5785.0, 40.0, "WIFI_5G", Color(0xFF18FFFF)),
        SweepChannel("Wi-Fi Ch 161", "5.0 GHz UNII-3", 5805.0, 40.0, "WIFI_5G", Color(0xFF18FFFF))
    )
}

@Composable
fun LargeSpectrumVisualizerCard(
    activeBlips: List<RadarBlip>,
    selectedTargetDeviceId: String? = null,
    onSelectTargetDevice: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedBandFilter by remember { mutableStateOf("ALL") }
    var isLogarithmicScale by remember { mutableStateOf(true) }

    // Automatic Frequency Sweep Controls State
    var isFrequencySweepActive by remember { mutableStateOf(true) }
    var sweepCategoryFilter by remember { mutableStateOf("ALL") } // "ALL", "WIFI_2G4", "BLE", "WIFI_5G"
    var sweepSpeedMs by remember { mutableStateOf(1500L) } // 1.0s, 1.5s, 3.0s
    var currentSweepChannelIndex by remember { mutableIntStateOf(0) }

    // Peak Hold Data Map: Freq Bin Index -> Max RSSI (0.0f..1.0f)
    val peakHoldArray = remember { mutableStateMapOf<Int, Float>() }

    // Filter available sweep channels based on sweepCategoryFilter
    val activeSweepChannels = remember(sweepCategoryFilter) {
        if (sweepCategoryFilter == "ALL") {
            CommonFrequencyChannels.CHANNELS
        } else {
            CommonFrequencyChannels.CHANNELS.filter { it.category == sweepCategoryFilter }
        }
    }

    // Step through channels automatically when sweep is active
    LaunchedEffect(isFrequencySweepActive, sweepCategoryFilter, sweepSpeedMs, activeSweepChannels.size) {
        if (isFrequencySweepActive && activeSweepChannels.isNotEmpty()) {
            while (true) {
                delay(sweepSpeedMs)
                currentSweepChannelIndex = (currentSweepChannelIndex + 1) % activeSweepChannels.size
            }
        }
    }

    val currentChannel = remember(currentSweepChannelIndex, activeSweepChannels) {
        if (activeSweepChannels.isNotEmpty()) {
            activeSweepChannels[currentSweepChannelIndex.coerceIn(0, activeSweepChannels.size - 1)]
        } else {
            CommonFrequencyChannels.CHANNELS[0]
        }
    }

    // Active blips in the current sweep channel
    val activeBlipsInCurrentChannel = remember(activeBlips, currentChannel) {
        activeBlips.filter { blip ->
            abs(blip.frequencyMhz - currentChannel.centerFreqMhz) <= (currentChannel.bandwidthMhz / 1.5)
        }
    }

    val maxRssiInCurrentChannel = remember(activeBlipsInCurrentChannel) {
        activeBlipsInCurrentChannel.maxOfOrNull { it.rssi } ?: -95
    }

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
            // Header Row: Title & Scale Toggle
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
                            text = "AUTOMATIC FREQUENCY SWEEP & SPECTRUM",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF00FF66)
                        )
                        Text(
                            text = "Auto-cycling Wi-Fi / BLE channels • Peak Intercept Tracking",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Gray
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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

            // Frequency Sweep Control Panel Row
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF07140B),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sweep Toggle Play/Pause
                        Button(
                            onClick = { isFrequencySweepActive = !isFrequencySweepActive },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFrequencySweepActive) Color(0xFF00FF66) else Color(0xFF223A2A),
                                contentColor = if (isFrequencySweepActive) Color.Black else Color(0xFF00FF66)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isFrequencySweepActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Toggle Sweep",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFrequencySweepActive) "SWEEP ACTIVE" else "PAUSED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            )
                        }

                        // Sweep Speed Selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "DWELL:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                ),
                                color = Color.Gray
                            )

                            listOf(1000L to "1s", 1500L to "1.5s", 3000L to "3s").forEach { (speedMs, label) ->
                                val isSelected = sweepSpeedMs == speedMs
                                Surface(
                                    onClick = { sweepSpeedMs = speedMs },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Color(0xFF00FF66).copy(alpha = 0.3f) else Color(0xFF12281B),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF00FF66) else Color.Transparent)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 9.5.sp
                                        ),
                                        color = if (isSelected) Color(0xFF00FF66) else Color.LightGray,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Reset Peak Hold
                        TextButton(
                            onClick = { peakHoldArray.clear() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Clear Peak Hold",
                                tint = Color.Yellow,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "CLEAR PEAKS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = Color.Yellow
                                )
                            )
                        }
                    }

                    // Channel Filter Sub-Chips (ALL, Wi-Fi 2.4G, BLE, Wi-Fi 5G)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SWEEP BAND:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            ),
                            color = Color.Gray
                        )

                        listOf(
                            "ALL" to "ALL CHANNELS",
                            "WIFI_2G4" to "Wi-Fi 2.4G",
                            "BLE" to "BLE 0-39",
                            "WIFI_5G" to "Wi-Fi 5G"
                        ).forEach { (catKey, catLabel) ->
                            val isSel = sweepCategoryFilter == catKey
                            Surface(
                                onClick = {
                                    sweepCategoryFilter = catKey
                                    currentSweepChannelIndex = 0
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) Color(0xFF00FF66) else Color(0xFF0F2618),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = catLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    color = if (isSel) Color.Black else Color(0xFF00FF66),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Current Swept Channel HUD Status Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0B2114))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isFrequencySweepActive) Color(0xFF00FF66) else Color.Gray)
                            )
                            Text(
                                text = "CURRENTLY SWEEPING: ${currentChannel.channelName} (${currentChannel.centerFreqMhz.toInt()} MHz)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp
                                ),
                                color = Color.White
                            )
                        }

                        Text(
                            text = if (activeBlipsInCurrentChannel.isNotEmpty())
                                "⚡ ${activeBlipsInCurrentChannel.size} PEAKS DETECTED ($maxRssiInCurrentChannel dBm)"
                            else "SCANNING...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp
                            ),
                            color = if (activeBlipsInCurrentChannel.isNotEmpty()) Color.Yellow else Color.Gray
                        )
                    }
                }
            }

            // Band Filter Chips (General Antenna Filter)
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

            // Spectrum Waterfall & Frequency Sweep Canvas Graph
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
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

                    // Highlight Currently Swept Channel Zone on Canvas
                    val channelStartX = getXForFreq(currentChannel.centerFreqMhz - (currentChannel.bandwidthMhz / 2.0))
                    val channelEndX = getXForFreq(currentChannel.centerFreqMhz + (currentChannel.bandwidthMhz / 2.0)).coerceAtLeast(channelStartX + 12f)
                    val channelWidth = (channelEndX - channelStartX).coerceAtLeast(10f)

                    // Swept Channel Vertical Highlight Pillar
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(currentChannel.color.copy(alpha = 0.45f), currentChannel.color.copy(alpha = 0.08f))
                        ),
                        topLeft = Offset(channelStartX, 0f),
                        size = Size(channelWidth, h)
                    )

                    drawLine(
                        color = currentChannel.color,
                        start = Offset(channelStartX, 0f),
                        end = Offset(channelStartX, h),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                    drawLine(
                        color = currentChannel.color,
                        start = Offset(channelEndX, 0f),
                        end = Offset(channelEndX, h),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    // Draw Live Spectrum Trace Line & Calculate Peak Hold
                    val livePath = Path()
                    val peakPath = Path()
                    val steps = 220

                    for (i in 0..steps) {
                        val normX = i / steps.toFloat()
                        val freqMhz = if (isLogarithmicScale) {
                            Math.pow(10.0, minFreqLog + normX * (maxFreqLog - minFreqLog))
                        } else {
                            normX * 40000.0
                        }

                        val currentX = normX * w

                        // Baseline background noise
                        var powerNorm = 0.12f + (sin(normX * 35f) * 0.04f).toFloat()

                        // Add peaks for hardware antenna coverage
                        PhoneSpectrumDatabase.ALL_PHONE_BANDS.forEach { band ->
                            if (freqMhz >= band.startFreqMhz * 0.95 && freqMhz <= band.endFreqMhz * 1.05) {
                                val centerFreq = (band.startFreqMhz + band.endFreqMhz) / 2.0
                                val delta = abs(freqMhz - centerFreq) / (band.endFreqMhz - band.startFreqMhz + 1.0)
                                val peakFactor = max(0.0, 1.0 - delta * 2.0).toFloat()
                                powerNorm += peakFactor * 0.25f
                            }
                        }

                        // Add live real-time peaks from active blips
                        activeBlips.forEach { blip ->
                            val freqDelta = abs(freqMhz - blip.frequencyMhz)
                            val normalizedRssi = ((blip.rssi + 100) / 70f).coerceIn(0.1f, 1.0f)

                            if (freqDelta < 80.0) {
                                val proximityFactor = (1.0 - (freqDelta / 80.0)).toFloat()
                                powerNorm += normalizedRssi * proximityFactor * 0.65f
                            }
                        }

                        val finalPower = powerNorm.coerceIn(0.05f, 0.95f)

                        // Update Peak Hold Array
                        val existingPeak = peakHoldArray[i] ?: 0f
                        if (finalPower > existingPeak) {
                            peakHoldArray[i] = finalPower
                        }
                        val peakPower = max(existingPeak, finalPower)

                        val yLive = h * (1.0f - finalPower)
                        val yPeak = h * (1.0f - peakPower)

                        if (i == 0) {
                            livePath.moveTo(currentX, yLive)
                            peakPath.moveTo(currentX, yPeak)
                        } else {
                            livePath.lineTo(currentX, yLive)
                            peakPath.lineTo(currentX, yPeak)
                        }
                    }

                    // Fill under live path
                    val fillPath = Path().apply {
                        addPath(livePath)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF00FF66).copy(alpha = 0.30f), Color.Transparent),
                            startY = 0f,
                            endY = h
                        )
                    )

                    // Draw Peak Hold Trace (Dashed Gold Curve)
                    drawPath(
                        path = peakPath,
                        color = Color.Yellow.copy(alpha = 0.8f),
                        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
                    )

                    // Draw Live Spectrum Line (Neon Green)
                    drawPath(
                        path = livePath,
                        color = Color(0xFF00FF66),
                        style = Stroke(width = 2.5f)
                    )

                    // Draw Active Intercept Peak Markers & Highlight Callouts
                    activeBlips.forEach { blip ->
                        val blipX = getXForFreq(blip.frequencyMhz)
                        val normalizedRssi = ((blip.rssi + 100) / 70f).coerceIn(0.15f, 0.95f)
                        val blipY = h * (1.0f - normalizedRssi)

                        val isSelected = blip.id == selectedTargetDeviceId || blip.name == selectedTargetDeviceId
                        val isInCurrentSweepChannel = abs(blip.frequencyMhz - currentChannel.centerFreqMhz) <= (currentChannel.bandwidthMhz / 1.5)

                        val markerColor = when {
                            isSelected -> Color.Red
                            isInCurrentSweepChannel -> Color.Yellow
                            else -> Color(0xFF00E5FF)
                        }

                        // Vertical Laser Pillar over Peak
                        drawLine(
                            color = markerColor.copy(alpha = if (isInCurrentSweepChannel) 0.8f else 0.3f),
                            start = Offset(blipX, 0f),
                            end = Offset(blipX, h),
                            strokeWidth = if (isInCurrentSweepChannel) 2f else 1f
                        )

                        // Peak Diamond / Circle Marker
                        drawCircle(
                            color = markerColor,
                            radius = if (isInCurrentSweepChannel) 8f else 5f,
                            center = Offset(blipX, blipY)
                        )
                        drawCircle(
                            color = markerColor.copy(alpha = 0.4f),
                            radius = if (isInCurrentSweepChannel) 16f else 10f,
                            center = Offset(blipX, blipY),
                            style = Stroke(width = 2f)
                        )
                    }

                    // Live Animated Sweep Beam Line
                    val sweepBeamX = getXForFreq(currentChannel.centerFreqMhz)
                    drawLine(
                        color = Color(0xFF00FF66),
                        start = Offset(sweepBeamX, 0f),
                        end = Offset(sweepBeamX, h),
                        strokeWidth = 3f
                    )
                }

                // Frequency Axis Labels Overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.7f))
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

            // Scrolling 2D Waterfall Spectral Heatmap Plot
            Waterfall2DSpectrumCanvas(activeBlips = activeBlips, isLogarithmicScale = isLogarithmicScale)

            // Swept Channel Intercept Peaks Section
            Text(
                text = "SWEPT CHANNEL ACTIVE PEAKS SUMMARY",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Cards for channels with active detected peaks
            val channelsWithPeaks = remember(activeBlips) {
                CommonFrequencyChannels.CHANNELS.map { ch ->
                    val blipsInCh = activeBlips.filter { blip ->
                        abs(blip.frequencyMhz - ch.centerFreqMhz) <= (ch.bandwidthMhz / 1.5)
                    }
                    ch to blipsInCh
                }.filter { it.second.isNotEmpty() }
            }

            if (channelsWithPeaks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0A1F13),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Sweeping channels... No active signal peaks detected in swept bands yet.",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Gray,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    channelsWithPeaks.forEach { (ch, blips) ->
                        val maxRssi = blips.maxOfOrNull { it.rssi } ?: -95
                        val strongestBlip = blips.maxByOrNull { it.rssi }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF091C10),
                            border = BorderStroke(1.dp, if (ch == currentChannel) Color(0xFF00FF66) else ch.color.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("swept_channel_card_${ch.channelName}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(ch.color)
                                    )

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = ch.channelName,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                color = Color.White
                                            )

                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = ch.color.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "${ch.centerFreqMhz.toInt()} MHz",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 9.sp
                                                    ),
                                                    color = ch.color,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Strongest: ${strongestBlip?.name ?: "Unknown"} • ${blips.size} Active Peaks",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = Color.LightGray
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "PEAK RSSI",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 8.sp
                                            ),
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "$maxRssi dBm",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = Color.Yellow
                                        )
                                    }

                                    if (strongestBlip != null) {
                                        val isTargetLocked = strongestBlip.id == selectedTargetDeviceId

                                        Surface(
                                            onClick = {
                                                onSelectTargetDevice(if (isTargetLocked) null else strongestBlip.id)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isTargetLocked) Color.Red else Color(0xFF14301E),
                                            border = BorderStroke(1.dp, if (isTargetLocked) Color.Red else Color(0xFF00FF66))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isTargetLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                                    contentDescription = "Lock Device",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = if (isTargetLocked) "LOCKED" else "LOCK",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 9.5.sp
                                                    ),
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Detailed List of Displayed Phone Antenna Frequency Bands
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

@Composable
fun Waterfall2DSpectrumCanvas(
    activeBlips: List<RadarBlip>,
    modifier: Modifier = Modifier,
    isLogarithmicScale: Boolean = true
) {
    val frameHistory = remember { mutableStateListOf<FloatArray>() }

    LaunchedEffect(activeBlips.size, activeBlips.firstOrNull()?.rssi) {
        val numBins = 64
        val currentBins = FloatArray(numBins)
        for (i in 0 until numBins) {
            val freqMhz = 10.0 * (40000.0 / 10.0).pow(i / (numBins - 1.0))
            val matchingBlip = activeBlips.firstOrNull { abs(it.frequencyMhz - freqMhz) < (freqMhz * 0.15) }
            val normPower = if (matchingBlip != null) {
                ((matchingBlip.rssi + 100) / 70f).coerceIn(0.15f, 1.0f)
            } else {
                (0.04f + (sin(i * 0.3f + System.currentTimeMillis() * 0.003f) * 0.04f).toFloat())
            }
            currentBins[i] = normPower
        }

        if (frameHistory.size >= 24) {
            frameHistory.removeAt(frameHistory.size - 1)
        }
        frameHistory.add(0, currentBins)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF040A07)),
        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00FF66)))
                    Text(
                        text = "SCROLLING 2D WATERFALL HEATMAP (TIME-SERIES)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace),
                        color = Color(0xFF00FF66)
                    )
                }
                Text(
                    text = "BURST & DUTY-CYCLE HEATMAP",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                    color = Color.Gray
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val numRows = frameHistory.size.coerceAtLeast(1)
                    val rowHeight = h / 24f

                    frameHistory.forEachIndexed { rowIndex, frame ->
                        val topY = rowIndex * rowHeight
                        val binWidth = w / frame.size.toFloat()

                        frame.forEachIndexed { binIndex, pwr ->
                            val leftX = binIndex * binWidth
                            val color = when {
                                pwr > 0.8f -> Color.Red
                                pwr > 0.6f -> Color.Yellow
                                pwr > 0.35f -> Color(0xFF00FF66)
                                pwr > 0.15f -> Color(0xFF00E5FF)
                                else -> Color(0xFF001108)
                            }
                            drawRect(
                                color = color,
                                topLeft = Offset(leftX, topY),
                                size = Size(binWidth + 0.5f, rowHeight + 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
