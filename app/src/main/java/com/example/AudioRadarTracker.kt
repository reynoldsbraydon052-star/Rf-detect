package com.example

import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class AudioRadarTracker {

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var proximityDistance = 15.0 // Meters
    private var trackerJob: Job? = null

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
            )

            try {
                audioTrack = AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufSize.coerceAtLeast(2048),
                    AudioTrack.MODE_STREAM
                )
                audioTrack?.play()

                while (isActive && isPlaying) {
                    // Closer distance = higher pitch and faster pulse rate
                    val dist = proximityDistance.coerceIn(0.5, 50.0)
                    val freq = (1200.0 - dist * 18.0).coerceIn(400.0, 1800.0)
                    val pulseDelayMs = (dist * 35.0).toLong().coerceIn(80L, 1200L)

                    // Emit sonar ping burst (100ms tone burst)
                    val burstSamples = (sampleRate * 0.08).toInt()
                    val buffer = ShortArray(burstSamples)
                    var ph = 0.0
                    val phInc = 2.0 * Math.PI * freq / sampleRate

                    for (i in buffer.indices) {
                        // Apply envelope to reduce audio click
                        val env = sin(Math.PI * i / burstSamples)
                        buffer[i] = (sin(ph) * env * 18000).toInt().toShort()
                        ph += phInc
                    }

                    audioTrack?.write(buffer, 0, buffer.size)

                    // Interval between sonar pings
                    delay(pulseDelayMs)
                }
            } catch (e: Exception) {
                isPlaying = false
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
