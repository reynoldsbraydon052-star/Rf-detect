import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

# Add to SignalRadarViewModel
if "val sonarState:" not in content:
    content = content.replace(
        "private val audioTracker = AudioRadarTracker()",
        "private val audioTracker = AudioRadarTracker()\n    val sonarState = audioTracker.currentState"
    )

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)

