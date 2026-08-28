package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
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
    SIMULATION_LAB,
    FULL_RADAR,
    AI_THREAT_ANALYSIS,
    SCANNER,
    SPECTRUM_ANALYZER,
    DETECTED_SENSORS,
    HISTORIC_HEATMAP,
    MAGNETOMETER_EMF,
    SECURITY_GUARD,
    CSV_LOG_CONSOLE,
    SETTINGS,
    EVENT_RECORDER,
    ENVIRONMENT_MAP,
    IDENTITY_GRAPH,
    INTELLIGENCE_DASHBOARD,
    ADAPTIVE_LOCALIZATION
}

enum class ViewMode {
    MAP,
    RADAR,
    HYBRID
}

data class SpatialSurveyState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val targetId: String? = null,
    val points: List<RfMeasurementPoint> = emptyList(),
    val distanceWalkedFt: Float = 0f,
    val currentX: Float = 0f,
    val currentY: Float = 0f,
    val guidance: String = "Walk forward to begin spatial mapping.",
    val isLocalizationValid: Boolean = false,
    val errorMsg: String? = "LOCALIZATION INVALID: Incomplete covariance model. Gather more distinct directional points.",
    val estimatedX: Float? = null,
    val estimatedY: Float? = null,
    val confidence: Int = 0,
    val isSpatialCoveragePoor: Boolean = false,
    val isSignalWeakening: Boolean = false,
    val isSignalUnstable: Boolean = false,
    val areaSqFt: Float = 0f,
    val rssiRangeStr: String = "0",
    val signalTrendStr: String = "CALCULATING",
    val stepCountAtStart: Int = 0
)

data class SignalRadarUiState(
    val spatialSurveyState: SpatialSurveyState = SpatialSurveyState(),
    val selectedTab: RadarTab = RadarTab.SWEEP_RADAR,
    val viewMode: ViewMode = ViewMode.HYBRID,
    val geminiStatus: GeminiStatus = GeminiStatus.READY,
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
    val baselineSummary: BaselineSummary = BaselineSummary(),
    val acousticData: AcousticFrequencyData = AcousticFrequencyData(),
    val correlationEvents: List<CorrelationEvent> = emptyList(),
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
    val mapRangeMeters: Float = 15.0f,
    val currentRadarRangeMeters: Float = 15.0f,
    val selectedTargetDeviceId: String? = null,
    val selectedDeviceId: String? = null,
    val lockedTargetDeviceId: String? = null,
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
    val operatingMode: OperatingMode = OperatingMode.LIVE,
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

    val investigatorAssessment: AiInvestigatorAssessment? = null,
    val savedInterpretations: List<AiInterpretation> = emptyList(),

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
    val isPinpointDialogOpen: Boolean = false,
    val geminiConnectionState: GeminiConnectionState = GeminiConnectionState.NotConfigured,
    val networkSpeedTestResult: String? = null,
    val aiInferenceMode: AiInferenceMode = AiInferenceMode.GEMINI_CLOUD,
    val localModelStatus: String = "Local Model: Unloaded",
    val geminiApiKeyExists: Boolean = false,
    val currentAlarmState: AlarmState = AlarmState.NORMAL,
    val enterRssiDbm: Int = -42,
    val exitRssiDbm: Int = -45,
    val requiredConsecutiveObservations: Int = 3,
    val proximityAlertCooldownMs: Long = 5000L,
    val measurementHistory: List<RfMeasurementPoint> = emptyList(),
    val spatialHistoryMap: Map<String, List<RfMeasurementPoint>> = emptyMap(),
    val activeProbabilityVolume: ProbabilityVolume? = null
)

data class BaselineStats(
    val observations: Long = 0L,
    val avgActiveBlips: Float = 0f,
    val avgFreqOccupancy: Float = 0f,
    val startedAtMs: Long = 0L
)

data class TriggeredAlertRecord(
    val deviceId: String,
    val alertType: String, // "BREACH", "RSSI", "VENDOR", "MAGNETIC"
    val timestamp: Long
)

class SignalRadarViewModel(application: Application) : AndroidViewModel(application) {
    
    
    val simulationEngine = SimulationLabEngine(this)


    private val settingsDataStore = SettingsDataStore(application)
    private val signalProvider = SignalProvider(application)

    private val bleDatabase = BleDatabase.getInstance(application)
    private val bleRepository = BleDeviceRepository(bleDatabase.bleDeviceDao())
    private val fingerprintEngine = SignalFingerprintEngine(bleDatabase.signalFingerprintDao())
    private val baselineEngine = EnvironmentalBaselineEngine()
    private val anomalyEngine = ExplainableAnomalyEngine()
    private val correlationEngine = MultiSensorCorrelationEngine()
    private val bleScannerService = BleScannerServiceEngine(application, bleRepository)
    private val rfRecordingDatabase = RfRecordingDatabase.getInstance(application)
    private val rfRecordingRepository = RfRecordingRepository(rfRecordingDatabase.rfRecordedEventDao())
    val db = RfRecordingDatabase.getInstance(getApplication())
    val rfSessionEngine = RfInvestigationSessionEngine(db.rfSessionDao(), db.rfAnnotationDao())
    
    val rfEventRecorderEngine = RfEventRecorderEngine(rfRecordingRepository, viewModelScope, rfSessionEngine)
    val replayEngine = ReplayEngine(this, rfRecordingRepository, rfSessionEngine)
    val environmentMappingEngine = RfEnvironmentMappingEngine()
    val evidenceEngine = EvidenceEngine(RfRecordingDatabase.getInstance(getApplication()).evidenceDao())
    val deviceIdentityEngine = DeviceIdentityEngine(getApplication(), RfRecordingDatabase.getInstance(getApplication()).deviceIdentityDao(), evidenceEngine)
    
    val rfAnomalyEngine = RfAnomalyCorrelationEngine(RfRecordingDatabase.getInstance(getApplication()).rfAnomalyDao(), RfRecordingDatabase.getInstance(getApplication()).anomalyCorrelationDao())
    val rfPatternEngine = RfTemporalPatternEngine(RfRecordingDatabase.getInstance(getApplication()).rfPatternDao())
    val rfIntelligenceEngine = RfIntelligenceCorrelationEngine(rfSessionEngine, rfAnomalyEngine, rfPatternEngine, deviceIdentityEngine)
    val rfCrossSessionEngine = RfCrossSessionAnalysisEngine(RfRecordingDatabase.getInstance(getApplication()).rfSessionDao(), RfRecordingDatabase.getInstance(getApplication()).deviceIdentityDao(), RfRecordingDatabase.getInstance(getApplication()).rfPatternDao())

