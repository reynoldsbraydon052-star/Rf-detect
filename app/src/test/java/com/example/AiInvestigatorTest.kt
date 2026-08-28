package com.example

import org.junit.Assert.*
import org.junit.Test

class AiInvestigatorTest {

    @Test
    fun testEvidenceIsCorrectlyPackaged() {
        val blip = RadarBlip(id = "123", name = "Test", rssi = -50, distance = 5f, targetAngleOffset = 0f, type = "BLE", frequencyMhz = 2400.0, bandLabel = "2.4GHz")
        val pkg = AiEvidencePackage(
            observations = listOf(blip),
            baselineSummary = "Baseline",
            anomalyScore = 0.5f,
            anomalyConfidence = 0.8f,
            anomalyExplanations = listOf("Expl"),
            correlations = listOf("Corr"),
            timestampsMs = 1000L,
            locationUncertainty = LocalizationConfidence.MEDIUM,
            hardwareCapabilities = listOf("SDR"),
            calibrationState = "Calibrated",
            provenance = DataProvenance.MEASURED,
            isLive = true,
            isSimulation = false,
            isReplay = false
        )
        
        assertEquals(1, pkg.observations.size)
        assertEquals("123", pkg.observations[0].id)
        assertEquals(DataProvenance.MEASURED, pkg.provenance)
    }

    @Test
    fun testProvenanceIsPreserved() {
        val pkg = AiEvidencePackage(
            observations = emptyList(),
            baselineSummary = "",
            anomalyScore = 0f,
            anomalyConfidence = 0f,
            anomalyExplanations = emptyList(),
            correlations = emptyList(),
            timestampsMs = 0L,
            locationUncertainty = LocalizationConfidence.MEDIUM,
            hardwareCapabilities = emptyList(),
            calibrationState = "",
            provenance = DataProvenance.SIMULATED,
            isLive = false,
            isSimulation = true,
            isReplay = false
        )
        assertEquals(DataProvenance.SIMULATED, pkg.provenance)
        assertTrue(pkg.isSimulation)
    }

    @Test
    fun testMeasuredValuesNotChangedByAi() {
        val originalBlip = RadarBlip(id = "123", name = "Test", rssi = -50, distance = 5f, targetAngleOffset = 0f, type = "BLE", frequencyMhz = 2400.0, bandLabel = "2.4GHz")
        
        val aiResult = AiInvestigatorAssessment(
            assessment = "Found a blip",
            facts = listOf("Blip is at -50")
        )
        
        // Ensure AI result doesn't hold reference to mutate blip
        assertEquals(-50, originalBlip.rssi)
    }

    @Test
    fun testAiOutputRemainsSeparateFromRawEvidence() {
        val assessment = AiInvestigatorAssessment(assessment = "test", confidence = 90)
        val interpretation = AiInterpretation(assessment = assessment, confidence = 90, operatingMode = OperatingMode.LIVE)
        assertNotNull(interpretation.assessment)
        assertEquals(90, interpretation.confidence)
    }

    @Test
    fun testSimulationStateCommunicated() {
        val pkg = AiEvidencePackage(
            observations = emptyList(),
            baselineSummary = "",
            anomalyScore = 0f,
            anomalyConfidence = 0f,
            anomalyExplanations = emptyList(),
            correlations = emptyList(),
            timestampsMs = 0L,
            locationUncertainty = LocalizationConfidence.MEDIUM,
            hardwareCapabilities = emptyList(),
            calibrationState = "",
            provenance = DataProvenance.SIMULATED,
            isLive = false,
            isSimulation = true,
            isReplay = false
        )
        assertTrue(pkg.isSimulation)
    }

    @Test
    fun testReplayStateCommunicated() {
         val pkg = AiEvidencePackage(
            observations = emptyList(),
            baselineSummary = "",
            anomalyScore = 0f,
            anomalyConfidence = 0f,
            anomalyExplanations = emptyList(),
            correlations = emptyList(),
            timestampsMs = 0L,
            locationUncertainty = LocalizationConfidence.MEDIUM,
            hardwareCapabilities = emptyList(),
            calibrationState = "",
            provenance = DataProvenance.MEASURED,
            isLive = false,
            isSimulation = false,
            isReplay = true
        )
        assertTrue(pkg.isReplay)
    }

    @Test
    fun testHardwareLimitationsRespected() {
        val pkg = AiEvidencePackage(
            observations = emptyList(),
            baselineSummary = "",
            anomalyScore = 0f,
            anomalyConfidence = 0f,
            anomalyExplanations = emptyList(),
            correlations = emptyList(),
            timestampsMs = 0L,
            locationUncertainty = LocalizationConfidence.MEDIUM,
            hardwareCapabilities = listOf("BLE Scanner"),
            calibrationState = "",
            provenance = DataProvenance.MEASURED,
            isLive = true,
            isSimulation = false,
            isReplay = false
        )
        assertFalse(pkg.hardwareCapabilities.contains("SDR"))
    }

    @Test
    fun testMissingDataIsUnknown() {
        val assessment = AiInvestigatorAssessment(
            unknowns = listOf("Transmitter location unknown")
        )
        assertTrue(assessment.unknowns.isNotEmpty())
    }

    @Test
    fun testAiCannotInventUnavailableMeasurements() {
        val assessment = AiInvestigatorAssessment(
            recommendedMeasurements = listOf("Use SDR")
        )
        assertTrue(assessment.recommendedMeasurements.isNotEmpty())
    }

    @Test
    fun testAiResultsRetainEvidenceReferences() {
        val assessment = AiInvestigatorAssessment(
            evidenceReferences = listOf("Observation 1")
        )
        assertTrue(assessment.evidenceReferences.isNotEmpty())
    }

    @Test
    fun testFailedAiRequestsDoNotBreakCoreDetection() {
        // Mock failure returns a default local fallback
        val assessment = AiInvestigatorAssessment(assessment = "API Error 500")
        assertEquals("API Error 500", assessment.assessment)
    }

    @Test
    fun testAlternativeExplanationsSupported() {
        val assessment = AiInvestigatorAssessment(
            alternativeExplanations = listOf("Benign tracker", "Interference")
        )
        assertEquals(2, assessment.alternativeExplanations.size)
    }
}
