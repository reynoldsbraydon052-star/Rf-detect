package com.example
import com.example.SonarState

import android.Manifest
import android.net.Uri
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Hub
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.content.ContextCompat
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the secure credential store on startup
        val credentialStore = AiCredentialStore.getInstance(this)
        lifecycleScope.launch {
            if (!credentialStore.hasGeminiApiKey()) {
                // Safely load build config key via reflection if present as a baseline
                try {
                    val buildConfigClass = Class.forName("com.example.BuildConfig")
                    val apiKeyField = buildConfigClass.getField("GEMINI_API_KEY")
                    val defaultKey = apiKeyField.get(null) as? String
                    if (!defaultKey.isNullOrBlank() && defaultKey != "MY_GEMINI_API_KEY" && !defaultKey.contains("ENTER_YOUR_KEY")) {
                        credentialStore.setGeminiApiKey(defaultKey)
                    }
                } catch (e: Exception) {
                    // No default build config key or class not found
                }
            }
        }

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
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showNoiseCalibration by remember { mutableStateOf(false) }

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

        if (uiState.isAiDeepAuditDialogOpen) {
        AiDeepAuditModal(
            isOpen = uiState.isAiDeepAuditDialogOpen,
            isAnalyzing = uiState.isAiAnalyzingThreats,
            threatReport = uiState.threatAnalysisReport,
            activeBlips = uiState.activeBlips,
            onDismiss = { viewModel.closeAiDeepAuditDialog() },
            onReRunAudit = { viewModel.runAiDeepAudit(openModal = true) },
            onSelectTargetOnRadar = { targetId ->
                viewModel.selectTargetDevice(targetId)
                viewModel.setTab(RadarTab.SWEEP_RADAR)
            }
        )
    }

    if (uiState.isFigureEightCalibrationActive) {
            FigureEightCalibrationDialog(
                onDismiss = { viewModel.closeFigureEightCalibration() },
                onCalibrationComplete = { score -> viewModel.completeFigureEightCalibration(score) }
            )
        }

        if (uiState.isGattDossierDialogOpen && uiState.interrogatedDossier != null) {
            GattDossierDialog(
                dossier = uiState.interrogatedDossier!!,
                onDismiss = { viewModel.closeGattDossierDialog() }
            )
        }

        if (uiState.isPinpointDialogOpen) {
            Ai3dPinpointDialog(
                pinpointResult = uiState.activePinpointResult,
                sensorSuite = uiState.sensorSuite,
                compassHeading = uiState.headingDegrees,
                isAudioSonarActive = uiState.isAudioSonarActive,
                onToggleAudioSonar = { viewModel.toggleAudioSonar() },
                onRefreshPinpoint = {
                    viewModel.triggerAiPinpointForCurrentTarget()
                },
                onDismiss = { viewModel.closeAiPinpointDialog() }
            )
        }

        val isImmersive = uiState.selectedTab == RadarTab.SWEEP_RADAR || uiState.selectedTab == RadarTab.FULL_RADAR

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .let { if (isImmersive) it else it.padding(padding) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .let {
                        if (isImmersive) {
                            it
                        } else {
                            it.padding(
                                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                            )
                        }
                    }
            ) {
                // Top Tactical Header Badge
                if (!isImmersive) {
                    TacticalHeader(
                        uiState = uiState,
                        onOpenSettings = { viewModel.setTab(RadarTab.SETTINGS) },
                        onOpenDocOverlay = { showHardwareDocOverlay = true },
                        onOpenFullRadar = { viewModel.setTab(RadarTab.FULL_RADAR) },
                        onOpenScanner = { viewModel.setTab(RadarTab.SCANNER) },
                        onOpenAiIntel = { viewModel.setTab(RadarTab.AI_THREAT_ANALYSIS) }
                    )
                }

                // High-Priority EW Alert Banners
                AnimatedVisibility(visible = (uiState.isRfJammingDetected || uiState.isGnssSpoofingDetected) && !isImmersive) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF330000)),
                        border = BorderStroke(1.5.dp, Color.Red)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "EW Alert", tint = Color.Red)
                            Text(
                                text = "CRITICAL WARN: BROADCAST RF JAMMING / GNSS SPOOFING DETECTED.",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                color = Color.Red
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = uiState.imsiCatcherAlert.isAlertTriggered && !isImmersive) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF331100)),
                        border = BorderStroke(1.5.dp, Color(0xFFFF6600))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CellTower, contentDescription = "Rogue Cell Alert", tint = Color(0xFFFF6600))
                            Text(
                                text = "CRITICAL WARN: UNVERIFIED / ROGUE CELL TOWER (IMSI CATCHER) DETECTED.",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                color = Color(0xFFFF6600)
                            )
                        }
                    }
                }

                // Accompanist Permissions Banner for Location Access (required for Bluetooth & Wi-Fi scanning)
                val hasLocationAccess = locationAndScanPermissionsState.permissions
                    .filter { it.permission == Manifest.permission.ACCESS_FINE_LOCATION || it.permission == Manifest.permission.ACCESS_COARSE_LOCATION }
                    .any { it.status.isGranted }

                AnimatedVisibility(visible = !hasLocationAccess && !isImmersive) {
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
                AnimatedVisibility(visible = uiState.calibrationNotificationMessage != null && !isImmersive) {
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

                val sonarState by viewModel.sonarState.collectAsStateWithLifecycle()
                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.selectedTab) {
                        RadarTab.SWEEP_RADAR -> SweepRadarScreen(
                            sonarState = sonarState,
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
                            onPlayTestPing = { viewModel.playTestAudioPing(it) },
                            onOpenCalibration = { viewModel.openFigureEightCalibration() },
                            onUpdateBleCatalogueTag = { mac, tag -> viewModel.updateBleCatalogueTag(mac, tag) },
                            onToggleFloorplan = { viewModel.toggleFloorplan() },
                            onToggleFocusMode = { viewModel.toggleFocusMode() },
                            onSetMaxDevices = { viewModel.setMaxVisibleDevices(it) },
                            onSetMinRssi = { viewModel.setMinRssiFilter(it) },
                            onToggleHudDeclutter = { viewModel.toggleHudDeclutter() },
                            onSetSortBy = { viewModel.setSortByPriority(it) },
                            onTriggerAiPinpoint = { viewModel.startAiPinpoint(it) },
                            onCycleRadarBoost = { viewModel.cycleRadarBoostLevel() },
                            onRunAiDeepAudit = { viewModel.openAiDeepAuditDialog() },
                            onCycleRadarGridMode = { viewModel.cycleRadarGridMode() },
                            onOpenRadarGridConfig = { viewModel.openRadarGridConfigDialog() },
                            targetRssiThresholds = viewModel.targetRssiThresholds,
                            targetDistanceThresholds = viewModel.targetDistanceThresholds,
                            onSetTargetRssiThreshold = { id, rssi -> viewModel.setTargetRssiThreshold(id, rssi) },
                            onSetTargetDistanceThreshold = { id, dist -> viewModel.setTargetDistanceThreshold(id, dist) },
                            onViewModeChanged = { viewModel.setViewMode(it) }
                        )

                        RadarTab.FULL_RADAR -> FullScreenRadarScreen(
                            uiState = uiState,
                            onZoomIn = { viewModel.zoomInMap() },
                            onZoomOut = { viewModel.zoomOutMap() },
                            onSetMapRange = { viewModel.setMapRangeMeters(it) },
                            onSelectTargetDevice = { viewModel.selectTargetDevice(it) },
                            onToggleAudioSonar = { viewModel.toggleAudioSonar() },
                            onSetFullScreenMapMode = { viewModel.setFullScreenMapMode(it) },
                            onSetMaxDevices = { viewModel.setMaxVisibleDevices(it) },
                            onToggleFocusMode = { viewModel.toggleFocusMode() },
                            onTriggerAiPinpoint = { viewModel.startAiPinpoint(it) },
                            onCycleRadarBoost = { viewModel.cycleRadarBoostLevel() },
                            onRunAiDeepAudit = { viewModel.openAiDeepAuditDialog() }
                        )

                        RadarTab.SIMULATION_LAB -> {
                            val activeSession by viewModel.replayEngine.activeSession.collectAsStateWithLifecycle()
                            val savedSessions by viewModel.rfSessionEngine.allSessions.collectAsStateWithLifecycle(initialValue = emptyList())
                            val replayState by viewModel.replayEngine.replayState.collectAsStateWithLifecycle()
                            val currentPositionMs by viewModel.replayEngine.currentPositionMs.collectAsStateWithLifecycle()
                            val playbackSpeed by viewModel.replayEngine.playbackSpeed.collectAsStateWithLifecycle()
                            val simulationScenario by viewModel.simulationEngine.activeScenario.collectAsStateWithLifecycle()

                            SimulationLabScreen(
                                uiState = uiState,
                                replayState = replayState,
                                simulationScenario = simulationScenario,
                                activeSession = activeSession,
                                savedSessions = savedSessions,
                                currentPositionMs = currentPositionMs,
                                playbackSpeed = playbackSpeed,
                                onStartSimulation = { 
                                    viewModel.setOperatingMode(OperatingMode.SIMULATION)
                                    viewModel.simulationEngine.startSimulation(it) 
                                },
                                onStopSimulation = { 
                                    viewModel.simulationEngine.stopSimulation()
                                    viewModel.setOperatingMode(OperatingMode.LIVE)
                                },
                                onLoadReplay = { 
                                    viewModel.setOperatingMode(OperatingMode.REPLAY)
                                    viewModel.replayEngine.loadSession(it) 
                                },
                                onPlayReplay = { viewModel.replayEngine.play() },
                                onPauseReplay = { viewModel.replayEngine.pause() },
                                onStopReplay = { viewModel.replayEngine.stopReplay() },
                                onSeekReplay = { viewModel.replayEngine.seekTo(it) },
                                onSetPlaybackSpeed = { viewModel.replayEngine.setSpeed(it) },
                                onReturnToLive = {
                                    viewModel.simulationEngine.stopSimulation()
                                    viewModel.replayEngine.stopReplay()
                                    viewModel.setOperatingMode(OperatingMode.LIVE)
                                }
                            )
                        }

                        RadarTab.AI_THREAT_ANALYSIS -> AiThreatIntelScreen(
                            uiState = uiState,
                            threatReport = uiState.threatAnalysisReport,
                            investigatorAssessment = uiState.investigatorAssessment,
                            onSaveInterpretation = { viewModel.saveAiInterpretation() },
                            isAnalyzing = uiState.isAiAnalyzingThreats,
                            copilotMessages = uiState.copilotMessages,
                            isCopilotThinking = uiState.isCopilotThinking,
                            selectedDeepAuditTarget = uiState.selectedDeepAuditTarget,
                            isDeepAuditingEmitterId = uiState.isDeepAuditingEmitterId,
                            onRunAiThreatScan = { viewModel.runAiInvestigator() },
                            onSendCopilotQuery = { viewModel.sendCopilotQuery(it) },
                            onSelectTargetOnRadar = { viewModel.selectTargetDevice(it) },
                            onOpenRadarTab = { viewModel.setTab(RadarTab.SWEEP_RADAR) },
                            onTriggerDeepAudit = { emitter -> viewModel.triggerTargetDeepAudit(emitter) },
                            onCloseDeepAudit = { viewModel.closeDeepAuditModal() },
                            onTestGeminiConnection = { viewModel.testGeminiConnection() }
                        )

                        RadarTab.SCANNER -> TacticalScannerScreen(
                            uiState = uiState,
                            onToggleBleScanner = { viewModel.toggleBleScannerService() },
                            onToggleScanning = { viewModel.toggleScanning() },
                            onFilterSelected = { viewModel.setFilterType(it) },
                            onSelectTargetDevice = { viewModel.selectTargetDevice(it) },
                            onOpenArCameraForTarget = { targetId ->
                                viewModel.selectTargetDevice(targetId)
                                viewModel.setFullScreenMapMode("AR")
                                viewModel.toggleFullScreenMap(true)
                            },
                            onPlayTestPing = { viewModel.playTestAudioPing(it) },
                            onClearScanHistory = { viewModel.clearBleDatabaseLogs() },
                            onAddSpatialPoint = { id, pt -> viewModel.addSpatialPoint(id, pt) },
                            onSetMapRange = { viewModel.setMapRangeMeters(it) }
                        )

                        RadarTab.SPECTRUM_ANALYZER -> SpectrumAnalyzerScreen(
                            sonarState = sonarState,
                            uiState = uiState,
                            onFilterSelected = { viewModel.setFilterType(it) },
                            onSelectTargetDevice = { viewModel.selectTargetDevice(it) },
                            onToggleAudioSonar = { viewModel.toggleAudioSonar() },
                            onOpenArCameraForTarget = { targetId ->
                                viewModel.selectTargetDevice(targetId)
                                viewModel.setFullScreenMapMode("AR")
                                viewModel.toggleFullScreenMap(true)
                            },
                            onOpenCalibration = { viewModel.openFigureEightCalibration() },
                            onInterrogateGatt = { blip -> viewModel.interrogateGattForBlip(blip) }
                        )

                        RadarTab.DETECTED_SENSORS -> DetectedSensorsScreen(
                            uiState = uiState,
                            onOpenDocOverlay = { showHardwareDocOverlay = true }
                        )

                        RadarTab.HISTORIC_HEATMAP -> HistoricHeatmapScreen(
                            uiState = uiState
                        )
                        RadarTab.ENVIRONMENT_MAP -> RfEnvironmentMapScreen(
                            uiState = uiState,
                            mappingEngine = viewModel.environmentMappingEngine
                        )
                        RadarTab.IDENTITY_GRAPH -> DeviceIdentityScreen(
                            uiState = uiState,
                            identityEngine = viewModel.deviceIdentityEngine
                        )
                        RadarTab.INTELLIGENCE_DASHBOARD -> IntelligenceDashboardScreen(
                            viewModel = viewModel,
                            uiState = uiState,
                            sessionEngine = viewModel.rfSessionEngine,
                            anomalyEngine = viewModel.rfAnomalyEngine,
                            patternEngine = viewModel.rfPatternEngine,
                            intelligenceEngine = viewModel.rfIntelligenceEngine
                        )

                        RadarTab.MAGNETOMETER_EMF -> MagnetometerScreen(
                            uiState = uiState,
                            onRecalibrate = { viewModel.recalibrateMagnetometer() },
                            onTriggerSpike = { viewModel.triggerRfSpike() },
                            onClearInterference = { viewModel.clearRfInterference() },
                            onExportAcousticReportUri = { uri -> viewModel.writeAcousticReportToUri(context, uri) }
                        )

                        RadarTab.SECURITY_GUARD -> SecurityGuardScreen(
                            uiState = uiState,
                            onThresholdChanged = { viewModel.setPerimeterThreshold(it) },
                            onToggleAlarm = { viewModel.togglePerimeterAlarm() },
                            onSnapshotTrustedBaseline = { viewModel.snapshotTrustedBaseline() }
                        )

                        RadarTab.CSV_LOG_CONSOLE -> CsvLogConsoleScreen(
                            uiState = uiState,
                            onClearLogs = { viewModel.clearLogHistory() },
                            onExportCsvUri = { uri -> viewModel.writeLogsToUri(context, uri) }
                        )

                        RadarTab.EVENT_RECORDER -> RfEventRecorderScreen(
                            uiState = uiState,
                            recorderEngine = viewModel.rfEventRecorderEngine,
                            onExportJson = { uri -> viewModel.exportRfEventsJson(context, uri) },
                            onExportCsv = { uri -> viewModel.exportRfEventsCsv(context, uri) },
                            onExportCaptures = { 
                                android.widget.Toast.makeText(context, "No PCAP or raw SDR capture files available for this session.", android.widget.Toast.LENGTH_LONG).show() 
                            }
                        )
                        RadarTab.ADAPTIVE_LOCALIZATION -> AdaptiveRfLocalizationScreen(
                            uiState = uiState,
                            onSelectTargetDevice = { viewModel.selectTargetDevice(it) },
                            onBackToRadar = { viewModel.setTab(RadarTab.SWEEP_RADAR) },
                            viewModel = viewModel
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
                            onExportLogsCsvUri = { uri -> viewModel.writeLogsToUri(context, uri) },
                            onExportKmlBreadcrumbsUri = { uri -> viewModel.writeKmlToUri(context, uri) },
                            onPurgeHistory = { viewModel.purgeInterceptionHistory() },
                            onOpenCalibration = { viewModel.openFigureEightCalibration() },
                            onSnapshotTrustedBaseline = { viewModel.snapshotTrustedBaseline() },
                            onSetMaxDevices = { viewModel.setMaxVisibleDevices(it) },
                            onTestGeminiConnection = { viewModel.testGeminiConnection() },
                            onTestNetworkSpeed = { viewModel.runNetworkConnectivityAndSpeedTest() },
                            onSetAiMode = { viewModel.setAiInferenceMode(it) },
                            onSaveGeminiKey = { viewModel.saveGeminiApiKey(it) },
                            onClearGeminiKey = { viewModel.clearGeminiApiKey() }
                        )
                    }
                }
            }

            if (uiState.isFullScreenMapVisible) {
                FullScreenRadarMapOverlay(
                    uiState = uiState,
                    anomalies = viewModel.rfAnomalyEngine.anomalies.collectAsStateWithLifecycle().value,
                    onDismiss = { viewModel.toggleFullScreenMap(false) },
                    onZoomIn = { viewModel.zoomInMap() },
                    onZoomOut = { viewModel.zoomOutMap() },
                    onSetMapRange = { viewModel.setMapRangeMeters(it) },
                    onSelectTargetDevice = { viewModel.selectTargetDevice(it) },
                    onToggleAudioSonar = { viewModel.toggleAudioSonar() },
                    onSetFullScreenMapMode = { viewModel.setFullScreenMapMode(it) },
                    onSetMaxDevices = { viewModel.setMaxVisibleDevices(it) },
                    onToggleFocusMode = { viewModel.toggleFocusMode() },
                    onTriggerAiPinpoint = { viewModel.startAiPinpoint(it) },
                    onCycleRadarBoost = { viewModel.cycleRadarBoostLevel() },
                    onRunAiDeepAudit = { viewModel.openAiDeepAuditDialog() },
                    onAddSpatialPoint = { id, pt -> viewModel.addSpatialPoint(id, pt) }
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
    onOpenFullRadar: (() -> Unit)? = null,
    onOpenScanner: (() -> Unit)? = null,
    onOpenAiIntel: (() -> Unit)? = null
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
                        text = "Google Pixel Spectrum • Multi-Antenna Active",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Gemini AI Intel quick access badge
                if (onOpenAiIntel != null) {
                    val threatLevel = uiState.threatAnalysisReport?.threatLevel ?: ThreatLevel.SECURE
                    Surface(
                        onClick = onOpenAiIntel,
                        shape = RoundedCornerShape(16.dp),
                        color = if (uiState.selectedTab == RadarTab.AI_THREAT_ANALYSIS) threatLevel.color.copy(alpha = 0.3f) else Color(0xFF071C11),
                        border = BorderStroke(1.dp, threatLevel.color),
                        modifier = Modifier.testTag("header_ai_intel_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Gemini AI Intel",
                                tint = threatLevel.color,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (uiState.isAiAnalyzingThreats) "AI SCANNING..." else "AI INTEL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = threatLevel.color
                            )
                        }
                    }
                }

                if (onOpenScanner != null) {
                    Surface(
                        onClick = onOpenScanner,
                        shape = RoundedCornerShape(16.dp),
                        color = if (uiState.selectedTab == RadarTab.SCANNER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("header_open_scanner_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "Scanner",
                                tint = if (uiState.selectedTab == RadarTab.SCANNER) Color.Black else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "SCANNER",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = if (uiState.selectedTab == RadarTab.SCANNER) Color.Black else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

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
                    text = "NEAREST: ${nearest.name.take(10)} (${String.format("%.1f ft", nearest.distance * 3.28084f)})",
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
    onToggleScanning: () -> Unit,
    onSetMaxDevices: (Int) -> Unit = {},
    onToggleFocusMode: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_controls_panel_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), thickness = 0.8.dp)

            // Direct Slider for Maximum Rendered Radar Devices in Quick Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Device Slider",
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "MAX RADAR TARGETS SLIDER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = Color(0xFF00FF66)
                        )
                    }
                    Text(
                        text = if (uiState.isFocusModeEnabled) "FOCUS MODE (3)" else if (uiState.maxVisibleDevices == 0) "ALL TARGETS" else "${uiState.maxVisibleDevices} MAX",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = if (uiState.isFocusModeEnabled) Color(0xFFFFCC00) else Color.White
                    )
                }

                Slider(
                    value = if (uiState.isFocusModeEnabled) 3f else if (uiState.maxVisibleDevices == 0) 50f else uiState.maxVisibleDevices.toFloat(),
                    onValueChange = { value ->
                        if (uiState.isFocusModeEnabled) onToggleFocusMode()
                        val intVal = value.toInt()
                        onSetMaxDevices(if (intVal >= 50) 0 else intVal)
                    },
                    valueRange = 1f..50f,
                    steps = 48,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00FF66),
                        activeTrackColor = Color(0xFF00FF66),
                        inactiveTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .testTag("quick_controls_max_devices_slider")
                )
            }
        }
    }
}@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SweepRadarScreen(
    sonarState: SonarState = SonarState.IDLE,
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
    onOpenCalibration: () -> Unit = {},
    onUpdateBleCatalogueTag: (String, String) -> Unit = { _, _ -> },
    onToggleFloorplan: () -> Unit = {},
    onToggleFocusMode: () -> Unit = {},
    onSetMaxDevices: (Int) -> Unit = {},
    onSetMinRssi: (Int) -> Unit = {},
    onToggleHudDeclutter: () -> Unit = {},
    onSetSortBy: (String) -> Unit = {},
    onTriggerAiPinpoint: (RadarBlip) -> Unit = {},
    onCycleRadarBoost: () -> Unit = {},
    onRunAiDeepAudit: () -> Unit = {},
    onCycleRadarGridMode: () -> Unit = {},
    onOpenRadarGridConfig: () -> Unit = {},
    onViewModeChanged: (ViewMode) -> Unit = {},
    targetRssiThresholds: Map<String, Int> = emptyMap(),
    targetDistanceThresholds: Map<String, Float> = emptyMap(),
    onSetTargetRssiThreshold: (String, Int) -> Unit = { _, _ -> },
    onSetTargetDistanceThreshold: (String, Float) -> Unit = { _, _ -> },
    windowSizeClass: WindowSizeClass = rememberWindowSizeClass()
) {
    // 1. Core States for Interaction: Gestures, Modes & Controls
    var recenterTriggerCount by remember { mutableStateOf(0) }
    var isOrientationLockedByHeading by rememberSaveable { mutableStateOf(true) }
    var isDockExpanded by rememberSaveable { mutableStateOf(false) }
    var isSheetExpanded by rememberSaveable { mutableStateOf(false) }
    var showDevDiagnosticsDialog by remember { mutableStateOf(false) }
    var showAiThreatDialog by remember { mutableStateOf(false) }
    var showLayersConfigDialog by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    var lastInteractionTime by remember { mutableStateOf(0L) }
    val isInteracting = lastInteractionTime > 0L
    LaunchedEffect(lastInteractionTime) {
        if (lastInteractionTime > 0L) {
            kotlinx.coroutines.delay(4000L)
            lastInteractionTime = 0L
        }
    }
    
    // Smooth visualization mode selection
    val selectedMode = uiState.viewMode
    
    // Smooth visibility alphas for transition
    val radarAlpha by animateFloatAsState(targetValue = if (selectedMode == ViewMode.MAP) 0f else 1f, label = "radarAlpha")
    val mapAlpha by animateFloatAsState(targetValue = if (selectedMode == ViewMode.RADAR) 0f else 1f, label = "mapAlpha")

    // Filtered blips based on active filters and search query
    val rawFilteredBlips = rememberFilteredBlips(
        blips = uiState.activeBlips,
        filterType = uiState.selectedFilterType,
        maxDevices = uiState.maxVisibleDevices,
        isFocusMode = uiState.isFocusModeEnabled,
        minRssiDbm = uiState.minRssiFilterDbm,
        selectedTargetId = uiState.selectedTargetDeviceId,
        sortBy = uiState.sortByPriority
    )
    
    val filteredBlips = remember(rawFilteredBlips, searchQuery) {
        if (searchQuery.isEmpty()) {
            rawFilteredBlips
        } else {
            rawFilteredBlips.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.id.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Selected blip state
    val selectedTargetId = uiState.selectedTargetDeviceId
    val selectedBlip = uiState.activeBlips.find { it.id == selectedTargetId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020704))
            .testTag("sweep_radar_fullscreen_container")
    ) {
        // LAYER 1: Full-screen interactive map and radar scope
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    }
                }
        ) {
            when (selectedMode) {
                ViewMode.MAP -> {
                    GeographicMapView(
                        headingDegrees = if (isOrientationLockedByHeading) uiState.headingDegrees else 0f,
                        filteredBlips = filteredBlips,
                        uiState = uiState,
                        mapAlpha = mapAlpha,
                        onZoomInMap = onZoomInMap,
                        onZoomOutMap = onZoomOutMap,
                        onSetMapRange = onSetMapRange,
                        onSelectTargetDevice = onSelectTargetDevice,
                        onToggleFloorplan = onToggleFloorplan,
                        onToggleFocusMode = onToggleFocusMode,
                        onSetMaxDevices = onSetMaxDevices,
                        onCycleRadarBoost = onCycleRadarBoost,
                        onTriggerAiPinpoint = onTriggerAiPinpoint,
                        onCycleRadarGridMode = onCycleRadarGridMode,
                        onOpenRadarGridConfig = onOpenRadarGridConfig,
                        recenterTriggerCount = recenterTriggerCount
                    )
                }
                ViewMode.RADAR -> {
                    SignalSweepCanvasView(
                        headingDegrees = if (isOrientationLockedByHeading) uiState.headingDegrees else 0f,
                        filteredBlips = filteredBlips,
                        uiState = uiState,
                        radarAlpha = radarAlpha,
                        onZoomInMap = onZoomInMap,
                        onZoomOutMap = onZoomOutMap,
                        onSetMapRange = onSetMapRange,
                        onSelectTargetDevice = onSelectTargetDevice,
                        onToggleFloorplan = onToggleFloorplan,
                        onToggleFocusMode = onToggleFocusMode,
                        onSetMaxDevices = onSetMaxDevices,
                        onCycleRadarBoost = onCycleRadarBoost,
                        onTriggerAiPinpoint = onTriggerAiPinpoint,
                        onCycleRadarGridMode = onCycleRadarGridMode,
                        onOpenRadarGridConfig = onOpenRadarGridConfig,
                        recenterTriggerCount = recenterTriggerCount
                    )
                }
                ViewMode.HYBRID -> {
                    HybridRadarMapView(
                        headingDegrees = if (isOrientationLockedByHeading) uiState.headingDegrees else 0f,
                        filteredBlips = filteredBlips,
                        uiState = uiState,
                        radarAlpha = radarAlpha,
                        mapAlpha = mapAlpha,
                        onZoomInMap = onZoomInMap,
                        onZoomOutMap = onZoomOutMap,
                        onSetMapRange = onSetMapRange,
                        onSelectTargetDevice = onSelectTargetDevice,
                        onToggleFloorplan = onToggleFloorplan,
                        onToggleFocusMode = onToggleFocusMode,
                        onSetMaxDevices = onSetMaxDevices,
                        onCycleRadarBoost = onCycleRadarBoost,
                        onTriggerAiPinpoint = onTriggerAiPinpoint,
                        onCycleRadarGridMode = onCycleRadarGridMode,
                        onOpenRadarGridConfig = onOpenRadarGridConfig,
                        recenterTriggerCount = recenterTriggerCount
                    )
                }
            }
        }

        // Empty state center indicator (if there are no blips)
        if (filteredBlips.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "READY",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = Color(0xFF00FF66).copy(alpha = 0.6f)
                )
                Text(
                    text = "WALK NATURALLY TO MAP SIGNALS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    color = Color.Gray.copy(alpha = 0.8f)
                )
            }
        }

        // LAYER 2: Minimal Translucent Glass Top Status Bar
        AnimatedVisibility(
            visible = !isInteracting,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF030705).copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.25f)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tactical Mode & Target telemetry info
                Column {
                    Text(
                        text = if (selectedBlip != null) "RF TRACKING • CH ${selectedBlip.frequencyMhz}" else "RF SCANNING • HYBRID SCOPE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF00FF66)
                    )
                    Text(
                        text = if (selectedBlip != null) "LOCK: ${selectedBlip.name} • ${selectedBlip.rssi} dBm" else "EST ALTITUDE: ${uiState.sensorSuite.estimatedAltitudeMeters.toInt()}m • ${filteredBlips.size} CHANNELS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.LightGray
                    )
                }

                // Tiny non-blocking warning indicators (Multipath, sensor quality etc.)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isRfJammingDetected) {
                        TinyWarningBadge(text = "⚠ JAM", tooltip = "RF Jamming Interference Detected", color = Color.Red)
                    }
                    if (uiState.isGnssSpoofingDetected) {
                        TinyWarningBadge(text = "⚠ GNSS", tooltip = "GNSS Spoofing Detected", color = Color(0xFFFF9800))
                    }
                    if (uiState.compassAccuracyScore < 80) {
                        TinyWarningBadge(text = "⚠ SENS", tooltip = "Low Compass Accuracy: ${uiState.compassAccuracyScore}%", color = Color(0xFFFFCC00))
                    }
                    if (filteredBlips.size < 3) {
                        TinyWarningBadge(text = "${4 - filteredBlips.size} more pts", tooltip = "Insufficient measurements", color = Color.Gray)
                    }
                }

                // Small Sensor Status Badges (Icons & Labels)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SensorStatusIcon(name = "GPS", isActive = true)
                    SensorStatusIcon(name = "AR", isActive = true)
                    SensorStatusIcon(name = "BLE", isActive = uiState.isBleScannerServiceActive)
                    SensorStatusIcon(name = "AI", isActive = uiState.copilotMessages.isNotEmpty() || uiState.isAiAnalyzingThreats)
                }
            }
        }
    }

        // LAYER 3: Floating Mode Selector (MAP | RADAR | HYBRID) below top status bar
        AnimatedVisibility(
            visible = !isInteracting,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
                .statusBarsPadding()
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF05110A).copy(alpha = 0.88f),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.35f)),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf("MAP", "RADAR", "HYBRID").forEach { mode ->
                        val isSelected = selectedMode.name == mode
                        Surface(
                            onClick = { onViewModeChanged(ViewMode.valueOf(mode)) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFF00FF66) else Color.Transparent,
                            modifier = Modifier.height(32.dp).width(75.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = mode,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    color = if (isSelected) Color.Black else Color(0xFF00FF66)
                                )
                            }
                        }
                    }
                }
            }
        }

        // LAYER 4: Collapsible Floating Control Dock (at the Bottom-Right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = if (isSheetExpanded) 430.dp else 100.dp, end = 16.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Expanded Dock Actions
                AnimatedVisibility(
                    visible = isDockExpanded,
                    enter = slideInVertically { h -> h } + fadeIn(),
                    exit = slideOutVertically { h -> h } + fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Layers Settings Configuration Trigger
                        FloatingDockIcon(
                            icon = Icons.Default.Layers,
                            label = "Layers",
                            onClick = { showLayersConfigDialog = true }
                        )

                        // Toggle Floorplan Map Overlays
                        FloatingDockIcon(
                            icon = Icons.Default.Map,
                            label = "Floorplan",
                            isActive = uiState.isFloorplanEnabled,
                            onClick = onToggleFloorplan
                        )

                        // Clear Data / Measurements Reset
                        FloatingDockIcon(
                            icon = Icons.Default.Refresh,
                            label = "Recalibrate",
                            onClick = onClearBleDb
                        )

                        // Switch focus modes
                        FloatingDockIcon(
                            icon = Icons.Default.FilterList,
                            label = "Focus Mode",
                            isActive = uiState.isFocusModeEnabled,
                            onClick = onToggleFocusMode
                        )

                        // AI Threat bottom sheet trigger
                        FloatingDockIcon(
                            icon = Icons.Default.Info,
                            label = "AI Insight",
                            onClick = { showAiThreatDialog = true }
                        )

                        // Developer Diagnostics Menu Option
                        FloatingDockIcon(
                            icon = Icons.Default.Settings,
                            label = "Diagnostics",
                            onClick = { showDevDiagnosticsDialog = true }
                        )
                    }
                }

                // Dock Main Controller Hub Button Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ◎ Scan Controller Button
                    FloatingDockIcon(
                        icon = if (uiState.isScanningActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        label = if (uiState.isScanningActive) "SCANNING" else "PAUSED",
                        isActive = uiState.isScanningActive,
                        activeColor = Color(0xFF00FF66),
                        onClick = onToggleScanning
                    )

                    // ◉ Target Finder & Audio Sonar Button
                    FloatingDockIcon(
                        icon = if (uiState.isAudioSonarActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        label = "SONAR",
                        isActive = uiState.isAudioSonarActive,
                        activeColor = Color(0xFF00E5FF),
                        onClick = onToggleAudioSonar
                    )

                    // ⌖ Recenter Camera / Reset Transforms Button
                    FloatingDockIcon(
                        icon = Icons.Default.MyLocation,
                        label = "RECENTER",
                        onClick = {
                            recenterTriggerCount++
                        }
                    )

                    // ≡ Menu Toggle Button
                    FloatingDockIcon(
                        icon = if (isDockExpanded) Icons.Default.Close else Icons.Default.Menu,
                        label = "TOOLS",
                        isActive = isDockExpanded,
                        activeColor = Color(0xFF00FF66),
                        onClick = { isDockExpanded = !isDockExpanded }
                    )
                }
            }
        }

        // Floating horizontal scrollable list overlay of currently detected devices
        AnimatedVisibility(
            visible = !isSheetExpanded && filteredBlips.isNotEmpty(),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 68.dp) // Sits perfectly above the collapsed 56.dp bottom sheet
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DETECTED DEVICES IN RANGE (${filteredBlips.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF00FF66).copy(alpha = 0.8f)
                    )
                    Text(
                        text = "TAP TO LOCK TARGET",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.Gray
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("floating_detected_devices_row")
                ) {
                    items(filteredBlips) { blip ->
                        val isSelected = blip.id == selectedTargetId
                        val nodeColor = when (blip.type.uppercase()) {
                            "WIFI" -> Color(0xFF00FF66)
                            "CELLULAR" -> Color(0xFFFF3366)
                            "BLE" -> Color(0xFF00E5FF)
                            "MAGNETIC" -> Color(0xFFFF00FF)
                            else -> Color(0xFFFFCC00)
                        }

                        Surface(
                            onClick = {
                                if (isSelected) {
                                    onSelectTargetDevice(null)
                                } else {
                                    onSelectTargetDevice(blip.id)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF00FF66).copy(alpha = 0.15f) else Color(0xFF030705).copy(alpha = 0.85f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF00FF66) else Color(0xFF00FF66).copy(alpha = 0.25f)
                            ),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .width(150.dp)
                                .testTag("detected_device_card_${blip.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Device Type Indicator / Badge
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = nodeColor.copy(alpha = 0.15f),
                                        border = BorderStroke(0.5.dp, nodeColor.copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            text = blip.type.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = nodeColor,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }

                                    // Signal strength (RSSI)
                                    Text(
                                        text = "${blip.rssi} dBm",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (blip.rssi > -60) Color(0xFF00FF66) else Color.White
                                    )
                                }

                                Text(
                                    text = blip.name,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = blip.distance.toFeetString(1),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp
                                        ),
                                        color = Color.LightGray
                                    )

                                    if (isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = Color(0xFFFFCC00),
                                        ) {
                                            Text(
                                                text = "LOCKED",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.Black
                                                ),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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

        // LAYER 5: Swipeable Glassmorphic Target Information Bottom Sheet
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(if (isSheetExpanded) 420.dp else 56.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color(0xFF030705).copy(alpha = 0.90f),
            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.35f)),
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Drag Handle / Collapsed Ribbon Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { isSheetExpanded = !isSheetExpanded }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isSheetExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = "Toggle Sheet",
                            tint = Color(0xFF00FF66)
                        )
                        if (selectedBlip != null) {
                            Text(
                                text = "LOCKED: ${selectedBlip.name}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF00FF66).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "${selectedBlip.rssi} dBm",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF00FF66),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "NO TARGET LOCKED • SELECT EMITTER TO BEGIN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.Gray
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "DEVICES: ${uiState.activeBlips.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = Color.LightGray
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (uiState.isScanningActive) Color(0xFF00FF66) else Color.Red,
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }
                }

                if (isSheetExpanded) {
                    HorizontalDivider(color = Color(0xFF00FF66).copy(alpha = 0.25f))

                    // Detail area contents
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (selectedBlip != null) {
                            // Target lock summary detail
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = selectedBlip.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "BSSID: [${selectedBlip.id}] • CH: ${selectedBlip.frequencyMhz} MHz",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        color = Color.Gray
                                    )
                                }

                                Button(
                                    onClick = { onTriggerAiPinpoint(selectedBlip) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00FF66),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = "Pinpoint", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AI PINPOINT",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                            }

                            // Sliders for dynamic alerts and perimeter breach configuration
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val currentTargetDistThreshold = targetDistanceThresholds[selectedBlip.id] ?: uiState.perimeterThresholdMeters
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Perimeter Alert Limit",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = currentTargetDistThreshold.toFeetString(1),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = Color(0xFF00FF66)
                                    )
                                }
                                Slider(
                                    value = currentTargetDistThreshold,
                                    onValueChange = { onSetTargetDistanceThreshold(selectedBlip.id, it) },
                                    valueRange = 1.0f..15.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF00FF66),
                                        activeTrackColor = Color(0xFF00FF66),
                                        inactiveTrackColor = Color(0xFF152A1D)
                                    )
                                )

                                val currentTargetRssiThreshold = targetRssiThresholds[selectedBlip.id] ?: uiState.rssiAlertThresholdDbm
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "RSSI Alert Threshold",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = "${currentTargetRssiThreshold} dBm",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = Color(0xFF00E5FF)
                                    )
                                }
                                Slider(
                                    value = currentTargetRssiThreshold.toFloat(),
                                    onValueChange = { onSetTargetRssiThreshold(selectedBlip.id, it.toInt()) },
                                    valueRange = -95.0f..-40.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF00E5FF),
                                        activeTrackColor = Color(0xFF00E5FF),
                                        inactiveTrackColor = Color(0xFF152A1D)
                                    )
                                )
                            }
                        }

                        // Search box to filter device catalog
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("SEARCH CHANNELS / BSSID / NAME", color = Color.Gray, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00FF66),
                                unfocusedBorderColor = Color(0xFF00FF66).copy(alpha = 0.3f),
                                focusedContainerColor = Color(0xFF040A07),
                                unfocusedContainerColor = Color(0xFF020503)
                            ),
                            singleLine = true
                        )

                        // Unified BLE Catalog & Emitter Database List
                        Text(
                            text = "ACTIVE EMITTER CATALOG",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            color = Color(0xFF00FF66)
                        )

                        filteredBlips.forEach { blip ->
                            val isBlipSelected = blip.id == selectedTargetId
                            Surface(
                                onClick = { onSelectTargetDevice(blip.id) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isBlipSelected) Color(0xFF05110A) else Color(0xFF030705),
                                border = BorderStroke(1.dp, if (isBlipSelected) Color(0xFF00FF66) else Color.Gray.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = blip.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${blip.type} • ${blip.frequencyMhz} MHz • DIST: ${blip.distance.toFeetString(1)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color.Gray
                                        )
                                    }

                                    Text(
                                        text = "${blip.rssi} dBm",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black
                                        ),
                                        color = if (blip.rssi > -60) Color(0xFF00FF66) else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // LAYER 6: Non-Blocking Overlay Dialogs
    
    // 1. Developer Diagnostics Overlay dialog
    if (showDevDiagnosticsDialog) {
        Dialog(onDismissRequest = { showDevDiagnosticsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF05110A),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "DEVELOPER DIAGNOSTICS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF00FF66)
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DiagnosticMetricRow("SCAN RATE", "60 FPS (HW OVERCLOCK)")
                        DiagnosticMetricRow("IMSI RISK", uiState.imsiCatcherAlert.severity)
                        DiagnosticMetricRow("MAGNETOMETER", "%.1f µT".format(uiState.magnetometerData.totalMicroTesla))
                        DiagnosticMetricRow("CALIBRATION STATE", "${uiState.compassAccuracyScore}% STABLE")
                        DiagnosticMetricRow("ACTIVE SENSORS", "${uiState.activeAntennaCount} INTERFACES")
                        DiagnosticMetricRow("ACOUSTIC FREQ", "${uiState.acousticData.dominantFrequencyHz} Hz")
                    }

                    Button(
                        onClick = { showDevDiagnosticsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66), contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CLOSE DIAGNOSTICS")
                    }
                }
            }
        }
    }

    // 2. AI Threat Assessment Overlay Dialog
    if (showAiThreatDialog) {
        Dialog(onDismissRequest = { showAiThreatDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF05110A),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "GEMINI AI ANALYSIS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF00FF66)
                    )

                    Button(
                        onClick = { onRunAiDeepAudit() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66), contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("RUN DEEP SIGNAL AUDIT")
                    }

                    Text(
                        text = uiState.threatAnalysisReport?.executiveSummary ?: "Evidence is ready. Trigger Deep Signal Audit to let Gemini cross-correlate active RF fingerprints and check for anomalies.",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.LightGray
                    )

                    Button(
                        onClick = { showAiThreatDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f), contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("BACK TO RADAR")
                    }
                }
            }
        }
    }

    // 3. Layers overlay configuration dialog
    if (showLayersConfigDialog) {
        Dialog(onDismissRequest = { showLayersConfigDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF05110A),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "RADAR GRID LAYERS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF00FF66)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Concentric Coverage Rings", color = Color.White, fontSize = 12.sp)
                        Switch(
                            checked = uiState.showCoverageRings,
                            onCheckedChange = { onToggleHudDeclutter() } // toggles or triggers configuration
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Distance Labels / Tick Tocks", color = Color.White, fontSize = 12.sp)
                        Switch(
                            checked = uiState.showDistanceTicks,
                            onCheckedChange = { onCycleRadarGridMode() }
                        )
                    }

                    Button(
                        onClick = { showLayersConfigDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66), contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("APPLY LAYERS")
                    }
                }
            }
        }
    }
}

