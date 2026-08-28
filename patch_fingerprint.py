with open('app/src/main/java/com/example/SignalFingerprint.kt', 'r') as f:
    content = f.read()

target1 = "val knownMacAddress: String? = null"
if "val lastAnomalyScore: Int? = null" not in content:
    content = content.replace(target1, "val knownMacAddress: String? = null,\n    val lastAnomalyScore: Int? = null,\n    val lastAnomalyConfidence: Float? = null")

target2 = "knownMacAddress = knownMacAddress"
if "lastAnomalyScore = lastAnomalyScore" not in content:
    content = content.replace(target2, "knownMacAddress = knownMacAddress,\n            lastAnomalyScore = lastAnomalyScore,\n            lastAnomalyConfidence = lastAnomalyConfidence")

target3 = "knownMacAddress = entity.knownMacAddress"
if "lastAnomalyScore = entity.lastAnomalyScore" not in content:
    content = content.replace(target3, "knownMacAddress = entity.knownMacAddress,\n                lastAnomalyScore = entity.lastAnomalyScore,\n                lastAnomalyConfidence = entity.lastAnomalyConfidence")

with open('app/src/main/java/com/example/SignalFingerprint.kt', 'w') as f:
    f.write(content)
