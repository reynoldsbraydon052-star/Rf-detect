package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

// Baseline tracking per device
data class DeviceBaseline(
    val deviceId: String,
    var firstSeenMs: Long = 0L,
    var lastSeenMs: Long = 0L,
    var observationCount: Int = 0,
    var rssiSum: Float = 0f,
    var rssiSqrSum: Float = 0f,
    val frequencies: MutableSet<Double> = mutableSetOf()
) {
    val meanRssi: Float get() = if (observationCount > 0) rssiSum / observationCount else 0f
    val stdDevRssi: Float get() = if (observationCount > 1) {
        val mean = meanRssi
        Math.sqrt(Math.max(0.0, (rssiSqrSum - observationCount * mean * mean) / (observationCount - 1).toDouble())).toFloat()
    } else 0f
}

// Global baseline stats
class BaselineEngine {
    val deviceBaselines = mutableMapOf<String, DeviceBaseline>()
    var globalObservationCount = 0
    val newDeviceTimes = mutableListOf<Long>()
    
    fun update(blip: RadarBlip) {
        globalObservationCount++
        
        val baseline = deviceBaselines.getOrPut(blip.id) { 
            newDeviceTimes.add(blip.timestampMs)
            DeviceBaseline(deviceId = blip.id, firstSeenMs = blip.timestampMs) 
        }
        
        baseline.lastSeenMs = blip.timestampMs
        baseline.observationCount++
        baseline.rssiSum += blip.rssi
        baseline.rssiSqrSum += (blip.rssi * blip.rssi)
        baseline.frequencies.add(blip.frequencyMhz)
        
        // Clean up old newDeviceTimes
        val now = System.currentTimeMillis()
        newDeviceTimes.removeAll { now - it > 60_000 }
    }
}

