package com.example

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 2.5D Isometric Coordinate Transformer for Tactical Radar Canvas.
 *
 * Tilted perspective projection:
 * - Projects local 3D target coordinates (x, y, z in meters) onto a 2.5D perspective ground plane.
 * - Computes ground-plane anchor coordinates (x_anchor, y_anchor) for shadow/grid placement.
 * - Computes elevated screen coordinates (x_screen, y_screen) with Z-stem elevation drop lines.
 * - Calculates viewport edge-clamping and peripheral indicator arrow angles for out-of-bounds nodes.
 */
class IsometricRadarTransformer(
    var pitchAngleDeg: Float = 35.0f,
    var elevationScaleFactor: Float = 1.2f
) {
    /**
     * 2.5D Projected Point containing ground anchor, elevated coordinate, visibility status, and 3D camera depth.
     */
    data class Projected25DPoint(
        val groundAnchor: Offset,
        val elevatedScreenPos: Offset,
        val zStemLengthPx: Float,
        val isElevated: Boolean,
        val isClamped: Boolean,
        val edgeAngleDeg: Float = 0f,
        val distanceMeters: Float = 0f,
        val relativeHeadingDeg: Float = 0f,
        val cameraDepth: Float = 0f // Distance along camera optical depth axis for Painter's algorithm Z-sorting
    )

    /**
     * Transforms local polar target coordinates into 3D/2.5D Screen Space with Camera Depth.
     *
     * @param distanceMeters Distance from user origin in meters.
     * @param angleOffsetDeg Relative angle from forward heading (0° = dead ahead, +90° = right/east).
     * @param zOffsetMeters Relative altitude offset (+ is above, - is below ground plane).
     * @param mapRangeMeters Radius of visible map in meters.
     * @param centerX Viewport center X in pixels.
     * @param centerY Viewport center Y in pixels.
     * @param maxRadius Viewport ground plane radius in pixels.
     * @param headingRotationDeg Additional rotation offset (e.g. compass heading or manual gesture rotation).
     * @param zoomFactor Scale multiplier.
     * @param panOffset Pan translation in pixels.
     */
    fun transformTo25D(
        distanceMeters: Float,
        angleOffsetDeg: Float,
        zOffsetMeters: Float,
        mapRangeMeters: Float,
        centerX: Float,
        centerY: Float,
        maxRadius: Float,
        headingRotationDeg: Float = 0f,
        zoomFactor: Float = 1.0f,
        panOffset: Offset = Offset.Zero
    ): Projected25DPoint {
        val effectiveRange = mapRangeMeters.coerceAtLeast(1.0f)
        val pixelsPerMeter = (maxRadius / effectiveRange) * zoomFactor

        // Calculate ground-plane Cartesian coordinates relative to user (X = Right/East, Y = Forward/North)
        val totalAngleRad = Math.toRadians((angleOffsetDeg - headingRotationDeg).toDouble())
        val localX = distanceMeters * sin(totalAngleRad).toFloat()
        val localY = distanceMeters * cos(totalAngleRad).toFloat()

        // 3D Perspective Tilt Matrix Transformation
        val pitchRad = Math.toRadians(pitchAngleDeg.toDouble())
        val cosPitch = cos(pitchRad).toFloat()
        val sinPitch = sin(pitchRad).toFloat()

        // Ground anchor coordinates on perspective grid plane
        val anchorScreenX = centerX + panOffset.x + (localX * pixelsPerMeter)
        val anchorScreenY = centerY + panOffset.y - (localY * pixelsPerMeter * cosPitch)

        // Elevation vertical offset (upwards on screen for positive Z)
        val zStemPx = zOffsetMeters * pixelsPerMeter * sinPitch * elevationScaleFactor
        val elevatedScreenX = anchorScreenX
        val elevatedScreenY = anchorScreenY - zStemPx

        // Optical Camera Depth for Z-sorting (Painter's Algorithm):
        // Camera looks along ground plane at pitch angle. Points with larger localY (further north) are deeper into scene.
        val opticalDepth = localY * sinPitch - zOffsetMeters * cosPitch + (effectiveRange * 2.0f)

        // Viewport / Bezel boundary check
        val dx = anchorScreenX - (centerX + panOffset.x)
        val dy = (anchorScreenY - (centerY + panOffset.y)) / cosPitch.coerceAtLeast(0.1f)
        val groundDistancePx = sqrt(dx * dx + dy * dy)
        val isOutOfBounds = groundDistancePx > maxRadius * zoomFactor

        val finalAnchor: Offset
        val finalElevated: Offset
        var edgeAngle = 0f

        if (isOutOfBounds) {
            val scale = (maxRadius * zoomFactor) / groundDistancePx.coerceAtLeast(1f)
            val clampedAnchorX = (centerX + panOffset.x) + dx * scale
            val clampedAnchorY = (centerY + panOffset.y) + (dy * scale) * cosPitch
            finalAnchor = Offset(clampedAnchorX, clampedAnchorY)
            finalElevated = Offset(clampedAnchorX, clampedAnchorY - zStemPx * scale)
            edgeAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        } else {
            finalAnchor = Offset(anchorScreenX, anchorScreenY)
            finalElevated = Offset(elevatedScreenX, elevatedScreenY)
        }

        return Projected25DPoint(
            groundAnchor = finalAnchor,
            elevatedScreenPos = finalElevated,
            zStemLengthPx = zStemPx,
            isElevated = kotlin.math.abs(zOffsetMeters) > 0.2f,
            isClamped = isOutOfBounds,
            edgeAngleDeg = edgeAngle,
            distanceMeters = distanceMeters,
            relativeHeadingDeg = angleOffsetDeg,
            cameraDepth = opticalDepth
        )
    }

    /**
     * Fills a pre-allocated Path with the FOV cone polygon without allocating Lists.
     */
    fun fillFovPath(
        path: androidx.compose.ui.graphics.Path,
        fovArcDeg: Float = 60.0f,
        rangeMeters: Float = 15.0f,
        headingDeg: Float = 0.0f,
        mapRangeMeters: Float = 30.0f,
        centerX: Float,
        centerY: Float,
        maxRadius: Float,
        zoomFactor: Float = 1.0f,
        panOffset: Offset = Offset.Zero,
        stepCount: Int = 12
    ) {
        path.reset()
        val originX = centerX + panOffset.x
        val originY = centerY + panOffset.y
        path.moveTo(originX, originY)

        val halfFov = fovArcDeg / 2f
        val startAngle = -halfFov
        val endAngle = halfFov
        val stepAngle = (endAngle - startAngle) / stepCount

        for (i in 0..stepCount) {
            val angle = startAngle + (i * stepAngle)
            val projected = transformTo25D(
                distanceMeters = rangeMeters,
                angleOffsetDeg = angle + headingDeg,
                zOffsetMeters = 0f,
                mapRangeMeters = mapRangeMeters,
                centerX = centerX,
                centerY = centerY,
                maxRadius = maxRadius,
                headingRotationDeg = 0f,
                zoomFactor = zoomFactor,
                panOffset = panOffset
            )
            path.lineTo(projected.groundAnchor.x, projected.groundAnchor.y)
        }
        path.close()
    }

    /**
     * Projects a field-of-view (FOV) cone onto the isometric ground plane.
     *
     * @param fovArcDeg Field of view arc width (e.g. 60°).
     * @param rangeMeters Range of FOV in meters.
     * @param headingDeg Forward direction heading in degrees.
     */
    fun computeFovPolygon(
        fovArcDeg: Float = 60.0f,
        rangeMeters: Float = 15.0f,
        headingDeg: Float = 0.0f,
        mapRangeMeters: Float = 30.0f,
        centerX: Float,
        centerY: Float,
        maxRadius: Float,
        zoomFactor: Float = 1.0f,
        panOffset: Offset = Offset.Zero,
        stepCount: Int = 12
    ): List<Offset> {
        val points = mutableListOf<Offset>()
        val origin = Offset(centerX + panOffset.x, centerY + panOffset.y)
        points.add(origin)

        val halfFov = fovArcDeg / 2f
        val startAngle = -halfFov
        val endAngle = halfFov
        val stepAngle = (endAngle - startAngle) / stepCount

        for (i in 0..stepCount) {
            val angle = startAngle + (i * stepAngle)
            val projected = transformTo25D(
                distanceMeters = rangeMeters,
                angleOffsetDeg = angle + headingDeg,
                zOffsetMeters = 0f,
                mapRangeMeters = mapRangeMeters,
                centerX = centerX,
                centerY = centerY,
                maxRadius = maxRadius,
                headingRotationDeg = 0f,
                zoomFactor = zoomFactor,
                panOffset = panOffset
            )
            points.add(projected.groundAnchor)
        }

        points.add(origin)
        return points
    }
}
