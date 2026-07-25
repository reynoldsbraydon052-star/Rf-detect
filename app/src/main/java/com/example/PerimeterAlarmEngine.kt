package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat

class PerimeterAlarmEngine(private val context: Context) {

    private var lastVibrationTimeMs = 0L
    private val alertCooldownMs = 1500L // Prevent continuous harsh buzz

    fun triggerProximityAlert() {
        val now = System.currentTimeMillis()
        if (now - lastVibrationTimeMs < alertCooldownMs) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                if (vibratorManager?.defaultVibrator?.hasVibrator() == true) {
                    val effect = VibrationEffect.createWaveform(
                        longArrayOf(0, 150, 100, 200),
                        intArrayOf(0, 255, 0, 255),
                        -1
                    )
                    vibratorManager.vibrate(CombinedVibration.createParallel(effect))
                    lastVibrationTimeMs = now
                }
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = VibrationEffect.createWaveform(
                            longArrayOf(0, 150, 100, 200),
                            intArrayOf(0, 255, 0, 255),
                            -1
                        )
                        vibrator.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 150, 100, 200), -1)
                    }
                    lastVibrationTimeMs = now
                }
            }
        } catch (e: Exception) {
            // Permission or hardware missing gracefully handled
        }
    }
}
