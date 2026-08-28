import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "val deviceIdentityEngine = DeviceIdentityEngine(RfRecordingDatabase.getInstance(getApplication()).deviceIdentityDao())",
    "val evidenceEngine = EvidenceEngine(RfRecordingDatabase.getInstance(getApplication()).evidenceDao())\n    val deviceIdentityEngine = DeviceIdentityEngine(RfRecordingDatabase.getInstance(getApplication()).deviceIdentityDao(), evidenceEngine)"
)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)

