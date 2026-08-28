package com.example

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ReplaySimulationTest {

    @Test
    fun testDeterministicReplayLocalization() {
        val targetId = "target_ble_007"
        val timestamp = System.currentTimeMillis()

        // 1. Construct a set of measurement points representing a walk
        val measurements1 = listOf(
            RfMeasurementPoint(UUID.randomUUID().toString(), timestamp, null, null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -80, -80.0f, 1.0f, targetId, 2400.0, 80, "WALK", MeasurementQuality.VALID),
            RfMeasurementPoint(UUID.randomUUID().toString(), timestamp + 1000, null, null, 1.0f, 0.5f, 15.0f, 0.0f, 0.0f, -74, -75.0f, 1.0f, targetId, 2400.0, 80, "WALK", MeasurementQuality.VALID),
            RfMeasurementPoint(UUID.randomUUID().toString(), timestamp + 2000, null, null, 2.0f, 1.0f, 30.0f, 0.0f, 0.0f, -68, -70.0f, 1.0f, targetId, 2400.0, 80, "WALK", MeasurementQuality.VALID),
            RfMeasurementPoint(UUID.randomUUID().toString(), timestamp + 3000, null, null, 2.0f, 2.0f, 45.0f, 0.0f, 0.0f, -62, -65.0f, 1.0f, targetId, 2400.0, 80, "WALK", MeasurementQuality.VALID),
            RfMeasurementPoint(UUID.randomUUID().toString(), timestamp + 4000, null, null, 1.0f, 2.0f, 90.0f, 0.0f, 0.0f, -66, -66.0f, 1.0f, targetId, 2400.0, 80, "WALK", MeasurementQuality.VALID),
            RfMeasurementPoint(UUID.randomUUID().toString(), timestamp + 5000, null, null, 0.0f, 2.0f, 180.0f, 0.0f, 0.0f, -70, -68.0f, 1.0f, targetId, 2400.0, 80, "WALK", MeasurementQuality.VALID)
        )

        // 2. Clone the measurements exactly to represent a perfect replay input (same target, same ordering, same spatial configuration)
        val measurements2 = measurements1.map {
            RfMeasurementPoint(
                id = UUID.randomUUID().toString(), // different measurement ID but identical attributes
                timestamp = it.timestamp,
                latitude = it.latitude,
                longitude = it.longitude,
                xOffsetMeters = it.xOffsetMeters,
                yOffsetMeters = it.yOffsetMeters,
                compassHeading = it.compassHeading,
                pitch = it.pitch,
                roll = it.roll,
                rssi = it.rssi,
                filteredRssi = it.filteredRssi,
                rssiVariance = it.rssiVariance,
                targetId = it.targetId,
                frequencyMhz = it.frequencyMhz,
                qualityScore = it.qualityScore,
                label = it.label,
                qualityState = it.qualityState
            )
        }

        // 3. Compute localization volume for the first series
        val vol1 = NextBestMeasurementEngine.estimateProbabilityVolume(measurements1, targetId)
        assertNotNull("Probability volume should be calculated for series 1", vol1)
        assertTrue("Volume 1 should be geometrically valid", vol1!!.isValid)

        // 4. Compute localization volume for the replayed series
        val vol2 = NextBestMeasurementEngine.estimateProbabilityVolume(measurements2, targetId)
        assertNotNull("Probability volume should be calculated for series 2", vol2)
        assertTrue("Volume 2 should be geometrically valid", vol2!!.isValid)

        // 5. Assert strict determinism of the underlying localization calculations
        // Output coordinates, orientations, and scales must be equivalent within double precision tolerance
        val deltaTolerance = 1e-4
        assertEquals("Determined center X coordinate must be identical", vol1.centerEnu.x.toDouble(), vol2.centerEnu.x.toDouble(), deltaTolerance)
        assertEquals("Determined center Y coordinate must be identical", vol1.centerEnu.y.toDouble(), vol2.centerEnu.y.toDouble(), deltaTolerance)
        assertEquals("Determined radius must be identical", vol1.radiusMeters.toDouble(), vol2.radiusMeters.toDouble(), deltaTolerance)
        assertEquals("Major axis must be identical", vol1.majorAxisMeters.toDouble(), vol2.majorAxisMeters.toDouble(), deltaTolerance)
        assertEquals("Minor axis must be identical", vol1.minorAxisMeters.toDouble(), vol2.minorAxisMeters.toDouble(), deltaTolerance)
        assertEquals("Ellipse orientation must be identical", vol1.ellipseOrientationDegrees.toDouble(), vol2.ellipseOrientationDegrees.toDouble(), deltaTolerance)
        assertEquals("Model consistency categorization must be identical", vol1.modelConsistency, vol2.modelConsistency)
        
        // 6. Verify that changing ordering or inputs results in responsive, non-identical estimates
        val perturbedMeasurements = measurements1.mapIndexed { idx, pt ->
            if (idx == 3) pt.copy(rssi = -95, filteredRssi = -95f) else pt
        }
        val volPerturbed = NextBestMeasurementEngine.estimateProbabilityVolume(perturbedMeasurements, targetId)
        if (volPerturbed != null && volPerturbed.isValid) {
            val isSameX = Math.abs(vol1.centerEnu.x - volPerturbed.centerEnu.x) < deltaTolerance
            val isSameY = Math.abs(vol1.centerEnu.y - volPerturbed.centerEnu.y) < deltaTolerance
            assertFalse("Perturbed measurements must result in a different localization center estimate", isSameX && isSameY)
        }
    }
}
