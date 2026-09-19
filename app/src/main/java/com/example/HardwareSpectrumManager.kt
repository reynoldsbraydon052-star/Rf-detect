package com.example

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.net.wifi.WifiManager
import android.telephony.CellInfoLte
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
    val status: String,
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

    @SuppressLint("HardwareIds")
    suspend fun performAllAntennaSweep(hasLocationPermission: Boolean = true) = withContext(Dispatchers.IO) {
        var realSignalsFound = 0

        if (hasLocationPermission) {
            realSignalsFound += sweepWifiSpectrum()
            realSignalsFound += sweepCellularTowers()
            realSignalsFound += sweepBluetoothSpectrum()
        }

        // Only emit real-measured RF targets. No synthetic targets are added when hardware is unavailable.
        // A real scan result set is allowed to be empty; the UI must show no fake emitters.
    }

    @SuppressLint("MissingPermission")
    private fun sweepWifiSpectrum(): Int {
        var count = 0
        try {
            val results = wifiManager?.scanResults ?: emptyList()
            for (res in results) {
                if (res.BSSID.isNullOrBlank()) continue
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
        } catch (_: Exception) {
            // Permission or missing service, no synthetic fallback permitted.
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
        } catch (_: Exception) {
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
                    val device = result.device
                    val address = device.address ?: return
                    if (address.isBlank()) return
                    count++
                    val name = device.name ?: "BLE Sounder (${address.takeLast(5)})"
                    val rssi = result.rssi
                    val dist = FrequencyConverter.calculateDistance(rssi, 2402.0)
                    val rawHash = kotlin.math.abs(address.hashCode())
                    val angle = (rawHash * 137.5f) % 360f

                    _rfSignals.tryEmit(
                        RadarBlip(
                            id = address,
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
        } catch (_: Exception) {
            // Handled
        }
        return count
    }
}
