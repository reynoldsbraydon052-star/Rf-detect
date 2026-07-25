package com.example

import kotlinx.coroutines.flow.Flow
import kotlin.math.pow

class BleDeviceRepository(private val bleDeviceDao: BleDeviceDao) {

    val allBleDevices: Flow<List<BleDeviceEntity>> = bleDeviceDao.getAllBleDevices()

    fun getBleDevicesInMicroPerimeter(maxDistanceMeters: Float = 5.0f): Flow<List<BleDeviceEntity>> {
        return bleDeviceDao.getBleDevicesWithinDistance(maxDistanceMeters)
    }

    suspend fun recordBleAdvertisement(
        macAddress: String,
        name: String?,
        rssi: Int,
        txPower: Int?,
        advertisementPayload: String
    ) {
        val currentTime = System.currentTimeMillis()
        val deviceName = if (!name.isNullOrBlank() && name != "null") name else "BLE Beacon (${macAddress.takeLast(5)})"

        // Path-Loss Model Distance Estimate: Distance = 10 ^ ((MeasuredTxPower - RSSI) / (10 * n))
        val measuredPower = txPower ?: -59
        val n = 2.4 // Propagation factor
        val rawDistance = 10.0.pow((measuredPower - rssi) / (10.0 * n)).toFloat().coerceIn(0.2f, 80.0f)

        val proximityCat = when {
            rawDistance <= 5.0f -> "MICRO_PERIMETER"
            rawDistance <= 15.0f -> "IMMEDIATE"
            else -> "FAR"
        }

        // Map RSSI (-100 to -30 dBm) to 0-100% signal strength
        val signalPercent = ((rssi + 100) * 100 / 70).coerceIn(0, 100)

        val existing = bleDeviceDao.getDeviceByMac(macAddress)
        val firstSeen = existing?.firstSeenTimestamp ?: currentTime
        val hitCount = (existing?.hitCount ?: 0) + 1

        val updatedEntity = BleDeviceEntity(
            macAddress = macAddress,
            deviceName = deviceName,
            rssi = rssi,
            txPower = txPower,
            distanceMeters = rawDistance,
            proximityCategory = proximityCat,
            signalStrengthPercent = signalPercent,
            advertisementPayload = advertisementPayload,
            firstSeenTimestamp = firstSeen,
            lastSeenTimestamp = currentTime,
            hitCount = hitCount
        )

        bleDeviceDao.upsertBleDevice(updatedEntity)
    }

    suspend fun clearDatabase() {
        bleDeviceDao.clearAllBleDevices()
    }

    suspend fun deleteDevice(macAddress: String) {
        bleDeviceDao.deleteBleDevice(macAddress)
    }
}
