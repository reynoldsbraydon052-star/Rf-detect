package com.example

import android.hardware.SensorManager
import androidx.compose.ui.geometry.Offset
import java.util.UUID
import kotlin.math.*

/**
 * Unified Phone Pose containing absolute GPS and relative ARCore/Sensor-Fused metrics.
 */
enum class AlarmState {
    NORMAL,
    APPROACHING,
    TRIGGERED,
    COOLDOWN
}

enum class MeasurementQuality {
    VALID,
    LOW_QUALITY,
    STALE,
    INVALID
}

/**
 * Unified Phone Pose containing absolute GPS and relative ARCore/Sensor-Fused metrics.
 */
data class PhonePose(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double = 0.0,
    val horizontalAccuracyMeters: Float = 3.0f,
    val altitudeAccuracyMeters: Float = 1.5f,
    val compassHeading: Float = 0f,
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val arCoreX: Float = 0f,
    val arCoreY: Float = 0f,
    val arCoreZ: Float = 0f,
    val arCoreRotation: FloatArray = floatArrayOf(0f, 0f, 0f, 1f), // quaternion [x, y, z, w]
    val arCoreTrackingState: String = "FALLBACK_GPS_SENSORS", // "TRACKING", "FALLBACK_GPS_SENSORS", "CALIBRATING"
    val stepCount: Int = 0,
    val pdrDistanceMeters: Float = 0.0f,
    val timestampMs: Long = System.currentTimeMillis()
)

data class Offset3D(val x: Float, val y: Float, val z: Float)

data class ScreenProjection(
    val screenX: Float,
    val screenY: Float,
    val distance: Float,
    val isVisible: Boolean,
    val depthRatio: Float
)

/**
 * Coordinate transformations supporting WGS84, Local ENU, ARCore local space, and Screen projections.
 */
object CoordinatesConverter {
    private const val EARTH_RADIUS = 6378137.0 // WGS-84 semi-major axis in meters

    /**
     * Rigorous validation of GPS coordinates.
     * Rejects null, NaN, Infinite, out-of-bounds, or 0.0/placeholder coordinates.
     */
    fun isValidGps(lat: Double?, lon: Double?): Boolean {
        if (lat == null || lon == null) return false
        if (lat.isNaN() || lat.isInfinite() || lon.isNaN() || lon.isInfinite()) return false
        if (abs(lat) < 1e-6 && abs(lon) < 1e-6) return false // reject exactly 0.0/near-zero placeholders
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return false
        return true
    }

    /**
     * Converts a target lat/lon/alt coordinate to local East-North-Up (ENU) coordinates
     * relative to a local reference anchor (WGS84).
     */
    fun wgs84ToEnu(
        lat: Double,
        lon: Double,
        alt: Double,
        anchorLat: Double,
        anchorLon: Double,
        anchorAlt: Double
    ): Offset3D {
        if (!isValidGps(lat, lon) || !isValidGps(anchorLat, anchorLon)) {
            // Reject placeholder coordinates by returning NaN coordinates so caller can reject immediately
            return Offset3D(Float.NaN, Float.NaN, Float.NaN)
        }

        val latRad = Math.toRadians(anchorLat)
        val dLat = Math.toRadians(lat - anchorLat)
        val dLon = Math.toRadians(lon - anchorLon)
        val dAlt = alt - anchorAlt

        // High-precision local ENU tangent plane approximation
        val x = (dLon * EARTH_RADIUS * cos(latRad)).toFloat()
        val y = (dLat * EARTH_RADIUS).toFloat()
        val z = dAlt.toFloat()

        return Offset3D(x, y, z)
    }

    /**
     * Aligns Local ENU coordinate with ARCore tracking coordinate frame using the physical compass heading.
     */
    fun enuToArCore(
        enuX: Float,
        enuY: Float,
        enuZ: Float,
        pose: PhonePose
    ): Offset3D {
        // Rotate ENU around the vertical Z axis by the calibrated compass heading
        // to match the coordinate system where -Z is forward from the initialization orientation.
        val headingRad = Math.toRadians(pose.compassHeading.toDouble())
        val cosH = cos(headingRad).toFloat()
        val sinH = sin(headingRad).toFloat()

        val localX = enuX * cosH - enuY * sinH
        val localZ = -(enuX * sinH + enuY * cosH)
        val localY = enuZ

        // Translate relative to the current ARCore camera pose
        val arX = localX - pose.arCoreX
        val arY = localY - pose.arCoreY
        val arZ = localZ - pose.arCoreZ

        return Offset3D(arX, arY, arZ)
    }

    /**
     * Projects local 3D ARCore space coordinates onto a 2D screen viewport.
     */
    fun arCoreToScreen(
        arX: Float,
        arY: Float,
        arZ: Float,
        pose: PhonePose,
        containerWidth: Float,
        containerHeight: Float,
        fovDegrees: Float = 60f
    ): ScreenProjection {
        val isBehind = arZ >= 0f

        // Horizontal rotation & projection
        val relativeAngleRad = atan2(arX.toDouble(), -arZ.toDouble())
        val relativeAngleDeg = Math.toDegrees(relativeAngleRad).toFloat()

        // Vertical elevation angle
        val elevationAngleRad = atan2(arY.toDouble(), -arZ.toDouble())
        val elevationAngleDeg = Math.toDegrees(elevationAngleRad).toFloat()

        // Apply device attitude pitch and roll adjustments
        val adjustedVerticalDeg = elevationAngleDeg + pose.pitchDeg
        val adjustedHorizontalDeg = relativeAngleDeg - pose.rollDeg * (adjustedVerticalDeg / 90f)

        val halfFov = fovDegrees / 2f
        val verticalFov = fovDegrees * (containerHeight / containerWidth.coerceAtLeast(1f))
        val halfVerticalFov = verticalFov / 2f

        val isVisibleInFov = !isBehind &&
                abs(adjustedHorizontalDeg) <= halfFov &&
                abs(adjustedVerticalDeg) <= halfVerticalFov

        val xFraction = 0.5f + (adjustedHorizontalDeg / fovDegrees)
        val yFraction = 0.5f - (adjustedVerticalDeg / verticalFov)

        val screenX = xFraction * containerWidth
        val screenY = yFraction * containerHeight

        val distance = sqrt(arX * arX + arY * arY + arZ * arZ)

        return ScreenProjection(
            screenX = screenX,
            screenY = screenY,
            distance = distance,
            isVisible = isVisibleInFov,
            depthRatio = (distance / 30f).coerceIn(0.05f, 3.0f)
        )
    }
}