@Composable
fun GeographicMapView(
    headingDegrees: Float,
    filteredBlips: List<RadarBlip>,
    uiState: SignalRadarUiState,
    mapAlpha: Float,
    onZoomInMap: () -> Unit,
    onZoomOutMap: () -> Unit,
    onSetMapRange: (Float) -> Unit,
    onSelectTargetDevice: (String?) -> Unit,
    onToggleFloorplan: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onSetMaxDevices: (Int) -> Unit,
    onCycleRadarBoost: () -> Unit,
    onTriggerAiPinpoint: (RadarBlip) -> Unit,
    onCycleRadarGridMode: () -> Unit,
    onOpenRadarGridConfig: () -> Unit,
    recenterTriggerCount: Int
) {
    TacticalRadarCanvas(
        headingDegrees = headingDegrees,
        blips = filteredBlips,
        nearestBlipId = uiState.nearestBlip?.id,
        selectedTargetDeviceId = uiState.selectedTargetDeviceId,
        perimeterThresholdMeters = uiState.perimeterThresholdMeters,
        mapRangeMeters = uiState.mapRangeMeters,
        isMapMaximized = true,
        isHudDeclutterEnabled = uiState.isHudDeclutterEnabled,
        isFocusModeEnabled = uiState.isFocusModeEnabled,
        maxVisibleDevices = uiState.maxVisibleDevices,
        radarBoostLevel = uiState.radarBoostLevel,
        radarGridMode = RadarGridMode.OFF, // Force radar grids OFF for pure geographical/CAD map feel
        radarGridOpacity = 0f, // No radar grid
        showCoverageRings = false, // No coverage rings
        showDistanceTicks = false, // No distance ticks
        onZoomIn = onZoomInMap,
        onZoomOut = onZoomOutMap,
        onSetMapRange = onSetMapRange,
        onToggleMaximizeMap = {},
        onOpenFullScreenMap = {},
        onSelectTargetDevice = onSelectTargetDevice,
        isFloorplanEnabled = true, // Force floorplans ON
        onToggleFloorplan = onToggleFloorplan,
        onToggleFocusMode = onToggleFocusMode,
        onSetMaxDevices = onSetMaxDevices,
        onCycleRadarBoost = onCycleRadarBoost,
        onTriggerAiPinpoint = onTriggerAiPinpoint,
        onCycleRadarGridMode = onCycleRadarGridMode,
        onOpenRadarGridConfig = onOpenRadarGridConfig,
        recenterTriggerCount = recenterTriggerCount,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun SignalSweepCanvasView(
    headingDegrees: Float,
    filteredBlips: List<RadarBlip>,
    uiState: SignalRadarUiState,
    radarAlpha: Float,
    onZoomInMap: () -> Unit,
    onZoomOutMap: () -> Unit,
    onSetMapRange: (Float) -> Unit,
    onSelectTargetDevice: (String?) -> Unit,
    onToggleFloorplan: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onSetMaxDevices: (Int) -> Unit,
    onCycleRadarBoost: () -> Unit,
    onTriggerAiPinpoint: (RadarBlip) -> Unit,
    onCycleRadarGridMode: () -> Unit,
    onOpenRadarGridConfig: () -> Unit,
    recenterTriggerCount: Int
) {
    TacticalRadarCanvas(
        headingDegrees = headingDegrees,
        blips = filteredBlips,
        nearestBlipId = uiState.nearestBlip?.id,
        selectedTargetDeviceId = uiState.selectedTargetDeviceId,
        perimeterThresholdMeters = uiState.perimeterThresholdMeters,
        mapRangeMeters = uiState.mapRangeMeters,
        isMapMaximized = true,
        isHudDeclutterEnabled = uiState.isHudDeclutterEnabled,
        isFocusModeEnabled = uiState.isFocusModeEnabled,
        maxVisibleDevices = uiState.maxVisibleDevices,
        radarBoostLevel = uiState.radarBoostLevel,
        radarGridMode = uiState.radarGridMode,
        radarGridOpacity = uiState.radarGridOpacity * radarAlpha,
        showCoverageRings = uiState.showCoverageRings,
        showDistanceTicks = uiState.showDistanceTicks,
        onZoomIn = onZoomInMap,
        onZoomOut = onZoomOutMap,
        onSetMapRange = onSetMapRange,
        onToggleMaximizeMap = {},
        onOpenFullScreenMap = {},
        onSelectTargetDevice = onSelectTargetDevice,
        isFloorplanEnabled = false, // Force floorplan GIS OFF for pure radar sweep
        onToggleFloorplan = onToggleFloorplan,
        onToggleFocusMode = onToggleFocusMode,
        onSetMaxDevices = onSetMaxDevices,
        onCycleRadarBoost = onCycleRadarBoost,
        onTriggerAiPinpoint = onTriggerAiPinpoint,
        onCycleRadarGridMode = onCycleRadarGridMode,
        onOpenRadarGridConfig = onOpenRadarGridConfig,
        recenterTriggerCount = recenterTriggerCount,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun HybridRadarMapView(
    headingDegrees: Float,
    filteredBlips: List<RadarBlip>,
    uiState: SignalRadarUiState,
    radarAlpha: Float,
    mapAlpha: Float,
    onZoomInMap: () -> Unit,
    onZoomOutMap: () -> Unit,
    onSetMapRange: (Float) -> Unit,
    onSelectTargetDevice: (String?) -> Unit,
    onToggleFloorplan: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onSetMaxDevices: (Int) -> Unit,
    onCycleRadarBoost: () -> Unit,
    onTriggerAiPinpoint: (RadarBlip) -> Unit,
    onCycleRadarGridMode: () -> Unit,
    onOpenRadarGridConfig: () -> Unit,
    recenterTriggerCount: Int
) {
    TacticalRadarCanvas(
        headingDegrees = headingDegrees,
        blips = filteredBlips,
        nearestBlipId = uiState.nearestBlip?.id,
        selectedTargetDeviceId = uiState.selectedTargetDeviceId,
        perimeterThresholdMeters = uiState.perimeterThresholdMeters,
        mapRangeMeters = uiState.mapRangeMeters,
        isMapMaximized = true,
        isHudDeclutterEnabled = uiState.isHudDeclutterEnabled,
        isFocusModeEnabled = uiState.isFocusModeEnabled,
        maxVisibleDevices = uiState.maxVisibleDevices,
        radarBoostLevel = uiState.radarBoostLevel,
        radarGridMode = uiState.radarGridMode,
        radarGridOpacity = uiState.radarGridOpacity * radarAlpha,
        showCoverageRings = uiState.showCoverageRings,
        showDistanceTicks = uiState.showDistanceTicks,
        onZoomIn = onZoomInMap,
        onZoomOut = onZoomOutMap,
        onSetMapRange = onSetMapRange,
        onToggleMaximizeMap = {},
        onOpenFullScreenMap = {},
        onSelectTargetDevice = onSelectTargetDevice,
        isFloorplanEnabled = uiState.isFloorplanEnabled && (mapAlpha > 0.5f),
        onToggleFloorplan = onToggleFloorplan,
        onToggleFocusMode = onToggleFocusMode,
        onSetMaxDevices = onSetMaxDevices,
        onCycleRadarBoost = onCycleRadarBoost,
        onTriggerAiPinpoint = onTriggerAiPinpoint,
        onCycleRadarGridMode = onCycleRadarGridMode,
        onOpenRadarGridConfig = onOpenRadarGridConfig,
        recenterTriggerCount = recenterTriggerCount,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun TinyWarningBadge(text: String, tooltip: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            ),
            color = color,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun SensorStatusIcon(name: String, isActive: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (isActive) Color(0xFF00FF66).copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, if (isActive) Color(0xFF00FF66).copy(alpha = 0.4f) else Color.Red.copy(alpha = 0.3f)),
        modifier = Modifier.size(18.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.take(1),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                ),
                color = if (isActive) Color(0xFF00FF66) else Color.Red
            )
        }
    }
}

@Composable
fun FloatingDockIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean = false,
    activeColor: Color = Color(0xFF00FF66),
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (isActive) activeColor.copy(alpha = 0.25f) else Color(0xFF05110A).copy(alpha = 0.85f),
            border = BorderStroke(1.dp, if (isActive) activeColor else Color(0xFF00FF66).copy(alpha = 0.4f)),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) activeColor else Color(0xFF00FF66),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun DiagnosticMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}
