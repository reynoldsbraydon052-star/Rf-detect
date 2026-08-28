package com.example

import org.junit.Assert.*
import org.junit.Test

class BlePayloadDecoderTest {

    @Test
    fun testExtractCompleteLocalName() {
        // [Length=0x0B, Type=0x09 (Complete Local Name), "PulseWatch"]
        val nameBytes = "PulseWatch".toByteArray(Charsets.UTF_8)
        val payload = byteArrayOf((nameBytes.size + 1).toByte(), 0x09.toByte()) + nameBytes

        val profile = BlePayloadDecoder.decode(payload)
        assertEquals("PulseWatch", profile.localName)
        assertFalse(profile.isShortenedName)
    }

    @Test
    fun testExtractShortenedLocalName() {
        // [Length=0x06, Type=0x08 (Shortened Local Name), "PW-01"]
        val nameBytes = "PW-01".toByteArray(Charsets.UTF_8)
        val payload = byteArrayOf((nameBytes.size + 1).toByte(), 0x08.toByte()) + nameBytes

        val profile = BlePayloadDecoder.decode(payload)
        assertEquals("PW-01", profile.localName)
        assertTrue(profile.isShortenedName)
    }

    @Test
    fun testAppearanceMapping() {
        // Test Phone (0x0040)
        val phoneInfo = SigAssignedNumbersDictionary.resolveAppearance(0x0040)
        assertEquals("Phone", phoneInfo.categoryName)
        assertEquals("Phone", phoneInfo.displayName)

        // Test Computer (0x0080)
        val compInfo = SigAssignedNumbersDictionary.resolveAppearance(0x0080)
        assertEquals("Computer", compInfo.categoryName)

        // Test Remote Control / Display (0x0180)
        val dispInfo = SigAssignedNumbersDictionary.resolveAppearance(0x0180)
        assertTrue(dispInfo.displayName.contains("Display") || dispInfo.displayName.contains("Remote"))

        // Test Smart Watch (0x0340)
        val watchInfo = SigAssignedNumbersDictionary.resolveAppearance(0x0340)
        assertTrue(watchInfo.displayName.contains("Smart Watch") || watchInfo.categoryName.contains("Watch") || watchInfo.categoryName.contains("Heart Rate"))

        // Test Keyboard / HID (0x0380)
        val kbInfo = SigAssignedNumbersDictionary.resolveAppearance(0x0380)
        assertTrue(kbInfo.displayName.contains("Keyboard") || kbInfo.categoryName.contains("Keyboard") || kbInfo.categoryName.contains("Human Interface"))

        // Payload decoding of Appearance (0x19 GAP type)
        // 0x0380 (Keyboard) in Little Endian: 0x80, 0x03
        val appearancePayload = byteArrayOf(0x03, 0x19, 0x80.toByte(), 0x03.toByte())
        val decoded = BlePayloadDecoder.decode(appearancePayload)
        assertEquals(0x0380, decoded.appearanceHex)
        assertNotNull(decoded.appearanceDisplayName)
        assertTrue(decoded.appearanceDisplayName!!.contains("Keyboard") || decoded.appearanceCategory!!.contains("Keyboard") || decoded.appearanceCategory!!.contains("Human Interface"))
    }

    @Test
    fun testServiceUuidExtractionAndCapabilities() {
        // GAP Type 0x03 (Complete 16-bit UUIDs):
        // 0x1812 (HID), 0x180F (Battery Service), 0x180A (Device Information), 0x1818 (Cycling Power)
        // Little Endian:
        // 0x1812 -> 0x12, 0x18
        // 0x180F -> 0x0F, 0x18
        // 0x180A -> 0x0A, 0x18
        // 0x1818 -> 0x18, 0x18
        val payload = byteArrayOf(
            0x09, 0x03,
            0x12, 0x18,
            0x0F, 0x18,
            0x0A, 0x18,
            0x18, 0x18
        )

        val profile = BlePayloadDecoder.decode(payload)
        assertEquals(4, profile.serviceUuids16.size)
        assertTrue(profile.serviceUuids16.contains(0x1812))
        assertTrue(profile.serviceUuids16.contains(0x180F))
        assertTrue(profile.serviceUuids16.contains(0x180A))
        assertTrue(profile.serviceUuids16.contains(0x1818))

        assertTrue(profile.serviceCapabilities.any { it.contains("Human Interface") })
        assertTrue(profile.serviceCapabilities.any { it.contains("Battery") })
        assertTrue(profile.serviceCapabilities.any { it.contains("Device Information") })
        assertTrue(profile.serviceCapabilities.any { it.contains("Cycling Power") })
    }

