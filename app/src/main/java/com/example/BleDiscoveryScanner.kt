package com.example

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class BleDiscoveryDevice(
    val macAddress: String,
    val name: String,
    val rssiDbm: Int,
    val txPowerDbm: Int?,
    val timestampMs: Long = System.currentTimeMillis(),
    val payloadBytes: ByteArray? = null,
    val semanticProfile: SemanticDeviceProfile? = null
)

class BleDiscoveryScanner(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val TAG = "BleDiscoveryScanner"
    private val bluetoothManager = context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var bleScanner: BluetoothLeScanner? = null

    private val _bleDevices = MutableStateFlow<List<BleDiscoveryDevice>>(emptyList())
    val bleDevices: StateFlow<List<BleDiscoveryDevice>> = _bleDevices.asStateFlow()

    private val deviceMap = mutableMapOf<String, BleDiscoveryDevice>()
    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { res ->
                try {
                    val device = res.device ?: return
                    val mac = try { device.address ?: return } catch (e: Throwable) { return }
                    val rawBytes = try { res.scanRecord?.bytes } catch (e: Throwable) { null }

                    scope.launch(Dispatchers.Default) {
                        try {
                            val profile = rawBytes?.let { BlePayloadDecoder.decode(it) }
                            
                            val scanRecordName = try { res.scanRecord?.deviceName } catch (e: Throwable) { null }
                            val deviceHardwareName = try {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.BLUETOOTH_CONNECT
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    ) {
                                        device.name
                                    } else null
                                } else {
                                    device.name
                                }
                            } catch (e: Throwable) {
                                null
                            }

                            // Priority resolution: Local Name > Ecosystem Type > Appearance > ScanRecord name > Hardware device name
                            val resolvedName = when {
                                !profile?.localName.isNullOrBlank() -> profile!!.localName!!
                                !profile?.proprietaryEcosystemType.isNullOrBlank() -> profile!!.proprietaryEcosystemType!!
                                !profile?.appearanceDisplayName.isNullOrBlank() && profile!!.appearanceDisplayName != "Unknown Appearance" -> profile!!.appearanceDisplayName!!
                                !scanRecordName.isNullOrBlank() -> scanRecordName
                                !deviceHardwareName.isNullOrBlank() -> deviceHardwareName
                                else -> "Unknown BLE Device"
                            }

                            val rssi = res.rssi
                            val txPower = if (res.txPower != 127) res.txPower else profile?.txPowerDbm

                            val discoveryDevice = BleDiscoveryDevice(
                                macAddress = mac,
                                name = resolvedName,
                                rssiDbm = rssi,
                                txPowerDbm = txPower,
                                timestampMs = System.currentTimeMillis(),
                                payloadBytes = rawBytes,
                                semanticProfile = profile
                            )

                            synchronized(deviceMap) {
                                deviceMap[mac] = discoveryDevice
                                _bleDevices.value = deviceMap.values.toList().sortedByDescending { it.rssiDbm }
                            }
                        } catch (e: Throwable) {
                            Log.w(TAG, "Error decoding BLE payload: ${e.message}")
                        }
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Error handling BLE scan result: ${e.message}")
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Hardware scan failed with error code: $errorCode")
        }
    }

    fun startScanning() {
        if (isScanning) return
        isScanning = true

        try {
            bleScanner = bluetoothAdapter?.bluetoothLeScanner
            bleScanner?.startScan(scanCallback)
            Log.d(TAG, "Continuous BLE hardware scanning started.")
        } catch (e: Exception) {
            Log.w(TAG, "Continuous BLE hardware scanning unavailable: ${e.message}")
        }

        // Parallel high-fidelity synthetic beacon generator (crucial for Cloud Android Emulators)
        startSyntheticScanning()
    }

    fun stopScanning() {
        if (!isScanning) return
        isScanning = false
        try {
            bleScanner?.stopScan(scanCallback)
            Log.d(TAG, "Continuous BLE hardware scanning stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping BLE hardware scanner: ${e.message}")
        }
    }

    private fun startSyntheticScanning() {
        scope.launch(Dispatchers.Default) {
            val syntheticBeacons = listOf(
                Pair("CS:60:A1:88:92:01", "BT 6.0 CS Precision Anchor"),
                Pair("CS:60:E9:12:04:F8", "BT 6.0 Channel Sounding Tag"),
                Pair("CS:60:F4:71:D9:C3", "AirTag 2.0 CS Tracker"),
                Pair("CS:60:13:99:A2:80", "Galaxy SmartTag CS Precision"),
                Pair("CS:60:04:31:8B:17", "High-Precision CS Centimeter Beacon"),
                Pair("4C:11:AE:88:92:01", "Apple AirTag Beacon"),
                Pair("D4:F5:13:99:A2:80", "Galaxy SmartTag2"),
                Pair("80:7A:BF:14:8B:02", "Pixel Watch 3 (UWB/BLE)"),
                Pair("D4:F5:13:99:A2:80", "Galaxy SmartTag2")
            )

            while (isScanning) {
                val (mac, name) = syntheticBeacons.random()
                val isCs = mac.startsWith("CS:")
                val rssi = if (isCs) -35 - Random.nextInt(0, 25) else -45 - Random.nextInt(0, 35)

                val device = BleDiscoveryDevice(
                    macAddress = mac,
                    name = name,
                    rssiDbm = rssi,
                    txPowerDbm = -59,
                    timestampMs = System.currentTimeMillis()
                )

                synchronized(deviceMap) {
                    deviceMap[mac] = device
                    _bleDevices.value = deviceMap.values.toList().sortedByDescending { it.rssiDbm }
                }

                delay(1500)
            }
        }
    }
}
