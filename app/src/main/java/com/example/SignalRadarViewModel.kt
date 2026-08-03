package com.example

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

enum class RadarTab {
    SWEEP_RADAR,
    FULL_RADAR,
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
    // Full Screen Map Option & Phone Hardware Sensor Suite State:
    val isFullScreenMapVisible: Boolean = false,
    val fullScreenMapMode: String = "TACTICAL", // "TACTICAL", "HEATMAP", "SAT_GRID"
    val sensorSuite: HardwareSensorSuiteData = HardwareSensorSuiteData(),
    // Background Alert Service & Threshold Notification Settings State:
    val isBackgroundAlertServiceActive: Boolean = true,
    val isHapticAlertsEnabled: Boolean = true,
    val isVisualNotifsEnabled: Boolean = true,
    val activeDeviceAlerts: List<DeviceAlertEvent> = emptyList()
)

class SignalRadarViewModel(application: Application) : AndroidViewModel(application) {

    private val bleDatabase = BleDatabase.getInstance(application)
    private val bleRepository = BleDeviceRepository(bleDatabase.bleDeviceDao())
    private val bleScannerService = BleScannerServiceEngine(application, bleRepository)

    private val hardwareSpectrumManager = HardwareSpectrumManager(application)
    private val audioTracker = AudioRadarTracker()
    private val historyLogger = SignalHistoryLogger(application)
    private val alarmEngine = PerimeterAlarmEngine(application)

    private var orientationManager: OrientationManager? = null
    private var magnetometerDetector: MagnetometerDetector? = null
    private var acousticDetector: AcousticFrequencyDetector? = null
    private var hardwareSensorSuiteManager: HardwareSensorSuiteManager? = null

    private val kalmanFilters = mutableMapOf<String, KalmanFilter>()
    private val blipMap = mutableMapOf<String, RadarBlip>()

    private val _uiState = MutableStateFlow(SignalRadarUiState())
    val uiState: StateFlow<SignalRadarUiState> = _uiState.asStateFlow()

    init {
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
                    val angle = (dev.macAddress.hashCode().run { Math.abs(this) % 360 }).toFloat()
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

    private fun processSignalIntercept(rawBlip: RadarBlip) {
        val distanceFilter = kalmanFilters.getOrPut("${rawBlip.id}_dist") { KalmanFilter(processNoise = 0.008f, measurementNoise = 0.4f) }
        val rssiFilter = kalmanFilters.getOrPut("${rawBlip.id}_rssi") { KalmanFilter(processNoise = 0.05f, measurementNoise = 1.0f) }

        val smoothedDistance = distanceFilter.update(rawBlip.distance)
        val smoothedRssi = rssiFilter.update(rawBlip.rssi.toFloat()).toInt()

        val smoothedBlip = rawBlip.copy(
            distance = smoothedDistance,
            rssi = smoothedRssi
        )

        blipMap[smoothedBlip.id] = smoothedBlip

        val threshold = _uiState.value.perimeterThresholdMeters
        val isBreach = smoothedDistance < threshold

        val rssiThreshold = _uiState.value.rssiAlertThresholdDbm
        val isRssiAlert = smoothedRssi >= rssiThreshold && _uiState.value.isRssiAlertEnabled

        if ((isBreach || isRssiAlert) && _uiState.value.isPerimeterAlarmEnabled && !_uiState.value.stealthModeEnabled) {
            alarmEngine.triggerProximityAlert()
        }

        historyLogger.logSignalEntry(
            deviceName = smoothedBlip.name,
            distanceMeters = smoothedDistance,
            type = smoothedBlip.type,
            freqMhz = smoothedBlip.frequencyMhz,
            isBreach = isBreach
        )

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

    fun recalibrateMagnetometer() {
        magnetometerDetector?.recalibrate()
        _uiState.update { it.copy(calibrationNotificationMessage = null) }
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

    override fun onCleared() {
        super.onCleared()
        bleScannerService.stopScannerService()
        orientationManager?.stopListening()
        magnetometerDetector?.stopListening()
        acousticDetector?.stopListening()
        hardwareSensorSuiteManager?.stopListening()
        audioTracker.stop()
    }
}
