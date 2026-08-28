import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

# Add to enum
content = content.replace("    ENVIRONMENT_MAP", "    ENVIRONMENT_MAP,\n    IDENTITY_GRAPH")

# Add property to ViewModel
prop_decl = """    val environmentMappingEngine = RfEnvironmentMappingEngine()
    val deviceIdentityEngine = DeviceIdentityEngine(RfRecordingDatabase.getInstance(getApplication()).deviceIdentityDao())"""
content = content.replace("    val environmentMappingEngine = RfEnvironmentMappingEngine()", prop_decl)

# Initialize Identity Engine in init
init_block = """    init {
        viewModelScope.launch {
            deviceIdentityEngine.loadHypotheses()
        }
        viewModelScope.launch {"""
content = content.replace("""    init {
        viewModelScope.launch {""", init_block)

# Update map & identity along with activeBlips update (approx every 65ms)
update_call = """            // Dispatch to mapping engine (background)
            viewModelScope.launch(Dispatchers.Default) {
                environmentMappingEngine.updateMap(
                    blips = evaluatedBlips,
                    headingDegrees = _uiState.value.headingDegrees,
                    userX = 0f, 
                    userY = 0f
                )
                
                deviceIdentityEngine.processObservations(evaluatedBlips, cachedFingerprints)
            }"""

content = content.replace("""            // Dispatch to mapping engine (background)
            viewModelScope.launch(Dispatchers.Default) {
                environmentMappingEngine.updateMap(
                    blips = evaluatedBlips,
                    headingDegrees = _uiState.value.headingDegrees,
                    userX = 0f, 
                    userY = 0f
                )
            }""", update_call)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
