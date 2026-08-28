import re
with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'r') as f:
    content = f.read()

# Replace first one
content = re.sub(
    r'AiInvestigatorAssessment\(\s*assessment\s*=\s*"API Key missing\. Local analysis only\.",\s*unknowns\s*=\s*listOf\("Generative AI analysis unavailable without API key\."\)\s*\)',
    r'AiInvestigatorAssessment(assessment = "API Key missing. Local analysis only.", confidence = 0)',
    content
)

# Replace the second one (line 143)
replacement = r"""            AiInvestigatorAssessment(
                assessment = factObj.optString("assessment", "Analysis completed."),
                confidence = factObj.optString("confidence", "50").toIntOrNull() ?: 50
            )"""

content = re.sub(
    r'AiInvestigatorAssessment\([\s\S]*?evidenceReferences = [^\n]*\)',
    replacement.strip(),
    content
)

with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/GeminiThreatModels.kt', 'r') as f:
    models_content = f.read()

models_content = models_content.replace(
    "package com.example\n\nenum class InferenceType {\n    DETERMINISTIC,\n    PROBABILISTIC,\n    HEURISTIC,\n    UNKNOWN\n}",
    "package com.example"
)

models_content = models_content.replace(
    "import androidx.compose.ui.graphics.Color",
    "import androidx.compose.ui.graphics.Color\n\nenum class InferenceType {\n    DETERMINISTIC,\n    PROBABILISTIC,\n    HEURISTIC,\n    UNKNOWN\n}"
)

with open('app/src/main/java/com/example/GeminiThreatModels.kt', 'w') as f:
    f.write(models_content)

