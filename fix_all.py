import re

# 1. Fix ReplayEngine
with open('app/src/main/java/com/example/ReplayEngine.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""        val anomalyResult = if (entity.anomalyScore != null || entity.classification != null) {
            AnomalyResult(
                score = entity.anomalyScore?.toDouble() ?: 0.0,
                isAnomaly = entity.anomalyScore != null && entity.anomalyScore > 0.5f,
                confidence = entity.classificationConfidence ?: 0f,
                explanations = if (entity.classification != null) listOf(AnomalyExplanation(entity.classification, 1.0, "Classification")) else emptyList()
            )
        } else null""",
"""        val anomalyResult = if (entity.anomalyScore != null || entity.classification != null) {
            AnomalyResult(
                score = entity.anomalyScore?.toInt() ?: 0,
                confidence = entity.classificationConfidence ?: 0f,
                explanations = if (entity.classification != null) listOf(AnomalyExplanation(entity.classification, 1)) else emptyList()
            )
        } else null"""
)
with open('app/src/main/java/com/example/ReplayEngine.kt', 'w') as f:
    f.write(content)

# 2. Fix SignalRadarViewModel
with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""    fun clearReplayState() {
        // Clear active replay data
        _activeBlips.value = emptyList()
        _selectedTargetDeviceId.value = null
        signalHistoryLogger.clearHistory()
        // Reset environment map if applicable
    }""",
"""    fun clearReplayState() {
        _uiState.update { it.copy(activeBlips = emptyList(), selectedTargetDeviceId = null) }
        // reset environment map if applicable
    }"""
)

content = content.replace(
"""        val reconstructedBlips = activeDeviceEvents.map { entity ->
            val anomalyResult = if (entity.anomalyScore != null || entity.classification != null) {
                AnomalyResult(
                    score = entity.anomalyScore?.toDouble() ?: 0.0,
                    isAnomaly = entity.anomalyScore != null && entity.anomalyScore > 0.5f,
                    confidence = entity.classificationConfidence ?: 0f,
                    explanations = if (entity.classification != null) listOf(AnomalyExplanation(entity.classification, 1.0, "Classification")) else emptyList()
                )
            } else null""",
"""        val reconstructedBlips = activeDeviceEvents.map { entity ->
            val anomalyResult = if (entity.anomalyScore != null || entity.classification != null) {
                AnomalyResult(
                    score = entity.anomalyScore?.toInt() ?: 0,
                    confidence = entity.classificationConfidence ?: 0f,
                    explanations = if (entity.classification != null) listOf(AnomalyExplanation(entity.classification, 1)) else emptyList()
                )
            } else null"""
)

content = content.replace(
"""        _activeBlips.value = reconstructedBlips
        signalHistoryLogger.clearHistory() // or rebuild history if we want
        reconstructedBlips.forEach { signalHistoryLogger.logSignal(it.id, it.rssi, it.frequencyMhz) }""",
"""        _uiState.update { it.copy(activeBlips = reconstructedBlips) }"""
)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)

# 3. Fix MainActivity
with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "val activeSession by viewModel.sessionEngine.currentSession.collectAsStateWithLifecycle()",
    "val activeSession by viewModel.rfSessionEngine.activeSession.collectAsStateWithLifecycle()"
)
content = content.replace(
    "val savedSessions by viewModel.sessionEngine.savedSessions.collectAsStateWithLifecycle()",
    "val savedSessions by viewModel.rfSessionEngine.allSessions.collectAsStateWithLifecycle(initialValue = emptyList())"
)

# 4. Fix SimulationLabScreen
with open('app/src/main/java/com/example/SimulationLabScreen.kt', 'r') as f:
    sim_content = f.read()

sim_content = sim_content.replace(
    "activeSession: InvestigationSession?,",
    "activeSession: RfSessionEntity?,"
)
sim_content = sim_content.replace(
    "savedSessions: List<InvestigationSession>,",
    "savedSessions: List<RfSessionEntity>,"
)
sim_content = sim_content.replace(
    "onLoadReplay: (InvestigationSession) -> Unit,",
    "onLoadReplay: (RfSessionEntity) -> Unit,"
)
# Inside SimulationLabScreen, it references session.name, session.startTimeMs, session.totalDurationMs
# RfSessionEntity doesn't have totalDurationMs. So we can use (session.endTimeMs ?: System.currentTimeMillis()) - session.startTimeMs
sim_content = sim_content.replace(
    "session.totalDurationMs",
    "((session.endTimeMs ?: System.currentTimeMillis()) - session.startTimeMs)"
)
sim_content = sim_content.replace(
    "val df = remember { SimpleDateFormat(\"yyyy-MM-dd HH:mm\", Locale.US) }",
    "val df = remember { java.text.SimpleDateFormat(\"yyyy-MM-dd HH:mm\", java.util.Locale.US) }"
)

with open('app/src/main/java/com/example/SimulationLabScreen.kt', 'w') as f:
    f.write(sim_content)


