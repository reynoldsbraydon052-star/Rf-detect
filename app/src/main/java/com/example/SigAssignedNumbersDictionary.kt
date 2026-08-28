package com.example

data class AppearanceInfo(
    val categoryHex: Int,
    val categoryName: String,
    val subcategoryHex: Int,
    val displayName: String
)

/**
 * Offline Bluetooth SIG Assigned Numbers Dictionary.
 * Maps 16-bit Appearance values, 16-bit Service UUIDs, and Company Identifiers
 * to human-readable semantic descriptions completely offline without external APIs.
 */
object SigAssignedNumbersDictionary {

    // ==========================================
    // 1. Bluetooth SIG Company Identifiers (16-bit)
    // ==========================================
    private val COMPANY_IDENTIFIERS = mapOf(
        0x004C to "Apple, Inc.",
        0x00E0 to "Google LLC",
        0x0075 to "Samsung Electronics Co. Ltd.",
        0x0006 to "Microsoft",
        0x000A to "Qualcomm",
        0x0059 to "Nordic Semiconductor ASA",
        0x0087 to "Garmin International, Inc.",
        0x0157 to "Anhui Huami Information Technology (Amazfit/Xiaomi)",
        0x00D2 to "Dialog Semiconductor B.V.",
        0x02AC to "Amazon.com Services LLC",
        0x038F to "Xiaomi Inc.",
        0x0171 to "Amazon Fulfillment Services, Inc.",
        0x000F to "Broadcom Corporation",
        0x009E to "Bose Corporation",
        0x002D to "Sony Corporation",
        0x000D to "Texas Instruments Inc.",
        0x0047 to "LG Electronics, Inc.",
        0x0048 to "Denso Corporation",
        0x0001 to "Nokia Mobile Phones",
        0x0002 to "Intel Corp.",
        0x0003 to "IBM Corp.",
        0x0004 to "Toshiba Corp.",
        0x006B to "Polar Electro Oy",
        0x0078 to "Nike, Inc.",
        0x008A to "Jawbone",
        0x00B5 to "Swirl Networks, Inc.",
        0x00C7 to "Radius Networks, Inc.",
        0x00FF to "Binauric SE",
        0x0131 to "Cisco Systems, Inc.",
        0x0136 to "Beats Electronics",
        0x0167 to "Fitbit, Inc.",
        0x019A to "Tile, Inc.",
        0x02DE to "Oura Health Oy",
        0x0399 to "Whoop, Inc.",
        0x0499 to "Ruuvi Innovations Ltd.",
        0x0590 to "Raspberry Pi Trading Ltd",
        0x0618 to "Espressif Systems (Shanghai) Co., Ltd."
    )

    fun resolveCompanyId(companyId: Int): String? {
        return COMPANY_IDENTIFIERS[companyId]
    }

    // ==========================================
    // 2. Bluetooth SIG Standard Service UUIDs (16-bit)
    // ==========================================
    private val SERVICE_UUIDS_16 = mapOf(
        0x1800 to "Generic Access",
        0x1801 to "Generic Attribute",
        0x1802 to "Immediate Alert",
        0x1803 to "Link Loss",
        0x1804 to "Tx Power",
        0x1805 to "Current Time Service",
        0x1806 to "Reference Time Update",
        0x1807 to "Next DST Change",
        0x1808 to "Glucose",
        0x1809 to "Health Thermometer",
        0x180A to "Device Information",
        0x180D to "Heart Rate",
        0x180E to "Phone Alert Status",
        0x180F to "Battery Service",
        0x1810 to "Blood Pressure",
        0x1811 to "Alert Notification",
        0x1812 to "Human Interface Device",
        0x1813 to "Scan Parameters",
        0x1814 to "Running Speed and Cadence",
        0x1815 to "Automation IO",
        0x1816 to "Cycling Speed and Cadence",
        0x1818 to "Cycling Power",
        0x1819 to "Location and Navigation",
        0x181A to "Environmental Sensing",
        0x181B to "Body Composition",
        0x181C to "User Data",
        0x181D to "Weight Scale",
        0x181E to "Bond Management",
        0x181F to "Continuous Glucose Monitoring",
        0x1820 to "Internet Protocol Support",
        0x1821 to "Indoor Positioning",
        0x1822 to "Pulse Oximeter",
        0x1823 to "HTTP Proxy",
        0x1824 to "Transport Discovery",
        0x1825 to "Object Transfer",
        0x1826 to "Fitness Machine",
        0x1827 to "Mesh Provisioning Service",
        0x1828 to "Mesh Proxy Service",
        0x1829 to "Reconnection Configuration",
        0x183A to "Insulin Delivery",
        0x183B to "Binary Sensor",
        0x183C to "Emergency Configuration",
        0x183E to "Physical Activity Monitor",
        0x1843 to "Audio Input Control",
        0x1844 to "Volume Control",
        0x184E to "Audio Stream Control",
        0x184F to "Broadcast Audio Scan",
        0x1850 to "Published Audio Capabilities",
        0x1853 to "Common Audio",
        0x1855 to "Coordination Set Identification",
        0x1856 to "Microphone Control",
        // Well-known 16-bit Member/Proprietary UUIDs:
        0xFD6F to "Apple Find My / Exposure Notification",
        0xFE2C to "Google Fast Pair / Find My Device",
        0xFEED to "Tile Tracker Service",
        0xFE33 to "Chipolo Tracker Service",
        0xFE9F to "Google Fast Pair Service",
        0xFEA0 to "Google Nearby Share",
        0xFD69 to "Samsung SmartThings",
        0xFDF0 to "Google Nearby Framework",
        0xFEAA to "Google Eddystone Beacon"
    )

