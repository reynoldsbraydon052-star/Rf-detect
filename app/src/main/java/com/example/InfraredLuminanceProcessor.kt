package com.example

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * High-pass luminance thresholding and false-color targeting processor for Passive IR Sweeper.
 *
 * Implements:
 * 1. Zero-allocation ARGB/RGBA false-color rendering:
 *    - Ambient IR (Y < threshold): Rendered as grayscale (R=Y, G=Y, B=Y, A=255).
 *    - High-intensity IR blooms (Y >= threshold): Artificially remapped to glaring neon magenta / pure red
 *      (0xFFFF007F or 0xFFFF1744) to instantly expose covert 850nm/940nm illuminator arrays in dark scenes.
 * 2. Connected Component Bloom Profiling:
 *    - Extracts spatial centroids, peak intensities, and bounding radii of active emitter glints.
 */
class InfraredLuminanceProcessor(
    var luminanceThreshold: Int = 210,
    var minBloomRadius: Float = 1.0f,
    var maxBloomRadius: Float = 60.0f
) {
    companion object {
        const val COLOR_NEON_MAGENTA = 0xFFFF007F.toInt()
        const val COLOR_NEON_CRIMSON = 0xFFFF1744.toInt()
        const val COLOR_NEON_VIOLET = 0xFFD500F9.toInt()
    }

    private var pixelBuffer: IntArray? = null
    private var cachedBitmap: Bitmap? = null
    private var bufferWidth = 0
    private var bufferHeight = 0

    /**
     * Data holder for internal cluster stats.
     */
    private data class BloomCluster(
        var pixelCount: Int = 0,
        var sumX: Long = 0L,
        var sumY: Long = 0L,
        var minX: Int = Int.MAX_VALUE,
        var maxX: Int = Int.MIN_VALUE,
        var minY: Int = Int.MAX_VALUE,
        var maxY: Int = Int.MIN_VALUE,
        var peakY: Int = 0,
        var sumYValues: Long = 0L
    )

    /**
     * Transforms a monochrome/Y-plane buffer into an ARGB_8888 false-color bitmap.
     *
     * @param yPlane Byte array of luminance/NIR values (0..255).
     * @param width Frame width.
     * @param height Frame height.
     * @param rowStride Stride in bytes.
     * @return Pair containing the false-color [Bitmap] and a list of detected [IrBloomTarget]s.
     */
    @Synchronized
    fun processMonochromeFrame(
        yPlane: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int = width
    ): Pair<Bitmap, List<IrBloomTarget>> {
        val totalPixels = width * height
        ensureBuffers(width, height)

        val outPixels = pixelBuffer ?: IntArray(totalPixels).also { pixelBuffer = it }
        val outBitmap = cachedBitmap ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { cachedBitmap = it }

        val threshold = luminanceThreshold
        var sumLum = 0L
        var maxLum = 0

        // Pass 1: Pixel false-color mapping & fast stats
        for (y in 0 until height) {
            val rowOffset = y * rowStride
            val pixelRowOffset = y * width

            for (x in 0 until width) {
                val byteIdx = rowOffset + x
                if (byteIdx >= yPlane.size) break

                val yVal = yPlane[byteIdx].toInt() and 0xFF
                sumLum += yVal
                if (yVal > maxLum) maxLum = yVal

                val pixelIdx = pixelRowOffset + x
                if (pixelIdx < outPixels.size) {
                    if (yVal >= threshold) {
                        // High-intensity IR bloom: False-color neon magenta/crimson
                        val intensityRatio = ((yVal - threshold).toFloat() / (255 - threshold).coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                        if (intensityRatio > 0.7f) {
                            outPixels[pixelIdx] = COLOR_NEON_MAGENTA
                        } else if (intensityRatio > 0.4f) {
                            outPixels[pixelIdx] = COLOR_NEON_CRIMSON
                        } else {
                            outPixels[pixelIdx] = COLOR_NEON_VIOLET
                        }
                    } else {
                        // Ambient IR: Neutral high-contrast grayscale
                        outPixels[pixelIdx] = -0x1000000 or (yVal shl 16) or (yVal shl 8) or yVal
                    }
                }
            }
        }

        outBitmap.setPixels(outPixels, 0, width, 0, 0, width, height)

        // Pass 2: Extract bloom targets if bright spots exist
        val targets = if (maxLum >= threshold) {
            extractBloomTargets(yPlane, width, height, rowStride, threshold)
        } else {
            emptyList()
        }

        return Pair(outBitmap, targets)
    }

    /**
     * Identifies connected emitter bloom clusters and computes their spatial coordinates.
     */
    private fun extractBloomTargets(
        yPlane: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int,
        threshold: Int
    ): List<IrBloomTarget> {
        val totalPixels = width * height
        val visited = BooleanArray(totalPixels)
        val bloomTargets = mutableListOf<IrBloomTarget>()

        val maxQueueCap = totalPixels.coerceAtMost(4096)
        val queueX = IntArray(maxQueueCap)
        val queueY = IntArray(maxQueueCap)

        val dx = intArrayOf(-1, 0, 1, -1, 1, -1, 0, 1)
        val dy = intArrayOf(-1, -1, -1, 0, 0, 1, 1, 1)

        for (y in 0 until height) {
            val rowOffset = y * rowStride
            val visitedRowOffset = y * width

            for (x in 0 until width) {
                val vIdx = visitedRowOffset + x
                if (visited[vIdx]) continue

                val bIdx = rowOffset + x
                if (bIdx >= yPlane.size) continue

                val lum = yPlane[bIdx].toInt() and 0xFF
                if (lum < threshold) {
                    visited[vIdx] = true
                    continue
                }

                // Discovered new bloom cluster
                val cluster = BloomCluster()
                var qHead = 0
                var qTail = 0

                queueX[qTail] = x
                queueY[qTail] = y
                qTail = (qTail + 1) % maxQueueCap
                visited[vIdx] = true

                while (qHead != qTail) {
                    val cx = queueX[qHead]
                    val cy = queueY[qHead]
                    qHead = (qHead + 1) % maxQueueCap

                    val currBIdx = cy * rowStride + cx
                    val currLum = if (currBIdx < yPlane.size) (yPlane[currBIdx].toInt() and 0xFF) else lum

                    cluster.pixelCount++
                    cluster.sumX += cx
                    cluster.sumY += cy
                    cluster.sumYValues += currLum
                    cluster.peakY = max(cluster.peakY, currLum)
                    cluster.minX = min(cluster.minX, cx)
                    cluster.maxX = max(cluster.maxX, cx)
                    cluster.minY = min(cluster.minY, cy)
                    cluster.maxY = max(cluster.maxY, cy)

                    for (k in 0 until 8) {
                        val nx = cx + dx[k]
                        val ny = cy + dy[k]

                        if (nx in 0 until width && ny in 0 until height) {
                            val nvIdx = ny * width + nx
                            val nbIdx = ny * rowStride + nx

                            if (!visited[nvIdx] && nbIdx < yPlane.size) {
                                val nLum = yPlane[nbIdx].toInt() and 0xFF
                                if (nLum >= threshold) {
                                    visited[nvIdx] = true
                                    if (((qTail + 1) % maxQueueCap) != qHead) {
                                        queueX[qTail] = nx
                                        queueY[qTail] = ny
                                        qTail = (qTail + 1) % maxQueueCap
                                    }
                                }
                            }
                        }
                    }
                }

                // Compute geometric and intensity metrics
                if (cluster.pixelCount >= 2) {
                    val spanW = (cluster.maxX - cluster.minX + 1).toFloat()
                    val spanH = (cluster.maxY - cluster.minY + 1).toFloat()
                    val radius = (max(spanW, spanH) / 2.0f).coerceAtLeast(1.0f)

                    if (radius in minBloomRadius..maxBloomRadius) {
                        val centerX = (cluster.sumX.toFloat() / cluster.pixelCount.toFloat()).coerceIn(0f, width.toFloat())
                        val centerY = (cluster.sumY.toFloat() / cluster.pixelCount.toFloat()).coerceIn(0f, height.toFloat())
                        val avgLum = cluster.sumYValues.toFloat() / cluster.pixelCount.toFloat()
                        val confidence = ((cluster.peakY - threshold).toFloat() / (255 - threshold).coerceAtLeast(1).toFloat()).coerceIn(0.1f, 1.0f)

                        bloomTargets.add(
                            IrBloomTarget(
                                targetId = "IR_EMITTER_%04d_%04d".format(centerX.toInt(), centerY.toInt()),
                                xPixel = centerX,
                                yPixel = centerY,
                                normalizedX = (centerX / width.toFloat()).coerceIn(0f, 1f),
                                normalizedY = (centerY / height.toFloat()).coerceIn(0f, 1f),
                                radiusPixels = radius,
                                peakLuminance = cluster.peakY,
                                averageLuminance = avgLum,
                                confidence = confidence
                            )
                        )
                    }
                }
            }
        }

        return bloomTargets
    }

    private fun ensureBuffers(width: Int, height: Int) {
        if (bufferWidth != width || bufferHeight != height || pixelBuffer == null || cachedBitmap == null) {
            bufferWidth = width
            bufferHeight = height
            val total = width * height
            pixelBuffer = IntArray(total)
            cachedBitmap?.recycle()
            cachedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
    }

    /**
     * Disposes resources and bitmap buffers.
     */
    @Synchronized
    fun release() {
        cachedBitmap?.recycle()
        cachedBitmap = null
        pixelBuffer = null
    }
}
