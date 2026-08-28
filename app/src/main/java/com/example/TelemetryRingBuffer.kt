package com.example

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance immutable data point representing an RF signal observation.
 */
data class TelemetrySample(
    val timestampMs: Long,
    val rssiDbm: Double,
    val protocolType: String,
    val deviceId: String = "",
    val frequencyMhz: Double = 0.0
) {
    /**
     * Checks if the telemetry sample contains valid, sanitized numerical data.
     */
    fun isValid(): Boolean {
        if (rssiDbm.isNaN() || rssiDbm.isInfinite()) return false
        if (frequencyMhz.isNaN() || frequencyMhz.isInfinite()) return false
        // Sensible physical RF RSSI limits: [-150 dBm, 20 dBm]
        if (rssiDbm < -150.0 || rssiDbm > 20.0) return false
        return true
    }
}

/**
 * Fixed-capacity circular/ring buffer for high-frequency signal telemetry.
 * Pre-allocates array storage to completely eliminate runtime GC pressure and memory reallocations.
 * Automatically drops the oldest samples when capacity is reached.
 */
class TelemetryRingBuffer(val capacity: Int = 100) {
    init {
        require(capacity > 0) { "Capacity must be greater than 0" }
    }

    private val buffer = arrayOfNulls<TelemetrySample>(capacity)
    private var head = 0 // points to the oldest valid element
    private var tail = 0 // points to the next write slot
    private var count = 0
    private val lock = Any()

    val size: Int
        get() = synchronized(lock) { count }

    val isEmpty: Boolean
        get() = synchronized(lock) { count == 0 }

    val isFull: Boolean
        get() = synchronized(lock) { count == capacity }

    /**
     * Inserts a telemetry sample. If buffer is full, automatically overwrites the oldest sample.
     * Silently rejects invalid samples (NaN, Infinite, out of range).
     */
    fun add(sample: TelemetrySample): Boolean {
        if (!sample.isValid()) return false

        synchronized(lock) {
            buffer[tail] = sample
            tail = (tail + 1) % capacity
            if (count < capacity) {
                count++
            } else {
                head = (head + 1) % capacity // overwrite oldest
            }
        }
        return true
    }

    /**
     * Convenience method for adding primitive signal values.
     */
    fun add(timestampMs: Long, rssiDbm: Double, protocolType: String, deviceId: String = "", frequencyMhz: Double = 0.0): Boolean {
        return add(TelemetrySample(timestampMs, rssiDbm, protocolType, deviceId, frequencyMhz))
    }

    /**
     * Returns an ordered snapshot of valid samples from oldest to newest.
     */
    fun getSnapshot(): List<TelemetrySample> {
        synchronized(lock) {
            if (count == 0) return emptyList()
            val result = ArrayList<TelemetrySample>(count)
            var idx = head
            for (i in 0 until count) {
                buffer[idx]?.let { result.add(it) }
                idx = (idx + 1) % capacity
            }
            return result
        }
    }

    /**
     * Returns the most recent valid sample, or null if buffer is empty.
     */
    fun getLatest(): TelemetrySample? {
        synchronized(lock) {
            if (count == 0) return null
            val latestIdx = (tail - 1 + capacity) % capacity
            return buffer[latestIdx]
        }
    }

    /**
     * Clears all samples in the ring buffer.
     */
    fun clear() {
        synchronized(lock) {
            for (i in 0 until capacity) {
                buffer[i] = null
            }
            head = 0
            tail = 0
            count = 0
        }
    }
}

/**
 * Multi-protocol ring buffer manager that maintains isolated, bounded circular buffers
 * for distinct RF protocols (WIFI, BLE, CELLULAR, MAGNETIC, etc.) and a global unified buffer.
 */
class MultiProtocolTelemetryBuffer(val perChannelCapacity: Int = 100) {
    private val channelBuffers = mutableMapOf<String, TelemetryRingBuffer>()
    private val globalBuffer = TelemetryRingBuffer(perChannelCapacity * 2)
    private val lock = Any()

    init {
        // Pre-initialize standard channels
        listOf("WIFI", "BLE", "CELLULAR", "MAGNETIC").forEach { type ->
            channelBuffers[type] = TelemetryRingBuffer(perChannelCapacity)
        }
    }

    fun getBuffer(protocol: String): TelemetryRingBuffer {
        val key = protocol.uppercase()
        synchronized(lock) {
            return channelBuffers.getOrPut(key) { TelemetryRingBuffer(perChannelCapacity) }
        }
    }

    fun getGlobalBuffer(): TelemetryRingBuffer = globalBuffer

    fun ingest(sample: TelemetrySample): Boolean {
        if (!sample.isValid()) return false
        val key = sample.protocolType.uppercase()
        val channelBuf = getBuffer(key)
        channelBuf.add(sample)
        globalBuffer.add(sample)
        return true
    }

    fun clearAll() {
        synchronized(lock) {
            channelBuffers.values.forEach { it.clear() }
            globalBuffer.clear()
        }
    }

    fun getAllSnapshots(): Map<String, List<TelemetrySample>> {
        synchronized(lock) {
            val result = mutableMapOf<String, List<TelemetrySample>>()
            channelBuffers.forEach { (proto, buf) ->
                if (!buf.isEmpty) {
                    result[proto] = buf.getSnapshot()
                }
            }
            return result
        }
    }
}