    @Test
    fun testAppleIBeaconIdentification() {
        // Apple iBeacon layout in Manufacturer Specific Data (0xFF):
        // Length = 26 (0x1A)
        // Type = 0xFF
        // Company ID = 0x004C (Apple) -> 0x4C, 0x00
        // iBeacon Type = 0x02
        // iBeacon Length = 0x15 (21 bytes)
        // Proximity UUID (16 bytes): E2 C5 6D B5 DF FB 48 D2 B0 60 D0 F5 A7 10 96 E0
        // Major (2 bytes): 0x0001 -> 0x00, 0x01
        // Minor (2 bytes): 0x0002 -> 0x00, 0x02
        // Measured Power at 1m (1 byte): 0xC5 (-59 dBm)
        val iBeaconBytes = byteArrayOf(
            0x1A, 0xFF.toByte(),
            0x4C, 0x00, // Apple Company ID
            0x02, 0x15, // iBeacon marker
            0xE2.toByte(), 0xC5.toByte(), 0x6D, 0xB5.toByte(),
            0xDF.toByte(), 0xFB.toByte(), 0x48, 0xD2.toByte(),
            0xB0.toByte(), 0x60, 0xD0.toByte(), 0xF5.toByte(),
            0xA7.toByte(), 0x10, 0x96.toByte(), 0xE0.toByte(),
            0x00, 0x01, // Major = 1
            0x00, 0x02, // Minor = 2
            0xC5.toByte() // Power = -59
        )

        val profile = BlePayloadDecoder.decode(iBeaconBytes)
        assertEquals(0x004C, profile.manufacturerId)
        assertEquals("Apple, Inc.", profile.manufacturerName)
        assertEquals("Apple iBeacon", profile.proprietaryEcosystemType)
        assertNotNull(profile.beaconDetails)
        assertEquals("iBeacon", profile.beaconDetails?.beaconType)
        assertEquals(1, profile.beaconDetails?.major)
        assertEquals(2, profile.beaconDetails?.minor)
        assertEquals(-59, profile.beaconDetails?.measuredPowerAt1m)
    }

    @Test
    fun testAppleAirTagFindMyIdentification() {
        // Apple AirTag FindMy advertisement: Company 0x004C, Type 0x12
        val airTagPayload = byteArrayOf(
            0x1B, 0xFF.toByte(),
            0x4C, 0x00, // Apple
            0x12, 0x19, // Type 0x12 (Find My)
            0x10, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16
        )

        val profile = BlePayloadDecoder.decode(airTagPayload)
        assertEquals("Apple AirTag / Find My", profile.proprietaryEcosystemType)
        assertEquals("FindMy", profile.beaconDetails?.beaconType)
    }

    @Test
    fun testGoogleFastPairAndNearbyShare() {
        // Google Fast Pair (Service UUID 0xFE2C in 0x03)
        val fastPairPayload = byteArrayOf(
            0x03, 0x03,
            0x2C, 0xFE.toByte() // 0xFE2C in Little Endian
        )
        val profileFastPair = BlePayloadDecoder.decode(fastPairPayload)
        assertEquals("Google Fast Pair", profileFastPair.proprietaryEcosystemType)

        // Google Nearby Share (Service UUID 0xFEA0 in 0x03)
        val nearbySharePayload = byteArrayOf(
            0x03, 0x03,
            0xA0.toByte(), 0xFE.toByte() // 0xFEA0
        )
        val profileNearby = BlePayloadDecoder.decode(nearbySharePayload)
        assertEquals("Google Nearby Share", profileNearby.proprietaryEcosystemType)
    }