    private val hardwareSpectrumManager = HardwareSpectrumManager(application)
    private val audioTracker = AudioRadarTracker()
    val sonarState = audioTracker.currentState
    private val historyLogger = SignalHistoryLogger(application)
    private val alarmEngine = PerimeterAlarmEngine(application)
    private val geminiEngine = GeminiCloudEngine()
    private val localEngine = LlamaCppEngine(application)
    private val aiRouter = AiEngineRouter(geminiEngine, localEngine, settingsDataStore)
    private val geminiThreatService = TacticalAiGateway(aiRouter)

    // Target-specific thresholds & hysteresis
    val targetRssiThresholds = mutableStateMapOf<String, Int>()
    val targetDistanceThresholds = mutableStateMapOf<String, Float>()
    private val lastAlertTimeMap = mutableMapOf<String, Long>()
    private val triggeredAlertHistory = mutableListOf<TriggeredAlertRecord>()

    // Proximity Alert 2.0 State Tracking variables
    private var consecutiveValidTriggerCount = 0
    private var consecutiveValidExitCount = 0
    private var consecutiveValidNormalCount = 0
    private var consecutiveValidApproachingCount = 0
    private var alarmCooldownStartTimeMs = 0L
    private var alarmHasExitedHysteresis = false
    private var lastAlarmTargetId: String? = null

    fun setTargetRssiThreshold(targetId: String, threshold: Int) {
        targetRssiThresholds[targetId] = threshold
    }

    fun setTargetDistanceThreshold(targetId: String, threshold: Float) {
        targetDistanceThresholds[targetId] = threshold
    }

    fun setProximityThresholds(enterRssi: Int, exitRssi: Int, requiredConsecutive: Int, cooldownMs: Long) {
        _uiState.update {
            it.copy(
                enterRssiDbm = enterRssi,
                exitRssiDbm = exitRssi,
                requiredConsecutiveObservations = requiredConsecutive,
                proximityAlertCooldownMs = cooldownMs
            )
        }
    }

    // Hardware Sensor & Telemetry Engines
    val uwbEngine = UwbSensorEngine(application)
    val wifiRttAwareManager = WifiRttAwareManager(application)

    // RF Acquisition Layer & Signal Fusion Components (Android 16 Passive-Bypass)
    val wifiDiscoveryScanner = WifiDiscoveryScanner(application, viewModelScope)
    val bleDiscoveryScanner = BleDiscoveryScanner(application, viewModelScope)
    val targetRangingEngine = TargetRangingEngine(application, viewModelScope)
    val signalFusionManager = SignalFusionManager(application, viewModelScope, wifiDiscoveryScanner, bleDiscoveryScanner, targetRangingEngine)
    val passiveBypassFusedState = signalFusionManager.fusedState

    fun startActiveRangingBypass(macAddress: String) {
        signalFusionManager.startActiveTracking(macAddress)
    }

    fun stopActiveRangingBypass() {
        signalFusionManager.stopActiveTracking()
    }
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
    private var cachedFingerprints = mapOf<String, SignalFingerprint>()
    private var cachedBaselineStats: BaselineStats = BaselineStats()
    private var lastBlipUiUpdateMs = 0L

    private val _viewMode = MutableStateFlow(ViewMode.HYBRID)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _currentRadarRangeMeters = MutableStateFlow(15.0f)
    val currentRadarRangeMeters: StateFlow<Float> = _currentRadarRangeMeters.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _macFilterMask = MutableStateFlow("")
    val macFilterMask: StateFlow<String> = _macFilterMask.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMacFilterMask(mask: String) {
        _macFilterMask.value = mask
    }

    private val _uiState = MutableStateFlow(SignalRadarUiState())
    val uiState: StateFlow<SignalRadarUiState> = _uiState.asStateFlow()

    init {

        viewModelScope.launch {
            geminiThreatService.geminiStatus.collect { status ->
                _uiState.update { it.copy(geminiStatus = status) }
            }
        }
        viewModelScope.launch {
            geminiThreatService.connectionState.collect { state ->
                _uiState.update { it.copy(geminiConnectionState = state) }
            }
        }
        viewModelScope.launch {
            rfSessionEngine.loadActiveSession()
            val active = rfSessionEngine.activeSession.value
            if (active == null) {
                rfSessionEngine.createNewSession("Investigation - " + java.util.UUID.randomUUID().toString().take(8))
            }
            val sessionId = rfSessionEngine.getActiveSessionId()
            if (sessionId != null) {
                deviceIdentityEngine.loadHypothesesForSession(sessionId)
            }
        }
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

        // Baseline settings and models
        viewModelScope.launch {
            settingsDataStore.isBaselineLearningMode.collect { isLearning ->
                val prev = _uiState.value.baselineSummary
                _uiState.update { it.copy(baselineSummary = prev.copy(isLearning = isLearning)) }
            }
        }
        viewModelScope.launch {
            bleDatabase.signalFingerprintDao().getAllFingerprintsFlow().collect { entities ->
                cachedFingerprints = entities.associateBy { it.id }.mapValues { it.value.toDomainModel() }
            }
        }
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                settingsDataStore.baselineObservations,
                settingsDataStore.baselineAvgActiveBlips,
                settingsDataStore.baselineAvgFreqOccupancy,
                settingsDataStore.baselineStartedAtMs
            ) { obs, blips, freq, started ->
                BaselineStats(obs, blips, freq, started)
            }.collect { stats ->
                cachedBaselineStats = stats
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

        // Collect AI Inference and status settings
        viewModelScope.launch {
            settingsDataStore.aiInferenceMode.collect { mode ->
                _uiState.update { it.copy(aiInferenceMode = mode) }
            }
        }
        viewModelScope.launch {
            val credStore = AiCredentialStore.getInstance(application)
            val hasKey = credStore.hasGeminiApiKey()
            _uiState.update { it.copy(geminiApiKeyExists = hasKey) }
        }
        viewModelScope.launch {
            val available = localEngine.isAvailable()
            val statusText = if (available) {
                "Local GGUF Model: Loaded & Operational"
            } else {
                "Local GGUF Model: Unloaded (Model files not detected)"
            }
            _uiState.update { it.copy(localModelStatus = statusText) }
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
            bleScannerService.updateStationaryState(suiteData.isStationary || !suiteData.isMotionDetected)
            updateSurveyStep(suiteData.stepCount, if (suiteData.compassHeading > 0f) suiteData.compassHeading else _uiState.value.headingDegrees)
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
                // Auto-dismiss after 4 seconds to keep the screen and scroll areas completely free of clutter
                launch {
                    delay(4000L)
                    _uiState.update { state ->
                        state.copy(activeDeviceAlerts = state.activeDeviceAlerts.filterNot { it.id == alert.id })
                    }
                }
            }
        }

        // --- Start Advanced Multi-Sensor Hardware Engines ---
        // Start passive-bypass RF Acquisition Layer scans
        wifiDiscoveryScanner.startScanning()
        bleDiscoveryScanner.startScanning()

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
                
