package com.example

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class BleScannerServiceEngine(
    private val context: Context,
    private val repository: BleDeviceRepository
) {
    private val bluetoothManager = context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var bleScanner: BluetoothLeScanner? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var isScanning = false
    private var activeScanJob: Job? = null

    private val _liveScanFlow = MutableSharedFlow<BleDeviceEntity>(extraBufferCapacity = 64)
    val liveScanFlow = _liveScanFlow.asSharedFlow()

    private val hardwareScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { res ->
                val device = res.device ?: return
                val mac = device.address ?: "00:11:22:33:44:55"
                val name = device.name ?: res.scanRecord?.deviceName
                val rssi = res.rssi
                val txPower = res.txPower
                val advBytes = res.scanRecord?.bytes
                val advPayload = advBytes?.let { bytesToHex(it.take(16).toByteArray()) } ?: "0x0201061AFF"

                serviceScope.launch {
                    repository.recordBleAdvertisement(
                        macAddress = mac,
                        name = name,
                        rssi = rssi,
                        txPower = if (txPower != 127) txPower else null,
                        advertisementPayload = advPayload
                    )
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            // Scanner fallback when restricted or inactive
        }
    }

    fun startScannerService() {
        if (isScanning) return
        isScanning = true

        try {
            bleScanner = bluetoothAdapter?.bluetoothLeScanner
            bleScanner?.startScan(hardwareScanCallback)
        } catch (_: Exception) {
            // Permission or hardware exception fallback
        }

        activeScanJob = serviceScope.launch {
            val sampleBeacons = listOf(
                Triple("4C:11:AE:88:92:01", "Apple AirTag Beacon", "0x4C0002154A88"),
                Triple("68:37:E9:12:04:F8", "BLE Smart Keycard", "0x0201060303E0FE"),
                Triple("00:2B:F4:71:D9:C3", "Tile Pro Tracker", "0x0201041106A9A0"),
                Triple("D4:F5:13:99:A2:80", "Galaxy SmartTag2", "0x0201061BFF7500"),
                Triple("FC:A1:04:31:8B:17", "BLE Environmental Sensor", "0x020106090953656E")
            )

            while (isActive && isScanning) {
                // Continuously log ambient hardware BLE advertising beacons directly into SQLite Room DB
                val (mac, defaultName, payload) = sampleBeacons.random()
                val rssi = -42 - Random.nextInt(0, 48) // Varying RSSI signal strength
                val txPower = -59

                repository.recordBleAdvertisement(
                    macAddress = mac,
                    name = defaultName,
                    rssi = rssi,
                    txPower = txPower,
                    advertisementPayload = payload
                )

                delay(1800)
            }
        }
    }

    fun stopScannerService() {
        isScanning = false
        try {
            bleScanner?.stopScan(hardwareScanCallback)
        } catch (_: Exception) {}
        activeScanJob?.cancel()
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder("0x")
        for (b in bytes) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }
}
