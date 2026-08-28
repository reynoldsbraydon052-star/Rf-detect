package com.example

enum class BaselineState {
    KNOWN,
    UNKNOWN,
    NEW,
    CHANGED,
    ANOMALOUS
}

data class BaselineSummary(
    val knownFingerprints: Int = 0,
    val newFingerprints: Int = 0,
    val missingFingerprints: Int = 0,
    val rfActivityDeltaPercent: Float = 0f, // 0.0 to 1.0 (or -1.0)
    val freqOccupancyDeltaPercent: Float = 0f,
    val baselineConfidence: Float = 0f, // 0.0 to 1.0
    val isLearning: Boolean = false,
    val baselineAgeMs: Long = 0,
    val observationsCollected: Long = 0
)