/**
 * Probability volume representation of RF localization uncertainty.
 */
data class ProbabilityVolume(
    val centerEnu: Offset3D,
    val radiusMeters: Float,
    val confidenceScore: Float, // 0.0f to 1.0f
    val isConverged: Boolean,
    val totalSamples: Int,

    // High fidelity measurement-derived uncertainty fields
    val sourcePosition: Offset3D = centerEnu,
    val uncertaintyRadiusMeters: Float = radiusMeters,
    val uncertaintyEastMeters: Float = radiusMeters,
    val uncertaintyNorthMeters: Float = radiusMeters,
    val covXX: Float = radiusMeters * radiusMeters,
    val covYY: Float = radiusMeters * radiusMeters,
    val covXY: Float = 0f,
    val bearingDegrees: Float = 0f,
    val bearingUncertaintyDegrees: Float = 45f,
    val confidence: Float = confidenceScore,
    val measurementCount: Int = totalSamples,
    val spatialCoverageMeters: Float = radiusMeters,
    val modelResidual: Float = 0f,
    val measurementQuality: Float = 50f,
    val timestamp: Long = System.currentTimeMillis(),
    val modelType: String = "Bayesian Posterior Estimation & Path Loss Model",

    val majorAxisMeters: Float = radiusMeters,
    val minorAxisMeters: Float = radiusMeters,
    val ellipseOrientationDegrees: Float = 0f,

    // UPGRADE FEATURE: RF LOCALIZATION UNCERTAINTY VALIDATION 1.0 fields
    val referenceRssi: Float = -35f,
    val referenceDistance: Float = 1.0f,
    val pathLossExponent: Float = 3.0f,
    val environmentType: String = "INDOOR_TYPICAL", // FREE_SPACE, INDOOR_TYPICAL, CUSTOM, UNKNOWN
    val modelConsistency: String = "FAIR", // GOOD, FAIR, POOR, INVALID
    val rmse: Float = 0f,
    val medianAbsoluteResidual: Float = 0f,
    val maxResidual: Float = 0f,
    val outlierCount: Int = 0,
    val effectiveSampleCount: Float = totalSamples.toFloat(),
    val isValid: Boolean = true,
    val errorMessage: String = "",
    val insufficientSpatialDiversity: Boolean = false,

    // Confidence components
    val confMeasurementQuality: Float = 0f,
    val confSpatialCoverage: Float = 0f,
    val confModelConsistency: Float = 0f,
    val confSampleCount: Float = 0f,
    val confTargetStability: Float = 0f,
    val confPositionAccuracy: Float = 0f,
    val confHeadingAccuracy: Float = 0f,
    val confTemporalFreshness: Float = 0f,

    // Sigmas
    val sigmaLevel: String = "2-SIGMA", // "1-SIGMA", "2-SIGMA", "3-SIGMA"

    // Model Status (Goal 8)
    val modelStatus: String = "INFERRED",

    // Outlier status for individual measurements (by index)
    val isOutlierList: List<Boolean> = emptyList()
) {
    fun label(): String = when {
        !isValid -> "LOCALIZATION INVALID"
        insufficientSpatialDiversity -> "INSUFFICIENT SPATIAL DIVERSITY"
        confidenceScore >= 0.8f -> "INFERRED (HIGH CONFIDENCE)"
        confidenceScore >= 0.5f -> "INFERRED (MEDIUM CONFIDENCE)"
        confidenceScore >= 0.2f -> "ESTIMATED (LOW CONFIDENCE)"
        else -> "UNKNOWN / CALIBRATING"
    }
}

/**
 * Directs movement to optimize the signal gradient.
 */
object NextBestMeasurementEngine {

    data class Guidance(
        val recommendation: String,
        val distanceSuggestionMeters: Float,
        val targetDirectionDegrees: Float,
        val rationale: String,
        val expectedUncertaintyMetersBefore: Float = 0f,
        val expectedUncertaintyMetersAfter: Float = 0f,
        val hasValidRecommendation: Boolean = false
    )

