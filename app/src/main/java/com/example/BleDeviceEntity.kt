package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ble_devices")
data class BleDeviceEntity(
    @PrimaryKey val macAddress: String,
    val deviceName: String,
    val rssi: Int,
    val txPower: Int?,
    val distanceMeters: Float,
    val proximityCategory: String, // "MICRO_PERIMETER", "IMMEDIATE", "FAR"
    val signalStrengthPercent: Int,
    val advertisementPayload: String,
    val firstSeenTimestamp: Long,
    val lastSeenTimestamp: Long,
    val hitCount: Int
)
