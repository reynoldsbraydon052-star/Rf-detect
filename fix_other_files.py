import re

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'r') as f:
    content = f.read()

# Fix severityScore -> severity
content = content.replace('anomaly.severityScore > 80', 'anomaly.severity == "HIGH" || anomaly.severity == "CRITICAL"')
content = content.replace('anomaly.severityScore > 50', 'anomaly.severity == "MEDIUM"')
# Fix relatedDeviceIdsJson -> deviceId
content = content.replace('val devicesJson = anomaly.relatedDeviceIdsJson', 'val deviceId = anomaly.deviceId ?: ""')
content = content.replace('Text(text = "Related Devices: $devicesJson"', 'Text(text = "Device: $deviceId"')
# If there are others
content = content.replace('anomaly.severityScore', 'anomaly.evidenceScore')
content = content.replace('anomaly.relatedDeviceIdsJson', 'anomaly.deviceId')

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/RfIntelligenceCorrelationEngine.kt', 'r') as f:
    content = f.read()

content = content.replace('it.relatedDeviceIdsJson.contains(blip.id)', 'it.deviceId == blip.id')

with open('app/src/main/java/com/example/RfIntelligenceCorrelationEngine.kt', 'w') as f:
    f.write(content)
