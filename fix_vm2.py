with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("class SignalRadarViewModel(application: Application) : AndroidViewModel(application) {",
"""class SignalRadarViewModel(application: Application) : AndroidViewModel(application) {
    val sessionEngine = InvestigationSessionEngine()
    val replayEngine = ReplayEngine(this)
    val simulationEngine = SimulationLabEngine(this)
""")

content = content.replace(
"""    private fun processSignalIntercept(rawBlip: RadarBlip) {
        val anomaly = anomalyEngine.evaluateAnomaly(rawBlip, cachedFingerprints, _uiState.value.baselineSummary)""",
"""    private fun processSignalIntercept(rawBlip: RadarBlip) {
        if (_uiState.value.operatingMode == OperatingMode.LIVE) {
            sessionEngine.recordEvent(SessionEvent(
                timestampMs = System.currentTimeMillis(),
                type = SessionEventType.BLIP,
                blip = rawBlip
            ))
        }

        val anomaly = anomalyEngine.evaluateAnomaly(rawBlip, cachedFingerprints, _uiState.value.baselineSummary)"""
)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
