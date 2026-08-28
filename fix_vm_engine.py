import re
with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "val rfAnomalyEngine = RfAnomalyCorrelationEngine(RfRecordingDatabase.getInstance(getApplication()).rfAnomalyDao())",
    "val rfAnomalyEngine = RfAnomalyCorrelationEngine(RfRecordingDatabase.getInstance(getApplication()).rfAnomalyDao(), RfRecordingDatabase.getInstance(getApplication()).anomalyCorrelationDao())"
)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)

