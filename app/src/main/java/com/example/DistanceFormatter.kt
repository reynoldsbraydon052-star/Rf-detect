package com.example

import java.util.Locale

fun Float.toFeet(): Float = this * 3.28084f
fun Double.toFeet(): Double = this * 3.28084

fun Float.toFeetString(decimals: Int = 1): String {
    val feet = this.toFeet()
    return "%.${decimals}f ft".format(Locale.US, feet)
}

fun Double.toFeetString(decimals: Int = 1): String {
    val feet = this.toFeet()
    return "%.${decimals}f ft".format(Locale.US, feet)
}

fun Float.toCompactFeetString(): String {
    return "%.0fft".format(Locale.US, this.toFeet())
}

fun Double.toCompactFeetString(): String {
    return "%.0fft".format(Locale.US, this.toFeet())
}

fun Float.toFeetLabel(decimals: Int = 1): String {
    return "%.${decimals}fft".format(Locale.US, this.toFeet())
}

fun Double.toFeetLabel(decimals: Int = 1): String {
    return "%.${decimals}fft".format(Locale.US, this.toFeet())
}
