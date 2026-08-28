import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

# Add to enum
content = content.replace("    IDENTITY_GRAPH", "    IDENTITY_GRAPH,\n    INTELLIGENCE_DASHBOARD")

# Add properties to ViewModel
prop_decl = """    val deviceIdentityEngine = DeviceIdentityEngine(RfRecordingDatabase.getInstance(getApplication()).deviceIdentityDao())
    
    val sessionEngine = RfInvestigationSessionEngine(RfRecordingDatabase.getInstance(getApplication()).rfSessionDao())
    val anomalyEngine = RfAnomalyCorrelationEngine(RfRecordingDatabase.getInstance(getApplication()).rfAnomalyDao())
    val patternEngine = RfTemporalPatternEngine(RfRecordingDatabase.getInstance(getApplication()).rfPatternDao())
    val intelligenceEngine = RfIntelligenceCorrelationEngine(sessionEngine, anomalyEngine, patternEngine, deviceIdentityEngine)"""
content = content.replace("    val deviceIdentityEngine = DeviceIdentityEngine(RfRecordingDatabase.getInstance(getApplication()).deviceIdentityDao())", prop_decl)

# Initialize engines
init_block = """    init {
        viewModelScope.launch {
            sessionEngine.createNewSession("Investigation - " + java.util.UUID.randomUUID().toString().take(8))
        }
        viewModelScope.launch {
            deviceIdentityEngine.loadHypotheses()
        }
        viewModelScope.launch {"""
content = content.replace("""    init {
        viewModelScope.launch {
            deviceIdentityEngine.loadHypotheses()
        }
        viewModelScope.launch {""", init_block)

# Update map & identity & intel along with activeBlips update (approx every 65ms)
update_call = """            // Dispatch to mapping engine (background)
            viewModelScope.launch(Dispatchers.Default) {
                environmentMappingEngine.updateMap(
                    blips = evaluatedBlips,
                    headingDegrees = _uiState.value.headingDegrees,
                    userX = 0f, 
                    userY = 0f
                )
                
                deviceIdentityEngine.processObservations(evaluatedBlips, cachedFingerprints)
                
                val sessionId = sessionEngine.getActiveSessionId()
                if (sessionId != null) {
                    anomalyEngine.processEvents(evaluatedBlips, sessionId, environmentMappingEngine.mapState.value, deviceIdentityEngine.hypotheses.value)
                    patternEngine.processEvents(evaluatedBlips, sessionId)
                    intelligenceEngine.updateGraph()
                    
                    sessionEngine.updateSessionStats(
                        eventCount = rfEventRecorderEngine.recorderState.value.eventCount,
                        anomalyCount = anomalyEngine.anomalies.value.size,
                        deviceCount = deviceIdentityEngine.hypotheses.value.size,
                        mapCellCount = environmentMappingEngine.mapState.value.cells.size
                    )
                }
            }"""

content = content.replace("""            // Dispatch to mapping engine (background)
            viewModelScope.launch(Dispatchers.Default) {
                environmentMappingEngine.updateMap(
                    blips = evaluatedBlips,
                    headingDegrees = _uiState.value.headingDegrees,
                    userX = 0f, 
                    userY = 0f
                )
                
                deviceIdentityEngine.processObservations(evaluatedBlips, cachedFingerprints)
            }""", update_call)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