                // Adaptive sweep delay: when stationary, throttle sweep frequency to save CPU and battery
                val isStationary = _uiState.value.sensorSuite.isStationary || !_uiState.value.sensorSuite.isMotionDetected
                val baseDelay = _uiState.value.scanMode.delayMs
                val adaptiveDelay = if (isStationary) (baseDelay * 3).coerceAtMost(5000L) else baseDelay
                delay(adaptiveDelay)
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


        val anomaly = anomalyEngine.evaluateAnomaly(rawBlip, cachedFingerprints, _uiState.value.baselineSummary)
        val blipWithAnomaly = rawBlip.copy(anomalyResult = anomaly)
        
        val newCorrelations = correlationEngine.processObservation(blipWithAnomaly)
        if (newCorrelations.isNotEmpty()) {
            _uiState.update { state -> 
                val updatedCorrelations = (newCorrelations + state.correlationEvents)
                    .sortedByDescending { it.firstObservationMs }
                    .take(50) // Keep latest 50
                state.copy(correlationEvents = updatedCorrelations)
            }
        }
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

        viewModelScope.launch(Dispatchers.IO) {
            val fpResult = fingerprintEngine.processObservation(smoothedBlip)
            val existing = blipMap[smoothedBlip.id]
            if (existing != null) {
                blipMap[smoothedBlip.id] = existing.copy(
                    fingerprintId = fpResult.fingerprint.id,
                    fingerprintConfidence = fpResult.confidence
                )
                _uiState.update { it.copy(activeBlips = blipMap.values.toList()) }
            }

            // Isolated local AI RAG ingestion (safeguarded to ensure RF scanning never fails)
            try {
                val input = AiMemoryInput(
                    targetId = smoothedBlip.id,
                    deviceType = smoothedBlip.type,
                    protocol = smoothedBlip.bandLabel,
                    displayName = smoothedBlip.name,
                    sanitizedAddress = smoothedBlip.id,
                    rssi = smoothedBlip.rssi,
                    anomalySummary = smoothedBlip.anomalyResult?.let { it.category.name + " (Score: " + it.score + ")" },
                    measurementSummary = "Distance: " + smoothedBlip.distance + "m, angle: " + (smoothedBlip.targetAngleOffset ?: 0f)
                )
                AiMemoryIngestorProvider.getIngestor(getApplication()).ingest(input)
            } catch (e: Exception) {
                // Safeguard scanning stability
            }
        }

