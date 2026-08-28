import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "val rfSessionEngine = RfInvestigationSessionEngine(RfRecordingDatabase.getInstance(getApplication()).rfSessionDao())",
    "val db = RfRecordingDatabase.getInstance(getApplication())\n    val rfSessionEngine = RfInvestigationSessionEngine(db.rfSessionDao(), db.rfAnnotationDao())"
)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)

