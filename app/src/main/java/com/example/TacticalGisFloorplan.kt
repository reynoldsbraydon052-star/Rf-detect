package com.example

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
     * into scaled canvas bitmap pixel coordinates using an affine transformation / radial project.
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

    /**
     * Creates a high-definition synthetic tactical building blueprint overlay bitmap.
     */
    fun createDefaultBlueprintBitmap(width: Int = 600, height: Int = 600): ImageBitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Dark Blueprint Canvas Background
        canvas.drawColor(android.graphics.Color.parseColor("#08121E"))

        val gridPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#12354A")
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }

        val wallPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#00E5FF")
            strokeWidth = 4f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#00FF66")
            textSize = 18f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
        }

        // Draw Blueprint Architectural Grid
        val step = 40
        for (i in 0..width step step) {
            canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), gridPaint)
        }
        for (j in 0..height step step) {
            canvas.drawLine(0f, j.toFloat(), width.toFloat(), j.toFloat(), gridPaint)
        }

        // Outer Floorplan Perimeter Walls
        canvas.drawRect(RectF(60f, 60f, width - 60f, height - 60f), wallPaint)

        // Internal Rooms & Corridors
        canvas.drawRect(RectF(60f, 60f, 260f, 260f), wallPaint) // Room A (TACTICAL LAB)
        canvas.drawRect(RectF(340f, 60f, width - 60f, 260f), wallPaint) // Room B (SERVER VAULT)
        canvas.drawRect(RectF(60f, 340f, 260f, height - 60f), wallPaint) // Room C (COMMS CENTER)
        canvas.drawRect(RectF(340f, 340f, width - 60f, height - 60f), wallPaint) // Room D (OPS HUB)

        // Room Labels
        canvas.drawText("[TACTICAL LAB]", 80f, 120f, textPaint)
        canvas.drawText("[SERVER VAULT]", 360f, 120f, textPaint)
        canvas.drawText("[COMMS CENTER]", 80f, 400f, textPaint)
        canvas.drawText("[OPS HUB]", 360f, 400f, textPaint)
        canvas.drawText("CORRIDOR / HALLWAY", 200f, 305f, textPaint)

        return bitmap.asImageBitmap()
    }
}

/**
 * Compose DrawScope helper to render the blueprint floorplan overlay underneath radar blips.
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
    val floorplanBitmap = bitmap ?: TacticalFloorplanEngine.createDefaultBlueprintBitmap()

    val canvasWidth = size.width
    val canvasHeight = size.height
    val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
    val maxRadius = kotlin.math.min(canvasWidth, canvasHeight) / 2f - 20f

    val destSize = IntSize((maxRadius * 2f).toInt(), (maxRadius * 2f).toInt())
    val destOffset = IntOffset(
        (center.x - destSize.width / 2f).toInt(),
        (center.y - destSize.height / 2f).toInt()
    )

    drawImage(
        image = floorplanBitmap,
        dstOffset = destOffset,
        dstSize = destSize,
        alpha = alpha
    )

    // Render blueprint cyan border outline
    drawRect(
        color = Color(0xFF00E5FF).copy(alpha = 0.6f),
        topLeft = Offset(destOffset.x.toFloat(), destOffset.y.toFloat()),
        size = Size(destSize.width.toFloat(), destSize.height.toFloat()),
        style = Stroke(width = 2.5f)
    )
}
