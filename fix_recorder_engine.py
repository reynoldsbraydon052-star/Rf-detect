import re

with open('app/src/main/java/com/example/RfEventRecorderEngine.kt', 'r') as f:
    content = f.read()

# Update constructor
content = content.replace(
    "class RfEventRecorderEngine(\n    private val repository: RfRecordingRepository,\n    private val coroutineScope: CoroutineScope\n)",
    "class RfEventRecorderEngine(\n    private val repository: RfRecordingRepository,\n    private val coroutineScope: CoroutineScope,\n    private val sessionEngine: RfInvestigationSessionEngine\n)"
)

# Update insertion logic to attach sessionId
content = content.replace(
    "val now = System.currentTimeMillis()\n        val entities = blips.map { blip ->",
    "val now = System.currentTimeMillis()\n        val sessionId = sessionEngine.getActiveSessionId() ?: \"\"\n        val entities = blips.map { blip ->"
)

content = content.replace(
    "@PrimaryKey val eventId: String = UUID.randomUUID().toString(),",
    "@PrimaryKey val eventId: String = UUID.randomUUID().toString(),\n                sessionId = sessionId,"
)
# Actually the replacement is in the constructor of RfRecordedEventEntity being called
content = content.replace(
    "timestampMs = now,",
    "sessionId = sessionId,\n                timestampMs = now,"
)

with open('app/src/main/java/com/example/RfEventRecorderEngine.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    vm_content = f.read()

# Move rfSessionEngine up and pass it
vm_content = vm_content.replace(
    "val rfEventRecorderEngine = RfEventRecorderEngine(rfRecordingRepository, viewModelScope)",
    "val rfSessionEngine = RfInvestigationSessionEngine(RfRecordingDatabase.getInstance(getApplication()).rfSessionDao())\n    val rfEventRecorderEngine = RfEventRecorderEngine(rfRecordingRepository, viewModelScope, rfSessionEngine)"
)
vm_content = vm_content.replace(
    "val rfSessionEngine = RfInvestigationSessionEngine(RfRecordingDatabase.getInstance(getApplication()).rfSessionDao())\n    val rfAnomalyEngine",
    "val rfAnomalyEngine"
)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(vm_content)

