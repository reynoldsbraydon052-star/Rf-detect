package com.example

import android.os.ParcelUuid
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class DeviceSignature(
    val hardwareFingerprintHash: String, // SHA-256 hash of invariant payload traits
    val primaryAssignedId: String,       // Human-readable pseudo ID (e.g., "SIG-B3A9")
    val knownMacAliases: Set<String>,    // All MACs observed carrying this fingerprint
    val manufacturer: String,            // Inferred manufacturer
    val firstSeenTimestamp: Long,
    val lastSeenTimestamp: Long
)

object FingerprintExtractor {

    /**
     * Hashes invariant BLE manufacturer metadata and service UUID arrays to produce
     * an invariant signature, ignoring rotating packet sequences or counter bytes.
     */
    fun extractBleFingerprint(
        manufacturerData: ByteArray?,
        serviceUuids: List<ParcelUuid>?
    ): String? {
        if (manufacturerData == null && serviceUuids.isNullOrEmpty()) return null
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            
            // Ingest company IDs / Static leading manufacturer payload chunks
            manufacturerData?.let {
                // Read up to the first 4 bytes which traditionally map company ID and device type
                val limit = minOf(it.size, 4)
                digest.update(it, 0, limit)
            }

            // Ingest ordered string representations of service UUIDs
            serviceUuids?.map { it.toString() }?.sorted()?.forEach { uuidStr ->
                digest.update(uuidStr.toByteArray(Charsets.UTF_8))
            }

            bytesToHex(digest.digest())
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses and hashes ordered 802.11 Information Element IDs and capability masks
     * from raw frames, filtering out dynamic fields (like SSID or timestamps).
     */
    fun extractWifiIeFingerprint(ieByteArray: ByteArray?): String? {
        if (ieByteArray == null || ieByteArray.isEmpty()) return null
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            
            // Extract and sort element IDs. The ordering of these elements is highly
            // distinctive of specific physical hardware, vendors, and device firmware.
            var index = 0
            val elementIds = mutableListOf<Byte>()
            while (index < ieByteArray.size) {
                val elementId = ieByteArray[index]
                elementIds.add(elementId)
                
                // Element Format: [ID (1 Byte)] [Length (1 Byte)] [Value (Length Bytes)]
                if (index + 1 < ieByteArray.size) {
                    val length = ieByteArray[index + 1].toInt() and 0xFF
                    index += 2 + length
                } else {
                    break
                }
            }

            // Sort and hash to ensure stable lookup matches
            elementIds.sorted().forEach { id ->
                digest.update(id)
            }

            bytesToHex(digest.digest())
        } catch (e: Exception) {
            null
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Thread-safe engine that correlates multiple randomized MAC addresses to their underlying
 * invariant hardware profile signatures.
 */
class DeviceFingerprintRegistry {
    private val signatures = ConcurrentHashMap<String, DeviceSignature>()

    fun ingestSignal(
        macAddress: String,
        rawFingerprintHash: String?,
        inferredManufacturer: String?
    ): DeviceSignature {
        val now = System.currentTimeMillis()
        
        // Failsafe fallback: If no invariant fingerprint is present, we must generate one
        // locked uniquely to this single MAC address.
        val hash = rawFingerprintHash ?: run {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(macAddress.toByteArray(Charsets.UTF_8))
            bytesToHex(digest.digest())
        }

        val existingSig = signatures[hash]
        return if (existingSig != null) {
            // Found a signature match! Append the new MAC alias and update timestamps
            val updatedSig = existingSig.copy(
                knownMacAliases = existingSig.knownMacAliases + macAddress,
                lastSeenTimestamp = now
            )
            signatures[hash] = updatedSig
            updatedSig
        } else {
            // Generate a fresh pseudo assigned ID for this fingerprint (e.g., "SIG-B3A9")
            val generatedId = "SIG-" + hash.take(4).uppercase()
            val newSig = DeviceSignature(
                hardwareFingerprintHash = hash,
                primaryAssignedId = generatedId,
                knownMacAliases = setOf(macAddress),
                manufacturer = inferredManufacturer ?: "Unknown",
                firstSeenTimestamp = now,
                lastSeenTimestamp = now
            )
            signatures[hash] = newSig
            newSig
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
