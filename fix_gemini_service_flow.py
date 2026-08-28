import re

with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'r') as f:
    content = f.read()

flow_import = "import kotlinx.coroutines.flow.StateFlow\n"
content = content.replace("package com.example\n", "package com.example\n\n" + flow_import)

status_property = """
    val geminiStatus: StateFlow<GeminiStatus> = apiClient.status
"""

content = content.replace("    companion object {", status_property + "\n    companion object {")

with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'w') as f:
    f.write(content)
