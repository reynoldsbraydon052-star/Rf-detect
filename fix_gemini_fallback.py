with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'r') as f:
    content = f.read()

import re

fallback_audit = """        DetailedTargetAudit(
            targetId = emitter.id,
            targetName = emitter.name,
            macAddress = emitter.macAddress ?: "Unknown",
            signalType = emitter.signalType,
            rssiDbm = emitter.rssiDbm,
            estimatedDistanceMeters = emitter.distanceMeters,
            threatScore = emitter.threatScore,
            threatCategory = emitter.threatCategory,
            manufacturerVendor = "Unknown/Anonymized",
            radioFingerprintSummary = "No anomalous RF fingerprints detected.",
            trackingHeuristicConfidence = 50,
            surveillanceRiskAnalysis = "Standard device footprint.",
            hardwareVectorAnalysis = "Standard RF Transmission",
            cryptographicProfile = "WPA2/AES standard",
            vulnerabilities = emptyList(),
            stepByStepNeutralizationPlan = listOf("Monitor closely")
        )"""

# replace DetailedTargetAudit( ... ) in performTargetDeepAudit
# Using regex to find the block
content = re.sub(r'DetailedTargetAudit\(\s*targetId = emitter\.id,[\s\S]*?confidenceScore = 75\.0f\s*\)', fallback_audit, content)

fallback_pinpoint = """        AiPinpointResult(
            targetId = blip.id,
            targetName = blip.name,
            macAddress = blip.macAddress ?: "Unknown",
            signalType = blip.type,
            currentRssiDbm = blip.rssi,
            distanceMeters = blip.distance,
            accuracyMarginMeters = 1.5f,
            confidencePercent = 85,
            azimuthDegrees = blip.targetAngleOffset,
            relativeClockHeading = "12 O'Clock",
            elevationPitchDeg = 0.0f,
            altitudeOffsetMeters = 0.0f,
            floorClassification = "SAME LEVEL",
            physicalZoneEstimation = "Open area",
            spatialVectorXyz = "X: 0.0m, Y: 0.0m, Z: 0.0m",
            aiTacticalGuidance = "Approach cautiously.",
            searchChecklist = listOf("Visual inspection")
        )"""

content = re.sub(r'AiPinpointResult\(\s*targetId = blip\.id,[\s\S]*?isMoving = false\s*\)', fallback_pinpoint, content)

with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'w') as f:
    f.write(content)
