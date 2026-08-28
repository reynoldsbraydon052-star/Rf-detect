import re

with open('app/src/main/java/com/example/SimulationLabScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("activeSession.totalDurationMs", "((activeSession.endTimeMs ?: System.currentTimeMillis()) - activeSession.startTimeMs)")
content = content.replace("session.events.size", "session.eventCount")

with open('app/src/main/java/com/example/SimulationLabScreen.kt', 'w') as f:
    f.write(content)

