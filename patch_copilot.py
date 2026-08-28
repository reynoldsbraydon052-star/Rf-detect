import re

with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'r') as f:
    content = f.read()

# Let's replace `snapshot: RfEnvironmentSnapshot` with `evidencePkg: AiEvidencePackage` in askTacticalCopilot
# And change the system prompt

func_copilot = """
    suspend fun askTacticalCopilot(
        query: String,
        evidencePkg: AiEvidencePackage,
        conversationHistory: List<TacticalCopilotMessage>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key missing. Cannot query evidence-based investigator."
        }

        try {
            val obsList = evidencePkg.observations.take(15).joinToString("\\n") { 
                "- ID:\${it.id.take(12)} Name:\${it.name} RSSI:\${it.rssi}dBm Dist:\${"%.1f".format(it.distance)}m Type:\${it.type}"
            }
            
            val telemetryContext = \"\"\"
                [EVIDENCE PACKAGE]
                Data Provenance: \${evidencePkg.provenance}
                Simulation Mode: \${evidencePkg.isSimulation}
                Replay Mode: \${evidencePkg.isReplay}
                Hardware Capabilities: \${evidencePkg.hardwareCapabilities.joinToString()}
                Baseline Summary: \${evidencePkg.baselineSummary}
                Anomaly Score: \${evidencePkg.anomalyScore} (Conf: \${evidencePkg.anomalyConfidence})
                Correlations: \${evidencePkg.correlations.joinToString()}
                
                [OBSERVATIONS SAMPLE]
                $obsList
            \"\"\".trimIndent()

            val contentsArray = JSONArray()
            conversationHistory.takeLast(6).forEach { msg ->
                contentsArray.put(JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", msg.text))
                    })
                })
            }

            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", "$telemetryContext\\n\\nUser Query: $query"))
                })
            })

            val systemPrompt = "You are an EVIDENCE-BASED AI INVESTIGATOR. Answer the user's questions based strictly on the provided evidence package. Distinguish between FACT, INFERENCE, HYPOTHESIS, and UNKNOWN. Do not fabricate answers. Do not use hyperbolic threat language unless explicitly supported by evidence."
            
            val requestBodyJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 800)
                })
            }

            val request = Request.Builder()
                .url("\$API_ENDPOINT?key=\$apiKey")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful || responseBody.isBlank()) {
                return@withContext "Error: Investigator API offline (\${response.code})."
            }

            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")
            
            text ?: "The investigator returned an empty response."
        } catch (e: Exception) {
            Log.e(TAG, "Copilot query error: \${e.message}", e)
            "Investigator network exception: \${e.localizedMessage ?: "Unknown error"}"
        }
    }
"""

content = re.sub(r'suspend fun askTacticalCopilot\(.*?return@withContext.*?}\s*}\s*}', func_copilot.strip(), content, flags=re.DOTALL)

with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'w') as f:
    f.write(content)

