package com.example

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SignalRadarUiState,
    onBack: () -> Unit,
    onSetMapRange: (Float) -> Unit,
    onSetRssiThreshold: (Int) -> Unit,
    onSetEmfThreshold: (Float) -> Unit,
    onSetBreachThreshold: (Float) -> Unit,
    onToggleBackgroundRecon: (Boolean) -> Unit,
    onToggleSmoothingLerp: (Boolean) -> Unit,
    onToggleHapticAlerts: (Boolean) -> Unit,
    onToggleVisualNotifs: (Boolean) -> Unit,
    onSetScanMode: (ScanMode) -> Unit,
    onExportLogsCsv: () -> Unit,
    onExportKmlBreadcrumbs: () -> Unit,
    onPurgeHistory: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SYSTEM CONFIGURATION",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            color = Color(0xFF00FF66)
                        )
                        Text(
                            text = "spec anal SIGINT & Hardware Diagnostic Parameters",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF00FF66)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF07120B)
                )
            )
        },
        containerColor = Color(0xFF030705)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Radar & Map Calibration
            SettingsSectionHeader(
                icon = Icons.Default.Radar,
                title = "RADAR & MAP CALIBRATION",
                subtitle = "Canvas rendering distance, smoothing & display limits"
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F13)),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Map Boundary Range Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Default Radar Canvas Range",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "${uiState.mapRangeMeters.toInt()} meters",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF00FF66)
                            )
                        }
                        Slider(
                            value = uiState.mapRangeMeters,
                            onValueChange = onSetMapRange,
                            valueRange = 5f..100f,
                            steps = 19,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00FF66),
                                activeTrackColor = Color(0xFF00FF66)
                            ),
                            modifier = Modifier.testTag("settings_map_range_slider")
                        )
                    }

                    // Spatial Interpolation (Lerp) Smoothing Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Marker Spatial Lerp Smoothing",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "Eliminate marker teleportation with smooth spatial interpolation",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = true,
                            onCheckedChange = { onToggleSmoothingLerp(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFF00FF66)
                            ),
                            modifier = Modifier.testTag("settings_lerp_switch")
                        )
                    }
                }
            }

            // Section 2: Hardware & RF Scanning Mode
            SettingsSectionHeader(
                icon = Icons.Default.Sensors,
                title = "HARDWARE & RF SCANNING ENGINE",
                subtitle = "Antenna polling frequency, RSSI cutoff & scan profile"
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F13)),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Scan Profile Presets
                    Text(
                        text = "Scan Profile Duty Cycle:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScanMode.entries.forEach { mode ->
                            val selected = uiState.scanMode == mode
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSetScanMode(mode) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) Color(0xFF00FF66).copy(alpha = 0.2f) else Color(0xFF06140B),
                                border = BorderStroke(1.dp, if (selected) Color(0xFF00FF66) else Color.Gray.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.title,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = if (selected) Color(0xFF00FF66) else Color.White
                                        )
                                        Text(
                                            text = mode.subtitle,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // RSSI Cutoff Threshold Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RSSI Signal Cutoff Noise Floor",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "${uiState.rssiAlertThresholdDbm} dBm",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF00E5FF)
                            )
                        }
                        Slider(
                            value = uiState.rssiAlertThresholdDbm.toFloat(),
                            onValueChange = { onSetRssiThreshold(it.toInt()) },
                            valueRange = -100f..-30f,
                            steps = 14,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF)
                            ),
                            modifier = Modifier.testTag("settings_rssi_cutoff_slider")
                        )
                    }
                }
            }

            // Section 3: Alerts & Breaches
            SettingsSectionHeader(
                icon = Icons.Default.Security,
                title = "ALERTS, BREACHES & BACKGROUND RECON",
                subtitle = "Security perimeter, haptics and background service"
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F13)),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Breach Perimeter Distance
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Breach Perimeter Distance",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "%.1f m".format(uiState.perimeterThresholdMeters),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFFFF3366)
                            )
                        }
                        Slider(
                            value = uiState.perimeterThresholdMeters,
                            onValueChange = onSetBreachThreshold,
                            valueRange = 1f..50f,
                            steps = 49,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF3366),
                                activeTrackColor = Color(0xFFFF3366)
                            ),
                            modifier = Modifier.testTag("settings_breach_slider")
                        )
                    }

                    // Background Service Recon Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Background Service Recon Engine",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "Continue monitoring RF & acoustic signals when app is in background",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = uiState.isBackgroundAlertServiceActive,
                            onCheckedChange = onToggleBackgroundRecon,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFF00FF66)
                            ),
                            modifier = Modifier.testTag("settings_background_recon_switch")
                        )
                    }

                    // Haptic Alerts Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tactical Haptic Vibration Pulse",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "Vibrate phone when a device breaches close proximity perimeter",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = uiState.isHapticAlertsEnabled,
                            onCheckedChange = onToggleHapticAlerts,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFF00FF66)
                            )
                        )
                    }
                }
            }

            // Section 4: Data & Storage
            SettingsSectionHeader(
                icon = Icons.Default.Download,
                title = "DATA EXPORT & STORAGE",
                subtitle = "Export captured wardriving logs and breadcrumbs"
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F13)),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            onExportLogsCsv()
                            Toast.makeText(context, "Exporting captured node logs to CSV...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_export_csv_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EXPORT CAPTURED LOGS (CSV / PCAP)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.Black
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            onExportKmlBreadcrumbs()
                            Toast.makeText(context, "Exporting GPS Wardriving Breadcrumbs (KML)...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = Color(0xFF00E5FF))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EXPORT GPS WARDRIVING BREADCRUMBS (KML)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFF00E5FF)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            onPurgeHistory()
                            Toast.makeText(context, "Captured interception history purged.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFFFF3366)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFFFF3366))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PURGE INTERCEPTION HISTORY CACHE",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFFFF3366)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF00FF66).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF00FF66),
                modifier = Modifier.size(20.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                ),
                color = Color(0xFF00FF66)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.LightGray
            )
        }
    }
}
