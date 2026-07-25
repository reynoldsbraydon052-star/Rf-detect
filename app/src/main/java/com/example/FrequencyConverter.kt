package com.example

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

object FrequencyConverter {

    fun earfcnToMhz(earfcn: Int): Double {
        return when {
            earfcn in 0..599 -> 2110.0 + (earfcn - 0) * 0.1 // Band 1
            earfcn in 600..1199 -> 1930.0 + (earfcn - 600) * 0.1 // Band 2
            earfcn in 1200..1949 -> 1805.0 + (earfcn - 1200) * 0.1 // Band 3
            earfcn in 1950..2399 -> 2110.0 + (earfcn - 1950) * 0.1 // Band 4
            earfcn in 2400..2649 -> 869.0 + (earfcn - 2400) * 0.1 // Band 5
            earfcn in 2750..3449 -> 2620.0 + (earfcn - 2750) * 0.1 // Band 7
            earfcn in 3450..3799 -> 925.0 + (earfcn - 3450) * 0.1 // Band 8
            earfcn in 36000..36199 -> 1900.0 + (earfcn - 36000) * 0.1 // Band 33
            earfcn in 37750..38249 -> 2570.0 + (earfcn - 37750) * 0.1 // Band 38
            earfcn in 38650..39649 -> 2300.0 + (earfcn - 38650) * 0.1 // Band 40
            earfcn in 39650..41589 -> 2496.0 + (earfcn - 39650) * 0.1 // Band 41
            else -> 1800.0 // Default 1.8 GHz LTE
        }
    }

    /**
     * Log-Distance Path Loss Model:
     * FSPL (dB) = 20 * log10(d) + 20 * log10(f_MHz) - 27.55
     * Distance d (meters) = 10 ^ ((27.55 - 20 * log10(f_MHz) + |RSSI|) / 20.0)
     */
    fun calculateDistance(rssi: Int, frequencyMhz: Double): Float {
        if (rssi >= 0 || frequencyMhz <= 0) return 1.0f
        val exp = (27.55 - (20.0 * log10(frequencyMhz)) + abs(rssi.toDouble())) / 20.0
        val dist = 10.0.pow(exp).toFloat()
        return dist.coerceIn(0.2f, 150.0f)
    }

    fun wifiChannelToMhz(channel: Int): Double {
        return when {
            channel in 1..14 -> 2412.0 + (channel - 1) * 5.0
            channel in 32..177 -> 5000.0 + channel * 5.0
            channel in 180..233 -> 5950.0 + (channel - 180) * 5.0 // Wi-Fi 6E/7 (6 GHz)
            else -> 2412.0
        }
    }

    fun getBandLabel(freqMhz: Double): String {
        return when {
            freqMhz in 2400.0..2500.0 -> "2.4 GHz Wi-Fi / BLE"
            freqMhz in 4900.0..5899.0 -> "5.0 GHz Wi-Fi"
            freqMhz in 5900.0..7200.0 -> "6.0/7.0 GHz Wi-Fi 7"
            freqMhz in 600.0..960.0 -> "Low-Band Cell (sub-1GHz)"
            freqMhz in 1700.0..2700.0 -> "Mid-Band LTE / 5G NR"
            freqMhz in 3300.0..4200.0 -> "C-Band 5G NR"
            else -> "${freqMhz.toInt()} MHz RF"
        }
    }
}
