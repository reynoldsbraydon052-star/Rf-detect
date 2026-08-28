with open('app/src/main/java/com/example/IntelligenceApi.kt', 'r') as f:
    content = f.read()

content = content.replace("fun createSession(name: String) = sessionEngine.createSession(name)", "suspend fun createSession(name: String) = sessionEngine.createNewSession(name)")

with open('app/src/main/java/com/example/IntelligenceApi.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("@Composable\nfun SessionOverviewTab(api: IntelligenceApi, viewModel: SignalRadarViewModel) {\n    val activeSession by api.sessionState.collectAsStateWithLifecycle()", "@Composable\nfun SessionOverviewTab(api: IntelligenceApi, viewModel: SignalRadarViewModel) {\n    val activeSession by api.sessionState.collectAsStateWithLifecycle()\n    val scope = rememberCoroutineScope()")

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'w') as f:
    f.write(content)

