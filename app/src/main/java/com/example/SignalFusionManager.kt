package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FusedSignalDevice(
    val macAddress: String,
    val name: String,
    val rssiDbm: Int,
    val distanceMeters: Double?,
    val isActivelyRanging: Boolean,
    val rangingMethod: String?, // "BLE_CS", "WIFI_NAN_RTT", or null
    val type: String, // "WIFI" or "BLE"
    val timestampMs: Long
)

data class FusedSignalState(
    val discoveredDevices: List<FusedSignalDevice> = emptyList(),
    val activeTrackedMac: String? = null,
    val activeTrackedDistanceMeters: Double? = null,
    val activeRangingMethod: String? = null
)

class SignalFusionManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val wifiScanner: WifiDiscoveryScanner,
    private val bleScanner: BleDiscoveryScanner,
    private val rangingEngine: TargetRangingEngine
) {
    private val TAG = "SignalFusionManager"

    private val _fusedState = MutableStateFlow(FusedSignalState())
    val fusedState: StateFlow<FusedSignalState> = _fusedState.asStateFlow()

    // Store latest scans to compile merged results
    private var latestWifi = emptyList<WifiDiscoveryDevice>()
    private var latestBle = emptyList<BleDiscoveryDevice>()

    // Current high-frequency ranging data for active target
    private var activeTargetMac: String? = null
    private var activeDistanceMeters: Double? = null
    private var activeMethod: String? = null

    init {
        startDataCollection()
    }

    private fun startDataCollection() {
        // 1. Subscribe to Wi-Fi scan flow
        scope.launch(Dispatchers.Default) {
            wifiScanner.wifiDevices.collect { devices ->
                latestWifi = devices
                rebuildFusedState()
            }
        }

        // 2. Subscribe to BLE scan flow
        scope.launch(Dispatchers.Default) {
            bleScanner.bleDevices.collect { devices ->
                latestBle = devices
                rebuildFusedState()
            }
        }

        // 3. Subscribe to high-frequency ranging engine updates
        scope.launch(Dispatchers.Default) {
            rangingEngine.rangingEvents.collectLatest { update ->
                if (update.macAddress == activeTargetMac) {
                    activeDistanceMeters = update.distanceMeters
                    activeMethod = update.method
                    rebuildFusedState()
                }
            }
        }
    }

    /**
     * Start active, high-frequency ranging for a specific target device
     */
    fun startActiveTracking(macAddress: String) {
        Log.i(TAG, "Initiating high-frequency passive-bypass active tracking for: $macAddress")
        activeTargetMac = macAddress
        activeDistanceMeters = null
        activeMethod = null
        
        rangingEngine.startRangingSession(macAddress)
        rebuildFusedState()
    }

    /**
     * Stop active tracking and return to standard low-frequency scanning
     */
    fun stopActiveTracking() {
        if (activeTargetMac != null) {
            Log.i(TAG, "Stopping active tracking session for: $activeTargetMac")
            rangingEngine.stopRangingSession()
            activeTargetMac = null
            activeDistanceMeters = null
            activeMethod = null
            rebuildFusedState()
        }
    }

    /**
     * Merge discovery flows and active ranging stats into a unified snapshot
     */
    @Synchronized
    private fun rebuildFusedState() {
        val fusedDevices = mutableListOf<FusedSignalDevice>()

        // Map Wi-Fi Devices
        latestWifi.forEach { device ->
            val isTarget = device.bssid == activeTargetMac
            fusedDevices.add(
                FusedSignalDevice(
                    macAddress = device.bssid,
                    name = device.ssid,
                    rssiDbm = device.rssiDbm,
                    distanceMeters = if (isTarget) activeDistanceMeters else null,
                    isActivelyRanging = isTarget,
                    rangingMethod = if (isTarget) activeMethod else null,
                    type = "WIFI",
                    timestampMs = device.timestampMs
                )
            )
        }

        // Map BLE Devices
        latestBle.forEach { device ->
            val isTarget = device.macAddress == activeTargetMac
            fusedDevices.add(
                FusedSignalDevice(
                    macAddress = device.macAddress,
                    name = device.name,
                    rssiDbm = device.rssiDbm,
                    distanceMeters = if (isTarget) activeDistanceMeters else null,
                    isActivelyRanging = isTarget,
                    rangingMethod = if (isTarget) activeMethod else null,
                    type = "BLE",
                    timestampMs = device.timestampMs
                )
            )
        }

        _fusedState.update {
            FusedSignalState(
                discoveredDevices = fusedDevices.sortedByDescending { it.rssiDbm },
                activeTrackedMac = activeTargetMac,
                activeTrackedDistanceMeters = activeDistanceMeters,
                activeRangingMethod = activeMethod
            )
        }
    }
}
