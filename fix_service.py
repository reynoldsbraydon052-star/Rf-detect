with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'r') as f:
    content = f.read()

# Fix the extra brace
content = content.replace("    }\n    }\n    suspend fun performAi3dPinpoint", "    }\n    suspend fun performAi3dPinpoint")

# Fix the unresolved macAddress
content = content.replace("macAddress = blip.macAddress ?: \"Unknown\"", "macAddress = blip.id")

# Add buildEnvironmentPrompt
prompt_func = """    private fun buildEnvironmentPrompt(snapshot: RfEnvironmentSnapshot): String {
        return "Analyze this RF snapshot: ${snapshot.totalBlipsCount} blips."
    }

    suspend fun analyzeRfEnvironment"""

content = content.replace("    suspend fun analyzeRfEnvironment", prompt_func)

with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'w') as f:
    f.write(content)
