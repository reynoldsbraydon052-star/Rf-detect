package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class ExplainableAnomalyEngineTest {

    private lateinit var anomalyEngine: ExplainableAnomalyEngine
    private lateinit var fingerprintDb: MutableMap<String, SignalFingerprint>
    private lateinit var baselineSummary: BaselineSummary

    @Before
    fun setup() {
        anomalyEngine = ExplainableAnomalyEngine()
        fingerprintDb = mutableMapOf()
        baselineSummary = BaselineSummary(
            observationsCollected = 1000,
            rfActivityDeltaPercent = 0.0f,
            freqOccupancyDeltaPercent = 0.0f,
            baselineConfidence = 1.0f
        )
    }

    @Test
    fun testSimulatedData_IsIgnored() {
        val blip = RadarBlip(
            name = "Test",
            distance = 10f,
            targetAngleOffset = 0f,
            type = "WIFI",
            provenance = DataProvenance.SIMULATED
        )
        
        val result = anomalyEngine.evaluateAnomaly(blip, fingerprintDb, baselineSummary)
        
        assertEquals(0, result.score)
        assertEquals(0f, result.confidence)
        assertEquals(AnomalyCategory.NORMAL, result.category)
        assertTrue(result.explanations.any { it.description.contains("Simulated") })
    }

    @Test
    fun testNewFingerprint_ProducesModerateScore() {
        val blip = RadarBlip(
            name = "Test",
            distance = 10f,
            targetAngleOffset = 0f,
            type = "WIFI",
            provenance = DataProvenance.MEASURED,
            fingerprintId = null
        )
        
        val result = anomalyEngine.evaluateAnomaly(blip, fingerprintDb, baselineSummary)
        
        assertEquals(35, result.score)
        assertTrue(result.confidence < 1.0f) // Reduced confidence due to lack of history
        assertEquals(AnomalyCategory.LOW_DEVIATION, result.category)
        assertTrue(result.explanations.any { it.scoreImpact == 35 })
    }

    @Test
    fun testKnownFingerprint_MatchesBaseline_ReducesScore() {
        val fpId = "fp_123"
        val fp = SignalFingerprint(
            id = fpId,
            signalType = "WIFI",
            frequencyMean = 2412.0,
            bandwidthMean = 20.0,
            rssiMean = -50.0,
            timingIntervalMean = 100.0,
            observationCount = 50,
            provenance = DataProvenance.MEASURED
        )
        fingerprintDb[fpId] = fp

        val blip = RadarBlip(
            name = "Test",
            distance = 10f,
            targetAngleOffset = 0f,
            type = "WIFI",
            provenance = DataProvenance.MEASURED,
            fingerprintId = fpId,
            frequencyMhz = 2412.0,
            bandwidthMhz = 20.0,
            rssi = -48, // Very close
            pulseDurationMs = 105.0 // Very close
        )
        
        val result = anomalyEngine.evaluateAnomaly(blip, fingerprintDb, baselineSummary)
        
        // Starts at 0
        // Prev seen: -15
        // RSSI matches: -5
        // Total should be bounded to 0
        assertEquals(0, result.score)
        assertEquals(1.0f, result.confidence)
        assertEquals(AnomalyCategory.NORMAL, result.category)
    }

    @Test
    fun testKnownFingerprint_HighlyAnomalous_ProducesHighDeviation() {
        val fpId = "fp_123"
        val fp = SignalFingerprint(
            id = fpId,
            signalType = "WIFI",
            frequencyMean = 2412.0,
            bandwidthMean = 20.0,
            rssiMean = -50.0,
            timingIntervalMean = 100.0,
            observationCount = 50,
            provenance = DataProvenance.MEASURED
        )
        fingerprintDb[fpId] = fp

        val blip = RadarBlip(
            name = "Test",
            distance = 10f,
            targetAngleOffset = 0f,
            type = "WIFI",
            provenance = DataProvenance.MEASURED,
            fingerprintId = fpId,
            frequencyMhz = 2437.0, // Big freq diff (> 10)
            bandwidthMhz = 40.0, // Big bandwidth diff (> 10)
            rssi = -25, // Big RSSI diff (> 20)
            pulseDurationMs = 200.0 // Big timing diff
        )
        
        val result = anomalyEngine.evaluateAnomaly(blip, fingerprintDb, baselineSummary)
        
        // Starts at 0
        // Prev seen: -15
        // RSSI: +25
        // Freq: +20
        // Bandwidth: +15
        // Timing: +20
        // Total = 65
        assertEquals(65, result.score)
        assertEquals(AnomalyCategory.MODERATE_DEVIATION, result.category)
        assertEquals(5, result.explanations.size)
    }

    @Test
    fun testEnvironmentalAnomalies_AddedToScore() {
        val blip = RadarBlip(
            name = "Test",
            distance = 10f,
            targetAngleOffset = 0f,
            type = "WIFI",
            provenance = DataProvenance.MEASURED,
            fingerprintId = null // +35 score
        )
        
        val anomalousBaseline = BaselineSummary(
            observationsCollected = 1000,
            rfActivityDeltaPercent = 0.6f, // > 0.5f = +15 score
            freqOccupancyDeltaPercent = 0.6f, // > 0.5f = +15 score
            baselineConfidence = 1.0f
        )
        
        val result = anomalyEngine.evaluateAnomaly(blip, fingerprintDb, anomalousBaseline)
        
        // 35 + 15 + 15 = 65
        assertEquals(65, result.score)
        assertTrue(result.explanations.any { it.description.contains("Environmental RF activity increased") })
        assertTrue(result.explanations.any { it.description.contains("Significant environmental change") })
    }

    @Test
    fun testLowBaselineData_ReducesConfidence() {
        val blip = RadarBlip(
            name = "Test",
            distance = 10f,
            targetAngleOffset = 0f,
            type = "WIFI",
            provenance = DataProvenance.MEASURED,
            fingerprintId = null
        )
        
        val lowDataBaseline = BaselineSummary(
            observationsCollected = 10, // < 50
            rfActivityDeltaPercent = 0.0f,
            freqOccupancyDeltaPercent = 0.0f,
            baselineConfidence = 0.5f
        )
        
        val result = anomalyEngine.evaluateAnomaly(blip, fingerprintDb, lowDataBaseline)
        
        // Confidence base = 0.5
        // Fingerprint == null -> confidence *= 0.8
        // Low environmental data -> confidence *= 0.5
        // Final approx 0.2
        assertTrue(abs(result.confidence - 0.2f) < 0.01f)
    }
}
