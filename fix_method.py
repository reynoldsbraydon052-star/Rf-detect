import re
with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'r') as f:
    content = f.read()

content = content.replace("performAi3dPinpoint(blip: RadarBlip, snapshot: RfEnvironmentSnapshot)", "performAi3dPinpoint(blip: RadarBlip, sensorSuite: EnvironmentalSensorSuite, heading: Float)")

with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'w') as f:
    f.write(content)
