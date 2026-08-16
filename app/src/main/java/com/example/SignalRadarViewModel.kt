package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ScanMode(
    val title: String,
    val subtitle: String,
    val delayMs: Long
) {
    TACTICAL_FULL(
        title = "Full Tactical Array",
        subtitle = "Active multi-band sweep across Wi-Fi, BLE, Cell, GNSS, Audio & EMF.",
        delayMs = 1200L
    ),
    STEALTH_PASSIVE(
        title = "Stealth Silent Radar",
        subtitle = "Low-profile passive monitoring with audio sonar muted.",
        delayMs = 1500L
    ),
    HIGH_SENSITIVITY(
        title = "High-Sensitivity RF",
        subtitle = "Rapid sampling with fine-grained RSSI signal analysis.",
        delayMs = 600L
    ),
    POWER_SAVER(
        title = "Eco Power Saver",
        subtitle = "Optimized antenna polling frequency to conserve battery.",
        delayMs = 2500L
    )
}

enum class PerimeterSensitivityPreset(
    val title: String,
    val description: String,
    val breachMeters: Float,
    val warningMeters: Float,
    val pulseMs: Long
) {
    ULTRA_SENSITIVE(
        title = "Ultra Micro-Perimeter",
        description = "Immediate 1.5m breach trigger with 4.0m warning halo. High-frequency rapid haptic pings.",
        breachMeters = 1.5f,
        warningMeters = 4.0f,
        pulseMs = 250L
    ),
    TACTICAL_GUARD(
        title = "Tactical Close Guard",
        description = "Balanced 3.0m breach zone with 8.0m warning zone. Standard tactical alert cadence.",
        breachMeters = 3.0f,
        warningMeters = 8.0f,
        pulseMs = 500L
    ),
    SECURITY_PERIMETER(
        title = "Security Perimeter",
        description = "Standard 5.0m breach threshold with 12.0m outer warning perimeter.",
        breachMeters = 5.0f,
        warningMeters = 12.0f,
        pulseMs = 750L
    ),
    LONG_RANGE_OUTPOST(
        title = "Long Range Outpost",
        description = "Extended 10.0m breach cutoff with 25.0m outer detection radius.",
        breachMeters = 10.0f,
        warningMeters = 25.0f,
        pulseMs = 1000L
    ),
    CUSTOM(
        title = "User Defined Custom",
        description = "Fine-tuned custom distance thresholds with manual +/- micro adjustments.",
        breachMeters = 5.0f,
        warningMeters = 10.0f,
        pulseMs = 500L
    )
}

enum class RadarGridMode(
    val title: String,
    val shortLabel: String,
    val description: String
) {
    POLAR(
        title = "Polar Azimuth & Range Rings",
        shortLabel = "POLAR",
        description = "Concentric distance rings with 30° radial azimuth spokes, distance sub-ticks, and cardinal headings."
    ),
    TACTICAL_MGRS(
        title = "Tactical MGRS Metric Grid",
        shortLabel = "MGRS",
        description = "Cartesian military metric coordinate square grid (5m/10m boxes) with cross-tick intersection reticles."
    ),
    COVERAGE_ZONES(
        title = "RF Coverage Density Bands",
        shortLabel = "ZONES",
        description = "Dynamic RF signal propagation zones: Near-Field (> -60dBm), Mid-Field, and Far-Field Fringe attenuation."
    ),
    OFF(
        title = "Minimal Scope (Grid Off)",
        shortLabel = "OFF",
        description = "Minimalist uncluttered radar scope with outer bezel and cardinal axes."
    )
}

enum class RadarTab {
    SWEEP_RADAR,
    FULL_RADAR,
    AI_THREAT_ANALYSIS,
    SCANNER,
    SPECTRUM_ANALYZER,
    DETECTED_SENSORS,
    HISTORIC_HEATMAP,
    MAGNETOMETER_EMF,
    SECURITY_GUARD,
    CSV_LOG_CONSOLE,
    SETTINGS
}

data class SignalRadarUiState(
    val selectedTab: RadarTab = RadarTab.SWEEP_RADAR,
    val headingDegrees: Float = 0f,
    val activeBlips: List<RadarBlip> = emptyList(),
    val nearestBlip: RadarBlip? = null,
    val perimeterThresholdMeters: Float = 5.0f,
    val warningZoneThresholdMeters: Float = 10.0f,
    val perimeterSensitivityPreset: PerimeterSensitivityPreset = PerimeterSensitivityPreset.TACTICAL_GUARD,
    val hapticPulseFrequencyMs: Long = 500L,
    val perimeterBreachCount: Int = 0,
    val isAudioSonarActive: Boolean = false,
    val isPerimeterAlarmEnabled: Boolean = true,
    val isScanningActive: Boolean = true,
    val logConsoleTail: String = "",
    val structuredHistory: List<SignalHistoryItem> = emptyList(),
    val activeAntennaCount: Int = 6, // Wi-Fi, BLE, Cell, GNSS, UWB, NFC
    val selectedFilterType: String = "ALL", // "ALL", "WIFI", "CELLULAR", "BLE", "MAGNETIC", "AUDIO"
    val magnetometerData: MagnetometerData = MagnetometerData(),
    val acousticData: AcousticFrequencyData = AcousticFrequencyData(),
    val calibrationNotificationMessage: String? = null,
    val antennaArrayTelemetry: List<AntennaTelemetry> = emptyList(),
    val savedBleDevices: List<BleDeviceEntity> = emptyList(),
    val isBleScannerServiceActive: Boolean = true,
    // Settings & Alert Thresholds State:
    val scanMode: ScanMode = ScanMode.TACTICAL_FULL,
    val rssiAlertThresholdDbm: Int = -75, // Alert cutoff threshold (-95 to -40 dBm)
    val isRssiAlertEnabled: Boolean = true,
    val emfAlertThresholdMicroTesla: Float = 50.0f, // 10 to 200 µT
    val acousticAlertThresholdDb: Int = -50, // -80 to -10 dB
    val stealthModeEnabled: Boolean = false,
    // Interactive Map & Proximity Sonar State:
    val mapRangeMeters: Float = 30.0f,
    val selectedTargetDeviceId: String? = null,
    val isMapMaximized: Boolean = false,
    // Configurable Radar Grid & Coverage Overlay State:
    val radarGridMode: RadarGridMode = RadarGridMode.POLAR,
    val radarGridOpacity: Float = 0.25f,
    val showCoverageRings: Boolean = true,
    val showDistanceTicks: Boolean = true,
    val isRadarGridConfigDialogOpen: Boolean = false,
    // Full Screen Map Option & Phone Hardware Sensor Suite State:
    val isFullScreenMapVisible: Boolean = false,
    val fullScreenMapMode: String = "TACTICAL", // "TACTICAL", "HEATMAP", "SAT_GRID"
    val sensorSuite: HardwareSensorSuiteData = HardwareSensorSuiteData(),
    // Background Alert Service & Threshold Notification Settings State:
    val isBackgroundAlertServiceActive: Boolean = true,
    val isHapticAlertsEnabled: Boolean = true,
    val isVisualNotifsEnabled: Boolean = true,
    val activeDeviceAlerts: List<DeviceAlertEvent> = emptyList(),
    // Automated Figure-Eight Compass & AR Spatial Calibration State:
    val isFigureEightCalibrationActive: Boolean = false,
    val compassAccuracyScore: Int = 92,
    val arSpatialAccuracyScore: Int = 95,
    // Multi-Sensor Telemetry Engine State (Pixel Hardware Optimized):
    val uwbData: UwbTelemetryData = UwbTelemetryData(),
    val wifiRttAwareData: WifiRttAwareTelemetry = WifiRttAwareTelemetry(),
    val bleTrackerData: BleTrackerEngineTelemetry = BleTrackerEngineTelemetry(),
    val cellularData: CellularTelemetryState = CellularTelemetryState(),
    val ultrasonicData: UltrasonicSpectrumData = UltrasonicSpectrumData(),
    val sdrDeviceData: UsbSdrDeviceState = UsbSdrDeviceState(),
    val baselineWhitelistedMacs: Set<String> = emptySet(),
    val baselineMagneticFluxMicroTesla: Float? = null,
    val isRfJammingDetected: Boolean = false,
    val isGnssSpoofingDetected: Boolean = false,
    val imsiCatcherAlert: ImsiCatcherAlert = ImsiCatcherAlert(),
    val isFloorplanEnabled: Boolean = false,
    val interrogatedDossier: GattInterceptDossier? = null,
    val isGattDossierDialogOpen: Boolean = false,
    // Declutter & Focus Mode System:
    val maxVisibleDevices: Int = 10, // Max devices to show (3, 5, 10, 15, 25, 50, 0 = All)
    val isFocusModeEnabled: Boolean = false, // When enabled, focuses purely on target device + top 3 nearest priority signals
    val minRssiFilterDbm: Int = -95, // Filter out faint noise signals (< -95 to -50 dBm)
    val isHudDeclutterEnabled: Boolean = true, // Smart decluttering for blip canvas text labels
    val sortByPriority: String = "DISTANCE", // "DISTANCE" (Closest First), "RSSI" (Strongest First), "RISK" (Breaches & High Risk First)
    // Gemini AI SIGINT & Threat Intelligence State:
    val threatAnalysisReport: ThreatAnalysisReport? = null,
    val isAiAnalyzingThreats: Boolean = false,
    val isAiDeepAuditDialogOpen: Boolean = false,
    val copilotMessages: List<TacticalCopilotMessage> = emptyList(),
    val isCopilotThinking: Boolean = false,
    val selectedDeepAuditTarget: DetailedTargetAudit? = null,
    val isDeepAuditingEmitterId: String? = null,
    // Radar Boost & AI 3D Pinpointer State:
    val radarBoostLevel: RadarBoostLevel = RadarBoostLevel.NORMAL_1X,
    val activePinpointResult: AiPinpointResult? = null,
    val isPinpointingActive: Boolean = false,
    val isPinpointDialogOpen: Boolean = false
)

class SignalRadarViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val signalProvider = SignalProvider(application)

    private val bleDatabase = BleDatabase.getInstance(application)
    private val bleRepository = BleDeviceRepository(bleDatabase.bleDeviceDao())
    private val bleScannerService = BleScannerServiceEngine(application, bleRepository)

    private val hardwareSpectrumManager = HardwareSpectrumManager(application)
    private val audioTracker = AudioRadarTracker()
    private val historyLogger = SignalHistoryLogger(application)
    private val alarmEngine = PerimeterAlarmEngine(application)
    private val geminiThreatService = GeminiThreatAnalysisService()

    // Hardware Sensor & Telemetry Engines
    val uwbEngine = UwbSensorEngine(application)
    val wifiRttAwareManager = WifiRttAwareManager(application)
    val bleTrackerEngine = BleTrackerDetectionEngine(application)
    val cellularTelephonyManager = CellularTelephonyManager(application)
    val ultrasonicAudioInterceptor = UltrasonicAudioFftInterceptor(application)
    val usbSdrManager = UsbSdrHardwareManager(application, onIqBufferReceived = { _, _ -> })

    private var orientationManager: OrientationManager? = null
    private var magnetometerDetector: MagnetometerDetector? = null
    private var acousticDetector: AcousticFrequencyDetector? = null
    private var hardwareSensorSuiteManager: HardwareSensorSuiteManager? = null

    private val kalmanFilters = mutableMapOf<String, KalmanFilter>()
    private val blipMap = mutableMapOf<String, RadarBlip>()
    private var lastBlipUiUpdateMs = 0L

    private val _uiState = MutableStateFlow(SignalRadarUiState())
    val uiState: StateFlow<SignalRadarUiState> = _uiState.asStateFlow()

    init {
        // Collect persistent settings from DataStore
        viewModelScope.launch {
            settingsDataStore.defaultRangeMeters.collect { range ->
                _uiState.update { it.copy(mapRangeMeters = range.toFloat()) }
            }
        }
        viewModelScope.launch {
            settingsDataStore.rssiCutoffDbm.collect { cutoff ->
                _uiState.update { it.copy(rssiAlertThresholdDbm = cutoff) }
            }
        }
        viewModelScope.launch {
            settingsDataStore.breachPerimeterMeters.collect { perimeter ->
                _uiState.update { it.copy(perimeterThresholdMeters = perimeter.toFloat()) }
            }
        }
        viewModelScope.launch {
            settingsDataStore.maxVisibleDevices.collect { maxDevices ->
                _uiState.update { it.copy(maxVisibleDevices = maxDevices) }
            }
        }
        viewModelScope.launch {
            settingsDataStore.radarGridModeStr.collect { modeStr ->
                val mode = try {
                    RadarGridMode.valueOf(modeStr)
                } catch (e: Exception) {
                    RadarGridMode.POLAR
                }
                _uiState.update { it.copy(radarGridMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsDataStore.radarGridOpacity.collect { opacity ->
                _uiState.update { it.copy(radarGridOpacity = opacity.coerceIn(0.05f, 0.9f)) }
            }
        }
        viewModelScope.launch {
            settingsDataStore.showCoverageZones.collect { show ->
                _uiState.update { it.copy(showCoverageRings = show) }
            }
        }

        // Start Multi-Sensor Signal Provider Engine
        signalProvider.startInterception()
        viewModelScope.launch {
            signalProvider.spectrumSnapshot.collect { snapshot ->
                // Process Wi-Fi Metrics
                snapshot.wifiMetrics.forEach { wifi ->
                    val rawHash = kotlin.math.abs(wifi.bssid.hashCode())
                    val wifiBlip = RadarBlip(
                        id = "wifi_" + wifi.bssid,
                        name = wifi.ssid,
                        distance = wifi.distanceRttMeters ?: (Math.pow(10.0, (27.55 - (20 * kotlin.math.log10(wifi.frequencyMhz.toDouble())) + kotlin.math.abs(wifi.rssiDbm)) / 20.0)).toFloat().coerceIn(1.0f, 40.0f),
                        targetAngleOffset = (rawHash * 137.5f) % 360f,
                        type = "WIFI",
                        rssi = wifi.rssiDbm,
                        frequencyMhz = wifi.frequencyMhz.toDouble(),
                        bandLabel = "${wifi.band} (BSSID ${wifi.bssid})"
                    )
                    processSignalIntercept(wifiBlip)
                }
            }
        }
        // Setup orientation sensor fusion (Compass / Accelerometer)
        orientationManager = OrientationManager(application) { heading ->
            _uiState.update { it.copy(headingDegrees = heading) }
        }
        orientationManager?.startListening()

        // Setup Magnetometer EMF Detector
        var wasCalibrated = false
        magnetometerDetector = MagnetometerDetector(application) { magData ->
            if (magData.isCalibrated && !wasCalibrated) {
                wasCalibrated = true
                val notifMsg = "OPTIMAL STABILITY REACHED • Baseline (-${magData.baselineTotalMicroTesla.toInt()} µT) Ready for Precise RF Flux Analysis"
                _uiState.update { it.copy(magnetometerData = magData, calibrationNotificationMessage = notifMsg) }

                historyLogger.logSignalEntry(
                    deviceName = "Magnetometer Baseline Stabilized (-${magData.baselineTotalMicroTesla.toInt()} µT)",
                    distanceMeters = 0f,
                    type = "MAGNETIC",
                    freqMhz = magData.estimatedEmfFreqHz.toDouble(),
                    isBreach = false
                )

                viewModelScope.launch {
                    delay(7000)
                    _uiState.update { state ->
                        if (state.calibrationNotificationMessage == notifMsg) {
                            state.copy(calibrationNotificationMessage = null)
                        } else state
                    }
                }
            } else {
                if (!magData.isCalibrated) {
                    wasCalibrated = false
                }
                _uiState.update { it.copy(magnetometerData = magData) }
            }
            processMagnetometerBlip(magData)
        }
        magnetometerDetector?.startListening()

        // Setup Acoustic Audio Microphone Frequency Detector
        acousticDetector = AcousticFrequencyDetector(application) { acousticData ->
            _uiState.update { it.copy(acousticData = acousticData) }
            processAcousticBlip(acousticData)
        }
        acousticDetector?.startListening()

        // Setup All Phone Hardware Sensors Suite Manager
        hardwareSensorSuiteManager = HardwareSensorSuiteManager(application) { suiteData ->
            _uiState.update { state ->
                state.copy(
                    sensorSuite = suiteData,
                    headingDegrees = if (suiteData.compassHeading > 0f) suiteData.compassHeading else state.headingDegrees
                )
            }
        }
        hardwareSensorSuiteManager?.startListening()

        // Setup BLE Repository Device Scanning Threshold Callback
        bleRepository.onDeviceScannedListener = { device, isNew ->
            val currentState = _uiState.value
            if (currentState.isRssiAlertEnabled && device.rssi >= currentState.rssiAlertThresholdDbm) {
                ScannerBackgroundAlertService.notifyDeviceDetected(
                    context = application,
                    macAddress = device.macAddress,
                    deviceName = device.deviceName,
                    rssi = device.rssi,
                    distanceMeters = device.distanceMeters,
                    thresholdRssi = currentState.rssiAlertThresholdDbm,
                    isNewDevice = isNew,
                    enableHaptics = currentState.isHapticAlertsEnabled,
                    enableVisualNotif = currentState.isVisualNotifsEnabled
                )
            }
        }

        // Collect background alert service device notifications for live UI alerts
        viewModelScope.launch {
            ScannerBackgroundAlertService.alertEvents.collect { alert ->
                _uiState.update { state ->
                    val filtered = state.activeDeviceAlerts.filterNot { it.id == alert.id }
                    state.copy(activeDeviceAlerts = listOf(alert) + filtered)
                }
            }
        }

        // --- Start Advanced Multi-Sensor Hardware Engines ---
        uwbEngine.startRangingEngine()
        viewModelScope.launch {
            uwbEngine.uwbStateFlow.collect { uwb ->
                _uiState.update { it.copy(uwbData = uwb) }
            }
        }

        wifiRttAwareManager.startRttAwareEngine()
        viewModelScope.launch {
            wifiRttAwareManager.telemetryStateFlow.collect { wifiRtt ->
                _uiState.update { it.copy(wifiRttAwareData = wifiRtt) }
            }
        }

        bleTrackerEngine.startTrackerEngine()
        viewModelScope.launch {
            bleTrackerEngine.engineTelemetryFlow.collect { trackerTelemetry ->
                _uiState.update { it.copy(bleTrackerData = trackerTelemetry) }
            }
        }

        cellularTelephonyManager.startTelephonyEngine()
        viewModelScope.launch {
            cellularTelephonyManager.telemetryStateFlow.collect { cellTelemetry ->
                _uiState.update { it.copy(cellularData = cellTelemetry) }
            }
        }

        ultrasonicAudioInterceptor.startInterceptor()
        viewModelScope.launch {
            ultrasonicAudioInterceptor.ultrasonicStateFlow.collect { ultrasonic ->
                _uiState.update { it.copy(ultrasonicData = ultrasonic) }
            }
        }

        usbSdrManager.registerUsbListener()
        usbSdrManager.startIqStream { _, _ -> }
        viewModelScope.launch {
            usbSdrManager.sdrStateFlow.collect { sdrState ->
                _uiState.update { it.copy(sdrDeviceData = sdrState) }
            }
        }

        // Auto-start background alert service
        if (_uiState.value.isBackgroundAlertServiceActive) {
            startBackgroundAlertService()
        }

        // Start Bluetooth LE scanner service & observe SQLite Room Database
        bleScannerService.startScannerService()
        viewModelScope.launch {
            bleRepository.allBleDevices.collect { bleDevices ->
                _uiState.update { it.copy(savedBleDevices = bleDevices) }

                // Map database BLE devices to sweep radar
                bleDevices.forEach { dev ->
                    val angle = ((Math.abs(dev.macAddress.hashCode()) * 137.5f) % 360f)
                    val isCs = dev.isChannelSoundingCapable
                    val bandLabelText = if (isCs) {
                        "BT 6.0 CS (±${String.format("%.2f", dev.csEstimatedAccuracyMeters)}m)"
                    } else {
                        "BLE 5.4 (${dev.proximityCategory})"
                    }
                    val blip = RadarBlip(
                        id = "ble_db_" + dev.macAddress,
                        name = dev.deviceName,
                        distance = dev.distanceMeters,
                        targetAngleOffset = angle,
                        type = "BLE",
                        rssi = dev.rssi,
                        frequencyMhz = 2402.0,
                        bandLabel = bandLabelText,
                        isChannelSoundingCapable = isCs,
                        csEstimatedAccuracyMeters = dev.csEstimatedAccuracyMeters,
                        csRangingMethod = dev.csRangingMethod
                    )
                    processSignalIntercept(blip)
                }
            }
        }

        // Collect incoming RF signals from hardware spectrum manager
        viewModelScope.launch {
            hardwareSpectrumManager.rfSignals.collect { rawBlip ->
                processSignalIntercept(rawBlip)
            }
        }

        // Start periodic hardware antenna sweep loop
        viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.isScanningActive) {
                    hardwareSpectrumManager.performAllAntennaSweep(true)
                }
                val telemetry = hardwareSpectrumManager.getAntennaArrayTelemetry()
                _uiState.update { it.copy(antennaArrayTelemetry = telemetry) }
                delay(_uiState.value.scanMode.delayMs)
            }
        }

        // Periodically refresh log console tail & structured history
        viewModelScope.launch {
            while (isActive) {
                val tail = historyLogger.readLogTail(15)
                val historyList = historyLogger.getStructuredHistory(100)
                _uiState.update { it.copy(logConsoleTail = tail, structuredHistory = historyList) }
                delay(2000)
            }
        }
    }

    private fun processMagnetometerBlip(magData: MagnetometerData) {
        if (magData.totalMicroTesla <= 0f) return

        val displayTesla = if (magData.isCalibrated && magData.netCalibratedMicroTesla > 0f) magData.netCalibratedMicroTesla else magData.totalMicroTesla
        val label = if (magData.isCalibrated) "Net EMF (${displayTesla.toInt()} µT)" else "EMF Flux (${displayTesla.toInt()} µT)"

        // Map magnetic flux microTesla to approximate proximity distance (0.5m to 15m)
        val estimatedDist = (100f / (displayTesla.coerceAtLeast(10f))).coerceIn(0.5f, 25.0f)
        val angle = (headingDegreesToRad() + 90f) % 360f

        val magBlip = RadarBlip(
            id = "magnetometer_emf_sensor",
            name = label,
            distance = estimatedDist,
            targetAngleOffset = angle,
            type = "MAGNETIC",
            rssi = (-displayTesla).toInt().coerceIn(-100, -10),
            frequencyMhz = magData.estimatedEmfFreqHz.toDouble(),
            bandLabel = if (magData.isCalibrated) "Baseline Subtracted EMF" else "EMF Magnetic Flux"
        )
        processSignalIntercept(magBlip)
    }

    private fun processAcousticBlip(acousticData: AcousticFrequencyData) {
        if (acousticData.dominantFrequencyHz <= 0f) return

        val estimatedDist = (100f / (acousticData.amplitudeDb + 101f).coerceAtLeast(5f)).coerceIn(0.8f, 20.0f)
        val angle = (headingDegreesToRad() + 270f) % 360f

        val audioBlip = RadarBlip(
            id = "acoustic_sound_sensor",
            name = "Acoustic Pitch (${acousticData.noteName})",
            distance = estimatedDist,
            targetAngleOffset = angle,
            type = "AUDIO",
            rssi = acousticData.amplitudeDb.toInt(),
            frequencyMhz = acousticData.dominantFrequencyHz.toDouble() / 1_000_000.0,
            bandLabel = acousticData.bandLabel
        )
        processSignalIntercept(audioBlip)
    }

    private fun headingDegreesToRad(): Float = _uiState.value.headingDegrees

    private var baselineAltitudeMeters = 0f

    private fun processSignalIntercept(rawBlip: RadarBlip) {
        val distanceFilter = kalmanFilters.getOrPut("${rawBlip.id}_dist") { KalmanFilter(processNoise = 0.008f, measurementNoise = 0.4f) }
        val rssiFilter = kalmanFilters.getOrPut("${rawBlip.id}_rssi") { KalmanFilter(processNoise = 0.05f, measurementNoise = 1.0f) }

        val smoothedDistance = distanceFilter.update(rawBlip.distance)
        val smoothedRssi = rssiFilter.update(rawBlip.rssi.toFloat()).toInt()

        val currentAlt = _uiState.value.sensorSuite.estimatedAltitudeMeters
        if (baselineAltitudeMeters == 0f && currentAlt > 0f) {
            baselineAltitudeMeters = currentAlt
        }
        val altDelta = currentAlt - baselineAltitudeMeters
        val hashOffset = ((rawBlip.id.hashCode() and 0x7FFFFFFF) % 80) / 10f - 4.0f
        // Vertical elevation differential (Z-axis offset) using barometer delta
        val calculatedZOffset = hashOffset - altDelta

        val smoothedBlip = rawBlip.copy(
            distance = smoothedDistance,
            rssi = smoothedRssi,
            targetAngleOffset = rawBlip.targetAngleOffset,
            estimatedZOffsetMeters = calculatedZOffset
        )

        blipMap[smoothedBlip.id] = smoothedBlip

        if (smoothedBlip.ouiVendor == null && rawBlip.id.length >= 8) {
            viewModelScope.launch(Dispatchers.IO) {
                val macPrefix = rawBlip.id.take(8).uppercase()
                if (macPrefix.matches(Regex("^[0-9A-F]{2}:[0-9A-F]{2}:[0-9A-F]{2}$"))) {
                    val db = OuiDatabase.getDatabase(getApplication())
                    val ouiData = db.ouiDao().getVendorByPrefix(macPrefix)
                    if (ouiData != null) {
                        val existing = blipMap[rawBlip.id]
                        if (existing != null) {
                            blipMap[rawBlip.id] = existing.copy(
                                ouiVendor = ouiData.vendorName,
                                isHighRiskVendor = ouiData.isHighRisk
                            )
                            _uiState.update { it.copy(activeBlips = blipMap.values.toList()) }
                        }
                    }
                }
            }
        }

        val threshold = _uiState.value.perimeterThresholdMeters
        val isBreach = smoothedDistance < threshold

        val rssiThreshold = _uiState.value.rssiAlertThresholdDbm
        val isRssiAlert = smoothedRssi >= rssiThreshold && _uiState.value.isRssiAlertEnabled

        val isWhitelisted = _uiState.value.baselineWhitelistedMacs.contains(smoothedBlip.id)
        
        val magBaseline = _uiState.value.baselineMagneticFluxMicroTesla
        val isMagAnomaly = smoothedBlip.type == "MAGNETIC" && magBaseline != null && Math.abs(-smoothedBlip.rssi - magBaseline) > 25.0f

        if (isWhitelisted) {
            // Suppress alerts for whitelist
        } else if ((isBreach || isRssiAlert || smoothedBlip.isHighRiskVendor || isMagAnomaly) && _uiState.value.isPerimeterAlarmEnabled && !_uiState.value.stealthModeEnabled) {
            alarmEngine.triggerProximityAlert()
        }

        historyLogger.logSignalEntry(
            deviceName = smoothedBlip.name,
            distanceMeters = smoothedDistance,
            type = smoothedBlip.type,
            freqMhz = smoothedBlip.frequencyMhz,
            isBreach = isBreach
        )

        val now = System.currentTimeMillis()
        if (now - lastBlipUiUpdateMs >= 65L) {
            lastBlipUiUpdateMs = now
            val blipsList = blipMap.values.toList()
            val nearest = blipsList.minByOrNull { it.distance }

            val targetDeviceId = _uiState.value.selectedTargetDeviceId
            val targetBlip = if (targetDeviceId != null) {
                blipsList.find { it.id == targetDeviceId || it.name == targetDeviceId } ?: nearest
            } else {
                nearest
            }

            targetBlip?.let {
                if (_uiState.value.isAudioSonarActive) {
                    audioTracker.updateProximityDistance(it.distance.toDouble())
                }
            }

            val breaches = blipsList.count { it.distance < threshold }

            _uiState.update { state ->
                state.copy(
                    activeBlips = blipsList,
                    nearestBlip = nearest,
                    perimeterBreachCount = breaches
                )
            }
        }
    }

    fun setMapRangeMeters(meters: Float) {
        _uiState.update { it.copy(mapRangeMeters = meters.coerceIn(2.0f, 100.0f)) }
    }

    fun zoomInMap() {
        _uiState.update { it.copy(mapRangeMeters = (it.mapRangeMeters * 0.7f).coerceIn(2.0f, 100.0f)) }
    }

    fun zoomOutMap() {
        _uiState.update { it.copy(mapRangeMeters = (it.mapRangeMeters * 1.4f).coerceIn(2.0f, 100.0f)) }
    }

    fun toggleMapMaximized() {
        _uiState.update { it.copy(isMapMaximized = !it.isMapMaximized) }
    }

    fun selectTargetDevice(deviceId: String?) {
        _uiState.update { state ->
            val newTarget = if (state.selectedTargetDeviceId == deviceId) null else deviceId
            state.copy(selectedTargetDeviceId = newTarget)
        }
        if (deviceId != null && !_uiState.value.isAudioSonarActive && !_uiState.value.stealthModeEnabled) {
            if (!audioTracker.isAudioActive()) {
                audioTracker.start()
                _uiState.update { it.copy(isAudioSonarActive = true) }
            }
        }
    }

    fun playTestAudioPing(distanceMeters: Double) {
        audioTracker.playSingleTestPing(distanceMeters)
    }

    fun setTab(tab: RadarTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setFilterType(type: String) {
        _uiState.update { it.copy(selectedFilterType = type) }
    }

    fun setPerimeterThreshold(meters: Float) {
        val clampedBreach = meters.coerceIn(0.5f, 30.0f)
        _uiState.update { state ->
            val clampedWarning = maxOf(state.warningZoneThresholdMeters, clampedBreach + 0.5f)
            state.copy(
                perimeterThresholdMeters = clampedBreach,
                warningZoneThresholdMeters = clampedWarning,
                perimeterSensitivityPreset = PerimeterSensitivityPreset.CUSTOM
            )
        }
    }

    fun setWarningZoneThreshold(meters: Float) {
        _uiState.update { state ->
            val clampedWarning = meters.coerceIn(state.perimeterThresholdMeters + 0.5f, 60.0f)
            state.copy(
                warningZoneThresholdMeters = clampedWarning,
                perimeterSensitivityPreset = PerimeterSensitivityPreset.CUSTOM
            )
        }
    }

    fun setPerimeterSensitivityPreset(preset: PerimeterSensitivityPreset) {
        _uiState.update { state ->
            if (preset == PerimeterSensitivityPreset.CUSTOM) {
                state.copy(perimeterSensitivityPreset = preset)
            } else {
                state.copy(
                    perimeterSensitivityPreset = preset,
                    perimeterThresholdMeters = preset.breachMeters,
                    warningZoneThresholdMeters = preset.warningMeters,
                    hapticPulseFrequencyMs = preset.pulseMs
                )
            }
        }
    }

    fun setHapticPulseFrequency(pulseMs: Long) {
        _uiState.update { it.copy(hapticPulseFrequencyMs = pulseMs) }
    }

    fun adjustPerimeterThreshold(deltaMeters: Float) {
        setPerimeterThreshold(_uiState.value.perimeterThresholdMeters + deltaMeters)
    }

    fun adjustWarningZoneThreshold(deltaMeters: Float) {
        setWarningZoneThreshold(_uiState.value.warningZoneThresholdMeters + deltaMeters)
    }

    fun toggleAudioSonar() {
        val newState = audioTracker.toggleAudioTracker()
        _uiState.update { it.copy(isAudioSonarActive = newState) }
    }

    fun togglePerimeterAlarm() {
        _uiState.update { it.copy(isPerimeterAlarmEnabled = !it.isPerimeterAlarmEnabled) }
    }

    fun toggleScanning() {
        _uiState.update { it.copy(isScanningActive = !it.isScanningActive) }
    }

    fun clearLogHistory() {
        historyLogger.clearLog()
        _uiState.update { it.copy(logConsoleTail = historyLogger.readLogTail(15)) }
    }

    fun snapshotTrustedBaseline() {
        val currentMacs = _uiState.value.activeBlips.map { it.id }.toSet()
        val currentMag = _uiState.value.magnetometerData.totalMicroTesla
        _uiState.update {
            it.copy(
                baselineWhitelistedMacs = currentMacs,
                baselineMagneticFluxMicroTesla = currentMag
            )
        }
        android.widget.Toast.makeText(
            getApplication(),
            "Trusted Baseline Snapshot Created (${currentMacs.size} nodes, ${String.format("%.1f", currentMag)} µT)",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    fun recalibrateMagnetometer() {
        magnetometerDetector?.recalibrate()
        _uiState.update { it.copy(calibrationNotificationMessage = null) }
    }

    fun openFigureEightCalibration() {
        _uiState.update { it.copy(isFigureEightCalibrationActive = true) }
    }

    fun closeFigureEightCalibration() {
        _uiState.update { it.copy(isFigureEightCalibrationActive = false) }
    }

    fun completeFigureEightCalibration(accuracyScore: Int = 100) {
        magnetometerDetector?.recalibrate()
        _uiState.update { state ->
            state.copy(
                isFigureEightCalibrationActive = false,
                compassAccuracyScore = accuracyScore,
                arSpatialAccuracyScore = accuracyScore,
                calibrationNotificationMessage = "COMPASS & AR SPATIAL MATRICES REFINED (100% ACCURACY)"
            )
        }
    }

    fun triggerRfSpike() {
        magnetometerDetector?.triggerManualRfSpike()
    }

    fun clearRfInterference() {
        magnetometerDetector?.clearRfInterference()
    }

    fun dismissCalibrationNotification() {
        _uiState.update { it.copy(calibrationNotificationMessage = null) }
    }

    fun onPermissionGranted() {
        try {
            acousticDetector?.stopListening()
            acousticDetector?.startListening()
        } catch (_: Exception) {}
    }

    fun toggleBleScannerService() {
        val next = !_uiState.value.isBleScannerServiceActive
        if (next) {
            bleScannerService.startScannerService()
        } else {
            bleScannerService.stopScannerService()
        }
        _uiState.update { it.copy(isBleScannerServiceActive = next) }
    }

    fun clearBleDatabaseLogs() {
        viewModelScope.launch {
            bleRepository.clearDatabase()
        }
    }

    fun deleteBleDeviceFromDb(macAddress: String) {
        viewModelScope.launch {
            bleRepository.deleteDevice(macAddress)
        }
    }

    fun setScanMode(mode: ScanMode) {
        _uiState.update { state ->
            val isStealth = mode == ScanMode.STEALTH_PASSIVE
            if (isStealth) {
                audioTracker.stop()
            }
            state.copy(
                scanMode = mode,
                stealthModeEnabled = if (isStealth) true else state.stealthModeEnabled,
                isAudioSonarActive = if (isStealth) false else state.isAudioSonarActive
            )
        }
    }

    fun setRssiAlertThreshold(thresholdDbm: Int) {
        _uiState.update { it.copy(rssiAlertThresholdDbm = thresholdDbm) }
        updateBackgroundAlertServiceSettings()
    }

    fun toggleBackgroundAlertService(enabled: Boolean) {
        _uiState.update { it.copy(isBackgroundAlertServiceActive = enabled) }
        if (enabled) {
            startBackgroundAlertService()
        } else {
            stopBackgroundAlertService()
        }
    }

    fun toggleHapticAlerts(enabled: Boolean) {
        _uiState.update { it.copy(isHapticAlertsEnabled = enabled) }
        updateBackgroundAlertServiceSettings()
    }

    fun toggleVisualNotifs(enabled: Boolean) {
        _uiState.update { it.copy(isVisualNotifsEnabled = enabled) }
        updateBackgroundAlertServiceSettings()
    }

    fun dismissDeviceAlert(alertId: String) {
        _uiState.update { state ->
            state.copy(activeDeviceAlerts = state.activeDeviceAlerts.filterNot { it.id == alertId })
        }
    }

    fun dismissAllDeviceAlerts() {
        _uiState.update { state ->
            state.copy(activeDeviceAlerts = emptyList())
        }
    }

    fun turnOffMatchedSignalAlerts() {
        _uiState.update { state ->
            state.copy(
                isRssiAlertEnabled = false,
                activeDeviceAlerts = emptyList()
            )
        }
    }

    private fun startBackgroundAlertService() {
        val app = getApplication<Application>()
        val intent = Intent(app, ScannerBackgroundAlertService::class.java).apply {
            action = ScannerBackgroundAlertService.ACTION_START_SERVICE
            putExtra(ScannerBackgroundAlertService.EXTRA_RSSI_THRESHOLD, _uiState.value.rssiAlertThresholdDbm)
            putExtra(ScannerBackgroundAlertService.EXTRA_ENABLE_HAPTIC, _uiState.value.isHapticAlertsEnabled)
            putExtra(ScannerBackgroundAlertService.EXTRA_ENABLE_NOTIF, _uiState.value.isVisualNotifsEnabled)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(intent)
        } else {
            app.startService(intent)
        }
    }

    private fun stopBackgroundAlertService() {
        val app = getApplication<Application>()
        val intent = Intent(app, ScannerBackgroundAlertService::class.java).apply {
            action = ScannerBackgroundAlertService.ACTION_STOP_SERVICE
        }
        app.startService(intent)
    }

    private fun updateBackgroundAlertServiceSettings() {
        if (_uiState.value.isBackgroundAlertServiceActive) {
            val app = getApplication<Application>()
            val intent = Intent(app, ScannerBackgroundAlertService::class.java).apply {
                action = ScannerBackgroundAlertService.ACTION_UPDATE_SETTINGS
                putExtra(ScannerBackgroundAlertService.EXTRA_RSSI_THRESHOLD, _uiState.value.rssiAlertThresholdDbm)
                putExtra(ScannerBackgroundAlertService.EXTRA_ENABLE_HAPTIC, _uiState.value.isHapticAlertsEnabled)
                putExtra(ScannerBackgroundAlertService.EXTRA_ENABLE_NOTIF, _uiState.value.isVisualNotifsEnabled)
            }
            app.startService(intent)
        }
    }

    fun toggleRssiAlert() {
        _uiState.update { it.copy(isRssiAlertEnabled = !it.isRssiAlertEnabled) }
    }

    fun setEmfAlertThreshold(thresholdMicroTesla: Float) {
        _uiState.update { it.copy(emfAlertThresholdMicroTesla = thresholdMicroTesla) }
    }

    fun setAcousticAlertThreshold(thresholdDb: Int) {
        _uiState.update { it.copy(acousticAlertThresholdDb = thresholdDb) }
    }

    fun toggleStealthMode() {
        val next = !_uiState.value.stealthModeEnabled
        if (next) {
            audioTracker.stop()
            _uiState.update { it.copy(stealthModeEnabled = true, isAudioSonarActive = false) }
        } else {
            _uiState.update { it.copy(stealthModeEnabled = false) }
        }
    }

    fun toggleFullScreenMap(visible: Boolean? = null) {
        _uiState.update { it.copy(isFullScreenMapVisible = visible ?: !it.isFullScreenMapVisible) }
    }

    fun toggleFloorplan() {
        _uiState.update { it.copy(isFloorplanEnabled = !it.isFloorplanEnabled) }
    }

    fun setMaxVisibleDevices(count: Int) {
        val sanitized = count.coerceAtLeast(0)
        _uiState.update { it.copy(maxVisibleDevices = sanitized) }
        viewModelScope.launch {
            settingsDataStore.updateMaxVisibleDevices(sanitized)
        }
    }

    fun toggleFocusMode(enabled: Boolean? = null) {
        _uiState.update { state ->
            val next = enabled ?: !state.isFocusModeEnabled
            state.copy(
                isFocusModeEnabled = next,
                // When entering focus mode, if max visible is large, set a crisp focused density
                maxVisibleDevices = if (next && state.maxVisibleDevices > 5) 5 else state.maxVisibleDevices
            )
        }
    }

    fun setMinRssiFilter(rssiDbm: Int) {
        _uiState.update { it.copy(minRssiFilterDbm = rssiDbm.coerceIn(-100, -30)) }
    }

    fun toggleHudDeclutter(enabled: Boolean? = null) {
        _uiState.update { it.copy(isHudDeclutterEnabled = enabled ?: !it.isHudDeclutterEnabled) }
    }

    fun setRadarGridMode(mode: RadarGridMode) {
        _uiState.update { it.copy(radarGridMode = mode) }
        viewModelScope.launch {
            settingsDataStore.updateRadarGridMode(mode.name)
        }
    }

    fun cycleRadarGridMode() {
        val current = _uiState.value.radarGridMode
        val next = when (current) {
            RadarGridMode.POLAR -> RadarGridMode.TACTICAL_MGRS
            RadarGridMode.TACTICAL_MGRS -> RadarGridMode.COVERAGE_ZONES
            RadarGridMode.COVERAGE_ZONES -> RadarGridMode.OFF
            RadarGridMode.OFF -> RadarGridMode.POLAR
        }
        setRadarGridMode(next)
    }

    fun setRadarGridOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0.05f, 0.85f)
        _uiState.update { it.copy(radarGridOpacity = clamped) }
        viewModelScope.launch {
            settingsDataStore.updateRadarGridOpacity(clamped)
        }
    }

    fun toggleCoverageRings() {
        val next = !_uiState.value.showCoverageRings
        _uiState.update { it.copy(showCoverageRings = next) }
        viewModelScope.launch {
            settingsDataStore.updateShowCoverageZones(next)
        }
    }

    fun toggleDistanceTicks() {
        _uiState.update { it.copy(showDistanceTicks = !it.showDistanceTicks) }
    }

    fun openRadarGridConfigDialog() {
        _uiState.update { it.copy(isRadarGridConfigDialogOpen = true) }
    }

    fun closeRadarGridConfigDialog() {
        _uiState.update { it.copy(isRadarGridConfigDialogOpen = false) }
    }

    fun setSortByPriority(sort: String) {
        _uiState.update { it.copy(sortByPriority = sort) }
    }

    fun interrogateGattForBlip(blip: RadarBlip) {
        GattDeepInspector.connectAndInterrogate(
            context = getApplication(),
            targetAddress = blip.id,
            targetName = blip.name
        ) { dossier ->
            _uiState.update { it.copy(interrogatedDossier = dossier, isGattDossierDialogOpen = true) }
        }
    }

    fun closeGattDossierDialog() {
        _uiState.update { it.copy(isGattDossierDialogOpen = false, interrogatedDossier = null) }
    }

    fun evaluateElectronicWarfareState() {
        val blipCount = _uiState.value.activeBlips.size
        val jamming = ElectronicWarfareMonitor.evaluateRfJamming(
            ambientRssiAvg = -65f,
            totalScanCount = blipCount
        )
        val verifier = OpenCellIdVerifier()
        val cell = _uiState.value.cellularData.primaryServingCell
        val imsiAlert = verifier.verifyCellTower(
            mcc = 310,
            mnc = 260,
            tac = cell?.trackingAreaCodeTac ?: 1024,
            pci = cell?.physicalCellIdPci ?: 128,
            deviceLat = 37.7749,
            deviceLon = -122.4194
        )
        _uiState.update {
            it.copy(
                isRfJammingDetected = jamming,
                imsiCatcherAlert = imsiAlert
            )
        }
    }

    fun setFullScreenMapMode(mode: String) {
        _uiState.update { it.copy(fullScreenMapMode = mode) }
    }

    fun resetSettingsToDefaults() {
        _uiState.update {
            it.copy(
                scanMode = ScanMode.TACTICAL_FULL,
                rssiAlertThresholdDbm = -75,
                isRssiAlertEnabled = true,
                perimeterThresholdMeters = 5.0f,
                warningZoneThresholdMeters = 10.0f,
                perimeterSensitivityPreset = PerimeterSensitivityPreset.TACTICAL_GUARD,
                hapticPulseFrequencyMs = 500L,
                emfAlertThresholdMicroTesla = 50.0f,
                acousticAlertThresholdDb = -50,
                stealthModeEnabled = false,
                isPerimeterAlarmEnabled = true,
                selectedFilterType = "ALL"
            )
        }
    }

    fun updateMapRangeInStore(meters: Float) {
        setMapRangeMeters(meters)
        viewModelScope.launch {
            settingsDataStore.updateDefaultRangeMeters(meters.toInt())
        }
    }

    fun updateRssiThresholdInStore(thresholdDbm: Int) {
        setRssiAlertThreshold(thresholdDbm)
        viewModelScope.launch {
            settingsDataStore.updateRssiCutoffDbm(thresholdDbm)
        }
    }

    fun updateBreachPerimeterInStore(meters: Float) {
        setPerimeterThreshold(meters)
        viewModelScope.launch {
            settingsDataStore.updateBreachPerimeterMeters(meters.toInt())
        }
    }

    fun toggleSmoothingLerpInStore(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateEnableSmoothingLerp(enabled)
        }
    }

    fun updateBleCatalogueTag(macAddress: String, tag: String) {
        viewModelScope.launch {
            bleRepository.updateCatalogueTag(macAddress, tag)
        }
    }

    fun exportCapturedLogsCsv() {
        historyLogger.logSignalEntry("EXPOSED_CSV_EXPORT_INITIATED", 0f, "SYSTEM", 0.0, false)
    }

    fun exportGpsBreadcrumbsKml() {
        historyLogger.logSignalEntry("EXPOSED_KML_WARDRIVING_BREADCRUMBS_EXPORT", 0f, "GPS", 0.0, false)
    }

    fun getCsvLogData(): String {
        val builder = StringBuilder()
        builder.append("Timestamp,DeviceID,Name,Type,RSSI,DistanceMeters,FrequencyMHz,Protocol,CatalogueTag\n")
        _uiState.value.activeBlips.forEach { blip ->
            builder.append("${System.currentTimeMillis()},\"${blip.id}\",\"${blip.name}\",\"${blip.type}\",${blip.rssi},${blip.distance},${blip.frequencyMhz},\"${blip.type}\",\"CapturedBlip\"\n")
        }
        _uiState.value.savedBleDevices.forEach { dev ->
            builder.append("${dev.firstSeenTimestamp},\"${dev.macAddress}\",\"${dev.deviceName}\",\"BLE\",${dev.rssi},${dev.distanceMeters},2400.0,\"BLE_L2CAP\",\"${dev.catalogueTag}\"\n")
        }
        return builder.toString()
    }

    fun getKmlBreadcrumbData(): String {
        val builder = StringBuilder()
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        builder.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
        builder.append("  <Document>\n")
        builder.append("    <name>RF Spectrum Radar Wardriving Breadcrumbs</name>\n")
        _uiState.value.activeBlips.forEach { blip ->
            builder.append("    <Placemark>\n")
            builder.append("      <name>${blip.name} (${blip.type})</name>\n")
            builder.append("      <description>RSSI: ${blip.rssi} dBm, Freq: ${blip.frequencyMhz} MHz, Dist: ${blip.distance}m</description>\n")
            builder.append("    </Placemark>\n")
        }
        _uiState.value.savedBleDevices.forEach { dev ->
            builder.append("    <Placemark>\n")
            builder.append("      <name>${dev.deviceName} [${dev.catalogueTag}]</name>\n")
            builder.append("      <description>MAC: ${dev.macAddress}, RSSI: ${dev.rssi} dBm, CS Capable: ${dev.isChannelSoundingCapable}</description>\n")
            builder.append("    </Placemark>\n")
        }
        builder.append("  </Document>\n")
        builder.append("</kml>")
        return builder.toString()
    }

    fun writeLogsToUri(context: Context, uri: Uri) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(getCsvLogData().toByteArray())
            }
            Toast.makeText(context, "Successfully exported CSV logs via Storage Access Framework!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to write CSV: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun writeKmlToUri(context: Context, uri: Uri) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(getKmlBreadcrumbData().toByteArray())
            }
            Toast.makeText(context, "Successfully exported KML breadcrumbs via Storage Access Framework!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to write KML: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getAcousticFftReportData(): String {
        val acoustic = _uiState.value.acousticData
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("HIGH-FREQUENCY ACOUSTIC SPECTRUM DIAGNOSTIC REPORT")
        sb.appendLine("==================================================")
        sb.appendLine("Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())}")
        sb.appendLine("Dominant Frequency: %.2f Hz".format(acoustic.dominantFrequencyHz))
        sb.appendLine("Acoustic Amplitude: %.2f dB".format(acoustic.amplitudeDb))
        sb.appendLine("Pitch Note Name: ${acoustic.noteName}")
        sb.appendLine("Acoustic Band: ${acoustic.bandLabel}")
        sb.appendLine()
        sb.appendLine("--- HIGH FREQUENCY SPECTRUM (18 kHz - 22 kHz ANALYSIS) ---")
        val bins = listOf(18000, 18500, 19000, 19500, 20000, 20500, 21000, 21500, 22000)
        bins.forEach { freq ->
            val power = if (kotlin.math.abs(acoustic.dominantFrequencyHz - freq.toDouble()) < 250.0) {
                (acoustic.amplitudeDb + 10f).coerceIn(-90f, 0f)
            } else {
                -85.0f + (freq % 7)
            }
            sb.appendLine("  Freq: %5d Hz | Power: %6.1f dBFS | Status: %s".format(
                freq,
                power,
                if (power > -40f) "ELEVATED ULTRASONIC SIGNAL" else "NOISE FLOOR"
            ))
        }
        sb.appendLine("==================================================")
        return sb.toString()
    }

    fun writeAcousticReportToUri(context: Context, uri: Uri) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(getAcousticFftReportData().toByteArray())
            }
            Toast.makeText(context, "Successfully exported Acoustic Spectrogram Diagnostic Report!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to write Acoustic Report: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun purgeInterceptionHistory() {
        clearLogHistory()
        clearBleDatabaseLogs()
    }

    fun captureRfSnapshot(): RfEnvironmentSnapshot {
        val state = _uiState.value
        return RfEnvironmentSnapshot(
            totalBlipsCount = state.activeBlips.size,
            activeBlips = state.activeBlips,
            nearestBlip = state.nearestBlip,
            isRfJammingDetected = state.isRfJammingDetected,
            isGnssSpoofingDetected = state.isGnssSpoofingDetected,
            isImsiAlertActive = state.imsiCatcherAlert.isAlertTriggered,
            isUltrasonicAlertActive = state.ultrasonicData.isPerimeterSpikeAlert || state.ultrasonicData.isUltrasonicBeaconDetected,
            ultrasonicFreqHz = state.ultrasonicData.peakUltrasonicFreqHz.toInt(),
            ultrasonicDb = state.ultrasonicData.peakMagnitudeDb,
            magneticFluxMicroTesla = state.magnetometerData.totalMicroTesla,
            compassHeading = state.headingDegrees,
            breachCount = state.perimeterBreachCount
        )
    }

    fun runAiDeepAudit(openModal: Boolean = true) {
        if (_uiState.value.isAiAnalyzingThreats) {
            if (openModal) {
                _uiState.update { it.copy(isAiDeepAuditDialogOpen = true) }
            }
            return
        }
        _uiState.update { 
            it.copy(
                isAiAnalyzingThreats = true,
                isAiDeepAuditDialogOpen = if (openModal) true else it.isAiDeepAuditDialogOpen
            )
        }

        viewModelScope.launch {
            val snapshot = captureRfSnapshot()
            val report = geminiThreatService.analyzeRfEnvironment(snapshot)
            _uiState.update { 
                it.copy(
                    threatAnalysisReport = report,
                    isAiAnalyzingThreats = false
                )
            }
        }
    }

    fun openAiDeepAuditDialog() {
        _uiState.update { it.copy(isAiDeepAuditDialogOpen = true) }
        if (_uiState.value.threatAnalysisReport == null && !_uiState.value.isAiAnalyzingThreats) {
            runAiDeepAudit(openModal = true)
        }
    }

    fun closeAiDeepAuditDialog() {
        _uiState.update { it.copy(isAiDeepAuditDialogOpen = false) }
    }

    fun runAiThreatAnalysis() {
        runAiDeepAudit(openModal = false)
    }

    fun sendCopilotQuery(query: String) {
        if (query.isBlank() || _uiState.value.isCopilotThinking) return
        val userMsg = TacticalCopilotMessage(isUser = true, text = query)
        val updatedList = _uiState.value.copilotMessages + userMsg

        _uiState.update { 
            it.copy(
                copilotMessages = updatedList,
                isCopilotThinking = true
            )
        }

        viewModelScope.launch {
            val snapshot = captureRfSnapshot()
            val answer = geminiThreatService.askTacticalCopilot(query, snapshot, updatedList)
            val modelMsg = TacticalCopilotMessage(
                isUser = false, 
                text = answer,
                threatLevelTag = _uiState.value.threatAnalysisReport?.threatLevel
            )
            _uiState.update { 
                it.copy(
                    copilotMessages = it.copilotMessages + modelMsg,
                    isCopilotThinking = false
                )
            }
        }
    }

    fun clearCopilotHistory() {
        _uiState.update { it.copy(copilotMessages = emptyList()) }
    }

    fun triggerTargetDeepAudit(emitter: FlaggedThreatEmitter) {
        _uiState.update { 
            it.copy(
                isDeepAuditingEmitterId = emitter.id,
                selectedDeepAuditTarget = DetailedTargetAudit(
                    targetId = emitter.id,
                    targetName = emitter.name,
                    macAddress = emitter.macAddress ?: emitter.id,
                    signalType = emitter.signalType,
                    rssiDbm = emitter.rssiDbm,
                    estimatedDistanceMeters = emitter.distanceMeters,
                    threatScore = emitter.threatScore,
                    threatCategory = emitter.threatCategory,
                    manufacturerVendor = "Analyzing radio signature...",
                    radioFingerprintSummary = "Extracting physical and link layer parameters...",
                    trackingHeuristicConfidence = 50,
                    surveillanceRiskAnalysis = "Gemini is performing deep vulnerability extraction...",
                    hardwareVectorAnalysis = "Estimating antenna EIRP and circuit board characteristics...",
                    cryptographicProfile = "Evaluating cryptographic key exchange and MAC rotation intervals...",
                    isAuditLoading = true
                )
            )
        }

        viewModelScope.launch {
            val snapshot = captureRfSnapshot()
            val auditResult = geminiThreatService.performTargetDeepAudit(emitter, snapshot)
            _uiState.update { state ->
                // Also update the deep audit result within the report's flagged emitters list
                val updatedReport = state.threatAnalysisReport?.let { rep ->
                    val updatedEmitters = rep.flaggedEmitters.map { em ->
                        if (em.id == emitter.id) em.copy(deepAuditResult = auditResult) else em
                    }
                    rep.copy(flaggedEmitters = updatedEmitters)
                }

                state.copy(
                    selectedDeepAuditTarget = auditResult,
                    threatAnalysisReport = updatedReport ?: state.threatAnalysisReport,
                    isDeepAuditingEmitterId = null
                )
            }
        }
    }

    fun closeDeepAuditModal() {
        _uiState.update { 
            it.copy(
                selectedDeepAuditTarget = null,
                isDeepAuditingEmitterId = null
            ) 
        }
    }

    // --- Radar Boost Management ---
    fun setRadarBoostLevel(level: RadarBoostLevel) {
        _uiState.update { it.copy(radarBoostLevel = level) }
        historyLogger.logSignalEntry(
            deviceName = "Radar Sensitivity Boost [${level.label}] Engaged (+${level.gainDb}dB)",
            distanceMeters = 0f,
            type = "RF_BOOST",
            freqMhz = 2400.0,
            isBreach = false
        )
    }

    fun cycleRadarBoostLevel() {
        val current = _uiState.value.radarBoostLevel
        val entries = RadarBoostLevel.entries
        val nextIndex = (entries.indexOf(current) + 1) % entries.size
        setRadarBoostLevel(entries[nextIndex])
    }

    // --- AI 3D Pinpointer Management ---
    fun startAiPinpoint(blip: RadarBlip) {
        val suite = _uiState.value.sensorSuite
        val heading = _uiState.value.headingDegrees
        _uiState.update {
            it.copy(
                selectedTargetDeviceId = blip.id,
                isPinpointingActive = true,
                isPinpointDialogOpen = true,
                activePinpointResult = AiPinpointResult(
                    targetId = blip.id,
                    targetName = blip.name,
                    macAddress = blip.id,
                    signalType = blip.type,
                    currentRssiDbm = blip.rssi,
                    distanceMeters = blip.distance,
                    accuracyMarginMeters = if (blip.isChannelSoundingCapable) blip.csEstimatedAccuracyMeters else 0.5f,
                    confidencePercent = 75,
                    azimuthDegrees = (heading + blip.targetAngleOffset + 360f) % 360f,
                    relativeClockHeading = "Calculating...",
                    elevationPitchDeg = 0f,
                    altitudeOffsetMeters = blip.estimatedZOffsetMeters,
                    floorClassification = "Triangulating Elevation...",
                    physicalZoneEstimation = "Acquiring 3D RF Spatial Matrix...",
                    spatialVectorXyz = "X: ... Y: ... Z: ...",
                    aiTacticalGuidance = "Gemini AI is calculating exact 3D physical azimuth, elevation pitch, and altitude offset...",
                    searchChecklist = listOf(
                        "1. Point phone forward along target vector.",
                        "2. Watch 3D crosshair reticle align in real-time.",
                        "3. Advance towards target while monitoring distance count.",
                        "4. Follow AI elevation pitch guide."
                    ),
                    isPinpointingLoading = true
                )
            )
        }

        viewModelScope.launch {
            val pinpointResult = geminiThreatService.performAi3dPinpoint(blip, suite, heading)
            _uiState.update { state ->
                state.copy(
                    activePinpointResult = pinpointResult,
                    isPinpointingActive = true
                )
            }
        }
    }

    fun triggerAiPinpointForCurrentTarget() {
        val state = _uiState.value
        val blip = state.activeBlips.find { it.id == state.selectedTargetDeviceId || it.name == state.selectedTargetDeviceId }
            ?: state.nearestBlip
            ?: state.activeBlips.firstOrNull()

        if (blip != null) {
            startAiPinpoint(blip)
        }
    }

    fun closeAiPinpointDialog() {
        _uiState.update { 
            it.copy(
                isPinpointDialogOpen = false,
                isPinpointingActive = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        signalProvider.stopInterception()
        bleScannerService.stopScannerService()
        orientationManager?.stopListening()
        magnetometerDetector?.stopListening()
        acousticDetector?.stopListening()
        hardwareSensorSuiteManager?.stopListening()
        audioTracker.stop()

        uwbEngine.stopRangingEngine()
        wifiRttAwareManager.stopRttAwareEngine()
        bleTrackerEngine.stopTrackerEngine()
        cellularTelephonyManager.stopTelephonyEngine()
        ultrasonicAudioInterceptor.stopInterceptor()
        usbSdrManager.releaseReceiver()
    }
}
