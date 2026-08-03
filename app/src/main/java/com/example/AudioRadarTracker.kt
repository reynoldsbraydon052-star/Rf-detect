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
                    val dist = proximityDistance.coerceIn(0.2, 50.0)
                    val freq = (1750.0 - dist * 30.0).coerceIn(400.0, 1800.0)
                    val pulseDelayMs = (dist * 30.0).toLong().coerceIn(70L, 1200L)

                    // Emit sonar ping burst (80ms tone burst)
                    val burstSamples = (sampleRate * 0.08).toInt()
                    val buffer = ShortArray(burstSamples)
                    var ph = 0.0
                    val phInc = 2.0 * Math.PI * freq / sampleRate

                    for (i in buffer.indices) {
                        // Apply sine window envelope to eliminate audio clipping
                        val env = sin(Math.PI * i / burstSamples)
                        buffer[i] = (sin(ph) * env * 22000).toInt().toShort()
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

    fun playSingleTestPing(distanceMeters: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            val sampleRate = 44100
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val tempTrack = try {
                AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufSize.coerceAtLeast(2048),
                    AudioTrack.MODE_STREAM
                )
            } catch (e: Exception) {
                null
            } ?: return@launch

            try {
                tempTrack.play()
                val dist = distanceMeters.coerceIn(0.2, 50.0)
                val freq = (1750.0 - dist * 30.0).coerceIn(400.0, 1800.0)
                val burstSamples = (sampleRate * 0.12).toInt()
                val buffer = ShortArray(burstSamples)
                var ph = 0.0
                val phInc = 2.0 * Math.PI * freq / sampleRate

                for (i in buffer.indices) {
                    val env = sin(Math.PI * i / burstSamples)
                    buffer[i] = (sin(ph) * env * 24000).toInt().toShort()
                    ph += phInc
                }

                tempTrack.write(buffer, 0, buffer.size)
                delay(150)
                tempTrack.stop()
                tempTrack.release()
            } catch (_: Exception) {}
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
