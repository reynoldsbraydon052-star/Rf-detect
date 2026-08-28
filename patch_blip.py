with open('app/src/main/java/com/example/RadarBlip.kt', 'r') as f:
    content = f.read()

target1 = "val pulseRepetitionIntervalMs: Double? = null"
if "val anomalyResult: AnomalyResult? = null" not in content:
    content = content.replace(target1, "val pulseRepetitionIntervalMs: Double? = null,\n    val anomalyResult: AnomalyResult? = null")

with open('app/src/main/java/com/example/RadarBlip.kt', 'w') as f:
    f.write(content)