    /**
     * Computes the signal spatial gradient vector and advises the user.
     */
    fun calculateGuidance(
        points: List<RfMeasurementPoint>,
        currentHeading: Float
    ): Guidance {
        val targetId = points.firstOrNull()?.targetId ?: ""
        val targetPoints = points.filter { it.targetId == targetId }.filter { pt ->
            val isInvalid = pt.rssi > 0 || pt.rssi < -125 ||
                    pt.xOffsetMeters.isNaN() || pt.xOffsetMeters.isInfinite() ||
                    pt.yOffsetMeters.isNaN() || pt.yOffsetMeters.isInfinite() ||
                    pt.qualityState == MeasurementQuality.INVALID
            !isInvalid
        }

        if (targetPoints.size < 4) {
            return Guidance(
                recommendation = "INSUFFICIENT DATA",
                distanceSuggestionMeters = 0.0f,
                targetDirectionDegrees = 0.0f,
                rationale = "At least 4 valid spatial points are required to compute a stable covariance matrix and justify a deterministic recommendation.",
                expectedUncertaintyMetersBefore = 0.0f,
                expectedUncertaintyMetersAfter = 0.0f,
                hasValidRecommendation = false
            )
        }

        val currentVolume = estimateProbabilityVolume(targetPoints, targetId)
        if (currentVolume == null || !currentVolume.isValid) {
            return Guidance(
                recommendation = "INSUFFICIENT DATA",
                distanceSuggestionMeters = 0.0f,
                targetDirectionDegrees = 0.0f,
                rationale = "Current localization solver failed. Please expand spatial dispersion to stabilize covariance matrix.",
                expectedUncertaintyMetersBefore = 0.0f,
                expectedUncertaintyMetersAfter = 0.0f,
                hasValidRecommendation = false
            )
        }

        // Helper function to evaluate coordinate safety
        fun isCoordinateSafe(x: Float, y: Float): Boolean {
            // Virtual road/restricted area:
            if (y in 5.0f..7.0f) return false // virtual road
            if (x in -6.0f..-4.0f && y in -3.0f..-1.0f) return false // virtual hazardous obstacle / restricted area
            if (abs(x) > 15f || abs(y) > 15f) return false // out of safe boundary
            return true
        }

        val candidates = listOf(
            Triple(0f, "N", "↑"),
            Triple(45f, "NE", "↗"),
            Triple(90f, "E", "→"),
            Triple(135f, "SE", "↘"),
            Triple(180f, "S", "↓"),
            Triple(225f, "SW", "↙"),
            Triple(270f, "W", "←"),
            Triple(315f, "NW", "↖")
        )

        val latestPoint = targetPoints.maxByOrNull { it.timestamp }!!
        val srcX = currentVolume.sourcePosition.x
        val srcY = currentVolume.sourcePosition.y

        val distanceFt = 8.0f
        val distanceMeters = distanceFt / 3.28084f // ~2.44 meters

        var bestCandidate: Triple<Float, String, String>? = null
        var bestNewUncertainty = currentVolume.radiusMeters
        var bestScore = -9999f
        var bestRationale = "Increase spatial coverage"

        candidates.forEach { cand ->
            val (deg, dirName, arrow) = cand
            val angleRad = Math.toRadians(deg.toDouble())
            val candX = latestPoint.xOffsetMeters + distanceMeters * sin(angleRad).toFloat()
            val candY = latestPoint.yOffsetMeters + distanceMeters * cos(angleRad).toFloat()

            if (isCoordinateSafe(candX, candY)) {
                val dSource = sqrt((candX - srcX).pow(2) + (candY - srcY).pow(2)).coerceAtLeast(0.1f)
                // Predict RSSI based on log-distance path loss model
                val predictedRssi = (-30.0f - 10.0f * 3.0f * log10(dSource.toDouble() / 1.0)).toFloat()

                val simulatedPoint = RfMeasurementPoint(
                    timestamp = System.currentTimeMillis() + 1000,
                    latitude = null,
                    longitude = null,
                    xOffsetMeters = candX,
                    yOffsetMeters = candY,
                    compassHeading = latestPoint.compassHeading,
                    pitch = latestPoint.pitch,
                    roll = latestPoint.roll,
                    rssi = predictedRssi.toInt().coerceIn(-100, -30),
                    filteredRssi = predictedRssi,
                    rssiVariance = 1.0f,
                    targetId = targetId,
                    frequencyMhz = latestPoint.frequencyMhz,
                    qualityScore = 95,
                    label = "SIMULATED",
                    qualityState = MeasurementQuality.VALID
                )

                val testPoints = targetPoints + simulatedPoint
                val newVol = estimateProbabilityVolume(testPoints, targetId)
                if (newVol != null && newVol.isValid && !newVol.insufficientSpatialDiversity) {
                    // Calculate simulated spread
                    var maxD = 0f
                    for (i in testPoints.indices) {
                        for (j in i + 1 until testPoints.size) {
                            val dx = testPoints[i].xOffsetMeters - testPoints[j].xOffsetMeters
                            val dy = testPoints[i].yOffsetMeters - testPoints[j].yOffsetMeters
                            val d = sqrt(dx * dx + dy * dy)
                            if (d > maxD) maxD = d
                        }
                    }

                    // Score candidate based on uncertainty reduction and spread expansion
                    val reduction = currentVolume.radiusMeters - newVol.radiusMeters
                    val spreadExpansion = maxD - currentVolume.spatialCoverageMeters
                    val score = reduction * 2.0f + spreadExpansion

                    if (score > bestScore) {
                        bestScore = score
                        bestNewUncertainty = newVol.radiusMeters
                        bestCandidate = cand
                        bestRationale = if (spreadExpansion > 0.1f) "Increase spatial coverage" else "Minimize model covariance eigenvalues"
                    }
                }
            }
        }

        if (bestCandidate != null) {
            val (deg, dirName, arrow) = bestCandidate!!
            return Guidance(
                recommendation = "$arrow MOVE ${distanceFt.toInt()} ft $dirName",
                distanceSuggestionMeters = distanceMeters,
                targetDirectionDegrees = deg,
                rationale = bestRationale,
                expectedUncertaintyMetersBefore = currentVolume.radiusMeters,
                expectedUncertaintyMetersAfter = bestNewUncertainty,
                hasValidRecommendation = true
            )
        } else {
            return Guidance(
                recommendation = "INSUFFICIENT DATA",
                distanceSuggestionMeters = 0.0f,
                targetDirectionDegrees = 0.0f,
                rationale = "No safe movement trajectory improves the current spatial geometry or uncertainty model bounds.",
                expectedUncertaintyMetersBefore = 0.0f,
                expectedUncertaintyMetersAfter = 0.0f,
                hasValidRecommendation = false
            )
        }
    }

