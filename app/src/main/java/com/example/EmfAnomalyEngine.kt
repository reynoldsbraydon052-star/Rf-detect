package com.example

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * EMF Power-Draw & Magnetometer Anomaly Telemetry Engine (Phase 4C).
 *
 * Coordinates real-time sensor sampling, low-latency DSP processing,
 * AC ripple extraction, and thread-safe StateFlow distribution.
 */
class EmfAnomalyEngine(
    private val scanner: EmfSensorScanner,
    val processor: EmfSignalProcessor = EmfSignalProcessor(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val minEmissionIntervalNs: Long = 33_333_333L // ~30 Hz maximum emission rate
) {
    private val _telemetryState = MutableStateFlow(EmfTelemetryState())
    val telemetryState: StateFlow<EmfTelemetryState> = _telemetryState.asStateFlow()

    private var scanJob: Job? = null
    private var lastEmittedTimestampNs: Long = 0L

    @Volatile
    var isRunning: Boolean = false
        private set

    /**
     * Starts continuous ingestion and processing of magnetometer samples.
     * All DSP computations occur on [Dispatchers.Default].
     */
    @Synchronized
    fun start(scope: CoroutineScope) {
        if (isRunning) return
        isRunning = true
        lastEmittedTimestampNs = 0L

        scanJob = scope.launch(dispatcher) {
            scanner.observeMagneticField()
                .flowOn(dispatcher)
                .collect { sample ->
                    ingestSample(sample)
                }
        }
    }

    /**
     * Directly ingests a sample into the DSP pipeline and conditionally emits to [StateFlow]
     * if the 30 Hz throttling boundary is satisfied.
     */
    fun ingestSample(sample: MagneticSample) {
        val state = processor.processSample(sample) ?: return

        val nowNs = if (sample.timestampNs > 0) sample.timestampNs else System.nanoTime()
        val timeSinceLastEmit = nowNs - lastEmittedTimestampNs

        if (timeSinceLastEmit >= minEmissionIntervalNs || lastEmittedTimestampNs == 0L) {
            lastEmittedTimestampNs = nowNs
            _telemetryState.value = state
        }
    }

    /**
     * Ingests raw vector components directly (useful for testing or simulation).
     */
    fun ingestRaw(x: Float, y: Float, z: Float, timestampNs: Long = System.nanoTime(), isUncalibrated: Boolean = false) {
        ingestSample(
            MagneticSample(
                x = x,
                y = y,
                z = z,
                timestampNs = timestampNs,
                isUncalibrated = isUncalibrated
            )
        )
    }

    /**
     * Halts sensor ingestion and immediately unregisters hardware listeners.
     */
    @Synchronized
    fun stop() {
        isRunning = false
        scanJob?.cancel()
        scanJob = null
    }

    /**
     * Resets the underlying signal baseline and state.
     */
    fun reset() {
        processor.resetBaseline()
        _telemetryState.value = EmfTelemetryState()
        lastEmittedTimestampNs = 0L
    }
}
