package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RfSignalFluctuationsTest {

    // 1. RING BUFFER BOUNDARY CONDITIONS
    @Test
    fun testRingBufferEmptyState() {
        val buffer = TelemetryRingBuffer(capacity = 5)
        assertTrue(buffer.isEmpty)
        assertFalse(buffer.isFull)
        assertEquals(0, buffer.size)
        assertTrue(buffer.getSnapshot().isEmpty())
        assertNull(buffer.getLatest())
    }

    @Test
    fun testRingBufferUnderCapacity() {
        val buffer = TelemetryRingBuffer(capacity = 5)
        val s1 = TelemetrySample(1000L, -70.0, "WIFI")
        val s2 = TelemetrySample(2000L, -65.0, "WIFI")

        assertTrue(buffer.add(s1))
        assertTrue(buffer.add(s2))

        assertEquals(2, buffer.size)
        assertFalse(buffer.isEmpty)
        assertFalse(buffer.isFull)

        val snapshot = buffer.getSnapshot()
        assertEquals(2, snapshot.size)
        assertEquals(s1, snapshot[0])
        assertEquals(s2, snapshot[1])
        assertEquals(s2, buffer.getLatest())
    }

    @Test
    fun testRingBufferOverflowAndOldestDropped() {
        val buffer = TelemetryRingBuffer(capacity = 3)
        buffer.add(TelemetrySample(1000L, -80.0, "BLE"))
        buffer.add(TelemetrySample(2000L, -75.0, "BLE"))
        buffer.add(TelemetrySample(3000L, -70.0, "BLE"))

        assertTrue(buffer.isFull)
        assertEquals(3, buffer.size)

        // Add 4th item -> drops the 1st (-80.0)
        buffer.add(TelemetrySample(4000L, -60.0, "BLE"))
        assertEquals(3, buffer.size)
        assertTrue(buffer.isFull)

        val snapshot = buffer.getSnapshot()
        assertEquals(3, snapshot.size)
        assertEquals(-75.0, snapshot[0].rssiDbm, 0.001)
        assertEquals(-70.0, snapshot[1].rssiDbm, 0.001)
        assertEquals(-60.0, snapshot[2].rssiDbm, 0.001)
        assertEquals(-60.0, buffer.getLatest()!!.rssiDbm, 0.001)

        // Add 5th item -> drops the 2nd (-75.0)
        buffer.add(TelemetrySample(5000L, -50.0, "BLE"))
        val snapshot2 = buffer.getSnapshot()
        assertEquals(3, snapshot2.size)
        assertEquals(-70.0, snapshot2[0].rssiDbm, 0.001)
        assertEquals(-60.0, snapshot2[1].rssiDbm, 0.001)
        assertEquals(-50.0, snapshot2[2].rssiDbm, 0.001)
    }

    @Test
    fun testRingBufferClear() {
        val buffer = TelemetryRingBuffer(capacity = 4)
        buffer.add(TelemetrySample(1000L, -70.0, "CELLULAR"))
        buffer.add(TelemetrySample(2000L, -65.0, "CELLULAR"))
        assertEquals(2, buffer.size)

        buffer.clear()
        assertTrue(buffer.isEmpty)
        assertEquals(0, buffer.size)
        assertNull(buffer.getLatest())
        assertTrue(buffer.getSnapshot().isEmpty())
    }

    // 2. DATA SANITIZATION: NAN, INFINITY, OUTLIERS REJECTION
    @Test
    fun testNanAndInfinityRejection() {
        val buffer = TelemetryRingBuffer(capacity = 5)

        // Invalid samples
        val nanSample = TelemetrySample(1000L, Double.NaN, "WIFI")
        val posInfSample = TelemetrySample(2000L, Double.POSITIVE_INFINITY, "WIFI")
        val negInfSample = TelemetrySample(3000L, Double.NEGATIVE_INFINITY, "WIFI")
        val impossibleHighSample = TelemetrySample(4000L, 50.0, "WIFI") // > 20 dBm
        val impossibleLowSample = TelemetrySample(5000L, -200.0, "WIFI") // < -150 dBm

        assertFalse(nanSample.isValid())
        assertFalse(posInfSample.isValid())
        assertFalse(negInfSample.isValid())
        assertFalse(impossibleHighSample.isValid())
        assertFalse(impossibleLowSample.isValid())

        assertFalse(buffer.add(nanSample))
        assertFalse(buffer.add(posInfSample))
        assertFalse(buffer.add(negInfSample))
        assertFalse(buffer.add(impossibleHighSample))
        assertFalse(buffer.add(impossibleLowSample))

        assertEquals(0, buffer.size)

        // Valid sample succeeds
        val validSample = TelemetrySample(6000L, -65.0, "WIFI")
        assertTrue(validSample.isValid())
        assertTrue(buffer.add(validSample))
        assertEquals(1, buffer.size)
    }

    // 3. MOVING AVERAGE AND PEAK CALCULATION ACCURACY
    @Test
    fun testMovingAverageAndPeakCalculation() {
        val samples = listOf(
            TelemetrySample(1000L, -80.0, "WIFI"),
            TelemetrySample(2000L, -60.0, "WIFI"),
            TelemetrySample(3000L, -70.0, "WIFI"),
            TelemetrySample(4000L, -50.0, "WIFI"), // Peak
            TelemetrySample(5000L, -90.0, "WIFI")  // Min
        )

        val metrics = SignalTelemetryCalculator.calculateMetrics(samples)

        // Current is last sample (-90.0)
        assertEquals(-90.0, metrics.currentRssi, 0.001)
        // Peak is highest dBm (-50.0)
        assertEquals(-50.0, metrics.peakRssi, 0.001)
        // Min is lowest dBm (-90.0)
        assertEquals(-90.0, metrics.minRssi, 0.001)
        // Avg is (-80 + -60 + -70 + -50 + -90) / 5 = -350 / 5 = -70.0
        assertEquals(-70.0, metrics.avgRssi, 0.001)
        assertEquals(5, metrics.sampleCount)
    }

    // 4. JITTER CALCULATION WITH VARYING DELTAS
    @Test
    fun testJitterCalculation() {
        // Case A: Perfectly flat signal -> Jitter should be 0.0
        val flatSamples = listOf(
            TelemetrySample(1000L, -65.0, "BLE"),
            TelemetrySample(2000L, -65.0, "BLE"),
            TelemetrySample(3000L, -65.0, "BLE"),
            TelemetrySample(4000L, -65.0, "BLE")
        )
        val flatMetrics = SignalTelemetryCalculator.calculateMetrics(flatSamples)
        assertEquals(0.0, flatMetrics.jitterDbm, 0.001)
        assertEquals(0.0, flatMetrics.standardDeviation, 0.001)
        assertEquals(100.0, flatMetrics.stabilityPercent, 0.001)

        // Case B: Known step deltas
        // Steps: -70 to -60 (delta 10), -60 to -65 (delta 5), -65 to -55 (delta 10)
        // Mean absolute deviation = (10 + 5 + 10) / 3 = 25 / 3 = 8.333
        val fluctuatingSamples = listOf(
            TelemetrySample(1000L, -70.0, "BLE"),
            TelemetrySample(2000L, -60.0, "BLE"),
            TelemetrySample(3000L, -65.0, "BLE"),
            TelemetrySample(4000L, -55.0, "BLE")
        )
        val fluctMetrics = SignalTelemetryCalculator.calculateMetrics(fluctuatingSamples)
        assertEquals(25.0 / 3.0, fluctMetrics.jitterDbm, 0.01)
        assertTrue(fluctMetrics.stabilityPercent < 80.0)
    }

    // 5. STABILITY INDEX SCALING (0% TO 100%)
    @Test
    fun testStabilityIndexScaling() {
        // High stability (small ±1 dBm jitter)
        val steadySamples = listOf(
            TelemetrySample(1000L, -60.0, "WIFI"),
            TelemetrySample(2000L, -61.0, "WIFI"),
            TelemetrySample(3000L, -60.0, "WIFI"),
            TelemetrySample(4000L, -59.0, "WIFI")
        )
        val steadyMetrics = SignalTelemetryCalculator.calculateMetrics(steadySamples)
        assertTrue("Steady signal stability should be high (>85%)", steadyMetrics.stabilityPercent >= 85.0)
        assertTrue("Stability must not exceed 100%", steadyMetrics.stabilityPercent <= 100.0)

        // Severe fading / extreme fluctuations (deltas > 20 dBm)
        val chaoticSamples = listOf(
            TelemetrySample(1000L, -30.0, "WIFI"),
            TelemetrySample(2000L, -95.0, "WIFI"),
            TelemetrySample(3000L, -25.0, "WIFI"),
            TelemetrySample(4000L, -100.0, "WIFI")
        )
        val chaoticMetrics = SignalTelemetryCalculator.calculateMetrics(chaoticSamples)
        assertTrue("Chaotic signal stability should be low (<40%)", chaoticMetrics.stabilityPercent < 40.0)
        assertTrue("Stability must be >= 0.0%", chaoticMetrics.stabilityPercent >= 0.0)
    }

    // 6. DYNAMIC Y-AXIS SCALE CALCULATION WITH HEADROOM AND OUTLIERS
    @Test
    fun testDynamicYAxisBounds() {
        // Empty case defaults
        val emptyBounds = SignalTelemetryCalculator.calculateDynamicYAxisBounds(emptyList())
        assertEquals(-100.0f, emptyBounds.minY, 0.001f)
        assertEquals(-20.0f, emptyBounds.maxY, 0.001f)

        // Narrow range centered at -60 dBm
        val narrowSamples = listOf(
            TelemetrySample(1000L, -62.0, "WIFI"),
            TelemetrySample(2000L, -60.0, "WIFI"),
            TelemetrySample(3000L, -58.0, "WIFI")
        )
        val narrowBounds = SignalTelemetryCalculator.calculateDynamicYAxisBounds(narrowSamples, minHeadroomDbm = 10.0f)
        // With min span of 20 dBm, should encompass at least -70 to -50
        assertTrue("minY should be <= -70", narrowBounds.minY <= -70.0f)
        assertTrue("maxY should be >= -50", narrowBounds.maxY >= -50.0f)
        assertTrue("Span should be >= 20 dBm", (narrowBounds.maxY - narrowBounds.minY) >= 20.0f)

        // Extreme bounds clamping check
        val extremeSamples = listOf(
            TelemetrySample(1000L, -145.0, "CELLULAR"),
            TelemetrySample(2000L, 15.0, "CELLULAR")
        )
        val extremeBounds = SignalTelemetryCalculator.calculateDynamicYAxisBounds(extremeSamples)
        assertTrue("minY must not go below clamped -110", extremeBounds.minY >= -110.0f)
        assertTrue("maxY must not exceed clamped -10", extremeBounds.maxY <= -10.0f)
    }

    // 7. MULTI-PROTOCOL SPECTRUM DENSITY BREAKDOWN
    @Test
    fun testSpectrumDensityComputation() {
        val snapshots = mapOf(
            "WIFI" to listOf(
                TelemetrySample(1000L, -60.0, "WIFI"),
                TelemetrySample(2000L, -65.0, "WIFI")
            ),
            "BLE" to listOf(
                TelemetrySample(1000L, -75.0, "BLE")
            ),
            "CELLULAR" to listOf(
                TelemetrySample(1000L, -85.0, "CELLULAR")
            ),
            "MAGNETIC" to emptyList<TelemetrySample>()
        )

        val density = SignalTelemetryCalculator.computeSpectrumDensity(snapshots)
        // Total samples = 4
        assertEquals(0.50f, density["WIFI"] ?: 0f, 0.01f) // 2 / 4 = 50%
        assertEquals(0.25f, density["BLE"] ?: 0f, 0.01f)  // 1 / 4 = 25%
        assertEquals(0.25f, density["CELLULAR"] ?: 0f, 0.01f) // 1 / 4 = 25%
        assertEquals(0.0f, density["MAGNETIC"] ?: 0f, 0.01f)  // 0 / 4 = 0%
    }
}
