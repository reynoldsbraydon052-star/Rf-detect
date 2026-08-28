package com.example

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

enum class SonarState {
    IDLE, FAR, APPROACHING, CLOSE, TARGET_REACHED, UNAVAILABLE
}

class AudioRadarTracker(private val context: Context? = null) {
    private var toneGenerator: ToneGenerator? = null
    private var isPlaying = false
    private var proximityDistance = 15.0 // Meters
    private var trackerJob: Job? = null
    
    // Sonar Configuration
    var isVibrationEnabled: Boolean = true
    var volumeLevel: Float = 0.85f
    
    var proximityThresholdMeters = 2.0
    
    private val _currentState = MutableStateFlow(SonarState.IDLE)
    val currentState: StateFlow<SonarState> = _currentState.asStateFlow()

    private val vibrator: Vibrator? by lazy {
        context?.let { ctx ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
            } catch (_: Throwable) {
                null
            }
        }
    }

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, (volumeLevel * 100).toInt())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleAudioTracker(): Boolean {
        if (isPlaying) {
            stop()
            return false
        } else {
            start()
            return true
        }
    }

    fun start() {
        if (isPlaying) return
        isPlaying = true
        _currentState.value = SonarState.IDLE
        startLoop()
    }
    
    private fun startLoop() {
        trackerJob?.cancel()
        trackerJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && isPlaying) {
                val dist = proximityDistance
                
                if (dist < 0) {
                    _currentState.value = SonarState.UNAVAILABLE
                    delay(1000)
                    continue
                }

                if (dist <= proximityThresholdMeters) {
                    if (_currentState.value != SonarState.TARGET_REACHED) {
                        _currentState.value = SonarState.TARGET_REACHED
                        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 5000)
                        
                        if (isVibrationEnabled) {
                            vibrate(1000L)
                        }
                    }
                    delay(5000) // Don't loop immediately
                } else {
                    _currentState.value = when {
                        dist > 50.0 -> SonarState.FAR
                        dist > 15.0 -> SonarState.APPROACHING
                        else -> SonarState.CLOSE
                    }
                    
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
                    
                    if (isVibrationEnabled) {
                        val vibDurationMs = (50L - (dist * 0.7f).toLong()).coerceIn(15L, 45L)
                        vibrate(vibDurationMs)
                    }
                    
                    // Interpolated delay: 100m -> 3000ms, 5m -> 200ms
                    val delayMs = max(200L, (dist * 30L).toLong())
                    delay(delayMs)
                }
            }
        }
    }
    
    private fun vibrate(duration: Long) {
        if (vibrator?.hasVibrator() == true) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(duration)
                }
            } catch (_: Throwable) {}
        }
    }

    fun playSingleTestPing(distanceMeters: Double) {
        if (!isPlaying) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                if (isVibrationEnabled) vibrate(35L)
            } catch (e: Exception) {}
        }
    }

    fun updateProximityDistance(distanceMeters: Double) {
        proximityDistance = distanceMeters
    }

    fun stop() {
        isPlaying = false
        trackerJob?.cancel()
        trackerJob = null
        toneGenerator?.stopTone()
        _currentState.value = SonarState.IDLE
    }

    fun isAudioActive(): Boolean = isPlaying
    
    
}
