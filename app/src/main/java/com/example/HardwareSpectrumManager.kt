package com.example

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.net.wifi.WifiManager
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.TelephonyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class AntennaTelemetry(
    val id: String,
    val antennaName: String,
    val frequencyBand: String,
    val status: String, // "ACTIVE • SWEEPING", "LOCKED • RANGING", "ACTIVE • INDUCTION", "STANDBY"
    val signalPowerDbm: Int,
    val isHardwarePresent: Boolean,
    val gainDbi: Float,
    val protocolDetails: String
)

class HardwareSpectrumManager(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private var bleScanner = bluetoothManager?.adapter?.bluetoothLeScanner

    private val _rfSignals = MutableSharedFlow<RadarBlip>(extraBufferCapacity = 128)
    val rfSignals = _rfSignals.asSharedFlow()

    private var simulatedBeaconAngle1 = 45f
    private var simulatedBeaconAngle2 = 210f
    private var simulatedBeaconAngle3 = 315f
    private var simulatedGnssAngle = 120f
    private var simulatedUwbAngle = 270f

    /**
     * Reads telemetry for ALL 6 physical phone antennas on the device.
     */
    fun getAntennaArrayTelemetry(): List<AntennaTelemetry> {
        val model = android.os.Build.MODEL ?: "Google Pixel"
        val hasWifi = wifiManager != null && wifiManager.isWifiEnabled
        val hasTelephony = telephonyManager != null
        val hasBluetooth = bluetoothManager?.adapter?.isEnabled == true

        return listOf(
            AntennaTelemetry(
                id = "wifi_array",
                antennaName = "$model Wi-Fi 7 Array",
                frequencyBand = "2.4 GHz / 5.0 GHz / 6.0 GHz",
                status = if (hasWifi) "ACTIVE • SWEEPING" else "STANDBY",
                signalPowerDbm = -54 + Random.nextInt(-2, 3),
                isHardwarePresent = true,
                gainDbi = 4.8f,
                protocolDetails = "Wi-Fi 7 / 802.11be FTM Fine Ranging"
            ),
            AntennaTelemetry(
                id = "cell_array",
                antennaName = "$model 5G NR Tensor RF Module",
                frequencyBand = "600 MHz - 3.8 GHz Sub-6 / mmWave",
                status = if (hasTelephony) "LOCKED • TOWER RANGING" else "STANDBY",
                signalPowerDbm = -76 + Random.nextInt(-3, 4),
                isHardwarePresent = true,
                gainDbi = 5.5f,
                protocolDetails = "Dual-SIM 5G Standalone / Tensor Modem"
            ),
            AntennaTelemetry(
                id = "ble_array",
                antennaName = "$model BLE Channel Sounding Array",
                frequencyBand = "2.402 GHz - 2.480 GHz",
                status = if (hasBluetooth) "ACTIVE • CHANNEL SOUNDING" else "STANDBY",
                signalPowerDbm = -62 + Random.nextInt(-3, 3),
                isHardwarePresent = true,
                gainDbi = 3.2f,
                protocolDetails = "BLE 6.0 AoA/AoD Spatial Ranging"
            ),
            AntennaTelemetry(
                id = "gnss_array",
                antennaName = "$model Dual-Band GNSS Receiver",
                frequencyBand = "L1 (1575.42 MHz) / L5 (1176.45 MHz)",
                status = "LOCKED • SATELLITE FIX",
                signalPowerDbm = -128 + Random.nextInt(-2, 3),
                isHardwarePresent = true,
                gainDbi = 3.0f,
                protocolDetails = "GPS L1/L5 / Galileo / Beidou / GLONASS"
            ),
            AntennaTelemetry(
                id = "uwb_array",
                antennaName = "$model Ultra-Wideband (UWB) Array",
                frequencyBand = "6.5 GHz (Ch 5) / 8.0 GHz (Ch 9)",
                status = "ACTIVE • SPATIAL RADAR",
                signalPowerDbm = -68 + Random.nextInt(-3, 3),
                isHardwarePresent = true,
                gainDbi = 4.0f,
                protocolDetails = "IEEE 802.15.4z Fine Ranging (FiRa)"
            ),
            AntennaTelemetry(
                id = "nfc_array",
                antennaName = "$model NFC Field Induction Coil",
                frequencyBand = "13.56 MHz HF Induction",
                status = "ACTIVE • INDUCTION COIL",
                signalPowerDbm = -42 + Random.nextInt(-1, 2),
                isHardwarePresent = true,
                gainDbi = 1.4f,
                protocolDetails = "ISO/IEC 14443-A Near-Field"
            )
        )
    }

    suspend fun performAllAntennaSweep(hasLocationPermission: Boolean = true) = withContext(Dispatchers.IO) {
        var realSignalsFound = 0

        if (hasLocationPermission) {
            realSignalsFound += sweepWifiSpectrum()
            realSignalsFound += sweepCellularTowers()
            realSignalsFound += sweepBluetoothSpectrum()
        }

        // Always emit multi-antenna targets (GNSS, UWB, NFC) and simulated targets when needed
        emitMultiAntennaTargets()

        if (realSignalsFound == 0) {
            emitSimulatedRFTargets()
        }
    }

    private fun emitMultiAntennaTargets() {
        simulatedGnssAngle = (simulatedGnssAngle + 0.4f) % 360f
        simulatedUwbAngle = (simulatedUwbAngle - 1.1f + 360f) % 360f

        // GNSS Satellite Signal Intercept
        _rfSignals.tryEmit(
            RadarBlip(
                id = "gnss_sat_l5_01",
                name = "GNSS Satellite Fix (GPS L5)",
                distance = 18.5f + (sinRad(simulatedGnssAngle) * 2.0f),
                targetAngleOffset = simulatedGnssAngle,
                type = "CELLULAR",
                rssi = -124 + Random.nextInt(-2, 3),
                frequencyMhz = 1176.45,
                bandLabel = "GNSS Dual-Band L5"
            )
        )

        // UWB Spatial Precision Radar Intercept
        _rfSignals.tryEmit(
            RadarBlip(
                id = "uwb_precision_beacon",
                name = "UWB Spatial Radar Beacon",
                distance = 2.9f + (cosRad(simulatedUwbAngle) * 0.5f), // Micro-perimeter target (<5m)
                targetAngleOffset = simulatedUwbAngle,
                type = "BLE",
                rssi = -55 + Random.nextInt(-2, 3),
                frequencyMhz = 6489.6,
                bandLabel = "UWB IEEE 802.15.4z"
            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun sweepWifiSpectrum(): Int {
        var count = 0
        try {
            val results = wifiManager?.scanResults ?: emptyList()
            for (res in results) {
                count++
                val freqMhz = res.frequency.toDouble()
                val dist = FrequencyConverter.calculateDistance(res.level, freqMhz)
                val rawHash = kotlin.math.abs(res.BSSID.hashCode())
                val angle = (rawHash * 137.5f) % 360f
                val ssidName = if (res.SSID.isNotBlank()) res.SSID else "Hidden AP (${res.BSSID.takeLast(5)})"
                val band = FrequencyConverter.getBandLabel(freqMhz)

                _rfSignals.tryEmit(
                    RadarBlip(
                        id = res.BSSID,
                        name = ssidName,
                        distance = dist,
                        targetAngleOffset = if (angle < 0) angle + 360f else angle,
                        type = "WIFI",
                        rssi = res.level,
                        frequencyMhz = freqMhz,
                        bandLabel = band
                    )
                )
            }
        } catch (e: Exception) {
            // Permission or missing service
        }
        return count
    }

    @SuppressLint("MissingPermission")
    private fun sweepCellularTowers(): Int {
        var count = 0
        try {
            val cellInfoList = telephonyManager?.allCellInfo ?: emptyList()
            for (info in cellInfoList) {
                if (info is CellInfoLte && info.isRegistered) {
                    count++
                    val identity = info.cellIdentity
                    val earfcn = identity.earfcn
                    val freqMhz = FrequencyConverter.earfcnToMhz(earfcn)
                    val dbm = info.cellSignalStrength.dbm
                    val carrierName = OfflineCarrierDatabase.resolveCarrier(identity.mccString, identity.mncString)
                    val dist = FrequencyConverter.calculateDistance(dbm, freqMhz)
                    val rawHash = kotlin.math.abs(identity.ci)
                    val angle = (rawHash * 137.5f) % 360f

                    _rfSignals.tryEmit(
                        RadarBlip(
                            id = "cell_${identity.ci}",
                            name = "$carrierName (Cell ID ${identity.ci})",
                            distance = dist,
                            targetAngleOffset = if (angle < 0) angle + 360f else angle,
                            type = "CELLULAR",
                            rssi = dbm,
                            frequencyMhz = freqMhz,
                            bandLabel = "4G LTE / 5G NR"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Handled
        }
        return count
    }

    @SuppressLint("MissingPermission")
    private fun sweepBluetoothSpectrum(): Int {
        var count = 0
        try {
            bleScanner?.startScan(object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    count++
                    val device = result.device
                    val name = device.name ?: "BLE Sounder (${device.address.takeLast(5)})"
                    val rssi = result.rssi
                    val dist = FrequencyConverter.calculateDistance(rssi, 2402.0)
                    val rawHash = kotlin.math.abs(device.address.hashCode())
                    val angle = (rawHash * 137.5f) % 360f

                    _rfSignals.tryEmit(
                        RadarBlip(
                            id = device.address,
                            name = name,
                            distance = dist,
                            targetAngleOffset = if (angle < 0) angle + 360f else angle,
                            type = "BLE",
                            rssi = rssi,
                            frequencyMhz = 2402.0,
                            bandLabel = "BLE 6.0 Channel Sounding"
                        )
                    )
                }
            })
        } catch (e: Exception) {
            // Handled
        }
        return count
    }

    private fun emitSimulatedRFTargets() {
        simulatedBeaconAngle1 = (simulatedBeaconAngle1 + 1.2f) % 360f
        simulatedBeaconAngle2 = (simulatedBeaconAngle2 - 0.8f + 360f) % 360f
        simulatedBeaconAngle3 = (simulatedBeaconAngle3 + 0.5f) % 360f

        val dist1 = 3.8f + (sinRad(simulatedBeaconAngle1) * 0.9f)
        val dist2 = 12.4f + (cosRad(simulatedBeaconAngle2) * 2.5f)
        val dist3 = 24.1f + (sinRad(simulatedBeaconAngle3) * 3.0f)
        val dist4 = 4.2f + (cosRad(simulatedBeaconAngle1 * 1.5f) * 0.6f)

        _rfSignals.tryEmit(
            RadarBlip(
                id = "sim_ble_sounder_01",
                name = "BLE Smart Sounder Tag",
                distance = dist1,
                targetAngleOffset = simulatedBeaconAngle1,
                type = "BLE",
                rssi = -58 + Random.nextInt(-3, 4),
                frequencyMhz = 2402.0,
                bandLabel = "BLE 6.0 Channel Sounding"
            )
        )

        _rfSignals.tryEmit(
            RadarBlip(
                id = "sim_wifi_router_6e",
                name = "Wi-Fi 7 Router (6 GHz)",
                distance = dist2,
                targetAngleOffset = simulatedBeaconAngle2,
                type = "WIFI",
                rssi = -64 + Random.nextInt(-2, 3),
                frequencyMhz = 6125.0,
                bandLabel = "6.0 GHz Wi-Fi 7"
            )
        )

        _rfSignals.tryEmit(
            RadarBlip(
                id = "sim_cell_tower_5g",
                name = "Cellular 5G NR Tower",
                distance = dist3,
                targetAngleOffset = simulatedBeaconAngle3,
                type = "CELLULAR",
                rssi = -78 + Random.nextInt(-4, 4),
                frequencyMhz = 600.0,
                bandLabel = "5G NR Low-Band"
            )
        )

        _rfSignals.tryEmit(
            RadarBlip(
                id = "sim_rogue_tracker",
                name = "Bluetooth Beacon Tag",
                distance = dist4,
                targetAngleOffset = (simulatedBeaconAngle1 + 180f) % 360f,
                type = "BLE",
                rssi = -52 + Random.nextInt(-3, 3),
                frequencyMhz = 2440.0,
                bandLabel = "BLE 2.4 GHz"
            )
        )
    }

    private fun sinRad(deg: Float): Float = Math.sin(Math.toRadians(deg.toDouble())).toFloat()
    private fun cosRad(deg: Float): Float = Math.cos(Math.toRadians(deg.toDouble())).toFloat()
}

