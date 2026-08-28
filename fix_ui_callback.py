import re

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("fun PastSessionsTab(sessionEngine: RfInvestigationSessionEngine)", "fun PastSessionsTab(sessionEngine: RfInvestigationSessionEngine, onResume: (String) -> Unit)")
content = content.replace("1 -> PastSessionsTab(sessionEngine)", "1 -> PastSessionsTab(sessionEngine) { sessionId -> viewModel.resumeInvestigationSession(sessionId) }")

content = content.replace("IntelligenceDashboardScreen(\n    uiState: SignalRadarUiState,", "IntelligenceDashboardScreen(\n    viewModel: SignalRadarViewModel,\n    uiState: SignalRadarUiState,")
content = content.replace("RadarTab.INTELLIGENCE_DASHBOARD -> IntelligenceDashboardScreen(\n                            uiState = uiState,", "RadarTab.INTELLIGENCE_DASHBOARD -> IntelligenceDashboardScreen(\n                            viewModel = viewModel,\n                            uiState = uiState,")

content = content.replace("""                        Button(onClick = { 
                            // Note: This needs to also inform ViewModel to reload hypotheses
                            // Currently just calls resume on engine
                        }) {
                            Text("Resume")
                        }""", """                        Button(onClick = { onResume(session.id) }) {
                            Text("Resume")
                        }""")

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    main = f.read()

main = main.replace("RadarTab.INTELLIGENCE_DASHBOARD -> IntelligenceDashboardScreen(\n                            uiState = uiState,", "RadarTab.INTELLIGENCE_DASHBOARD -> IntelligenceDashboardScreen(\n                            viewModel = viewModel,\n                            uiState = uiState,")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(main)
