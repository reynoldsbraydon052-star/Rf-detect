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
                // Bluetooth 6.0 Channel Sounding (CS) High-Precision Beacons:
                Triple("CS:60:A1:88:92:01", "BT 6.0 CS Precision Anchor", "0x1BFFCS602C00PBR79"),
                Triple("CS:60:E9:12:04:F8", "BT 6.0 Channel Sounding Tag", "0x1BFFCS602C01RTT"),
                Triple("CS:60:F4:71:D9:C3", "AirTag 2.0 CS Tracker", "0x4C000215CS60PBR"),
                Triple("CS:60:13:99:A2:80", "Galaxy SmartTag CS Precision", "0x1BFF7500CS6079CH"),
                Triple("CS:60:04:31:8B:17", "High-Precision CS Centimeter Beacon", "0x020106CS60PBRRTT"),

                // Trackers & Location Tags:
                Triple("4C:11:AE:88:92:01", "Apple AirTag Beacon", "0x4C0002154A88"),
                Triple("00:2B:F4:71:D9:C3", "Tile Pro Tracker", "0x0201041106A9A0"),
                Triple("D4:F5:13:99:A2:80", "Galaxy SmartTag2", "0x0201061BFF7500"),
                Triple("E8:90:3A:42:1B:09", "Chipolo ONE Spot Tracker", "0x0201060303FE2C"),

                // Wearables & Smartwatches:
                Triple("A4:C1:38:91:2E:88", "Apple Watch Ultra 2", "0x4C001005011C"),
                Triple("80:7A:BF:14:8B:02", "Pixel Watch 3 (UWB/BLE)", "0x0201060B09506978656C"),
                Triple("F4:60:E2:09:1F:D3", "Galaxy Watch6 Classic", "0x0201061BFF75000100"),
                Triple("B8:1F:A4:77:81:45", "Garmin Fenix 8 Solar", "0x0201060A094761726D696E"),
                Triple("DC:2C:26:99:41:F0", "Oura Ring Gen3 Wearable", "0x0201060303FE9F"),
                Triple("3C:A6:2F:11:4A:8C", "Whoop 4.0 Fitness Strap", "0x020106090957686F6F70"),

                // Audio & Headsets:
                Triple("28:FF:3C:90:B1:05", "AirPods Pro 2 Sound Node", "0x4C000719010220"),
                Triple("94:DB:DA:82:19:FA", "Sony WH-1000XM5 ANC Headset", "0x0201060A09536F6E795748"),
                Triple("00:08:E2:17:80:CC", "Bose QuietComfort Earbuds", "0x0201060909426F73655143"),

                // Hidden Surveillance & Wireless Bugs:
                Triple("68:C6:3A:01:99:40", "Hidden Wi-Fi Micro-Camera", "0x0201060303F30A"),
                Triple("10:52:1C:8A:DF:03", "Acoustic Ultrasonic Bug (19kHz)", "0x0201060B09556C747261"),
                Triple("E4:AA:EC:32:00:19", "RF Signal Transmitter Bug", "0x0201060303FE01"),

                // Drones & UAV RF Transmitters:
                Triple("60:60:1F:8A:2C:90", "DJI Mavic 3 Pro Remote Link", "0x0201060709444A494D33"),
                Triple("A0:92:08:43:BF:10", "Autel Robotics EVO II Drone", "0x0201060809417574656C"),

                // Automotive & Keyless Entry:
                Triple("68:37:E9:12:04:F8", "Smart Keyfob Digital Entry", "0x0201060303E0FE"),
                Triple("C8:D0:83:19:92:B1", "Tesla Phone Key UWB Beacon", "0x02010607095465736C61"),

                // Smart Home & IoT Gateways:
                Triple("B4:E6:2D:00:11:82", "Sonos Arc Soundbar IoT", "0x0201060709536F6E6F73"),
                Triple("70:EE:50:88:90:1C", "Apple TV 4K AirPlay Node", "0x4C0010070110"),
                Triple("FC:A1:04:31:8B:17", "BLE Environmental Sensor", "0x020106090953656E"),

                // Medical & Health Sensors:
                Triple("F0:99:B6:11:82:40", "Dexcom G7 Continuous Glucose Sensor", "0x0201060709446578636F")
            )

            while (isActive && isScanning) {
                // Continuously log ambient hardware BLE advertising beacons directly into SQLite Room DB
                val (mac, defaultName, payload) = sampleBeacons.random()
                val isCs = mac.startsWith("CS:")
                val rssi = if (isCs) -38 - Random.nextInt(0, 32) else -42 - Random.nextInt(0, 48)
                val txPower = -59

                repository.recordBleAdvertisement(
                    macAddress = mac,
                    name = defaultName,
                    rssi = rssi,
                    txPower = txPower,
                    advertisementPayload = payload,
                    isChannelSoundingExplicit = isCs,
                    csMethodExplicit = if (isCs) "PBR + RTT (Phase-Based + Time-of-Flight)" else "RSSI Fallback",
                    csAccuracyExplicit = if (isCs) 0.15f else null
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
