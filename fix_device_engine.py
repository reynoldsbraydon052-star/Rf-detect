import re

with open('app/src/main/java/com/example/DeviceIdentityEngine.kt', 'r') as f:
    content = f.read()

# Update loadHypotheses
content = content.replace("suspend fun loadHypotheses() {", "suspend fun loadHypothesesForSession(sessionId: String) {")
content = content.replace("dao.getAllHypotheses().map", "dao.getHypothesesForSession(sessionId).map")

# Update processObservations signature
content = content.replace("suspend fun processObservations(blips: List<RadarBlip>, fingerprints: Map<String, SignalFingerprint>) {", "suspend fun processObservations(blips: List<RadarBlip>, fingerprints: Map<String, SignalFingerprint>, sessionId: String) {")

# Update constructor call
content = content.replace(
    "val newHypothesis = DeviceIdentityHypothesis(\n                    primaryMac = blip.id,",
    "val newHypothesis = DeviceIdentityHypothesis(\n                    sessionId = sessionId,\n                    primaryMac = blip.id,"
)

with open('app/src/main/java/com/example/DeviceIdentityEngine.kt', 'w') as f:
    f.write(content)