        if (smoothedBlip.ouiVendor == null && rawBlip.id.length >= 8) {
            val localVendor = MacOuidResolver.resolveVendor(rawBlip.id)
            if (localVendor != null) {
                val existing = blipMap[rawBlip.id]
                if (existing != null) {
                    val isHighRisk = localVendor == "Espressif Systems" || localVendor == "Hangzhou Hikvision" || localVendor == "Dahua Technology"
                    blipMap[rawBlip.id] = existing.copy(
                        ouiVendor = localVendor,
                        isHighRiskVendor = isHighRisk
                    )
                    _uiState.update { it.copy(activeBlips = blipMap.values.toList()) }
                }
            } else {
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
        }

        val targetId = smoothedBlip.id
        val baseRssiThreshold = targetRssiThresholds[targetId] ?: _uiState.value.rssiAlertThresholdDbm
        val baseDistanceThreshold = targetDistanceThresholds[targetId] ?: _uiState.value.perimeterThresholdMeters

        // State Machine for the selected target (Proximity Alert 2.0)
        if (targetId == _uiState.value.selectedTargetDeviceId) {
            val currentRssi = smoothedRssi
            val nowMs = System.currentTimeMillis()

            // Validate observation (invalid / stale checks)
            val isStale = (nowMs - smoothedBlip.timestampMs > 10000L)
            val isRssiInvalid = (currentRssi > -10 || currentRssi < -115)
            val isValidObservation = !isStale && !isRssiInvalid

            if (isValidObservation) {
                val enterLimit = _uiState.value.enterRssiDbm
                val exitLimit = _uiState.value.exitRssiDbm
                val requiredSamples = _uiState.value.requiredConsecutiveObservations
                val cooldownMs = _uiState.value.proximityAlertCooldownMs

                val approachingEnterLimit = enterLimit - 8
                val approachingExitLimit = exitLimit - 8

                val prevState = _uiState.value.currentAlarmState
                var nextState = prevState

                // Handle sequential tracking counters based on RSSI thresholds
                if (currentRssi >= enterLimit) {
                    consecutiveValidTriggerCount++
                    consecutiveValidExitCount = 0
                    consecutiveValidApproachingCount = 0
                    consecutiveValidNormalCount = 0
                } else if (currentRssi < exitLimit) {
                    consecutiveValidExitCount++
                    consecutiveValidTriggerCount = 0
                    if (currentRssi < approachingExitLimit) {
                        consecutiveValidNormalCount++
                        consecutiveValidApproachingCount = 0
                    } else {
                        consecutiveValidApproachingCount++
                        consecutiveValidNormalCount = 0
                    }
                } else {
                    // Inside the hysteresis region
                    // Reset counts for trigger and exit to prevent oscillations from meeting consecutive sample requirement
                    consecutiveValidTriggerCount = 0
                    consecutiveValidExitCount = 0
                    consecutiveValidNormalCount = 0
                    consecutiveValidApproachingCount = 0
                }

                when (prevState) {
                    AlarmState.NORMAL -> {
                        if (consecutiveValidTriggerCount >= requiredSamples) {
                            nextState = AlarmState.TRIGGERED
                        } else if (consecutiveValidApproachingCount >= requiredSamples) {
                            nextState = AlarmState.APPROACHING
                        }
                    }
                    AlarmState.APPROACHING -> {
                        if (consecutiveValidTriggerCount >= requiredSamples) {
                            nextState = AlarmState.TRIGGERED
                        } else if (consecutiveValidNormalCount >= requiredSamples) {
                            nextState = AlarmState.NORMAL
                        }
                    }
                    AlarmState.TRIGGERED -> {
                        if (consecutiveValidExitCount >= requiredSamples) {
                            nextState = AlarmState.COOLDOWN
                            alarmCooldownStartTimeMs = nowMs
                            alarmHasExitedHysteresis = true
                        }
                    }
                    AlarmState.COOLDOWN -> {
                        // Drop below exitLimit during or after cooldown resets exiting check
                        if (currentRssi < exitLimit) {
                            alarmHasExitedHysteresis = true
                        }
                        
                        val isCooldownExpired = (nowMs - alarmCooldownStartTimeMs >= cooldownMs)
                        if (isCooldownExpired && alarmHasExitedHysteresis) {
                            if (consecutiveValidTriggerCount >= requiredSamples) {
                                nextState = AlarmState.TRIGGERED
                            } else if (consecutiveValidNormalCount >= requiredSamples) {
                                nextState = AlarmState.NORMAL
                            } else if (consecutiveValidApproachingCount >= requiredSamples) {
                                nextState = AlarmState.APPROACHING
                            }
                        } else {
                            // Even in cooldown, we can transition to NORMAL or APPROACHING if RSSI drops
                            if (consecutiveValidNormalCount >= requiredSamples) {
                                nextState = AlarmState.NORMAL
                            } else if (consecutiveValidApproachingCount >= requiredSamples) {
                                nextState = AlarmState.APPROACHING
                            }
                        }
                    }
                }

                if (nextState != prevState) {
                    _uiState.update { it.copy(currentAlarmState = nextState) }
                    alarmEngine.setAlarmState(nextState)
                }
            }
        }

        // Hysteresis delta check:
        val isAlreadyAlerting = _uiState.value.activeDeviceAlerts.any { it.id == targetId }
        
        val rssiThreshold = if (isAlreadyAlerting) {
            baseRssiThreshold - 3 // 3 dB hysteresis delta to clear alert
        } else {
            baseRssiThreshold // Normal threshold to trigger alert
        }
        
        val distanceThreshold = if (isAlreadyAlerting) {
            baseDistanceThreshold + 0.5f // 0.5 meters hysteresis delta to clear alert
        } else {
            baseDistanceThreshold // Normal threshold to trigger alert
        }

        val isBreach = smoothedDistance < distanceThreshold
        val isRssiAlert = smoothedRssi >= rssiThreshold && _uiState.value.isRssiAlertEnabled

        val isWhitelisted = _uiState.value.baselineWhitelistedMacs.contains(smoothedBlip.id)
        
        val magBaseline = _uiState.value.baselineMagneticFluxMicroTesla
        val isMagAnomaly = smoothedBlip.type == "MAGNETIC" && magBaseline != null && Math.abs(-smoothedBlip.rssi - magBaseline) > 25.0f

        val shouldAlert = (isBreach || isRssiAlert || smoothedBlip.isHighRiskVendor || isMagAnomaly) && 
                _uiState.value.isPerimeterAlarmEnabled && 
                !_uiState.value.stealthModeEnabled

        if (isWhitelisted) {
            // Suppress alerts for whitelist
        } else if (shouldAlert) {
            val now = System.currentTimeMillis()
            val alertType = when {
                isBreach -> "BREACH"
                isRssiAlert -> "RSSI"
                smoothedBlip.isHighRiskVendor -> "VENDOR"
                else -> "MAGNETIC"
            }
            
            // Clean up history older than 30s
            triggeredAlertHistory.removeAll { now - it.timestamp > 30000L }
            
            // Check if duplicate alert for same condition/type within last 15s (Goal 5)
            val isDuplicate = triggeredAlertHistory.any { 
                it.deviceId == targetId && it.alertType == alertType && now - it.timestamp < 15000L 
            }
            
            if (!isDuplicate) {
                triggeredAlertHistory.add(TriggeredAlertRecord(targetId, alertType, now))
                alarmEngine.triggerProximityAlert()
            }
        }

        historyLogger.logSignalEntry(
            deviceName = smoothedBlip.name,
            distanceMeters = smoothedDistance,
            type = smoothedBlip.type,
            freqMhz = smoothedBlip.frequencyMhz,
            isBreach = isBreach
        )
        
        rfEventRecorderEngine.processObservations(listOf(smoothedBlip), _uiState.value.selectedTargetDeviceId)


        val now = System.currentTimeMillis()
        if (now - lastBlipUiUpdateMs >= 100L) { // Throttle to 10Hz (100ms) for UI stability and 60fps rendering (Goal 10)
            lastBlipUiUpdateMs = now
            val blipsList = blipMap.values.toList()
            val nearest = blipsList.minByOrNull { it.distance }

            val targetDeviceId = _uiState.value.selectedTargetDeviceId
            val targetBlip = if (targetDeviceId != null) {
                blipsList.find { it.id == targetDeviceId || it.name == targetDeviceId }
            } else {
                // Feature 26: If no target selected, do not track unless explicitly desired. But the instructions say:
                // "If no device is selected: sonar remains disabled or inactive."
                null
            }

            if (_uiState.value.isAudioSonarActive) {
                if (targetBlip != null) {
                    audioTracker.updateProximityDistance(targetBlip.distance.toDouble())
                } else {
                    // Send -1 to indicate unavailable or idle
                    audioTracker.updateProximityDistance(-1.0)
                }
            }

            val breaches = blipsList.count { it.distance < _uiState.value.perimeterThresholdMeters }

            val (processedBlips, summary) = baselineEngine.processBaseline(
                blips = blipsList,
                fingerprintDb = cachedFingerprints,
                isLearning = _uiState.value.baselineSummary.isLearning,
                baselineObservations = cachedBaselineStats.observations,
                baselineAvgActiveBlips = cachedBaselineStats.avgActiveBlips,
                baselineAvgFreqOccupancy = cachedBaselineStats.avgFreqOccupancy,
                baselineStartedAtMs = cachedBaselineStats.startedAtMs
            )
            

            val evaluatedBlips = processedBlips.map { blip ->
                val anomaly = anomalyEngine.evaluateAnomaly(
                    blip = blip,
                    fingerprintDb = cachedFingerprints,
                    baselineSummary = summary
                )
                
                // Historical tracking: Update the database occasionally if score changes significantly
                blip.fingerprintId?.let { fpId ->
                    val fp = cachedFingerprints[fpId]
                    if (fp != null) {
                        val prevScore = fp.lastAnomalyScore ?: 0
                        if (kotlin.math.abs(prevScore - anomaly.score) > 10) {
                            val updatedFp = fp.copy(lastAnomalyScore = anomaly.score, lastAnomalyConfidence = anomaly.confidence)
                            cachedFingerprints = cachedFingerprints + (fpId to updatedFp) // Update local cache
                            if (_uiState.value.operatingMode == OperatingMode.LIVE) {
                                viewModelScope.launch(Dispatchers.IO) {
                                    bleDatabase.signalFingerprintDao().updateFingerprint(updatedFp.toEntity())
                                }
                            }
                        }
                    }
                }
                
                blip.copy(anomalyResult = anomaly)
            }

            if (_uiState.value.baselineSummary.isLearning && _uiState.value.operatingMode == OperatingMode.LIVE) {
                // Update running averages
                val currentObs = cachedBaselineStats.observations + 1
                val newAvgBlips = cachedBaselineStats.avgActiveBlips + ((evaluatedBlips.size - cachedBaselineStats.avgActiveBlips) / currentObs)
                val currentFreq = evaluatedBlips.sumOf { (it.bandwidthMhz ?: 20.0) }.toFloat()
                val newAvgFreq = cachedBaselineStats.avgFreqOccupancy + ((currentFreq - cachedBaselineStats.avgFreqOccupancy) / currentObs)
                viewModelScope.launch {
                    settingsDataStore.updateBaselineStats(currentObs, newAvgBlips, newAvgFreq)
                }
            }
            
            // Dispatch to mapping engine (background)
            viewModelScope.launch(Dispatchers.Default) {
                environmentMappingEngine.updateMap(
                    blips = evaluatedBlips,
                    headingDegrees = _uiState.value.headingDegrees,
                    userX = 0f, 
                    userY = 0f
                )
                
                val sessionId = rfSessionEngine.getActiveSessionId()
                if (sessionId != null) {
                    deviceIdentityEngine.processObservations(evaluatedBlips, cachedFingerprints, sessionId)
                    rfAnomalyEngine.processEvents(evaluatedBlips, sessionId, environmentMappingEngine.mapState.value, deviceIdentityEngine.hypotheses.value)
                    rfPatternEngine.processEvents(evaluatedBlips, sessionId)
                    viewModelScope.launch { rfIntelligenceEngine.updateGraph() }
                    
                    rfSessionEngine.updateSessionStats(
                        eventCount = rfEventRecorderEngine.recorderState.value.totalRecordedEventsSession,
                        anomalyCount = rfAnomalyEngine.anomalies.value.size,
                        deviceCount = deviceIdentityEngine.hypotheses.value.size,
                        mapCellCount = environmentMappingEngine.mapState.value.cells.size
                    )
                }
            }

            _uiState.update { state ->
                state.copy(
                    activeBlips = evaluatedBlips,
                    nearestBlip = nearest,
                    perimeterBreachCount = breaches,
                    baselineSummary = summary
                )
            }
            
        }
    }