    /**
     * Calculates the probability volume center and radius from measurement points using
     * a mathematically rigorous 2D Bayesian Grid-Search and Log-Distance Path Loss Model.
     * Incorporates Huber weighting, explicit propagation environment models, residual analysis,
     * multipath uncertainty scaling, degenerate geometry detection, and strict covariance validation.
     */
    fun estimateProbabilityVolume(
        points: List<RfMeasurementPoint>,
        activeTargetId: String?
    ): ProbabilityVolume? {
        val targetPoints = points.filter { it.targetId == activeTargetId }.filter { pt ->
            // Reject if invalid values, placeholder coordinates, or explicitly marked INVALID
            val isInvalid = pt.rssi > 0 || pt.rssi < -125 ||
                    pt.xOffsetMeters.isNaN() || pt.xOffsetMeters.isInfinite() ||
                    pt.yOffsetMeters.isNaN() || pt.yOffsetMeters.isInfinite() ||
                    (pt.latitude != null && (pt.latitude == 0.0 || pt.latitude.isNaN() || pt.latitude.isInfinite() || pt.latitude !in -90.0..90.0)) ||
                    (pt.longitude != null && (pt.longitude == 0.0 || pt.longitude.isNaN() || pt.longitude.isInfinite() || pt.longitude !in -180.0..180.0)) ||
                    pt.qualityState == MeasurementQuality.INVALID
            !isInvalid
        }
        if (targetPoints.isEmpty()) return null

        val measurementCount = targetPoints.size
        val timestamp = System.currentTimeMillis()

        // 1. Determine Path Loss Model & Environment parameters (Requirement 2)
        // Check labels for explicit environment hints: FREE_SPACE, INDOOR_TYPICAL, CUSTOM, UNKNOWN
        var envType = "INDOOR_TYPICAL"
        var nPathLoss = 3.0f
        var refRssi = -30.0f
        val refDist = 1.0f // 1.0m typical reference distance

        // Check if any point explicitly suggests an environment
        val hasFreeSpaceHint = targetPoints.any { it.label.contains("FREE_SPACE", ignoreCase = true) }
        val hasIndoorHint = targetPoints.any { it.label.contains("INDOOR", ignoreCase = true) }
        val hasCustomHint = targetPoints.any { it.label.contains("CUSTOM", ignoreCase = true) }
        val hasUnknownHint = targetPoints.any { it.label.contains("UNKNOWN", ignoreCase = true) }

        if (hasFreeSpaceHint) {
            envType = "FREE_SPACE"
            nPathLoss = 2.0f
            refRssi = -35.0f
        } else if (hasCustomHint) {
            envType = "CUSTOM"
            nPathLoss = 2.5f
            refRssi = -32.0f
        } else if (hasUnknownHint) {
            envType = "UNKNOWN"
            nPathLoss = 2.0f
            refRssi = -40.0f
        } else if (hasIndoorHint) {
            envType = "INDOOR_TYPICAL"
            nPathLoss = 3.0f
            refRssi = -30.0f
        } else {
            // Autonomous classification based on frequency or spatial dispersion
            val avgFreq = targetPoints.map { it.frequencyMhz }.average()
            if (avgFreq > 5000.0) {
                // 5GHz signals experience higher indoor attenuation
                envType = "INDOOR_TYPICAL"
                nPathLoss = 3.3f
                refRssi = -28.0f
            } else if (targetPoints.any { it.rssiVariance > 45.0f }) {
                // High variance indicates an unstable environment
                envType = "UNKNOWN"
                nPathLoss = 2.0f
                refRssi = -40.0f
            } else {
                envType = "INDOOR_TYPICAL"
                nPathLoss = 3.0f
                refRssi = -30.0f
            }
        }

        // 2. Calculate Spatial Coverage & Dispersion
        var maxDistance = 0.0f
        for (i in targetPoints.indices) {
            for (j in i + 1 until targetPoints.size) {
                val dx = targetPoints[i].xOffsetMeters - targetPoints[j].xOffsetMeters
                val dy = targetPoints[i].yOffsetMeters - targetPoints[j].yOffsetMeters
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > maxDistance) {
                    maxDistance = dist
                }
            }
        }
        val spatialCoverageMeters = maxDistance

        // 3. Compute Spatial Center baseline
        var sumW = 0f
        var sumX = 0f
        var sumY = 0f
        var sumRssi = 0f
        targetPoints.forEach { pt ->
            val normRssi = (pt.filteredRssi + 100f).coerceAtLeast(1f)
            val w = normRssi.pow(2.0f)
            sumW += w
            sumX += pt.xOffsetMeters * w
            sumY += pt.yOffsetMeters * w
            sumRssi += pt.filteredRssi
        }
        val fallbackX = if (sumW > 0) sumX / sumW else 0f
        val fallbackY = if (sumW > 0) sumY / sumW else 0f
        val avgRssi = sumRssi / measurementCount

        // 4. Degenerate Geometry Detection (Requirement 9)
        var ptSumX = 0f
        var ptSumY = 0f
        targetPoints.forEach {
            ptSumX += it.xOffsetMeters
            ptSumY += it.yOffsetMeters
        }
        val ptMeanX = ptSumX / measurementCount
        val ptMeanY = ptSumY / measurementCount

        var ptCovXX = 0f
        var ptCovYY = 0f
        var ptCovXY = 0f
        targetPoints.forEach { pt ->
            val dx = pt.xOffsetMeters - ptMeanX
            val dy = pt.yOffsetMeters - ptMeanY
            ptCovXX += dx * dx
            ptCovYY += dy * dy
            ptCovXY += dx * dy
        }
        val ptTrace = ptCovXX + ptCovYY
        val ptTerm = sqrt(((ptCovXX - ptCovYY) / 2.0).pow(2.0) + ptCovXY.toDouble().pow(2.0)).toFloat()
        val ptLambda1 = (ptTrace / 2f) + ptTerm
        val ptLambda2 = (ptTrace / 2f) - ptTerm