class RfAnomalyCorrelationEngine(
    private val anomalyDao: RfAnomalyDao,
    private val correlationDao: AnomalyCorrelationDao
) {
    private val _anomalies = MutableStateFlow<List<RfAnomalyEntity>>(emptyList())
    val anomalies: StateFlow<List<RfAnomalyEntity>> = _anomalies.asStateFlow()
    
    private val _correlations = MutableStateFlow<List<AnomalyCorrelationEntity>>(emptyList())
    val correlations: StateFlow<List<AnomalyCorrelationEntity>> = _correlations.asStateFlow()

    private val baselineEngine = BaselineEngine()
    private val mutex = Mutex()

    fun loadAnomaliesForSession(sessionId: String) = anomalyDao.getAnomaliesForSessionFlow(sessionId)
    fun loadCorrelations() = correlationDao.getAllCorrelationsFlow()

    suspend fun processEvents(
        events: List<RadarBlip>, 
        sessionId: String,
        environmentMap: EnvironmentMapState,
        hypotheses: Map<String, DeviceIdentityHypothesis>
    ) = mutex.withLock {
        val newAnomalies = mutableListOf<RfAnomalyEntity>()
        val now = System.currentTimeMillis()
        
        for (blip in events) {
            if (blip.provenance != DataProvenance.MEASURED) continue

            val isNew = !baselineEngine.deviceBaselines.containsKey(blip.id)
            val baseline = baselineEngine.deviceBaselines[blip.id]
            
            baselineEngine.update(blip)
            
            val updatedBaseline = baselineEngine.deviceBaselines[blip.id]!!
            
            // Requirements:
            // - minimum observations
            // - minimum deviation
            
            // 1. SUDDEN_SIGNAL_APPEARANCE
            if (isNew && updatedBaseline.observationCount == 1 && blip.rssi > -60) {
                // Not enough baseline to call it an anomaly right away unless very strong?
                // Let's only trigger if observationCount reaches a threshold
            }
            if (updatedBaseline.observationCount == 15 && (updatedBaseline.lastSeenMs - updatedBaseline.firstSeenMs) < 30_000) {
                 // Appeared suddenly and sustained
                 newAnomalies.add(createAnomaly(
                     sessionId = sessionId,
                     timestampMs = blip.timestampMs,
                     type = TemporalAnomalyType.SUDDEN_SIGNAL_APPEARANCE,
                     severity = AnomalySeverity.LOW,
                     confidence = 80,
                     evidenceScore = 50,
                     deviceId = blip.id,
                     freq = blip.frequencyMhz,
                     band = blip.bandLabel,
                     sourceEvents = listOf(blip.id),
                     supporting = listOf("Device appeared recently with ${updatedBaseline.observationCount} observations."),
                     contradicting = listOf(),
                     baselineInfo = "New device",
                     algo = "AppearanceDetector",
                     version = "1.0.0"
                 ))
            }
            
            // 2. RSSI_ANOMALY
            if (updatedBaseline.observationCount > 20) {
                val mean = updatedBaseline.meanRssi
                val stdDev = updatedBaseline.stdDevRssi.coerceAtLeast(3f)
                val diff = Math.abs(blip.rssi - mean)
                
                if (diff > stdDev * 3 && diff > 10f) {
                    val severity = if (diff > 20f) AnomalySeverity.HIGH else AnomalySeverity.MEDIUM
                    newAnomalies.add(createAnomaly(
                        sessionId = sessionId,
                        timestampMs = blip.timestampMs,
                        type = TemporalAnomalyType.RSSI_ANOMALY,
                        severity = severity,
                        confidence = 85,
                        evidenceScore = 60,
                        deviceId = blip.id,
                        freq = blip.frequencyMhz,
                        band = blip.bandLabel,
                        sourceEvents = listOf(blip.id),
                        supporting = listOf("RSSI deviated by ${diff}dB from mean $mean"),
                        contradicting = listOf("Could be natural fading"),
                        baselineInfo = "Mean: $mean, StdDev: $stdDev",
                        algo = "RssiDeviation",
                        version = "1.1.0"
                    ))
                }
            }
            
            // 3. FREQUENCY_ANOMALY
            if (updatedBaseline.observationCount > 10 && !baseline!!.frequencies.contains(blip.frequencyMhz)) {
                newAnomalies.add(createAnomaly(
                    sessionId = sessionId,
                    timestampMs = blip.timestampMs,
                    type = TemporalAnomalyType.FREQUENCY_ANOMALY,
                    severity = AnomalySeverity.MEDIUM,
                    confidence = 90,
                    evidenceScore = 70,
                    deviceId = blip.id,
                    freq = blip.frequencyMhz,
                    band = blip.bandLabel,
                    sourceEvents = listOf(blip.id),
                    supporting = listOf("Unexpected frequency ${blip.frequencyMhz} MHz observed."),
                    contradicting = listOf(),
                    baselineInfo = "Historical freqs: ${baseline.frequencies.joinToString()}",
                    algo = "FreqMonitor",
                    version = "1.0.0"
                ))
            }
        }
        
        // 4. NEW_DEVICE_CLUSTER
        val recentNewDevices = baselineEngine.newDeviceTimes.count { now - it < 10_000 }
        if (recentNewDevices >= 5 && events.isNotEmpty()) {
            newAnomalies.add(createAnomaly(
                sessionId = sessionId,
                timestampMs = now,
                type = TemporalAnomalyType.NEW_DEVICE_CLUSTER,
                severity = AnomalySeverity.HIGH,
                confidence = 95,
                evidenceScore = 80,
                deviceId = null,
                freq = null,
                band = null,
                sourceEvents = emptyList(),
                supporting = listOf("$recentNewDevices previously unseen observations within 10 seconds."),
                contradicting = listOf("Could be a randomized MAC burst"),
                baselineInfo = "Typical arrival rate is lower",
                algo = "ClusterDetector",
                version = "1.0.0"
            ))
            // Clear to avoid spam
            baselineEngine.newDeviceTimes.clear()
        }

        if (newAnomalies.isNotEmpty()) {
            _anomalies.update { it + newAnomalies }
            newAnomalies.forEach { anomalyDao.insertAnomaly(it) }
            
            // Correlate new anomalies
            correlateAnomalies(newAnomalies)
        }
    }
    
    private suspend fun correlateAnomalies(newAnomalies: List<RfAnomalyEntity>) {
        val currentAnomalies = _anomalies.value
        val newCorrelations = mutableListOf<AnomalyCorrelationEntity>()
        
        for (newAnomaly in newAnomalies) {
            val related = currentAnomalies.filter { 
                it.id != newAnomaly.id && 
                it.timestampMs >= newAnomaly.timestampMs - 60_000 &&
                (it.deviceId == newAnomaly.deviceId || it.type == newAnomaly.type)
            }
            
            if (related.isNotEmpty()) {
                val ids = related.map { it.id } + newAnomaly.id
                val corr = AnomalyCorrelationEntity(
                    correlationId = UUID.randomUUID().toString(),
                    relatedAnomalyIdsJson = "[\"" + ids.joinToString("\",\"") + "\"]",
                    confidence = 85,
                    correlationType = AnomalyCorrelationType.TEMPORAL.name,
                    timeRelationship = "Concurrent within 60s",
                    spatialRelationship = "Unknown",
                    frequencyRelationship = "Unknown",
                    deviceRelationship = if (related.any { it.deviceId == newAnomaly.deviceId }) "Same Device" else "None",
                    supportingEvidenceJson = "[\"Multiple anomalies detected in tight time window\"]",
                    contradictingEvidenceJson = "[]",
                    provenance = "MEASURED"
                )
                newCorrelations.add(corr)
            }
        }
        
        if (newCorrelations.isNotEmpty()) {
            _correlations.update { it + newCorrelations }
            newCorrelations.forEach { correlationDao.insertCorrelation(it) }
        }
    }

    private fun createAnomaly(
        sessionId: String,
        timestampMs: Long,
        type: TemporalAnomalyType,
        severity: AnomalySeverity,
        confidence: Int,
        evidenceScore: Int,
        deviceId: String?,
        freq: Double?,
        band: String?,
        sourceEvents: List<String>,
        supporting: List<String>,
        contradicting: List<String>,
        baselineInfo: String,
        algo: String,
        version: String
    ): RfAnomalyEntity {
        return RfAnomalyEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            timestampMs = timestampMs,
            startTimestampMs = timestampMs - 5000,
            endTimestampMs = timestampMs,
            type = type.name,
            severity = severity.name,
            confidenceScore = confidence,
            evidenceScore = evidenceScore,
            deviceId = deviceId,
            frequencyMhz = freq,
            band = band,
            locationReference = null,
            sourceEventIdsJson = "[\"" + sourceEvents.joinToString("\",\"") + "\"]",
            relatedPatternIdsJson = "[]",
            relatedIdentityHypothesisId = null,
            baselineInformationJson = baselineInfo,
            detectionAlgorithm = algo,
            algorithmVersion = version,
            supportingEvidenceJson = "[\"" + supporting.joinToString("\",\"") + "\"]",
            contradictingEvidenceJson = "[\"" + contradicting.joinToString("\",\"") + "\"]",
            status = AnomalyStatus.NEW.name,
            provenance = "MEASURED",
            spatialX = null,
            spatialY = null
        )
    }
    
    // UI actions
    fun updateAnomalyStatus(anomalyId: String, newStatus: AnomalyStatus) {
        val updated = _anomalies.value.map {
            if (it.id == anomalyId) it.copy(status = newStatus.name) else it
        }
        _anomalies.value = updated
        // In real app, launch coroutine to update DB
    }
}
