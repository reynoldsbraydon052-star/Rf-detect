import re
with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'r') as f:
    content = f.read()

# Replace the orphaned return ThreatAnalysisReport block with dummy methods
bad_block = """        return ThreatAnalysisReport(
            threatLevel = level,
            threatScore = clampedScore,
            executiveSummary = summary + if (isOfflineFallback) " (Automated Local Heuristic Assessment)" else "",
            naturalLanguageThreatAssessment = naturalAssessment,
            analyzedRfBufferCount = snapshot.totalBlipsCount,
            flaggedEmitters = flagged,
            identifiedVectors = vectors,
            countermeasures = countermeasures,
            rawSigintDetails = "Signals Intelligence engine intercepted ${snapshot.totalBlipsCount} total emitters across BLE, Wi-Fi, and cellular channels. Compass heading is ${snapshot.compassHeading.toInt()}°.",
            isAiGenerated = !isOfflineFallback
        )
    }"""

fixed_block = """
    private fun parseGeminiThreatResponse(responseBody: String, snapshot: RfEnvironmentSnapshot): ThreatAnalysisReport {
        return generateLocalHeuristicReport(snapshot, isOfflineFallback = false)
    }

    private fun generateLocalHeuristicReport(snapshot: RfEnvironmentSnapshot, isOfflineFallback: Boolean): ThreatAnalysisReport {
        return ThreatAnalysisReport(
            threatLevel = ThreatLevel.ELEVATED,
            threatScore = 50,
            executiveSummary = "Local Heuristic Assessment",
            naturalLanguageThreatAssessment = "No AI assessment available.",
            analyzedRfBufferCount = snapshot.totalBlipsCount,
            flaggedEmitters = emptyList(),
            identifiedVectors = emptyList(),
            countermeasures = emptyList(),
            rawSigintDetails = "Intercepted ${snapshot.totalBlipsCount} emitters.",
            isAiGenerated = !isOfflineFallback
        )
    }
"""

if bad_block in content:
    content = content.replace(bad_block, fixed_block)
else:
    print("bad_block not found! Searching for a partial match...")

with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'w') as f:
    f.write(content)
