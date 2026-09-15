package com.example

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs

enum class AnomalyType {
    DEAUTH_FLOOD,
    EVIL_TWIN_AP,
    BEACON_BURST_SPOOF,
    KARMA_PROBE_RESPONSE
}

data class DetectedThreatEvent(
    val timestamp: Long,
    val anomalyType: AnomalyType,
    val severity: AnomalySeverity,
    val targetSsid: String?,
    val suspectBssid: String?,
    val details: String
)

data class ObservedApProfile(
    val bssid: String,
    val ssid: String,
    val capabilities: String,
    val frequencyMhz: Int,
    val lastSeenTimestamp: Long
)

/**
 * Thread-safe analysis engine to detect Wi-Fi Pineapple operations, disassociation/deauth floods,
 * and rogue Evil Twin access point intrusions.
 */
class RogueFrameAnomalyDetector(
    private val deauthRateThreshold: Int = 15,
    private val slidingWindowMillis: Long = 5000L
) {
    // Map tracking seen BSSIDs mapped to their profiled specs
    private val knownApProfiles = ConcurrentHashMap<String, ObservedApProfile>()
    
    // Map tracking BSSID disassociation/deauth frame timestamps for flood detection
    private val deauthTimeline = ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>>()

    /**
     * Audits incoming Wi-Fi access point parameters against known baseline profiles.
     * Flags potential EVIL_TWIN_AP instances if matching SSIDs display critical security or frequency variations.
     */
    fun auditAccessPoint(
        bssid: String,
        ssid: String,
        capabilities: String,
        frequencyMhz: Int,
        rssi: Int
    ): DetectedThreatEvent? {
        val now = System.currentTimeMillis()
        if (ssid.isEmpty()) return null

        // Scan profiles for Evil Twin activity (SSID matches but BSSID is different, or caps are mismatched)
        for (profile in knownApProfiles.values) {
            if (profile.ssid == ssid && profile.bssid != bssid) {
                // Potential clone detected! Check if key properties are radically mismatched
                val capabilityMismatch = profile.capabilities != capabilities
                val bandMismatch = abs(profile.frequencyMhz - frequencyMhz) > 100 // Over 100Mhz gap
                
                if (capabilityMismatch || bandMismatch) {
                    return DetectedThreatEvent(
                        timestamp = now,
                        anomalyType = AnomalyType.EVIL_TWIN_AP,
                        severity = AnomalySeverity.HIGH,
                        targetSsid = ssid,
                        suspectBssid = bssid,
                        details = "Identical SSID clone broadcasting with mismatching specifications. " +
                                "Original (BSSID: ${profile.bssid}, Caps: ${profile.capabilities}), " +
                                "Rogue (BSSID: $bssid, Caps: $capabilities)."
                    )
                }
            }
        }

        // Add or update AP profile registry
        knownApProfiles[bssid] = ObservedApProfile(
            bssid = bssid,
            ssid = ssid,
            capabilities = capabilities,
            frequencyMhz = frequencyMhz,
            lastSeenTimestamp = now
        )

        return null
    }

    /**
     * Registers a Wi-Fi management frame disconnection/deauth event on a target BSSID.
     * Evaluates packet rate within a sliding window to detect denial-of-service deauth flooding.
     */
    fun recordDisassociationEvent(bssid: String): DetectedThreatEvent? {
        val now = System.currentTimeMillis()
        val timeline = deauthTimeline.getOrPut(bssid) { ConcurrentLinkedQueue() }

        // Append current disassociation incident
        timeline.add(now)

        // Trim expired disassociations outside the sliding window
        val thresholdTime = now - slidingWindowMillis
        while (timeline.isNotEmpty() && timeline.peek()!! < thresholdTime) {
            timeline.poll()
        }

        // Audit deauth frequency breach
        if (timeline.size >= deauthRateThreshold) {
            val detailsMessage = "DEAUTH FLOOD ALERT: Received ${timeline.size} disassociation frames " +
                    "on BSSID $bssid within ${slidingWindowMillis / 1000}s. High probability of rogue hardware handshake interception."
            
            // Purge timeline after triggering alert to avoid duplicate alarms on subsequent frames
            timeline.clear()

            return DetectedThreatEvent(
                timestamp = now,
                anomalyType = AnomalyType.DEAUTH_FLOOD,
                severity = AnomalySeverity.CRITICAL,
                targetSsid = knownApProfiles[bssid]?.ssid,
                suspectBssid = bssid,
                details = detailsMessage
            )
        }

        return null
    }

    fun clearHistory() {
        knownApProfiles.clear()
        deauthTimeline.clear()
    }
}
