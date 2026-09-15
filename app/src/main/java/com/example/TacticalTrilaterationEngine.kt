package com.example

import kotlin.math.*

data class Point2D(val x: Double, val y: Double)

data class RangingAnchor(
    val observerPosition: Point2D,     // Where the user was standing
    val estimatedDistanceMeters: Double, // Kalman-filtered distance to target
    val timestamp: Long
)

data class TrilaterationResult(
    val estimatedTargetPosition: Point2D,
    val residualErrorMeters: Double, // Fit quality / residual variance
    val confidenceScore: Float       // Geometric diversity coefficient (0.0 to 1.0)
)

/**
 * High-performance step and indoor dead-reckoning tracker using polar trigonometry.
 */
class DeadReckoningTracker(val strideLengthMeters: Double = 0.75) {
    private var currentPosition = Point2D(0.0, 0.0)

    /**
     * Call this when a step event is detected by the accelerometer/step sensor.
     * Updates the user's estimated coordinates relative to their walk start origin.
     */
    fun onStepDetected(headingDegrees: Float): Point2D {
        val headingInRadians = Math.toRadians(headingDegrees.toDouble())
        val dx = strideLengthMeters * sin(headingInRadians)
        val dy = strideLengthMeters * cos(headingInRadians)
        
        currentPosition = Point2D(
            x = currentPosition.x + dx,
            y = currentPosition.y + dy
        )
        return currentPosition
    }

    fun getPosition(): Point2D = currentPosition

    fun reset() {
        currentPosition = Point2D(0.0, 0.0)
    }
}

/**
 * Linearized Least-Squares Matrix solver for multi-anchor 2D trilateration.
 */
object TrilaterationSolver {

    /**
     * Solves target 2D coordinates using 3 or more non-collinear anchor points.
     * Linearizes around the first anchor:
     * 2*(x_i - x_1)*x + 2*(y_i - y_1)*y = (r_1^2 - r_i^2) - (x_1^2 - x_i^2) - (y_1^2 - y_i^2)
     */
    fun solve(anchors: List<RangingAnchor>): TrilaterationResult? {
        if (anchors.size < 3) return null

        val reference = anchors[0]
        val x1 = reference.observerPosition.x
        val y1 = reference.observerPosition.y
        val r1 = reference.estimatedDistanceMeters

        val numEquations = anchors.size - 1
        val A = Array(numEquations) { DoubleArray(2) }
        val b = DoubleArray(numEquations)

        for (i in 1 until anchors.size) {
            val anchor = anchors[i]
            val xi = anchor.observerPosition.x
            val yi = anchor.observerPosition.y
            val ri = anchor.estimatedDistanceMeters

            // Linearization equations
            val index = i - 1
            A[index][0] = 2.0 * (xi - x1)
            A[index][1] = 2.0 * (yi - y1)
            b[index] = (r1 * r1) - (ri * ri) - (x1 * x1 - xi * xi) - (y1 * y1 - yi * yi)
        }

        // Solves the system using Ordinary Least Squares: X = (A^T * A)^(-1) * A^T * b
        // 1. Compute M = A^T * A (a symmetric 2x2 matrix)
        var m00 = 0.0
        var m01 = 0.0
        var m11 = 0.0
        for (i in 0 until numEquations) {
            m00 += A[i][0] * A[i][0]
            m01 += A[i][0] * A[i][1]
            m11 += A[i][1] * A[i][1]
        }
        val m10 = m01

        // 2. Compute V = A^T * b (a 2x1 vector)
        var v0 = 0.0
        var v1 = 0.0
        for (i in 0 until numEquations) {
            v0 += A[i][0] * b[i]
            v1 += A[i][1] * b[i]
        }

        // 3. Invert the 2x2 matrix M: det = m00*m11 - m01*m10
        val det = m00 * m11 - m01 * m10
        if (abs(det) < 1e-9) {
            // Collinear warning: The path/walk geometry lacks sufficient spatial diversity
            return null
        }

        // 4. Matrix Multiplication: X = M^(-1) * V
        val targetX = (m11 * v0 - m01 * v1) / det
        val targetY = (-m10 * v0 + m00 * v1) / det
        val targetPos = Point2D(targetX, targetY)

        // 5. Calculate Residual Error (Root-Mean-Square distance drift)
        var sumSquaredError = 0.0
        for (anchor in anchors) {
            val xi = anchor.observerPosition.x
            val yi = anchor.observerPosition.y
            val observedDistance = anchor.estimatedDistanceMeters
            val computedDistance = sqrt((targetX - xi).pow(2) + (targetY - yi).pow(2))
            
            sumSquaredError += (computedDistance - observedDistance).pow(2)
        }
        val residualError = sqrt(sumSquaredError / anchors.size)

        // 6. Compute Geometric Dilution of Precision (GDOP) based confidence score
        // We evaluate variance in anchor positions x and y
        val avgX = anchors.map { it.observerPosition.x }.average()
        val avgY = anchors.map { it.observerPosition.y }.average()
        val varianceX = anchors.map { (it.observerPosition.x - avgX).pow(2) }.sum()
        val varianceY = anchors.map { (it.observerPosition.y - avgY).pow(2) }.sum()
        
        // Spread threshold: More spatial spread = higher geometric confidence (up to 1.0)
        val geometrySpread = sqrt(varianceX + varianceY)
        val confidence = (geometrySpread / 15.0).coerceIn(0.1, 1.0).toFloat()

        return TrilaterationResult(
            estimatedTargetPosition = targetPos,
            residualErrorMeters = residualError,
            confidenceScore = confidence
        )
    }
}
