package com.example

object OfflineCarrierDatabase {

    private val CARRIER_MAP = mapOf(
        "310-260" to "T-Mobile US 5G",
        "310-410" to "AT&T Mobility 5G",
        "311-480" to "Verizon Wireless 5G",
        "310-120" to "Sprint LTE / T-Mo",
        "313-100" to "FirstNet Public Safety",
        "302-220" to "Telus Mobility CA",
        "302-610" to "Bell Mobility CA",
        "302-720" to "Rogers Wireless CA",
        "234-15" to "Vodafone UK 5G",
        "234-10" to "O2 UK 5G",
        "234-30" to "EE Limited UK",
        "262-01" to "Telekom Deutschland",
        "262-02" to "Vodafone Germany",
        "262-03" to "O2 Telefónica DE",
        "208-01" to "Orange France 5G",
        "208-10" to "SFR France",
        "440-10" to "NTT Docomo JP",
        "440-20" to "SoftBank JP",
        "505-01" to "Telstra Australia",
        "505-02" to "Optus Mobile AU"
    )

    fun resolveCarrier(mcc: String?, mnc: String?): String {
        if (mcc.isNull_or_blank() || mnc.isNull_or_blank()) return "Private / Local Cell"
        val key = "$mcc-$mnc"
        return CARRIER_MAP[key] ?: "Cellular Tower ($mcc-$mnc)"
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty() || this == "null"
    }
}
