import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("private val bleScannerService = BleScannerServiceEngine(application, bleRepository)", 
"""private val bleScannerService = BleScannerServiceEngine(application, bleRepository)
    private val rfRecordingDatabase = RfRecordingDatabase.getInstance(application)
    private val rfRecordingRepository = RfRecordingRepository(rfRecordingDatabase.rfRecordedEventDao())
    val rfEventRecorderEngine = RfEventRecorderEngine(rfRecordingRepository, viewModelScope)""")

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