    fun resolveServiceUuid16(uuid16: Int): String? {
        return SERVICE_UUIDS_16[uuid16]
    }

    // ==========================================
    // 3. Bluetooth SIG Appearance Values (16-bit)
    // ==========================================
    // Category = bits 15..6 (appearance >> 6)
    // Subcategory = bits 5..0 (appearance & 0x3F)

    private val EXACT_APPEARANCES = mapOf(
        0x0000 to AppearanceInfo(0x00, "Unknown", 0x00, "Unknown Appearance"),
        0x0040 to AppearanceInfo(0x01, "Phone", 0x00, "Phone"),
        0x0041 to AppearanceInfo(0x01, "Phone", 0x01, "Cellular Phone"),
        0x0042 to AppearanceInfo(0x01, "Phone", 0x02, "Cordless Phone"),
        0x0043 to AppearanceInfo(0x01, "Phone", 0x03, "Smartphone"),
        0x0080 to AppearanceInfo(0x02, "Computer", 0x00, "Computer"),
        0x0081 to AppearanceInfo(0x02, "Computer", 0x01, "Desktop Workstation"),
        0x0082 to AppearanceInfo(0x02, "Computer", 0x02, "Server"),
        0x0083 to AppearanceInfo(0x02, "Computer", 0x03, "Laptop"),
        0x0084 to AppearanceInfo(0x02, "Computer", 0x04, "Handheld PC/PDA"),
        0x0085 to AppearanceInfo(0x02, "Computer", 0x05, "Palm-size PC/PDA"),
        0x0086 to AppearanceInfo(0x02, "Computer", 0x06, "Wearable Computer"),
        0x0087 to AppearanceInfo(0x02, "Computer", 0x07, "Tablet"),
        0x00C0 to AppearanceInfo(0x03, "Watch", 0x00, "Watch"),
        0x00C1 to AppearanceInfo(0x03, "Watch", 0x01, "Sports Watch"),
        0x00C2 to AppearanceInfo(0x03, "Watch", 0x02, "Smartwatch"),
        0x0100 to AppearanceInfo(0x04, "Clock", 0x00, "Clock"),
        0x0140 to AppearanceInfo(0x05, "Display", 0x00, "Display"),
        0x0180 to AppearanceInfo(0x06, "Remote Control", 0x00, "Remote Control / Display"),
        0x01C0 to AppearanceInfo(0x07, "Eye-glasses", 0x00, "Eye-glasses / AR Glasses"),
        0x0200 to AppearanceInfo(0x08, "Tag", 0x00, "Generic Tag"),
        0x0240 to AppearanceInfo(0x09, "Keyring", 0x00, "Keyring / Key Fob"),
        0x0280 to AppearanceInfo(0x0A, "Media Player", 0x00, "Media Player"),
        0x02C0 to AppearanceInfo(0x0B, "Barcode Scanner", 0x00, "Barcode Scanner"),
        0x0300 to AppearanceInfo(0x0C, "Thermometer", 0x00, "Thermometer"),
        0x0340 to AppearanceInfo(0x0D, "Smart Watch", 0x00, "Smart Watch / Heart Rate Sensor"),
        0x0341 to AppearanceInfo(0x0D, "Heart Rate", 0x01, "Heart Rate Belt"),
        0x0380 to AppearanceInfo(0x0F, "Keyboard", 0x00, "Keyboard / HID Device"),
        0x0381 to AppearanceInfo(0x0F, "Keyboard", 0x01, "Keyboard"),
        0x0382 to AppearanceInfo(0x0F, "Mouse", 0x02, "Mouse"),
        0x0383 to AppearanceInfo(0x0F, "Joystick", 0x03, "Joystick"),
        0x0384 to AppearanceInfo(0x0F, "Gamepad", 0x04, "Gamepad"),
        0x0385 to AppearanceInfo(0x0F, "Digitizer Tablet", 0x05, "Digitizer Tablet"),
        0x0386 to AppearanceInfo(0x0F, "Card Reader", 0x06, "Card Reader"),
        0x0387 to AppearanceInfo(0x0F, "Digital Pen", 0x07, "Digital Pen"),
        0x0388 to AppearanceInfo(0x0F, "Barcode Scanner", 0x08, "Barcode Scanner"),
        0x03C0 to AppearanceInfo(0x10, "Glucose Meter", 0x00, "Glucose Meter"),
        0x0400 to AppearanceInfo(0x11, "Running Walking Sensor", 0x00, "Running Walking Sensor"),
        0x0440 to AppearanceInfo(0x12, "Cycling", 0x00, "Cycling"),
        0x0441 to AppearanceInfo(0x12, "Cycling", 0x01, "Cycling Computer"),
        0x0442 to AppearanceInfo(0x12, "Cycling", 0x02, "Cycling Speed Sensor"),
        0x0443 to AppearanceInfo(0x12, "Cycling", 0x03, "Cycling Cadence Sensor"),
        0x0444 to AppearanceInfo(0x12, "Cycling", 0x04, "Cycling Power Sensor"),
        0x0445 to AppearanceInfo(0x12, "Cycling", 0x05, "Cycling Speed and Cadence Sensor"),
        0x0840 to AppearanceInfo(0x21, "Audio / Headset", 0x00, "Audio / Headset"),
        0x0841 to AppearanceInfo(0x21, "Headphones", 0x01, "Headphones"),
        0x0842 to AppearanceInfo(0x21, "Earbuds", 0x02, "Earbuds / In-Ear Headset"),
        0x0843 to AppearanceInfo(0x21, "Speaker", 0x03, "Speaker"),
        0x0844 to AppearanceInfo(0x21, "Soundbar", 0x04, "Soundbar"),
        0x0845 to AppearanceInfo(0x21, "Voice Assistant", 0x05, "Voice Assistant / Smart Speaker")
    )

