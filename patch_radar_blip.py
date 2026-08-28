with open('app/src/main/java/com/example/RadarBlip.kt', 'r') as f:
    content = f.read()

target1 = "val anomalyResult: AnomalyResult? = null"
if "val timestampMs: Long = System.currentTimeMillis()" not in content:
    content = content.replace(target1, target1 + ",\n    val timestampMs: Long = System.currentTimeMillis()")

with open('app/src/main/java/com/example/RadarBlip.kt', 'w') as f:
    f.write(content)
