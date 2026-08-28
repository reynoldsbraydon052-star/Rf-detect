package com.example

import android.content.Context
import java.util.UUID

data class EnrichmentResult(
    val hardwareCategory: String,
    val powerClass: String,
    val protocolFingerprint: String,
    val securityProfile: String,
    val extractedIdentifier: String? = null,
    val semanticProfile: SemanticDeviceProfile? = null
)

object PayloadParser {
    /**
     * Parses BLE Manufacturer Specific Data or Wi-Fi Information Elements (IEs) from raw payload bytes.
     * Extracts embedded real identifiers such as MAC addresses, custom UUIDs, or proprietary serial tags.
     */
    fun parsePayload(payloadBytes: ByteArray?): String? {
        if (payloadBytes == null || payloadBytes.isEmpty()) return null
        
        try {
            // Check for BLE Manufacturer Specific Data pattern
            // For example, if it contains an Apple Airtag prefix or custom serial signature:
            // Custom payload can encode a real MAC or a unique serial in ASCII or HEX.
            if (payloadBytes.size >= 6) {
                // Let's check for custom markers: e.g. "SR" (SignalRadar) or custom UUID structure
                val prefix = String(payloadBytes.take(2).toByteArray(), Charsets.US_ASCII)
                if (prefix == "SR" || prefix == "ID") {
                    // Extract remaining bytes as string/id
                    return payloadBytes.drop(2).joinToString("") { "%02X".format(it) }
                }
                
                // If it looks like a MAC address embedded in payload bytes (6 bytes)
                if (payloadBytes.size == 6) {
                    val macBytes = payloadBytes.joinToString(":") { "%02X".format(it) }
                    if (macBytes != "02:00:00:00:00:00") {
                        return macBytes
                    }
                }
            }
        } catch (e: Exception) {
            // Fail-safe
        }
        return null
    }
}

object MacSanitizer {
    /**
     * Strips database prefixes, formats MAC addresses to clean uppercase XX:XX:XX:XX:XX:XX structures,
     * and flags masked "02:00:00:00:00:00" MACs as "MASKED_BY_PLATFORM".
     */
    fun sanitize(mac: String?): String {
        if (mac == null || mac.isBlank()) return "UNKNOWN_MAC"
        
        // Strip database prefixes (like "ble_db_", "wifi_db_", or any prefix containing "_db_")
        var clean = mac.replace("ble_db_", "")
                       .replace("wifi_db_", "")
                       .replace("cellular_db_", "")
        if (clean.contains("_db_")) {
            clean = clean.substringAfter("_db_")
        }
        
        clean = clean.uppercase().trim()
        
        if (clean == "02:00:00:00:00:00" || clean == "020000000000") {
            return "MASKED_BY_PLATFORM"
        }
        
        // Format to XX:XX:XX:XX:XX:XX
        val hexOnly = clean.replace(":", "").replace("-", "")
        if (hexOnly.length == 12 && hexOnly.all { it.isDigit() || it in 'A'..'F' }) {
            return hexOnly.chunked(2).joinToString(":")
        }
        
        return clean
    }
}

object OfflineOuiResolver {
    /**
     * Queries the offline OuiDatabase first and falls back to a hardcoded map if not found.
     * Completely local and offline.
     */
    suspend fun resolveVendorOffline(context: Context, mac: String?): String? {
        if (mac == null || mac.isBlank()) return null
        val cleanMac = MacSanitizer.sanitize(mac)
        if (cleanMac == "MASKED_BY_PLATFORM") return "Masked Platform Transmitter"
        
        if (cleanMac.length < 8) return null
        val prefix = cleanMac.take(8).uppercase()
        
        // 1. Check Room database
        try {
            val db = OuiDatabase.getDatabase(context)
            val entity = db.ouiDao().getVendorByPrefix(prefix)
            if (entity != null) return entity.vendorName
        } catch (e: Exception) {
            // Fall through to local map
        }
        
        // 2. Check MacOuidResolver direct local map
        return MacOuidResolver.resolveVendor(cleanMac)
    }
}

