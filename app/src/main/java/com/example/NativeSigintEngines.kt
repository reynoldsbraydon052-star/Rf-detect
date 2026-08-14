package com.example

import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class KnownCellTower(
    val mcc: Int,
    val mnc: Int,
    val tac: Int,
    val pci: Int,
    val latitude: Double,
    val longitude: Double,
    val maxRangeMeters: Float = 3000f
)

data class ImsiCatcherAlert(
    val isAlertTriggered: Boolean = false,
    val connectedMcc: Int = 0,
    val connectedMnc: Int = 0,
    val connectedTac: Int = 0,
    val connectedPci: Int = 0,
    val severity: String = "NORMAL",
    val alertMessage: String = ""
)

data class EwAlertState(
    val isRfJammingDetected: Boolean = false,
    val isGnssSpoofingDetected: Boolean = false,
    val imsiCatcherAlert: ImsiCatcherAlert = ImsiCatcherAlert()
)

object ElectronicWarfareMonitor {
    private var lastRssiAverage: Float? = null
    private var lastSurgeTimestamp: Long = 0L

    /**
     * RF Jamming Heuristic:
     * Track ambient noise floor RSSI averages. If ambient noise surges (> 20 dBm increase)
     * while scan result counts drop to zero over a 3000ms window, flag isRfJammingDetected = true.
     */
    fun evaluateRfJamming(ambientRssiAvg: Float, totalScanCount: Int): Boolean {
        val now = System.currentTimeMillis()
        val prevAvg = lastRssiAverage
        lastRssiAverage = ambientRssiAvg

        if (prevAvg != null && (ambientRssiAvg - prevAvg) > 20f && totalScanCount == 0) {
            lastSurgeTimestamp = now
        }

        return (now - lastSurgeTimestamp) < 3000L
    }

    /**
     * GNSS Spoofing Heuristic:
     * Inspect satellite Carrier-to-Noise density (C/N0) across 4+ satellites.
     * Flag isGnssSpoofingDetected = true if C/N0 becomes abnormally uniform (variance < 1.0 dB-Hz),
     * or if geodetic position velocity exceeds physical limits (> 250 m/s).
     */
    fun evaluateGnssSpoofing(
        cn0DbHzList: List<Float>,
        currentVelocityMps: Float
    ): Boolean {
        if (currentVelocityMps > 250f) return true

        if (cn0DbHzList.size >= 4) {
            val mean = cn0DbHzList.average()
            val variance = cn0DbHzList.map { (it - mean) * (it - mean) }.average()
            if (variance < 1.0) return true
        }

        return false
    }
}

class OpenCellIdVerifier {
    private val knownTowersWhitelist = mutableListOf(
        KnownCellTower(310, 260, 1024, 128, 37.7749, -122.4194, 5000f),
        KnownCellTower(310, 410, 2048, 256, 34.0522, -118.2437, 5000f),
        KnownCellTower(311, 480, 512, 64, 40.7128, -74.0060, 5000f)
    )

    fun addKnownTower(tower: KnownCellTower) {
        knownTowersWhitelist.add(tower)
    }

    /**
     * Cross-reference active cell site against local KnownCellTower whitelist.
     * If connected cell site ID is unlisted or distance > maxRangeMeters,
     * flag an ImsiCatcherAlert with severity "ROGUE_CELL_SITE_IMSI_CATCHER".
     */
    fun verifyCellTower(
        mcc: Int,
        mnc: Int,
        tac: Int,
        pci: Int,
        deviceLat: Double,
        deviceLon: Double
    ): ImsiCatcherAlert {
        if (mcc == 0 && mnc == 0 && tac == 0 && pci == 0) {
            return ImsiCatcherAlert()
        }

        val matchingTower = knownTowersWhitelist.find {
            (it.mcc == mcc || mcc == 0) &&
            (it.mnc == mnc || mnc == 0) &&
            it.tac == tac && it.pci == pci
        }

        if (matchingTower == null) {
            return ImsiCatcherAlert(
                isAlertTriggered = true,
                connectedMcc = mcc,
                connectedMnc = mnc,
                connectedTac = tac,
                connectedPci = pci,
                severity = "ROGUE_CELL_SITE_IMSI_CATCHER",
                alertMessage = "UNLISTED CELL TOWER TAC=$tac PCI=$pci (IMSI CATCHER STINGRAY SUSPECTED)"
            )
        }

        val distance = calculateDistanceMeters(deviceLat, deviceLon, matchingTower.latitude, matchingTower.longitude)
        if (distance > matchingTower.maxRangeMeters) {
            return ImsiCatcherAlert(
                isAlertTriggered = true,
                connectedMcc = mcc,
                connectedMnc = mnc,
                connectedTac = tac,
                connectedPci = pci,
                severity = "ROGUE_CELL_SITE_IMSI_CATCHER",
                alertMessage = "CELL TOWER RANGE ANOMALY: ${distance.toInt()}m > ${matchingTower.maxRangeMeters.toInt()}m"
            )
        }

        return ImsiCatcherAlert(
            isAlertTriggered = false,
            connectedMcc = mcc,
            connectedMnc = mnc,
            connectedTac = tac,
            connectedPci = pci,
            severity = "NORMAL",
            alertMessage = "VERIFIED CELL TOWER SITE"
        )
    }

    private fun calculateDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toFloat()
    }
}
