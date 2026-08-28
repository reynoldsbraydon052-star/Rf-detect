import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

# Replace the new ones with rf* prefixes
content = content.replace("val sessionEngine = RfInvestigationSessionEngine(", "val rfSessionEngine = RfInvestigationSessionEngine(")
content = content.replace("val anomalyEngine = RfAnomalyCorrelationEngine(", "val rfAnomalyEngine = RfAnomalyCorrelationEngine(")
content = content.replace("val patternEngine = RfTemporalPatternEngine(", "val rfPatternEngine = RfTemporalPatternEngine(")
content = content.replace("val intelligenceEngine = RfIntelligenceCorrelationEngine(sessionEngine, anomalyEngine, patternEngine, deviceIdentityEngine)", "val rfIntelligenceEngine = RfIntelligenceCorrelationEngine(rfSessionEngine, rfAnomalyEngine, rfPatternEngine, deviceIdentityEngine)")

# Fix init block
content = content.replace("sessionEngine.createNewSession", "rfSessionEngine.createNewSession")

# Fix update block
content = content.replace("val sessionId = sessionEngine.getActiveSessionId()", "val sessionId = rfSessionEngine.getActiveSessionId()")
content = content.replace("anomalyEngine.processEvents", "rfAnomalyEngine.processEvents")
content = content.replace("patternEngine.processEvents", "rfPatternEngine.processEvents")
content = content.replace("intelligenceEngine.updateGraph()", "rfIntelligenceEngine.updateGraph()")
content = content.replace("sessionEngine.updateSessionStats", "rfSessionEngine.updateSessionStats")
content = content.replace("anomalyCount = anomalyEngine.anomalies.value.size", "anomalyCount = rfAnomalyEngine.anomalies.value.size")

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
