package com.example

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Tactical Audio & Tactile Haptic Sonar Engine.
 * Sonar pings and vibration only execute when actively triggered by the user.
 */
class AudioRadarTracker(private val context: Context? = null) {

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var proximityDistance = 15.0 // Meters
    private var trackerJob: Job? = null

    // Sonar Configuration
    var isVibrationEnabled: Boolean = true
    var volumeLevel: Float = 0.85f

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

        trackerJob = CoroutineScope(Dispatchers.IO).launch {
            val sampleRate = 44100
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(minBufSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.setVolume(volumeLevel.coerceIn(0f, 1f))
                audioTrack?.play()

                while (isActive && isPlaying) {
                    // Closer distance = higher pitch and faster pulse rate
                    val dist = proximityDistance.coerceIn(0.2, 50.0)
                    val freq = (1750.0 - dist * 30.0).coerceIn(400.0, 1800.0)
                    val pulseDelayMs = (dist * 30.0).toLong().coerceIn(75L, 1200L)

                    // Emit sonar ping burst (70ms tone burst)
                    val burstSamples = (sampleRate * 0.07).toInt()
                    val buffer = ShortArray(burstSamples)
                    var ph = 0.0
                    val phInc = 2.0 * Math.PI * freq / sampleRate

                    for (i in buffer.indices) {
                        // Apply sine window envelope to eliminate audio clipping
                        val env = sin(Math.PI * i / burstSamples)
                        buffer[i] = (sin(ph) * env * 22000 * volumeLevel).toInt().toShort()
                        ph += phInc
                    }

                    audioTrack?.write(buffer, 0, buffer.size)

                    // Synchronized tactile vibration pulse (only when enabled and active)
                    if (isVibrationEnabled && vibrator?.hasVibrator() == true) {
                        try {
                            val vibDurationMs = (50L - (dist * 0.7f)).toLong().coerceIn(15L, 45L)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator?.vibrate(VibrationEffect.createOneShot(vibDurationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator?.vibrate(vibDurationMs)
                            }
                        } catch (_: Throwable) {}
                    }

                    // Interval between sonar pings
                    delay(pulseDelayMs)
                }
            } catch (e: Exception) {
                isPlaying = false
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (_: Exception) {}
                audioTrack = null
            }
        }
    }

    fun playSingleTestPing(distanceMeters: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            val sampleRate = 44100
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            var tempTrack: AudioTrack? = null
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()

                tempTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(minBufSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                tempTrack.setVolume(volumeLevel.coerceIn(0f, 1f))
                tempTrack.play()
                val dist = distanceMeters.coerceIn(0.2, 50.0)
                val freq = (1750.0 - dist * 30.0).coerceIn(400.0, 1800.0)
                val burstSamples = (sampleRate * 0.10).toInt()
                val buffer = ShortArray(burstSamples)
                var ph = 0.0
                val phInc = 2.0 * Math.PI * freq / sampleRate

                for (i in buffer.indices) {
                    val env = sin(Math.PI * i / burstSamples)
                    buffer[i] = (sin(ph) * env * 24000 * volumeLevel).toInt().toShort()
                    ph += phInc
                }

                tempTrack.write(buffer, 0, buffer.size)

                // Haptic pulse for test ping
                if (isVibrationEnabled && vibrator?.hasVibrator() == true) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator?.vibrate(VibrationEffect.createOneShot(35L, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator?.vibrate(35L)
                        }
                    } catch (_: Throwable) {}
                }

                delay(120)
            } catch (_: Exception) {
            } finally {
                try {
                    tempTrack?.stop()
                    tempTrack?.release()
                } catch (_: Exception) {}
            }
        }
    }

    fun updateProximityDistance(distanceMeters: Double) {
        proximityDistance = distanceMeters
    }

    fun stop() {
        isPlaying = false
        trackerJob?.cancel()
        trackerJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {}
        audioTrack = null
    }

    fun isAudioActive(): Boolean = isPlaying
}
