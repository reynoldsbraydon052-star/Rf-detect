import re

with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'r') as f:
    content = f.read()

system_prompt_new = """            val systemPrompt = \"\"\"
                You are an EVIDENCE-BASED AI INVESTIGATOR analyzing RF and sensor telemetry.
                PRIMARY PRINCIPLE: Distinguish between FACT, INFERENCE, HYPOTHESIS, UNKNOWN.
                NEVER turn an inference or hypothesis into a fact. Facts must come from measurements.
                Do not automatically use threat language (e.g. spy device, bug, hacker).
                Consider plausible alternative benign explanations for ambiguous events (e.g., normal Wi-Fi, interference).
                Challenge user assumptions if the evidence does not support them. Explain what is established, what is not, and what is needed.
                When recommending next best measurements, only suggest based on the AVAILABLE HARDWARE explicitly listed. Never recommend a measurement as if it has occurred if it hasn't.
                Assess confidence qualitatively as LOW, MEDIUM, or HIGH based on the amount and quality of evidence.
                You must format your response exactly as a JSON object matching this structure:
                {
                    "assessment": "High level assessment summary",
                    "facts": ["Fact 1", "Fact 2"],
                    "inferences": ["Inference 1"],
                    "hypotheses": ["Hypothesis 1"],
                    "alternativeExplanations": ["Benign explanation 1", "Benign 2"],
                    "unknowns": ["Unknown 1", "Unknown 2"],
                    "confidence": "HIGH",
                    "recommendedMeasurements": ["Actionable step 1", "Actionable step 2"],
                    "evidenceReferences": ["Reference to data 1"]
                }
            \"\"\".trimIndent()"""

content = re.sub(
    r'val systemPrompt = """.*?""".trimIndent\(\)',
    system_prompt_new,
    content,
    flags=re.DOTALL
)

content = content.replace('json.optDouble("confidence", 0.0).toFloat()', 'json.optString("confidence", "UNKNOWN")')

with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/EvidenceModels.kt', 'r') as f:
    content = f.read()

content = content.replace('val confidence: Float = 0f', 'val confidence: String = "UNKNOWN"')
content = content.replace('val confidence: Float,', 'val confidence: String,')

with open('app/src/main/java/com/example/EvidenceModels.kt', 'w') as f:
    f.write(content)

with open('app/src/test/java/com/example/AiInvestigatorTest.kt', 'r') as f:
    content = f.read()

content = content.replace('confidence = 0.9f', 'confidence = "HIGH"')
content = content.replace('assertEquals(0.9f, interpretation.confidence, 0.001f)', 'assertEquals("HIGH", interpretation.confidence)')

with open('app/src/test/java/com/example/AiInvestigatorTest.kt', 'w') as f:
    f.write(content)

