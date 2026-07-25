package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RadarTab {
    SWEEP_RADAR,
    SPECTRUM_ANALYZER,
    MAGNETOMETER_EMF,
    SECURITY_GUARD,
    CSV_LOG_CONSOLE
}

data class SignalRadarUiState(
    val selectedTab: RadarTab = RadarTab.SWEEP_RADAR,
    val headingDegrees: Float = 0f,
    val activeBlips: List<RadarBlip> = emptyList(),
    val nearestBlip: RadarBlip? = null,
    val perimeterThresholdMeters: Float = 5.0f,
    val perimeterBreachCount: Int = 0,
    val isAudioSonarActive: Boolean = false,
    val isPerimeterAlarmEnabled: Boolean = true,
    val isScanningActive: Boolean = true,
    val logConsoleTail: String = "",
    val activeAntennaCount: Int = 6, // Wi-Fi, BLE, Cell, GNSS, UWB, NFC
    val selectedFilterType: String = "ALL", // "ALL", "WIFI", "CELLULAR", "BLE", "MAGNETIC", "AUDIO"
    val magnetometerData: MagnetometerData = MagnetometerData(),
    val acousticData: AcousticFrequencyData = AcousticFrequencyData(),
    val calibrationNotificationMessage: String? = null,
    val antennaArrayTelemetry: List<AntennaTelemetry> = emptyList(),
    val savedBleDevices: List<BleDeviceEntity> = emptyList(),
    val isBleScannerServiceActive: Boolean = true
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

        // Start Bluetooth LE scanner service & observe SQLite Room Database
        bleScannerService.startScannerService()
        viewModelScope.launch {
            bleRepository.allBleDevices.collect { bleDevices ->
                _uiState.update { it.copy(savedBleDevices = bleDevices) }

                // Map database BLE devices to sweep radar
                bleDevices.forEach { dev ->
                    val angle = (dev.macAddress.hashCode().run { Math.abs(this) % 360 }).toFloat()
                    val blip = RadarBlip(
                        id = "ble_db_" + dev.macAddress,
                        name = dev.deviceName,
                        distance = dev.distanceMeters,
                        targetAngleOffset = angle,
                        type = "BLE",
                        rssi = dev.rssi,
                        frequencyMhz = 2402.0,
                        bandLabel = "BLE 5.4 (${dev.proximityCategory})"
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
                delay(1200)
            }
        }

        // Periodically refresh log console tail
        viewModelScope.launch {
            while (isActive) {
                val tail = historyLogger.readLogTail(15)
                _uiState.update { it.copy(logConsoleTail = tail) }
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

        if (isBreach && _uiState.value.isPerimeterAlarmEnabled) {
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

        nearest?.let {
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

    fun setTab(tab: RadarTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setFilterType(type: String) {
        _uiState.update { it.copy(selectedFilterType = type) }
    }

    fun setPerimeterThreshold(meters: Float) {
        _uiState.update { it.copy(perimeterThresholdMeters = meters) }
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

    override fun onCleared() {
        super.onCleared()
        bleScannerService.stopScannerService()
        orientationManager?.stopListening()
        magnetometerDetector?.stopListening()
        acousticDetector?.stopListening()
        audioTracker.stop()
    }
}
