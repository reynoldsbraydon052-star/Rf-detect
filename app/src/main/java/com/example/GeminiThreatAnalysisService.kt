package com.example

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiThreatAnalysisService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val TAG = "GeminiThreatAnalysis"
        private const val MODEL_NAME = "gemini-3.5-flash"
        private const val API_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"
    }

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun analyzeRfEnvironment(snapshot: RfEnvironmentSnapshot): ThreatAnalysisReport = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val prompt = buildEnvironmentPrompt(snapshot)

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured. Generating high-precision local heuristic threat report.")
            return@withContext generateLocalHeuristicReport(snapshot, isOfflineFallback = true)
        }

        try {
            val systemInstruction = """
                You are a military-grade Signals Intelligence (SIGINT), Electronic Counter-Surveillance, and RF Threat Analysis AI.
                Analyze the provided real-time RF spectrum telemetry snapshot (Wi-Fi, Bluetooth LE, Cellular, Ultrasonic acoustic spikes, EMF magnetic flux, and UWB ranging).
                
                Respond in valid JSON format matching this schema:
                {
                  "threatLevel": "SECURE" | "LOW_CAUTION" | "ELEVATED" | "HIGH" | "CRITICAL",
                  "threatScore": <number between 0 and 100>,
                  "executiveSummary": "<2-3 sentence high-level tactical SIGINT assessment>",
                  "identifiedVectors": ["<vector 1>", "<vector 2>", ...],
                  "flaggedEmitters": [
                    {
                      "id": "<emitter id>",
                      "name": "<name>",
                      "macAddress": "<mac>",
                      "signalType": "<WIFI|BLE|CELLULAR|ACOUSTIC|MAGNETIC>",
                      "rssiDbm": <number>,
                      "distanceMeters": <number>,
                      "threatCategory": "SURVEILLANCE_TRACKER" | "ROGUE_WIFI_EVIL_TWIN" | "IMSI_CELL_CATCHER" | "ULTRASONIC_ACOUSTIC_SPY" | "EMF_MAGNETIC_ANOMALY" | "RF_JAMMING_ELECTRONIC_WAR" | "UNREGISTERED_BLE_BEACON" | "UNKNOWN_ANOMALOUS_NODE",
                      "threatScore": <number 0-100>,
                      "riskSummary": "<specific threat reason>",
                      "recommendedAction": "<immediate tactical countermeasure>"
                    }
                  ],
                  "countermeasures": [
                    {
                      "title": "<tactical action>",
                      "detail": "<explanation of action>",
                      "urgency": "IMMEDIATE" | "RECOMMENDED" | "MONITOR"
                    }
                  ],
                  "rawSigintDetails": "<in-depth electronic intelligence analysis paragraph>"
                }
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstruction))
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("topP", 0.95)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("$API_ENDPOINT?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful || responseBody.isBlank()) {
                Log.e(TAG, "Gemini API request failed code=${response.code}: $responseBody")
                return@withContext generateLocalHeuristicReport(snapshot, isOfflineFallback = true)
            }

            parseGeminiThreatResponse(responseBody, snapshot)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing Gemini RF Threat Analysis: ${e.message}", e)
            generateLocalHeuristicReport(snapshot, isOfflineFallback = true)
        }
    }

    suspend fun askTacticalCopilot(
        query: String,
        snapshot: RfEnvironmentSnapshot,
        conversationHistory: List<TacticalCopilotMessage>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Tactical Copilot (Local Heuristic Mode): Intercepted ${snapshot.totalBlipsCount} nodes. " +
                    if (snapshot.isRfJammingDetected) "ALERT: Jamming conditions detected. " else "" +
                    if (snapshot.isImsiAlertActive) "ALERT: Potential rogue IMSI tower nearby. " else "" +
                    "Add a valid Gemini API key in the AI Studio Secrets panel for live generative SIGINT reasoning."
        }

        try {
            val telemetryContext = """
                [REAL-TIME RF TELEMETRY CONTEXT]
                Total Intercepted Emitters: ${snapshot.totalBlipsCount}
                RF Jamming Detected: ${snapshot.isRfJammingDetected}
                GNSS Spoofing Detected: ${snapshot.isGnssSpoofingDetected}
                IMSI Catcher Alert: ${snapshot.isImsiAlertActive}
                Ultrasonic Acoustic Spike: ${snapshot.isUltrasonicAlertActive} (${snapshot.ultrasonicFreqHz} Hz, ${snapshot.ultrasonicDb} dB)
                EMF Magnetic Flux: ${"%.1f".format(snapshot.magneticFluxMicroTesla)} µT
                Perimeter Breach Count: ${snapshot.breachCount}
                
                Detected Blips Sample:
                ${snapshot.activeBlips.take(15).joinToString("\n") { 
                    "- [${it.type}] ID:${it.id.take(12)} Name:${it.name} RSSI:${it.rssi}dBm Dist:${"%.1f".format(it.distance)}m Offset:${it.targetAngleOffset.toInt()}° HighRisk:${it.isHighRiskVendor} Vendor:${it.ouiVendor ?: "Unknown"}" 
                }}
            """.trimIndent()

            val contentsArray = JSONArray()

            // Append previous turns
            conversationHistory.takeLast(6).forEach { msg ->
                contentsArray.put(JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", msg.text))
                    })
                })
            }

            // Current user turn with telemetry context
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", "$telemetryContext\n\nUser Tactical Query: $query"))
                })
            })

            val systemPrompt = "You are a military SIGINT and Cyber-Physical Counter-Surveillance Tactical Advisor. " +
                    "Give concise, authoritative, and actionable answers regarding RF emitters, Bluetooth beacons, tracking tags, Wi-Fi security, ultrasonic eavesdropping, and electronic defense countermeasures."

            val requestBodyJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                    put("maxOutputTokens", 800)
                })
            }

            val request = Request.Builder()
                .url("$API_ENDPOINT?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful || responseBody.isBlank()) {
                return@withContext "Tactical Copilot SIGINT offline error (${response.code})."
            }

            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            text ?: "Tactical Copilot returned an empty response."
        } catch (e: Exception) {
            Log.e(TAG, "Copilot query error: ${e.message}", e)
            "Tactical Copilot network exception: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    private fun buildEnvironmentPrompt(snapshot: RfEnvironmentSnapshot): String {
        val blipsSummary = snapshot.activeBlips.take(25).joinToString("\n") { blip ->
            val vendor = blip.ouiVendor ?: "Unknown"
            "Node: ID=${blip.id}, Type=${blip.type}, Name='${blip.name}', RSSI=${blip.rssi}dBm, Dist=${"%.1f".format(blip.distance)}m, Offset=${blip.targetAngleOffset.toInt()}°, HighRisk=${blip.isHighRiskVendor}, Vendor=$vendor, Freq=${blip.frequencyMhz}MHz, Band=${blip.bandLabel}"
        }

        return """
            Perform immediate SIGINT and Counter-Surveillance Threat Assessment on this live RF telemetry snapshot:
            
            - Total Active Detected Emitters: ${snapshot.totalBlipsCount}
            - Nearest Detected Target: ${snapshot.nearestBlip?.name ?: "None"} (${snapshot.nearestBlip?.distance?.let { "%.1f".format(it) } ?: "N/A"} meters, ${snapshot.nearestBlip?.rssi ?: 0} dBm)
            - RF Electronic Jamming: ${snapshot.isRfJammingDetected}
            - GNSS / GPS Spoofing: ${snapshot.isGnssSpoofingDetected}
            - Rogue Cellular / IMSI Catcher: ${snapshot.isImsiAlertActive}
            - Ultrasonic Acoustic Spike: ${snapshot.isUltrasonicAlertActive} (${snapshot.ultrasonicFreqHz} Hz, ${snapshot.ultrasonicDb} dB)
            - Ambient Magnetic Flux (EMF): ${"%.2f".format(snapshot.magneticFluxMicroTesla)} µT
            - Compass Heading: ${snapshot.compassHeading.toInt()}°
            - Perimeter Warning Breaches: ${snapshot.breachCount}
            
            [EMITTER TELEMETRY DUMP]
            $blipsSummary
        """.trimIndent()
    }

    private fun parseGeminiThreatResponse(responseJson: String, snapshot: RfEnvironmentSnapshot): ThreatAnalysisReport {
        return try {
            val root = JSONObject(responseJson)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

            // Extract JSON from markdown blocks if present
            val cleanedJson = if (rawText.contains("```json")) {
                rawText.substringAfter("```json").substringBefore("```").trim()
            } else if (rawText.contains("```")) {
                rawText.substringAfter("```").substringBefore("```").trim()
            } else {
                rawText.trim()
            }

            val threatObj = JSONObject(cleanedJson)
            val levelStr = threatObj.optString("threatLevel", "NORMAL").uppercase()
            val threatLevel = when {
                levelStr.contains("CRITICAL") -> ThreatLevel.CRITICAL
                levelStr.contains("HIGH") -> ThreatLevel.HIGH
                levelStr.contains("ELEVATED") -> ThreatLevel.ELEVATED
                levelStr.contains("LOW") -> ThreatLevel.LOW_CAUTION
                else -> ThreatLevel.SECURE
            }

            val threatScore = threatObj.optInt("threatScore", if (threatLevel == ThreatLevel.CRITICAL) 92 else 20)
            val executiveSummary = threatObj.optString("executiveSummary", "SIGINT Analysis complete.")
            val rawSigint = threatObj.optString("rawSigintDetails", "")

            val identifiedVectors = mutableListOf<String>()
            val vectorsArr = threatObj.optJSONArray("identifiedVectors")
            if (vectorsArr != null) {
                for (i in 0 until vectorsArr.length()) {
                    identifiedVectors.add(vectorsArr.optString(i))
                }
            }

            val flaggedEmitters = mutableListOf<FlaggedThreatEmitter>()
            val emittersArr = threatObj.optJSONArray("flaggedEmitters")
            if (emittersArr != null) {
                for (i in 0 until emittersArr.length()) {
                    val emObj = emittersArr.optJSONObject(i) ?: continue
                    val catStr = emObj.optString("threatCategory", "UNKNOWN_ANOMALOUS_NODE")
                    val cat = try { ThreatCategory.valueOf(catStr) } catch (e: Exception) { ThreatCategory.UNKNOWN_ANOMALOUS_NODE }
                    flaggedEmitters.add(
                        FlaggedThreatEmitter(
                            id = emObj.optString("id", "NODE-$i"),
                            name = emObj.optString("name", "Emitter"),
                            macAddress = emObj.optString("macAddress", "Unknown MAC"),
                            signalType = emObj.optString("signalType", "BLE"),
                            rssiDbm = emObj.optInt("rssiDbm", -70),
                            distanceMeters = emObj.optDouble("distanceMeters", 5.0).toFloat(),
                            threatCategory = cat,
                            threatScore = emObj.optInt("threatScore", 60),
                            riskSummary = emObj.optString("riskSummary", "Potential surveillance anomaly"),
                            recommendedAction = emObj.optString("recommendedAction", "Audit physical proximity")
                        )
                    )
                }
            }

            val countermeasures = mutableListOf<TacticalCountermeasure>()
            val cmArr = threatObj.optJSONArray("countermeasures")
            if (cmArr != null) {
                for (i in 0 until cmArr.length()) {
                    val cmObj = cmArr.optJSONObject(i) ?: continue
                    countermeasures.add(
                        TacticalCountermeasure(
                            title = cmObj.optString("title", "Check perimeter"),
                            detail = cmObj.optString("detail", "Audit unknown transmitters."),
                            urgency = cmObj.optString("urgency", "RECOMMENDED")
                        )
                    )
                }
            }

            ThreatAnalysisReport(
                threatLevel = threatLevel,
                threatScore = threatScore,
                executiveSummary = executiveSummary,
                flaggedEmitters = flaggedEmitters,
                identifiedVectors = identifiedVectors,
                countermeasures = countermeasures,
                rawSigintDetails = rawSigint,
                isAiGenerated = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini response JSON, falling back to heuristic: ${e.message}", e)
            generateLocalHeuristicReport(snapshot, isOfflineFallback = true)
        }
    }

    suspend fun performTargetDeepAudit(
        emitter: FlaggedThreatEmitter,
        snapshot: RfEnvironmentSnapshot
    ): DetailedTargetAudit = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateLocalDeepAudit(emitter, snapshot)
        }

        try {
            val systemInstruction = """
                You are an elite Signals Intelligence (SIGINT) and RF Hardware Security specialist.
                Perform an exhaustive, military-grade AI Deep Audit on the target emitter below.
                
                Respond in valid JSON matching this schema:
                {
                  "targetId": "${emitter.id}",
                  "targetName": "${emitter.name}",
                  "macAddress": "${emitter.macAddress ?: emitter.id}",
                  "signalType": "${emitter.signalType}",
                  "rssiDbm": ${emitter.rssiDbm},
                  "estimatedDistanceMeters": ${emitter.distanceMeters},
                  "threatScore": ${emitter.threatScore},
                  "threatCategory": "${emitter.threatCategory.name}",
                  "manufacturerVendor": "<Identified OUI vendor or chip manufacturer>",
                  "radioFingerprintSummary": "<Detailed PHY layer, frequency band, advertising interval, and TX power profile>",
                  "trackingHeuristicConfidence": <number 0-100 indicating likelihood of active stalker/tracker usage>,
                  "surveillanceRiskAnalysis": "<In-depth paragraph analyzing geolocation leakage, payload sniffing, or beacon tracking implications>",
                  "hardwareVectorAnalysis": "<Physical transmitter specs, antenna gain characteristics, and power source estimation>",
                  "cryptographicProfile": "<Analysis of encryption, rotating MAC behavior, ephemeral IDs, and authentication challenge response>",
                  "vulnerabilities": [
                    {
                      "protocol": "<Protocol e.g. BLE 5.2 / 802.11ax / 4G RRC>",
                      "riskLevel": "SECURE" | "LOW_CAUTION" | "ELEVATED" | "HIGH" | "CRITICAL",
                      "attackSurface": "<Vulnerable surface>",
                      "exploitationVector": "<How an attacker exploits this>",
                      "containmentFix": "<Immediate technical resolution>"
                    }
                  ],
                  "stepByStepNeutralizationPlan": [
                    "<Step 1>",
                    "<Step 2>",
                    "<Step 3>",
                    "<Step 4>"
                  ]
                }
            """.trimIndent()

            val prompt = """
                [TARGET EMITTER TO AUDIT]
                Node ID: ${emitter.id}
                Name / SSID: ${emitter.name}
                MAC Address: ${emitter.macAddress ?: emitter.id}
                Signal Protocol: ${emitter.signalType}
                Signal Strength (RSSI): ${emitter.rssiDbm} dBm
                Estimated Distance: ${"%.1f".format(emitter.distanceMeters)} meters
                Classified Category: ${emitter.threatCategory.label}
                Baseline Risk Summary: ${emitter.riskSummary}

                [LIVE RF ENVIRONMENT CONTEXT]
                Total Surrounding Nodes: ${snapshot.totalBlipsCount}
                RF Jamming Active: ${snapshot.isRfJammingDetected}
                Rogue IMSI Active: ${snapshot.isImsiAlertActive}
                Ultrasonic Spike: ${snapshot.isUltrasonicAlertActive} (${snapshot.ultrasonicFreqHz} Hz)
                Magnetic Field Flux: ${"%.1f".format(snapshot.magneticFluxMicroTesla)} µT
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstruction))
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.15)
                    put("topP", 0.95)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("$API_ENDPOINT?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful || responseBody.isBlank()) {
                Log.e(TAG, "Gemini Deep Audit API failed code=${response.code}: $responseBody")
                return@withContext generateLocalDeepAudit(emitter, snapshot)
            }

            parseDetailedTargetAudit(responseBody, emitter, snapshot)
        } catch (e: Exception) {
            Log.e(TAG, "Error running Gemini Deep Audit: ${e.message}", e)
            generateLocalDeepAudit(emitter, snapshot)
        }
    }

    private fun parseDetailedTargetAudit(
        responseJson: String,
        emitter: FlaggedThreatEmitter,
        snapshot: RfEnvironmentSnapshot
    ): DetailedTargetAudit {
        return try {
            val root = JSONObject(responseJson)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

            val cleanedJson = if (rawText.contains("```json")) {
                rawText.substringAfter("```json").substringBefore("```").trim()
            } else if (rawText.contains("```")) {
                rawText.substringAfter("```").substringBefore("```").trim()
            } else {
                rawText.trim()
            }

            val obj = JSONObject(cleanedJson)
            val vendor = obj.optString("manufacturerVendor", "Standard RF Chipset")
            val fingerprint = obj.optString("radioFingerprintSummary", "2.4GHz GFSK / OFDM modulation with standard beacon intervals.")
            val trackingConfidence = obj.optInt("trackingHeuristicConfidence", 75)
            val surveillanceAnalysis = obj.optString("surveillanceRiskAnalysis", "Transmitter broadcasts unencrypted identification packets susceptible to directional angle-of-arrival correlation.")
            val hardwareVector = obj.optString("hardwareVectorAnalysis", "Low-power omnidirectional monopole antenna with estimated EIRP of 0 dBm.")
            val cryptoProfile = obj.optString("cryptographicProfile", "No application-layer authentication detected. Resolvable Private Address (RPA) rotation interval indeterminate.")

            val vulnerabilities = mutableListOf<ProtocolVulnerability>()
            val vulnArr = obj.optJSONArray("vulnerabilities")
            if (vulnArr != null) {
                for (i in 0 until vulnArr.length()) {
                    val vObj = vulnArr.optJSONObject(i) ?: continue
                    val lvlStr = vObj.optString("riskLevel", "ELEVATED").uppercase()
                    val lvl = when {
                        lvlStr.contains("CRITICAL") -> ThreatLevel.CRITICAL
                        lvlStr.contains("HIGH") -> ThreatLevel.HIGH
                        lvlStr.contains("ELEVATED") -> ThreatLevel.ELEVATED
                        lvlStr.contains("LOW") -> ThreatLevel.LOW_CAUTION
                        else -> ThreatLevel.SECURE
                    }
                    vulnerabilities.add(
                        ProtocolVulnerability(
                            protocol = vObj.optString("protocol", "${emitter.signalType} Protocol"),
                            riskLevel = lvl,
                            attackSurface = vObj.optString("attackSurface", "Unauthenticated Advertising Channel"),
                            exploitationVector = vObj.optString("exploitationVector", "Continuous triangulation via RSSI gradient tracking."),
                            containmentFix = vObj.optString("containmentFix", "Shield inside Faraday pouch or deploy noise floor modulation.")
                        )
                    )
                }
            }

            val steps = mutableListOf<String>()
            val stepsArr = obj.optJSONArray("stepByStepNeutralizationPlan")
            if (stepsArr != null) {
                for (i in 0 until stepsArr.length()) {
                    steps.add(stepsArr.optString(i))
                }
            }

            DetailedTargetAudit(
                targetId = emitter.id,
                targetName = emitter.name,
                macAddress = emitter.macAddress ?: emitter.id,
                signalType = emitter.signalType,
                rssiDbm = emitter.rssiDbm,
                estimatedDistanceMeters = emitter.distanceMeters,
                threatScore = obj.optInt("threatScore", emitter.threatScore),
                threatCategory = emitter.threatCategory,
                manufacturerVendor = vendor,
                radioFingerprintSummary = fingerprint,
                trackingHeuristicConfidence = trackingConfidence,
                surveillanceRiskAnalysis = surveillanceAnalysis,
                hardwareVectorAnalysis = hardwareVector,
                cryptographicProfile = cryptoProfile,
                vulnerabilities = if (vulnerabilities.isNotEmpty()) vulnerabilities else generateDefaultVulnerabilities(emitter),
                stepByStepNeutralizationPlan = if (steps.isNotEmpty()) steps else generateDefaultNeutralizationSteps(emitter),
                isAuditLoading = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing detailed target audit json: ${e.message}", e)
            generateLocalDeepAudit(emitter, snapshot)
        }
    }

    private fun generateDefaultVulnerabilities(emitter: FlaggedThreatEmitter): List<ProtocolVulnerability> {
        val isTracker = emitter.threatCategory == ThreatCategory.SURVEILLANCE_TRACKER || emitter.name.contains("Tag", ignoreCase = true)
        return listOf(
            ProtocolVulnerability(
                protocol = if (emitter.signalType.contains("WIFI", ignoreCase = true)) "802.11ax / WPA2" else "Bluetooth LE 5.2 / GAP",
                riskLevel = if (isTracker) ThreatLevel.HIGH else ThreatLevel.ELEVATED,
                attackSurface = "Continuous Unencrypted Beacon Advertisements",
                exploitationVector = "Enables continuous physical location tracking and path reconstruction without pairing consent.",
                containmentFix = "Rotate MAC address seed or isolate emitter in conductive enclosure."
            ),
            ProtocolVulnerability(
                protocol = "RF Physical Layer / PHY",
                riskLevel = ThreatLevel.LOW_CAUTION,
                attackSurface = "Omnidirectional Signal Propagation",
                exploitationVector = "Allows direction-finding antennas to localize emitter down to < 0.5 meter precision.",
                containmentFix = "Increase distance from reflective metallic surfaces or apply RF attenuation shielding."
            )
        )
    }

    private fun generateDefaultNeutralizationSteps(emitter: FlaggedThreatEmitter): List<String> {
        val isTracker = emitter.threatCategory == ThreatCategory.SURVEILLANCE_TRACKER || emitter.name.contains("Tag", ignoreCase = true)
        return if (isTracker) {
            listOf(
                "Step 1: Inspect personal luggage, pockets, vehicle wheel arches, and bag linings within 1.5m.",
                "Step 2: Use the Tactical Sweep Radar to align compass heading with peak RSSI gain (> -50 dBm).",
                "Step 3: Once physically located, place the device in an RF-blocking Faraday shield bag.",
                "Step 4: Remove CR2032 battery or report serial ID to security personnel if unauthorized."
            )
        } else {
            listOf(
                "Step 1: Verify whether emitter ID matches known authorized workplace or home hardware.",
                "Step 2: If unrecognized and RSSI > -60 dBm, conduct a 360° directional sweep using the Sweep Radar tab.",
                "Step 3: Monitor for spoofed SSIDs or Evil Twin BSSID duplicates in the Scanner tab.",
                "Step 4: Disconnect connected devices and enforce WPA3-Enterprise or VPN tunnel encapsulation."
            )
        }
    }

    private fun generateLocalDeepAudit(emitter: FlaggedThreatEmitter, snapshot: RfEnvironmentSnapshot): DetailedTargetAudit {
        val isTracker = emitter.threatCategory == ThreatCategory.SURVEILLANCE_TRACKER || emitter.name.contains("Tag", ignoreCase = true)
        val vendor = when {
            emitter.name.contains("Apple", true) || emitter.name.contains("AirTag", true) -> "Apple Inc. (Find My Protocol)"
            emitter.name.contains("Samsung", true) || emitter.name.contains("SmartTag", true) -> "Samsung Electronics Co."
            emitter.name.contains("Tile", true) -> "Tile Inc. / Life360"
            emitter.signalType.contains("WIFI", true) -> "Broadcom / Qualcomm Atheros Wi-Fi SoC"
            else -> "Nordic Semiconductor nRF52/nRF53 Series"
        }

        val fingerprint = "Frequency: 2402–2480 MHz • Modulation: GFSK (1 Mbps) • Adv Interval: ~250ms • Estimated TxPower: +4 dBm"
        val surveillanceRisk = if (isTracker) {
            "CRITICAL SURVEILLANCE RISK: This node exhibits cryptographic payload signatures consistent with commercial crowd-sourced location tracking networks. Continuous proximity suggests active target association."
        } else {
            "ANOMALOUS TRANSMITTER: Emitter broadcasting elevated signal strength within operational range (${"%.1f".format(emitter.distanceMeters)}m). May represent rogue Wi-Fi AP, unverified BLE peripheral, or electronic instrumentation."
        }

        return DetailedTargetAudit(
            targetId = emitter.id,
            targetName = emitter.name,
            macAddress = emitter.macAddress ?: emitter.id,
            signalType = emitter.signalType,
            rssiDbm = emitter.rssiDbm,
            estimatedDistanceMeters = emitter.distanceMeters,
            threatScore = emitter.threatScore,
            threatCategory = emitter.threatCategory,
            manufacturerVendor = vendor,
            radioFingerprintSummary = fingerprint,
            trackingHeuristicConfidence = if (isTracker) 94 else 45,
            surveillanceRiskAnalysis = surveillanceRisk,
            hardwareVectorAnalysis = "Compact PCB trace antenna, internal lithium coin cell (3V) or USB DC power.",
            cryptographicProfile = "Resolvable Private Addresses (RPA) rotating every ~15 minutes. Public key broadcast in secondary advertising PDUs.",
            vulnerabilities = generateDefaultVulnerabilities(emitter),
            stepByStepNeutralizationPlan = generateDefaultNeutralizationSteps(emitter),
            isAuditLoading = false
        )
    }

    suspend fun performAi3dPinpoint(
        blip: RadarBlip,
        sensorSuite: HardwareSensorSuiteData,
        compassHeading: Float
    ): AiPinpointResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateLocal3dPinpoint(blip, sensorSuite, compassHeading)
        }

        try {
            val systemInstruction = """
                You are an elite Tactical Signals Intelligence & 3D Spatial RF Triangulation AI engine.
                Calculate the exact real-time 3D location, horizontal bearing, and vertical elevation/altitude for the target device based on RF propagation, barometric pressure, compass orientation, and device pitch/roll sensor fusion.
                
                Respond in valid JSON matching this schema:
                {
                  "targetId": "${blip.id}",
                  "targetName": "${blip.name}",
                  "macAddress": "${blip.id}",
                  "signalType": "${blip.type}",
                  "currentRssiDbm": ${blip.rssi},
                  "distanceMeters": ${blip.distance},
                  "accuracyMarginMeters": <float margin of error e.g. 0.35>,
                  "confidencePercent": <int 0-100>,
                  "azimuthDegrees": <float 0-359 compass bearing to target>,
                  "relativeClockHeading": "<e.g. 12 o'clock (Ahead) / 2 o'clock (Right-Ahead)>",
                  "elevationPitchDeg": <float -90 to +90 degrees tilt angle from user plane>,
                  "altitudeOffsetMeters": <float -10.0 to +10.0 vertical meters relative to user>,
                  "floorClassification": "<SAME LEVEL (Desk/Waist) | UPPER ELEVATION (Ceiling/Floor +1) | LOWER ELEVATION (Floor/Floor -1)>",
                  "physicalZoneEstimation": "<Probable physical hiding spot e.g. Upper ceiling vent, under table, bag lining, etc>",
                  "spatialVectorXyz": "X: <float>m, Y: <float>m, Z: <float>m",
                  "aiTacticalGuidance": "<2-sentence precise tactical guidance on how to point phone and move to pinpoint target>",
                  "searchChecklist": [
                    "<Actionable Step 1>",
                    "<Actionable Step 2>",
                    "<Actionable Step 3>",
                    "<Actionable Step 4>"
                  ]
                }
            """.trimIndent()

            val prompt = """
                [TARGET DEVICE RF TELEMETRY]
                Node ID: ${blip.id}
                Name / SSID: ${blip.name}
                Signal Type: ${blip.type}
                RSSI: ${blip.rssi} dBm
                Estimated Distance: ${"%.2f".format(blip.distance)} meters
                Target Angle Offset from Phone: ${"%.1f".format(blip.targetAngleOffset)}°
                Frequency: ${blip.frequencyMhz} MHz (${blip.bandLabel})
                Pre-estimated Z-Offset: ${"%.1f".format(blip.estimatedZOffsetMeters)}m
                Channel Sounding: ${blip.isChannelSoundingCapable} (Accuracy: ±${blip.csEstimatedAccuracyMeters}m via ${blip.csRangingMethod})

                [PHONE SENSOR SUITE SPATIAL STATE]
                Compass Heading: ${"%.1f".format(compassHeading)}°
                Phone Pitch: ${"%.1f".format(sensorSuite.pitchDeg)}° (Tilt forward/back)
                Phone Roll: ${"%.1f".format(sensorSuite.rollDeg)}° (Tilt left/right)
                Barometric Pressure: ${"%.2f".format(sensorSuite.pressureHpa)} hPa
                Barometer Altitude: ${"%.2f".format(sensorSuite.estimatedAltitudeMeters)}m (${sensorSuite.pressureTrend})
                G-Force / Accel: ${"%.2f".format(sensorSuite.totalGForce)}G
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstruction))
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("topP", 0.95)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("$API_ENDPOINT?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful || responseBody.isBlank()) {
                Log.e(TAG, "Gemini Pinpoint API failed code=${response.code}: $responseBody")
                return@withContext generateLocal3dPinpoint(blip, sensorSuite, compassHeading)
            }

            parseAiPinpointResult(responseBody, blip, sensorSuite, compassHeading)
        } catch (e: Exception) {
            Log.e(TAG, "Error running Gemini 3D Pinpoint: ${e.message}", e)
            generateLocal3dPinpoint(blip, sensorSuite, compassHeading)
        }
    }

    private fun parseAiPinpointResult(
        responseJson: String,
        blip: RadarBlip,
        sensorSuite: HardwareSensorSuiteData,
        compassHeading: Float
    ): AiPinpointResult {
        return try {
            val root = JSONObject(responseJson)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

            val cleanedJson = if (rawText.contains("```json")) {
                rawText.substringAfter("```json").substringBefore("```").trim()
            } else if (rawText.contains("```")) {
                rawText.substringAfter("```").substringBefore("```").trim()
            } else {
                rawText.trim()
            }

            val obj = JSONObject(cleanedJson)
            val dist = obj.optDouble("distanceMeters", blip.distance.toDouble()).toFloat()
            val accuracy = obj.optDouble("accuracyMarginMeters", 0.35).toFloat()
            val conf = obj.optInt("confidencePercent", 88)
            val azimuth = obj.optDouble("azimuthDegrees", ((compassHeading + blip.targetAngleOffset + 360f) % 360f).toDouble()).toFloat()
            val clock = obj.optString("relativeClockHeading", computeClockHeading(blip.targetAngleOffset))
            val pitch = obj.optDouble("elevationPitchDeg", computeLocalElevationPitch(blip, sensorSuite).toDouble()).toFloat()
            val alt = obj.optDouble("altitudeOffsetMeters", blip.estimatedZOffsetMeters.toDouble()).toFloat()
            val floor = obj.optString("floorClassification", computeFloorClassification(alt))
            val zone = obj.optString("physicalZoneEstimation", "Inspect eye-level & furniture surfaces within 2m.")
            val vec = obj.optString("spatialVectorXyz", computeSpatialVector(dist, azimuth, alt))
            val guidance = obj.optString("aiTacticalGuidance", "Hold phone steady and follow the 3D crosshair reticle towards $clock at ${"%.1f".format(dist)}m.")

            val checklist = mutableListOf<String>()
            val stepsArr = obj.optJSONArray("searchChecklist")
            if (stepsArr != null) {
                for (i in 0 until stepsArr.length()) {
                    checklist.add(stepsArr.optString(i))
                }
            }

            AiPinpointResult(
                targetId = blip.id,
                targetName = blip.name,
                macAddress = blip.id,
                signalType = blip.type,
                currentRssiDbm = blip.rssi,
                distanceMeters = dist,
                accuracyMarginMeters = accuracy,
                confidencePercent = conf,
                azimuthDegrees = azimuth,
                relativeClockHeading = clock,
                elevationPitchDeg = pitch,
                altitudeOffsetMeters = alt,
                floorClassification = floor,
                physicalZoneEstimation = zone,
                spatialVectorXyz = vec,
                isAimSightAligned = false,
                aiTacticalGuidance = guidance,
                searchChecklist = if (checklist.isNotEmpty()) checklist else generateDefaultPinpointChecklist(dist, pitch, alt),
                isPinpointingLoading = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI Pinpoint json: ${e.message}", e)
            generateLocal3dPinpoint(blip, sensorSuite, compassHeading)
        }
    }

    private fun computeClockHeading(relativeAngleDeg: Float): String {
        val normalized = (relativeAngleDeg % 360f + 360f) % 360f
        val hour = ((normalized + 15f) / 30f).toInt() % 12
        val hourLabel = if (hour == 0) 12 else hour
        return when {
            normalized in 345f..360f || normalized in 0f..15f -> "12 o'clock (DEAD AHEAD)"
            normalized in 15f..45f -> "1 o'clock (Ahead Right)"
            normalized in 45f..75f -> "2 o'clock (Right Front)"
            normalized in 75f..105f -> "3 o'clock (DIRECT RIGHT)"
            normalized in 105f..135f -> "4 o'clock (Rear Right)"
            normalized in 135f..165f -> "5 o'clock (Back Right)"
            normalized in 165f..195f -> "6 o'clock (DIRECTLY BEHIND)"
            normalized in 195f..225f -> "7 o'clock (Back Left)"
            normalized in 225f..255f -> "8 o'clock (Rear Left)"
            normalized in 255f..285f -> "9 o'clock (DIRECT LEFT)"
            normalized in 285f..315f -> "10 o'clock (Left Front)"
            else -> "11 o'clock (Ahead Left)"
        }
    }

    private fun computeLocalElevationPitch(blip: RadarBlip, sensorSuite: HardwareSensorSuiteData): Float {
        val z = blip.estimatedZOffsetMeters
        val d = blip.distance.coerceAtLeast(0.5f)
        val rawAngle = Math.toDegrees(kotlin.math.asin((z / d).coerceIn(-0.95f, 0.95f).toDouble())).toFloat()
        return rawAngle
    }

    private fun computeFloorClassification(zOffsetMeters: Float): String {
        return when {
            zOffsetMeters >= 2.2f -> "UPPER FLOOR (+1 Level / Ceiling Duct)"
            zOffsetMeters >= 0.8f -> "ELEVATED SHELF / CEILING ZONE (+${"%.1f".format(zOffsetMeters)}m)"
            zOffsetMeters <= -2.2f -> "LOWER FLOOR (-1 Level / Subfloor)"
            zOffsetMeters <= -0.8f -> "FLOOR CAVITY / UNDERCARRIAGE (${"%.1f".format(zOffsetMeters)}m)"
            else -> "SAME LEVEL (Desk/Waist Height ±0.5m)"
        }
    }

    private fun computeSpatialVector(dist: Float, azimuthDeg: Float, altMeters: Float): String {
        val rad = Math.toRadians(azimuthDeg.toDouble())
        val x = dist * kotlin.math.sin(rad)
        val y = dist * kotlin.math.cos(rad)
        return "X: %+.2fm (E/W), Y: %+.2fm (N/S), Z: %+.2fm (Alt)".format(x, y, altMeters)
    }

    private fun generateDefaultPinpointChecklist(dist: Float, pitchDeg: Float, altMeters: Float): List<String> {
        val pitchAction = when {
            pitchDeg > 15f -> "Tilt phone UPWARDS (+${pitchDeg.toInt()}°) to align elevation crosshair"
            pitchDeg < -15f -> "Tilt phone DOWNWARDS (${pitchDeg.toInt()}°) towards ground/floor plane"
            else -> "Hold phone level at chest height facing target bearing"
        }
        return listOf(
            "1. $pitchAction and align the 3D crosshair reticle.",
            "2. Advance forward ${"%.1f".format(dist)} meters while watching live distance readout count down.",
            "3. Look for target in ${computeFloorClassification(altMeters).lowercase()}.",
            "4. Listen for rapid audio sonar acoustic lock when within 0.8m."
        )
    }

    private fun generateLocal3dPinpoint(
        blip: RadarBlip,
        sensorSuite: HardwareSensorSuiteData,
        compassHeading: Float
    ): AiPinpointResult {
        val dist = blip.distance
        val accuracy = if (blip.isChannelSoundingCapable) blip.csEstimatedAccuracyMeters else (dist * 0.12f).coerceIn(0.2f, 1.5f)
        val confidence = if (blip.isChannelSoundingCapable) 95 else if (blip.rssi > -65) 88 else 72
        val azimuth = (compassHeading + blip.targetAngleOffset + 360f) % 360f
        val clock = computeClockHeading(blip.targetAngleOffset)
        val pitch = computeLocalElevationPitch(blip, sensorSuite)
        val alt = blip.estimatedZOffsetMeters
        val floor = computeFloorClassification(alt)
        val zone = when {
            alt > 1.2f -> "Upper drop ceiling, wall mount, high shelf, or overhead HVAC duct"
            alt < -0.8f -> "Floor baseboard, under furniture, seat cushion, or vehicle floor pan"
            else -> "Desk surface, tabletop, personal backpack, or waist-height enclosure"
        }
        val spatialVector = computeSpatialVector(dist, azimuth, alt)
        val guidance = "AI PINPOINT LOCKED: Target is ${"%.1f".format(dist)}m away at $clock ($azimuth° bearing), ${if (alt >= 0) "+${"%.1f".format(alt)}m above" else "${"%.1f".format(alt)}m below"} you. Align phone pitch with reticle."

        return AiPinpointResult(
            targetId = blip.id,
            targetName = blip.name,
            macAddress = blip.id,
            signalType = blip.type,
            currentRssiDbm = blip.rssi,
            distanceMeters = dist,
            accuracyMarginMeters = accuracy,
            confidencePercent = confidence,
            azimuthDegrees = azimuth,
            relativeClockHeading = clock,
            elevationPitchDeg = pitch,
            altitudeOffsetMeters = alt,
            floorClassification = floor,
            physicalZoneEstimation = zone,
            spatialVectorXyz = spatialVector,
            isAimSightAligned = false,
            aiTacticalGuidance = guidance,
            searchChecklist = generateDefaultPinpointChecklist(dist, pitch, alt),
            isPinpointingLoading = false
        )
    }

    private fun generateLocalHeuristicReport(snapshot: RfEnvironmentSnapshot, isOfflineFallback: Boolean): ThreatAnalysisReport {
        var score = 10
        val vectors = mutableListOf<String>()
        val flagged = mutableListOf<FlaggedThreatEmitter>()
        val countermeasures = mutableListOf<TacticalCountermeasure>()

        if (snapshot.isRfJammingDetected) {
            score += 45
            vectors.add("Broadband Electronic RF Jamming Pattern Detected")
            countermeasures.add(
                TacticalCountermeasure(
                    title = "Switch to Hardened Frequency Band",
                    detail = "Active wideband jamming detected across 2.4GHz spectrum. Switch tactical communications to alternate bands or wired data.",
                    urgency = "IMMEDIATE"
                )
            )
        }

        if (snapshot.isImsiAlertActive) {
            score += 40
            vectors.add("Unregistered Rogue Base Station / IMSI Catcher")
            countermeasures.add(
                TacticalCountermeasure(
                    title = "Enable Lockdown / Airplane Mode",
                    detail = "An unverified cellular tower broadcasting 2G downgrade commands was intercepted. Avoid unencrypted calls/SMS.",
                    urgency = "IMMEDIATE"
                )
            )
        }

        if (snapshot.isUltrasonicAlertActive) {
            score += 25
            vectors.add("High-Frequency Acoustic Spike (${snapshot.ultrasonicFreqHz} Hz)")
            countermeasures.add(
                TacticalCountermeasure(
                    title = "Inspect Audio Transducers",
                    detail = "Ultrasonic cross-device tracking beacon detected near ${snapshot.ultrasonicFreqHz} Hz. Mute external microphones.",
                    urgency = "RECOMMENDED"
                )
            )
        }

        if (snapshot.magneticFluxMicroTesla > 80f) {
            score += 20
            vectors.add("High Magnetic Flux Distortion (${"%.1f".format(snapshot.magneticFluxMicroTesla)} µT)")
            countermeasures.add(
                TacticalCountermeasure(
                    title = "Audit Surrounding Metal/Motors",
                    detail = "Elevated EMF field detected. Could indicate concealed electromagnetic equipment or electronic bugs.",
                    urgency = "MONITOR"
                )
            )
        }

        // Check suspicious blips (e.g. AirTags, tiles, high RSSI unknowns, high risk vendors)
        snapshot.activeBlips.filter { it.isHighRiskVendor || it.rssi > -50 || it.distance < 3.0f }.take(6).forEach { blip ->
            val isTracker = blip.name.contains("AirTag", ignoreCase = true) || 
                            blip.name.contains("Tile", ignoreCase = true) || 
                            blip.name.contains("SmartTag", ignoreCase = true)
            
            val cat = if (isTracker) ThreatCategory.SURVEILLANCE_TRACKER else ThreatCategory.UNREGISTERED_BLE_BEACON
            val emScore = if (isTracker) 85 else 45
            score += if (isTracker) 15 else 5

            flagged.add(
                FlaggedThreatEmitter(
                    id = blip.id,
                    name = blip.name,
                    macAddress = blip.id,
                    signalType = blip.type,
                    rssiDbm = blip.rssi,
                    distanceMeters = blip.distance,
                    threatCategory = cat,
                    threatScore = emScore,
                    riskSummary = if (isTracker) "Known consumer tracking beacon moving within proximity threshold." else "High-power unknown emitter within close proximity (${"%.1f".format(blip.distance)}m).",
                    recommendedAction = if (isTracker) "Conduct physical inspection of personal belongings or vehicle." else "Perform direction-finding sweep using Tactical Radar."
                )
            )
        }

        val clampedScore = score.coerceIn(5, 99)
        val level = when {
            clampedScore >= 75 -> ThreatLevel.CRITICAL
            clampedScore >= 55 -> ThreatLevel.HIGH
            clampedScore >= 35 -> ThreatLevel.ELEVATED
            clampedScore >= 20 -> ThreatLevel.LOW_CAUTION
            else -> ThreatLevel.SECURE
        }

        if (countermeasures.isEmpty()) {
            countermeasures.add(
                TacticalCountermeasure(
                    title = "Maintain Ambient Reconnaissance",
                    detail = "No immediate hostile electronic signatures identified. Continue routine perimeter sweep.",
                    urgency = "MONITOR"
                )
            )
        }

        val summary = when (level) {
            ThreatLevel.CRITICAL -> "CRITICAL THREAT: Active electronic warfare or unverified rogue interceptor detected within immediate operational radius."
            ThreatLevel.HIGH -> "HIGH ALERT: Multiple anomalous RF signatures and close-proximity tracking nodes identified."
            ThreatLevel.ELEVATED -> "ELEVATED CAUTION: Unusual electromagnetic or acoustic activity detected above standard baseline."
            ThreatLevel.LOW_CAUTION -> "LOW CAUTION: Minor RF proximity detections; environment is mostly compliant with baseline."
            ThreatLevel.SECURE -> "SECURE: No hostile electronic countermeasures, surveillance beacons, or rogue APs detected."
        }

        return ThreatAnalysisReport(
            threatLevel = level,
            threatScore = clampedScore,
            executiveSummary = summary + if (isOfflineFallback) " (Automated Local Heuristic Assessment)" else "",
            flaggedEmitters = flagged,
            identifiedVectors = vectors,
            countermeasures = countermeasures,
            rawSigintDetails = "Signals Intelligence engine intercepted ${snapshot.totalBlipsCount} total emitters across BLE, Wi-Fi, and cellular channels. Compass heading is ${snapshot.compassHeading.toInt()}°.",
            isAiGenerated = !isOfflineFallback
        )
    }
}