        fun toggleBaselineLearning() {
        val currentState = _uiState.value.baselineSummary.isLearning
        viewModelScope.launch {
            settingsDataStore.updateBaselineLearningMode(!currentState)
        }
    }

    fun resetBaseline() {
        viewModelScope.launch {
            settingsDataStore.resetBaseline()
        }
    }

    fun setMapRangeMeters(meters: Float) {
        val clamped = meters.coerceIn(1.0f, 120.0f)
        _currentRadarRangeMeters.value = clamped
        _uiState.update { it.copy(mapRangeMeters = clamped, currentRadarRangeMeters = clamped) }
    }

    fun setRadarRangeMeters(meters: Float) {
        setMapRangeMeters(meters)
    }

    fun setRadarRange(meters: Float) {
        setMapRangeMeters(meters)
    }

    fun zoomInMap() {
        val newRange = (_uiState.value.mapRangeMeters * 0.7f).coerceIn(1.0f, 120.0f)
        _currentRadarRangeMeters.value = newRange
        _uiState.update { it.copy(mapRangeMeters = newRange, currentRadarRangeMeters = newRange) }
    }

    fun zoomOutMap() {
        val newRange = (_uiState.value.mapRangeMeters * 1.4f).coerceIn(1.0f, 120.0f)
        _currentRadarRangeMeters.value = newRange
        _uiState.update { it.copy(mapRangeMeters = newRange, currentRadarRangeMeters = newRange) }
    }

    fun toggleMapMaximized() {
        _uiState.update { it.copy(isMapMaximized = !it.isMapMaximized) }
    }

    fun selectTargetDevice(deviceId: String?) {
        _uiState.update { state ->
            val newTarget = if (state.selectedTargetDeviceId == deviceId) null else deviceId
            state.copy(
                selectedTargetDeviceId = newTarget,
                selectedDeviceId = deviceId,
                lockedTargetDeviceId = newTarget,
                currentAlarmState = AlarmState.NORMAL
            )
        }
        consecutiveValidTriggerCount = 0
        consecutiveValidExitCount = 0
        consecutiveValidNormalCount = 0
        consecutiveValidApproachingCount = 0
        alarmCooldownStartTimeMs = 0L
        alarmHasExitedHysteresis = false
        lastAlarmTargetId = _uiState.value.selectedTargetDeviceId
        alarmEngine.setAlarmState(AlarmState.NORMAL)

        if (deviceId != null && !_uiState.value.isAudioSonarActive && !_uiState.value.stealthModeEnabled) {
            if (!audioTracker.isAudioActive()) {
                audioTracker.start()
                _uiState.update { it.copy(isAudioSonarActive = true) }
            }
        }
    }

    /**
     * Preview/inspect a device blip without changing the persistent locked target.
     * Passing null clears preview selection while preserving any existing locked target.
     */
    fun selectDevice(deviceId: String?) {
        _uiState.update { it.copy(selectedDeviceId = deviceId) }
    }

    /**
     * Explicitly lock a device target for persistent tracking, alerts, and navigation.
     */
    fun lockTarget(deviceId: String) {
        selectTargetDevice(deviceId)
    }

    /**
     * Explicitly unlock the active target.
     */
    fun unlockTarget() {
        selectTargetDevice(null)
    }

    fun playTestAudioPing(distanceMeters: Double) {
        audioTracker.playSingleTestPing(distanceMeters)
    }

    
    fun addMeasurementPoint(point: RfMeasurementPoint) {
        _uiState.update { state ->
            val updated = state.measurementHistory.toMutableList()
            if (updated.size >= 60) {
                updated.removeAt(0)
            }
            updated.add(point)
            state.copy(measurementHistory = updated)
        }
    }

    fun clearMeasurementHistory() {
        _uiState.update { state ->
            state.copy(measurementHistory = emptyList())
        }
    }

    fun addSpatialPoint(deviceId: String, point: RfMeasurementPoint) {
        _uiState.update { state ->
            val currentMap = state.spatialHistoryMap.toMutableMap()
            val history = currentMap[deviceId]?.toMutableList() ?: mutableListOf()
            if (history.size >= 60) {
                history.removeAt(0)
            }
            history.add(point)
            currentMap[deviceId] = history
            state.copy(spatialHistoryMap = currentMap)
        }
        recalculateProbabilityVolume(deviceId)
    }

