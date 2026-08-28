import re

with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'r') as f:
    content = f.read()

func_code = """
    suspend fun runEvidenceInvestigator(evidencePkg: AiEvidencePackage, query: String? = null): AiInvestigatorAssessment = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext AiInvestigatorAssessment(
                assessment = "API Key missing. Local analysis only.",
                unknowns = listOf("Generative AI analysis unavailable without API key.")
            )
        }

        try {
            val systemPrompt = \"\"\"
                You are an EVIDENCE-BASED AI INVESTIGATOR analyzing RF and sensor telemetry.
                PRIMARY PRINCIPLE: Distinguish between FACT, INFERENCE, HYPOTHESIS, UNKNOWN.
                NEVER turn an inference or hypothesis into a fact.
                Do not automatically use threat language (e.g. spy device, bug, hacker).
                Challenge user assumptions if the evidence does not support them.
                When recommending next measurements, only suggest based on available hardware.
                You must format your response exactly as a JSON object matching this structure:
                {
                    "assessment": "High level assessment summary",
                    "facts": ["Fact 1", "Fact 2"],
                    "inferences": ["Inference 1"],
                    "hypotheses": ["Hypothesis 1", "Alternative 1"],
                    "alternativeExplanations": ["Benign explanation 1", "Benign 2"],
                    "unknowns": ["Unknown 1", "Unknown 2"],
                    "confidence": 0.85,
                    "recommendedMeasurements": ["Actionable step 1", "Actionable step 2"],
                    "evidenceReferences": ["Reference to data 1"]
                }
            \"\"\".trimIndent()

            val obsList = evidencePkg.observations.take(20).joinToString("\\n") { 
                "Obs: \${it.id}, Type: \${it.type}, Name: \${it.name}, RSSI: \${it.rssi}, Freq: \${it.frequencyMhz}MHz, Band: \${it.bandLabel}"
            }
            
            val dataState = when {
                evidencePkg.isSimulation -> "SIMULATED DATA"
                evidencePkg.isReplay -> "REPLAYED RECORDED DATA"
                else -> "LIVE MEASUREMENT"
            }

            val evidenceText = \"\"\"
                [STATE]: $dataState
                [HARDWARE AVAILABLE]: \${evidencePkg.hardwareCapabilities.joinToString()}
                [BASELINE]: \${evidencePkg.baselineSummary}
                [ANOMALY SCORE]: \${evidencePkg.anomalyScore} (Conf: \${evidencePkg.anomalyConfidence})
                [ANOMALY EXPLANATIONS]: \${evidencePkg.anomalyExplanations.joinToString()}
                [CORRELATIONS]: \${evidencePkg.correlations.joinToString()}
                [OBSERVATIONS]:
                $obsList
                \${if (query != null) "\\n[USER QUERY]: $query" else ""}
            \"\"\".trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", evidenceText))
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("\$API_ENDPOINT?key=\$apiKey")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful || responseBody.isBlank()) {
                Log.e(TAG, "Gemini API request failed code=\${response.code}: $responseBody")
                return@withContext AiInvestigatorAssessment(assessment = "API Error \${response.code}")
            }

            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: "{}"
            
            val json = JSONObject(text)
            
            fun parseList(key: String): List<String> {
                val arr = json.optJSONArray(key) ?: return emptyList()
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) { list.add(arr.optString(i)) }
                return list
            }
            
            AiInvestigatorAssessment(
                assessment = json.optString("assessment", ""),
                facts = parseList("facts"),
                inferences = parseList("inferences"),
                hypotheses = parseList("hypotheses"),
                alternativeExplanations = parseList("alternativeExplanations"),
                unknowns = parseList("unknowns"),
                confidence = json.optDouble("confidence", 0.0).toFloat(),
                recommendedMeasurements = parseList("recommendedMeasurements"),
                evidenceReferences = parseList("evidenceReferences")
            )
        } catch (e: Exception) {
            Log.e(TAG, "runEvidenceInvestigator error: \${e.message}", e)
            AiInvestigatorAssessment(assessment = "Local fallback due to error: \${e.message}")
        }
    }
"""

if "fun runEvidenceInvestigator" not in content:
    content = content.replace("    suspend fun analyzeRfEnvironment", func_code + "\n    suspend fun analyzeRfEnvironment")
    with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'w') as f:
        f.write(content)
        print("Updated GeminiThreatAnalysisService.kt")
else:
    print("Already there")
