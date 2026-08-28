package com.example

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
    onExportLogsCsv: () -> Unit = {},
    onExportKmlBreadcrumbs: () -> Unit = {},
    onExportLogsCsvUri: (Uri) -> Unit = {},
    onExportKmlBreadcrumbsUri: (Uri) -> Unit = {},
    onPurgeHistory: () -> Unit,
    onOpenCalibration: () -> Unit = {},
    onSnapshotTrustedBaseline: () -> Unit = {},
    onSetMaxDevices: (Int) -> Unit = {},
    onTestGeminiConnection: () -> Unit = {},
    onTestNetworkSpeed: () -> Unit = {},
    onSetAiMode: (AiInferenceMode) -> Unit = {},
    onSaveGeminiKey: (String) -> Unit = {},
    onClearGeminiKey: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var isLocalModelManagerOpen by remember { mutableStateOf(false) }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { onExportLogsCsvUri(it) }
    }

    val kmlExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.google-earth.kml+xml")
    ) { uri ->
        uri?.let { onExportKmlBreadcrumbsUri(it) }
    }

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

                    // Maximum Rendered Targets Limit (DataStore Persisted)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF06150D), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Max Visible Radar Targets",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "Limits simultenously rendered blips to eliminate scope clutter (Saved in DataStore)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = if (uiState.maxVisibleDevices == 0) "ALL (UNLIMITED)" else "${uiState.maxVisibleDevices} TARGETS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF00FF66)
                            )
                        }

                        Slider(
                            value = if (uiState.maxVisibleDevices == 0) 50f else uiState.maxVisibleDevices.toFloat(),
                            onValueChange = { value ->
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
                            modifier = Modifier.testTag("settings_max_devices_slider")
                        )

                        // Preset chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PRESETS:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.Gray
                            )
                            listOf(3, 5, 10, 25, 50, 0).forEach { preset ->
                                val isSelected = uiState.maxVisibleDevices == preset
                                val label = if (preset == 0) "ALL" else "$preset"
                                Surface(
                                    onClick = { onSetMaxDevices(preset) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Color(0xFF00FF66) else Color(0xFF0B2114),
                                    border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF00FF66).copy(alpha = 0.3f)),
                                    modifier = Modifier.testTag("settings_preset_$label")
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp
                                        ),
                                        color = if (isSelected) Color.Black else Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
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

            // Automated Figure-Eight Compass & AR Calibration Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF041409)),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "AUTOMATED FIGURE-EIGHT CALIBRATION",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Refines geomagnetic compass declination & AR spatial accuracy matrices (${uiState.compassAccuracyScore}% accuracy)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFF00FF66)
                        )
                    }

                    Button(
                        onClick = onOpenCalibration,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00FF66),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("settings_run_figure8_calibration_button")
                    ) {
                        Text(
                            text = "CALIBRATE ♾️",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }

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

            // Section 4: AI Gateway & Multi-Model Inference Co-Pilot
            SettingsSectionHeader(
                icon = Icons.Default.Security,
                title = "AI GATEWAY & CO-PILOT CONFIG",
                subtitle = "Manage local LLM models (llama.cpp) & Gemini cloud integrations securely"
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F13)),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("ai_gateway_diagnostics_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Inference Mode Selector
                    Text(
                        text = "AI INFERENCE MODE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiInferenceMode.values().forEach { mode ->
                            val selected = uiState.aiInferenceMode == mode
                            val label = when (mode) {
                                AiInferenceMode.GEMINI_CLOUD -> "Gemini Cloud"
                                AiInferenceMode.LOCAL_GGUF -> "Local GGUF"
                                AiInferenceMode.AUTO_HYBRID -> "Auto Hybrid"
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { onSetAiMode(mode) },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00FF66),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color.Black.copy(alpha = 0.4f),
                                    labelColor = Color.LightGray
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    selectedBorderColor = Color(0xFF00FF66),
                                    borderColor = Color(0xFF00FF66).copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.testTag("ai_mode_chip_${mode.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // API Key Settings (Password-masked with toggle visibility, Save/Clear buttons)
                    Text(
                        text = "GEMINI CLOUD API KEY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )

                    var apiKeyText by remember { mutableStateOf("") }
                    var isApiKeyVisible by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        modifier = Modifier.fillMaxWidth().testTag("gemini_key_text_field"),
                        label = {
                            Text(
                                "Enter API Key...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        ),
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val icon = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            val description = if (isApiKeyVisible) "Hide password" else "Show password"
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(imageVector = icon, contentDescription = description, tint = Color(0xFF00FF66))
                            }
                        },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF66),
                            unfocusedBorderColor = Color(0xFF00FF66).copy(alpha = 0.5f),
                            cursorColor = Color(0xFF00FF66)
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (apiKeyText.isNotBlank()) {
                                    onSaveGeminiKey(apiKeyText)
                                    apiKeyText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                            modifier = Modifier.weight(1f).testTag("save_api_key_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SAVE KEY", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                        }

                        OutlinedButton(
                            onClick = {
                                onClearGeminiKey()
                                apiKeyText = ""
                            },
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            modifier = Modifier.weight(1f).testTag("clear_api_key_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("CLEAR KEY", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.Red)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status indicators (Gemini & Local GGUF status)
                    Text(
                        text = "ENGINE PLATFORM STATUS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Gemini Configured Status
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (uiState.geminiApiKeyExists) Color(0xFF00FF66) else Color.Gray,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                                Text(
                                    text = if (uiState.geminiApiKeyExists) "Gemini Cloud: Configured" else "Gemini Cloud: Not Configured",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    color = if (uiState.geminiApiKeyExists) Color(0xFF00FF66) else Color.Gray
                                )
                            }

                            // Local GGUF Status
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isLocalLoaded = uiState.localModelStatus.contains("Loaded")
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (isLocalLoaded) Color(0xFF00FF66) else Color(0xFFFFCC00),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                                Text(
                                    text = uiState.localModelStatus,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    color = if (isLocalLoaded) Color(0xFF00FF66) else Color(0xFFFFCC00)
                                )
                            }
                        }
                    }

                    // Diagnostics section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DIAGNOSTIC API TELEMETRY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )

                        val (stateLabel, stateColor) = when (val cs = uiState.geminiConnectionState) {
                            is GeminiConnectionState.NotConfigured -> "NOT CONFIGURED" to Color.Gray
                            is GeminiConnectionState.Testing -> "TESTING..." to Color(0xFFFFCC00)
                            is GeminiConnectionState.Connected -> "CONNECTED" to Color(0xFF00FF66)
                            is GeminiConnectionState.AuthenticationError -> "AUTH ERROR" to Color(0xFFFF4444)
                            is GeminiConnectionState.HttpError -> "HTTP ERROR" to Color(0xFFFF4444)
                            is GeminiConnectionState.NetworkError -> "NETWORK ERROR" to Color(0xFFFFCC00)
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = stateColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, stateColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = stateLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = stateColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val connectionDetail = when (val cs = uiState.geminiConnectionState) {
                        is GeminiConnectionState.NotConfigured -> "No API Key has been loaded. Set GEMINI_API_KEY to enable real-time tactical summaries and audits."
                        is GeminiConnectionState.Testing -> "Pinging Gemini API server to verify host reachability and credentials..."
                        is GeminiConnectionState.Connected -> "Successfully connected using model '${cs.model}'. Real-time threat auditing and tactical co-pilot are fully operational."
                        is GeminiConnectionState.AuthenticationError -> "Authorization failed (Code ${cs.code}): ${cs.message}. Check your API Key."
                        is GeminiConnectionState.HttpError -> "HTTP exception occurred (Code ${cs.code}): ${cs.message}."
                        is GeminiConnectionState.NetworkError -> "Network/Socket Exception: ${cs.message}. Check your connectivity."
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = connectionDetail,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            ),
                            color = Color.LightGray,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    val isTesting = uiState.geminiConnectionState is GeminiConnectionState.Testing
                    Button(
                        onClick = onTestGeminiConnection,
                        enabled = !isTesting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("settings_test_gemini_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTesting) Color(0xFF1B3B26) else Color(0xFF00FF66),
                            disabledContainerColor = Color(0xFF12261A)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isTesting) "DIAGNOSING CONNECTION..." else "TEST GEMINI API REACHABILITY",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (isTesting) Color(0xFF00FF66).copy(alpha = 0.5f) else Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "DIAGNOSTIC NETWORK SPEED & CONNECTIVITY TEST",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )

                    val isNetworkTesting = uiState.networkSpeedTestResult == "TESTING..."
                    val speedTestStatusColor = when {
                        uiState.networkSpeedTestResult == null -> Color.Gray
                        uiState.networkSpeedTestResult.startsWith("SUCCESS") -> Color(0xFF00FF66)
                        uiState.networkSpeedTestResult.startsWith("FAIL") -> Color(0xFFFF4444)
                        else -> Color(0xFFFFCC00)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, speedTestStatusColor.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.networkSpeedTestResult ?: "Select 'Run Network Speed Test' below to measure connectivity latency via Standard Google generate_204 HTTP endpoint.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            ),
                            color = if (uiState.networkSpeedTestResult == null) Color.LightGray else speedTestStatusColor,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Button(
                        onClick = onTestNetworkSpeed,
                        enabled = !isNetworkTesting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("settings_test_network_speed_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNetworkTesting) Color(0xFF1B3B26) else Color(0xFF00FF66),
                            disabledContainerColor = Color(0xFF12261A)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isNetworkTesting) "MEASURING BANDWIDTH/LATENCY..." else "RUN NETWORK SPEED TEST",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (isNetworkTesting) Color(0xFF00FF66).copy(alpha = 0.5f) else Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { isLocalModelManagerOpen = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("settings_open_local_ai_model_manager_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color(0xFF00FF66)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF00FF66)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color(0xFF00FF66)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LOCAL MODEL MANAGER",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }

            if (isLocalModelManagerOpen) {
                androidx.compose.ui.window.Dialog(
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                    onDismissRequest = { isLocalModelManagerOpen = false }
                ) {
                    LocalAiModelScreen(onClose = { isLocalModelManagerOpen = false })
                }
            }

            // Section 5: Data & Storage
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
                            csvExportLauncher.launch("rf_spectrum_radar_log_${System.currentTimeMillis()}.csv")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("settings_export_csv_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EXPORT CAPTURED LOGS (CSV / SAF)",
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
                            kmlExportLauncher.launch("wardriving_breadcrumbs_${System.currentTimeMillis()}.kml")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = Color(0xFF00E5FF))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EXPORT GPS WARDRIVING BREADCRUMBS (KML / SAF)",
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
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