        // Collinearity value ranges from 0 (isotropic) to 1 (perfect line)
        val collinearity = if (ptLambda1 > 0f) (1.0f - (ptLambda2 / ptLambda1)).coerceIn(0f, 1f) else 1f

        // Check degenerate flags
        val tooFewMeasurements = measurementCount < 3
        val tinySpatialSpread = spatialCoverageMeters < 1.0f
        val isHighlyCollinear = collinearity > 0.90f
        val isHighlyClustered = ptLambda1 < 0.25f // extremely narrow spatial concentration
        
        val insufficientSpatialDiversity = tooFewMeasurements || tinySpatialSpread || isHighlyCollinear || isHighlyClustered

        // 5. Grid-Search Optimization / Bayesian Posterior with Robust Huber Weighting (Requirement 4)
        val xCoords = targetPoints.map { it.xOffsetMeters }
        val yCoords = targetPoints.map { it.yOffsetMeters }
        val minX = (xCoords.minOrNull() ?: 0f) - 15f
        val maxX = (xCoords.maxOrNull() ?: 0f) + 15f
        val minY = (yCoords.minOrNull() ?: 0f) - 15f
        val maxY = (yCoords.maxOrNull() ?: 0f) + 15f

        val gridSteps = 31
        val stepX = (maxX - minX) / (gridSteps - 1).coerceAtLeast(1)
        val stepY = (maxY - minY) / (gridSteps - 1).coerceAtLeast(1)

        val chi2Grid = FloatArray(gridSteps * gridSteps)
        val gridX = FloatArray(gridSteps * gridSteps)
        val gridY = FloatArray(gridSteps * gridSteps)
        var minChi2 = Float.MAX_VALUE

        // Precompute point variances (incorporating tracking quality and measurement age)
        val ptTotalVariances = FloatArray(measurementCount)
        for (i in 0 until measurementCount) {
            val pt = targetPoints[i]
            val rssiVar = pt.rssiVariance.coerceIn(4f, 100f)
            val sensorVar = if (pt.label.contains("LIVE", ignoreCase = true)) 1f else 3f
            val ageMs = timestamp - pt.timestamp
            val ageVar = (ageMs / 25000f).coerceAtLeast(0f) * 2f // stale age penalty
            val qualityPenalty = when (pt.qualityState) {
                MeasurementQuality.LOW_QUALITY -> 15f
                MeasurementQuality.STALE -> 25f
                else -> 0f
            }
            ptTotalVariances[i] = rssiVar + sensorVar + ageVar + qualityPenalty
        }

        // Fast centroid pre-filter of outliers before grid search
        val cleanIndices = ArrayList<Int>()
        var preOutliersCount = 0
        var centA = refRssi
        for (iter in 0 until 3) {
            var sumAWeights = 0f
            var sumA = 0f
            for (i in 0 until measurementCount) {
                val pt = targetPoints[i]
                val dx = pt.xOffsetMeters - fallbackX
                val dy = pt.yOffsetMeters - fallbackY
                val dist = sqrt(dx * dx + dy * dy + 0.25f)
                val predRssi = centA - nPathLoss * 10f * log10(dist.toDouble().coerceAtLeast(0.1)).toFloat()
                val residual = pt.filteredRssi - predRssi
                val normalizedResidual = residual / sqrt(ptTotalVariances[i])
                val wHuber = if (abs(normalizedResidual) > 1.8f) 1.8f / abs(normalizedResidual) else 1.0f
                val weight = (1f / ptTotalVariances[i]) * wHuber
                sumAWeights += weight
                sumA += (pt.filteredRssi + nPathLoss * 10f * log10(dist.toDouble().coerceAtLeast(0.1))).toFloat() * weight
            }
            centA = if (sumAWeights > 0f) sumA / sumAWeights else refRssi
        }

        for (i in 0 until measurementCount) {
            val pt = targetPoints[i]
            val dx = pt.xOffsetMeters - fallbackX
            val dy = pt.yOffsetMeters - fallbackY
            val dist = sqrt(dx * dx + dy * dy + 0.25f)
            val predRssi = centA - nPathLoss * 10f * log10(dist.toDouble().coerceAtLeast(0.1)).toFloat()
            val residual = pt.filteredRssi - predRssi
            val normalizedResidual = residual / sqrt(ptTotalVariances[i])
            if (abs(normalizedResidual) > 3.0f && (measurementCount - preOutliersCount) >= 4) {
                preOutliersCount++
            } else {
                cleanIndices.add(i)
            }
        }

