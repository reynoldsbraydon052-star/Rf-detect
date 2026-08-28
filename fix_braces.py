with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'r') as f:
    content = f.read()

content = content.replace("stepByStepNeutralizationPlan = listOf(\"Monitor closely\")\n        )", "stepByStepNeutralizationPlan = listOf(\"Monitor closely\")\n        )\n    }")

content = content.replace("searchChecklist = listOf(\"Visual inspection\")\n        )\n    }", "searchChecklist = listOf(\"Visual inspection\")\n        )\n    }\n}")

with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'w') as f:
    f.write(content)