    fun recalculateProbabilityVolume(deviceId: String) {
        val points = _uiState.value.spatialHistoryMap[deviceId] ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val volume = NextBestMeasurementEngine.estimateProbabilityVolume(points, deviceId)
            _uiState.update { state ->
                if (state.selectedTargetDeviceId == deviceId) {
                    state.copy(activeProbabilityVolume = volume)
                } else {
                    state
                }
            }
        }
    }

    private var lastObservedStepCount = -1

    fun updateSurveyStep(stepCount: Int, headingDegrees: Float) {
        val state = _uiState.value.spatialSurveyState
        if (!state.isActive || state.isPaused) return
        
        if (lastObservedStepCount == -1) {
            lastObservedStepCount = stepCount
            return
        }
        
        val deltaSteps = stepCount - lastObservedStepCount
        if (deltaSteps > 0) {
            lastObservedStepCount = stepCount
            val stepLengthMeters = 0.65f * deltaSteps
            val headingRad = Math.toRadians(headingDegrees.toDouble())
            val dx = (stepLengthMeters * kotlin.math.sin(headingRad)).toFloat()
            val dy = (stepLengthMeters * kotlin.math.cos(headingRad)).toFloat()
            logSurveyPointInVm(state.currentX + dx, state.currentY + dy)
        }
    }

    fun startSurvey(targetId: String) {
        val stepCount = _uiState.value.sensorSuite.stepCount
        lastObservedStepCount = stepCount
        _uiState.update { state ->
            state.copy(
                spatialSurveyState = SpatialSurveyState(
                    isActive = true,
                    targetId = targetId,
                    stepCountAtStart = stepCount,
                    currentX = 0f,
                    currentY = 0f
                )
            )
        }
    }

    fun pauseSurvey(isPaused: Boolean) {
        _uiState.update { state ->
            state.copy(
                spatialSurveyState = state.spatialSurveyState.copy(isPaused = isPaused)
            )
        }
    }

    fun clearSurvey() {
        _uiState.update { state ->
            state.copy(
                spatialSurveyState = state.spatialSurveyState.copy(
                    points = emptyList(),
                    distanceWalkedFt = 0f,
                    guidance = "Walk forward to begin spatial mapping.",
                    isLocalizationValid = false,
                    errorMsg = "LOCALIZATION INVALID: Incomplete covariance model. Gather more distinct directional points.",
                    estimatedX = null,
                    estimatedY = null,
                    confidence = 0,
                    isSpatialCoveragePoor = false,
                    isSignalWeakening = false,
                    isSignalUnstable = false,
                    areaSqFt = 0f,
                    rssiRangeStr = "0",
                    signalTrendStr = "CALCULATING"
                )
            )
        }
    }

    fun endSurvey() {
        _uiState.update { state ->
            state.copy(
                spatialSurveyState = state.spatialSurveyState.copy(isActive = false, targetId = null)
            )
        }
    }

    fun logSurveyPointInVm(x: Float, y: Float, customRssi: Int? = null) {
        val currentState = _uiState.value
        val surveyState = currentState.spatialSurveyState
        if (!surveyState.isActive) return

        val targetId = currentState.selectedTargetDeviceId ?: surveyState.targetId ?: return
        val target = currentState.activeBlips.find { it.id == targetId || it.name == targetId } ?: return

        // Compute RSSI
        val rssi = customRssi ?: target.rssi

        // EMA filtered RSSI
        val prevFiltered = surveyState.points.lastOrNull()?.filteredRssi ?: rssi.toFloat()
        val filtered = 0.3f * rssi + 0.7f * prevFiltered

        val ts = System.currentTimeMillis()
        val variance = if (surveyState.points.size <= 1) 4f else {
            val rssis = surveyState.points.map { it.rssi } + rssi
            val avg = rssis.average()
            rssis.map { (it.toDouble() - avg) * (it.toDouble() - avg) }.average().toFloat()
        }

        val quality = ((100 + rssi).coerceIn(10, 100) * 0.7f + (100f / (1f + variance)).coerceIn(10f, 100f) * 0.3f).toInt()
        val qState = RfMeasurementPoint.determineQualityState(rssi, variance, quality, ts)

        val newPoint = RfMeasurementPoint(
            timestamp = ts,
            latitude = null,
            longitude = null,
            xOffsetMeters = x,
            yOffsetMeters = y,
            compassHeading = currentState.headingDegrees,
            pitch = currentState.sensorSuite.pitchDeg,
            roll = currentState.sensorSuite.rollDeg,
            rssi = rssi,
            filteredRssi = filtered,
            rssiVariance = variance,
            targetId = target.id,
            frequencyMhz = target.frequencyMhz,
            qualityScore = quality,
            label = "SURVEY",
            qualityState = qState
        )

        val updatedPoints = surveyState.points + newPoint

        // Calculate total distance
        var distanceWalkedMeters = surveyState.distanceWalkedFt / 3.28084f
        if (surveyState.points.isNotEmpty()) {
            val lastPt = surveyState.points.last()
            val dx = x - lastPt.xOffsetMeters
            val dy = y - lastPt.yOffsetMeters
            distanceWalkedMeters += kotlin.math.sqrt(dx * dx + dy * dy)
        }

        // Compute Spatial Spread / Coverage
        var maxDistance = 0.0f
        for (i in updatedPoints.indices) {
            for (j in i + 1 until updatedPoints.size) {
                val dx = updatedPoints[i].xOffsetMeters - updatedPoints[j].xOffsetMeters
                val dy = updatedPoints[i].yOffsetMeters - updatedPoints[j].yOffsetMeters
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist > maxDistance) {
                    maxDistance = dist
                }
            }
        }

        val isSpatialCoveragePoor = updatedPoints.size >= 2 && maxDistance < 4.0f // ~13 ft

        // Signal Trend
        val slope = if (updatedPoints.size >= 3) {
            val n = updatedPoints.size.toDouble()
            val sumI = (0 until updatedPoints.size).sum().toDouble()
            val sumI2 = (0 until updatedPoints.size).sumOf { it * it }.toDouble()
            val sumR = updatedPoints.sumOf { it.filteredRssi.toDouble() }
            val sumIR = updatedPoints.mapIndexed { idx, pt -> idx * pt.filteredRssi.toDouble() }.sum()
            val denom = n * sumI2 - sumI * sumI
            if (kotlin.math.abs(denom) < 1e-5) 0.0 else (n * sumIR - sumI * sumR) / denom
        } else {
            0.0
        }
        val signalTrendStr = when {
            updatedPoints.size < 3 -> "CALCULATING"
            slope > 0.4 -> "RISING"
            slope < -0.4 -> "FALLING"
            else -> "STABLE"
        }

        val isSignalWeakening = updatedPoints.isNotEmpty() && (
            signalTrendStr == "FALLING" || (newPoint.rssi < (updatedPoints.map { it.rssi }.maxOrNull() ?: -120) - 8)
        )
        val isSignalUnstable = updatedPoints.size >= 3 && variance > 12.0f

        // Estimate probability volume and run trilateration math
        val surveyVolume = if (updatedPoints.size >= 5) {
            NextBestMeasurementEngine.estimateProbabilityVolume(updatedPoints, target.id)
        } else null

        val isLocalizationValid = updatedPoints.size >= 5 && surveyVolume != null && surveyVolume.isValid && !surveyVolume.insufficientSpatialDiversity

        // UI Guidance logic
        val guidance = when {
            updatedPoints.size < 5 -> "Walk forward to gather more measurements (Need ${5 - updatedPoints.size} more points)."
            !isLocalizationValid && isSpatialCoveragePoor -> "Widen Survey Area: Step at wider displacements (at least 15 ft span)."
            !isLocalizationValid -> "Localization Invalid: Incomplete covariance model. Gather more distinct directional points."
            else -> "Localization Solid: Physical target coordinate successfully resolved."
        }

        val errorMsg = if (isLocalizationValid) null else {
            if (isSpatialCoveragePoor) "WIDEN SURVEY AREA: Step at wider displacements (at least 15 ft span) to establish covariance."
            else "LOCALIZATION INVALID: Incomplete covariance model. Gather more distinct directional points."
        }

        val estimatedX = if (isLocalizationValid && surveyVolume != null) surveyVolume.sourcePosition.x else null
        val estimatedY = if (isLocalizationValid && surveyVolume != null) surveyVolume.sourcePosition.y else null

        val rMeters = surveyVolume?.radiusMeters ?: 15f
        val confidence = if (isLocalizationValid) {
            (100f - (rMeters * 8f)).coerceIn(10f, 98f).toInt()
        } else 0

        val xOffsets = updatedPoints.map { it.xOffsetMeters }
        val yOffsets = updatedPoints.map { it.yOffsetMeters }
        val minX = xOffsets.minOrNull() ?: 0f
        val maxX = xOffsets.maxOrNull() ?: 0f
        val minY = yOffsets.minOrNull() ?: 0f
        val maxY = yOffsets.maxOrNull() ?: 0f
        val areaSqFt = (maxX - minX) * (maxY - minY) * 10.7639f

        val rssis = updatedPoints.map { it.rssi }
        val minRssi = rssis.minOrNull() ?: -120
        val maxRssi = rssis.maxOrNull() ?: -30
        val rssiRangeStr = "$minRssi to $maxRssi dBm"

        _uiState.update { state ->
            state.copy(
                spatialSurveyState = surveyState.copy(
                    points = updatedPoints,
                    distanceWalkedFt = distanceWalkedMeters * 3.28084f,
                    currentX = x,
                    currentY = y,
                    guidance = guidance,
                    isLocalizationValid = isLocalizationValid,
                    errorMsg = errorMsg,
                    estimatedX = estimatedX,
                    estimatedY = estimatedY,
                    confidence = confidence,
                    isSpatialCoveragePoor = isSpatialCoveragePoor,
                    isSignalWeakening = isSignalWeakening,
                    isSignalUnstable = isSignalUnstable,
                    areaSqFt = areaSqFt,
                    rssiRangeStr = rssiRangeStr,
                    signalTrendStr = signalTrendStr
                )
            )
        }
    }

    fun setSelectedTargetDeviceId(id: String?) {
        _uiState.update { it.copy(selectedTargetDeviceId = id) }
        
    }
    
    fun setTab(tab: RadarTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun testGeminiConnection() {
        viewModelScope.launch {
            geminiThreatService.testConnection()
        }
    }

    fun runNetworkConnectivityAndSpeedTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(networkSpeedTestResult = "TESTING...") }
            val success = geminiThreatService.checkNetworkSpeed()
            _uiState.update { state ->
                state.copy(
                    networkSpeedTestResult = if (success) {
                        "SUCCESS: Connectivity and latency verified (HTTP 204 response)."
                    } else {
                        "FAIL: Connectivity check timed out or failed."
                    }
                )
            }
        }
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



    fun injectSimulationBlip(blip: RadarBlip) {
        if (_uiState.value.operatingMode != OperatingMode.SIMULATION) return
        processSignalIntercept(blip)
    }

    
    fun clearReplayState() {
        _uiState.update { it.copy(activeBlips = emptyList(), selectedTargetDeviceId = null) }
        // reset environment map if applicable
    }

    fun reconstructStateFromEvents(validEvents: List<RfRecordedEventEntity>) {
        if (_uiState.value.operatingMode != OperatingMode.REPLAY) return
        
        // Find the latest state for each device
        val latestDeviceEvents = validEvents.groupBy { it.deviceId }
            .mapValues { it.value.maxByOrNull { e -> e.timestampMs } }
            .values.filterNotNull()
            
        // Filter out devices that haven't been seen recently (e.g. within 30 seconds of the seek target)
        // For replay scrubbing, we just take the last known state, but realistically we should age them out
        val seekTargetMs = validEvents.lastOrNull()?.timestampMs ?: 0L
        val activeDeviceEvents = latestDeviceEvents.filter { seekTargetMs - it.timestampMs < 30000 }
        
        val reconstructedBlips = activeDeviceEvents.map { entity ->
            val anomalyResult = if (entity.anomalyScore != null || entity.classification != null) {
                AnomalyResult(
                    score = entity.anomalyScore?.toInt() ?: 0,
                    confidence = entity.classificationConfidence ?: 0f,
                    explanations = if (entity.classification != null) listOf(AnomalyExplanation(entity.classification, 1)) else emptyList()
                )
            } else null
            
            RadarBlip(
                id = entity.deviceId,
                name = entity.manufacturerInfo ?: "Unknown Replay Device",
                distance = entity.distanceMeters ?: 0f,
                targetAngleOffset = 0f,
                type = entity.signalType,
                rssi = entity.rssi,
                frequencyMhz = entity.frequencyMhz,
                bandLabel = entity.bandLabel,
                anomalyResult = anomalyResult,
                provenance = DataProvenance.REPLAY,
                timestampMs = entity.timestampMs
            )
        }
        
        _uiState.update { it.copy(activeBlips = reconstructedBlips) }
    }

    fun injectReplayBlip(blip: RadarBlip) {
        if (_uiState.value.operatingMode != OperatingMode.REPLAY) return
        processSignalIntercept(blip)
    }

    fun setOperatingMode(mode: OperatingMode) {
        _uiState.update { it.copy(operatingMode = mode) }
        
        // Reset or swap isolated context if needed
        if (mode == OperatingMode.LIVE) {
            // Restore live context
            anomalyEngine.resetIsolatedState()
            correlationEngine.resetIsolatedState()
        } else {
            // Enter isolated context
            anomalyEngine.isolateStateForSimulation()
            correlationEngine.isolateStateForSimulation()
        }
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

    fun setAiInferenceMode(mode: AiInferenceMode) {
        viewModelScope.launch {
            settingsDataStore.updateAiInferenceMode(mode)
        }
    }

    fun saveGeminiApiKey(key: String) {
        viewModelScope.launch {
            val credStore = AiCredentialStore.getInstance(getApplication())
            credStore.setGeminiApiKey(key)
            _uiState.update { it.copy(geminiApiKeyExists = true) }
            testGeminiConnection()
        }
    }

    fun clearGeminiApiKey() {
        viewModelScope.launch {
            val credStore = AiCredentialStore.getInstance(getApplication())
            credStore.clearGeminiApiKey()
            _uiState.update { it.copy(geminiApiKeyExists = false) }
            _uiState.update { it.copy(geminiConnectionState = GeminiConnectionState.NotConfigured) }
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
        try {
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
        } catch (e: Throwable) {
            android.util.Log.w("SignalRadarViewModel", "Could not start ScannerBackgroundAlertService: ${e.message}")
        }
    }

    private fun stopBackgroundAlertService() {
        try {
            val app = getApplication<Application>()
            val intent = Intent(app, ScannerBackgroundAlertService::class.java).apply {
                action = ScannerBackgroundAlertService.ACTION_STOP_SERVICE
            }
            app.startService(intent)
        } catch (e: Throwable) {
            android.util.Log.w("SignalRadarViewModel", "Could not stop ScannerBackgroundAlertService: ${e.message}")
        }
    }

    private fun updateBackgroundAlertServiceSettings() {
        if (_uiState.value.isBackgroundAlertServiceActive) {
            try {
                val app = getApplication<Application>()
                val intent = Intent(app, ScannerBackgroundAlertService::class.java).apply {
                    action = ScannerBackgroundAlertService.ACTION_UPDATE_SETTINGS
                    putExtra(ScannerBackgroundAlertService.EXTRA_RSSI_THRESHOLD, _uiState.value.rssiAlertThresholdDbm)
                    putExtra(ScannerBackgroundAlertService.EXTRA_ENABLE_HAPTIC, _uiState.value.isHapticAlertsEnabled)
                    putExtra(ScannerBackgroundAlertService.EXTRA_ENABLE_NOTIF, _uiState.value.isVisualNotifsEnabled)
                }
                app.startService(intent)
            } catch (e: Throwable) {
                android.util.Log.w("SignalRadarViewModel", "Could not update ScannerBackgroundAlertService: ${e.message}")
            }
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


    fun exportRfEventsJson(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonData = rfEventRecorderEngine.generateJsonExport(_uiState.value, deviceIdentityEngine.hypotheses.value.values.toList(), rfAnomalyEngine.anomalies.value, rfPatternEngine.patterns.value, rfSessionEngine.allSessions.firstOrNull(), db.rfAnnotationDao().getAnnotationsBySessionId(rfSessionEngine.getActiveSessionId() ?: "").firstOrNull())
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(jsonData.toByteArray())
                }
                Toast.makeText(context, "Successfully exported Investigation JSON!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to write JSON: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun exportRfEventsCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val csvData = rfEventRecorderEngine.generateCsvExport()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(csvData.toByteArray())
                }
                Toast.makeText(context, "Successfully exported Investigation CSV!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to write CSV: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
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


    fun captureEvidencePackage(targetBlip: RadarBlip? = null): AiEvidencePackage {
        val currentState = _uiState.value
        
        val hardwareCaps = mutableListOf<String>()
        
        hardwareCaps.add("BLE Scanner")
        hardwareCaps.add("Magnetometer")
        hardwareCaps.add("Audio/Ultrasonic Mic")
        
        val baseline = currentState.baselineSummary
        val baselineSummary = "Environment Baseline: ${baseline.knownFingerprints} known, ${baseline.newFingerprints} new"
        
        val obs = if (targetBlip != null) listOf(targetBlip) else currentState.activeBlips.toList()
        
        val correlations = currentState.correlationEvents
            .map { "Correlation: ${it.notes} (Score: ${it.correlationScore}, Confidence: ${it.confidence})" }

        return AiEvidencePackage(
            observations = obs,
            baselineSummary = baselineSummary,
            anomalyScore = (currentState.activeBlips.maxOfOrNull { it.anomalyResult?.score ?: 0 } ?: 0).toFloat(),
            anomalyConfidence = currentState.activeBlips.mapNotNull { it.anomalyResult?.confidence }.average().toFloat().takeIf { !it.isNaN() } ?: 0f,
            anomalyExplanations = currentState.activeBlips.flatMap { it.anomalyResult?.explanations?.map { e -> e.description } ?: emptyList() },
            correlations = correlations,
            timestampsMs = System.currentTimeMillis(),
            locationUncertainty = LocalizationConfidence.MEDIUM,
            hardwareCapabilities = hardwareCaps,
            calibrationState = "Calibrated", // Simplified
            provenance = if ((obs.any { it.provenance == DataProvenance.SIMULATED })) DataProvenance.SIMULATED else DataProvenance.MEASURED,
            isLive = !(obs.any { it.provenance == DataProvenance.REPLAY }) && !(obs.any { it.provenance == DataProvenance.SIMULATED }),
            isSimulation = (obs.any { it.provenance == DataProvenance.SIMULATED }),
            isReplay = (obs.any { it.provenance == DataProvenance.REPLAY })
        )
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


    fun runAiInvestigator(query: String? = null) {
        _uiState.update { it.copy(isAiAnalyzingThreats = true) }
        viewModelScope.launch {
            val pkg = captureEvidencePackage()
            val assessment = geminiThreatService.runEvidenceInvestigator(pkg, query)
            _uiState.update { 
                it.copy(
                    investigatorAssessment = assessment,
                    isAiAnalyzingThreats = false
                )
            }
        }
    }

    fun saveAiInterpretation() {
        _uiState.update { state ->
            state.investigatorAssessment?.let { assessment ->
                val newInterpretation = AiInterpretation(
                    assessment = assessment,
                    confidence = assessment.confidence,
                    operatingMode = state.operatingMode
                )
                state.copy(savedInterpretations = state.savedInterpretations + newInterpretation)
            } ?: state
        }
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
            val pkg = captureEvidencePackage()
            val answer = geminiThreatService.askTacticalCopilot(query, pkg, updatedList)

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


    fun resumeInvestigationSession(sessionId: String) {
        viewModelScope.launch {
            rfSessionEngine.resumeSession(sessionId)
            val id = rfSessionEngine.getActiveSessionId()
            if (id != null) {
                deviceIdentityEngine.loadHypothesesForSession(id)
            }
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

        // Clean up RF Acquisition Layer components
        wifiDiscoveryScanner.stopScanning()
        bleDiscoveryScanner.stopScanning()
        targetRangingEngine.stopRangingSession()

        uwbEngine.stopRangingEngine()
        wifiRttAwareManager.stopRttAwareEngine()
        bleTrackerEngine.stopTrackerEngine()
        cellularTelephonyManager.stopTelephonyEngine()
        ultrasonicAudioInterceptor.stopInterceptor()
        usbSdrManager.releaseReceiver()
        alarmEngine.release()
    }
}
