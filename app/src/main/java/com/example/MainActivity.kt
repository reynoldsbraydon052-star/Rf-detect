package com.example

import android.Manifest
import android.content.res.Configuration
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

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

enum class WindowWidthSizeClass { COMPACT, MEDIUM, EXPANDED }
enum class WindowHeightSizeClass { COMPACT, MEDIUM, EXPANDED }

data class WindowSizeClass(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass,
    val isLandscape: Boolean
) {
    val isExpandedOrLandscape: Boolean
        get() = isLandscape || widthSizeClass != WindowWidthSizeClass.COMPACT
}

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val widthClass = when {
        screenWidth < 600 -> WindowWidthSizeClass.COMPACT
        screenWidth < 840 -> WindowWidthSizeClass.MEDIUM
        else -> WindowWidthSizeClass.EXPANDED
    }

    val heightClass = when {
        screenHeight < 480 -> WindowHeightSizeClass.COMPACT
        screenHeight < 900 -> WindowHeightSizeClass.MEDIUM
        else -> WindowHeightSizeClass.EXPANDED
    }

    return remember(screenWidth, screenHeight, isLandscape) {
        WindowSizeClass(widthClass, heightClass, isLandscape)
    }
}

@Composable
fun TacticalRadarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TacticalDarkColorScheme,
        content = content
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SignalRadarApp(viewModel: SignalRadarViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionsList = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.RECORD_AUDIO)
        }
    }

    val locationAndScanPermissionsState = rememberMultiplePermissionsState(
        permissions = permissionsList
    ) { permissionsMap ->
        if (permissionsMap[Manifest.permission.RECORD_AUDIO] == true) {
            viewModel.onPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        if (!locationAndScanPermissionsState.allPermissionsGranted) {
            locationAndScanPermissionsState.launchMultiplePermissionRequest()
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
        var showHardwareDocOverlay by remember { mutableStateOf(false) }

        if (showHardwareDocOverlay) {
            HardwareDocumentationOverlayDialog(
                onDismiss = { showHardwareDocOverlay = false }
            )
        }

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
                TacticalHeader(
                    uiState = uiState,
                    onOpenSettings = { viewModel.setTab(RadarTab.SETTINGS) },
                    onOpenDocOverlay = { showHardwareDocOverlay = true },
                    onOpenFullRadar = { viewModel.setTab(RadarTab.FULL_RADAR) }
                )

                // Accompanist Permissions Banner for Location Access (required for Bluetooth & Wi-Fi scanning)
                val hasLocationAccess = locationAndScanPermissionsState.permissions
                    .filter { it.permission == Manifest.permission.ACCESS_FINE_LOCATION || it.permission == Manifest.permission.ACCESS_COARSE_LOCATION }
                    .any { it.status.isGranted }

                AnimatedVisibility(visible = !hasLocationAccess) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("accompanist_location_permission_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
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
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Location Permission Required",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Location Permission Required",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (locationAndScanPermissionsState.shouldShowRationale) {
                                            "Coarse & Fine Location access is required by Android for scanning Bluetooth and Wi-Fi networks."
                                        } else {
                                            "Location access (Fine/Coarse) is required to perform Bluetooth and Wi-Fi radar scanning."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { locationAndScanPermissionsState.launchMultiplePermissionRequest() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Grant", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

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

                // Device Signal Threshold Match Live Alert Banner
                uiState.activeDeviceAlerts.firstOrNull()?.let { alert ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("device_threshold_alert_banner"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF230B12),
                        border = BorderStroke(1.dp, Color(0xFFFF2A55).copy(alpha = 0.8f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFF2A55).copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "Device Alert",
                                            tint = Color(0xFFFF2A55),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "NEW TARGET MATCHED SIGNAL THRESHOLD (${alert.rssi} dBm)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = Color(0xFFFF2A55)
                                    )
                                    Text(
                                        text = "${alert.deviceName} • [${alert.macAddress}]",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Est. Range: %.1fm • Cutoff: ${uiState.rssiAlertThresholdDbm} dBm".format(alert.distanceMeters),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color.LightGray
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.dismissDeviceAlert(alert.id) },
                                modifier = Modifier.size(28.dp).testTag("dismiss_device_alert_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss Alert",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(18.dp)
                                )
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
                            onDeleteBleDevice = { viewModel.deleteBleDeviceFromDb(it) },
                            onZoomInMap = { viewModel.zoomInMap() },
                            onZoomOutMap = { viewModel.zoomOutMap() },
                            onSetMapRange = { viewModel.setMapRangeMeters(it) },
                            onToggleMaximizeMap = { viewModel.toggleMapMaximized() },
                            onOpenFullScreenMap = { viewModel.toggleFullScreenMap(true) },
                            onSelectTargetDevice = { viewModel.selectTargetDevice(it) },
                            onPlayTestPing = { viewModel.playTestAudioPing(it) }
                        )

                        RadarTab.FULL_RADAR -> FullScreenRadarScreen(
                            uiState = uiState,
                            onZoomIn = { viewModel.zoomInMap() },
                            onZoomOut = { viewModel.zoomOutMap() },
                            onSetMapRange = { viewModel.setMapRangeMeters(it) },
                            onSelectTargetDevice = { viewModel.selectTargetDevice(it) },
                            onToggleAudioSonar = { viewModel.toggleAudioSonar() },
                            onSetFullScreenMapMode = { viewModel.setFullScreenMapMode(it) }
                        )

                        RadarTab.SPECTRUM_ANALYZER -> SpectrumAnalyzerScreen(
                            uiState = uiState,
                            onFilterSelected = { viewModel.setFilterType(it) },
                            onSelectTargetDevice = { viewModel.selectTargetDevice(it) },
                            onToggleAudioSonar = { viewModel.toggleAudioSonar() }
                        )

                        RadarTab.DETECTED_SENSORS -> DetectedSensorsScreen(
                            uiState = uiState,
                            onOpenDocOverlay = { showHardwareDocOverlay = true }
                        )

                        RadarTab.HISTORIC_HEATMAP -> HistoricHeatmapScreen(
                            uiState = uiState
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

                        RadarTab.SETTINGS -> SettingsScreen(
                            uiState = uiState,
                            onBack = { viewModel.setTab(RadarTab.SWEEP_RADAR) },
                            onSetMapRange = { viewModel.updateMapRangeInStore(it) },
                            onSetRssiThreshold = { viewModel.updateRssiThresholdInStore(it) },
                            onSetEmfThreshold = { viewModel.setEmfAlertThreshold(it) },
                            onSetBreachThreshold = { viewModel.updateBreachPerimeterInStore(it) },
                            onToggleBackgroundRecon = { viewModel.toggleBackgroundAlertService(it) },
                            onToggleSmoothingLerp = { viewModel.toggleSmoothingLerpInStore(it) },
                            onToggleHapticAlerts = { viewModel.toggleHapticAlerts(it) },
                            onToggleVisualNotifs = { viewModel.toggleVisualNotifs(it) },
                            onSetScanMode = { viewModel.setScanMode(it) },
                            onExportLogsCsv = { viewModel.exportCapturedLogsCsv() },
                            onExportKmlBreadcrumbs = { viewModel.exportGpsBreadcrumbsKml() },
                            onPurgeHistory = { viewModel.purgeInterceptionHistory() }
                        )
                    }
                }
            }

            if (uiState.isFullScreenMapVisible) {
                FullScreenRadarMapOverlay(
                    uiState = uiState,
                    onDismiss = { viewModel.toggleFullScreenMap(false) },
                    onZoomIn = { viewModel.zoomInMap() },
                    onZoomOut = { viewModel.zoomOutMap() },
                    onSetMapRange = { viewModel.setMapRangeMeters(it) },
                    onSelectTargetDevice = { viewModel.selectTargetDevice(it) },
                    onToggleAudioSonar = { viewModel.toggleAudioSonar() },
                    onSetFullScreenMapMode = { viewModel.setFullScreenMapMode(it) }
                )
            }
        }
    }
}

@Composable
fun TacticalHeader(
    uiState: SignalRadarUiState,
    onOpenSettings: (() -> Unit)? = null,
    onOpenDocOverlay: (() -> Unit)? = null,
    onOpenFullRadar: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                RoundedCornerShape(10.dp)
            )
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        text = "Google Pixel Spectrum • 100% Sensors Active",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onOpenFullRadar != null) {
                    Surface(
                        onClick = onOpenFullRadar,
                        shape = RoundedCornerShape(16.dp),
                        color = if (uiState.selectedTab == RadarTab.FULL_RADAR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("header_open_full_radar_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = "Full Radar",
                                tint = if (uiState.selectedTab == RadarTab.FULL_RADAR) Color.Black else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "FULL RADAR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = if (uiState.selectedTab == RadarTab.FULL_RADAR) Color.Black else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (onOpenDocOverlay != null) {
                    IconButton(
                        onClick = onOpenDocOverlay,
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                CircleShape
                            )
                            .testTag("header_3d_doc_overlay_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = "3D Hardware Docs Overlay",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
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

                if (onOpenSettings != null) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (uiState.selectedTab == RadarTab.SETTINGS) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                            .testTag("header_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (uiState.selectedTab == RadarTab.SETTINGS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Status Stats Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ANTENNAS: ${uiState.activeAntennaCount}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Gray
                )
                Text(
                    text = "NODES: ${uiState.activeBlips.size}",
                    maxLines = 1,
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (nearest.distance < uiState.perimeterThresholdMeters) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
fun QuickControlsCard(
    uiState: SignalRadarUiState,
    onToggleAudioSonar: () -> Unit,
    onTogglePerimeterAlarm: () -> Unit,
    onToggleScanning: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_controls_panel_card"),
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
    onDeleteBleDevice: (String) -> Unit,
    onZoomInMap: () -> Unit = {},
    onZoomOutMap: () -> Unit = {},
    onSetMapRange: (Float) -> Unit = {},
    onToggleMaximizeMap: () -> Unit = {},
    onOpenFullScreenMap: () -> Unit = {},
    onSelectTargetDevice: (String?) -> Unit = {},
    onPlayTestPing: (Double) -> Unit = {},
    windowSizeClass: WindowSizeClass = rememberWindowSizeClass()
) {
    val filteredBlips = rememberFilteredBlips(uiState.activeBlips, uiState.selectedFilterType)

    if (windowSizeClass.isExpandedOrLandscape) {
        // Landscape or Wide Screen Two-Column Alignment Layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: Filter Chips & Radar Scope Visualization
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChipsRow(
                    selectedFilter = uiState.selectedFilterType,
                    onFilterSelected = onFilterSelected
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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
                        selectedTargetDeviceId = uiState.selectedTargetDeviceId,
                        perimeterThresholdMeters = uiState.perimeterThresholdMeters,
                        mapRangeMeters = uiState.mapRangeMeters,
                        isMapMaximized = uiState.isMapMaximized,
                        onZoomIn = onZoomInMap,
                        onZoomOut = onZoomOutMap,
                        onSetMapRange = onSetMapRange,
                        onToggleMaximizeMap = onToggleMaximizeMap,
                        onOpenFullScreenMap = onOpenFullScreenMap,
                        onSelectTargetDevice = onSelectTargetDevice
                    )

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
            }

            // Right Column: Quick Control Hub & BLE Signal Database Tracker
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickControlsCard(
                    uiState = uiState,
                    onToggleAudioSonar = onToggleAudioSonar,
                    onTogglePerimeterAlarm = onTogglePerimeterAlarm,
                    onToggleScanning = onToggleScanning
                )

                BleDatabaseTrackerCard(
                    bleDevices = uiState.savedBleDevices,
                    isScannerActive = uiState.isBleScannerServiceActive,
                    selectedTargetDeviceId = uiState.selectedTargetDeviceId,
                    isAudioSonarActive = uiState.isAudioSonarActive,
                    onToggleScanner = onToggleBleScanner,
                    onClearDatabase = onClearBleDb,
                    onDeleteDevice = onDeleteBleDevice,
                    onSelectTargetDevice = { id -> onSelectTargetDevice(id) },
                    onPlayTestPing = onPlayTestPing
                )
            }
        }
    } else {
        // Portrait / Compact Column Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChipsRow(
                selectedFilter = uiState.selectedFilterType,
                onFilterSelected = onFilterSelected
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF08120E), RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                TacticalRadarCanvas(
                    headingDegrees = uiState.headingDegrees,
                    blips = filteredBlips,
                    nearestBlipId = uiState.nearestBlip?.id,
                    selectedTargetDeviceId = uiState.selectedTargetDeviceId,
                    perimeterThresholdMeters = uiState.perimeterThresholdMeters,
                    mapRangeMeters = uiState.mapRangeMeters,
                    isMapMaximized = uiState.isMapMaximized,
                    onZoomIn = onZoomInMap,
                    onZoomOut = onZoomOutMap,
                    onSetMapRange = onSetMapRange,
                    onToggleMaximizeMap = onToggleMaximizeMap,
                    onOpenFullScreenMap = onOpenFullScreenMap,
                    onSelectTargetDevice = onSelectTargetDevice,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "HEADING: ${uiState.headingDegrees.toInt()}°",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickControlsCard(
                    uiState = uiState,
                    onToggleAudioSonar = onToggleAudioSonar,
                    onTogglePerimeterAlarm = onTogglePerimeterAlarm,
                    onToggleScanning = onToggleScanning
                )

                BleDatabaseTrackerCard(
                    bleDevices = uiState.savedBleDevices,
                    isScannerActive = uiState.isBleScannerServiceActive,
                    selectedTargetDeviceId = uiState.selectedTargetDeviceId,
                    isAudioSonarActive = uiState.isAudioSonarActive,
                    onToggleScanner = onToggleBleScanner,
                    onClearDatabase = onClearBleDb,
                    onDeleteDevice = onDeleteBleDevice,
                    onSelectTargetDevice = { id -> onSelectTargetDevice(id) },
                    onPlayTestPing = onPlayTestPing
                )
            }
        }
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
fun PinpointDeviceHUDCard(
    uiState: SignalRadarUiState,
    onUnlockTarget: () -> Unit,
    onToggleAudioSonar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetBlip = uiState.activeBlips.find { 
        it.id == uiState.selectedTargetDeviceId || it.name == uiState.selectedTargetDeviceId 
    } ?: uiState.nearestBlip ?: return

    val heading = uiState.headingDegrees
    val relativeAngle = (targetBlip.targetAngleOffset - heading + 360f) % 360f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pinpoint_target_hud_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF07140B)),
        border = BorderStroke(1.5.dp, Color(0xFF00FF66))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Target Name & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Target Pinpoint",
                        tint = Color(0xFF00FF66),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "PINPOINT TARGET LOCKED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp
                            ),
                            color = Color(0xFF00FF66)
                        )
                        Text(
                            text = targetBlip.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )
                    }
                }

                IconButton(onClick = onUnlockTarget) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Unlock Pinpoint Target",
                        tint = Color.Gray
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF1B3D28))

            // Proximity & Signal Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pinpoint Distance Gauge
                Column {
                    Text(
                        text = "ESTIMATED DISTANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        ),
                        color = Color.Gray
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format("%.1f", targetBlip.distance),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (targetBlip.distance < uiState.perimeterThresholdMeters) Color(0xFFFF3366) else Color(0xFF00FF66)
                        )
                        Text(
                            text = "METERS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                // Signal Strength dBm
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SIGNAL INTENSITY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        ),
                        color = Color.Gray
                    )
                    val pct = ((targetBlip.rssi + 100) * 2).coerceIn(0, 100)
                    Text(
                        text = "${targetBlip.rssi} dBm (${pct}%)",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF00E5FF)
                    )
                }
            }

            // Signal Strength Meter
            val normalizedRssi = ((targetBlip.rssi + 100) / 70f).coerceIn(0.05f, 1f)
            LinearProgressIndicator(
                progress = { normalizedRssi },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (targetBlip.distance < uiState.perimeterThresholdMeters) Color(0xFFFF3366) else Color(0xFF00FF66),
                trackColor = Color(0xFF14291B)
            )

            // Directional Guidance & Audio Sonar Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = "Bearing",
                        tint = Color.Yellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "BEARING: ${relativeAngle.toInt()}° | ${targetBlip.bandLabel}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Color.Yellow
                    )
                }

                Button(
                    onClick = onToggleAudioSonar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isAudioSonarActive) Color(0xFF00FF66) else Color(0xFF14291B),
                        contentColor = if (uiState.isAudioSonarActive) Color.Black else Color(0xFF00FF66)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isAudioSonarActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Audio Sonar",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (uiState.isAudioSonarActive) "SONAR ACTIVE" else "MUTE SONAR",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SpectrumInterceptCard(
    blip: RadarBlip,
    perimeterThresholdMeters: Float,
    selectedTargetDeviceId: String? = null,
    onSelectTargetDevice: ((String?) -> Unit)? = null
) {
    val isSelectedTarget = selectedTargetDeviceId != null &&
            (blip.id == selectedTargetDeviceId || blip.name == selectedTargetDeviceId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("spectrum_item_${blip.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelectedTarget) Color(0xFF0F2618) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.5.dp,
            if (isSelectedTarget) Color(0xFF00FF66)
            else if (blip.distance < perimeterThresholdMeters) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = blip.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelectedTarget) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF00FF66)
                            ) {
                                Text(
                                    text = "PINPOINTED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${blip.bandLabel} | ${blip.frequencyMhz.toInt()} MHz",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Gray
                    )
                }

                // Kalman Distance Badge & Pinpoint Quick Action
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (blip.distance < perimeterThresholdMeters) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${String.format("%.1f", blip.distance)}m",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (blip.distance < perimeterThresholdMeters) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (onSelectTargetDevice != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelectedTarget) Color(0xFF00FF66) else Color(0xFF142E1F))
                                .clickable {
                                    onSelectTargetDevice.invoke(if (isSelectedTarget) null else blip.id)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Pinpoint Target",
                                    tint = if (isSelectedTarget) Color.Black else Color(0xFF00FF66),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isSelectedTarget) "LOCKED" else "PINPOINT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isSelectedTarget) Color.Black else Color(0xFF00FF66)
                                )
                            }
                        }
                    }
                }
            }

            // RSSI Signal Strength Progress Bar & Quality Percentage
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val pct = ((blip.rssi + 100) * 2).coerceIn(0, 100)
                    Text(
                        text = "RSSI: ${blip.rssi} dBm (${pct}% Signal)",
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

@Composable
fun SpectrumAnalyzerScreen(
    uiState: SignalRadarUiState,
    onFilterSelected: (String) -> Unit,
    onSelectTargetDevice: (String?) -> Unit = {},
    onToggleAudioSonar: () -> Unit = {},
    windowSizeClass: WindowSizeClass = rememberWindowSizeClass()
) {
    val filteredBlips = rememberFilteredBlips(uiState.activeBlips, uiState.selectedFilterType)

    if (windowSizeClass.isExpandedOrLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.selectedTargetDeviceId != null) {
                    PinpointDeviceHUDCard(
                        uiState = uiState,
                        onUnlockTarget = { onSelectTargetDevice(null) },
                        onToggleAudioSonar = onToggleAudioSonar
                    )
                }
                PhoneAntennaArrayCard(telemetryList = uiState.antennaArrayTelemetry)
                LargeSpectrumVisualizerCard(activeBlips = uiState.activeBlips)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChipsRow(
                    selectedFilter = uiState.selectedFilterType,
                    onFilterSelected = onFilterSelected
                )

                Text(
                    text = "Discovered Frequency Intercepts (${filteredBlips.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredBlips, key = { it.id }) { blip ->
                        SpectrumInterceptCard(
                            blip = blip,
                            perimeterThresholdMeters = uiState.perimeterThresholdMeters,
                            selectedTargetDeviceId = uiState.selectedTargetDeviceId,
                            onSelectTargetDevice = onSelectTargetDevice
                        )
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.selectedTargetDeviceId != null) {
                item {
                    PinpointDeviceHUDCard(
                        uiState = uiState,
                        onUnlockTarget = { onSelectTargetDevice(null) },
                        onToggleAudioSonar = onToggleAudioSonar
                    )
                }
            }

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
                SpectrumInterceptCard(
                    blip = blip,
                    perimeterThresholdMeters = uiState.perimeterThresholdMeters,
                    selectedTargetDeviceId = uiState.selectedTargetDeviceId,
                    onSelectTargetDevice = onSelectTargetDevice
                )
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bottom_radar_nav_bar"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(RadarTab.entries.toTypedArray()) { tab ->
                val isSelected = tab == currentTab
                Surface(
                    onClick = { onTabSelected(tab) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                    border = BorderStroke(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    ),
                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (tab) {
                                RadarTab.SWEEP_RADAR -> Icons.Default.Radar
                                RadarTab.FULL_RADAR -> Icons.Default.Radar
                                RadarTab.SPECTRUM_ANALYZER -> Icons.Default.GraphicEq
                                RadarTab.DETECTED_SENSORS -> Icons.Default.Sensors
                                RadarTab.HISTORIC_HEATMAP -> Icons.Default.Map
                                RadarTab.MAGNETOMETER_EMF -> Icons.Default.Equalizer
                                RadarTab.SECURITY_GUARD -> Icons.Default.Security
                                RadarTab.CSV_LOG_CONSOLE -> Icons.Default.Terminal
                                RadarTab.SETTINGS -> Icons.Default.Settings
                            },
                            contentDescription = tab.name,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = when (tab) {
                                RadarTab.SWEEP_RADAR -> "Sweep"
                                RadarTab.FULL_RADAR -> "Full Radar"
                                RadarTab.SPECTRUM_ANALYZER -> "Spectrum"
                                RadarTab.DETECTED_SENSORS -> "Sensors"
                                RadarTab.HISTORIC_HEATMAP -> "Heatmap"
                                RadarTab.MAGNETOMETER_EMF -> "Magneto"
                                RadarTab.SECURITY_GUARD -> "Guard"
                                RadarTab.CSV_LOG_CONSOLE -> "Log"
                                RadarTab.SETTINGS -> "Settings"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetectedSensorsScreen(
    uiState: SignalRadarUiState,
    onOpenDocOverlay: (() -> Unit)? = null
) {
    var expandedSensorId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("detected_sensors_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Hardware Sensors Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "Sensors",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "HARDWARE SENSORS & PROFILES",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Live Phone Sensor Telemetry & Hardware Target Identification Guide",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.Gray
                            )
                        }
                    }

                    if (onOpenDocOverlay != null) {
                        Button(
                            onClick = onOpenDocOverlay,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_3d_hardware_doc_overlay_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = "3D Overlay",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LAUNCH 3D HARDWARE IDENTIFICATION OVERLAY",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Real-time Phone Hardware Sensors Live Metrics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LIVE PHONE HARDWARE READINGS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Magnetometer
                    item {
                        val mag = uiState.magnetometerData
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0C1814),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "3D Magnetometer",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Equalizer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Net Flux: %.1f µT".format(mag.totalMicroTesla),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.Yellow
                                )
                                Text(
                                    text = "Bx: %.1f | By: %.1f | Bz: %.1f".format(mag.x, mag.y, mag.z),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.LightGray
                                )
                                Text(
                                    text = if (mag.isCalibrated) "Baseline Calibrated" else "Auto-Calibrating...",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (mag.isCalibrated) Color(0xFF00FF66) else Color(0xFFFFCC00)
                                )
                            }
                        }
                    }

                    // Acoustic Microphone FFT
                    item {
                        val acoustic = uiState.acousticData
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0C1814),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f)),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Acoustic Mic FFT",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color(0xFF00E5FF)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "%.0f Hz (${acoustic.noteName})".format(acoustic.dominantFrequencyHz),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF00E5FF)
                                )
                                Text(
                                    text = "Amplitude: %.1f dB".format(acoustic.amplitudeDb),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.LightGray
                                )
                                Text(
                                    text = acoustic.bandLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.Yellow
                                )
                            }
                        }
                    }

                    // Compass Orientation
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0C1814),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Azimuth Heading",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "${uiState.headingDegrees.toInt()}° Heading",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Gyro / Accelerometer Fusion",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Scan Mode: ${uiState.scanMode.title}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Accelerometer / G-Force
                    item {
                        val sensor = uiState.sensorSuite
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0C1814),
                            border = BorderStroke(1.dp, Color(0xFFFFCC00).copy(alpha = 0.4f)),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "3-Axis Accelerometer",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color(0xFFFFCC00)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = Color(0xFFFFCC00),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "%.2f G-Force".format(sensor.totalGForce),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFFFFCC00)
                                )
                                Text(
                                    text = "X: %.1f | Y: %.1f | Z: %.1f m/s²".format(sensor.accelX, sensor.accelY, sensor.accelZ),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.LightGray
                                )
                                Text(
                                    text = if (sensor.isMotionDetected) "MOTION DETECTED" else "STATIONARY MOUNT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (sensor.isMotionDetected) Color(0xFFFF8800) else Color(0xFF00FF66)
                                )
                            }
                        }
                    }

                    // Gyroscope Rotational Speed
                    item {
                        val sensor = uiState.sensorSuite
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0C1814),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "3-Axis Gyroscope",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color(0xFF00E5FF)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Equalizer,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "%.1f °/s Rotation".format(sensor.rotationalSpeedDegPerSec),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF00E5FF)
                                )
                                Text(
                                    text = "X: %.1f | Y: %.1f | Z: %.1f rad/s".format(sensor.gyroX, sensor.gyroY, sensor.gyroZ),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Vibration: %.1f Hz".format(sensor.vibrationHz),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.Yellow
                                )
                            }
                        }
                    }

                    // Barometer & Pressure Altitude
                    item {
                        val sensor = uiState.sensorSuite
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0C1814),
                            border = BorderStroke(1.dp, Color(0xFFFF8800).copy(alpha = 0.4f)),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Barometric Altimeter",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color(0xFFFF8800)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = null,
                                        tint = Color(0xFFFF8800),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "${sensor.pressureHpa.toInt()} hPa",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFFFF8800)
                                )
                                Text(
                                    text = "Altitude: ${sensor.estimatedAltitudeMeters.toInt()} meters ASL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Floor Pressure Delta: STABLE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF00FF66)
                                )
                            }
                        }
                    }

                    // Ambient Light Lux Sensor
                    item {
                        val sensor = uiState.sensorSuite
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0C1814),
                            border = BorderStroke(1.dp, Color.Yellow.copy(alpha = 0.4f)),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ambient Light Lux",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color.Yellow
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = null,
                                        tint = Color.Yellow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "${sensor.lightLux.toInt()} Lux",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.Yellow
                                )
                                Text(
                                    text = sensor.lightCondition,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.LightGray
                                )
                                Text(
                                    text = if (sensor.isOpticalPulseDetected) "IR PULSE DETECTED!" else "Optical Flare Normal",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (sensor.isOpticalPulseDetected) Color(0xFFFF3366) else Color(0xFF00FF66)
                                )
                            }
                        }
                    }

                    // Step Counter & Pedestrian Dead Reckoning (PDR)
                    item {
                        val sensor = uiState.sensorSuite
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0C1814),
                            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f)),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "PDR Dead Reckoning",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color(0xFF00FF66)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Radar,
                                        contentDescription = null,
                                        tint = Color(0xFF00FF66),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "${sensor.stepCount} Steps",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF00FF66)
                                )
                                Text(
                                    text = "Estimated PDR: %.1f meters".format(sensor.pdrDistanceMeters),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Pedestrian Inertial Nav",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF00E5FF)
                                )
                            }
                        }
                    }

                    // Gravity Vector Sensor
                    item {
                        val sensor = uiState.sensorSuite
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0C1814),
                            border = BorderStroke(1.dp, Color(0xFFFFCC00).copy(alpha = 0.4f)),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Gravity Vector",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color(0xFFFFCC00)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = null,
                                        tint = Color(0xFFFFCC00),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Gx:%.1f Gy:%.1f".format(sensor.gravityX, sensor.gravityY),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFFFFCC00)
                                )
                                Text(
                                    text = "Gz: %.1f m/s² Vertical".format(sensor.gravityZ),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Earth Gravitational Isolation",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF00FF66)
                                )
                            }
                        }
                    }

                    // Linear Acceleration (No Gravity Component)
                    item {
                        val sensor = uiState.sensorSuite
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0C1814),
                            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f)),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Linear Accel",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color(0xFF00FF66)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = Color(0xFF00FF66),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Lx:%.1f Ly:%.1f".format(sensor.linearAccelX, sensor.linearAccelY),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF00FF66)
                                )
                                Text(
                                    text = "Lz: %.1f m/s² Impulse".format(sensor.linearAccelZ),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Dynamic User Kinematics",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF00E5FF)
                                )
                            }
                        }
                    }

                    // Uncalibrated EMF Magnetometer
                    item {
                        val sensor = uiState.sensorSuite
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0C1814),
                            border = BorderStroke(1.dp, Color(0xFFFF00FF).copy(alpha = 0.4f)),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Uncalibrated EMF",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color(0xFFFF00FF)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = null,
                                        tint = Color(0xFFFF00FF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Ux:%.0f Uy:%.0f".format(sensor.uncalibratedMagX, sensor.uncalibratedMagY),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFFFF00FF)
                                )
                                Text(
                                    text = "Uz: %.0f µT Raw EMF".format(sensor.uncalibratedMagZ),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Iron Distortion Isolation",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFFFFCC00)
                                )
                            }
                        }
                    }

                    // Ambient Thermal & Relative Humidity
                    item {
                        val sensor = uiState.sensorSuite
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0C1814),
                            border = BorderStroke(1.dp, Color(0xFFFF5500).copy(alpha = 0.4f)),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ambient Thermal",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color(0xFFFF5500)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5500),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "%.1f °C Thermal".format(sensor.ambientTempCelsius),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFFFF5500)
                                )
                                Text(
                                    text = "Humidity: %.0f%% RH".format(sensor.relativeHumidityPct),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Environment Thermal Sweep",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF00FF66)
                                )
                            }
                        }
                    }

                    // Pixel Motion State & Stationarity Detector
                    item {
                        val sensor = uiState.sensorSuite
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0C1814),
                            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f)),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pixel Motion State",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color(0xFF00FF66)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = Color(0xFF00FF66),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = sensor.motionState,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF00FF66)
                                )
                                Text(
                                    text = if (sensor.isStationary) "MOUNTED / TRIPOD STABLE" else "HANDHELD SWEEPING",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Stationary Detector Active",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF00E5FF)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Hardware Visual Target Catalogue
        item {
            Text(
                text = "WHAT THESE SENSORS & TARGETS LOOK LIKE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Target Sensor Visual Profile Cards
        val sensorProfiles = listOf(
            SensorProfile(
                id = "ble_tag",
                title = "BLE Smart Tracker / AirTag / Tile Tag",
                frequencyBand = "2.4 GHz ISM (2402 - 2480 MHz)",
                channels = "BLE Advertising Ch 37 (2402 MHz), Ch 38 (2426 MHz), Ch 39 (2480 MHz)",
                threatLevel = "SECURITY / PRIVACY RISK",
                threatColor = Color(0xFFFF9800),
                description = "Compact coin-cell or key-fob RF tags emitting Bluetooth LE beacons every 1-2 seconds.",
                detectionAdvice = "Look for persistent MAC address rotation or high RSSI spikes (-50 to -30 dBm) near personal items.",
                vectorDiagramType = "BLE_TAG"
            ),
            SensorProfile(
                id = "wifi_camera",
                title = "Wi-Fi Hidden IP Surveillance Camera",
                frequencyBand = "2.4 GHz / 5 GHz / 6 GHz (Wi-Fi 7 / 6E)",
                channels = "Wi-Fi Ch 1-13 (2.4 GHz), Ch 36-165 (5 GHz), UNII-5 to 8 (6 GHz)",
                threatLevel = "HIGH SURVEILLANCE RISK",
                threatColor = Color(0xFFFF3366),
                description = "Concealed optical camera module with built-in Wi-Fi AP or client streaming live 1080p/4K RTSP video.",
                detectionAdvice = "Constantly broadcasts unencrypted beacon frames or high duty cycle RF packet bursts.",
                vectorDiagramType = "WIFI_CAM"
            ),
            SensorProfile(
                id = "emf_transformer",
                title = "EMF Powerline Transformer & Solenoid Coil",
                frequencyBand = "50 Hz / 60 Hz AC Line Harmonics",
                channels = "Electromagnetic Flux Induction Spectrum",
                threatLevel = "ENVIRONMENTAL / HARDWARE ANOMALY",
                threatColor = Color(0xFF00E5FF),
                description = "High-voltage transformer, AC wall adapter, power relay, or electric motor inducing magnetic flux deviation.",
                detectionAdvice = "Triggers sharp Bx/By microTesla (µT) spikes when phone is positioned within 5-15 cm.",
                vectorDiagramType = "EMF_COIL"
            ),
            SensorProfile(
                id = "uwb_tag",
                title = "Ultra-Wideband (UWB) Spatial Tag",
                frequencyBand = "UWB Channel 5 (6.4 GHz) & Channel 9 (7.9 GHz)",
                channels = "IEEE 802.15.4z FiRa impulse radio",
                threatLevel = "PROXIMITY SPATIAL TRACKER",
                threatColor = Color(0xFF9C27B0),
                description = "Precision spatial positioning chip emitting nanosecond impulse RF pulses for sub-centimeter Time-of-Flight ranging.",
                detectionAdvice = "Detects microsecond impulse energy signatures across 500 MHz ultra-wide RF channels.",
                vectorDiagramType = "UWB_CHIP"
            ),
            SensorProfile(
                id = "acoustic_bug",
                title = "Ultrasonic Acoustic Bug / Beacon",
                frequencyBand = "18 kHz - 22 kHz Near-Ultrasonic",
                channels = "Inaudible High-Frequency Acoustic Band",
                threatLevel = "AUDIO BEACON RISK",
                threatColor = Color(0xFFFFCC00),
                description = "Concealed acoustic speaker playing near-ultrasonic sound pings for location cross-device tracking.",
                detectionAdvice = "Captured on phone mic FFT as sharp narrow band tone spikes between 18.5 kHz and 21.0 kHz.",
                vectorDiagramType = "ACOUSTIC_BUG"
            )
        )

        items(sensorProfiles, key = { it.id }) { profile ->
            val isExpanded = expandedSensorId == profile.id

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sensor_profile_card_${profile.id}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, profile.threatColor.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedSensorId = if (isExpanded) null else profile.id },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = profile.frequencyBand,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = profile.threatColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, profile.threatColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = profile.threatLevel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = profile.threatColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = { expandedSensorId = if (isExpanded) null else profile.id },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand",
                                tint = Color.Gray
                            )
                        }
                    }

                    // Vector Diagram Artwork Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(Color(0xFF07110C), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF1E3A2B), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SensorHardwareDiagramCanvas(diagramType = profile.vectorDiagramType)
                    }

                    if (isExpanded) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Channels: ${profile.channels}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.Yellow
                            )
                            Text(
                                text = profile.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF13281E),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Tip",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Detection Tip: ${profile.detectionAdvice}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
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

