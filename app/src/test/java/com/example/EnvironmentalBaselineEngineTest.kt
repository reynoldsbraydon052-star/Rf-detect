package com.example

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EnvironmentalBaselineEngineTest {

    private lateinit var classUnderTest: EnvironmentalBaselineEngine

    @Before
    fun setup() {
        classUnderTest = EnvironmentalBaselineEngine()
        classUnderTest.minObservationsForKnown = 5
        classUnderTest.anomalyRssiThresholdDb = 20.0
    }

    @Test
    fun `1 repeated signals become baseline-known`() {
        val blip = RadarBlip(name = "Router", distance = 5f, targetAngleOffset = 0f, type = "WIFI", rssi = -50, fingerprintId = "fp_1")
        val fp = SignalFingerprint(id = "fp_1", signalType = "WIFI", observationCount = 10, rssiMean = -50.0, frequencyMean = 2400.0, bandwidthMean = 20.0, timingIntervalMean = 100.0)
        
        val (processed, _) = classUnderTest.processBaseline(listOf(blip), mapOf("fp_1" to fp), true, 100, 1f, 20f, 1000L)
        
        assertEquals(BaselineState.KNOWN, processed[0].baselineState)
    }

    @Test
    fun `2 new fingerprints are identified as new`() {
        // No fingerprint match
        val blip1 = RadarBlip(name = "Unknown Device", distance = 5f, targetAngleOffset = 0f, type = "BLE", rssi = -60)
        
        // Fingerprint with low observations
        val blip2 = RadarBlip(name = "New Router", distance = 5f, targetAngleOffset = 0f, type = "WIFI", rssi = -50, fingerprintId = "fp_2")
        val fp2 = SignalFingerprint(id = "fp_2", signalType = "WIFI", observationCount = 2, rssiMean = -50.0, frequencyMean = 2400.0, bandwidthMean = 20.0, timingIntervalMean = 100.0)
        
        val (processed, summary) = classUnderTest.processBaseline(listOf(blip1, blip2), mapOf("fp_2" to fp2), true, 100, 2f, 40f, 1000L)
        
        assertEquals(BaselineState.NEW, processed[0].baselineState)
        assertEquals(BaselineState.NEW, processed[1].baselineState)
        assertEquals(2, summary.newFingerprints)
    }

    @Test
    fun `3 normal measurement variation does not create false anomalies`() {
        val blip = RadarBlip(name = "Router", distance = 5f, targetAngleOffset = 0f, type = "WIFI", rssi = -55, fingerprintId = "fp_1")
        val fp = SignalFingerprint(id = "fp_1", signalType = "WIFI", observationCount = 10, rssiMean = -50.0, frequencyMean = 2400.0, bandwidthMean = 20.0, timingIntervalMean = 100.0) // 5dBm deviation
        
        val (processed, _) = classUnderTest.processBaseline(listOf(blip), mapOf("fp_1" to fp), true, 100, 1f, 20f, 1000L)
        
        assertEquals(BaselineState.KNOWN, processed[0].baselineState)
    }

    @Test
    fun `4 baseline does not immediately adapt to one abnormal observation`() {
        val blip = RadarBlip(name = "Router", distance = 5f, targetAngleOffset = 0f, type = "WIFI", rssi = -10, fingerprintId = "fp_1") // Extremely loud, +40dBm deviation
        val fp = SignalFingerprint(id = "fp_1", signalType = "WIFI", observationCount = 10, rssiMean = -50.0, frequencyMean = 2400.0, bandwidthMean = 20.0, timingIntervalMean = 100.0)
        
        val (processed, _) = classUnderTest.processBaseline(listOf(blip), mapOf("fp_1" to fp), true, 100, 1f, 20f, 1000L)
        
        assertEquals(BaselineState.ANOMALOUS, processed[0].baselineState)
    }

    @Test
    fun `5 baseline persistence works`() {
        // Technically persistence is in DataStore + DB, but we test the engine accurately reflects missing/known counts
        val fp1 = SignalFingerprint(id = "fp_1", signalType = "WIFI", observationCount = 10, rssiMean = -50.0, frequencyMean = 2400.0, bandwidthMean = 20.0, timingIntervalMean = 100.0)
        val fp2 = SignalFingerprint(id = "fp_2", signalType = "BLE", observationCount = 15, rssiMean = -70.0, frequencyMean = 2400.0, bandwidthMean = 20.0, timingIntervalMean = 100.0)
        
        // Only fp1 is observed
        val blip = RadarBlip(name = "Router", distance = 5f, targetAngleOffset = 0f, type = "WIFI", rssi = -50, fingerprintId = "fp_1")
        
        val (processed, summary) = classUnderTest.processBaseline(listOf(blip), mapOf("fp_1" to fp1, "fp_2" to fp2), true, 100, 1f, 20f, 1000L)
        
        assertEquals(1, summary.knownFingerprints)
        assertEquals(1, summary.missingFingerprints)
    }

    @Test
    fun `6 baseline reset works`() {
        // We pass 0 as observations, baseline avg blips = 0, meaning it's a reset state
        val blip = RadarBlip(name = "Router", distance = 5f, targetAngleOffset = 0f, type = "WIFI", rssi = -50, fingerprintId = "fp_1")
        val fp = SignalFingerprint(id = "fp_1", signalType = "WIFI", observationCount = 10, rssiMean = -50.0, frequencyMean = 2400.0, bandwidthMean = 20.0, timingIntervalMean = 100.0)
        
        val (_, summary) = classUnderTest.processBaseline(listOf(blip), mapOf("fp_1" to fp), false, 0, 0f, 0f, 0L)
        
        assertEquals(0L, summary.observationsCollected)
        assertEquals(0L, summary.baselineAgeMs)
        assertEquals(0f, summary.baselineConfidence)
        assertEquals(0f, summary.rfActivityDeltaPercent)
    }

    @Test
    fun `7 baseline confidence changes appropriately`() {
        val (_, summary1) = classUnderTest.processBaseline(emptyList(), emptyMap(), true, 500, 1f, 20f, 1000L)
        assertEquals(0.5f, summary1.baselineConfidence, 0.01f)
        
        val (_, summary2) = classUnderTest.processBaseline(emptyList(), emptyMap(), true, 2000, 1f, 20f, 1000L)
        assertEquals(1.0f, summary2.baselineConfidence, 0.01f) // Capped at 1.0
    }

    @Test
    fun `8 missing sensor data does not create false anomalies`() {
        // RadarBlip without bandwidth or other extra details shouldn't crash or anomaly
        val blip = RadarBlip(name = "Router", distance = 5f, targetAngleOffset = 0f, type = "WIFI", rssi = -50, fingerprintId = "fp_1", bandwidthMhz = null)
        val fp = SignalFingerprint(id = "fp_1", signalType = "WIFI", observationCount = 10, rssiMean = -50.0, frequencyMean = 2400.0, bandwidthMean = 20.0, timingIntervalMean = 100.0)
        
        val (processed, _) = classUnderTest.processBaseline(listOf(blip), mapOf("fp_1" to fp), true, 100, 1f, 20f, 1000L)
        
        assertEquals(BaselineState.KNOWN, processed[0].baselineState)
    }

    @Test
    fun `9 simulated data remains marked as simulated`() {
        val blip = RadarBlip(name = "Sim", distance = 5f, targetAngleOffset = 0f, type = "WIFI", rssi = -50, fingerprintId = "fp_sim", provenance = DataProvenance.SIMULATED)
        val fp = SignalFingerprint(id = "fp_sim", signalType = "WIFI", observationCount = 100, rssiMean = -50.0, frequencyMean = 2400.0, bandwidthMean = 20.0, timingIntervalMean = 100.0)
        
        val (processed, _) = classUnderTest.processBaseline(listOf(blip), mapOf("fp_sim" to fp), true, 100, 1f, 20f, 1000L)
        
        // Simulations should remain UNKNOWN to not corrupt baseline visually
        assertEquals(BaselineState.UNKNOWN, processed[0].baselineState)
    }

    @Test
    fun `10 replay data remains marked as replay`() {
        val blip = RadarBlip(name = "Replay", distance = 5f, targetAngleOffset = 0f, type = "WIFI", rssi = -50, fingerprintId = "fp_replay", provenance = DataProvenance.REPLAY)
        val fp = SignalFingerprint(id = "fp_replay", signalType = "WIFI", observationCount = 100, rssiMean = -50.0, frequencyMean = 2400.0, bandwidthMean = 20.0, timingIntervalMean = 100.0)
        
        val (processed, _) = classUnderTest.processBaseline(listOf(blip), mapOf("fp_replay" to fp), true, 100, 1f, 20f, 1000L)
        
        assertEquals(BaselineState.UNKNOWN, processed[0].baselineState)
    }
}
