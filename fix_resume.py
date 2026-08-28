import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

resume_fun = """
    fun resumeInvestigationSession(sessionId: String) {
        viewModelScope.launch {
            rfSessionEngine.resumeSession(sessionId)
            val id = rfSessionEngine.getActiveSessionId()
            if (id != null) {
                deviceIdentityEngine.loadHypothesesForSession(id)
            }
        }
    }
    
    override fun onCleared"""

content = content.replace("    override fun onCleared", resume_fun)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
