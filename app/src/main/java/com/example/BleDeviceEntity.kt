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
    val hitCount: Int,
    // Bluetooth 6.0 Channel Sounding (CS) High-Precision Tracking Fields
    val isChannelSoundingCapable: Boolean = false,
    val csRangingMethod: String = "RSSI Fallback", // "PBR + RTT (Phase-Based + Time-of-Flight)", "PBR Phase Ranging", "RTT Time-of-Flight", "RSSI Fallback"
    val csPhaseQualityIndex: Int = 0, // 0 to 100% phase alignment score across 79 channels
    val csEstimatedAccuracyMeters: Float = 3.5f, // Precision error margin e.g., ±0.15m for CS vs ±3.5m for RSSI
    val csRttTimeOfFlightNs: Float = 0.0f, // Nanosecond flight time
    val csChannelCount: Int = 0, // Number of RF channels swept (e.g. 79 channels)
    val catalogueTag: String = "Uncategorized" // Custom user catalogue tag e.g. "Known Home Router", "Unknown Personal Tracker"
)
