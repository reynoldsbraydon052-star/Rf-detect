import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

func_update = """
    fun runAiInvestigator(query: String? = null) {
        _uiState.update { it.copy(isAiAnalyzingThreats = true) }
        viewModelScope.launch {
            val pkg = captureEvidencePackage()
            val assessment = geminiThreatService.runEvidenceInvestigator(pkg, query)
            _uiState.update { 
                it.copy(
                    investigatorAssessment = assessment,
                    isAiAnalyzingThreats = false
                )
            }
        }
    }

    fun saveAiInterpretation() {
        _uiState.update { state ->
            state.investigatorAssessment?.let { assessment ->
                val newInterpretation = AiInterpretation(
                    assessment = assessment,
                    confidence = assessment.confidence
                )
                state.copy(savedInterpretations = state.savedInterpretations + newInterpretation)
            } ?: state
        }
    }
"""

if "fun runAiInvestigator" not in content:
    content = content.replace("    fun runAiThreatAnalysis", func_update + "\n    fun runAiThreatAnalysis")
    with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
        f.write(content)
