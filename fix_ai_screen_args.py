with open('app/src/main/java/com/example/AiThreatIntelScreen.kt', 'r') as f:
    content = f.read()

bad_args = """fun AiThreatIntelScreen(
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
) -> Unit,
    onSendCopilotQuery: (String) -> Unit,
    onSelectTargetOnRadar: (String) -> Unit,
    onOpenRadarTab: () -> Unit,
    onTriggerDeepAudit: (FlaggedThreatEmitter) -> Unit = {},
    onSaveInterpretation: () -> Unit = {},
    onCloseDeepAudit: () -> Unit = {}
) {"""

good_args = """fun AiThreatIntelScreen(
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

if bad_args in content:
    content = content.replace(bad_args, good_args)
else:
    print("Could not find the exact bad_args string to replace, I will try regex.")
    import re
    content = re.sub(r'\)\s*->\s*Unit,[\s\S]*?\)\s*\{', ') {', content, count=1)

with open('app/src/main/java/com/example/AiThreatIntelScreen.kt', 'w') as f:
    f.write(content)
