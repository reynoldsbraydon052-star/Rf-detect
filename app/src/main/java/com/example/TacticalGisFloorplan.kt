package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.cos
import kotlin.math.sin

data class FloorplanAnchor(
    val pixelX: Float,
    val pixelY: Float,
    val metersX: Float = 0f,
    val metersY: Float = 0f,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

object TacticalFloorplanEngine {

    /**
     * Converts relative target coordinates (distance in meters, azimuth angle in degrees)
     * into scaled canvas pixel coordinates using an affine transformation / radial project.
     */
    fun mapTargetToCanvasPixel(
        tofDistanceMeters: Float,
        azimuthDegrees: Float,
        centerCanvasOffset: Offset,
        pixelsPerMeter: Float
    ): Offset {
        val rad = Math.toRadians((azimuthDegrees - 90).toDouble())
        val dx = (tofDistanceMeters * cos(rad) * pixelsPerMeter).toFloat()
        val dy = (tofDistanceMeters * sin(rad) * pixelsPerMeter).toFloat()
        return Offset(centerCanvasOffset.x + dx, centerCanvasOffset.y + dy)
    }

    /**
     * Calculates pixels per meter ratio from two calibration anchors.
     */
    fun calculatePixelsPerMeter(anchors: Pair<FloorplanAnchor, FloorplanAnchor>): Float {
        val dxPixels = anchors.second.pixelX - anchors.first.pixelX
        val dyPixels = anchors.second.pixelY - anchors.first.pixelY
        val distPixels = kotlin.math.sqrt(dxPixels * dxPixels + dyPixels * dyPixels)

        val dxMeters = anchors.second.metersX - anchors.first.metersX
        val dyMeters = anchors.second.metersY - anchors.first.metersY
        val distMeters = kotlin.math.sqrt(dxMeters * dxMeters + dyMeters * dyMeters)

        return if (distMeters > 0.001f) distPixels / distMeters else 12f
    }
}

/**
 * Compose DrawScope helper to render the blueprint floorplan overlay underneath radar blips.
 * Renders directly via GPU DrawScope vector commands, eliminating software Bitmap allocation.
 */
fun DrawScope.drawFloorplanOverlay(
    bitmap: ImageBitmap? = null,
    anchors: Pair<FloorplanAnchor, FloorplanAnchor> = Pair(
        FloorplanAnchor(60f, 60f, -15f, -15f),
        FloorplanAnchor(540f, 540f, 15f, 15f)
    ),
    alpha: Float = 0.45f,
    radarRangeMeters: Float = 30f
) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
    val maxRadius = kotlin.math.min(canvasWidth, canvasHeight) / 2f - 20f

    val rectSize = maxRadius * 2f
    val topLeft = Offset(center.x - maxRadius, center.y - maxRadius)

    if (bitmap != null) {
        val destSize = IntSize(rectSize.toInt(), rectSize.toInt())
        val destOffset = IntOffset(topLeft.x.toInt(), topLeft.y.toInt())
        drawImage(
            image = bitmap,
            dstOffset = destOffset,
            dstSize = destSize,
            alpha = alpha
        )
    } else {
        // Direct GPU Vector Blueprint Rendering
        // Dark Blueprint Background
        drawRect(
            color = Color(0xFF08121E).copy(alpha = alpha * 0.9f),
            topLeft = topLeft,
            size = Size(rectSize, rectSize)
        )

        val gridColor = Color(0xFF12354A).copy(alpha = alpha)
        val wallColor = Color(0xFF00E5FF).copy(alpha = alpha * 1.2f)
        val roomWallStroke = Stroke(width = 3.5f)
        val gridStroke = Stroke(width = 1.2f)

        // Architectural Grid
        val step = rectSize / 15f
        var cur = topLeft.x
        while (cur <= topLeft.x + rectSize) {
            drawLine(
                color = gridColor,
                start = Offset(cur, topLeft.y),
                end = Offset(cur, topLeft.y + rectSize),
                strokeWidth = gridStroke.width
            )
            cur += step
        }
        var curY = topLeft.y
        while (curY <= topLeft.y + rectSize) {
            drawLine(
                color = gridColor,
                start = Offset(topLeft.x, curY),
                end = Offset(topLeft.x + rectSize, curY),
                strokeWidth = gridStroke.width
            )
            curY += step
        }

        // Perimeter Wall
        drawRect(
            color = wallColor,
            topLeft = Offset(topLeft.x + 20f, topLeft.y + 20f),
            size = Size(rectSize - 40f, rectSize - 40f),
            style = roomWallStroke
        )

        // Tactical Lab (Top-Left)
        drawRect(
            color = wallColor,
            topLeft = Offset(topLeft.x + 20f, topLeft.y + 20f),
            size = Size((rectSize - 40f) * 0.42f, (rectSize - 40f) * 0.42f),
            style = roomWallStroke
        )

        // Server Vault (Top-Right)
        drawRect(
            color = wallColor,
            topLeft = Offset(topLeft.x + 20f + (rectSize - 40f) * 0.58f, topLeft.y + 20f),
            size = Size((rectSize - 40f) * 0.42f, (rectSize - 40f) * 0.42f),
            style = roomWallStroke
        )

        // Comms Center (Bottom-Left)
        drawRect(
            color = wallColor,
            topLeft = Offset(topLeft.x + 20f, topLeft.y + 20f + (rectSize - 40f) * 0.58f),
            size = Size((rectSize - 40f) * 0.42f, (rectSize - 40f) * 0.42f),
            style = roomWallStroke
        )

        // Ops Hub (Bottom-Right)
        drawRect(
            color = wallColor,
            topLeft = Offset(topLeft.x + 20f + (rectSize - 40f) * 0.58f, topLeft.y + 20f + (rectSize - 40f) * 0.58f),
            size = Size((rectSize - 40f) * 0.42f, (rectSize - 40f) * 0.42f),
            style = roomWallStroke
        )
    }

    // Outer Blueprint Cyan Accent Border
    drawRect(
        color = Color(0xFF00E5FF).copy(alpha = 0.6f),
        topLeft = topLeft,
        size = Size(rectSize, rectSize),
        style = Stroke(width = 2.5f)
    )
}
