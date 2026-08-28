package com.example

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class ArSensorFusionEngineTest {

    private fun createPoint(
        x: Float,
        y: Float,
        rssi: Float,
        rssiVar: Float = 5.0f,
        quality: Int = 85,
        label: String = "LIVE",
        timestamp: Long = System.currentTimeMillis()
    ) = RfMeasurementPoint(
        timestamp = timestamp,
        latitude = 37.7749,
        longitude = -122.4194,
        xOffsetMeters = x,
        yOffsetMeters = y,
        compassHeading = 0f,
        pitch = 0f,
        roll = 0f,
        rssi = rssi.toInt(),
        filteredRssi = rssi,
        rssiVariance = rssiVar,
        targetId = "target_1",
        frequencyMhz = 2412.0,
        qualityScore = quality,
        label = label
    )

    @Test
    fun testEmptyMeasurements() {
        // Zero measurements should return null
        val volume = NextBestMeasurementEngine.estimateProbabilityVolume(emptyList(), "target_1")
        assertNull("Zero measurements should return a null volume", volume)
    }

    @Test
    fun testInsufficientMeasurements() {
        // Having only 2 measurements should not yield sufficient confidence
        val points = listOf(
            createPoint(0f, 0f, -40f),
            createPoint(1f, 1f, -42f)
        )
        val volume = NextBestMeasurementEngine.estimateProbabilityVolume(points, "target_1")
        assertNotNull(volume)
        assertTrue("Insufficient measurements should result in unconverged state", volume!!.confidence < 0.45f)
        assertFalse(volume.isConverged)
    }

    @Test
    fun testCollinearPoints() {
        // Points perfectly aligned on a 1D straight line
        val collinearPoints = listOf(
            createPoint(0f, 0f, -40f),
            createPoint(0f, 2f, -45f),
            createPoint(0f, 4f, -50f),
            createPoint(0f, 6f, -55f),
            createPoint(0f, 8f, -60f)
        )
        val volume = NextBestMeasurementEngine.estimateProbabilityVolume(collinearPoints, "target_1")
        assertNotNull("Volume should not be null", volume)
        
        // Collinear geometry should have high major axis (unresolved direction) and small minor axis,
        // and should NOT be marked as converged because directionality is ambiguous.
        assertTrue("Major axis must be larger than minor axis for collinear geometry", volume!!.majorAxisMeters > volume.minorAxisMeters)
        assertTrue("Bearing uncertainty should be large for highly collinear spatial distribution", volume.bearingUncertaintyDegrees > 30f)
        assertFalse("Highly collinear spatial distribution should not converge", volume.isConverged)
    }

    @Test
    fun testHighVsLowVariance() {
        // Low variance RSSI points
        val lowVarPoints = listOf(
            createPoint(0f, 0f, -40f, rssiVar = 4.0f, quality = 95),
            createPoint(2f, 0f, -45f, rssiVar = 4.0f, quality = 95),
            createPoint(0f, 2f, -45f, rssiVar = 4.0f, quality = 95),
            createPoint(-2f, 0f, -48f, rssiVar = 4.0f, quality = 95),
            createPoint(0f, -2f, -48f, rssiVar = 4.0f, quality = 95)
        )
        
        // High variance RSSI points
        val highVarPoints = listOf(
            createPoint(0f, 0f, -40f, rssiVar = 64.0f, quality = 25),
            createPoint(2f, 0f, -45f, rssiVar = 64.0f, quality = 25),
            createPoint(0f, 2f, -45f, rssiVar = 64.0f, quality = 25),
            createPoint(-2f, 0f, -48f, rssiVar = 64.0f, quality = 25),
            createPoint(0f, -2f, -48f, rssiVar = 64.0f, quality = 25)
        )

        val lowVol = NextBestMeasurementEngine.estimateProbabilityVolume(lowVarPoints, "target_1")
        val highVol = NextBestMeasurementEngine.estimateProbabilityVolume(highVarPoints, "target_1")

        assertNotNull(lowVol)
        assertNotNull(highVol)

        println("DEBUG: lowVol.majorAxisMeters = ${lowVol!!.majorAxisMeters}, highVol.majorAxisMeters = ${highVol!!.majorAxisMeters}")
        println("DEBUG: lowVol.confidence = ${lowVol.confidence}, highVol.confidence = ${highVol.confidence}")

        // Mathematical check: High variance should result in a larger uncertainty bounds (axes)
        // and a lower localization confidence score.
        assertTrue("High RSSI variance must result in larger major axis", highVol!!.majorAxisMeters > lowVol!!.majorAxisMeters)
        assertTrue("High RSSI variance must result in larger minor axis", highVol.minorAxisMeters > lowVol.minorAxisMeters)
        assertTrue("Low variance should yield higher localization confidence", lowVol.confidence > highVol.confidence)
    }

    @Test
    fun testUncertaintyDecayWithInformativePoints() {
        // Starting with a few localized points
        val initialPoints = listOf(
            createPoint(0f, 0f, -40f),
            createPoint(2f, 0f, -45f),
            createPoint(0f, 2f, -45f),
            createPoint(-2f, 0f, -48f)
        )

        // Adding more spaced informative points around the source (good geometry, high quality)
        val expandedPoints = initialPoints + listOf(
            createPoint(0f, -2f, -48f),
            createPoint(3f, 3f, -50f),
            createPoint(-3f, -3f, -52f),
            createPoint(3f, -3f, -52f)
        )

        val initialVol = NextBestMeasurementEngine.estimateProbabilityVolume(initialPoints, "target_1")
        val expandedVol = NextBestMeasurementEngine.estimateProbabilityVolume(expandedPoints, "target_1")

        assertNotNull(initialVol)
        assertNotNull(expandedVol)

        // Real measurement geometry improvement must decrease the major standard deviation axis
        // and increase overall localization confidence
        assertTrue("Adding informative points must decrease the uncertainty major axis", expandedVol!!.majorAxisMeters < initialVol!!.majorAxisMeters)
        assertTrue("Adding informative points must increase confidence score", expandedVol.confidence > initialVol.confidence)
    }

    @Test
    fun testUncertaintyIncreasesWithDeterioratingQuality() {
        val baseTime = System.currentTimeMillis()
        
        // Good quality fresh points
        val freshPoints = listOf(
            createPoint(0f, 0f, -40f, quality = 95, rssiVar = 4.0f, timestamp = baseTime),
            createPoint(2f, 0f, -45f, quality = 95, rssiVar = 4.0f, timestamp = baseTime),
            createPoint(0f, 2f, -45f, quality = 95, rssiVar = 4.0f, timestamp = baseTime),
            createPoint(-2f, 0f, -48f, quality = 95, rssiVar = 4.0f, timestamp = baseTime),
            createPoint(0f, -2f, -48f, quality = 95, rssiVar = 4.0f, timestamp = baseTime)
        )

        // Same physical geometry, but deteriorating quality scores, higher RSSI variance, and stale age
        val deterioratedPoints = listOf(
            createPoint(0f, 0f, -40f, quality = 30, rssiVar = 36.0f, timestamp = baseTime - 60000),
            createPoint(2f, 0f, -45f, quality = 30, rssiVar = 36.0f, timestamp = baseTime - 60000),
            createPoint(0f, 2f, -45f, quality = 30, rssiVar = 36.0f, timestamp = baseTime - 60000),
            createPoint(-2f, 0f, -48f, quality = 30, rssiVar = 36.0f, timestamp = baseTime - 60000),
            createPoint(0f, -2f, -48f, quality = 30, rssiVar = 36.0f, timestamp = baseTime - 60000)
        )

        val freshVol = NextBestMeasurementEngine.estimateProbabilityVolume(freshPoints, "target_1")
        val badVol = NextBestMeasurementEngine.estimateProbabilityVolume(deterioratedPoints, "target_1")

        assertNotNull(freshVol)
        assertNotNull(badVol)

        // Deteriorated sensor quality and stale age must increase uncertainty radius/axes and decrease confidence
        assertTrue("Deteriorated quality/age must increase major uncertainty axis", badVol!!.majorAxisMeters > freshVol!!.majorAxisMeters)
        assertTrue("Deteriorated quality/age must reduce localization confidence", freshVol.confidence > badVol.confidence)
    }

    @Test
    fun testHuberRobustOutlierRejection() {
        // We have 4 good points clustered around center, plus 1 extreme outlier point (RSSI far too high/low for its position)
        val goodPoints = listOf(
            createPoint(0f, 0f, -40f, rssiVar = 2f, quality = 95),
            createPoint(2f, 0f, -45f, rssiVar = 2f, quality = 95),
            createPoint(0f, 2f, -45f, rssiVar = 2f, quality = 95),
            createPoint(-2f, 0f, -48f, rssiVar = 2f, quality = 95),
            createPoint(0f, -2f, -48f, rssiVar = 2f, quality = 95)
        )
        // Extreme outlier point: very far away physically, but reporting extremely high RSSI (-35dBm) which violates path-loss
        val outlierPoint = createPoint(30f, 30f, -35f, rssiVar = 2f, quality = 95)
        val combinedPoints = goodPoints + outlierPoint

        val baseVol = NextBestMeasurementEngine.estimateProbabilityVolume(goodPoints, "target_1")
        val robustVol = NextBestMeasurementEngine.estimateProbabilityVolume(combinedPoints, "target_1")

        assertNotNull(baseVol)
        assertNotNull(robustVol)

        // Robust estimator must detect the outlier (outlierCount > 0)
        assertTrue("Huber robust weighting must identify the extreme outlier", robustVol!!.outlierCount >= 1)
        
        // The center estimates should be very close because Huber weights down the extreme outlier
        val dx = robustVol.centerEnu.x - baseVol!!.centerEnu.x
        val dy = robustVol.centerEnu.y - baseVol.centerEnu.y
        val shift = sqrt(dx * dx + dy * dy)
        assertTrue("Outlier shift must be highly suppressed by Huber weights", shift < 5.0f)
    }

    @Test
    fun testCovarianceValidationAndDegenerateGeometry() {
        // Extremely collinear or minimal spatial baseline diversity points
        val collinearPoints = listOf(
            createPoint(0f, 0f, -45f),
            createPoint(0f, 0.1f, -45.1f),
            createPoint(0f, 0.2f, -45.2f),
            createPoint(0f, 0.3f, -45.3f)
        )
        val volume = NextBestMeasurementEngine.estimateProbabilityVolume(collinearPoints, "target_1")
        assertNotNull(volume)
        
        // When geometry is highly degenerate/collinear, isInsufficientSpatialDiversity must be true,
        // and covariance elements must remain positive semi-definite (symmetric and forced positive-definite via regularizer).
        assertTrue("Insufficient spatial baseline diversity must be flagged", volume!!.insufficientSpatialDiversity)
        assertTrue("CovXX must be positive regularized", volume.covXX >= 1e-4f)
        assertTrue("CovYY must be positive regularized", volume.covYY >= 1e-4f)
        val determinant = volume.covXX * volume.covYY - volume.covXY * volume.covXY
        assertTrue("Covariance matrix must remain positive semi-definite", determinant >= -1e-5f)
    }

    @Test
    fun test8ComponentConfidenceCalibration() {
        val points = listOf(
            createPoint(0f, 0f, -40f),
            createPoint(2f, 0f, -45f),
            createPoint(0f, 2f, -45f),
            createPoint(-2f, 0f, -48f),
            createPoint(0f, -2f, -48f)
        )
        val volume = NextBestMeasurementEngine.estimateProbabilityVolume(points, "target_1")
        assertNotNull(volume)

        // Verify that the 8-component fields are populated and within logical range [0..1]
        assertTrue("confMeasurementQuality must be between 0 and 1", volume!!.confMeasurementQuality in 0.0f..1.0f)
        assertTrue("confSpatialCoverage must be between 0 and 1", volume.confSpatialCoverage in 0.0f..1.0f)
        assertTrue("confModelConsistency must be between 0 and 1", volume.confModelConsistency in 0.0f..1.0f)
        assertTrue("confSampleCount must be between 0 and 1", volume.confSampleCount in 0.0f..1.0f)
        assertTrue("confTemporalFreshness must be between 0 and 1", volume.confTemporalFreshness in 0.0f..1.0f)
        assertTrue("confTargetStability must be between 0 and 1", volume.confTargetStability in 0.0f..1.0f)
        assertTrue("confPositionAccuracy must be between 0 and 1", volume.confPositionAccuracy in 0.0f..1.0f)
        assertTrue("confHeadingAccuracy must be between 0 and 1", volume.confHeadingAccuracy in 0.0f..1.0f)
        
        // Verify they are combined into confidenceScore
        assertTrue("confidenceScore must be non-negative", volume.confidenceScore >= 0.0f)
    }

    @Test
    fun testGpsUnavailableStateAndPlaceholderRejection() {
        // Test with unavailable/null GPS coordinates — should be processed perfectly
        val pointsWithNullGps = listOf(
            createPoint(0f, 0f, -40f).copy(latitude = null, longitude = null),
            createPoint(2f, 0f, -45f).copy(latitude = null, longitude = null),
            createPoint(0f, 2f, -45f).copy(latitude = null, longitude = null),
            createPoint(-2f, 0f, -48f).copy(latitude = null, longitude = null),
            createPoint(0f, -2f, -48f).copy(latitude = null, longitude = null)
        )
        val volumeNullGps = NextBestMeasurementEngine.estimateProbabilityVolume(pointsWithNullGps, "target_1")
        assertNotNull("Null/unavailable GPS must not block relative localization", volumeNullGps)
        assertTrue(volumeNullGps!!.isValid)

        // Test rejection of placeholder coordinate: 0.0 Latitude or 0.0 Longitude
        val pointsWithPlaceholderGps = listOf(
            createPoint(0f, 0f, -40f).copy(latitude = 0.0, longitude = 0.0),
            createPoint(2f, 0f, -45f).copy(latitude = 0.0, longitude = 0.0),
            createPoint(0f, 2f, -45f).copy(latitude = 0.0, longitude = 0.0)
        )
        val volumePlaceholder = NextBestMeasurementEngine.estimateProbabilityVolume(pointsWithPlaceholderGps, "target_1")
        assertNull("Points with placeholder GPS coordinates (0.0/0.0) must be rejected entirely", volumePlaceholder)

        // Test NaN / Infinity rejection in xOffsetMeters or yOffsetMeters
        val pointsWithNanOffset = listOf(
            createPoint(Float.NaN, 0f, -40f),
            createPoint(2f, Float.POSITIVE_INFINITY, -45f)
        )
        val volumeNan = NextBestMeasurementEngine.estimateProbabilityVolume(pointsWithNanOffset, "target_1")
        assertNull("Points with NaN or Infinity offsets must be rejected entirely", volumeNan)
    }

    @Test
    fun testInvalidAndLowQualityMeasurementHandling() {
        val basePoints = listOf(
            createPoint(0f, 0f, -40f),
            createPoint(2f, 0f, -45f),
            createPoint(0f, 2f, -45f),
            createPoint(-2f, 0f, -48f),
            createPoint(0f, -2f, -48f)
        )

        // 1. INVALID state rejection
        val pointsWithInvalid = basePoints + createPoint(3f, 3f, -50f).copy(qualityState = MeasurementQuality.INVALID)
        val volumeWithInvalid = NextBestMeasurementEngine.estimateProbabilityVolume(pointsWithInvalid, "target_1")
        assertNotNull(volumeWithInvalid)
        assertEquals("Invalid measurement must be rejected and not included in count", 5, volumeWithInvalid!!.totalSamples)

        // 2. LOW_QUALITY and STALE weight reduction
        // Clean high quality points
        val highQualityPoints = basePoints.map { it.copy(qualityState = MeasurementQuality.VALID, qualityScore = 95) }
        val lowQualityPoints = basePoints.map { it.copy(qualityState = MeasurementQuality.LOW_QUALITY, qualityScore = 30) }

        val highQualityVol = NextBestMeasurementEngine.estimateProbabilityVolume(highQualityPoints, "target_1")
        val lowQualityVol = NextBestMeasurementEngine.estimateProbabilityVolume(lowQualityPoints, "target_1")

        assertNotNull(highQualityVol)
        assertNotNull(lowQualityVol)

        // Low quality points must have their weight reduced (larger variance), resulting in a lower confidence score
        assertTrue(
            "Low-quality measurements must receive reduced weight leading to lower confidence score",
            highQualityVol!!.confidenceScore > lowQualityVol!!.confidenceScore
        )
    }

    @Test
    fun testLocalizationInvalidation() {
        // Trigger invalidation with non-finite coordinates or invalid covariance conditions
        val badPoints = listOf(
            createPoint(0f, 0f, -40f),
            createPoint(0.01f, 0.01f, -40f),
            createPoint(0.02f, 0.02f, -40f)
        )
        val volume = NextBestMeasurementEngine.estimateProbabilityVolume(badPoints, "target_1")
        assertNotNull(volume)
        // Highly clustered points at identical RSSI result in failure due to insufficient spatial diversity or bad fit
        assertFalse("Failure to establish spatial diversity must mark volume as invalid", volume!!.isValid)
    }
}
