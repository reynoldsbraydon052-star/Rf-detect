package com.example

data class AiInvestigatorAssessment(
    val assessment: String = "",
    val confidence: Int = 0,
    val facts: List<String> = emptyList(),
    val unknowns: List<String> = emptyList(),
    val recommendedMeasurements: List<String> = emptyList(),
    val evidenceReferences: List<String> = emptyList(),
    val alternativeExplanations: List<String> = emptyList()
)

data class AiInterpretation(
    val assessment: AiInvestigatorAssessment,
    val confidence: Int,
    val operatingMode: OperatingMode
)

data class AiEvidencePackage(
    val observations: List<RadarBlip>,
    val baselineSummary: String,
    val anomalyScore: Float,
    val anomalyConfidence: Float,
    val anomalyExplanations: List<String>,
    val correlations: List<String>,
    val timestampsMs: Long,
    val locationUncertainty: LocalizationConfidence,
    val hardwareCapabilities: List<String>,
    val calibrationState: String,
    val provenance: DataProvenance,
    val isLive: Boolean,
    val isSimulation: Boolean,
    val isReplay: Boolean
)