        // Perform Grid Search using Huber weights, restricted to clean (non-outlier) points
        // Apply a Bayesian prior penalty on A (prior variance = 36.0 dB^2, centered at refRssi) to prevent unphysical values
        for (gxIdx in 0 until gridSteps) {
            val gx = minX + gxIdx * stepX
            for (gyIdx in 0 until gridSteps) {
                val gy = minY + gyIdx * stepY
                val idx = gxIdx * gridSteps + gyIdx
                gridX[idx] = gx
                gridY[idx] = gy

                // Iterative Huber Weighting Pass with prior on A
                var currentA = refRssi
                val maxHuberIterations = 2
                for (iter in 0 until maxHuberIterations) {
                    var sumAWeights = 1.0f / 36.0f // prior contribution
                    var sumA = refRssi / 36.0f
                    for (i in cleanIndices) {
                        val pt = targetPoints[i]
                        val dx = pt.xOffsetMeters - gx
                        val dy = pt.yOffsetMeters - gy
                        val dist = sqrt(dx * dx + dy * dy + 0.25f)
                        
                        // Path-loss predicted RSSI
                        val predRssi = currentA - nPathLoss * 10f * log10(dist.toDouble().coerceAtLeast(0.1)).toFloat()
                        val residual = pt.filteredRssi - predRssi
                        val normalizedResidual = residual / sqrt(ptTotalVariances[i])
                        
                        // Huber weight multiplier
                        val wHuber = if (abs(normalizedResidual) > 1.8f) 1.8f / abs(normalizedResidual) else 1.0f
                        val weight = (1f / ptTotalVariances[i]) * wHuber
                        
                        sumAWeights += weight
                        sumA += (pt.filteredRssi + nPathLoss * 10f * log10(dist.toDouble().absoluteValue.coerceAtLeast(0.1))).toFloat() * weight
                    }
                    currentA = if (sumAWeights > 0) sumA / sumAWeights else refRssi
                }

                // Compute final Huber-weighted Chi-squared residual with Bayesian prior penalty for this cell
                var chi2 = ((currentA - refRssi) * (currentA - refRssi)) / 36.0f
                for (i in cleanIndices) {
                    val pt = targetPoints[i]
                    val dx = pt.xOffsetMeters - gx
                    val dy = pt.yOffsetMeters - gy
                    val dist = sqrt(dx * dx + dy * dy + 0.25f)
                    val predRssi = currentA - nPathLoss * 10f * log10(dist.toDouble().coerceAtLeast(0.1)).toFloat()
                    val diff = pt.filteredRssi - predRssi
                    val normalizedDiff = diff / sqrt(ptTotalVariances[i])
                    
                    val wHuber = if (abs(normalizedDiff) > 1.8f) 1.8f / abs(normalizedDiff) else 1.0f
                    chi2 += (diff * diff * wHuber) / ptTotalVariances[i]
                }
                
                chi2Grid[idx] = chi2
                if (chi2 < minChi2) {
                    minChi2 = chi2
                }
            }
        }

        // Convert grid residuals to posterior probabilities with 3-sigma gating to avoid linear pull from far outliers
        val likelihoods = DoubleArray(gridSteps * gridSteps)
        var sumLikelihood = 0.0
        for (i in likelihoods.indices) {
            val deltaChi2 = chi2Grid[i] - minChi2
            if (deltaChi2 < 9.0f) { // 3-sigma gate (chi2 residual change threshold)
                likelihoods[i] = exp(-0.5 * deltaChi2.toDouble())
            } else {
                likelihoods[i] = 0.0
            }
            sumLikelihood += likelihoods[i]
        }

        // 6. Compute spatial expectation (mean) and raw covariance matrix
        var estX = fallbackX
        var estY = fallbackY
        var covXX = 4f
        var covYY = 4f
        var covXY = 0f

        if (sumLikelihood > 0.0) {
            var weightedSumX = 0.0
            var weightedSumY = 0.0
            for (i in likelihoods.indices) {
                val p = likelihoods[i] / sumLikelihood
                weightedSumX += gridX[i] * p
                weightedSumY += gridY[i] * p
            }
            estX = weightedSumX.toFloat()
            estY = weightedSumY.toFloat()

            var weightedVarX = 0.0
            var weightedVarY = 0.0
            var weightedCovXY = 0.0
            for (i in likelihoods.indices) {
                val p = likelihoods[i] / sumLikelihood
                val dx = gridX[i] - estX
                val dy = gridY[i] - estY
                weightedVarX += dx * dx * p
                weightedVarY += dy * dy * p
                weightedCovXY += dx * dy * p
            }
            covXX = weightedVarX.toFloat().coerceAtLeast(0.05f)
            covYY = weightedVarY.toFloat().coerceAtLeast(0.05f)
            covXY = weightedCovXY.toFloat()
        }

        // 7. Robust fit on the estimated center to derive residuals & classify outliers (Requirement 3 & 4)
        var finalA = refRssi
        val maxFinalAIterations = 2
        for (iter in 0 until maxFinalAIterations) {
            var sumAWeights = 0f
            var sumA = 0f
            for (i in 0 until measurementCount) {
                val pt = targetPoints[i]
                val dx = pt.xOffsetMeters - estX
                val dy = pt.yOffsetMeters - estY
                val dist = sqrt(dx * dx + dy * dy + 0.25f)
                
                val predRssi = finalA - nPathLoss * 10f * log10(dist.toDouble().coerceAtLeast(0.1)).toFloat()
                val residual = pt.filteredRssi - predRssi
                val normalizedResidual = residual / sqrt(ptTotalVariances[i])
                
                val wHuber = if (abs(normalizedResidual) > 1.8f) 1.8f / abs(normalizedResidual) else 1.0f
                val weight = (1f / ptTotalVariances[i]) * wHuber
                
                sumAWeights += weight
                sumA += (pt.filteredRssi + nPathLoss * 10f * log10(dist.toDouble().absoluteValue.coerceAtLeast(0.1))).toFloat() * weight
            }
            finalA = if (sumAWeights > 0) sumA / sumAWeights else refRssi
        }

        val predictedRssiList = ArrayList<Float>()
        val observedRssiList = ArrayList<Float>()
        val residualList = ArrayList<Float>()
        val absoluteResidualList = ArrayList<Float>()
        val weightedResidualList = ArrayList<Float>()
        val isOutlierList = ArrayList<Boolean>()
        
        var totalSqErr = 0f
        var sumAbsoluteErr = 0f
        var maxResid = 0f
        var outlierCount = 0
        var effectiveSampleCount = 0f

