package com.example

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class MultiSensorCorrelationEngineTest {

    private lateinit var engine: MultiSensorCorrelationEngine
    private lateinit var config: CorrelationEngineConfig

    @Before
    fun setup() {
        config = CorrelationEngineConfig(maxTemporalWindowMs = 1000L, minConfidenceThreshold = 0.5f)
        engine = MultiSensorCorrelationEngine(config)
    }

    // 1. temporally close events correlate
    @Test
    fun `test temporally close events correlate`() {
        val blip1 = RadarBlip(id = "1", name = "RF", type = "WIFI", distance = 5f, targetAngleOffset = 0f, timestampMs = 1000L)
        val blip2 = RadarBlip(id = "2", name = "Audio", type = "AUDIO", distance = 5f, targetAngleOffset = 0f, timestampMs = 1200L)
        
        engine.processObservation(blip1)
        val correlations = engine.processObservation(blip2)
        
        assertEquals(1, correlations.size)
        assertEquals(200L, correlations[0].maxTimeSeparationMs)
    }

    // 2. events outside the configured window do not correlate
    @Test
    fun `test events outside window do not correlate`() {
        val blip1 = RadarBlip(id = "1", name = "RF", type = "WIFI", distance = 5f, targetAngleOffset = 0f, timestampMs = 1000L)
        val blip2 = RadarBlip(id = "2", name = "Audio", type = "AUDIO", distance = 5f, targetAngleOffset = 0f, timestampMs = 2500L)
        
        engine.processObservation(blip1)
        val correlations = engine.processObservation(blip2)
        
        assertEquals(0, correlations.size)
    }

    // 3. repeated co-occurrence increases confidence appropriately
    @Test
    fun `test repeated co-occurrence increases confidence`() {
        // First pair
        val blip1 = RadarBlip(id = "1", name = "RF", type = "WIFI", distance = 5f, targetAngleOffset = 0f, timestampMs = 1000L)
        val blip2 = RadarBlip(id = "2", name = "Audio", type = "AUDIO", distance = 5f, targetAngleOffset = 0f, timestampMs = 1100L)
        engine.processObservation(blip1)
        val firstCorrelation = engine.processObservation(blip2).first()
        
        // Advance time to clear window
        val clearBlip = RadarBlip(id = "clear", name = "clear", type = "CLEAR", distance = 0f, targetAngleOffset = 0f, timestampMs = 5000L)
        engine.processObservation(clearBlip)
        
        // Second pair (same types)
        val blip3 = RadarBlip(id = "3", name = "RF2", type = "WIFI", distance = 5f, targetAngleOffset = 0f, timestampMs = 6000L)
        val blip4 = RadarBlip(id = "4", name = "Audio2", type = "AUDIO", distance = 5f, targetAngleOffset = 0f, timestampMs = 6100L)
        engine.processObservation(blip3)
        val secondCorrelation = engine.processObservation(blip4).first()
        
        assertTrue(secondCorrelation.confidence > firstCorrelation.confidence)
        assertTrue(secondCorrelation.correlationScore >= firstCorrelation.correlationScore)
    }

    // 4. missing timestamps are handled safely
    @Test
    fun `test missing timestamps default to now safely`() {
        // RadarBlip has a default timestampMs = System.currentTimeMillis()
        val blip1 = RadarBlip(id = "1", name = "RF", type = "WIFI", distance = 5f, targetAngleOffset = 0f)
        val blip2 = RadarBlip(id = "2", name = "Audio", type = "AUDIO", distance = 5f, targetAngleOffset = 0f)
        
        engine.processObservation(blip1)
        val correlations = engine.processObservation(blip2)
        
        assertEquals(1, correlations.size)
        assertTrue(correlations[0].maxTimeSeparationMs < 100L) // Practically immediate
    }

    // 5. spatial uncertainty is respected
    @Test
    fun `test spatial relationships are assigned correctly`() {
        val blip1 = RadarBlip(id = "1", name = "RF", type = "WIFI", distance = 5.0f, targetAngleOffset = 0f, timestampMs = 1000L)
        
        val blip2CoLocated = RadarBlip(id = "2", name = "Audio1", type = "AUDIO", distance = 6.0f, targetAngleOffset = 0f, timestampMs = 1100L)
        val blip3Proximate = RadarBlip(id = "3", name = "Audio2", type = "AUDIO", distance = 10.0f, targetAngleOffset = 0f, timestampMs = 1100L)
        val blip4Distant = RadarBlip(id = "4", name = "Audio3", type = "AUDIO", distance = 25.0f, targetAngleOffset = 0f, timestampMs = 1100L)
        
        engine.processObservation(blip1)
        
        val corrCoLocated = engine.processObservation(blip2CoLocated).first()
        assertEquals(SpatialRelationship.CO_LOCATED, corrCoLocated.spatialRelationship)
        
        val corrProximate = engine.processObservation(blip3Proximate).first { it.observations.any { obs -> obs.id == "3" } }
        assertEquals(SpatialRelationship.PROXIMATE, corrProximate.spatialRelationship)
        
        // Wait, multiple correlations might have been generated for 3 against 2 and 1
        val corrDistant = engine.processObservation(blip4Distant).first { it.observations.any { obs -> obs.id == "4" } && it.observations.any { obs -> obs.id == "1" } }
        assertEquals(SpatialRelationship.DISTANT, corrDistant.spatialRelationship)
    }

    // 6. simulated observations remain marked simulated
    @Test
    fun `test simulated observations keep simulated provenance`() {
        val blip1 = RadarBlip(id = "1", name = "RF", type = "WIFI", distance = 5f, targetAngleOffset = 0f, timestampMs = 1000L, provenance = DataProvenance.SIMULATED)
        val blip2 = RadarBlip(id = "2", name = "Audio", type = "AUDIO", distance = 5f, targetAngleOffset = 0f, timestampMs = 1100L, provenance = DataProvenance.MEASURED)
        
        engine.processObservation(blip1)
        val correlations = engine.processObservation(blip2)
        
        assertEquals(DataProvenance.SIMULATED, correlations[0].provenance)
        assertTrue(correlations[0].confidence < 1.0f) // Simulated penalized
    }

    // 7. replay observations remain marked replay
    @Test
    fun `test replay observations keep replay provenance`() {
        val blip1 = RadarBlip(id = "1", name = "RF", type = "WIFI", distance = 5f, targetAngleOffset = 0f, timestampMs = 1000L, provenance = DataProvenance.REPLAY)
        val blip2 = RadarBlip(id = "2", name = "Audio", type = "AUDIO", distance = 5f, targetAngleOffset = 0f, timestampMs = 1100L, provenance = DataProvenance.MEASURED)
        
        engine.processObservation(blip1)
        val correlations = engine.processObservation(blip2)
        
        assertEquals(DataProvenance.REPLAY, correlations[0].provenance)
    }

    // 8. correlation scoring is deterministic
    @Test
    fun `test correlation scoring is deterministic`() {
        val engine1 = MultiSensorCorrelationEngine(config)
        val engine2 = MultiSensorCorrelationEngine(config)
        
        val blip1 = RadarBlip(id = "1", name = "RF", type = "WIFI", distance = 5f, targetAngleOffset = 0f, timestampMs = 1000L)
        val blip2 = RadarBlip(id = "2", name = "Audio", type = "AUDIO", distance = 6f, targetAngleOffset = 0f, timestampMs = 1500L)
        
        engine1.processObservation(blip1)
        val corr1 = engine1.processObservation(blip2)[0]
        
        engine2.processObservation(blip1)
        val corr2 = engine2.processObservation(blip2)[0]
        
        assertEquals(corr1.correlationScore, corr2.correlationScore, 0.001f)
        assertEquals(corr1.confidence, corr2.confidence, 0.001f)
    }

    // 9. causal conclusions are never generated by the correlation engine
    @Test
    fun `test notes do not claim causation`() {
        val blip1 = RadarBlip(id = "1", name = "RF", type = "WIFI", distance = 5f, targetAngleOffset = 0f, timestampMs = 1000L)
        val blip2 = RadarBlip(id = "2", name = "Audio", type = "AUDIO", distance = 5f, targetAngleOffset = 0f, timestampMs = 1100L)
        
        engine.processObservation(blip1)
        val corr = engine.processObservation(blip2)[0]
        
        assertFalse(corr.notes.lowercase().contains("caused"))
        assertFalse(corr.notes.lowercase().contains("because"))
        assertTrue(corr.notes.contains("Correlation does not imply causation"))
    }

    // 10. unrelated sensor activity does not automatically become correlated
    @Test
    fun `test same type same name events do not correlate`() {
        // e.g. a WiFi blip updating itself
        val blip1 = RadarBlip(id = "1", name = "WIFI_AP", type = "WIFI", distance = 5f, targetAngleOffset = 0f, timestampMs = 1000L)
        val blip2 = RadarBlip(id = "2", name = "WIFI_AP", type = "WIFI", distance = 5.5f, targetAngleOffset = 0f, timestampMs = 1100L)
        
        engine.processObservation(blip1)
        val correlations = engine.processObservation(blip2)
        
        assertEquals(0, correlations.size)
    }
}
