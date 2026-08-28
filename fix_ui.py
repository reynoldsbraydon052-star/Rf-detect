import re
with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'r') as f:
    content = f.read()

# Fix PastSessionsTab signature
content = content.replace("fun PastSessionsTab(sessionEngine: RfInvestigationSessionEngine, onResume: (String) -> Unit) {", "fun PastSessionsTab(api: IntelligenceApi, sessionEngine: RfInvestigationSessionEngine, onResume: (String) -> Unit) {")

content = content.replace("1 -> PastSessionsTab(sessionEngine) { sessionId ->", "1 -> PastSessionsTab(api, viewModel.rfSessionEngine) { sessionId ->")

# Fix suspend resumeSession
content = re.sub(r'api\.resumeSession\(session\.id\)', r'scope.launch { api.resumeSession(session.id) }', content)
content = re.sub(r'\{ sessionId -> api\.resumeSession\(sessionId\) \}', r'{ sessionId -> scope.launch { api.resumeSession(sessionId) } }', content)

# Fix MainActivity.kt missing api
with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    main_content = f.read()

main_content = main_content.replace(
    "IntelligenceDashboardScreen(viewModel.intelligenceApi, viewModel, uiState, viewModel.rfSessionEngine, viewModel.rfAnomalyEngine, viewModel.rfPatternEngine, viewModel.rfIntelligenceEngine)",
    "IntelligenceDashboardScreen(api = viewModel.intelligenceApi, viewModel = viewModel, uiState = uiState, sessionEngine = viewModel.rfSessionEngine, anomalyEngine = viewModel.rfAnomalyEngine, patternEngine = viewModel.rfPatternEngine, intelligenceEngine = viewModel.rfIntelligenceEngine)"
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(main_content)

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'w') as f:
    f.write(content)
