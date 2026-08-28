import re
with open('app/src/main/java/com/example/AudioRadarTracker.kt', 'r') as f:
    content = f.read()
content = content.replace("currentState = when", "_currentState.value = when")
with open('app/src/main/java/com/example/AudioRadarTracker.kt', 'w') as f:
    f.write(content)
