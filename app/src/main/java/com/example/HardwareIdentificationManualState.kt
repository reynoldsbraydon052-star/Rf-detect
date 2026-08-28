package com.example

/**
 * State representing manual or automated hardware identification resolution for a target device.
 * Consumes the decoded [SemanticDeviceProfile] and prioritizes semantic names & appearances
 * over raw MAC addresses and generic OUI names.
 */
data class HardwareIdentificationManualState(
    val targetMac: String = "",
    val semanticProfile: SemanticDeviceProfile? = null,
    val resolvedDisplayName: String = "",
    val hardwareCategory: String = "Unknown Category",
    val capabilitiesSummary: List<String> = emptyList(),
    val isEcosystemTracker: Boolean = false,
    val ecosystemLabel: String? = null,
    val rawPayloadHex: String = "",
    val decodedGapTypes: List<String> = emptyList(),
    val isNameFromPayload: Boolean = false,
    val isAppearanceFromPayload: Boolean = false
) {
    companion object {
        /**
         * Resolves the primary semantic identity from available telemetry with strict priority hierarchy:
         * 1. Complete/Shortened Local Name from BLE payload
         * 2. Proprietary Ecosystem device type (e.g., Apple iBeacon, AirTag, Galaxy SmartTag, Fast Pair)
         * 3. Distinct Appearance Name from BLE payload (e.g., Smartwatch, Keyboard, Heart Rate Sensor)
         * 4. Provided non-generic Device Name
         * 5. OUI Vendor name
         * 6. Sanitized MAC address (fallback)
         */
        fun fromProfile(
            macAddress: String,
            fallbackName: String? = null,
            ouiVendor: String? = null,
            profile: SemanticDeviceProfile? = null,
            rawBytes: ByteArray? = null
        ): HardwareIdentificationManualState {
            val cleanMac = MacSanitizer.sanitize(macAddress)
            val isMasked = cleanMac == "MASKED_BY_PLATFORM"

            val localName = profile?.localName?.takeIf { it.isNotBlank() }
            val appearanceName = profile?.appearanceDisplayName?.takeIf { it.isNotBlank() && it != "Unknown Appearance" }
            val ecosystemType = profile?.proprietaryEcosystemType?.takeIf { it.isNotBlank() }
            val vendor = profile?.manufacturerName ?: ouiVendor

            // Resolve prioritized display name
            val (resolvedName, isNamePayload, isAppPayload) = when {
                !localName.isNullOrBlank() -> Triple(localName, true, false)
                !ecosystemType.isNullOrBlank() -> Triple(ecosystemType, true, false)
                !appearanceName.isNullOrBlank() -> Triple(appearanceName, false, true)
                !fallbackName.isNullOrBlank() && fallbackName != "Unknown BLE Device" && fallbackName != "Unknown Device" -> Triple(fallbackName, false, false)
                !vendor.isNullOrBlank() && vendor != "Masked Platform Transmitter" -> Triple("$vendor Device", false, false)
                isMasked -> Triple("Masked Transceiver", false, false)
                else -> Triple("Target [${cleanMac.takeLast(8)}]", false, false)
            }

            // Determine Hardware Category
            val hardwareCategory = when {
                profile?.appearanceCategory != null && profile.appearanceCategory != "Unknown" -> profile.appearanceCategory
                ecosystemType != null -> when {
                    ecosystemType.contains("AirTag", ignoreCase = true) || ecosystemType.contains("SmartTag", ignoreCase = true) || ecosystemType.contains("iBeacon", ignoreCase = true) || ecosystemType.contains("Tile", ignoreCase = true) -> "Location Tracker"
                    ecosystemType.contains("Audio", ignoreCase = true) || ecosystemType.contains("AirPods", ignoreCase = true) -> "Audio / Headset"
                    ecosystemType.contains("Fast Pair", ignoreCase = true) || ecosystemType.contains("Nearby Share", ignoreCase = true) -> "Handheld / Peripheral"
                    else -> "Smart IoT"
                }
                !vendor.isNullOrBlank() -> when {
                    vendor.contains("Apple", ignoreCase = true) -> "Apple Peripheral"
                    vendor.contains("Samsung", ignoreCase = true) -> "Samsung Mobile/IoT"
                    vendor.contains("Google", ignoreCase = true) -> "Google Hardware"
                    vendor.contains("Nordic", ignoreCase = true) || vendor.contains("Espressif", ignoreCase = true) -> "Embedded Sensor"
                    else -> "Transceiver"
                }
                else -> "Unknown Category"
            }

            // Capabilities summary
            val capabilities = mutableListOf<String>()
            profile?.serviceCapabilities?.let { capabilities.addAll(it) }
            if (profile?.beaconDetails != null) {
                profile.beaconDetails.trackingCategory?.let { capabilities.add(it) }
            }
            if (capabilities.isEmpty() && profile?.txPowerDbm != null) {
                capabilities.add("Calibrated Tx Power (${profile.txPowerDbm} dBm)")
            }

            // Ecosystem Tracking Flag
            val isEcosystemTracker = ecosystemType != null || profile?.beaconDetails != null

            // Decoded GAP type labels
            val gapTypes = profile?.rawGapRecords?.map { it.typeName } ?: emptyList()

            val hex = profile?.manufacturerPayloadHex
                ?: rawBytes?.joinToString("") { "%02X".format(it) }
                ?: ""

            return HardwareIdentificationManualState(
                targetMac = cleanMac,
                semanticProfile = profile,
                resolvedDisplayName = resolvedName,
                hardwareCategory = hardwareCategory,
                capabilitiesSummary = capabilities.distinct(),
                isEcosystemTracker = isEcosystemTracker,
                ecosystemLabel = ecosystemType ?: profile?.beaconDetails?.trackingCategory,
                rawPayloadHex = hex,
                decodedGapTypes = gapTypes,
                isNameFromPayload = isNamePayload,
                isAppearanceFromPayload = isAppPayload
            )
        }
    }
}
