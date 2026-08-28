import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "deviceIdentityEngine.processObservations(evaluatedBlips, cachedFingerprints)",
    "val sessionId = rfSessionEngine.getActiveSessionId()\n                if (sessionId != null) {\n                    deviceIdentityEngine.processObservations(evaluatedBlips, cachedFingerprints, sessionId)\n                }"
)

# And remove the duplicate sessionId fetch
content = content.replace(
    """                val sessionId = rfSessionEngine.getActiveSessionId()
                if (sessionId != null) {
                    deviceIdentityEngine.processObservations(evaluatedBlips, cachedFingerprints, sessionId)
                }
                
                val sessionId = rfSessionEngine.getActiveSessionId()""",
    """                val sessionId = rfSessionEngine.getActiveSessionId()
                if (sessionId != null) {
                    deviceIdentityEngine.processObservations(evaluatedBlips, cachedFingerprints, sessionId)"""
)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
