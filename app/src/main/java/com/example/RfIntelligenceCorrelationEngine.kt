package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class RfIntelligenceCorrelationEngine(
    private val sessionEngine: RfInvestigationSessionEngine,
    private val anomalyEngine: RfAnomalyCorrelationEngine,
    private val patternEngine: RfTemporalPatternEngine,
    private val identityEngine: DeviceIdentityEngine
) {

    private val _graph = MutableStateFlow(IntelligenceGraph(emptyList(), emptyList()))
    val graph: StateFlow<IntelligenceGraph> = _graph.asStateFlow()

    private var lastUpdateMs = 0L

    suspend fun updateGraph(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastUpdateMs < 5000L) {
            return // Throttle updates
        }
        lastUpdateMs = now
        
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val nodes = mutableListOf<CorrelationNode>()
            val edges = mutableListOf<CorrelationEdge>()
            val sessionId = sessionEngine.getActiveSessionId() ?: return@withContext
            
            nodes.add(CorrelationNode(sessionId, NodeType.SESSION, "Active Session", now))
            
            val hypotheses = identityEngine.hypotheses.value
            hypotheses.values.forEach { h ->
                nodes.add(CorrelationNode(h.id, NodeType.DEVICE, h.primaryMac, h.lastSeenMs))
                edges.add(CorrelationEdge(h.id, sessionId, 100, "BELONGS_TO_SESSION", "Device active in session"))
            }
            
            val anomalies = anomalyEngine.anomalies.value
            anomalies.forEach { a ->
                nodes.add(CorrelationNode(a.id, NodeType.ANOMALY, a.type, a.timestampMs))
                edges.add(CorrelationEdge(a.id, sessionId, 100, "BELONGS_TO_SESSION", "Anomaly detected in session"))
                
                if (a.deviceId != null) {
                    val raw = a.deviceId!!
                    edges.add(CorrelationEdge(a.id, raw, a.confidenceScore, "AFFECTS_DEVICE", "Anomaly involves device"))
                }
            }
            
            val patterns = patternEngine.patterns.value
            patterns.forEach { p ->
                nodes.add(CorrelationNode(p.id, NodeType.PATTERN, p.type, p.lastObservedMs))
                edges.add(CorrelationEdge(p.id, sessionId, 100, "BELONGS_TO_SESSION", "Pattern detected in session"))
                
                if (p.deviceHypothesisId != null) {
                    edges.add(CorrelationEdge(p.id, p.deviceHypothesisId, p.confidenceScore, "EXHIBITS_PATTERN", "Device exhibits pattern"))
                }
            }
            
            _graph.update { IntelligenceGraph(nodes, edges) }
        }
    }
}
