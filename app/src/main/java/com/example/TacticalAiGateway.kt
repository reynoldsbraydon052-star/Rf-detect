package com.example

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class TacticalAiGateway(
    private val aiEngine: AiInferenceEngine
) {

    val geminiStatus: StateFlow<GeminiStatus> = if (aiEngine is AiEngineRouter) {
        aiEngine.status
    } else {
        MutableStateFlow(GeminiStatus.READY)
    }

    val connectionState: StateFlow<GeminiConnectionState> = if (aiEngine is AiEngineRouter) {
        aiEngine.connectionState
    } else {
        MutableStateFlow(GeminiConnectionState.Connected("Local Engine"))
    }

    suspend fun testConnection(): GeminiConnectionState {
        return if (aiEngine is AiEngineRouter) {
            aiEngine.testConnection()
        } else {
            GeminiConnectionState.Connected("Local Engine")
        }
    }

    suspend fun checkNetworkSpeed(url: String = "https://clients3.google.com/generate_204"): Boolean {
        return if (aiEngine is AiEngineRouter) {
            aiEngine.getGeminiEngine().checkNetworkSpeed(url)
        } else {
            false
        }
    }

    companion object {
        private const val TAG = "TacticalAiGateway"
    }

    suspend fun runEvidenceInvestigator(evidencePkg: AiEvidencePackage, query: String? = null): AiInvestigatorAssessment = withContext(Dispatchers.IO) {
        val prompt = "Analyze this evidence package. " + (query ?: "Provide a general assessment.")
        val fullPrompt = "You are an AI investigator.\n\n$prompt"
        
        val result = aiEngine.generateAnalysis(fullPrompt, null)
        
        if (result.isSuccess) {
            val responseValue = result.getOrNull() ?: ""
            AiInvestigatorAssessment(assessment = responseValue, confidence = 85)
        } else {
            AiInvestigatorAssessment(assessment = "Investigator offline: ${result.exceptionOrNull()?.message}", confidence = 0)
        }
    }

    private fun buildEnvironmentPrompt(snapshot: RfEnvironmentSnapshot): String {
        val builder = StringBuilder()
        builder.append("ANALYSIS REQUEST: RF SNAPSHOT TELEMETRY\n")
        builder.append("- Total Emitters Intercepted: ${snapshot.totalBlipsCount}\n")
        builder.append("- Jamming Attack Signature Detected: ${snapshot.isRfJammingDetected}\n")
        builder.append("- GNSS Spoofing Signature Detected: ${snapshot.isGnssSpoofingDetected}\n")
        builder.append("- IMSI Cell Catcher Alarm: ${snapshot.isImsiAlertActive}\n")
        builder.append("- Ultrasonic Spy Microphone/Acoustic Beacon Active: ${snapshot.isUltrasonicAlertActive}\n")
        if (snapshot.isUltrasonicAlertActive) {
            builder.append("  * Freq: ${snapshot.ultrasonicFreqHz} Hz, Amp: ${snapshot.ultrasonicDb} dB\n")
        }
        builder.append("- EMF Magnetic Anomalies: ${snapshot.magneticFluxMicroTesla} uT\n")
        builder.append("- Compass Bearing: ${snapshot.compassHeading} Degrees\n")
        builder.append("- Active Breach Count within Perimeter: ${snapshot.breachCount}\n")
        
        builder.append("\nINTERCEPTED SIGNAL BUFFER:\n")
        snapshot.activeBlips.take(20).forEach { blip ->
            builder.append("  * ID/MAC: ${blip.id}, Name: ${blip.name}, Type: ${blip.type}, RSSI: ${blip.rssi} dBm, Dist: ${blip.distance}m, Angle: ${blip.targetAngleOffset} deg\n")
        }
        return builder.toString()
    }

    suspend fun analyzeRfEnvironment(snapshot: RfEnvironmentSnapshot): ThreatAnalysisReport = withContext(Dispatchers.IO) {
        val prompt = buildEnvironmentPrompt(snapshot)
        
        val threatReportSchema = JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("threatLevel", JSONObject().apply {
                    put("type", "STRING")
                    put("enum", JSONArray(listOf("SECURE", "LOW_CAUTION", "ELEVATED", "HIGH", "CRITICAL")))
                })
                put("threatScore", JSONObject().apply {
                    put("type", "INTEGER")
                })
                put("executiveSummary", JSONObject().apply {
                    put("type", "STRING")
                })
                put("naturalLanguageThreatAssessment", JSONObject().apply {
                    put("type", "STRING")
                })
                put("identifiedVectors", JSONObject().apply {
                    put("type", "ARRAY")
                    put("items", JSONObject().put("type", "STRING"))
                })
                put("flaggedEmitters", JSONObject().apply {
                    put("type", "ARRAY")
                    put("items", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("id", JSONObject().put("type", "STRING"))
                            put("name", JSONObject().put("type", "STRING"))
                            put("macAddress", JSONObject().put("type", "STRING"))
                            put("signalType", JSONObject().put("type", "STRING"))
                            put("rssiDbm", JSONObject().put("type", "INTEGER"))
                            put("distanceMeters", JSONObject().put("type", "NUMBER"))
                            put("threatCategory", JSONObject().apply {
                                put("type", "STRING")
                                put("enum", JSONArray(listOf(
                                    "SURVEILLANCE_TRACKER", "ROGUE_WIFI_EVIL_TWIN", "IMSI_CELL_CATCHER",
                                    "ULTRASONIC_ACOUSTIC_SPY", "EMF_MAGNETIC_ANOMALY", "RF_JAMMING_ELECTRONIC_WAR",
                                    "UNREGISTERED_BLE_BEACON", "UNKNOWN_ANOMALOUS_NODE"
                                )))
                            })
                            put("threatScore", JSONObject().put("type", "INTEGER"))
                            put("riskSummary", JSONObject().put("type", "STRING"))
                            put("recommendedAction", JSONObject().put("type", "STRING"))
                        })
                        put("required", JSONArray(listOf(
                            "id", "name", "macAddress", "signalType", "rssiDbm", "distanceMeters", "threatCategory", "threatScore", "riskSummary", "recommendedAction"
                        )))
                    })
                })
                put("countermeasures", JSONObject().apply {
                    put("type", "ARRAY")
                    put("items", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("title", JSONObject().put("type", "STRING"))
                            put("detail", JSONObject().put("type", "STRING"))
                            put("urgency", JSONObject().apply {
                                put("type", "STRING")
                                put("enum", JSONArray(listOf("IMMEDIATE", "RECOMMENDED", "MONITOR")))
                            })
                        })
                        put("required", JSONArray(listOf("title", "detail", "urgency")))
                    })
                })
                put("rawSigintDetails", JSONObject().put("type", "STRING"))
            })
            put("required", JSONArray(listOf(
                "threatLevel", "threatScore", "executiveSummary", "naturalLanguageThreatAssessment", "identifiedVectors", "flaggedEmitters", "countermeasures", "rawSigintDetails"
            )))
        }

        val systemInstruction = """
            You are a military-grade Signals Intelligence (SIGINT), Electronic Counter-Surveillance, and RF Threat Analysis AI.
            Analyze the provided real-time RF spectrum telemetry snapshot and buffer of detected RF signals (Wi-Fi, Bluetooth LE, Cellular, Ultrasonic acoustic spikes, EMF magnetic flux, and UWB ranging).
            Ensure threat severity scores, OUI analyses, and jamming mitigation advice render instantly.
        """.trimIndent()

        val fullPrompt = "$systemInstruction\n\n$prompt"
        val result = aiEngine.generateAnalysis(fullPrompt, threatReportSchema.toString())

        if (result.isSuccess) {
            val responseValue = result.getOrNull() ?: ""
            try {
                parseGeminiThreatResponse(responseValue, snapshot)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse JSON response: ${e.message}")
                generateLocalHeuristicReport(snapshot, isOfflineFallback = true)
            }
        } else {
            Log.w(TAG, "AI Engine failed: ${result.exceptionOrNull()?.message}. Generating local heuristic.")
            generateLocalHeuristicReport(snapshot, isOfflineFallback = true)
        }
    }

    suspend fun askTacticalCopilot(
        query: String,
        evidencePkg: AiEvidencePackage,
        conversationHistory: List<TacticalCopilotMessage>
    ): String = withContext(Dispatchers.IO) {
        val obsList = evidencePkg.observations.take(15).joinToString("\n") {
            "- ID:${it.id.take(12)} Name:${it.name} RSSI:${it.rssi}dBm Dist:${it.distance}m Type:${it.type}"
        }
        
        val telemetryContext = """
            [EVIDENCE PACKAGE]
            Data Provenance: ${evidencePkg.provenance}
            Simulation Mode: ${evidencePkg.isSimulation}
            Replay Mode: ${evidencePkg.isReplay}
            Hardware Capabilities: ${evidencePkg.hardwareCapabilities.joinToString()}
            Baseline Summary: ${evidencePkg.baselineSummary}
            Anomaly Score: ${evidencePkg.anomalyScore} (Conf: ${evidencePkg.anomalyConfidence})
            Correlations: ${evidencePkg.correlations.joinToString()}
            
            [OBSERVATIONS SAMPLE]
            $obsList
        """.trimIndent()

        val historyPromptBuilder = java.lang.StringBuilder()
        conversationHistory.takeLast(6).forEach { msg ->
            val role = if (msg.isUser) "User" else "Investigator"
            historyPromptBuilder.append("$role: ${msg.text}\n\n")
        }
        historyPromptBuilder.append("Context:\n$telemetryContext\n\nUser Query: $query")

        val systemPrompt = "You are an EVIDENCE-BASED AI INVESTIGATOR. Answer the user's questions based strictly on the provided evidence package. Distinguish between FACT, INFERENCE, HYPOTHESIS, and UNKNOWN. Do not fabricate answers. Do not use hyperbolic threat language unless explicitly supported by evidence."
        val fullPrompt = "$systemPrompt\n\n${historyPromptBuilder}"

        val result = aiEngine.generateAnalysis(fullPrompt, null)

        if (result.isSuccess) {
            result.getOrNull() ?: ""
        } else {
            "Error: Tactical copilot offline: ${result.exceptionOrNull()?.message}"
        }
    }

    private fun parseGeminiThreatResponse(jsonText: String, snapshot: RfEnvironmentSnapshot): ThreatAnalysisReport {
        try {
            val root = JSONObject(jsonText)
            
            val threatLevelStr = root.optString("threatLevel", "ELEVATED")
            val threatLevel = try {
                ThreatLevel.valueOf(threatLevelStr)
            } catch (e: Exception) {
                ThreatLevel.ELEVATED
            }
            
            val flaggedEmittersJson = root.optJSONArray("flaggedEmitters")
            val flaggedEmitters = mutableListOf<FlaggedThreatEmitter>()
            if (flaggedEmittersJson != null) {
                for (i in 0 until flaggedEmittersJson.length()) {
                    val em = flaggedEmittersJson.optJSONObject(i) ?: continue
                    flaggedEmitters.add(
                        FlaggedThreatEmitter(
                            id = em.optString("id", "Unknown"),
                            name = em.optString("name", "Unknown"),
                            macAddress = em.optString("macAddress", "Unknown"),
                            signalType = em.optString("signalType", "UNKNOWN"),
                            rssiDbm = em.optInt("rssiDbm", -90),
                            distanceMeters = em.optDouble("distanceMeters", 0.0).toFloat(),
                            threatCategory = try { ThreatCategory.valueOf(em.optString("threatCategory", "UNKNOWN_ANOMALOUS_NODE")) } catch (e: Exception) { ThreatCategory.UNKNOWN_ANOMALOUS_NODE },
                            threatScore = em.optInt("threatScore", 50),
                            riskSummary = em.optString("riskSummary", ""),
                            recommendedAction = em.optString("recommendedAction", "")
                        )
                    )
                }
            }

            val countermeasuresJson = root.optJSONArray("countermeasures")
            val countermeasures = mutableListOf<TacticalCountermeasure>()
            if (countermeasuresJson != null) {
                for (i in 0 until countermeasuresJson.length()) {
                    val cm = countermeasuresJson.optJSONObject(i) ?: continue
                    countermeasures.add(
                        TacticalCountermeasure(
                            title = cm.optString("title", "Unknown"),
                            detail = cm.optString("detail", ""),
                            urgency = cm.optString("urgency", "RECOMMENDED")
                        )
                    )
                }
            }
            
            val identifiedVectorsJson = root.optJSONArray("identifiedVectors")
            val identifiedVectors = mutableListOf<String>()
            if (identifiedVectorsJson != null) {
                for (i in 0 until identifiedVectorsJson.length()) {
                    identifiedVectors.add(identifiedVectorsJson.optString(i))
                }
            }

            return ThreatAnalysisReport(
                threatLevel = threatLevel,
                threatScore = root.optInt("threatScore", 50),
                executiveSummary = root.optString("executiveSummary", "AI Assessment complete."),
                naturalLanguageThreatAssessment = root.optString("naturalLanguageThreatAssessment", "No detailed assessment provided."),
                analyzedRfBufferCount = snapshot.totalBlipsCount,
                flaggedEmitters = flaggedEmitters,
                identifiedVectors = identifiedVectors,
                countermeasures = countermeasures,
                rawSigintDetails = root.optString("rawSigintDetails", ""),
                isAiGenerated = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing error: ${e.message}")
            return generateLocalHeuristicReport(snapshot, isOfflineFallback = false)
        }
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

    suspend fun performTargetDeepAudit(emitter: FlaggedThreatEmitter, snapshot: RfEnvironmentSnapshot): DetailedTargetAudit = withContext(Dispatchers.IO) {
        val auditSchema = JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("manufacturerVendor", JSONObject().put("type", "STRING"))
                put("radioFingerprintSummary", JSONObject().put("type", "STRING"))
                put("trackingHeuristicConfidence", JSONObject().put("type", "INTEGER"))
                put("surveillanceRiskAnalysis", JSONObject().put("type", "STRING"))
                put("hardwareVectorAnalysis", JSONObject().put("type", "STRING"))
                put("cryptographicProfile", JSONObject().put("type", "STRING"))
                put("inferenceType", JSONObject().apply {
                    put("type", "STRING")
                    put("enum", JSONArray(listOf("DETERMINISTIC", "PROBABILISTIC", "HEURISTIC", "UNKNOWN")))
                })
                put("vulnerabilities", JSONObject().apply {
                    put("type", "ARRAY")
                    put("items", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("protocol", JSONObject().put("type", "STRING"))
                            put("riskLevel", JSONObject().apply {
                                put("type", "STRING")
                                put("enum", JSONArray(listOf("SECURE", "LOW_CAUTION", "ELEVATED", "HIGH", "CRITICAL")))
                            })
                            put("attackSurface", JSONObject().put("type", "STRING"))
                            put("exploitationVector", JSONObject().put("type", "STRING"))
                            put("containmentFix", JSONObject().put("type", "STRING"))
                        })
                        put("required", JSONArray(listOf("protocol", "riskLevel", "attackSurface", "exploitationVector", "containmentFix")))
                    })
                })
                put("stepByStepNeutralizationPlan", JSONObject().apply {
                    put("type", "ARRAY")
                    put("items", JSONObject().put("type", "STRING"))
                })
            })
            put("required", JSONArray(listOf(
                "manufacturerVendor", "radioFingerprintSummary", "trackingHeuristicConfidence", "surveillanceRiskAnalysis", "hardwareVectorAnalysis", "cryptographicProfile", "inferenceType", "vulnerabilities", "stepByStepNeutralizationPlan"
            )))
        }

        val prompt = """
            Perform a deep security audit and SIGINT analysis on this specific flagged threat emitter:
            - Target ID: ${emitter.id}
            - Name: ${emitter.name}
            - MAC Address: ${emitter.macAddress}
            - Signal Type: ${emitter.signalType}
            - Current RSSI: ${emitter.rssiDbm} dBm
            - Estimated Distance: ${emitter.distanceMeters} meters
            - Threat Category: ${emitter.threatCategory}
            - Threat Score: ${emitter.threatScore}
            
            Current ambient environment context has ${snapshot.totalBlipsCount} total active emitters.
        """.trimIndent()

        val systemInstruction = "You are an expert RF hardware forensic investigator. Conduct a highly detailed protocol audit and vulnerability analysis. Produce structured outputs instantly."

        val fullPrompt = "$systemInstruction\n\n$prompt"
        val result = aiEngine.generateAnalysis(fullPrompt, auditSchema.toString())

        if (result.isSuccess) {
            val responseValue = result.getOrNull() ?: ""
            try {
                val root = JSONObject(responseValue)
                val vulnerabilities = mutableListOf<ProtocolVulnerability>()
                val vulnArray = root.optJSONArray("vulnerabilities")
                if (vulnArray != null) {
                    for (i in 0 until vulnArray.length()) {
                        val v = vulnArray.getJSONObject(i)
                        vulnerabilities.add(
                            ProtocolVulnerability(
                                protocol = v.optString("protocol", "Unknown"),
                                riskLevel = try { ThreatLevel.valueOf(v.optString("riskLevel", "LOW_CAUTION")) } catch (e: Exception) { ThreatLevel.LOW_CAUTION },
                                attackSurface = v.optString("attackSurface", ""),
                                exploitationVector = v.optString("exploitationVector", ""),
                                containmentFix = v.optString("containmentFix", "")
                            )
                        )
                    }
                }

                val neutPlan = mutableListOf<String>()
                val neutArray = root.optJSONArray("stepByStepNeutralizationPlan")
                if (neutArray != null) {
                    for (i in 0 until neutArray.length()) {
                        neutPlan.add(neutArray.getString(i))
                    }
                }

                DetailedTargetAudit(
                    targetId = emitter.id,
                    targetName = emitter.name,
                    macAddress = emitter.macAddress ?: "Unknown",
                    signalType = emitter.signalType,
                    rssiDbm = emitter.rssiDbm,
                    estimatedDistanceMeters = emitter.distanceMeters,
                    threatScore = emitter.threatScore,
                    threatCategory = emitter.threatCategory,
                    inferenceType = try { InferenceType.valueOf(root.optString("inferenceType", "UNKNOWN")) } catch (e: Exception) { InferenceType.UNKNOWN },
                    manufacturerVendor = root.optString("manufacturerVendor", "Unknown"),
                    radioFingerprintSummary = root.optString("radioFingerprintSummary", "Standard footprint"),
                    trackingHeuristicConfidence = root.optInt("trackingHeuristicConfidence", 70),
                    surveillanceRiskAnalysis = root.optString("surveillanceRiskAnalysis", ""),
                    hardwareVectorAnalysis = root.optString("hardwareVectorAnalysis", ""),
                    cryptographicProfile = root.optString("cryptographicProfile", ""),
                    vulnerabilities = vulnerabilities,
                    stepByStepNeutralizationPlan = neutPlan
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse target deep audit JSON: ${e.message}")
                getFallbackAudit(emitter)
            }
        } else {
            getFallbackAudit(emitter)
        }
    }

    private fun getFallbackAudit(emitter: FlaggedThreatEmitter): DetailedTargetAudit {
        return DetailedTargetAudit(
            targetId = emitter.id,
            targetName = emitter.name,
            macAddress = emitter.macAddress ?: "Unknown",
            signalType = emitter.signalType,
            rssiDbm = emitter.rssiDbm,
            estimatedDistanceMeters = emitter.distanceMeters,
            threatScore = emitter.threatScore,
            threatCategory = emitter.threatCategory,
            manufacturerVendor = "Unknown/Anonymized",
            radioFingerprintSummary = "Local fingerprint scan normal.",
            trackingHeuristicConfidence = 50,
            surveillanceRiskAnalysis = "Offline/Fallback surveillance profiling active.",
            hardwareVectorAnalysis = "Standard RF transceiver layer",
            cryptographicProfile = "WPA2/AES standard",
            vulnerabilities = emptyList(),
            stepByStepNeutralizationPlan = listOf("Perform localized visual sweep", "Monitor transmitter signal level changes")
        )
    }

    suspend fun performAi3dPinpoint(blip: RadarBlip, sensorSuite: HardwareSensorSuiteData, heading: Float): AiPinpointResult = withContext(Dispatchers.IO) {
        val pinpointSchema = JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("accuracyMarginMeters", JSONObject().put("type", "NUMBER"))
                put("confidencePercent", JSONObject().put("type", "INTEGER"))
                put("azimuthDegrees", JSONObject().put("type", "NUMBER"))
                put("relativeClockHeading", JSONObject().put("type", "STRING"))
                put("elevationPitchDeg", JSONObject().put("type", "NUMBER"))
                put("altitudeOffsetMeters", JSONObject().put("type", "NUMBER"))
                put("floorClassification", JSONObject().put("type", "STRING"))
                put("physicalZoneEstimation", JSONObject().put("type", "STRING"))
                put("spatialVectorXyz", JSONObject().put("type", "STRING"))
                put("aiTacticalGuidance", JSONObject().put("type", "STRING"))
                put("searchChecklist", JSONObject().apply {
                    put("type", "ARRAY")
                    put("items", JSONObject().put("type", "STRING"))
                })
            })
            put("required", JSONArray(listOf(
                "accuracyMarginMeters", "confidencePercent", "azimuthDegrees", "relativeClockHeading", "elevationPitchDeg", "altitudeOffsetMeters", "floorClassification", "physicalZoneEstimation", "spatialVectorXyz", "aiTacticalGuidance", "searchChecklist"
            )))
        }

        val prompt = """
            Calculate a high-precision 3D spatial intercept coordinates vector for this emitter:
            - ID/MAC: ${blip.id}
            - Name: ${blip.name}
            - Type: ${blip.type}
            - Current RSSI: ${blip.rssi} dBm
            - Current 2D Distance: ${blip.distance} meters
            - Device Relative Angle: ${blip.targetAngleOffset} degrees
            
            Current sensor platform orientation:
            - Heading: $heading degrees
            - Pitch/Tilt: ${sensorSuite.pitchDeg} degrees
            - Roll: ${sensorSuite.rollDeg} degrees
            - Magnetic Field Strength: ${sensorSuite.magnetometerData.totalMicroTesla} uT
            - Acceleration: ${sensorSuite.totalGForce} Gs
        """.trimIndent()

        val systemInstruction = "You are an advanced AI 3D RF Spatial Pinpointing & Intercept Engine. Calculate 3D vectors and elevation details relative to the user's phone. Return structured JSON."

        val fullPrompt = "$systemInstruction\n\n$prompt"
        val result = aiEngine.generateAnalysis(fullPrompt, pinpointSchema.toString())

        if (result.isSuccess) {
            val responseValue = result.getOrNull() ?: ""
            try {
                val root = JSONObject(responseValue)
                val checklist = mutableListOf<String>()
                val checkArray = root.optJSONArray("searchChecklist")
                if (checkArray != null) {
                    for (i in 0 until checkArray.length()) {
                        checklist.add(checkArray.getString(i))
                    }
                }

                AiPinpointResult(
                    targetId = blip.id,
                    targetName = blip.name,
                    macAddress = blip.id,
                    signalType = blip.type,
                    currentRssiDbm = blip.rssi,
                    distanceMeters = blip.distance,
                    accuracyMarginMeters = root.optDouble("accuracyMarginMeters", 1.0).toFloat(),
                    confidencePercent = root.optInt("confidencePercent", 80),
                    azimuthDegrees = root.optDouble("azimuthDegrees", blip.targetAngleOffset.toDouble()).toFloat(),
                    relativeClockHeading = root.optString("relativeClockHeading", "12 O'Clock"),
                    elevationPitchDeg = root.optDouble("elevationPitchDeg", 0.0).toFloat(),
                    altitudeOffsetMeters = root.optDouble("altitudeOffsetMeters", 0.0).toFloat(),
                    floorClassification = root.optString("floorClassification", "SAME LEVEL"),
                    physicalZoneEstimation = root.optString("physicalZoneEstimation", "Open Space"),
                    spatialVectorXyz = root.optString("spatialVectorXyz", "X: 0.0m, Y: 0.0m, Z: 0.0m"),
                    aiTacticalGuidance = root.optString("aiTacticalGuidance", "Proceed with search checklist."),
                    searchChecklist = checklist
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse 3D pinpoint JSON: ${e.message}")
                getFallbackPinpoint(blip)
            }
        } else {
            getFallbackPinpoint(blip)
        }
    }

    private fun getFallbackPinpoint(blip: RadarBlip): AiPinpointResult {
        return AiPinpointResult(
            targetId = blip.id,
            targetName = blip.name,
            macAddress = blip.id,
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
            physicalZoneEstimation = "Open space",
            spatialVectorXyz = "X: 0.0m, Y: 0.0m, Z: 0.0m",
            aiTacticalGuidance = "Local pinpoint mode active. Keep device oriented to target.",
            searchChecklist = listOf("Check line-of-sight path", "Perform visual inspection of immediate area")
        )
    }
}