    @Test
    fun testSamsungSmartTagIdentification() {
        // Samsung (0x0075) Manufacturer Specific Data
        val samsungPayload = byteArrayOf(
            0x06, 0xFF.toByte(),
            0x75, 0x00, // Samsung Company ID
            0x01, 0x00, 0x02
        )
        val profile = BlePayloadDecoder.decode(samsungPayload)
        assertEquals("Samsung Electronics Co. Ltd.", profile.manufacturerName)
        assertEquals("Samsung Galaxy SmartTag", profile.proprietaryEcosystemType)
    }

    @Test
    fun testMalformedAndEdgeCasePayloads() {
        // 1. Null payload
        val profileNull = BlePayloadDecoder.decode(null)
        assertNull(profileNull.localName)
        assertTrue(profileNull.serviceUuids16.isEmpty())

        // 2. Empty byte array
        val profileEmpty = BlePayloadDecoder.decode(byteArrayOf())
        assertNull(profileEmpty.localName)

        // 3. All zeros (should safely terminate without exception)
        val profileZeros = BlePayloadDecoder.decode(ByteArray(31))
        assertNull(profileZeros.localName)

        // 4. Truncated length (Length says 20 bytes, but array only has 3 bytes)
        val profileTruncated = BlePayloadDecoder.decode(byteArrayOf(0x14, 0x09, 'A'.code.toByte()))
        assertEquals("A", profileTruncated.localName)

        // 5. Corrupt TLV chain (length byte claims 0xFF)
        val profileCorrupt = BlePayloadDecoder.decode(byteArrayOf(0xFF.toByte(), 0x09, 0x01, 0x02))
        assertNotNull(profileCorrupt)

        // 6. Negative byte values throughout
        val profileNegative = BlePayloadDecoder.decode(byteArrayOf(-1, -2, -3, -4, -5))
        assertNotNull(profileNegative)
    }

    @Test
    fun testHardwareIdentificationManualStatePriority() {
        // 1. Profile with Local Name prioritizes Local Name over MAC and OUI
        val profileWithName = SemanticDeviceProfile(
            localName = "Smart Tracker Plus",
            proprietaryEcosystemType = "Apple iBeacon",
            appearanceDisplayName = "Keyring / Key Fob"
        )
        val state1 = HardwareIdentificationManualState.fromProfile(
            macAddress = "AA:BB:CC:DD:EE:FF",
            ouiVendor = "Acme Vendor",
            profile = profileWithName
        )
        assertEquals("Smart Tracker Plus", state1.resolvedDisplayName)
        assertTrue(state1.isNameFromPayload)

        // 2. Profile without Local Name but with Ecosystem Type prioritizes Ecosystem Type
        val profileWithEcosystem = SemanticDeviceProfile(
            proprietaryEcosystemType = "Apple AirTag / Find My",
            appearanceDisplayName = "Generic Tag"
        )
        val state2 = HardwareIdentificationManualState.fromProfile(
            macAddress = "AA:BB:CC:DD:EE:FF",
            ouiVendor = "Apple, Inc.",
            profile = profileWithEcosystem
        )
        assertEquals("Apple AirTag / Find My", state2.resolvedDisplayName)
        assertTrue(state2.isEcosystemTracker)

        // 3. Profile without Local Name or Ecosystem, but with Appearance prioritizes Appearance
        val profileWithApp = SemanticDeviceProfile(
            appearanceDisplayName = "Smartwatch",
            appearanceCategory = "Watch"
        )
        val state3 = HardwareIdentificationManualState.fromProfile(
            macAddress = "AA:BB:CC:DD:EE:FF",
            ouiVendor = "Samsung",
            profile = profileWithApp
        )
        assertEquals("Smartwatch", state3.resolvedDisplayName)
        assertTrue(state3.isAppearanceFromPayload)

        // 4. Clean fallback to OUI Vendor when payload has no name or appearance
        val state4 = HardwareIdentificationManualState.fromProfile(
            macAddress = "AA:BB:CC:DD:EE:FF",
            ouiVendor = "Nordic Semiconductor",
            profile = null
        )
        assertEquals("Nordic Semiconductor Device", state4.resolvedDisplayName)

        // 5. Clean fallback to masked / MAC when vendor is null
        val state5 = HardwareIdentificationManualState.fromProfile(
            macAddress = "AA:BB:CC:11:22:33",
            ouiVendor = null,
            profile = null
        )
        assertEquals("Target [11:22:33]", state5.resolvedDisplayName)
    }
}
