package com.example

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewState containing pre-calculated metrics, smoothed waveforms, and adaptive scale limits.
 */
data class FluctuationViewState(
    val selectedFilter: String = "ALL",
    val primaryMetrics: ComputedTelemetryMetrics = ComputedTelemetryMetrics(-100.0, -100.0, -100.0, -100.0, 0.0, 0.0, 0.0, 0),
    val channelSnapshots: Map<String, List<TelemetrySample>> = emptyMap(),
    val activeSamplesToDraw: List<TelemetrySample> = emptyList(),
    val yAxisBounds: DynamicYAxisBounds = DynamicYAxisBounds(-100f, -20f),
    val spectrumDensity: Map<String, Float> = mapOf("WIFI" to 0f, "BLE" to 0f, "CELLULAR" to 0f, "MAGNETIC" to 0f),
    val selectedTargetBlip: RadarBlip? = null,
    val totalSignalsTracked: Int = 0,
    val isLive: Boolean = true
)

/**
 * High-performance telemetry engine for RF signal fluctuation analysis.
 * Ingests incoming sensor observations, calculates metrics off the main thread,
 * and throttles UI StateFlow emissions to maintain smooth 30-60 FPS rendering.
 */
class RfFluctuationEngine(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val calculationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val bufferCapacityPerChannel: Int = 100
) {
    val telemetryBuffer = MultiProtocolTelemetryBuffer(bufferCapacityPerChannel)

    private val _viewState = MutableStateFlow(FluctuationViewState())
    val viewState: StateFlow<FluctuationViewState> = _viewState.asStateFlow()

    private var currentFilter = "ALL"
    private var selectedTargetId: String? = null
    private var activeBlipsCache: List<RadarBlip> = emptyList()

    private var tickerJob: Job? = null

    init {
        startTelemetryLoop()
    }

    fun setFilter(filter: String) {
        currentFilter = filter
        triggerCalculation()
    }

    fun setSelectedTarget(targetId: String?) {
        selectedTargetId = targetId
        triggerCalculation()
    }

    /**
     * Ingests a new batch of radar blips from real-time radio scanners.
     */
    fun ingestBlips(blips: List<RadarBlip>, targetId: String? = null) {
        if (targetId != null) {
            selectedTargetId = targetId
        }
        activeBlipsCache = blips
        val now = System.currentTimeMillis()

        scope.launch(calculationDispatcher) {
            blips.forEach { blip ->
                val sample = TelemetrySample(
                    timestampMs = if (blip.timestampMs > 0) blip.timestampMs else now,
                    rssiDbm = blip.rssi.toDouble(),
                    protocolType = blip.type.uppercase(),
                    deviceId = blip.id,
                    frequencyMhz = blip.frequencyMhz
                )
                telemetryBuffer.ingest(sample)
            }
        }
    }

    /**
     * Starts a background calculation loop throttled to ~40ms (25-30 FPS) to decouple
     * heavy math from Compose UI rendering.
     */
    private fun startTelemetryLoop() {
        tickerJob?.cancel()
        tickerJob = scope.launch(calculationDispatcher) {
            while (isActive) {
                calculateAndEmit()
                delay(40L) // ~25 updates per sec
            }
        }
    }

    private fun triggerCalculation() {
        scope.launch(calculationDispatcher) {
            calculateAndEmit()
        }
    }

    private fun calculateAndEmit() {
        val snapshots = telemetryBuffer.getAllSnapshots()
        val globalSnapshot = telemetryBuffer.getGlobalBuffer().getSnapshot()

        // 1. Identify target blip or primary signal
        val currentBlips = activeBlipsCache
        val targetBlip = currentBlips.firstOrNull { it.id == selectedTargetId || it.name == selectedTargetId }
        val filteredBlips = if (currentFilter == "ALL") {
            currentBlips
        } else {
            currentBlips.filter { it.type.equals(currentFilter, ignoreCase = true) }
        }
        val primaryBlip = targetBlip ?: filteredBlips.minByOrNull { it.distance }

        // 2. Select samples for active drawing
        val samplesToDraw = if (targetBlip != null) {
            // Filter global samples for this target device if available, otherwise protocol samples
            val deviceSamples = globalSnapshot.filter { it.deviceId == targetBlip.id }
            if (deviceSamples.isNotEmpty()) deviceSamples else (snapshots[targetBlip.type.uppercase()] ?: emptyList())
        } else if (currentFilter == "ALL") {
            globalSnapshot
        } else {
            snapshots[currentFilter.uppercase()] ?: emptyList()
        }

        // 3. Compute Metrics
        val metrics = SignalTelemetryCalculator.calculateMetrics(samplesToDraw)

        // 4. Compute Dynamic Y-Axis Bounds
        val yBounds = SignalTelemetryCalculator.calculateDynamicYAxisBounds(samplesToDraw)

        // 5. Compute Spectrum Density
        val density = SignalTelemetryCalculator.computeSpectrumDensity(snapshots)

        // 6. Update ViewState
        _viewState.update {
            it.copy(
                selectedFilter = currentFilter,
                primaryMetrics = metrics,
                channelSnapshots = snapshots,
                activeSamplesToDraw = samplesToDraw,
                yAxisBounds = yBounds,
                spectrumDensity = density,
                selectedTargetBlip = targetBlip,
                totalSignalsTracked = currentBlips.size,
                isLive = true
            )
        }
    }

    fun clear() {
        telemetryBuffer.clearAll()
        triggerCalculation()
    }

    fun destroy() {
        tickerJob?.cancel()
    }
}
