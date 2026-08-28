package com.example

import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MultiSensorCorrelationEngine(
    private val config: CorrelationEngineConfig = CorrelationEngineConfig()
) {
    private var isIsolated = false

    fun isolateStateForSimulation() {
        isIsolated = true
        observationBuffer.clear()
        lastCorrelatedPairs.clear()
        historicalPatterns.clear()
    }

    fun resetIsolatedState() {
        isIsolated = false
        observationBuffer.clear()
        lastCorrelatedPairs.clear()
        historicalPatterns.clear()
    }
    // Sliding window of recent observations
    private val observationBuffer = ConcurrentLinkedDeque<RadarBlip>()
    
    // Track historical patterns (e.g., Set of sensor types -> occurrence count)
    private val historicalPatterns = mutableMapOf<Set<String>, Int>()
    private val lastCorrelatedPairs = mutableMapOf<Set<String>, Long>()

    fun processObservation(newObservation: RadarBlip): List<CorrelationEvent> {
        val correlations = mutableListOf<CorrelationEvent>()
        
        // Add to buffer
        observationBuffer.addLast(newObservation)
        
        // Clean up old observations outside the temporal window (e.g., keeping slightly more than max temporal window)
        val cutoffTime = newObservation.timestampMs - (config.maxTemporalWindowMs * 2)
        while (observationBuffer.isNotEmpty() && observationBuffer.first().timestampMs < cutoffTime) {
            observationBuffer.pollFirst()
        }

        // Compare new observation against existing ones in the window
        val window = observationBuffer.toList()
        
        for (historicalObs in window) {
            if (historicalObs.id == newObservation.id) continue
            
            val timeSeparationMs = abs(newObservation.timestampMs - historicalObs.timestampMs)
            
            // 1. Temporal Correlation
            if (timeSeparationMs <= config.maxTemporalWindowMs) {
                // Ensure they are from different sensor types, or we can correlate same-sensor events too?
                // The objective mentions "observations from different sensor systems"
                if (historicalObs.type == newObservation.type && historicalObs.name == newObservation.name) {
                     continue // Same entity, not a multi-sensor correlation
                }

                val pairId = setOf(historicalObs.id, newObservation.id)
                val lastTime = lastCorrelatedPairs[pairId]
                if (lastTime == null || newObservation.timestampMs - lastTime > config.maxTemporalWindowMs * 2) {
                    val event = buildCorrelationEvent(historicalObs, newObservation, timeSeparationMs)
                    if (event.confidence >= config.minConfidenceThreshold) {
                        correlations.add(event)
                        lastCorrelatedPairs[pairId] = newObservation.timestampMs
                    }
                }
            }
        }
        
        return correlations
    }

    private fun buildCorrelationEvent(obs1: RadarBlip, obs2: RadarBlip, timeSeparationMs: Long): CorrelationEvent {
        val participatingSensors = setOf(obs1.type, obs2.type)
        
        // Track repeated co-occurrence
        val patternCount = historicalPatterns.getOrDefault(participatingSensors, 0) + 1
        historicalPatterns[participatingSensors] = patternCount

        // 2. Spatial Correlation
        // Both have `distance` and `targetAngleOffset` and `estimatedZOffsetMeters`
        // We can check if they are spatially proximate
        var spatialRel = SpatialRelationship.UNKNOWN
        // Basic spatial check based on distance delta for now, since angles might be imprecise
        val distDelta = abs(obs1.distance - obs2.distance)
        if (obs1.distance > 0 && obs2.distance > 0) {
             spatialRel = if (distDelta < 2.0f) {
                 SpatialRelationship.CO_LOCATED
             } else if (distDelta < 10.0f) {
                 SpatialRelationship.PROXIMATE
             } else {
                 SpatialRelationship.DISTANT
             }
        }

        // 3. Correlation Scoring
        var baseScore = 0.5f // Start with a base temporal correlation
        
        // Temporal factor (closer = higher score)
        val timeFactor = 1.0f - (timeSeparationMs.toFloat() / config.maxTemporalWindowMs.toFloat())
        baseScore += (timeFactor * 0.3f)
        
        // Spatial factor
        when (spatialRel) {
             SpatialRelationship.CO_LOCATED -> baseScore += 0.2f
             SpatialRelationship.PROXIMATE -> baseScore += 0.1f
             SpatialRelationship.DISTANT -> baseScore -= 0.1f
             SpatialRelationship.UNKNOWN -> {}
        }
        
        // 4. Repeated Correlation Bonus
        val repetitionBonus = min(0.3f, patternCount * config.repeatedOccurrenceBonus)
        baseScore += repetitionBonus
        
        // 5. Sensor Reliability & Data Quality
        // Consider anomalies
        if (obs1.anomalyResult != null || obs2.anomalyResult != null) {
             baseScore += 0.1f // Anomalous events occurring together are highly notable
        }

        val finalScore = baseScore.coerceIn(0.0f, 1.0f)
        
        // Confidence calculation
        var confidence = 0.6f
        confidence += repetitionBonus
        if (obs1.provenance == DataProvenance.SIMULATED || obs2.provenance == DataProvenance.SIMULATED) {
            confidence *= 0.8f // Lower confidence for simulated
        }
        if (spatialRel == SpatialRelationship.UNKNOWN) {
            confidence *= 0.9f
        }
        val finalConfidence = confidence.coerceIn(0.0f, 1.0f)

        // 6. Provenance merging
        val prov = if (obs1.provenance == DataProvenance.SIMULATED || obs2.provenance == DataProvenance.SIMULATED) {
            DataProvenance.SIMULATED
        } else if (obs1.provenance == DataProvenance.REPLAY || obs2.provenance == DataProvenance.REPLAY) {
            DataProvenance.REPLAY
        } else {
            DataProvenance.MEASURED
        }

        return CorrelationEvent(
            firstObservationMs = min(obs1.timestampMs, obs2.timestampMs),
            lastObservationMs = max(obs1.timestampMs, obs2.timestampMs),
            observations = listOf(obs1, obs2),
            participatingSensors = participatingSensors,
            maxTimeSeparationMs = timeSeparationMs,
            spatialRelationship = spatialRel,
            correlationScore = finalScore,
            confidence = finalConfidence,
            provenance = prov,
            notes = "Temporal proximity detected between ${obs1.type} and ${obs2.type}. Correlation does not imply causation."
        )
    }
}
