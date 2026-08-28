package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * Extracted raw GAP record representing a single TLV (Length-Type-Value) structure.
 */
data class ParsedGapRecord(
    val type: Int,
    val typeName: String,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ParsedGapRecord
        if (type != other.type) return false
        if (typeName != other.typeName) return false
        if (!data.contentEquals(other.data)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + typeName.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * High-level proprietary beacon telemetry details.
 */
data class BeaconDetails(
    val beaconType: String, // "iBeacon", "FindMy", "NearbyShare", "FastPair", "SmartTag", "AltBeacon", "Eddystone"
    val proximityUuid: String? = null,
    val major: Int? = null,
    val minor: Int? = null,
    val measuredPowerAt1m: Int? = null,
    val trackingCategory: String? = null,
    val extraDescription: String? = null
)

/**
 * Aggregated semantic profile extracted entirely offline from BLE advertisement packets.
 */
data class SemanticDeviceProfile(
    val localName: String? = null,
    val isShortenedName: Boolean = false,
    val appearanceHex: Int? = null,
    val appearanceCategory: String? = null,
    val appearanceDisplayName: String? = null,
    val serviceUuids16: List<Int> = emptyList(),
    val serviceUuids32: List<Long> = emptyList(),
    val serviceUuids128: List<String> = emptyList(),
    val serviceCapabilities: List<String> = emptyList(),
    val manufacturerId: Int? = null,
    val manufacturerName: String? = null,
    val manufacturerPayloadHex: String? = null,
    val proprietaryEcosystemType: String? = null,
    val beaconDetails: BeaconDetails? = null,
    val txPowerDbm: Int? = null,
    val rawGapRecords: List<ParsedGapRecord> = emptyList()
)

/**
 * Passive offline BLE advertisement payload decoder (GAP parser & ecosystem identifier).
 */
object BlePayloadDecoder {

    // Standard GAP Data Type Constants
    const val GAP_FLAGS = 0x01
    const val GAP_INCOMPLETE_16BIT_UUIDS = 0x02
    const val GAP_COMPLETE_16BIT_UUIDS = 0x03
    const val GAP_INCOMPLETE_32BIT_UUIDS = 0x04
    const val GAP_COMPLETE_32BIT_UUIDS = 0x05
    const val GAP_INCOMPLETE_128BIT_UUIDS = 0x06
    const val GAP_COMPLETE_128BIT_UUIDS = 0x07
    const val GAP_SHORTENED_LOCAL_NAME = 0x08
    const val GAP_COMPLETE_LOCAL_NAME = 0x09
    const val GAP_TX_POWER_LEVEL = 0x0A
    const val GAP_SERVICE_DATA_16BIT = 0x16
    const val GAP_APPEARANCE = 0x19
    const val GAP_SERVICE_DATA_32BIT = 0x20
    const val GAP_SERVICE_DATA_128BIT = 0x21
    const val GAP_MANUFACTURER_SPECIFIC = 0xFF

    /**
     * Decode byte array asynchronously on Dispatchers.Default to prevent dropping frames on UI threads.
     */
    suspend fun decodeAsync(payloadBytes: ByteArray?): SemanticDeviceProfile = withContext(Dispatchers.Default) {
        decode(payloadBytes)
    }

    /**
     * Synchronously decodes raw BLE ScanRecord byte arrays into a structured SemanticDeviceProfile.
     * Guaranteed to never throw IndexOutOfBoundsException or crash on corrupt inputs.
     */
    fun decode(payloadBytes: ByteArray?): SemanticDeviceProfile {
        if (payloadBytes == null || payloadBytes.isEmpty()) {
            return SemanticDeviceProfile()
        }

        val records = parseGapRecords(payloadBytes)
        if (records.isEmpty()) {
            return SemanticDeviceProfile()
        }

        var localName: String? = null
        var isShortenedName = false
        var appearanceHex: Int? = null
        var txPower: Int? = null

        val uuids16 = mutableListOf<Int>()
        val uuids32 = mutableListOf<Long>()
        val uuids128 = mutableListOf<String>()

        var manufacturerId: Int? = null
        var manufacturerBytes: ByteArray? = null

        for (record in records) {
            when (record.type) {
                GAP_COMPLETE_LOCAL_NAME -> {
                    val name = parseStringSafely(record.data)
                    if (name.isNotBlank()) {
                        localName = name
                        isShortenedName = false
                    }
                }
                GAP_SHORTENED_LOCAL_NAME -> {
                    if (localName == null) {
                        val name = parseStringSafely(record.data)
                        if (name.isNotBlank()) {
                            localName = name
                            isShortenedName = true
                        }
                    }
                }
                GAP_APPEARANCE -> {
                    if (record.data.size >= 2) {
                        val low = record.data[0].toInt() and 0xFF
                        val high = record.data[1].toInt() and 0xFF
                        appearanceHex = low or (high shl 8)
                    }
                }
                GAP_INCOMPLETE_16BIT_UUIDS, GAP_COMPLETE_16BIT_UUIDS -> {
                    var offset = 0
                    while (offset + 1 < record.data.size) {
                        val low = record.data[offset].toInt() and 0xFF
                        val high = record.data[offset + 1].toInt() and 0xFF
                        uuids16.add(low or (high shl 8))
                        offset += 2
                    }
                }
                GAP_SERVICE_DATA_16BIT -> {
                    if (record.data.size >= 2) {
                        val low = record.data[0].toInt() and 0xFF
                        val high = record.data[1].toInt() and 0xFF
                        uuids16.add(low or (high shl 8))
                    }
                }
                GAP_INCOMPLETE_32BIT_UUIDS, GAP_COMPLETE_32BIT_UUIDS, GAP_SERVICE_DATA_32BIT -> {
                    var offset = 0
                    while (offset + 3 < record.data.size) {
                        val b0 = (record.data[offset].toLong() and 0xFF)
                        val b1 = (record.data[offset + 1].toLong() and 0xFF) shl 8
                        val b2 = (record.data[offset + 2].toLong() and 0xFF) shl 16
                        val b3 = (record.data[offset + 3].toLong() and 0xFF) shl 24
                        uuids32.add(b0 or b1 or b2 or b3)
                        offset += 4
                    }
                }
                GAP_INCOMPLETE_128BIT_UUIDS, GAP_COMPLETE_128BIT_UUIDS, GAP_SERVICE_DATA_128BIT -> {
                    var offset = 0
                    while (offset + 15 < record.data.size) {
                        val uuidStr = parse128BitUuid(record.data, offset)
                        if (uuidStr != null) {
                            uuids128.add(uuidStr)
                        }
                        offset += 16
                    }
                }
                GAP_TX_POWER_LEVEL -> {
                    if (record.data.isNotEmpty()) {
                        txPower = record.data[0].toInt()
                    }
                }
                GAP_MANUFACTURER_SPECIFIC -> {
                    if (record.data.size >= 2) {
                        val low = record.data[0].toInt() and 0xFF
                        val high = record.data[1].toInt() and 0xFF
                        manufacturerId = low or (high shl 8)
                        manufacturerBytes = record.data
                    }
                }
            }
        }

        // Appearance Resolution
        val appearanceInfo = appearanceHex?.let { SigAssignedNumbersDictionary.resolveAppearance(it) }

        // Capabilities resolution from Service UUIDs
        val capabilities = mutableListOf<String>()
        uuids16.distinct().forEach { u16 ->
            SigAssignedNumbersDictionary.resolveServiceUuid16(u16)?.let { desc ->
                capabilities.add(desc)
            }
        }

        // Manufacturer Identification & Ecosystem Pattern Matching
        val manufacturerName = manufacturerId?.let { SigAssignedNumbersDictionary.resolveCompanyId(it) }
        val manufacturerHex = manufacturerBytes?.let { bytesToHex(it) }

        val (ecosystemType, beaconDetails) = if (manufacturerBytes != null && manufacturerId != null) {
            matchProprietaryBeacon(manufacturerId, manufacturerBytes, uuids16)
        } else if (uuids16.isNotEmpty()) {
            matchServiceUuidBeacon(uuids16)
        } else {
            Pair(null, null)
        }

        return SemanticDeviceProfile(
            localName = localName,
            isShortenedName = isShortenedName,
            appearanceHex = appearanceHex,
            appearanceCategory = appearanceInfo?.categoryName,
            appearanceDisplayName = appearanceInfo?.displayName,
            serviceUuids16 = uuids16.distinct(),
            serviceUuids32 = uuids32.distinct(),
            serviceUuids128 = uuids128.distinct(),
            serviceCapabilities = capabilities.distinct(),
            manufacturerId = manufacturerId,
            manufacturerName = manufacturerName,
            manufacturerPayloadHex = manufacturerHex,
            proprietaryEcosystemType = ecosystemType,
            beaconDetails = beaconDetails,
            txPowerDbm = txPower,
            rawGapRecords = records
        )
    }

    /**
     * Parses raw byte array into a list of GAP records safely without throwing exceptions.
     */
    fun parseGapRecords(bytes: ByteArray): List<ParsedGapRecord> {
        val records = mutableListOf<ParsedGapRecord>()
        var index = 0
        val length = bytes.size

        while (index < length) {
            val elementLength = bytes[index].toInt() and 0xFF
            if (elementLength == 0) {
                // End of active GAP records in advertising frame
                break
            }

            if (index + 1 >= length) {
                // Corrupt / truncated header
                break
            }

            val type = bytes[index + 1].toInt() and 0xFF
            val dataLength = elementLength - 1
            if (dataLength < 0) {
                break
            }

            val dataStart = index + 2
            val dataEnd = (dataStart + dataLength).coerceAtMost(length)
            val data = if (dataStart <= length) {
                bytes.copyOfRange(dataStart, dataEnd)
            } else {
                byteArrayOf()
            }

            records.add(
                ParsedGapRecord(
                    type = type,
                    typeName = getGapTypeName(type),
                    data = data
                )
            )

            index += (elementLength + 1)
        }

        return records
    }

    /**
     * Pattern matches 0xFF Manufacturer Specific Data for Apple, Google, Samsung, AltBeacon ecosystems.
     */
    private fun matchProprietaryBeacon(
        companyId: Int,
        payload: ByteArray, // starts with Company ID (2 bytes) + manufacturer payload
        serviceUuids16: List<Int>
    ): Pair<String?, BeaconDetails?> {
        val payloadLen = payload.size

        // 1. Apple Ecosystem (0x004C)
        if (companyId == 0x004C && payloadLen >= 3) {
            val appleType = payload[2].toInt() and 0xFF

            when (appleType) {
                // Apple iBeacon: [0x4C, 0x00, 0x02, 0x15, UUID (16 bytes), Major (2 bytes), Minor (2 bytes), TxPower (1 byte)]
                0x02 -> {
                    if (payloadLen >= 23) {
                        val uuidBytes = payload.copyOfRange(4, 20.coerceAtMost(payloadLen))
                        val uuidStr = formatUuid(uuidBytes)
                        val major = if (payloadLen >= 22) {
                            ((payload[20].toInt() and 0xFF) shl 8) or (payload[21].toInt() and 0xFF)
                        } else null
                        val minor = if (payloadLen >= 24) {
                            ((payload[22].toInt() and 0xFF) shl 8) or (payload[23].toInt() and 0xFF)
                        } else null
                        val txPower = if (payloadLen >= 25) payload[24].toInt() else -59

                        val details = BeaconDetails(
                            beaconType = "iBeacon",
                            proximityUuid = uuidStr,
                            major = major,
                            minor = minor,
                            measuredPowerAt1m = txPower,
                            trackingCategory = "Apple iBeacon Proximity Location",
                            extraDescription = "Major: $major, Minor: $minor, Power@1m: ${txPower}dBm"
                        )
                        return Pair("Apple iBeacon", details)
                    }
                    return Pair("Apple iBeacon (Truncated)", null)
                }

                // Apple AirTag / Find My Offline Finding Token (Type 0x12)
                0x12 -> {
                    val details = BeaconDetails(
                        beaconType = "FindMy",
                        trackingCategory = "Apple AirTag / Find My Network Tracker",
                        extraDescription = "Encrypted rotating public key advertisement"
                    )
                    return Pair("Apple AirTag / Find My", details)
                }

                // Apple AirPods / Proximity Audio (Type 0x07 or 0x0F)
                0x07, 0x0F -> {
                    val details = BeaconDetails(
                        beaconType = "AirPods",
                        trackingCategory = "Apple AirPods / Beats Audio",
                        extraDescription = "Proximity audio device broadcast"
                    )
                    return Pair("Apple AirPods / Audio", details)
                }

                // Apple AirDrop / Nearby Action (Type 0x05 or 0x10)
                0x05, 0x10 -> {
                    val details = BeaconDetails(
                        beaconType = "AirDrop",
                        trackingCategory = "Apple AirDrop / Nearby Action",
                        extraDescription = "Direct sharing / continuity broadcast"
                    )
                    return Pair("Apple AirDrop", details)
                }

                // Apple Continuity / Handoff (Type 0x0C or 0x0B)
                0x0B, 0x0C -> {
                    return Pair("Apple Continuity / Handoff", null)
                }

                else -> {
                    return Pair("Apple Accessory (0x%02X)".format(appleType), null)
                }
            }
        }

        // 2. Google / Android Ecosystem (0x00E0 or Google Fast Pair / Nearby Share)
        if (companyId == 0x00E0) {
            val isFastPair = serviceUuids16.contains(0xFE2C) || serviceUuids16.contains(0xFE9F)
            val isNearbyShare = serviceUuids16.contains(0xFEA0) || serviceUuids16.contains(0xFDF0)

            return when {
                isFastPair -> {
                    val details = BeaconDetails(
                        beaconType = "FastPair",
                        trackingCategory = "Google Fast Pair Accessory",
                        extraDescription = "Quick pairing service broadcast"
                    )
                    Pair("Google Fast Pair", details)
                }
                isNearbyShare -> {
                    val details = BeaconDetails(
                        beaconType = "NearbyShare",
                        trackingCategory = "Google Nearby Share / Quick Share",
                        extraDescription = "Local transfer discovery beacon"
                    )
                    Pair("Google Nearby Share / Quick Share", details)
                }
                else -> {
                    val details = BeaconDetails(
                        beaconType = "NearbyShare",
                        trackingCategory = "Google Ecosystem Broadcast",
                        extraDescription = "Google Android platform advertisement"
                    )
                    Pair("Google Ecosystem Beacon", details)
                }
            }
        }

        // 3. Samsung Ecosystem (0x0075)
        if (companyId == 0x0075) {
            // Check for SmartTag / SmartThings payload
            val isSmartTag = payloadLen >= 4
            val details = BeaconDetails(
                beaconType = "SmartTag",
                trackingCategory = "Samsung SmartThings / Galaxy SmartTag",
                extraDescription = "Samsung Find network location tag"
            )
            return Pair(if (isSmartTag) "Samsung Galaxy SmartTag" else "Samsung SmartThings", details)
        }

        // 4. AltBeacon Check (Beacon Code 0xBEAC at bytes 2..3)
        if (payloadLen >= 26 && payload[2] == 0xBE.toByte() && payload[3] == 0xAC.toByte()) {
            val uuidBytes = payload.copyOfRange(4, 20)
            val major = ((payload[20].toInt() and 0xFF) shl 8) or (payload[21].toInt() and 0xFF)
            val minor = ((payload[22].toInt() and 0xFF) shl 8) or (payload[23].toInt() and 0xFF)
            val power = payload[24].toInt()

            val details = BeaconDetails(
                beaconType = "AltBeacon",
                proximityUuid = formatUuid(uuidBytes),
                major = major,
                minor = minor,
                measuredPowerAt1m = power,
                trackingCategory = "Open AltBeacon Standard",
                extraDescription = "Major: $major, Minor: $minor, Ref: ${power}dBm"
            )
            return Pair("AltBeacon", details)
        }

        return Pair(null, null)
    }

    /**
     * Fallback beacon identification based purely on Service UUIDs.
     */
    private fun matchServiceUuidBeacon(serviceUuids16: List<Int>): Pair<String?, BeaconDetails?> {
        return when {
            serviceUuids16.contains(0xFD6F) -> {
                Pair(
                    "Apple Find My",
                    BeaconDetails(
                        beaconType = "FindMy",
                        trackingCategory = "Apple Offline Finding Network (0xFD6F)",
                        extraDescription = "Find My / Exposure Notification token"
                    )
                )
            }
            serviceUuids16.contains(0xFE2C) || serviceUuids16.contains(0xFE9F) -> {
                Pair(
                    "Google Fast Pair",
                    BeaconDetails(
                        beaconType = "FastPair",
                        trackingCategory = "Google Fast Pair Service (0xFE2C)",
                        extraDescription = "Google Fast Pair discovery token"
                    )
                )
            }
            serviceUuids16.contains(0xFEA0) -> {
                Pair(
                    "Google Nearby Share",
                    BeaconDetails(
                        beaconType = "NearbyShare",
                        trackingCategory = "Google Nearby Share (0xFEA0)",
                        extraDescription = "Quick Share discovery broadcast"
                    )
                )
            }
            serviceUuids16.contains(0xFEED) -> {
                Pair(
                    "Tile Tracker",
                    BeaconDetails(
                        beaconType = "Tile",
                        trackingCategory = "Tile Network Tag (0xFEED)",
                        extraDescription = "Tile Proximity Beacon"
                    )
                )
            }
            serviceUuids16.contains(0xFE33) -> {
                Pair(
                    "Chipolo Tracker",
                    BeaconDetails(
                        beaconType = "Chipolo",
                        trackingCategory = "Chipolo Finder (0xFE33)",
                        extraDescription = "Chipolo Spot Beacon"
                    )
                )
            }
            serviceUuids16.contains(0xFEAA) -> {
                Pair(
                    "Google Eddystone",
                    BeaconDetails(
                        beaconType = "Eddystone",
                        trackingCategory = "Eddystone Beacon Standard (0xFEAA)",
                        extraDescription = "Eddystone Open Beacon Frame"
                    )
                )
            }
            else -> Pair(null, null)
        }
    }

    private fun parseStringSafely(bytes: ByteArray): String {
        return try {
            String(bytes, Charsets.UTF_8).trim { it <= ' ' || it == '\u0000' }
        } catch (_: Exception) {
            try {
                String(bytes, Charsets.US_ASCII).trim { it <= ' ' || it == '\u0000' }
            } catch (_: Exception) {
                ""
            }
        }
    }

    private fun parse128BitUuid(bytes: ByteArray, offset: Int): String? {
        if (offset + 16 > bytes.size) return null
        return try {
            // BLE 128-bit UUIDs are transmitted in Little-Endian byte order
            val bb = ByteBuffer.wrap(bytes, offset, 16).order(ByteOrder.LITTLE_ENDIAN)
            val leastSig = bb.long
            val mostSig = bb.long
            UUID(mostSig, leastSig).toString().uppercase()
        } catch (_: Exception) {
            null
        }
    }

    private fun formatUuid(bytes: ByteArray): String {
        if (bytes.size != 16) return bytesToHex(bytes)
        return try {
            val bb = ByteBuffer.wrap(bytes)
            val mostSig = bb.long
            val leastSig = bb.long
            UUID(mostSig, leastSig).toString().uppercase()
        } catch (_: Exception) {
            bytesToHex(bytes)
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append("%02X".format(b.toInt() and 0xFF))
        }
        return sb.toString()
    }

    private fun getGapTypeName(type: Int): String {
        return when (type) {
            GAP_FLAGS -> "Flags"
            GAP_INCOMPLETE_16BIT_UUIDS -> "Incomplete 16-bit Service UUIDs"
            GAP_COMPLETE_16BIT_UUIDS -> "Complete 16-bit Service UUIDs"
            GAP_INCOMPLETE_32BIT_UUIDS -> "Incomplete 32-bit Service UUIDs"
            GAP_COMPLETE_32BIT_UUIDS -> "Complete 32-bit Service UUIDs"
            GAP_INCOMPLETE_128BIT_UUIDS -> "Incomplete 128-bit Service UUIDs"
            GAP_COMPLETE_128BIT_UUIDS -> "Complete 128-bit Service UUIDs"
            GAP_SHORTENED_LOCAL_NAME -> "Shortened Local Name"
            GAP_COMPLETE_LOCAL_NAME -> "Complete Local Name"
            GAP_TX_POWER_LEVEL -> "Tx Power Level"
            GAP_SERVICE_DATA_16BIT -> "Service Data (16-bit UUID)"
            GAP_APPEARANCE -> "Appearance"
            GAP_SERVICE_DATA_32BIT -> "Service Data (32-bit UUID)"
            GAP_SERVICE_DATA_128BIT -> "Service Data (128-bit UUID)"
            GAP_MANUFACTURER_SPECIFIC -> "Manufacturer Specific Data"
            else -> "GAP Type 0x%02X".format(type)
        }
    }
}
