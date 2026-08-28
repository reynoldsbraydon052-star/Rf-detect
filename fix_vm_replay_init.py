import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "val replayEngine = ReplayEngine(this)",
    "val replayEngine = ReplayEngine(this, rfRecordingRepository, rfSessionEngine)"
)

# wait, rfSessionEngine and rfRecordingRepository might be initialized AFTER replayEngine
# Let's fix the order.
content = content.replace(
    "val replayEngine = ReplayEngine(this, rfRecordingRepository, rfSessionEngine)",
    "" # remove it temporarily
)

# add it after rfSessionEngine
content = content.replace(
    "val rfEventRecorderEngine = RfEventRecorderEngine(rfRecordingRepository, viewModelScope, rfSessionEngine)",
    "val rfEventRecorderEngine = RfEventRecorderEngine(rfRecordingRepository, viewModelScope, rfSessionEngine)\n    val replayEngine = ReplayEngine(this, rfRecordingRepository, rfSessionEngine)"
)


with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)

