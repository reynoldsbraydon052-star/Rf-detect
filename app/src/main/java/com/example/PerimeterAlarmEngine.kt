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
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
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
        } catch (e: Throwable) {
            // Hardware or AppOps permission mismatch handled safely
        }
    }
}
