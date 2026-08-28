with open('app/src/main/java/com/example/SignalFingerprintEntity.kt', 'r') as f:
    content = f.read()

target1 = "val knownMacAddress: String? = null // For strong matching when available"
if "val lastAnomalyScore: Int? = null" not in content:
    content = content.replace(target1, "val knownMacAddress: String? = null, // For strong matching when available\n    val lastAnomalyScore: Int? = null,\n    val lastAnomalyConfidence: Float? = null")

target2 = "knownMacAddress = this.knownMacAddress"
if "lastAnomalyScore = this.lastAnomalyScore" not in content:
    content = content.replace(target2, "knownMacAddress = this.knownMacAddress,\n        lastAnomalyScore = this.lastAnomalyScore,\n        lastAnomalyConfidence = this.lastAnomalyConfidence")

with open('app/src/main/java/com/example/SignalFingerprintEntity.kt', 'w') as f:
    f.write(content)
