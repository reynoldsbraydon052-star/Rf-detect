import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

resume_fun = """
    fun resumeInvestigationSession(sessionId: String) {
        viewModelScope.launch {
            rfSessionEngine.resumeSession(sessionId)
            deviceIdentityEngine.loadHypothesesForSession(sessionId)
        }
    }
"""

if "fun resumeInvestigationSession" not in content:
    content = content.replace("fun clearAllData() {", resume_fun + "\n    fun clearAllData() {")

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
