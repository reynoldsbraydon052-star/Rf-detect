package com.example

import java.util.ArrayDeque
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Morphological glint profiler and zero-allocation Y-plane differencing engine.
 *
 * Implements:
 * 1. Strobe Differencing: |Frame_ON - Frame_OFF| across YUV_420_888 Luminance (Y) planes.
 * 2. Connected Component Clustering: Fast spatial grouping of contiguous thresholded pixels.
 * 3. Airy Disk Morphological Validation: Calculates Circularity Ratio (4π * Area / Perimeter^2)
 *    and bounds checks diameter (2.0 to 15.0 px) to isolate true pinhole/camera retroreflections
 *    from flat glints, specular mirrors, and rectangular screw heads.
 */
class OpticalMorphologicalFilter(
    val minLuminanceDelta: Int = 40,
    val minDiameterPixels: Float = 2.0f,
    val maxDiameterPixels: Float = 15.0f,
    val minCircularityRatio: Float = 0.80f
) {

    /**
     * Performs zero-allocation absolute differencing on two consecutive Y-plane luminance buffers:
     * diff[i] = |frameOn[i] - frameOff[i]|
     */
    fun computeLuminanceDifference(
        frameOn: ByteArray,
        frameOff: ByteArray,
        diffOutput: ByteArray,
        length: Int = min(min(frameOn.size, frameOff.size), diffOutput.size)
    ) {
        for (i in 0 until length) {
            val yOn = frameOn[i].toInt() and 0xFF
            val yOff = frameOff[i].toInt() and 0xFF
            diffOutput[i] = abs(yOn - yOff).toByte()
        }
    }

    /**
     * Internal data structure representing a segmented pixel cluster.
     */
    data class PixelCluster(
        var pixelCount: Int = 0,
        var perimeterCount: Int = 0,
        var sumX: Long = 0L,
        var sumY: Long = 0L,
        var minX: Int = Int.MAX_VALUE,
        var maxX: Int = Int.MIN_VALUE,
        var minY: Int = Int.MAX_VALUE,
        var maxY: Int = Int.MIN_VALUE,
        var maxDelta: Int = 0,
        var sumDelta: Long = 0L
    )

    /**
     * Analyzes the luminance differential buffer to extract qualified retroreflective glint candidates.
     *
     * @param diffBuffer Byte array containing the differential Y-plane (|Frame_ON - Frame_OFF|).
     * @param width Frame pixel width.
     * @param height Frame pixel height.
     * @param rowStride Sensor row stride in bytes (defaults to width if contiguous).
     * @return List of candidates passing luminance, diameter, and circularity thresholds.
     */
    fun extractGlintCandidates(
        diffBuffer: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int = width
    ): List<OpticalGlintTarget> {
        if (width <= 0 || height <= 0 || diffBuffer.isEmpty()) {
            return emptyList()
        }

        val totalPixels = width * height
        val visited = BooleanArray(totalPixels)
        val candidateTargets = mutableListOf<OpticalGlintTarget>()

        val maxQueueCapacity = totalPixels.coerceAtMost(4096)
        val queueX = IntArray(maxQueueCapacity)
        val queueY = IntArray(maxQueueCapacity)
        var qHead = 0
        var qTail = 0

        // 8-connectivity neighbor offsets
        val dx = intArrayOf(-1, 0, 1, -1, 1, -1, 0, 1)
        val dy = intArrayOf(-1, -1, -1, 0, 0, 1, 1, 1)

        val maxAllowedArea = (PI * (maxDiameterPixels / 2.0f) * (maxDiameterPixels / 2.0f) * 2.0).toInt() + 10

        for (y in 0 until height) {
            val rowOffset = y * rowStride
            val visitedRowOffset = y * width

            for (x in 0 until width) {
                val visitedIdx = visitedRowOffset + x
                if (visited[visitedIdx]) continue

                val pixelOffset = rowOffset + x
                if (pixelOffset >= diffBuffer.size) continue

                val delta = diffBuffer[pixelOffset].toInt() and 0xFF
                if (delta < minLuminanceDelta) {
                    visited[visitedIdx] = true
                    continue
                }

                // Discovered a new unvisited bright pixel exceeding threshold
                val cluster = PixelCluster()
                qHead = 0
                qTail = 0

                queueX[qTail] = x
                queueY[qTail] = y
                qTail = (qTail + 1) % maxQueueCapacity
                visited[visitedIdx] = true

                while (qHead != qTail) {
                    val currX = queueX[qHead]
                    val currY = queueY[qHead]
                    qHead = (qHead + 1) % maxQueueCapacity

                    val currPixelOffset = currY * rowStride + currX
                    val currDelta = if (currPixelOffset < diffBuffer.size) {
                        diffBuffer[currPixelOffset].toInt() and 0xFF
                    } else delta

                    cluster.pixelCount++
                    cluster.sumX += currX
                    cluster.sumY += currY
                    cluster.sumDelta += currDelta
                    cluster.maxDelta = max(cluster.maxDelta, currDelta)
                    cluster.minX = min(cluster.minX, currX)
                    cluster.maxX = max(cluster.maxX, currX)
                    cluster.minY = min(cluster.minY, currY)
                    cluster.maxY = max(cluster.maxY, currY)

                    var isPerimeter = false

                    // Explore 8-connected neighborhood
                    for (k in 0 until 8) {
                        val nx = currX + dx[k]
                        val ny = currY + dy[k]

                        if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                            isPerimeter = true
                            continue
                        }

                        val nVisitedIdx = ny * width + nx
                        val nPixelOffset = ny * rowStride + nx

                        if (nPixelOffset >= diffBuffer.size) {
                            isPerimeter = true
                            continue
                        }

                        val nDelta = diffBuffer[nPixelOffset].toInt() and 0xFF
                        if (nDelta >= minLuminanceDelta) {
                            if (!visited[nVisitedIdx]) {
                                visited[nVisitedIdx] = true
                                if (((qTail + 1) % maxQueueCapacity) != qHead) {
                                    queueX[qTail] = nx
                                    queueY[qTail] = ny
                                    qTail = (qTail + 1) % maxQueueCapacity
                                }
                            }
                        } else {
                            isPerimeter = true
                        }
                    }

                    if (isPerimeter) {
                        cluster.perimeterCount++
                    }
                }

                // Validate cluster against morphological constraints
                val candidate = evaluateCluster(cluster, width, height)
                if (candidate != null) {
                    candidateTargets.add(candidate)
                }
            }
        }

        return candidateTargets
    }

    /**
     * Evaluates a pixel cluster for geometric circularity (Airy disk) and diameter limits.
     */
    fun evaluateCluster(cluster: PixelCluster, frameWidth: Int, frameHeight: Int): OpticalGlintTarget? {
        val area = cluster.pixelCount
        if (area <= 0) return null

        val perimeter = max(cluster.perimeterCount, 4) // Minimum perimeter boundary

        // Circularity formula: 4 * PI * Area / (Perimeter^2)
        val circularity = ((4.0 * PI * area) / (perimeter.toDouble() * perimeter.toDouble())).toFloat()

        // Equivalent circle diameter: 2 * sqrt(Area / PI)
        val equivalentDiameter = (2.0 * sqrt(area.toDouble() / PI)).toFloat()

        // Bounding box span
        val boundWidth = (cluster.maxX - cluster.minX + 1).toFloat()
        val boundHeight = (cluster.maxY - cluster.minY + 1).toFloat()
        val boundingDiameter = max(boundWidth, boundHeight)

        val representativeDiameter = (equivalentDiameter + boundingDiameter) / 2.0f

        // Rejection criteria: Diameter out of bounds (too small to be lens, or too large = reflection/mirror)
        if (representativeDiameter < minDiameterPixels || representativeDiameter > maxDiameterPixels) {
            return null
        }

        // Rejection criteria: Non-circular shape (scratches, screw heads, lines)
        // We permit slight tolerance when area is very small (3-5 pixels) due to grid quantization
        val effectiveMinCircularity = if (area <= 5) minCircularityRatio * 0.90f else minCircularityRatio
        if (circularity < effectiveMinCircularity) {
            return null
        }

        val centerX = cluster.sumX.toFloat() / area.toFloat()
        val centerY = cluster.sumY.toFloat() / area.toFloat()

        val normalizedX = (centerX / frameWidth.toFloat()).coerceIn(0.0f, 1.0f)
        val normalizedY = (centerY / frameHeight.toFloat()).coerceIn(0.0f, 1.0f)

        // Confidence estimation based on circularity perfection and delta luminance intensity
        val circularityFactor = (circularity / 1.0f).coerceIn(0.0f, 1.0f)
        val intensityFactor = (cluster.maxDelta / 255.0f).coerceIn(0.0f, 1.0f)
        val confidence = ((circularityFactor * 0.6f) + (intensityFactor * 0.4f)).coerceIn(0.0f, 1.0f)

        val targetId = "GLINT_%04d_%04d".format(centerX.toInt(), centerY.toInt())

        return OpticalGlintTarget(
            id = targetId,
            xPixel = centerX,
            yPixel = centerY,
            normalizedX = normalizedX,
            normalizedY = normalizedY,
            diameterPixels = representativeDiameter,
            circularityRatio = circularity,
            deltaLuminance = cluster.maxDelta,
            confidence = confidence,
            persistenceFrameCount = 1,
            isTelephotoHandoffReady = false
        )
    }
}