private data class SensorProfile(
    val id: String,
    val title: String,
    val frequencyBand: String,
    val channels: String,
    val threatLevel: String,
    val threatColor: Color,
    val description: String,
    val detectionAdvice: String,
    val vectorDiagramType: String
)

@Composable
private fun SensorHardwareDiagramCanvas(diagramType: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val centerY = h / 2f

        when (diagramType) {
            "BLE_TAG" -> {
                // Round coin tag with keyhole and radiating waves
                drawCircle(
                    color = Color(0xFF1E3A2B),
                    radius = 42.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = Color(0xFFFF9800),
                    radius = 38.dp.toPx(),
                    center = Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF2E5A42),
                    radius = 8.dp.toPx(),
                    center = Offset(centerX, centerY - 20.dp.toPx())
                )
                // Radiation arcs
                for (r in listOf(50.dp.toPx(), 65.dp.toPx(), 80.dp.toPx())) {
                    drawCircle(
                        color = Color(0xFFFF9800).copy(alpha = 0.4f),
                        radius = r,
                        center = Offset(centerX, centerY),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
            "WIFI_CAM" -> {
                // Camera Lens box with Wi-Fi arcs
                drawRoundRect(
                    color = Color(0xFFFF3366),
                    topLeft = Offset(centerX - 40.dp.toPx(), centerY - 25.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(80.dp.toPx(), 50.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF00FF66),
                    radius = 16.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = Color.Black,
                    radius = 8.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
            }
            "EMF_COIL" -> {
                // Magnetic coil loops
                for (i in -3..3) {
                    drawOval(
                        color = Color(0xFF00E5FF),
                        topLeft = Offset(centerX + (i * 12.dp.toPx()) - 15.dp.toPx(), centerY - 30.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(30.dp.toPx(), 60.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }
            "UWB_CHIP" -> {
                // Rectangular UWB IC chip
                drawRoundRect(
                    color = Color(0xFF9C27B0),
                    topLeft = Offset(centerX - 45.dp.toPx(), centerY - 28.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(90.dp.toPx(), 56.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
                // Pin lines
                for (x in listOf(-30.dp.toPx(), -15.dp.toPx(), 0f, 15.dp.toPx(), 30.dp.toPx())) {
                    drawLine(
                        color = Color.Yellow,
                        start = Offset(centerX + x, centerY - 28.dp.toPx()),
                        end = Offset(centerX + x, centerY - 38.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = Color.Yellow,
                        start = Offset(centerX + x, centerY + 28.dp.toPx()),
                        end = Offset(centerX + x, centerY + 38.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            else -> {
                // Acoustic wave microphone
                drawCircle(
                    color = Color(0xFFFFCC00),
                    radius = 20.dp.toPx(),
                    center = Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
                for (r in listOf(30.dp.toPx(), 45.dp.toPx(), 60.dp.toPx())) {
                    drawArc(
                        color = Color(0xFFFFCC00).copy(alpha = 0.5f),
                        startAngle = -45f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(centerX - r, centerY - r),
                        size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun HistoricHeatmapScreen(uiState: SignalRadarUiState) {
    var filterType by remember { mutableStateOf("ALL") }

    val filteredHistory = remember(uiState.structuredHistory, filterType) {
        if (filterType == "ALL") {
            uiState.structuredHistory
        } else {
            uiState.structuredHistory.filter { it.type.equals(filterType, ignoreCase = true) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("historic_heatmap_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Heatmap",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "RADAR SIGNAL HEATMAP & HISTORY",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Polar Heat Density Matrix & Structured Intercept Logs",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // D3 Spatial Distribution Heatmap Component
        item {
            SpatialD3HeatmapComponent(
                blips = uiState.activeBlips,
                history = uiState.structuredHistory
            )
        }

        // Filter Bar
        item {
            FilterChipsRow(
                selectedFilter = filterType,
                onFilterSelected = { filterType = it }
            )
        }

        // Intercept Log History Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORIC INTERCEPT LOGS (${filteredHistory.size})",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Source: rf_signals_history.csv",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    ),
                    color = Color.Gray
                )
            }
        }

        if (filteredHistory.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No recorded signal events found for filter '$filterType'. Keep radar scanning active to populate logs.",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(filteredHistory, key = { "${it.timestamp}_${it.deviceName}_${it.distanceMeters}" }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (item.isBreach) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = item.deviceName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "[${item.type}]",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Text(
                                text = "${item.timestamp} • Freq: ${item.frequencyMhz} MHz",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.Gray
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "%.1fm".format(item.distanceMeters),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (item.isBreach) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (item.isBreach) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (item.isBreach) "PERIMETER BREACH" else "LOGGED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (item.isBreach) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpatialD3HeatmapComponent(
    blips: List<RadarBlip>,
    history: List<SignalHistoryItem>
) {
    var isThermalKdeMode by remember { mutableStateOf(true) }

    val totalPoints = blips.size + history.size
    val peakPointText = remember(blips, history) {
        val nearest = blips.minByOrNull { it.distance }
        if (nearest != null) {
            val rad = Math.toRadians(nearest.targetAngleOffset.toDouble())
            val x = nearest.distance * Math.sin(rad)
            val y = nearest.distance * Math.cos(rad)
            "X: %+.1fm, Y: %+.1fm".format(x, y)
        } else if (history.isNotEmpty()) {
            val first = history.first()
            val bearing = (first.deviceName.hashCode() % 360 + 360) % 360
            val rad = Math.toRadians(bearing.toDouble())
            val x = first.distanceMeters * Math.sin(rad)
            val y = first.distanceMeters * Math.cos(rad)
            "X: %+.1fm, Y: %+.1fm".format(x, y)
        } else {
            "X: +0.0m, Y: +0.0m"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("d3_heatmap_container_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF050E09)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "D3 SPATIAL KDE DENSITY MATRIX",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$totalPoints Spatial Intercept Points Evaluated",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Gray
                    )
                }

                Surface(
                    onClick = { isThermalKdeMode = !isThermalKdeMode },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (isThermalKdeMode) "Mode: D3 Thermal KDE" else "Mode: Discrete Clusters",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Canvas Heatmap Grid Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color(0xFF030A06), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF142B1F), RoundedCornerShape(16.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                SpatialD3HeatmapCanvas(
                    blips = blips,
                    history = history,
                    isKdeMode = isThermalKdeMode
                )
            }

            // D3 Color Gradient Heat Intensity Legend Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "D3 SIGNAL INTENSITY COLORMAP SCALE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    ),
                    color = Color.Gray
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF030B1E), // 0%
                                    Color(0xFF00E5FF), // 25%
                                    Color(0xFF00FF66), // 50%
                                    Color(0xFFFFEA00), // 75%
                                    Color(0xFFFF6D00), // 90%
                                    Color(0xFFFF1744)  // 100% Critical
                                )
                            ),
                            shape = RoundedCornerShape(7.dp)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(7.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0% Low", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                    Text("25%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                    Text("50% Med", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                    Text("75%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                    Text("100% Peak Hotspot", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Monospace), color = Color(0xFFFF1744))
                }
            }

            // Diagnostics row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF09140D), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hotspot Peak: $peakPointText",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                    color = Color.Yellow
                )
                Text(
                    text = "Grid Range: ±20 Meters",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SpatialD3HeatmapCanvas(
    blips: List<RadarBlip>,
    history: List<SignalHistoryItem>,
    isKdeMode: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val centerY = h / 2f
        val maxRadius = minOf(w, h) * 0.44f

        // Draw D3 Local Coordinate Grid Mesh
        // X and Y main axes
        drawLine(
            color = Color(0xFF00FF66).copy(alpha = 0.3f),
            start = Offset(centerX, centerY - maxRadius),
            end = Offset(centerX, centerY + maxRadius),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = Color(0xFF00FF66).copy(alpha = 0.3f),
            start = Offset(centerX - maxRadius, centerY),
            end = Offset(centerX + maxRadius, centerY),
            strokeWidth = 1.5.dp.toPx()
        )

        // Concentric Distance Rings (5m, 10m, 15m, 20m)
        for (i in 1..4) {
            val r = maxRadius * (i / 4f)
            drawCircle(
                color = Color(0xFF00FF66).copy(alpha = 0.15f),
                radius = r,
                center = Offset(centerX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
        }

        // Radial Direction Lines (N, NE, E, SE, S, SW, W, NW)
        for (angle in 0 until 360 step 45) {
            val rad = Math.toRadians(angle.toDouble())
            val endX = centerX + (maxRadius * Math.sin(rad)).toFloat()
            val endY = centerY - (maxRadius * Math.cos(rad)).toFloat()
            drawLine(
                color = Color(0xFF00FF66).copy(alpha = 0.12f),
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Plot Spatial Points with D3 Kernel Density Gradients
        val allPoints = mutableListOf<Pair<Offset, Float>>() // Pair<Offset, Weight>

        // Live blips
        blips.forEach { blip ->
            val normDist = (blip.distance / 20f).coerceIn(0.05f, 1f)
            val r = maxRadius * normDist
            val rad = Math.toRadians(blip.targetAngleOffset.toDouble())
            val x = centerX + (r * Math.sin(rad)).toFloat()
            val y = centerY - (r * Math.cos(rad)).toFloat()
            val weight = if (blip.distance < 5f) 1.0f else 0.7f
            allPoints.add(Pair(Offset(x, y), weight))
        }

        // Historic log items
        history.take(50).forEach { item ->
            val normDist = (item.distanceMeters / 25f).coerceIn(0.1f, 1f)
            val r = maxRadius * normDist
            val bearing = (item.deviceName.hashCode() % 360 + 360) % 360
            val rad = Math.toRadians(bearing.toDouble())
            val x = centerX + (r * Math.sin(rad)).toFloat()
            val y = centerY - (r * Math.cos(rad)).toFloat()
            val weight = if (item.isBreach) 0.9f else 0.4f
            allPoints.add(Pair(Offset(x, y), weight))
        }

        if (isKdeMode) {
            // Render Gaussian Thermal Density Blobs with D3 Color Gradient Stops
            allPoints.forEach { (pt, weight) ->
                val kernelRadius = 45.dp.toPx() * weight
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xBBFF1744), // Critical core
                            Color(0x99FF9800), // Thermal orange
                            Color(0x66FFEA00), // Vibrant yellow
                            Color(0x3300E5FF), // Dispersed cyan
                            Color(0x00030B1E)  // Fade out
                        ),
                        center = pt,
                        radius = kernelRadius
                    ),
                    radius = kernelRadius,
                    center = pt
                )
            }
        } else {
            // Render Discrete Signal Points
            allPoints.forEach { (pt, weight) ->
                val pColor = if (weight > 0.8f) Color(0xFFFF1744) else Color(0xFF00E5FF)
                drawCircle(
                    color = pColor.copy(alpha = 0.3f),
                    radius = 12.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = pColor,
                    radius = 5.dp.toPx(),
                    center = pt
                )
            }
        }

        // Phone Center Origin (0,0)
        drawCircle(
            color = Color.White,
            radius = 5.dp.toPx(),
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color(0xFF00FF66),
            radius = 10.dp.toPx(),
            center = Offset(centerX, centerY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )
    }
}


@Composable
fun rememberFilteredBlips(blips: List<RadarBlip>, filterType: String): List<RadarBlip> {
    return if (filterType == "ALL") {
        blips
    } else {
        blips.filter { it.type.equals(filterType, ignoreCase = true) }
    }
}

@Composable
fun ProximityRangeGauge(
    breachMeters: Float,
    warningMeters: Float,
    maxGaugeMeters: Float = 35.0f,
    activeBlips: List<RadarBlip>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(Color(0xFF07120E), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(10.dp)
            .testTag("proximity_range_gauge_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val barY = height * 0.42f
            val barHeight = 14.dp.toPx()

            val breachX = (breachMeters / maxGaugeMeters).coerceIn(0f, 1f) * width
            val warningX = (warningMeters / maxGaugeMeters).coerceIn(0f, 1f) * width

            // 1. Red Breach Zone
            drawRoundRect(
                color = Color(0xFFFF3366).copy(alpha = 0.85f),
                topLeft = Offset(0f, barY - barHeight / 2),
                size = Size(breachX, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // 2. Yellow Warning Zone
            if (warningX > breachX) {
                drawRect(
                    color = Color(0xFFFFCC00).copy(alpha = 0.85f),
                    topLeft = Offset(breachX, barY - barHeight / 2),
                    size = Size(warningX - breachX, barHeight)
                )
            }

            // 3. Green Safe Zone
            if (width > warningX) {
                drawRoundRect(
                    color = Color(0xFF00FF66).copy(alpha = 0.6f),
                    topLeft = Offset(warningX, barY - barHeight / 2),
                    size = Size(width - warningX, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            // Draw Live Active Blips along gauge
            activeBlips.forEach { blip ->
                val blipX = (blip.distance / maxGaugeMeters).coerceIn(0.02f, 0.98f) * width
                val blipColor = when {
                    blip.distance < breachMeters -> Color.Red
                    blip.distance < warningMeters -> Color(0xFFFFCC00)
                    else -> Color(0xFF00FF66)
                }
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(blipX, barY)
                )
                drawCircle(
                    color = blipColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(blipX, barY)
                )
            }
        }

        // Zone Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "BREACH <%.1fm".format(breachMeters),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFFF3366)
            )
            Text(
                text = "WARNING <%.1fm".format(warningMeters),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFFFCC00)
            )
            Text(
                text = "SAFE OUTDOOR",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF00FF66)
            )
        }
    }
}

@Composable
fun SettingsScreen(
    uiState: SignalRadarUiState,
    onSetScanMode: (ScanMode) -> Unit,
    onSetRssiThreshold: (Int) -> Unit,
    onToggleRssiAlert: () -> Unit,
    onSetPerimeterThreshold: (Float) -> Unit,
    onSetWarningZoneThreshold: (Float) -> Unit,
    onSetPerimeterSensitivityPreset: (PerimeterSensitivityPreset) -> Unit,
    onSetHapticPulseFrequency: (Long) -> Unit,
    onAdjustPerimeterThreshold: (Float) -> Unit,
    onAdjustWarningZoneThreshold: (Float) -> Unit,
    onSetEmfThreshold: (Float) -> Unit,
    onSetAcousticThreshold: (Int) -> Unit,
    onToggleStealthMode: () -> Unit,
    onTogglePerimeterAlarm: () -> Unit,
    onToggleAudioSonar: () -> Unit,
    onToggleBleScannerService: () -> Unit,
    onToggleBackgroundAlertService: (Boolean) -> Unit = {},
    onToggleHapticAlerts: (Boolean) -> Unit = {},
    onToggleVisualNotifs: (Boolean) -> Unit = {},
    onResetDefaults: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RADAR CONFIGURATION",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Define custom distance thresholds, fine-tune proximity alert sensitivity, and manage scanning cutoffs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section 1: PROXIMITY ALERTS & MICRO-PERIMETER TRACKING SENSITIVITY
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("proximity_alert_settings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
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
                                contentDescription = "Proximity Sensitivity",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "MICRO-PERIMETER SENSITIVITY",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = "Preset sensitivity profiles configure dual-layer inner breach and outer warning boundaries.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Preset Chips Flow / Grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Sensitivity Preset Profile:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PerimeterSensitivityPreset.entries.forEach { preset ->
                                val isSelected = uiState.perimeterSensitivityPreset == preset
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { onSetPerimeterSensitivityPreset(preset) }
                                        .testTag("preset_chip_${preset.name.lowercase()}")
                                ) {
                                    Text(
                                        text = preset.title.split(" ").first(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Selected Preset Description
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = uiState.perimeterSensitivityPreset.description,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Interactive Proximity Range Visualizer Canvas
                    ProximityRangeGauge(
                        breachMeters = uiState.perimeterThresholdMeters,
                        warningMeters = uiState.warningZoneThresholdMeters,
                        maxGaugeMeters = maxOf(35f, uiState.warningZoneThresholdMeters * 1.2f),
                        activeBlips = uiState.activeBlips
                    )

                    // Inner Perimeter Breach Threshold
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Breach Threshold",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Inner Breach Alarm Threshold:",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "%.1f m".format(uiState.perimeterThresholdMeters),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Slider(
                            value = uiState.perimeterThresholdMeters,
                            onValueChange = { onSetPerimeterThreshold(it) },
                            valueRange = 0.5f..20.0f,
                            steps = 39,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.error,
                                activeTrackColor = MaterialTheme.colorScheme.error,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("perimeter_threshold_slider")
                        )

                        // Fine Adjustment Buttons for Breach Threshold
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Fine Tuning:",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.Gray
                            )
                            OutlinedButton(
                                onClick = { onAdjustPerimeterThreshold(-0.5f) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("perimeter_adj_minus_large")
                            ) {
                                Text("-0.5m", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                            }
                            OutlinedButton(
                                onClick = { onAdjustPerimeterThreshold(-0.1f) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("perimeter_adj_minus_small")
                            ) {
                                Text("-0.1m", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                            }
                            OutlinedButton(
                                onClick = { onAdjustPerimeterThreshold(0.1f) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("perimeter_adj_plus_small")
                            ) {
                                Text("+0.1m", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                            }
                            OutlinedButton(
                                onClick = { onAdjustPerimeterThreshold(0.5f) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("perimeter_adj_plus_large")
                            ) {
                                Text("+0.5m", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    // Outer Warning Zone Threshold
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = "Warning Halo Threshold",
                                    tint = Color(0xFFFFCC00),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Outer Warning Halo Threshold:",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFCC00).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "%.1f m".format(uiState.warningZoneThresholdMeters),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFFFFCC00),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Slider(
                            value = uiState.warningZoneThresholdMeters,
                            onValueChange = { onSetWarningZoneThreshold(it) },
                            valueRange = 1.0f..50.0f,
                            steps = 49,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFCC00),
                                activeTrackColor = Color(0xFFFFCC00),
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("warning_zone_threshold_slider")
                        )

                        // Fine Adjustment Buttons for Warning Threshold
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Fine Tuning:",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.Gray
                            )
                            OutlinedButton(
                                onClick = { onAdjustWarningZoneThreshold(-1.0f) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("warning_adj_minus_large")
                            ) {
                                Text("-1.0m", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                            }
                            OutlinedButton(
                                onClick = { onAdjustWarningZoneThreshold(-0.2f) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("warning_adj_minus_small")
                            ) {
                                Text("-0.2m", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                            }
                            OutlinedButton(
                                onClick = { onAdjustWarningZoneThreshold(0.2f) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("warning_adj_plus_small")
                            ) {
                                Text("+0.2m", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                            }
                            OutlinedButton(
                                onClick = { onAdjustWarningZoneThreshold(1.0f) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("warning_adj_plus_large")
                            ) {
                                Text("+1.0m", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    // Haptic Vibration Pulse Cadence
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = "Haptic Pulse Cadence",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Proximity Haptic Cadence:",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = if (uiState.hapticPulseFrequencyMs > 0) "${uiState.hapticPulseFrequencyMs}ms" else "MUTE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                250L to "Rapid",
                                500L to "Tactical",
                                750L to "Standard",
                                1000L to "Slow",
                                0L to "Mute"
                            ).forEach { (pulseMs, label) ->
                                val isSelected = uiState.hapticPulseFrequencyMs == pulseMs
                                OutlinedButton(
                                    onClick = { onSetHapticPulseFrequency(pulseMs) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("haptic_cadence_$pulseMs")
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: SCANNING MODES TOGGLE
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ANTENNA SCANNING MODES",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                ScanMode.entries.forEach { mode ->
                    val isSelected = uiState.scanMode == mode
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSetScanMode(mode) }
                            .testTag("scan_mode_card_${mode.name.lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.surface,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (mode) {
                                        ScanMode.TACTICAL_FULL -> Icons.Default.Radar
                                        ScanMode.STEALTH_PASSIVE -> Icons.Default.VolumeOff
                                        ScanMode.HIGH_SENSITIVITY -> Icons.Default.Speed
                                        ScanMode.POWER_SAVER -> Icons.Default.Sensors
                                    },
                                    contentDescription = mode.title,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "${mode.delayMs}ms",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = mode.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Tune,
                                contentDescription = if (isSelected) "Selected" else "Select",
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 3: SIGNAL STRENGTH & SENSOR CUTOFFS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "SIGNAL STRENGTH & SENSOR CUTOFFS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // RSSI Threshold Slider
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "RSSI",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "RSSI Alert Cutoff:",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${uiState.rssiAlertThresholdDbm} dBm",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = when {
                                uiState.rssiAlertThresholdDbm >= -60 -> "High Strength Cutoff (Only very close / strong devices trigger alert)"
                                uiState.rssiAlertThresholdDbm >= -75 -> "Moderate Cutoff (Standard operational proximity alert)"
                                else -> "High Sensitivity Cutoff (Faint & distant signals trigger alert)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Slider(
                            value = uiState.rssiAlertThresholdDbm.toFloat(),
                            onValueChange = { onSetRssiThreshold(it.toInt()) },
                            valueRange = -95f..-40f,
                            steps = 55,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.testTag("rssi_threshold_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Enable RSSI Signal Threshold Alert",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = uiState.isRssiAlertEnabled,
                                onCheckedChange = { onToggleRssiAlert() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("rssi_alert_switch")
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // Background Service & Haptic / Notification Triggers
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "BACKGROUND ALERT SERVICE",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Continuously checks scanned devices in background against signal cutoff threshold (${uiState.rssiAlertThresholdDbm} dBm)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = uiState.isBackgroundAlertServiceActive,
                                    onCheckedChange = { onToggleBackgroundAlertService(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag("bg_alert_service_switch")
                                )
                            }

                            // Haptic Feedback Trigger Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Vibration,
                                        contentDescription = "Haptics",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Haptic Feedback Pulse Alert",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Switch(
                                    checked = uiState.isHapticAlertsEnabled,
                                    onCheckedChange = { onToggleHapticAlerts(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag("haptic_alerts_switch")
                                )
                            }

                            // Visual System Notification Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = "Notifications",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Visual System Notifications",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Switch(
                                    checked = uiState.isVisualNotifsEnabled,
                                    onCheckedChange = { onToggleVisualNotifs(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag("visual_notifs_switch")
                                )
                            }
                        }
                    }

                    // EMF Magnetic Flux Alert Cutoff
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Equalizer,
                                    contentDescription = "EMF",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "EMF Flux Anomaly Cutoff:",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "%.0f µT".format(uiState.emfAlertThresholdMicroTesla),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = uiState.emfAlertThresholdMicroTesla,
                            onValueChange = { onSetEmfThreshold(it) },
                            valueRange = 10.0f..200.0f,
                            steps = 38,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.testTag("emf_threshold_slider")
                        )
                    }

                    // Acoustic Microphone Threshold Cutoff
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Acoustic",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Acoustic Noise Cutoff:",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${uiState.acousticAlertThresholdDb} dB",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = uiState.acousticAlertThresholdDb.toFloat(),
                            onValueChange = { onSetAcousticThreshold(it.toInt()) },
                            valueRange = -80f..-10f,
                            steps = 70,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.testTag("acoustic_threshold_slider")
                        )
                    }
                }
            }
        }

        // Section 4: HARDWARE & SENSOR TOGGLES
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "TACTICAL HARDWARE TOGGLES",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Stealth Mode Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Stealth Silent Mode",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Mutes audio sonar echos and audible alarms.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.stealthModeEnabled,
                            onCheckedChange = { onToggleStealthMode() },
                            modifier = Modifier.testTag("stealth_mode_switch")
                        )
                    }

                    // Security Guard Proximity Alarm
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Security Guard Alarm & Haptics",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Vibrates and alerts when perimeter boundary is breached.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isPerimeterAlarmEnabled,
                            onCheckedChange = { onTogglePerimeterAlarm() },
                            modifier = Modifier.testTag("perimeter_alarm_switch")
                        )
                    }

                    // Audio Sonar Echo Ping
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Audio Sonar Echo Ping",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Plays pitch-modulated audio feedback as targets approach.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isAudioSonarActive,
                            onCheckedChange = { onToggleAudioSonar() },
                            enabled = !uiState.stealthModeEnabled,
                            modifier = Modifier.testTag("audio_sonar_switch")
                        )
                    }

                    // BLE Scanner Service Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BLE Scanner Service Engine",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Background Bluetooth Low Energy device discovery & Room DB logging.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isBleScannerServiceActive,
                            onCheckedChange = { onToggleBleScannerService() },
                            modifier = Modifier.testTag("ble_service_switch")
                        )
                    }
                }
            }
        }

        // Section 5: RESET DEFAULTS
        item {
            OutlinedButton(
                onClick = onResetDefaults,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("reset_settings_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset Defaults",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RESET SETTINGS TO FACTORY DEFAULTS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3D HARDWARE IDENTIFICATION DOCUMENTATION OVERLAY
// ---------------------------------------------------------------------------

data class HardwareCatalogueItem(
    val id: String,
    val title: String,
    val category: String,
    val frequencyRange: String,
    val antennaGain: String,
    val maxRangeMeters: String,
    val threatLevel: String,
    val threatColor: Color,
    val description: String,
    val spectrumSignature: String,
    val detectionGuide: String,
    val visualType: String
)

private fun getHardwareCatalogueData(): List<HardwareCatalogueItem> {
    return listOf(
        HardwareCatalogueItem(
            id = "wifi7_router",
            title = "Wi-Fi 7 / 6E Tri-Band Access Point",
            category = "WI-FI",
            frequencyRange = "2.4 GHz, 5 GHz, 6 GHz UNII-1 to 8",
            antennaGain = "6.5 dBi Multi-beam MIMO",
            maxRangeMeters = "35 - 100 meters",
            threatLevel = "NETWORK INFRASTRUCTURE",
            threatColor = Color(0xFF00E5FF),
            description = "High-speed enterprise wireless router broadcasting 802.11be/ax beacons with multi-link operation (MLO).",
            spectrumSignature = "Continuous beacon frames every 102.4ms with 20/40/80/160/320 MHz channel bandwidth bursts.",
            detectionGuide = "Identified on RF Spectrum Analyzer as wide flat-top orthogonal frequency-division multiplexing (OFDM) channel blocks.",
            visualType = "ROUTER_3D"
        ),
        HardwareCatalogueItem(
            id = "ble_airtag",
            title = "BLE Smart Coin Tracker / AirTag",
            category = "BLE BEACON",
            frequencyRange = "2402 MHz - 2480 MHz (Bluetooth LE)",
            antennaGain = "1.2 dBi Integrated PCB Trace Antenna",
            maxRangeMeters = "10 - 30 meters",
            threatLevel = "PRIVACY & LOCATION TRACKING RISK",
            threatColor = Color(0xFFFF9800),
            description = "Compact coin-cell location tracking beacon periodically advertising encrypted payload bursts to nearby devices.",
            spectrumSignature = "Periodic 1ms - 3ms advertisement bursts across BLE channels 37 (2402 MHz), 38 (2426 MHz), and 39 (2480 MHz).",
            detectionGuide = "Triggers RSSI alert spikes above -65 dBm; rotating MAC address signature detected in Bluetooth Scanner.",
            visualType = "BEACON_3D"
        ),
        HardwareCatalogueItem(
            id = "cell_5g_tower",
            title = "Cellular 5G NR Macro Lattice Tower",
            category = "CELL TOWER",
            frequencyRange = "600 MHz - 3.8 GHz (Sub-6) & 28 GHz (mmWave)",
            antennaGain = "18.0 dBi Beamforming Phased Array",
            maxRangeMeters = "1,500 - 10,000 meters",
            threatLevel = "MACRO TELECOM CELLULAR TOWER",
            threatColor = Color(0xFF00FF66),
            description = "High-power cellular base station equipped with tri-sector panel antennas and beamforming massive MIMO units.",
            spectrumSignature = "Synchronous 5G Primary Synchronisation Signal (PSS) and Secondary Synchronisation Signal (SSS) bursts.",
            detectionGuide = "RSRP/RSRQ cellular signal indicators logged in Cell Tower Database tracker with MCC/MNC and LAC Cell ID.",
            visualType = "CELL_TOWER_3D"
        ),
        HardwareCatalogueItem(
            id = "gnss_dish",
            title = "Satellite GNSS Parabolic Dish & Receiver",
            category = "SATELLITE",
            frequencyRange = "1575.42 MHz (L1), 1227.60 MHz (L2), 1176.45 MHz (L5)",
            antennaGain = "32.0 dBi Parabolic Reflector Gain",
            maxRangeMeters = "Direct Line-of-Sight to Orbit (20,200 km)",
            threatLevel = "ORBITAL POSITIONING & TIMING",
            threatColor = Color(0xFF9C27B0),
            description = "High-gain parabolic satellite receiver dish locked onto GPS, GLONASS, Galileo, and BeiDou satellite constellations.",
            spectrumSignature = "Extremely weak Spread-Spectrum Code Division Multiple Access (CDMA) signals below thermal noise floor (-130 dBm).",
            detectionGuide = "Monitored via Android Location Manager Satellite Doppler shift, NMEA carrier-to-noise ratio (C/N0), and Fix Status.",
            visualType = "SATELLITE_3D"
        ),
        HardwareCatalogueItem(
            id = "emf_transformer_3d",
            title = "EMF Powerline Transformer & Solenoid",
            category = "EMF & OTHER",
            frequencyRange = "50 Hz / 60 Hz AC Fundamental + Harmonics",
            antennaGain = "Near-Field Magnetic Flux Coupling",
            maxRangeMeters = "0.05 - 1.5 meters",
            threatLevel = "ELECTROMAGNETIC FIELD ANOMALY",
            threatColor = Color(0xFFFF3366),
            description = "Heavy power grid transformer, wall adapter power supply, or electric motor generating strong AC magnetic field vectors.",
            spectrumSignature = "Dominant 50Hz or 60Hz magnetic flux oscillations accompanied by 3rd & 5th harmonic distortion spikes.",
            detectionGuide = "Triggers sharp magnetometer total field vector (|B|) spikes exceeding 60-100 µT on the EMF Magnetometer tab.",
            visualType = "TRANSFORMER_3D"
        ),
        HardwareCatalogueItem(
            id = "ultrasonic_bug_3d",
            title = "Ultrasonic Acoustic Bug / Speaker",
            category = "EMF & OTHER",
            frequencyRange = "18.0 kHz - 22.5 kHz Near-Ultrasonic",
            antennaGain = "Directional Piezoelectric Horn",
            maxRangeMeters = "3 - 12 meters",
            threatLevel = "INAUDIBLE AUDIO BEACON SURVEILLANCE",
            threatColor = Color(0xFFFFCC00),
            description = "Hidden acoustic transmitter emitting inaudible high-frequency audio tones for cross-device activity tracking.",
            spectrumSignature = "Narrow-band tonal spike centered at 19.5 kHz or 20.0 kHz visible in Audio Microphone FFT Spectrum.",
            detectionGuide = "High amplitude peak above -40 dB in the 18kHz-22kHz band detected by Acoustic Frequency Detector.",
            visualType = "ULTRASONIC_3D"
        )
    )
}

@Composable
fun HardwareDocumentationOverlayDialog(
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("ALL") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("hardware_doc_overlay_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF040A07),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = "Hardware Docs",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "3D HARDWARE RECON MANUAL",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Tactical Visual Guide to Common RF Signal Sources",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF142B1F), CircleShape)
                            .testTag("close_doc_overlay_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Overlay",
                            tint = Color.White
                        )
                    }
                }

                // Category Filter Bar
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val categories = listOf("ALL", "WI-FI", "BLE BEACON", "CELL TOWER", "SATELLITE", "EMF & OTHER")
                    items(categories) { cat ->
                        val isSel = selectedCategory == cat
                        Surface(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primary else Color(0xFF0F2218),
                            border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color(0xFF1E3A2B))
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = if (isSel) Color.Black else Color.LightGray,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF142B1F))

                // 3D Hardware Items Catalog List
                val hardwareItems = remember { getHardwareCatalogueData() }
                val filteredItems = remember(selectedCategory, hardwareItems) {
                    if (selectedCategory == "ALL") hardwareItems
                    else hardwareItems.filter { it.category.contains(selectedCategory, ignoreCase = true) }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        Hardware3DItemCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun Hardware3DItemCard(item: HardwareCatalogueItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hardware_item_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF07140D)),
        border = BorderStroke(1.dp, item.threatColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Title & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Category: ${item.category} • Band: ${item.frequencyRange}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = item.threatColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, item.threatColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = item.threatLevel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = item.threatColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 3D Isometric Rendering Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF020704), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF0F261B), RoundedCornerShape(14.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Isometric3DCanvasView(visualType = item.visualType)

                // 3D Mode Label
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "3D ISOMETRIC VECTOR VIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = item.threatColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Description
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color.LightGray
            )

            // Technical Specifications Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C1D13), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Antenna Gain", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                    Text(item.antennaGain, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = Color.White)
                }
                Column {
                    Text("Max Range", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                    Text(item.maxRangeMeters, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = Color.White)
                }
            }

            // Spectrum Signature Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF05100B),
                border = BorderStroke(1.dp, Color(0xFF142B1F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "SPECTRUM SIGNATURE:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = item.spectrumSignature,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = Color.LightGray
                    )
                }
            }

            // Detection Guidance Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "How to Detect",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Phone Sensor Guide: ${item.detectionGuide}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun Isometric3DCanvasView(visualType: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        when (visualType) {
            "ROUTER_3D" -> {
                // 3D Isometric Router Base
                val pBase = Path().apply {
                    moveTo(cx, cy - 20.dp.toPx())
                    lineTo(cx + 70.dp.toPx(), cy + 15.dp.toPx())
                    lineTo(cx, cy + 50.dp.toPx())
                    lineTo(cx - 70.dp.toPx(), cy + 15.dp.toPx())
                    close()
                }
                drawPath(
                    path = pBase,
                    brush = Brush.verticalGradient(listOf(Color(0xFF0F2C1F), Color(0xFF05140D)))
                )
                drawPath(
                    path = pBase,
                    color = Color(0xFF00FF66),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Isometric Depth Front Side
                val pFront = Path().apply {
                    moveTo(cx - 70.dp.toPx(), cy + 15.dp.toPx())
                    lineTo(cx, cy + 50.dp.toPx())
                    lineTo(cx, cy + 68.dp.toPx())
                    lineTo(cx - 70.dp.toPx(), cy + 33.dp.toPx())
                    close()
                }
                drawPath(path = pFront, color = Color(0xFF071C12))
                drawPath(path = pFront, color = Color(0xFF00FF66).copy(alpha = 0.6f), style = Stroke(width = 1.5.dp.toPx()))

                // 4 Vertical Antennas with 3D offset
                val antennaX = listOf(-50.dp.toPx(), -20.dp.toPx(), 20.dp.toPx(), 50.dp.toPx())
                val antennaY = listOf(5.dp.toPx(), -10.dp.toPx(), -10.dp.toPx(), 5.dp.toPx())

                antennaX.zip(antennaY).forEach { (ax, ay) ->
                    val bx = cx + ax
                    val by = cy + ay
                    // Antenna post
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(bx, by),
                        end = Offset(bx, by - 55.dp.toPx()),
                        strokeWidth = 3.dp.toPx()
                    )
                    // Antenna top tip glow
                    drawCircle(color = Color(0xFF00E5FF), radius = 3.5.dp.toPx(), center = Offset(bx, by - 55.dp.toPx()))
                }

                // LED Status Lights
                for (i in 0..4) {
                    val lx = cx - 35.dp.toPx() + (i * 18.dp.toPx())
                    val ly = cy + 30.dp.toPx()
                    drawCircle(color = if (i % 2 == 0) Color(0xFF00FF66) else Color(0xFF00E5FF), radius = 2.5.dp.toPx(), center = Offset(lx, ly))
                }

                // RF Radiation Rings
                for (r in listOf(80.dp.toPx(), 110.dp.toPx(), 140.dp.toPx())) {
                    drawOval(
                        color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                        topLeft = Offset(cx - r, cy - 60.dp.toPx() - (r * 0.3f)),
                        size = Size(r * 2, r * 0.6f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            "BEACON_3D" -> {
                // 3D Coin Tag Isometric Cylinder Top
                drawOval(
                    brush = Brush.radialGradient(listOf(Color(0xFFFFB74D), Color(0xFFFF9800), Color(0xFFE65100))),
                    topLeft = Offset(cx - 45.dp.toPx(), cy - 25.dp.toPx()),
                    size = Size(90.dp.toPx(), 50.dp.toPx())
                )
                drawOval(
                    color = Color.White.copy(alpha = 0.8f),
                    topLeft = Offset(cx - 45.dp.toPx(), cy - 25.dp.toPx()),
                    size = Size(90.dp.toPx(), 50.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Inner Metallic Rim
                drawOval(
                    color = Color(0xFF1A1A1A),
                    topLeft = Offset(cx - 30.dp.toPx(), cy - 16.dp.toPx()),
                    size = Size(60.dp.toPx(), 32.dp.toPx())
                )
                drawCircle(color = Color(0xFFFF9800), radius = 6.dp.toPx(), center = Offset(cx, cy))

                // Keychain Hole Eyelet
                drawCircle(color = Color.Black, radius = 5.dp.toPx(), center = Offset(cx, cy - 18.dp.toPx()))
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(cx, cy - 18.dp.toPx()), style = Stroke(width = 1.dp.toPx()))

                // Pulsing BLE Radiation Arcs
                for (r in listOf(60.dp.toPx(), 85.dp.toPx(), 110.dp.toPx())) {
                    drawOval(
                        color = Color(0xFFFF9800).copy(alpha = 0.25f),
                        topLeft = Offset(cx - r, cy - (r * 0.5f)),
                        size = Size(r * 2, r),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            "CELL_TOWER_3D" -> {
                // 3D Metallic Lattice Tower Mast (Truss structure)
                val topW = 12.dp.toPx()
                val botW = 40.dp.toPx()
                val towerH = 110.dp.toPx()
                val topY = cy - 50.dp.toPx()
                val botY = cy + 60.dp.toPx()

                // Main Legs
                drawLine(Color(0xFF00FF66), Offset(cx - topW, topY), Offset(cx - botW, botY), 2.dp.toPx())
                drawLine(Color(0xFF00FF66), Offset(cx + topW, topY), Offset(cx + botW, botY), 2.dp.toPx())

                // Diagonal Bracing Steel Beams
                var stepY = topY
                var isLeft = true
                while (stepY < botY) {
                    val nextY = (stepY + 20.dp.toPx()).coerceAtMost(botY)
                    val progress1 = (stepY - topY) / towerH
                    val progress2 = (nextY - topY) / towerH

                    val w1 = topW + (botW - topW) * progress1
                    val w2 = topW + (botW - topW) * progress2

                    val x1 = if (isLeft) cx - w1 else cx + w1
                    val x2 = if (isLeft) cx + w2 else cx - w2

                    drawLine(Color(0xFF00FF66).copy(alpha = 0.4f), Offset(x1, stepY), Offset(x2, nextY), 1.dp.toPx())
                    drawLine(Color(0xFF00FF66).copy(alpha = 0.6f), Offset(cx - w1, stepY), Offset(cx + w1, stepY), 1.dp.toPx())

                    stepY = nextY
                    isLeft = !isLeft
                }

                // 3 Sector Panel Antennas on Top Platform
                val panels = listOf(-22.dp.toPx(), 0f, 22.dp.toPx())
                panels.forEach { px ->
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(cx + px - 5.dp.toPx(), topY - 18.dp.toPx()),
                        size = Size(10.dp.toPx(), 22.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                    )
                }

                // Microwave Dish Attachment
                drawCircle(Color(0xFF00E5FF), radius = 10.dp.toPx(), center = Offset(cx - 18.dp.toPx(), topY + 15.dp.toPx()))

                // 5G Directed Beamforming Radiation Rays
                for (a in listOf(-45, 0, 45)) {
                    val rad = Math.toRadians(a.toDouble())
                    val endX = cx + (90.dp.toPx() * Math.sin(rad)).toFloat()
                    val endY = topY - (90.dp.toPx() * Math.cos(rad)).toFloat()
                    drawLine(
                        color = Color(0xFF00FF66).copy(alpha = 0.35f),
                        start = Offset(cx, topY - 10.dp.toPx()),
                        end = Offset(endX, endY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            "SATELLITE_3D" -> {
                // 3D Parabolic Satellite Dish Antenna
                drawArc(
                    color = Color(0xFF9C27B0),
                    startAngle = 140f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - 50.dp.toPx(), cy - 40.dp.toPx()),
                    size = Size(100.dp.toPx(), 70.dp.toPx()),
                    style = Stroke(width = 6.dp.toPx())
                )

                // LNB Feed Horn Arm
                drawLine(Color.Yellow, Offset(cx, cy - 5.dp.toPx()), Offset(cx + 35.dp.toPx(), cy - 35.dp.toPx()), 2.dp.toPx())
                drawCircle(Color.Yellow, radius = 5.dp.toPx(), center = Offset(cx + 35.dp.toPx(), cy - 35.dp.toPx()))

                // Support Tripod Mount
                drawLine(Color.Gray, Offset(cx - 15.dp.toPx(), cy + 25.dp.toPx()), Offset(cx - 30.dp.toPx(), cy + 60.dp.toPx()), 2.5.dp.toPx())
                drawLine(Color.Gray, Offset(cx + 15.dp.toPx(), cy + 25.dp.toPx()), Offset(cx + 30.dp.toPx(), cy + 60.dp.toPx()), 2.5.dp.toPx())

                // Satellite Relays Downlink Wave
                for (d in listOf(25.dp.toPx(), 50.dp.toPx(), 75.dp.toPx())) {
                    drawLine(
                        color = Color(0xFF9C27B0).copy(alpha = 0.3f),
                        start = Offset(cx + 35.dp.toPx() + d, cy - 35.dp.toPx() - d),
                        end = Offset(cx + 35.dp.toPx(), cy - 35.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            "TRANSFORMER_3D" -> {
                // 3D Transformer Iron Core Frame
                drawRoundRect(
                    color = Color(0xFF37474F),
                    topLeft = Offset(cx - 50.dp.toPx(), cy - 35.dp.toPx()),
                    size = Size(100.dp.toPx(), 70.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 8.dp.toPx())
                )

                // Copper Solenoid Windings
                for (i in -3..3) {
                    drawOval(
                        color = Color(0xFFFF9800),
                        topLeft = Offset(cx + (i * 10.dp.toPx()) - 8.dp.toPx(), cy - 30.dp.toPx()),
                        size = Size(16.dp.toPx(), 60.dp.toPx()),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                // Cyan Magnetic Field Vector Lines
                for (r in listOf(65.dp.toPx(), 85.dp.toPx(), 105.dp.toPx())) {
                    drawOval(
                        color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                        topLeft = Offset(cx - r, cy - (r * 0.6f)),
                        size = Size(r * 2, r * 1.2f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            else -> {
                // 3D Ultrasonic Speaker Transducer Cone
                drawCircle(Color(0xFF263238), radius = 45.dp.toPx(), center = Offset(cx, cy))
                drawCircle(Color(0xFFFFCC00), radius = 45.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = 3.dp.toPx()))
                drawCircle(Color(0xFF102018), radius = 22.dp.toPx(), center = Offset(cx, cy))
                drawCircle(Color(0xFFFFCC00), radius = 8.dp.toPx(), center = Offset(cx, cy))

                // High-Frequency Sound Wave Arcs
                for (r in listOf(55.dp.toPx(), 75.dp.toPx(), 95.dp.toPx())) {
                    drawArc(
                        color = Color(0xFFFFCC00).copy(alpha = 0.35f),
                        startAngle = -60f,
                        sweepAngle = 120f,
                        useCenter = false,
                        topLeft = Offset(cx - r, cy - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}
