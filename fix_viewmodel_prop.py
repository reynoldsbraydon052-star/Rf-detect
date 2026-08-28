import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

prop_decl = """    val rfEventRecorderEngine = RfEventRecorderEngine(rfRecordingRepository, viewModelScope)
    val environmentMappingEngine = RfEnvironmentMappingEngine()"""
content = content.replace("    val rfEventRecorderEngine = RfEventRecorderEngine(rfRecordingRepository, viewModelScope)", prop_decl)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
