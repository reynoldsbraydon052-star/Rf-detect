package com.example

import java.util.UUID
import kotlin.math.max
import kotlin.math.min

data class CorrelationEvent(
    val id: String = UUID.randomUUID().toString(),
    val firstObservationMs: Long,
    val lastObservationMs: Long,
    val observations: List<RadarBlip>,
    val participatingSensors: Set<String>,
    val maxTimeSeparationMs: Long,
    val spatialRelationship: SpatialRelationship,
    val correlationScore: Float, // 0.0 to 1.0
    val confidence: Float, // 0.0 to 1.0
    val provenance: DataProvenance,
    val notes: String
) {
    fun getDurationMs(): Long = lastObservationMs - firstObservationMs
}

enum class SpatialRelationship {
    UNKNOWN,
    CO_LOCATED,
    PROXIMATE,
    DISTANT
}

data class CorrelationEngineConfig(
    val maxTemporalWindowMs: Long = 1000L,
    val minConfidenceThreshold: Float = 0.5f,
    val repeatedOccurrenceBonus: Float = 0.1f
)
