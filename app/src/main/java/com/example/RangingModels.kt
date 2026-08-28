package com.example

enum class RangingMethod {
    BLE_CHANNEL_SOUNDING,
    BLE_RSSI_ESTIMATE
}

enum class SignalQuality {
    HIGH,
    MEDIUM,
    LOW
}

data class TacticalRangingResult(
    val targetMac: String,
    val distanceMeters: Double,
    val method: RangingMethod,
    val confidenceScore: Double, // (0.0 to 1.0)
    val quality: SignalQuality,
    val rttOrRssiDb: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    // Explicit requested properties for distance in feet, confidence, and quality
    val distanceFeet: Double get() = distanceMeters * 3.28084
    val confidence: Double get() = confidenceScore
}