object HardwareIdentityEnricher {
    /**
     * Analyzes vendor details, payload elements, and masked states to determine Category, Power Class,
     * Protocol Fingerprints, and Security Profiles.
     */
    fun enrich(mac: String, vendor: String?, payload: ByteArray?, blipType: String): EnrichmentResult {
        val sanitized = MacSanitizer.sanitize(mac)
        val extractedId = PayloadParser.parsePayload(payload)
        
        // Base defaults
        var category = "Embedded"
        var powerClass = "Mobile Transceiver"
        var fingerprint = "Generic Protocol Fingerprint"
        var securityProfile = "WPA2-Personal"
        
        val isMasked = sanitized == "MASKED_BY_PLATFORM"
        
        if (isMasked) {
            fingerprint = "Platform-Masked MAC Signature"
            if (blipType == "WIFI") {
                category = "Router"
                powerClass = "High-Power Infrastructure"
                fingerprint = "Wi-Fi Information Element (IE) Fingerprint [802.11be MLO]"
                securityProfile = "WPA3-Enterprise"
            } else if (blipType == "BLE") {
                category = "Smart IoT"
                powerClass = "Low-Energy Beacon"
                fingerprint = "BLE Advertisement Payload Hash"
                securityProfile = "BLE-Secure-Pairing"
            }
        } else {
            // Non-masked, classify based on type and vendor
            if (blipType == "WIFI") {
                category = "Router"
                powerClass = "High-Power Infrastructure"
                fingerprint = "802.11 Frame Sequence Hash"
                securityProfile = "WPA2-Personal"
                
                vendor?.let { v ->
                    if (v.contains("Cisco", ignoreCase = true)) {
                        fingerprint = "Cisco Discovery Protocol (CDP) Element"
                        securityProfile = "WPA3-Enterprise"
                    } else if (v.contains("Apple", ignoreCase = true)) {
                        category = "Handheld"
                        powerClass = "Mobile Transceiver"
                        fingerprint = "Apple Wireless Direct Link (AWDL)"
                        securityProfile = "WPA3-Personal"
                    } else if (v.contains("Espressif", ignoreCase = true)) {
                        category = "Sensor"
                        powerClass = "Low-Energy Beacon"
                        fingerprint = "Espressif SmartConfig Information Element"
                        securityProfile = "Open-Broadcast"
                    }
                }
            } else if (blipType == "BLE") {
                category = "Sensor"
                powerClass = "Low-Energy Beacon"
                fingerprint = "BLE Proximity Advertisements"
                securityProfile = "Open-Broadcast"
                
                vendor?.let { v ->
                    if (v.contains("Apple", ignoreCase = true)) {
                        category = "Smart IoT"
                        fingerprint = "Apple iBeacon / FindMy Protocol"
                        securityProfile = "BLE-Secure-Pairing"
                    } else if (v.contains("Tile", ignoreCase = true) || v.contains("Oura", ignoreCase = true) || v.contains("Whoop", ignoreCase = true)) {
                        category = "Smart IoT"
                        securityProfile = "BLE-Secure-Pairing"
                    } else if (v.contains("Google", ignoreCase = true)) {
                        category = "Handheld"
                        powerClass = "Mobile Transceiver"
                        fingerprint = "Google Fast Pair Service (GFPS)"
                        securityProfile = "BLE-Secure-Pairing"
                    }
                }
            } else if (blipType == "CELLULAR") {
                category = "Handheld"
                powerClass = "Mobile Transceiver"
                fingerprint = "3GPP cellular tracking signatures"
                securityProfile = "GSM-A5/3-Encryption"
            }
        }
        
        // If we successfully extracted an identifier from raw payload, flag it in fingerprint
        if (extractedId != null) {
            fingerprint += " (Extracted Real ID: $extractedId)"
        }

        val semanticProfile = if (blipType == "BLE" && payload != null && payload.isNotEmpty()) {
            val decoded = BlePayloadDecoder.decode(payload)
            if (decoded.proprietaryEcosystemType != null) {
                fingerprint = "${decoded.proprietaryEcosystemType} Protocol"
                category = when {
                    decoded.proprietaryEcosystemType.contains("AirTag") || decoded.proprietaryEcosystemType.contains("SmartTag") || decoded.proprietaryEcosystemType.contains("iBeacon") || decoded.proprietaryEcosystemType.contains("Tile") -> "Smart IoT / Tracker"
                    decoded.proprietaryEcosystemType.contains("AirPods") || decoded.proprietaryEcosystemType.contains("Audio") -> "Audio / Wearable"
                    else -> "Smart IoT"
                }
            } else if (decoded.appearanceCategory != null && decoded.appearanceCategory != "Unknown") {
                category = decoded.appearanceCategory
            }
            decoded
        } else null

        return EnrichmentResult(
            hardwareCategory = category,
            powerClass = powerClass,
            protocolFingerprint = fingerprint,
            securityProfile = securityProfile,
            extractedIdentifier = extractedId,
            semanticProfile = semanticProfile
        )
    }
}
