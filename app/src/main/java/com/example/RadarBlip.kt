package com.example

import java.util.UUID

data class RadarBlip(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val distance: Float,           // in meters
    val targetAngleOffset: Float,  // Relative to compass heading (0 = dead ahead)
    val type: String,              // "WIFI", "BLE", "CELLULAR", "MAGNETIC", "AUDIO", etc.
    val rssi: Int = -70,
    val frequencyMhz: Double = 2400.0,
    val bandLabel: String = "2.4G",
    val estimatedZOffsetMeters: Float = 0f, // Relative altitude offset (+ is above, - is below)
    val ouiVendor: String? = null,
    val isHighRiskVendor: Boolean = false,
    val isChannelSoundingCapable: Boolean = false,
    val csEstimatedAccuracyMeters: Float = 1.0f,
    val csRangingMethod: String = "RSSI",
    val provenance: DataProvenance = DataProvenance.UNKNOWN,
    val localizationConfidence: LocalizationConfidence = LocalizationConfidence.NONE,
    val fingerprintId: String? = null,
    val fingerprintConfidence: Float? = null,
    val bandwidthMhz: Double? = null,
    val pulseDurationMs: Double? = null,
    val baselineState: BaselineState = BaselineState.UNKNOWN,
    val pulseRepetitionIntervalMs: Double? = null,
    val anomalyResult: AnomalyResult? = null,
    val payloadBytes: ByteArray? = null,
    val timestampMs: Long = System.currentTimeMillis()
)
