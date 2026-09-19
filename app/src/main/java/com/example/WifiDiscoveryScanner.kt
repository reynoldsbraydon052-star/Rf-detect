package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

data class WifiDiscoveryDevice(
    val bssid: String,
    val ssid: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val band: String,
    val timestampMs: Long = System.currentTimeMillis()
)

class WifiDiscoveryScanner(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val TAG = "WifiDiscoveryScanner"
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val _wifiDevices = MutableStateFlow<List<WifiDiscoveryDevice>>(emptyList())
    val wifiDevices: StateFlow<List<WifiDiscoveryDevice>> = _wifiDevices.asStateFlow()

    private val lastScanTimeMs = AtomicLong(0)
    private val MIN_SCAN_INTERVAL_MS = 30_000L

    @Volatile
    private var isScanning = false

    fun startScanning() {
        if (isScanning) return
        isScanning = true

        scope.launch(Dispatchers.Default) {
            while (isScanning) {
                performScan()
                delay(MIN_SCAN_INTERVAL_MS)
            }
        }
    }

    fun stopScanning() {
        isScanning = false
    }

    @SuppressLint("MissingPermission")
    private fun performScan() {
        val now = System.currentTimeMillis()
        val last = lastScanTimeMs.get()
        if (now - last < MIN_SCAN_INTERVAL_MS) {
            Log.d(TAG, "Wi-Fi scan throttled. Time since last scan: ${now - last}ms")
            return
        }

        lastScanTimeMs.set(now)
        Log.d(TAG, "Requesting WifiManager.startScan()...")

        try {
            if (wifiManager == null || !wifiManager.isWifiEnabled) {
                _wifiDevices.value = emptyList()
                Log.w(TAG, "Wi-Fi is unavailable or disabled; no measured networks reported.")
                return
            }

            if (!wifiManager.startScan()) {
                _wifiDevices.value = emptyList()
                Log.w(TAG, "Wi-Fi scan request was rejected; no synthetic networks will be reported.")
                return
            }

            val mapped = wifiManager.scanResults.mapNotNull { result ->
                val bssid = result.BSSID?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val band = when {
                    result.frequency in 2400..2500 -> "2.4 GHz"
                    result.frequency in 4900..5900 -> "5 GHz"
                    result.frequency in 5925..7125 -> "6 GHz"
                    else -> "RF Band"
                }
                WifiDiscoveryDevice(
                    bssid = bssid,
                    ssid = result.SSID?.ifEmpty { "<HIDDEN_AP>" } ?: "<HIDDEN_AP>",
                    rssiDbm = result.level,
                    frequencyMhz = result.frequency,
                    band = band,
                    timestampMs = System.currentTimeMillis()
                )
            }

            _wifiDevices.value = mapped
            Log.d(TAG, "Processed ${mapped.size} measured Wi-Fi networks.")
        } catch (e: Exception) {
            _wifiDevices.value = emptyList()
            Log.e(TAG, "Wi-Fi scan failed; no synthetic results reported: ${e.message}")
        }
    }
}
