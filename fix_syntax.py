import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

# Fix the duplicate block
bad_block = """                val sessionId = rfSessionEngine.getActiveSessionId()
                if (sessionId != null) {
                    deviceIdentityEngine.processObservations(evaluatedBlips, cachedFingerprints, sessionId)
                if (sessionId != null) {"""

good_block = """                val sessionId = rfSessionEngine.getActiveSessionId()
                if (sessionId != null) {
                    deviceIdentityEngine.processObservations(evaluatedBlips, cachedFingerprints, sessionId)"""

content = content.replace(bad_block, good_block)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
