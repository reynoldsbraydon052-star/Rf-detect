package com.example

object MacOuidResolver {
    // Dictionary of known MAC prefixes (first 3 octets) to vendor names
    private val localOuiMap = mapOf(
        "00:08:E2" to "Cisco Systems",
        "00:24:E4" to "Apple, Inc.",
        "F4:F2:6D" to "Apple, Inc.",
        "24:0A:C4" to "Espressif Systems",
        "A4:C1:38" to "Espressif Systems",
        "00:1A:E8" to "Hangzhou Hikvision",
        "00:12:34" to "Dahua Technology",
        "00:25:D3" to "Texas Instruments",
        "F4:CB:52" to "Texas Instruments",
        "00:14:22" to "Dell Inc.",
        "00:1A:11" to "Google LLC",
        "3C:5E:C3" to "Microsoft Corporation",
        "00:0F:60" to "Sony Corporation",
        "00:00:F0" to "Samsung Electronics"
    )

    /**
     * Resolves the vendor name for a given MAC address string (e.g. "00:08:E2:17:80:CC").
     * Checks local prefix dictionary and returns the vendor name or null.
     */
    fun resolveVendor(mac: String?): String? {
        if (mac == null || mac.length < 8) return null
        val prefix = mac.take(8).uppercase()
        return localOuiMap[prefix]
    }

    /**
     * Detects if the MAC is randomized (locally administered address or private address).
     * This checks the local/universal administration bit (bit 1 of the first octet, 0-indexed).
     */
    fun isRandomized(mac: String?): Boolean {
        if (mac == null) return false
        val cleanMac = mac.replace(":", "").replace("-", "")
        if (cleanMac.length < 2) return false
        
        return try {
            val firstByte = cleanMac.take(2).toInt(16)
            // Bit 1 (value 0x02) of the first byte is the Locally Administered Address (LAA) bit.
            // If it is 1, the address is locally administered (randomized / private).
            val isLocal = (firstByte and 0x02) != 0
            
            // BLE Private Addresses:
            // Top two bits of first byte of the address:
            // 01 -> Resolvable Private Address
            // 00 -> Non-resolvable Private Address
            // 11 -> Static Random Address
            val topTwoBits = (firstByte and 0xC0)
            val isBlePrivate = topTwoBits == 0x40 || topTwoBits == 0x00 || topTwoBits == 0xC0
            
            isLocal || isBlePrivate
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Specifically checks if the MAC address is a Resolvable Private Address (RPA).
     * In BLE, an RPA has its two most significant bits set to 0 and 1 respectively (binary 01xxxxxx).
     */
    fun isResolvablePrivateAddress(mac: String?): Boolean {
        if (mac == null) return false
        val cleanMac = mac.replace(":", "").replace("-", "")
        if (cleanMac.length < 2) return false
        return try {
            val firstByte = cleanMac.take(2).toInt(16)
            (firstByte and 0xC0) == 0x40
        } catch (e: Exception) {
            false
        }
    }
}
