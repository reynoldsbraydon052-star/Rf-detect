import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

# We need to move intelligenceApi down below all the other engines.
# Find where it is currently:
line_to_move = "    val intelligenceApi = IntelligenceApi(rfSessionEngine, rfEventRecorderEngine, deviceIdentityEngine, rfAnomalyEngine, rfPatternEngine, environmentMappingEngine, rfIntelligenceEngine, rfCrossSessionEngine, evidenceEngine)\n"
content = content.replace(line_to_move, "")

# Insert it after rfCrossSessionEngine
target_line = "    val rfCrossSessionEngine = RfCrossSessionAnalysisEngine(RfRecordingDatabase.getInstance(getApplication()).rfSessionDao(), RfRecordingDatabase.getInstance(getApplication()).deviceIdentityDao(), RfRecordingDatabase.getInstance(getApplication()).rfPatternDao())\n"
content = content.replace(target_line, target_line + line_to_move)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)

