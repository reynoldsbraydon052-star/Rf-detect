package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class PerimeterAlarmEngine(private val context: Context) {

    private var lastVibrationTimeMs = 0L
    private val alertCooldownMs = 1500L // Prevent continuous harsh buzz

    private var toneGenerator: ToneGenerator? = null
    private var currentAlarmState: AlarmState = AlarmState.NORMAL
    private var beepJob: Job? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setAlarmState(state: AlarmState) {
        if (currentAlarmState == state) return
        currentAlarmState = state

        // Stop any active periodic beep job
        beepJob?.cancel()
        beepJob = null

        when (state) {
            AlarmState.NORMAL, AlarmState.COOLDOWN -> {
                // Silent
            }
            AlarmState.APPROACHING -> {
                // Slow periodic beep: e.g., every 2.0 seconds
                beepJob = CoroutineScope(Dispatchers.IO).launch {
                    while (isActive && currentAlarmState == AlarmState.APPROACHING) {
                        playBeep()
                        delay(2000L)
                    }
                }
            }
            AlarmState.TRIGGERED -> {
                // Single distinct alert: play exactly once on transition
                playSingleDistinctAlert()
                triggerProximityAlert()
            }
        }
    }

    private fun playBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
        } catch (_: Exception) {}
    }

    private fun playSingleDistinctAlert() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
        } catch (_: Exception) {}
    }

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

    fun release() {
        beepJob?.cancel()
        beepJob = null
        try {
            toneGenerator?.release()
        } catch (_: Exception) {}
        toneGenerator = null
    }
}
