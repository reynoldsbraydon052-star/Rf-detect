package com.example

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Secondary Digital Signal Processing (DSP) Layer.
 * Attenuates 50Hz and 60Hz AC power line electromagnetic hum from raw 3-axis magnetometer readings,
 * producing a clean baseline for precise RF signal flux analysis.
 */
class PowerlineNotchFilter(
    private val sampleRateHz: Float = 100.0f
) {
    private class NotchBiquad {
        private var x1 = 0f
        private var x2 = 0f
        private var y1 = 0f
        private var y2 = 0f

        fun process(input: Float, b0: Float, b1: Float, b2: Float, a1: Float, a2: Float): Float {
            val output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = input
            y2 = y1
            y1 = output
            return output
        }

        fun reset() {
            x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
        }
    }

    private val notch50X = NotchBiquad()
    private val notch50Y = NotchBiquad()
    private val notch50Z = NotchBiquad()

    private val notch60X = NotchBiquad()
    private val notch60Y = NotchBiquad()
    private val notch60Z = NotchBiquad()

    private var b0_50 = 1f; private var b1_50 = 0f; private var b2_50 = 0f
    private var a1_50 = 0f; private var a2_50 = 0f

    private var b0_60 = 1f; private var b1_60 = 0f; private var b2_60 = 0f
    private var a1_60 = 0f; private var a2_60 = 0f

    init {
        val coeffs50 = computeNotchCoeffs(50.0f, sampleRateHz)
        b0_50 = coeffs50[0]; b1_50 = coeffs50[1]; b2_50 = coeffs50[2]
        a1_50 = coeffs50[3]; a2_50 = coeffs50[4]

        val coeffs60 = computeNotchCoeffs(60.0f, sampleRateHz)
        b0_60 = coeffs60[0]; b1_60 = coeffs60[1]; b2_60 = coeffs60[2]
        a1_60 = coeffs60[3]; a2_60 = coeffs60[4]
    }

    private fun computeNotchCoeffs(f0: Float, fs: Float): FloatArray {
        val w0 = (2.0 * PI * f0 / fs).toFloat()
        val bwHz = 6.0f
        val w0D = w0.toDouble()
        val sinW0 = sin(w0D)
        val sinhVal = (exp(bwHz * w0D / (2.0 * sinW0)) - exp(-bwHz * w0D / (2.0 * sinW0))) / 2.0
        val alpha = (sinW0 * sinhVal).toFloat().coerceIn(0.01f, 0.95f)

        val b0 = 1f
        val b1 = -2f * cos(w0)
        val b2 = 1f
        val a0 = 1f + alpha
        val a1 = -2f * cos(w0)
        val a2 = 1f - alpha

        return floatArrayOf(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    /**
     * Filters 3-axis magnetometer readings (x, y, z) through cascaded 50Hz & 60Hz notch filters.
     * @return FloatArray containing [filteredX, filteredY, filteredZ]
     */
    fun filter(rawX: Float, rawY: Float, rawZ: Float): FloatArray {
        val x50 = notch50X.process(rawX, b0_50, b1_50, b2_50, a1_50, a2_50)
        val xF = notch60X.process(x50, b0_60, b1_60, b2_60, a1_60, a2_60)

        val y50 = notch50Y.process(rawY, b0_50, b1_50, b2_50, a1_50, a2_50)
        val yF = notch60Y.process(y50, b0_60, b1_60, b2_60, a1_60, a2_60)

        val z50 = notch50Z.process(rawZ, b0_50, b1_50, b2_50, a1_50, a2_50)
        val zF = notch60Z.process(z50, b0_60, b1_60, b2_60, a1_60, a2_60)

        return floatArrayOf(xF, yF, zF)
    }

    fun reset() {
        notch50X.reset(); notch50Y.reset(); notch50Z.reset()
        notch60X.reset(); notch60Y.reset(); notch60Z.reset()
    }
}
