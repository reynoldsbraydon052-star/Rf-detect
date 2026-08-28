import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

init_injection = """    init {
        viewModelScope.launch {
            rfSessionEngine.loadActiveSession()
            val active = rfSessionEngine.activeSession.value
            if (active == null) {
                rfSessionEngine.createNewSession("Investigation - " + java.util.UUID.randomUUID().toString().take(8))
            }
            val sessionId = rfSessionEngine.getActiveSessionId()
            if (sessionId != null) {
                deviceIdentityEngine.loadHypothesesForSession(sessionId)
            }
        }"""

content = content.replace("    init {", init_injection, 1)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
