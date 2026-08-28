with open('app/src/main/java/com/example/AiThreatIntelScreen.kt', 'r') as f:
    content = f.read()

bad = """fun AiThreatIntelScreen(
    uiState: SignalRadarUiState,
    threatReport: ThreatAnalysisReport?,
    investigatorAssessment: AiInvestigatorAssessment? = null,
    isAnalyzing: Boolean,
    copilotMessages: List<TacticalCopilotMessage>,
    isCopilotThinking: Boolean,
    selectedDeepAuditTarget: DetailedTargetAudit? = null,
    isDeepAuditingEmitterId: String? = null,
    onRunAiThreatScan: () {"""

fixed = """fun AiThreatIntelScreen(
    uiState: SignalRadarUiState,
    threatReport: ThreatAnalysisReport?,
    investigatorAssessment: AiInvestigatorAssessment? = null,
    isAnalyzing: Boolean,
    copilotMessages: List<TacticalCopilotMessage>,
    isCopilotThinking: Boolean,
    selectedDeepAuditTarget: DetailedTargetAudit? = null,
    isDeepAuditingEmitterId: String? = null,
    onRunAiThreatScan: () -> Unit,
    onSendCopilotQuery: (String) -> Unit,
    onSelectTargetOnRadar: (String) -> Unit,
    onOpenRadarTab: () -> Unit,
    onTriggerDeepAudit: (FlaggedThreatEmitter) -> Unit = {},
    onSaveInterpretation: () -> Unit = {},
    onCloseDeepAudit: () -> Unit = {}
) {"""

content = content.replace(bad, fixed)

with open('app/src/main/java/com/example/AiThreatIntelScreen.kt', 'w') as f:
    f.write(content)