        for (i in 0 until measurementCount) {
            val pt = targetPoints[i]
            val dx = pt.xOffsetMeters - estX
            val dy = pt.yOffsetMeters - estY
            val dist = sqrt(dx * dx + dy * dy + 0.25f)
            
            val pred = finalA - nPathLoss * 10f * log10(dist.toDouble().coerceAtLeast(0.1)).toFloat()
            val observed = pt.filteredRssi
            val resid = observed - pred
            val absResid = abs(resid)
            val wResid = resid / sqrt(ptTotalVariances[i])
            
            // Huber outlier threshold (Requirement 4)
            val wHuber = if (abs(wResid) > 1.8f) 1.8f / abs(wResid) else 1.0f
            val isOutlier = wHuber < 0.8f
            
            if (isOutlier) {
                outlierCount++
            }
            effectiveSampleCount += wHuber

            predictedRssiList.add(pred)
            observedRssiList.add(observed)
            residualList.add(resid)
            absoluteResidualList.add(absResid)
            weightedResidualList.add(wResid)
            isOutlierList.add(isOutlier)

            totalSqErr += resid * resid
            sumAbsoluteErr += absResid
            if (absResid > maxResid) {
                maxResid = absResid
            }
        }

        val rmse = sqrt(targetPoints.mapIndexed { idx, pt ->
            val res = residualList[idx]
            res * res
        }.average().toFloat().coerceAtLeast(0.01f))

        // Median absolute residual
        val sortedAbsResiduals = absoluteResidualList.sorted()
        val medianAbsoluteResidual = if (sortedAbsResiduals.isNotEmpty()) {
            if (sortedAbsResiduals.size % 2 == 1) {
                sortedAbsResiduals[sortedAbsResiduals.size / 2]
            } else {
                (sortedAbsResiduals[sortedAbsResiduals.size / 2 - 1] + sortedAbsResiduals[sortedAbsResiduals.size / 2]) / 2f
            }
        } else 0f

        // Classify Model Consistency (Requirement 3)
        val modelConsistency = when {
            insufficientSpatialDiversity -> "INVALID"
            rmse < 4.5f -> "GOOD"
            rmse < 8.0f -> "FAIR"
            rmse < 15.0f -> "POOR"
            else -> "INVALID"
        }

        // 8. Multipath / Environmental Uncertainty Expansion (Requirement 5)
        // Inflate covariance if high residual RMSE indicates instability or multipath interference
        val inflationFactor = (1.0f + rmse / 5.0f).coerceIn(1.0f, 4.0f)
        val avgMeasurementVar = targetPoints.map { it.rssiVariance }.average().toFloat().coerceIn(4f, 100f)
        val varMultiplier = sqrt(avgMeasurementVar / 4.0f)
        covXX *= inflationFactor * inflationFactor * varMultiplier * varMultiplier
        covYY *= inflationFactor * inflationFactor * varMultiplier * varMultiplier
        covXY *= inflationFactor * inflationFactor * varMultiplier * varMultiplier

        // 9. Covariance Matrix Validation (Requirement 6)
        val isCovFinite = !(covXX.isNaN() || covXX.isInfinite() || covYY.isNaN() || covYY.isInfinite() || covXY.isNaN() || covXY.isInfinite())
        val isCovSymmetric = true // constructed symmetrically
        val covDet = covXX * covYY - covXY * covXY
        val isCovPositiveSemiDefinite = isCovFinite && (covDet >= -1e-5f) && (covXX >= 0f) && (covYY >= 0f)

        // Eigenvalue solver for standard deviations
        val trace = covXX + covYY
        val term = sqrt(((covXX - covYY) / 2.0).pow(2.0) + covXY.toDouble().pow(2.0)).toFloat()
        val lambda1 = (trace / 2f) + term
        val lambda2 = (trace / 2f) - term

        val isEigenvaluesNonNegative = isCovFinite && (lambda1 >= -1e-5f) && (lambda2 >= -1e-5f)
        
        val majorMetersRaw = sqrt(lambda1.coerceAtLeast(0.01f))
        val minorMetersRaw = sqrt(lambda2.coerceAtLeast(0.01f))
        val isMajorGreaterOrEqual = majorMetersRaw >= (minorMetersRaw - 1e-4f)

        val isCovValid = isCovFinite && isCovSymmetric && isCovPositiveSemiDefinite && isEigenvaluesNonNegative && isMajorGreaterOrEqual && !insufficientSpatialDiversity

        // If validation fails, mark solution as invalid and do not allow visual rendering
        val finalIsValid = isCovValid && (modelConsistency != "INVALID")
        val errorMsg = when {
            insufficientSpatialDiversity -> "INSUFFICIENT SPATIAL DIVERSITY"
            !isCovFinite -> "COVARIANCE FINITE VALIDATION FAILED"
            !isCovPositiveSemiDefinite -> "COVARIANCE POSITIVE SEMI-DEFINITE FAILED"
            !isEigenvaluesNonNegative -> "EIGENVALUES MUST BE NON-NEGATIVE"
            !isMajorGreaterOrEqual -> "MAJOR AXIS MUST BE GREATER THAN MINOR"
            modelConsistency == "INVALID" -> "MODEL INCONSISTENT / EXCESSIVE RESIDUALS"
            else -> ""
        }

        // 10. Eigenvalue-derived Axes and orientation
        val majorMeters = majorMetersRaw
        val minorMeters = minorMetersRaw
        val orientationRad = 0.5 * atan2(2.0 * covXY, (covXX - covYY).toDouble())
        val ellipseOrientationDegrees = ((Math.toDegrees(orientationRad).toFloat() + 360f) % 360f)

        // 11. Confidence Calibration: 8 separate sub-components (Requirement 7)
        val confMeasurementQuality = (targetPoints.map { it.qualityScore.toFloat() }.average().toFloat().coerceIn(0f, 100f) / 100f)
        val confSpatialCoverage = (spatialCoverageMeters / 12.0f).coerceIn(0.1f, 1.0f)
        val confModelConsistency = (6.0f / (6.0f + rmse)).coerceIn(0.1f, 1.0f)
        val confSampleCount = (measurementCount.toFloat() / 10f).coerceIn(0.1f, 1.0f)
        
        val targetStability = 1.0f - (targetPoints.map { it.rssiVariance }.average().toFloat() / 80f).coerceIn(0f, 0.5f)
        val confTargetStability = targetStability.coerceIn(0.1f, 1.0f)
        
