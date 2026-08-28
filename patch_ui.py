import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("viewModel.runAiThreatAnalysis()", "viewModel.runAiInvestigator()")
content = content.replace("threatReport = uiState.threatAnalysisReport", "threatReport = uiState.threatAnalysisReport,\n                            investigatorAssessment = uiState.investigatorAssessment,\n                            onSaveInterpretation = { viewModel.saveAiInterpretation() }")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/AiThreatIntelScreen.kt', 'r') as f:
    content = f.read()

func_sig = """
fun AiThreatIntelScreen(
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
)"""
content = re.sub(r'fun AiThreatIntelScreen\([^)]+\)', func_sig.strip(), content)

with open('app/src/main/java/com/example/AiThreatIntelScreen.kt', 'w') as f:
    f.write(content)

