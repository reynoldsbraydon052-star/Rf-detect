import re

with open('app/src/main/java/com/example/RfIntelligenceCorrelationEngine.kt', 'r') as f:
    content = f.read()

# Make updateGraph a suspend function and do processing off-thread
engine_methods = """
    private var lastUpdateMs = 0L

    suspend fun updateGraph(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastUpdateMs < 5000L) {
            return // Throttle updates
        }
        lastUpdateMs = now
        
        kotlinx.coroutines.Dispatchers.Default.invoke {
            val nodes = mutableListOf<CorrelationNode>()
            val edges = mutableListOf<CorrelationEdge>()
            val sessionId = sessionEngine.getActiveSessionId() ?: return@invoke
            
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
                
                if (a.relatedDeviceIdsJson.isNotEmpty() && a.relatedDeviceIdsJson != "[]") {
                    val raw = a.relatedDeviceIdsJson.removePrefix("[\"").removeSuffix("\"]")
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
"""

content = re.sub(r"fun updateGraph\(\) \{.*_graph\.update \{ IntelligenceGraph\(nodes, edges\) \}\n    \}", engine_methods.strip(), content, flags=re.DOTALL)

with open('app/src/main/java/com/example/RfIntelligenceCorrelationEngine.kt', 'w') as f:
    f.write(content)
