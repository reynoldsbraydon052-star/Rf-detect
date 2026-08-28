package com.example

import kotlinx.coroutines.flow.StateFlow
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InvestigationSummary(
    val activeSession: RfSessionEntity?,
    val eventCount: Int,
    val deviceCount: Int,
    val anomalyCount: Int,
    val patternCount: Int,
    val graphRelationships: Int
)

class IntelligenceApi(
    private val sessionEngine: RfInvestigationSessionEngine,
    private val recorderEngine: RfEventRecorderEngine,
    private val identityEngine: DeviceIdentityEngine,
    private val anomalyEngine: RfAnomalyCorrelationEngine,
    private val patternEngine: RfTemporalPatternEngine,
    private val mapEngine: RfEnvironmentMappingEngine,
    private val correlationEngine: RfIntelligenceCorrelationEngine,
    private val crossSessionEngine: RfCrossSessionAnalysisEngine,
    private val evidenceEngine: EvidenceEngine
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    // Reactive StateFlows
    val sessionState: StateFlow<RfSessionEntity?> = sessionEngine.activeSession
    val recordingState: StateFlow<RecorderState> = recorderEngine.recorderState
    val hypotheses: StateFlow<Map<String, DeviceIdentityHypothesis>> = identityEngine.hypotheses
    val anomalies: StateFlow<List<RfAnomalyEntity>> = anomalyEngine.anomalies
    val patterns: StateFlow<List<RfPatternEntity>> = patternEngine.patterns
    val intelligenceGraph: StateFlow<IntelligenceGraph> = correlationEngine.graph
    val evidenceList: StateFlow<List<EvidenceItem>> = evidenceEngine.evidenceList
    
    // Derived States
    val investigationSummary: InvestigationSummary
        get() = InvestigationSummary(
            activeSession = sessionState.value,
            eventCount = recordingState.value.totalRecordedEventsSession,
            deviceCount = hypotheses.value.size,
            anomalyCount = anomalies.value.size,
            patternCount = patterns.value.size,
            graphRelationships = intelligenceGraph.value.edges.size
        )

    fun startRecording() = recorderEngine.startRecording()
    fun stopRecording() = recorderEngine.stopRecording()
    
    suspend fun createSession(name: String) = sessionEngine.createNewSession(name)
    suspend fun pauseSession() = sessionEngine.pauseSession()
    suspend fun resumeSession(sessionId: String) {
        sessionEngine.resumeSession(sessionId)
        val id = sessionEngine.getActiveSessionId()
        if (id != null) {
            identityEngine.loadHypothesesForSession(id)
            anomalyEngine.loadAnomaliesForSession(id)
            patternEngine.loadPatternsForSession(id)
            evidenceEngine.loadEvidenceForSession(id)
            correlationEngine.updateGraph(force = true)
        }
    }
    suspend fun archiveSession(sessionId: String) = sessionEngine.archiveSession(sessionId)
    suspend fun deleteSession(sessionId: String) = sessionEngine.deleteSession(sessionId)
    suspend fun closeSession() = sessionEngine.closeSession()
    
    suspend fun processObservations(blips: List<RadarBlip>, cachedFingerprints: Map<String, SignalFingerprint>, threshold: Float) {
        val sessionId = sessionEngine.getActiveSessionId() ?: return
        
        withContext(Dispatchers.Default) {
            recorderEngine.processObservations(blips, null)
            identityEngine.processObservations(blips, cachedFingerprints, sessionId)
            anomalyEngine.processEvents(blips, sessionId, mapEngine.mapState.value, identityEngine.hypotheses.value)
            patternEngine.processEvents(blips, sessionId)
            correlationEngine.updateGraph()
            
            sessionEngine.updateSessionStats(
                eventCount = recorderEngine.recorderState.value.totalRecordedEventsSession,
                anomalyCount = anomalyEngine.anomalies.value.size,
                deviceCount = identityEngine.hypotheses.value.size,
                mapCellCount = mapEngine.mapState.value.cells.size
            )
        }
    }
    
    suspend fun generateExport(uiState: SignalRadarUiState): String {
        return withContext(Dispatchers.Default) {
            recorderEngine.generateJsonExport(
                uiState = uiState,
                hypotheses = hypotheses.value.values.toList(),
                anomalies = anomalies.value,
                patterns = patterns.value
            )
        }
    }
}
