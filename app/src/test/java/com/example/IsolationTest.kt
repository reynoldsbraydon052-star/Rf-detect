package com.example

import org.junit.Assert.*
import org.junit.Test

class IsolationTest {

    @Test
    fun testBaselineIsolation() {
        val engine = EnvironmentalBaselineEngine()
        val fpDb = emptyMap<String, SignalFingerprint>()
        val blips = listOf(
            RadarBlip(name = "Test", distance = 1f, targetAngleOffset = 0f, type = "WIFI", provenance = DataProvenance.SIMULATED)
        )
        val (_, summary) = engine.processBaseline(blips, fpDb, true, 0, 0f, 0f, 0L)
        // Should not count simulated data towards baseline observation if handled correctly
    }
}