@Composable
fun FilterChipsRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("ALL", "WIFI", "CELLULAR", "BLE", "MAGNETIC", "AUDIO", "OFF")

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(filters) { filter ->
            val isSelected = filter == selectedFilter
            val isOffChip = filter == "OFF"

            val containerColor = when {
                isSelected && isOffChip -> Color(0xFFFF2A55).copy(alpha = 0.25f)
                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }

            val strokeColor = when {
                isSelected && isOffChip -> Color(0xFFFF2A55)
                isSelected -> MaterialTheme.colorScheme.primary
                else -> Color.Transparent
            }

            val textColor = when {
                isSelected && isOffChip -> Color(0xFFFF2A55)
                isSelected -> MaterialTheme.colorScheme.primary
                isOffChip -> Color(0xFFFF2A55).copy(alpha = 0.6f)
                else -> Color.Gray
            }

            Surface(
                onClick = { onFilterSelected(filter) },
                shape = RoundedCornerShape(8.dp),
                color = containerColor,
                border = BorderStroke(1.dp, strokeColor),
                modifier = Modifier.testTag("filter_chip_$filter")
            ) {
                Text(
                    text = if (isOffChip) "OFF (MUTE ALL)" else filter,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = textColor,
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
    sonarState: SonarState = SonarState.IDLE,
    uiState: SignalRadarUiState,
    onUnlockTarget: () -> Unit,
    onToggleAudioSonar: () -> Unit,
    onOpenCalibration: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    CompassDeviceFinderCard(
        sonarState = sonarState,
        uiState = uiState,
        onSelectTargetDevice = { id -> if (id == null) onUnlockTarget() },
        onToggleAudioSonar = onToggleAudioSonar,
        onOpenCalibration = onOpenCalibration,
        modifier = modifier
    )
}

@Composable
fun SpectrumInterceptCard(
    blip: RadarBlip,
    perimeterThresholdMeters: Float,
    selectedTargetDeviceId: String? = null,
    onSelectTargetDevice: ((String?) -> Unit)? = null,
    onInterrogateGatt: ((RadarBlip) -> Unit)? = null,
    correlations: List<CorrelationEvent> = emptyList()
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
            when {
                isSelectedTarget -> Color(0xFF00FF66)
                blip.baselineState == BaselineState.ANOMALOUS -> Color(0xFFFF3366)
                blip.baselineState == BaselineState.CHANGED -> Color(0xFFFF8800)
                blip.baselineState == BaselineState.NEW -> Color(0xFFFFCC00)
                blip.distance < perimeterThresholdMeters -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            }
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
                        if (blip.baselineState != BaselineState.UNKNOWN) {
                            val stateColor = when (blip.baselineState) {
                                BaselineState.KNOWN -> Color(0xFF00E5FF)
                                BaselineState.NEW -> Color(0xFFFFCC00)
                                BaselineState.CHANGED -> Color(0xFFFF8800)
                                BaselineState.ANOMALOUS -> Color(0xFFFF3366)
                                else -> Color.Gray
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = stateColor.copy(alpha = 0.2f), border = BorderStroke(1.dp, stateColor)) {
                                Text(
                                    text = blip.baselineState.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace),
                                    color = stateColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
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
                            text = "${String.format("%.1f", blip.distance * 3.28084f)}m",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (blip.distance < perimeterThresholdMeters) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (blip.type.contains("BLE", ignoreCase = true) && onInterrogateGatt != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF003344))
                                .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                                .clickable { onInterrogateGatt.invoke(blip) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("interrogate_gatt_button_${blip.id}")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = "Interrogate GATT",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "INTERROGATE GATT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF00E5FF)
                                )
                            }
                        }
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
            if (blip.fingerprintId != null) {
                val conf = blip.fingerprintConfidence ?: 0f
                val confColor = if (conf > 0.9f) Color(0xFF00FF66) else if (conf > 0.7f) Color(0xFFFFCC00) else Color(0xFFFF3366)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).background(Color(0xFF0A1A10), RoundedCornerShape(4.dp)).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "SIGNAL FINGERPRINT", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = Color.Gray)
                        Text(text = blip.fingerprintId.take(8).uppercase(), style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = Color(0xFF00E5FF))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Match confidence: ${String.format("%.2f", conf)}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace), color = confColor)
                        Text(text = if (conf > 0.8f) "Possible persistent emitter" else "New signal pattern", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                    }
                }
            }
            if (isSelectedTarget && blip.anomalyResult != null) {
                val anomaly = blip.anomalyResult
                val categoryColor = when (anomaly.category) {
                    AnomalyCategory.HIGH_DEVIATION -> Color(0xFFFF3366)
                    AnomalyCategory.MODERATE_DEVIATION -> Color(0xFFFF8800)
                    AnomalyCategory.LOW_DEVIATION -> Color(0xFFFFCC00)
                    AnomalyCategory.NORMAL -> Color(0xFF00FF66)
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "ANOMALY EXPLANATION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "${anomaly.score} / 100", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = categoryColor)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Confidence: ${String.format("%.0f", anomaly.confidence * 100)}% | Category: ${anomaly.category.name.replace("_", " ")}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                        if (anomaly.previousScore != null) {
                            Text(text = "Prev: ${anomaly.previousScore}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                        }
                    }
                    
                    if (anomaly.explanations.isNotEmpty()) {
                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                        anomaly.explanations.forEach { exp ->
                            val sign = if (exp.scoreImpact > 0) "+" else ""
                            val impactColor = if (exp.scoreImpact > 0) Color(0xFFFF8800) else if (exp.scoreImpact < 0) Color(0xFF00FF66) else Color.Gray
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = exp.description, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "$sign${exp.scoreImpact}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp), color = impactColor)
                            }
                        }
                    }
                }
            }
            if (isSelectedTarget && correlations.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).background(Color(0xFF111A22), RoundedCornerShape(6.dp)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "CORRELATED EVENTS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = Color(0xFF00E5FF))
                    correlations.forEach { event ->
                        val otherObs = event.observations.firstOrNull { it.id != blip.id }
                        if (otherObs != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(text = otherObs.type, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 10.sp), color = Color.White)
                                    Text(text = "Δt: ${event.maxTimeSeparationMs}ms", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp), color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Score: ${String.format("%.2f", event.correlationScore)}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp), color = if (event.correlationScore > 0.7f) Color(0xFF00FF66) else Color(0xFFFFCC00))
                                    Text(text = event.spatialRelationship.name, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp), color = Color.Gray)
                                }
                            }
                            Text(text = event.notes, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp), color = Color.DarkGray)
                            androidx.compose.material3.HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun SpectrumAnalyzerScreen(
    sonarState: SonarState = SonarState.IDLE,
    uiState: SignalRadarUiState,
    onFilterSelected: (String) -> Unit,
    onSelectTargetDevice: (String?) -> Unit = {},
    onToggleAudioSonar: () -> Unit = {},
    onOpenArCameraForTarget: (String) -> Unit = {},
    onOpenCalibration: () -> Unit = {},
    onInterrogateGatt: ((RadarBlip) -> Unit)? = null,
    onToggleLearning: () -> Unit = {},
    onResetBaseline: () -> Unit = {},
    windowSizeClass: WindowSizeClass = rememberWindowSizeClass()
) {
    val filteredBlips = rememberFilteredBlips(
        blips = uiState.activeBlips,
        filterType = uiState.selectedFilterType,
        maxDevices = uiState.maxVisibleDevices,
        isFocusMode = uiState.isFocusModeEnabled,
        minRssiDbm = uiState.minRssiFilterDbm,
        selectedTargetId = uiState.selectedTargetDeviceId,
        sortBy = uiState.sortByPriority
    )

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
                EnvironmentalBaselineCard(
                    summary = uiState.baselineSummary,
                    onToggleLearning = onToggleLearning,
                    onResetBaseline = onResetBaseline
                )
                if (uiState.selectedTargetDeviceId != null) {
                    PinpointDeviceHUDCard(
                        sonarState = sonarState,
                        uiState = uiState,
                        onUnlockTarget = { onSelectTargetDevice(null) },
                        onToggleAudioSonar = onToggleAudioSonar,
                        onOpenCalibration = onOpenCalibration
                    )
                }
                RealTimeSignalStrengthChartCard(
                    activeBlips = uiState.activeBlips,
                    selectedTargetDeviceId = uiState.selectedTargetDeviceId,
                    onSelectTargetDevice = onSelectTargetDevice,
                    onOpenArCameraForTarget = onOpenArCameraForTarget
                )
                PhoneAntennaArrayCard(telemetryList = uiState.antennaArrayTelemetry)
                ThermalFusionCard(uiState = uiState)
                LargeSpectrumVisualizerCard(
                    activeBlips = uiState.activeBlips,
                    selectedTargetDeviceId = uiState.selectedTargetDeviceId,
                    onSelectTargetDevice = onSelectTargetDevice
                )
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
                            onSelectTargetDevice = onSelectTargetDevice,
                            onInterrogateGatt = onInterrogateGatt,
                            correlations = uiState.correlationEvents.filter { event -> event.observations.any { it.id == blip.id } }
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
            item {
                EnvironmentalBaselineCard(
                    summary = uiState.baselineSummary,
                    onToggleLearning = onToggleLearning,
                    onResetBaseline = onResetBaseline
                )
            }
            if (uiState.selectedTargetDeviceId != null) {
                item {
                    PinpointDeviceHUDCard(
                        sonarState = sonarState,
                        uiState = uiState,
                        onUnlockTarget = { onSelectTargetDevice(null) },
                        onToggleAudioSonar = onToggleAudioSonar,
                        onOpenCalibration = onOpenCalibration
                    )
                }
            }

            item {
                RealTimeSignalStrengthChartCard(
                    activeBlips = uiState.activeBlips,
                    selectedTargetDeviceId = uiState.selectedTargetDeviceId,
                    onSelectTargetDevice = onSelectTargetDevice,
                    onOpenArCameraForTarget = onOpenArCameraForTarget
                )
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
                ThermalFusionCard(uiState = uiState)
            }

            item {
                LargeSpectrumVisualizerCard(
                    activeBlips = uiState.activeBlips,
                    selectedTargetDeviceId = uiState.selectedTargetDeviceId,
                    onSelectTargetDevice = onSelectTargetDevice
                )
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
                    onSelectTargetDevice = onSelectTargetDevice,
                    onInterrogateGatt = onInterrogateGatt
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
    onClearInterference: () -> Unit = {},
    onExportAcousticReportUri: (Uri) -> Unit = {}
) {
    val mag = uiState.magnetometerData
    val acoustic = uiState.acousticData
    val reportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let { onExportAcousticReportUri(it) }
    }

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

                    Button(
                        onClick = {
                            reportLauncher.launch("acoustic_fft_spectrogram_report_${System.currentTimeMillis()}.txt")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("export_acoustic_spectrogram_report_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFCC00),
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export Acoustic Spectrogram Report",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EXPORT ACOUSTIC SPECTROGRAM REPORT (18-22 kHz)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun SecurityGuardScreen(
    uiState: SignalRadarUiState,
    onThresholdChanged: (Float) -> Unit,
    onToggleAlarm: () -> Unit,
    onSnapshotTrustedBaseline: () -> Unit = {}
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Guard",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "PERIMETER SECURE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Switch(
                        checked = uiState.isPerimeterAlarmEnabled,
                        onCheckedChange = { onToggleAlarm() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.error,
                            checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Threshold Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Boundary Radius: ${uiState.perimeterThresholdMeters.toInt()} Feet",
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
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                Button(
                    onClick = onSnapshotTrustedBaseline,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B3D28))
                ) {
                    Text(
                        text = "SNAPSHOT TRUSTED BASELINE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF00FF66)
                    )
                }
                
                Text(
                    text = "Any node recorded in the baseline will not trigger perimeter intrusion alerts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

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
    onClearLogs: () -> Unit,
    onExportCsvUri: (android.net.Uri) -> Unit = {}
) {
    val context = LocalContext.current
    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { onExportCsvUri(it) }
    }

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
                        createCsvLauncher.launch("rf_spectrum_radar_log_${System.currentTimeMillis()}.csv")
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
                                RadarTab.SIMULATION_LAB -> Icons.Default.Settings
                                RadarTab.SWEEP_RADAR -> Icons.Default.Radar
                                RadarTab.FULL_RADAR -> Icons.Default.Radar
                                RadarTab.AI_THREAT_ANALYSIS -> Icons.Default.Security
                                RadarTab.SCANNER -> Icons.Default.Sensors
                                RadarTab.SPECTRUM_ANALYZER -> Icons.Default.GraphicEq
                                RadarTab.DETECTED_SENSORS -> Icons.Default.Sensors
                                RadarTab.HISTORIC_HEATMAP -> Icons.Default.Map
                                RadarTab.ENVIRONMENT_MAP -> Icons.Filled.Public
                                RadarTab.IDENTITY_GRAPH -> Icons.Default.Fingerprint
                                RadarTab.INTELLIGENCE_DASHBOARD -> Icons.Default.Hub
                                RadarTab.MAGNETOMETER_EMF -> Icons.Default.Equalizer
                                RadarTab.SECURITY_GUARD -> Icons.Default.Security
                                RadarTab.CSV_LOG_CONSOLE -> Icons.Default.Terminal
                                RadarTab.SETTINGS -> Icons.Default.Settings
                                RadarTab.EVENT_RECORDER -> Icons.Default.Info
                                RadarTab.ADAPTIVE_LOCALIZATION -> Icons.Default.MyLocation
                            },
                            contentDescription = tab.name,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = when (tab) {
                                RadarTab.SIMULATION_LAB -> "Lab"
                                RadarTab.SWEEP_RADAR -> "Sweep"
                                RadarTab.FULL_RADAR -> "Full Radar"
                                RadarTab.AI_THREAT_ANALYSIS -> "AI Intel"
                                RadarTab.SCANNER -> "Scanner"
                                RadarTab.SPECTRUM_ANALYZER -> "Spectrum"
                                RadarTab.DETECTED_SENSORS -> "Sensors"
                                RadarTab.HISTORIC_HEATMAP -> "Heatmap"
                                RadarTab.ENVIRONMENT_MAP -> "RF Map"
                                RadarTab.IDENTITY_GRAPH -> "Identity"
                                RadarTab.INTELLIGENCE_DASHBOARD -> "Intelligence"
                                RadarTab.MAGNETOMETER_EMF -> "Magneto"
                                RadarTab.SECURITY_GUARD -> "Guard"
                                RadarTab.CSV_LOG_CONSOLE -> "Log"
                                RadarTab.SETTINGS -> "Settings"
                                RadarTab.EVENT_RECORDER -> "Rec"
                                RadarTab.ADAPTIVE_LOCALIZATION -> "Localizer"
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
                                    text = "X: %.1f | Y: %.1f | Z: %.1f ft/s²".format(sensor.accelX, sensor.accelY, sensor.accelZ),
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
                                    text = "Altitude: ${sensor.estimatedAltitudeMeters.toInt()} feet ASL",
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
                                    text = "Gz: %.1f ft/s² Vertical".format(sensor.gravityZ),
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
                                    text = "Lz: %.1f ft/s² Impulse".format(sensor.linearAccelZ),
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

        // Real-Time Signal Strength Time-Series Chart
        item {
            RealTimeSignalStrengthChartCard(
                activeBlips = uiState.activeBlips,
                selectedTargetDeviceId = uiState.selectedTargetDeviceId,
                onSelectTargetDevice = {}
            )
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
                                text = "%.1f ft".format((item.distanceMeters * 3.28084f)),
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
                    text = "Grid Range: ±20 Feet",
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
fun rememberFilteredBlips(
    blips: List<RadarBlip>,
    filterType: String,
    maxDevices: Int = 10,
    isFocusMode: Boolean = false,
    minRssiDbm: Int = -95,
    selectedTargetId: String? = null,
    sortBy: String = "DISTANCE"
): List<RadarBlip> {
    return remember(blips, filterType, maxDevices, isFocusMode, minRssiDbm, selectedTargetId, sortBy) {
        if (filterType.equals("OFF", ignoreCase = true) || filterType.equals("NONE", ignoreCase = true)) {
            return@remember emptyList()
        }

        // 1. Filter by Signal Type
        var list = if (filterType.equals("ALL", ignoreCase = true)) {
            blips
        } else {
            blips.filter { it.type.equals(filterType, ignoreCase = true) }
        }

        // 2. Filter by minimum RSSI threshold (always keep selected target visible)
        list = list.filter { blip ->
            blip.id == selectedTargetId || blip.rssi >= minRssiDbm
        }

        // 3. Sort by priority
        list = when (sortBy.uppercase()) {
            "RSSI" -> list.sortedWith(
                compareByDescending<RadarBlip> { it.id == selectedTargetId }
                    .thenByDescending { it.rssi / 5 * 5 } // Bucket by 5 dBm to stabilize list layout (Goal 2)
                    .thenByDescending { it.rssi }
                    .thenBy { it.distance }
            )
            "RISK" -> list.sortedWith(
                compareByDescending<RadarBlip> { it.id == selectedTargetId }
                    .thenByDescending { it.isHighRiskVendor }
                    .thenBy { it.distance }
            )
            else -> list.sortedWith(
                compareByDescending<RadarBlip> { it.id == selectedTargetId }
                    .thenBy { (it.distance * 10f).toInt() / 5 } // Bucket distance by 50cm to prevent rapid jitter (Goal 2)
                    .thenBy { it.distance }
                    .thenByDescending { it.rssi }
            )
        }

        // 4. Focus Mode: focus on target device + top 3 nearest priority devices
        if (isFocusMode) {
            val limit = if (selectedTargetId != null) 3 else 5
            return@remember list.take(limit)
        }

        // 5. Max Devices Limit (0 = ALL)
        if (maxDevices > 0 && list.size > maxDevices) {
            val topList = list.take(maxDevices).toMutableList()
            if (selectedTargetId != null && topList.none { it.id == selectedTargetId }) {
                val target = list.find { it.id == selectedTargetId }
                if (target != null) {
                    if (topList.isNotEmpty()) topList.removeAt(topList.lastIndex)
                    topList.add(0, target)
                }
            }
            topList
        } else {
            list
        }
    }
}

@Composable
fun RadarFocusDeclutterBar(
    uiState: SignalRadarUiState,
    totalDiscoveredCount: Int,
    displayedCount: Int,
    onToggleFocusMode: () -> Unit,
    onSetMaxDevices: (Int) -> Unit,
    onSetMinRssi: (Int) -> Unit,
    onToggleHudDeclutter: () -> Unit,
    onSetSortBy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpandedControlsOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF091410), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Top Row: Focus Mode Pill, Presets, and Discovered Nodes Counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Focus Mode Pill
                Surface(
                    onClick = onToggleFocusMode,
                    shape = RoundedCornerShape(8.dp),
                    color = if (uiState.isFocusModeEnabled) Color(0xFFFFCC00) else Color(0xFF14241C),
                    border = BorderStroke(1.dp, if (uiState.isFocusModeEnabled) Color(0xFFFFE066) else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("radar_focus_mode_toggle_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Focus Mode",
                            tint = if (uiState.isFocusModeEnabled) Color.Black else Color(0xFF00FF66),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (uiState.isFocusModeEnabled) "FOCUS: ON (TOP 3)" else "FOCUS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = if (uiState.isFocusModeEnabled) Color.Black else Color(0xFF00FF66)
                        )
                    }
                }

                // Quick Max Devices Limit Presets
                val devicePresets = listOf(3, 5, 10, 25, 0)
                devicePresets.forEach { preset ->
                    val isSelected = uiState.maxVisibleDevices == preset && !uiState.isFocusModeEnabled
                    val label = if (preset == 0) "ALL" else "$preset"
                    Surface(
                        onClick = {
                            if (uiState.isFocusModeEnabled) onToggleFocusMode()
                            onSetMaxDevices(preset)
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF111E18),
                        border = BorderStroke(1.dp, if (isSelected) Color.White else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.testTag("max_devices_preset_$label")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp
                            ),
                            color = if (isSelected) Color.Black else Color.LightGray,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Right side: Active count badge & Expand Fine-tuning toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "$displayedCount/$totalDiscoveredCount NODES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF00FF66),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                IconButton(
                    onClick = { isExpandedControlsOpen = !isExpandedControlsOpen },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("toggle_declutter_slider_panel")
                ) {
                    Icon(
                        imageVector = if (isExpandedControlsOpen) Icons.Default.ExpandLess else Icons.Default.Tune,
                        contentDescription = "Fine-tune sliders",
                        tint = if (isExpandedControlsOpen) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Prominent Direct Slider: Maximum Visible Radar Devices Slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
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
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Max Devices Slider",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "MAX RADAR TARGETS LIMIT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = if (uiState.isFocusModeEnabled) "FOCUS MODE (TOP 3)" else if (uiState.maxVisibleDevices == 0) "ALL (${totalDiscoveredCount} MAX)" else "${uiState.maxVisibleDevices} TARGETS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = if (uiState.isFocusModeEnabled) Color(0xFFFFCC00) else Color.White
                )
            }

            Slider(
                value = if (uiState.isFocusModeEnabled) 3f else if (uiState.maxVisibleDevices == 0) 50f else uiState.maxVisibleDevices.toFloat(),
                onValueChange = { value ->
                    if (uiState.isFocusModeEnabled) {
                        onToggleFocusMode()
                    }
                    val intVal = value.toInt()
                    onSetMaxDevices(if (intVal >= 50) 0 else intVal)
                },
                valueRange = 1f..50f,
                steps = 48,
                colors = SliderDefaults.colors(
                    thumbColor = if (uiState.isFocusModeEnabled) Color(0xFFFFCC00) else MaterialTheme.colorScheme.primary,
                    activeTrackColor = if (uiState.isFocusModeEnabled) Color(0xFFFFCC00) else MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color(0xFF1B3024)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .testTag("max_visible_devices_slider")
            )
        }

        // Expandable Secondary Slider & Fine-Tuning Precision Controls
        AnimatedVisibility(
            visible = isExpandedControlsOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), thickness = 0.8.dp)

                // 2. Minimum Signal RSSI Cutoff Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MIN SIGNAL CUTOFF (RSSI)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Color(0xFF00E5FF)
                    )
                    Text(
                        text = if (uiState.minRssiFilterDbm <= -95) "ALL SIGNALS (-95 dBm)" else "${uiState.minRssiFilterDbm} dBm",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Color.White
                    )
                }

                Slider(
                    value = uiState.minRssiFilterDbm.toFloat(),
                    onValueChange = { onSetMinRssi(it.toInt()) },
                    valueRange = -95f..-50f,
                    steps = 44,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .testTag("min_rssi_cutoff_slider")
                )

                // 3. Toggles: Smart HUD Label Declutter & Priority Sort
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = onToggleHudDeclutter,
                            shape = RoundedCornerShape(6.dp),
                            color = if (uiState.isHudDeclutterEnabled) Color(0xFF00FF66).copy(alpha = 0.2f) else Color(0xFF18221D),
                            border = BorderStroke(1.dp, if (uiState.isHudDeclutterEnabled) Color(0xFF00FF66) else Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("hud_declutter_toggle_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = "Declutter Labels",
                                    tint = if (uiState.isHudDeclutterEnabled) Color(0xFF00FF66) else Color.Gray,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (uiState.isHudDeclutterEnabled) "LABELS: CLEAN HUD" else "LABELS: SHOW ALL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (uiState.isHudDeclutterEnabled) Color(0xFF00FF66) else Color.LightGray
                                )
                            }
                        }
                    }

                    // Sort By selector chips
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("DISTANCE" to "DIST", "RSSI" to "RSSI", "RISK" to "RISK").forEach { (sortKey, label) ->
                            val isSelected = uiState.sortByPriority == sortKey
                            Surface(
                                onClick = { onSetSortBy(sortKey) },
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) Color(0xFFFFCC00) else Color(0xFF121B16),
                                border = BorderStroke(0.8.dp, if (isSelected) Color.Yellow else Color.DarkGray),
                                modifier = Modifier.testTag("sort_by_chip_$label")
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isSelected) Color.Black else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
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
                text = "BREACH <%.1f ft".format(breachMeters),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFFF3366)
            )
            Text(
                text = "WARNING <%.1f ft".format(warningMeters),
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
            maxRangeMeters = "35 - 100 feet",
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
            maxRangeMeters = "10 - 30 feet",
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
            maxRangeMeters = "1,500 - 10,000 feet",
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
            maxRangeMeters = "0.05 - 1.5 feet",
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
            maxRangeMeters = "3 - 12 feet",
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
@Composable
fun EnvironmentalBaselineCard(
    summary: BaselineSummary,
    onToggleLearning: () -> Unit,
    onResetBaseline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ENVIRONMENT BASELINE",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Controls
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (summary.isLearning) Color(0xFF00FF66).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (summary.isLearning) Color(0xFF00FF66) else MaterialTheme.colorScheme.outline),
                        modifier = Modifier.clickable { onToggleLearning() }
                    ) {
                        Text(
                            text = if (summary.isLearning) "LEARNING ACTIVE" else "PAUSED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                            color = if (summary.isLearning) Color(0xFF00FF66) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { onResetBaseline() }
                    ) {
                        Text(
                            text = "RESET",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Known Fingerprints: ${summary.knownFingerprints}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                    Text(text = "New Fingerprints: ${summary.newFingerprints}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                    Text(text = "Missing Fingerprints: ${summary.missingFingerprints}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                    val actSign = if (summary.rfActivityDeltaPercent >= 0) "+" else ""
                    val freqSign = if (summary.freqOccupancyDeltaPercent >= 0) "+" else ""
                    Text(text = "RF Activity: $actSign${String.format("%.1f", summary.rfActivityDeltaPercent * 100)}%", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                    Text(text = "Frequency Occupancy: $freqSign${String.format("%.1f", summary.freqOccupancyDeltaPercent * 100)}%", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                    Text(text = "Baseline Confidence: ${String.format("%.2f", summary.baselineConfidence)}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                }
            }
            
            Text(
                text = "Age: ${summary.baselineAgeMs / 1000 / 60} min | Data Points: ${summary.observationsCollected}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray)
            )
        }
    }
}
