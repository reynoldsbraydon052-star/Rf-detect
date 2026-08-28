import re

with open('app/src/main/java/com/example/RfEventRecorderEngine.kt', 'r') as f:
    content = f.read()

# Fix the broken append lines
content = re.sub(
    r'builder\.append\("timestamp,event_id,source,signal_type,device_id,frequency_mhz,channel,band,rssi,distance,classification,confidence,fingerprint_id,anomaly_score,evidence_score\s+"\)',
    r'builder.append("timestamp,event_id,source,signal_type,device_id,frequency_mhz,channel,band,rssi,distance,classification,confidence,fingerprint_id,anomaly_score,evidence_score\\n")',
    content
)

content = re.sub(
    r'builder\.append\("\$\{e\.timestampMs\},"(\$\{e\.eventId\}[^\n]+)\s+"\)',
    r'builder.append("${e.timestampMs},\\"${e.eventId}\\",\\"${e.sensorSource}\\",\\"${e.signalType}\\",\\"${e.deviceId}\\",${e.frequencyMhz},${e.channel ?: \\"\\"},\\"${e.bandLabel}\\",${e.rssi},${e.distanceMeters ?: \\"\\"},\\"${e.classification ?: \\"\\"}\",${e.classificationConfidence ?: \\"\\"},\\"${e.fingerprintId ?: \\"\\"}\\",${e.anomalyScore ?: \\"\\"},${e.evidenceScore ?: \\"\\"}\\n")',
    content
)

content = content.replace("activeScanMode = uiState.activeScanMode.name", "activeScanMode = uiState.scanMode.name")

with open('app/src/main/java/com/example/RfEventRecorderEngine.kt', 'w') as f:
    f.write(content)
