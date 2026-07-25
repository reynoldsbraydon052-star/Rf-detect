package com.example

data class RadarBlip(
    val id: String,
    val name: String,
    val distance: Float, // Calculated distance in meters
    val targetAngleOffset: Float, // Angle 0..360
    val type: String, // "WIFI", "CELLULAR", "BLE", "AUDIO"
    val rssi: Int = -60,
    val frequencyMhz: Double = 2412.0,
    val bandLabel: String = "2.4 GHz",
    val timestampMs: Long = System.currentTimeMillis()
)