    private val CATEGORY_NAMES = mapOf(
        0x00 to "Unknown",
        0x01 to "Phone",
        0x02 to "Computer",
        0x03 to "Watch",
        0x04 to "Clock",
        0x05 to "Display",
        0x06 to "Remote Control",
        0x07 to "Eye-glasses",
        0x08 to "Tag",
        0x09 to "Keyring",
        0x0A to "Media Player",
        0x0B to "Barcode Scanner",
        0x0C to "Thermometer",
        0x0D to "Heart Rate Sensor",
        0x0E to "Blood Pressure",
        0x0F to "Human Interface Device",
        0x10 to "Glucose Meter",
        0x11 to "Running Walking Sensor",
        0x12 to "Cycling Sensor",
        0x13 to "Control Device",
        0x14 to "Network Device",
        0x15 to "Sensor",
        0x16 to "Light Fixtures",
        0x17 to "Fan",
        0x18 to "HVAC",
        0x19 to "Air Conditioning",
        0x1A to "Humidifier",
        0x1B to "Heating",
        0x1C to "Access Control",
        0x1D to "Motorized Device",
        0x1E to "Power Device",
        0x1F to "Light Source",
        0x20 to "Home Appliance",
        0x21 to "Audio / Headset"
    )

    /**
     * Resolves a 16-bit Appearance integer into a structured AppearanceInfo.
     */
    fun resolveAppearance(appearanceHex: Int): AppearanceInfo {
        EXACT_APPEARANCES[appearanceHex]?.let { return it }

        val category = (appearanceHex shr 6) and 0x3FF
        val subcategory = appearanceHex and 0x3F
        val categoryName = CATEGORY_NAMES[category] ?: "Generic (Category 0x%02X)".format(category)
        val displayName = if (subcategory == 0) categoryName else "$categoryName (Subtype 0x%02X)".format(subcategory)

        return AppearanceInfo(
            categoryHex = category,
            categoryName = categoryName,
            subcategoryHex = subcategory,
            displayName = displayName
        )
    }
}
