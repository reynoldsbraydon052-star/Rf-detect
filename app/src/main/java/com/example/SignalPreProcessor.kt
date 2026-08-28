package com.example

/**
 * Data class representing raw signals captured from the hardware scanner receivers.
 */
data class RawSignal(
    val macAddress: String,
    val vendorOrSsid: String?,
    val currentRssi: Int,
    val previousRssi: Int
)

/**
 * Data class representing the payload structure generated specifically for Gemini LLM analysis.
 */
data class ThreatPayload(
    val targetMac: String,
    val rssiDelta: Int,
    val currentRssi: Int,
    val context: String?
)

/**
 * A highly optimized Pre-Processor class that filters, ranks, and truncates raw RF/BLE signals
 * before they are marshaled to the on-device LLM (Gemini) payload, preventing context bloat and memory pressure.
 */
class SignalPreProcessor(
    private val whitelist: Set<String>,
    private val k: Int = 5
) {

    /**
     * Executes the highly performant collection pipeline over a stream of raw ambient signals:
     * 1. Whitelist Filtering: Drops trusted devices.
     * 2. Delta Calculation & Hysteresis Gating: Evaluates approach speed (currentRssi - previousRssi)
     *    and filters out non-moving or fading signals (delta <= 2).
     * 3. Descending Sorting: Ranks signals with the most rapid physical approach.
     * 4. Top-K Truncation: Hard-caps the payload list to prevent token overflow.
     * 5. Late Mapping: Converts surviving signals to the target payload format to minimize allocation churn.
     *
     * @param signals The list of raw signals detected during the hardware sweep loop.
     * @return List of high-priority [ThreatPayload] targets prepared for LLM serialization.
     */
    fun processSignals(signals: List<RawSignal>): List<ThreatPayload> {
        if (signals.isEmpty()) return emptyList()

        return signals
            // 1. Whitelist Filtering: Eagerly drop whitelisted addresses with O(1) set lookups
            .filterNot { whitelist.contains(it.macAddress) }
            
            // 2. Delta Calculation & 3. Hysteresis Gating:
            // Calculate delta and only retain active signals where approach is significant (delta > 2)
            .filter { signal ->
                val delta = signal.currentRssi - signal.previousRssi
                delta > 2
            }
            
            // 4. Sorting: Sort descending based on positive approach delta
            .sortedByDescending { it.currentRssi - it.previousRssi }
            
            // 5. Truncation: Cap to Top-K elements
            .take(k)
            
            // 6. Data Mapping: Late-map only the surviving K items to minimize GC pressure on the UI thread
            .map { signal ->
                val delta = signal.currentRssi - signal.previousRssi
                ThreatPayload(
                    targetMac = signal.macAddress,
                    rssiDelta = delta,
                    currentRssi = signal.currentRssi,
                    context = signal.vendorOrSsid?.let { "Vendor/SSID: $it" } ?: "Unknown Device"
                )
            }
    }
}
