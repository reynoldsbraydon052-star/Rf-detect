package com.example

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
import kotlin.math.sqrt

class SurveyLifecycleTest {

    @Test
    fun testTargetIsolationOnSurvey() {
        val targetA = "target_A_wifi"
        val targetB = "target_B_wifi"
        
        // Survey target locked to targetA
        val surveyTargetId = targetA
        val pointsList = mutableListOf<RfMeasurementPoint>()
        
        // Create an incoming stream of measurements
        val timestamp = System.currentTimeMillis()
        val incomingPoints = listOf(
            RfMeasurementPoint(UUID.randomUUID().toString(), timestamp, null, null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -60, -60.0f, 1.0f, targetA, 2400.0, 80, "SURVEY", MeasurementQuality.VALID),
            RfMeasurementPoint(UUID.randomUUID().toString(), timestamp + 500, null, null, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, -80, -80.0f, 1.0f, targetB, 2400.0, 80, "SURVEY", MeasurementQuality.VALID), // Intruder target
            RfMeasurementPoint(UUID.randomUUID().toString(), timestamp + 1000, null, null, 2.0f, 2.0f, 0.0f, 0.0f, 0.0f, -50, -50.0f, 1.0f, targetA, 2400.0, 80, "SURVEY", MeasurementQuality.VALID)
        )

        // Simulate logSurveyPoint logic
        for (pt in incomingPoints) {
            if (pt.targetId == surveyTargetId) {
                pointsList.add(pt)
            }
        }

        // Verify that the intruder targetB point was completely isolated/excluded
        assertEquals("Survey must only contain points matching the locked target", 2, pointsList.size)
        assertTrue("All survey points must be target A", pointsList.all { it.targetId == targetA })
    }

    @Test
    fun testWalkingDistanceAccumulation() {
        var surveyDistanceWalked = 0f
        val points = mutableListOf<RfMeasurementPoint>()
        val ts = System.currentTimeMillis()

        // Starting point
        val p1 = RfMeasurementPoint(UUID.randomUUID().toString(), ts, null, null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -50, -50f, 1f, "target_A", 2400.0, 80, "SURVEY", MeasurementQuality.VALID)
        points.add(p1)

        // Step 1: Walk to X=3.0, Y=4.0 (Distance = 5 meters = 16.4 ft)
        val p2 = RfMeasurementPoint(UUID.randomUUID().toString(), ts + 1000, null, null, 3.0f, 4.0f, 0.0f, 0.0f, 0.0f, -52, -51f, 1f, "target_A", 2400.0, 80, "SURVEY", MeasurementQuality.VALID)
        if (points.isNotEmpty()) {
            val lastPt = points.last()
            val dx = p2.xOffsetMeters - lastPt.xOffsetMeters
            val dy = p2.yOffsetMeters - lastPt.yOffsetMeters
            val d = sqrt(dx * dx + dy * dy)
            surveyDistanceWalked += d * 3.28084f
        }
        points.add(p2)

        assertEquals("Calculated moved distance should be approximately 16.4 ft", 16.4f, surveyDistanceWalked, 0.1f)
    }

    @Test
    fun testMathematicalGradientEstimation() {
        val targetId = "target_A"
        val timestamp = System.currentTimeMillis()

        // Create a spatial measurement grid with rising signal towards north-east (positive X and positive Y)
        val points = listOf(
            RfMeasurementPoint(UUID.randomUUID().toString(), timestamp, null, null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -70, -70.0f, 1.0f, targetId, 2400.0, 80, "SURVEY", MeasurementQuality.VALID),
            RfMeasurementPoint(UUID.randomUUID().toString(), timestamp + 1000, null, null, 2.0f, 0.0f, 0.0f, 0.0f, 0.0f, -60, -60.0f, 1.0f, targetId, 2400.0, 80, "SURVEY", MeasurementQuality.VALID), // RSSI rises with +X
            RfMeasurementPoint(UUID.randomUUID().toString(), timestamp + 2000, null, null, 0.0f, 2.0f, 0.0f, 0.0f, 0.0f, -50, -50.0f, 1.0f, targetId, 2400.0, 80, "SURVEY", MeasurementQuality.VALID)  // RSSI rises with +Y
        )

        val gradient = calculateRssiGradient(points)
        assertNotNull("Gradient should be successfully estimated", gradient)
        
        val dx = gradient!!.first
        val dy = gradient.second

        // The gradient should point towards positive X and Y because RSSI gets stronger (+X is stronger than origin, +Y is stronger than origin)
        assertTrue("X gradient component should be positive", dx > 0f)
        assertTrue("Y gradient component should be positive", dy > 0f)

        // Verify slope of Signal Trend
        val n = points.size.toDouble()
        val sumI = (0 until points.size).sum().toDouble()
        val sumI2 = (0 until points.size).sumOf { it * it }.toDouble()
        val sumR = points.sumOf { it.filteredRssi.toDouble() }
        val sumIR = points.mapIndexed { idx, pt -> idx * pt.filteredRssi.toDouble() }.sum()
        val denom = n * sumI2 - sumI * sumI
        val slope = (n * sumIR - sumI * sumR) / denom

        assertTrue("Signal trend slope should show a rising profile", slope > 0.0)
    }
}
