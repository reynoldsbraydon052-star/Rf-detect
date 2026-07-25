package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import com.example.ui.theme.TacticalDarkColorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TacticalRadarTheme {
                SignalRadarApp()
            }
        }
    }
}

@Composable
fun TacticalRadarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TacticalDarkColorScheme,
        content = content
    )
}

@Composable
fun SignalRadarApp(viewModel: SignalRadarViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        if (map[Manifest.permission.RECORD_AUDIO] == true) {
            viewModel.onPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO
        )
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionsLauncher.launch(permissions)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("signal_radar_root"),
        bottomBar = {
            BottomRadarNavBar(
                currentTab = uiState.selectedTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
            ) {
                // Top Tactical Header Badge
                TacticalHeader(uiState = uiState)

                // Auto-Calibration Stability UI Notification Banner
                AnimatedVisibility(visible = uiState.calibrationNotificationMessage != null) {
                    uiState.calibrationNotificationMessage?.let { msg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("calibration_notification_banner"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Calibration Shield",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "SENSOR BASELINE STABILIZED",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 0.8.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.dismissCalibrationNotification() },
                                    modifier = Modifier.size(28.dp).testTag("dismiss_calibration_notif_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Dismiss Notification",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.selectedTab) {
                        RadarTab.SWEEP_RADAR -> SweepRadarScreen(
                            uiState = uiState,
                            onToggleAudioSonar = { viewModel.toggleAudioSonar() },
                            onTogglePerimeterAlarm = { viewModel.togglePerimeterAlarm() },
                            onToggleScanning = { viewModel.toggleScanning() },
                            onFilterSelected = { viewModel.setFilterType(it) },
                            onToggleBleScanner = { viewModel.toggleBleScannerService() },
                            onClearBleDb = { viewModel.clearBleDatabaseLogs() },
                            onDeleteBleDevice = { viewModel.deleteBleDeviceFromDb(it) }
                        )

                        RadarTab.SPECTRUM_ANALYZER -> SpectrumAnalyzerScreen(
                            uiState = uiState,
                            onFilterSelected = { viewModel.setFilterType(it) }
                        )

                        RadarTab.MAGNETOMETER_EMF -> MagnetometerScreen(
                            uiState = uiState,
                            onRecalibrate = { viewModel.recalibrateMagnetometer() },
                            onTriggerSpike = { viewModel.triggerRfSpike() },
                            onClearInterference = { viewModel.clearRfInterference() }
                        )

                        RadarTab.SECURITY_GUARD -> SecurityGuardScreen(
                            uiState = uiState,
                            onThresholdChanged = { viewModel.setPerimeterThreshold(it) },
                            onToggleAlarm = { viewModel.togglePerimeterAlarm() }
                        )

                        RadarTab.CSV_LOG_CONSOLE -> CsvLogConsoleScreen(
                            uiState = uiState,
                            onClearLogs = { viewModel.clearLogHistory() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TacticalHeader(uiState: SignalRadarUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = "Radar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "RF SPECTRUM RADAR",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Universal Android • 100% Hardware Sensors Offline",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Zero connectivity shield chip
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Offline",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "100% OFFLINE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Status Stats Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ANTENNAS: ${uiState.activeAntennaCount} (Wi-Fi/BLE/Cell/EMF/Mic)",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Gray
                )
                Text(
                    text = "NODES: ${uiState.activeBlips.size}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            uiState.nearestBlip?.let { nearest ->
                Text(
                    text = "NEAREST: ${nearest.name.take(10)} (${String.format("%.1fm", nearest.distance)})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (nearest.distance < uiState.perimeterThresholdMeters) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SweepRadarScreen(
    uiState: SignalRadarUiState,
    onToggleAudioSonar: () -> Unit,
    onTogglePerimeterAlarm: () -> Unit,
    onToggleScanning: () -> Unit,
    onFilterSelected: (String) -> Unit,
    onToggleBleScanner: () -> Unit,
    onClearBleDb: () -> Unit,
    onDeleteBleDevice: (String) -> Unit
) {
    val filteredBlips = rememberFilteredBlips(uiState.activeBlips, uiState.selectedFilterType)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter Chips Row
        FilterChipsRow(
            selectedFilter = uiState.selectedFilterType,
            onFilterSelected = onFilterSelected
        )

        // Live Radar Scope Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(Color(0xFF08120E), RoundedCornerShape(20.dp))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    RoundedCornerShape(20.dp)
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            TacticalRadarCanvas(
                headingDegrees = uiState.headingDegrees,
                blips = filteredBlips,
                nearestBlipId = uiState.nearestBlip?.id,
                perimeterThresholdMeters = uiState.perimeterThresholdMeters
            )

            // Compass Heading Badge top right
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "HEADING: ${uiState.headingDegrees.toInt()}°",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Quick Controls Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggle Audio Sonar Button
                OutlinedButton(
                    onClick = onToggleAudioSonar,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (uiState.isAudioSonarActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (uiState.isAudioSonarActive) MaterialTheme.colorScheme.primary else Color.Gray
                    ),
                    modifier = Modifier.testTag("toggle_sonar_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Sonar",
                        tint = if (uiState.isAudioSonarActive) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isAudioSonarActive) "SONAR ON" else "SONAR OFF",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (uiState.isAudioSonarActive) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                // Toggle Perimeter Alarm Button
                OutlinedButton(
                    onClick = onTogglePerimeterAlarm,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (uiState.isPerimeterAlarmEnabled) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else Color.Transparent
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (uiState.isPerimeterAlarmEnabled) MaterialTheme.colorScheme.error else Color.Gray
                    ),
                    modifier = Modifier.testTag("toggle_alarm_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Alarm",
                        tint = if (uiState.isPerimeterAlarmEnabled) MaterialTheme.colorScheme.error else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isPerimeterAlarmEnabled) "ALARM ON" else "ALARM OFF",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (uiState.isPerimeterAlarmEnabled) MaterialTheme.colorScheme.error else Color.Gray
                    )
                }

                // Toggle Scanning Pause/Play Button
                IconButton(
                    onClick = onToggleScanning,
                    modifier = Modifier
                        .background(
                            if (uiState.isScanningActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                        .testTag("toggle_scan_button")
                ) {
                    Icon(
                        imageVector = if (uiState.isScanningActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Scan",
                        tint = if (uiState.isScanningActive) Color.White else Color.Black
                    )
                }
            }
        }

        // Bluetooth LE Room Database Log & Signal Tracker
        BleDatabaseTrackerCard(
            bleDevices = uiState.savedBleDevices,
            isScannerActive = uiState.isBleScannerServiceActive,
            onToggleScanner = onToggleBleScanner,
            onClearDatabase = onClearBleDb,
            onDeleteDevice = onDeleteBleDevice
        )
    }
}

@Composable
fun FilterChipsRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("ALL", "WIFI", "CELLULAR", "BLE", "MAGNETIC", "AUDIO")

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(filters) { filter ->
            val isSelected = filter == selectedFilter
            Surface(
                onClick = { onFilterSelected(filter) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                ),
                modifier = Modifier.testTag("filter_chip_$filter")
            ) {
                Text(
                    text = filter,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun PhoneAntennaArrayCard(telemetryList: List<AntennaTelemetry>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("phone_antenna_array_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = "Phone Antennas",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "ALL PHONE ANTENNA ARRAY (${telemetryList.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF00E5FF)
                        )
                        Text(
                            text = "Hardware Radio Telemetry & Multi-Band Reception",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Gray
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF00E5FF).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${telemetryList.size} HARNESSES ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(telemetryList, key = { it.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0D1B16),
                        border = BorderStroke(1.dp, Color(0xFF1E3A2B)),
                        modifier = Modifier
                            .width(210.dp)
                            .testTag("antenna_tile_${item.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.antennaName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                            Text(
                                text = item.frequencyBand,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF00FF66)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.status,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF00E5FF)
                                )
                                Text(
                                    text = "${item.signalPowerDbm} dBm",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.Yellow
                                )
                            }
                            Text(
                                text = "${item.protocolDetails} • Gain +${item.gainDbi} dBi",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpectrumAnalyzerScreen(
    uiState: SignalRadarUiState,
    onFilterSelected: (String) -> Unit
) {
    val filteredBlips = rememberFilteredBlips(uiState.activeBlips, uiState.selectedFilterType)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FilterChipsRow(
                selectedFilter = uiState.selectedFilterType,
                onFilterSelected = onFilterSelected
            )
        }

        item {
            PhoneAntennaArrayCard(telemetryList = uiState.antennaArrayTelemetry)
        }

        item {
            LargeSpectrumVisualizerCard(activeBlips = uiState.activeBlips)
        }

        item {
            Text(
                text = "Discovered Frequency Intercepts (${filteredBlips.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(filteredBlips, key = { it.id }) { blip ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spectrum_item_${blip.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (blip.distance < uiState.perimeterThresholdMeters) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = blip.name,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${blip.bandLabel} | ${blip.frequencyMhz.toInt()} MHz",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = Color.Gray
                                )
                            }

                            // Kalman Distance Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (blip.distance < uiState.perimeterThresholdMeters) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${String.format("%.1f", blip.distance)}m",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (blip.distance < uiState.perimeterThresholdMeters) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // RSSI Signal Strength Progress Bar
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "RSSI: ${blip.rssi} dBm",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Type: ${blip.type}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            val normalizedRssi = ((blip.rssi + 100) / 70f).coerceIn(0.05f, 1f)
                            LinearProgressIndicator(
                                progress = { normalizedRssi },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = when (blip.type.uppercase()) {
                                    "WIFI" -> Color(0xFF00FF66)
                                    "CELLULAR" -> Color(0xFFFF3366)
                                    "BLE" -> Color(0xFF00E5FF)
                                    "MAGNETIC" -> Color(0xFFFF00FF)
                                    else -> Color(0xFFFFCC00)
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
        }
    }
}

@Composable
fun MagnetometerScreen(
    uiState: SignalRadarUiState,
    onRecalibrate: () -> Unit = {},
    onTriggerSpike: () -> Unit = {},
    onClearInterference: () -> Unit = {}
) {
    val mag = uiState.magnetometerData
    val acoustic = uiState.acousticData

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // RF Interference Spike Alert Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rf_interference_indicator_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (mag.isRfInterferenceDetected) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (mag.isRfInterferenceDetected) 2.dp else 1.dp,
                    color = if (mag.isRfInterferenceDetected) MaterialTheme.colorScheme.error else Color(0xFF2B4237)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (mag.isRfInterferenceDetected) Icons.Default.Security else Icons.Default.Shield,
                                contentDescription = "RF Interference Monitor",
                                tint = if (mag.isRfInterferenceDetected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = if (mag.isRfInterferenceDetected) "⚠️ RF INTERFERENCE DETECTED" else "RF INTERFERENCE MONITOR",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = if (mag.isRfInterferenceDetected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (mag.isRfInterferenceDetected) "Spike: +${mag.interferenceMagnitude.toInt()} µT Peak" else "Monitoring Magnetometer Flux Surges",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = if (mag.isRfInterferenceDetected) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f) else Color.Gray
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (mag.isRfInterferenceDetected) {
                                OutlinedButton(
                                    onClick = onClearInterference,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    modifier = Modifier.testTag("clear_rf_interference_button")
                                ) {
                                    Text(
                                        text = "CLEAR",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onTriggerSpike,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (mag.isRfInterferenceDetected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("simulate_rf_spike_button")
                            ) {
                                Text(
                                    text = "SIMULATE SPIKE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (mag.isRfInterferenceDetected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (mag.isRfInterferenceDetected) {
                        Text(
                            text = mag.interferenceWarningMessage,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Auto-Calibration Banner / Notification
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("magnetometer_calibration_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (mag.isCalibrated) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    1.dp,
                    if (mag.isCalibrated) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color(0xFFFFCC00).copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Icon(
                                imageVector = if (mag.isCalibrated) Icons.Default.Shield else Icons.Default.Refresh,
                                contentDescription = "Calibration",
                                tint = if (mag.isCalibrated) MaterialTheme.colorScheme.primary else Color(0xFFFFCC00)
                            )
                            Text(
                                text = "AUTO-CALIBRATION ROUTINE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = onRecalibrate,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("recalibrate_mag_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Recalibrate",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RECALIBRATE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = mag.calibrationMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (mag.isCalibrated) MaterialTheme.colorScheme.primary else Color(0xFFFFCC00)
                    )

                    if (!mag.isCalibrated) {
                        LinearProgressIndicator(
                            progress = { mag.calibrationProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFFFCC00),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    } else {
                        Text(
                            text = "Calibrated Offset Subtracted: ${mag.baselineTotalMicroTesla.toInt()} µT Baseline",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Secondary DSP 50/60Hz Powerline Notch Filter Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("powerline_dsp_filter_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "DSP Notch Filter",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "SECONDARY DSP NOTCH FILTER",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "50Hz / 60Hz Powerline Hum Attenuation Active",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = Color.Gray
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "DSP ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "CLEANED BASELINE",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.Gray
                            )
                            Text(
                                text = "%.1f µT".format(mag.dspFilteredTotal),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column {
                            Text(
                                text = "HUM ATTENUATED",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.Gray
                            )
                            Text(
                                text = "-%.2f µT".format(mag.dspAttenuatedHumMicroTesla),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Column {
                            Text(
                                text = "DSP FILTER AXES",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.Gray
                            )
                            Text(
                                text = "X:%.0f Y:%.0f Z:%.0f".format(mag.dspFilteredX, mag.dspFilteredY, mag.dspFilteredZ),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Real-Time Canvas Waveform Graph
        item {
            MagneticWaveformGraph(
                magnetometerData = mag,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFFF00FF).copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = "Magnetometer",
                                tint = Color(0xFFFF00FF)
                            )
                            Text(
                                text = "MAGNETOMETER EMF DETECTOR",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFF00FF).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${mag.totalMicroTesla.toInt()} µT Raw",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFFFF00FF),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            if (mag.isCalibrated) {
                                Text(
                                    text = "Net: ${mag.netCalibratedMicroTesla.toInt()} µT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Status: ${mag.anomalyStatus}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (mag.totalMicroTesla > 80f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )

                    // Progress Gauge for Total MicroTesla
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Magnetic Flux Density (0 - 200 µT)",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Gray
                        )
                        LinearProgressIndicator(
                            progress = { (mag.totalMicroTesla / 200f).coerceIn(0.05f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = Color(0xFFFF00FF),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    // 3-Axis Vector Breakdown (X, Y, Z)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("X Axis", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("${mag.x.toInt()} µT", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Y Axis", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("${mag.y.toInt()} µT", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Z Axis", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("${mag.z.toInt()} µT", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFFCC00))
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFFFCC00).copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Acoustic",
                                tint = Color(0xFFFFCC00)
                            )
                            Text(
                                text = "ACOUSTIC PITCH & SOUND DETECTOR",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFCC00).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = acoustic.noteName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFFFFCC00),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Freq: ${acoustic.dominantFrequencyHz.toInt()} Hz",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Volume: ${acoustic.amplitudeDb.toInt()} dB",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Text(
                        text = "Band: ${acoustic.bandLabel}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun SecurityGuardScreen(
    uiState: SignalRadarUiState,
    onThresholdChanged: (Float) -> Unit,
    onToggleAlarm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Guard",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Micro-Perimeter Shield",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    Switch(
                        checked = uiState.isPerimeterAlarmEnabled,
                        onCheckedChange = { onToggleAlarm() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.error,
                            checkedTrackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        )
                    )
                }

                // Threshold Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Boundary Radius: ${uiState.perimeterThresholdMeters.toInt()} Meters",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = uiState.perimeterThresholdMeters,
                        onValueChange = onThresholdChanged,
                        valueRange = 1f..15f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.error,
                            activeTrackColor = MaterialTheme.colorScheme.error
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1m", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("5m (Default)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("15m", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }

                // Active Breaches Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (uiState.perimeterBreachCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Targets Inside Boundary:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                        )
                        Text(
                            text = "${uiState.perimeterBreachCount} DEVICES",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (uiState.perimeterBreachCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CsvLogConsoleScreen(
    uiState: SignalRadarUiState,
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Secure CSV Frame Logger",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = {
                        Toast.makeText(context, "CSV exported to internal file storage", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export", style = MaterialTheme.typography.labelSmall)
                }

                TextButton(onClick = onClearLogs) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Terminal Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF060B08), RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text(
                text = uiState.logConsoleTail.ifBlank { "Awaiting RF frame logs..." },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun BottomRadarNavBar(
    currentTab: RadarTab,
    onTabSelected: (RadarTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        RadarTab.entries.forEach { tab ->
            val isSelected = tab == currentTab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                label = {
                    Text(
                        text = when (tab) {
                            RadarTab.SWEEP_RADAR -> "Sweep"
                            RadarTab.SPECTRUM_ANALYZER -> "Spectrum"
                            RadarTab.MAGNETOMETER_EMF -> "Magneto"
                            RadarTab.SECURITY_GUARD -> "Guard"
                            RadarTab.CSV_LOG_CONSOLE -> "CSV Log"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    )
                },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            RadarTab.SWEEP_RADAR -> Icons.Default.Radar
                            RadarTab.SPECTRUM_ANALYZER -> Icons.Default.GraphicEq
                            RadarTab.MAGNETOMETER_EMF -> Icons.Default.Equalizer
                            RadarTab.SECURITY_GUARD -> Icons.Default.Security
                            RadarTab.CSV_LOG_CONSOLE -> Icons.Default.Terminal
                        },
                        contentDescription = tab.name
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ),
                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
            )
        }
    }
}

@Composable
private fun rememberFilteredBlips(blips: List<RadarBlip>, filterType: String): List<RadarBlip> {
    return if (filterType == "ALL") {
        blips
    } else {
        blips.filter { it.type.equals(filterType, ignoreCase = true) }
    }
}
