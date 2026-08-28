with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

target = """        viewModelScope.launch {
            settingsDataStore.maxVisibleDevices.collect { maxDevices ->
                _uiState.update { it.copy(maxVisibleDevices = maxDevices) }
            }
        }"""
        
replacement = target + """

        // Baseline settings and models
        viewModelScope.launch {
            settingsDataStore.isBaselineLearningMode.collect { isLearning ->
                val prev = _uiState.value.baselineSummary
                _uiState.update { it.copy(baselineSummary = prev.copy(isLearning = isLearning)) }
            }
        }
        viewModelScope.launch {
            bleDatabase.signalFingerprintDao().getAllFingerprintsFlow().collect { entities ->
                cachedFingerprints = entities.associateBy { it.id }.mapValues { it.value.toDomainModel() }
            }
        }"""
        
if "cachedFingerprints" not in content:
    content = content.replace(target, replacement)
    
target_vars = "private val blipMap = mutableMapOf<String, RadarBlip>()"
replacement_vars = target_vars + "\n    private var cachedFingerprints = mapOf<String, SignalFingerprint>()\n    private var cachedBaselineStats: BaselineStats = BaselineStats()"

if "cachedBaselineStats" not in content:
    content = content.replace(target_vars, replacement_vars)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
