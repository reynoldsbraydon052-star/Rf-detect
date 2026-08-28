with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

func_code = """
    fun captureEvidencePackage(targetBlip: RadarBlip? = null): AiEvidencePackage {
        val currentState = _uiState.value
        
        val hardwareCaps = mutableListOf<String>()
        if (hardwareManager.isSdrConnected.value) hardwareCaps.add("USB SDR attached (24MHz-1766MHz)")
        if (bleScanner.isScanning.value) hardwareCaps.add("BLE Scanner")
        hardwareCaps.add("Magnetometer")
        hardwareCaps.add("Audio/Ultrasonic Mic")
        
        val baseline = baselineEngine.baselineState.value
        val baselineSummary = "Environment Baseline: known MACs=${baseline.knownMacAddresses.size}, normal freq=${baseline.averageUltrasonicFreqHz}Hz"
        
        val obs = if (targetBlip != null) listOf(targetBlip) else currentState.activeBlips.toList()
        
        val correlations = correlationEngine.correlatedPairs.value
            .map { "Correlation: ${it.description} (Score: ${it.correlationScore}, Confidence: ${it.confidence})" }

        return AiEvidencePackage(
            observations = obs,
            baselineSummary = baselineSummary,
            anomalyScore = currentState.globalAnomalyScore,
            anomalyConfidence = currentState.globalAnomalyConfidence,
            anomalyExplanations = currentState.anomalyExplanations.toList(),
            correlations = correlations,
            timestampsMs = System.currentTimeMillis(),
            locationUncertainty = LocalizationConfidence.MEDIUM,
            hardwareCapabilities = hardwareCaps,
            calibrationState = "Calibrated", // Simplified
            provenance = if (currentState.isSimulationModeActive) DataProvenance.SIMULATED else DataProvenance.MEASURED,
            isLive = !currentState.isReplayModeActive && !currentState.isSimulationModeActive,
            isSimulation = currentState.isSimulationModeActive,
            isReplay = currentState.isReplayModeActive
        )
    }
"""

if "fun captureEvidencePackage" not in content:
    content = content.replace("    fun captureRfSnapshot", func_code + "\n    fun captureRfSnapshot")
    with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
        f.write(content)