        // Tracking accuracy based on live signals and tracking qualities
        val liveCount = targetPoints.count { it.label.contains("LIVE", ignoreCase = true) }
        val confPositionAccuracy = (liveCount.toFloat() / measurementCount.toFloat()).coerceIn(0.3f, 1.0f)
        
        // Heading accuracy degraded at extreme orientation angles (e.g. pitch or roll > 45)
        val avgOrientationTilt = targetPoints.map { abs(it.pitch) + abs(it.roll) }.average().toFloat()
        val confHeadingAccuracy = (1.0f - (avgOrientationTilt / 90f)).coerceIn(0.4f, 1.0f)

        // Freshness (time of latest scan)
        val latestTime = targetPoints.map { it.timestamp }.maxOrNull() ?: timestamp
        val latestAgeMs = (timestamp - latestTime).coerceAtLeast(0)
        val confTemporalFreshness = (30000f / (30000f + latestAgeMs)).coerceIn(0.1f, 1.0f)

        // Documented linear composition weighting
        var confidence = (
            confMeasurementQuality * 0.15f +
            confSpatialCoverage * 0.20f +
            confModelConsistency * 0.20f +
            confSampleCount * 0.15f +
            confTargetStability * 0.10f +
            confPositionAccuracy * 0.05f +
            confHeadingAccuracy * 0.05f +
            confTemporalFreshness * 0.10f
        ).coerceIn(0.01f, 0.95f)

        // Adaptive scaling penalties based on unstable model fits or UNKNOWN propagation trust
        if (envType == "UNKNOWN") {
            confidence *= 0.6f
        }
        if (modelConsistency == "POOR") {
            confidence *= 0.4f
        }
        if (modelConsistency == "INVALID" || !finalIsValid) {
            confidence = 0.0f
        }

        // 12. Bearing Estimation with Degenerate Mitigation
        val bearingDegrees = if (!insufficientSpatialDiversity) {
            ((Math.toDegrees(atan2(estX.toDouble(), estY.toDouble())).toFloat() + 360f) % 360f)
        } else {
            0.0f // do not produce a falsely confident bearing
        }
        
        val geomFactor = if (spatialCoverageMeters > 0.5f) (1f + collinearity * 5f) else 10f
        val bearingUncertaintyDegrees = if (!insufficientSpatialDiversity) {
            (45f * geomFactor * (1f + rmse / 10f) / sqrt(measurementCount.toFloat())).coerceIn(2f, 180f)
        } else {
            180.0f // maximum uncertainty
        }

        // Uncertainty radius scales the visual bounds.
        // It uses a standard 2-sigma boundary by default for visual consistency (major * 2.0)
        val radiusMeters = (majorMeters * 2.0f).coerceIn(1.0f, 30.0f)
        val isConverged = measurementCount >= 5 && confidence > 0.45f && !insufficientSpatialDiversity

        val hasDirect = targetPoints.any { it.label.contains("DIRECT", ignoreCase = true) || it.label.contains("UWB", ignoreCase = true) }
        val finalModelStatus = when {
            insufficientSpatialDiversity || measurementCount < 4 -> "NO_CORRELATION"
            hasDirect -> "DIRECT"
            isConverged -> "INFERRED"
            (timestamp - (targetPoints.map { it.timestamp }.maxOrNull() ?: 0L)) > 10000L -> "EXTRAPOLATED"
            else -> "INFERRED"
        }

        return ProbabilityVolume(
            centerEnu = Offset3D(estX, estY, 0.5f),
            radiusMeters = radiusMeters,
            confidenceScore = confidence,
            isConverged = isConverged,
            totalSamples = measurementCount,
            
            sourcePosition = Offset3D(estX, estY, 0.5f),
            uncertaintyRadiusMeters = radiusMeters,
            uncertaintyEastMeters = majorMeters,
            uncertaintyNorthMeters = minorMeters,
            covXX = covXX,
            covYY = covYY,
            covXY = covXY,
            bearingDegrees = bearingDegrees,
            bearingUncertaintyDegrees = bearingUncertaintyDegrees,
            confidence = confidence,
            measurementCount = measurementCount,
            spatialCoverageMeters = spatialCoverageMeters,
            modelResidual = rmse,
            measurementQuality = confMeasurementQuality * 100f,
            timestamp = timestamp,
            modelType = "Bayesian Posterior Estimation ($envType Env)",
            
            majorAxisMeters = majorMeters,
            minorAxisMeters = minorMeters,
            ellipseOrientationDegrees = ellipseOrientationDegrees,

            // UPGRADE VALIDATION 1.0 explicit fields
            referenceRssi = finalA,
            referenceDistance = refDist,
            pathLossExponent = nPathLoss,
            environmentType = envType,
            modelConsistency = modelConsistency,
            rmse = rmse,
            medianAbsoluteResidual = medianAbsoluteResidual,
            maxResidual = maxResid,
            outlierCount = outlierCount,
            effectiveSampleCount = effectiveSampleCount,
            isValid = finalIsValid,
            errorMessage = errorMsg,
            insufficientSpatialDiversity = insufficientSpatialDiversity,

            // Confidence subcomponents
            confMeasurementQuality = confMeasurementQuality,
            confSpatialCoverage = confSpatialCoverage,
            confModelConsistency = confModelConsistency,
            confSampleCount = confSampleCount,
            confTargetStability = confTargetStability,
            confPositionAccuracy = confPositionAccuracy,
            confHeadingAccuracy = confHeadingAccuracy,
            confTemporalFreshness = confTemporalFreshness,

            // Default level is 2-SIGMA
            sigmaLevel = "2-SIGMA",
            modelStatus = finalModelStatus,
            isOutlierList = isOutlierList
        )
    }
}
