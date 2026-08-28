import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("val deviceIdentityEngine = DeviceIdentityEngine(RfRecordingDatabase.getInstance(getApplication()).deviceIdentityDao())", 
"""val evidenceEngine = EvidenceEngine(RfRecordingDatabase.getInstance(getApplication()).evidenceDao())
    val deviceIdentityEngine = DeviceIdentityEngine(RfRecordingDatabase.getInstance(getApplication()).deviceIdentityDao(), evidenceEngine)
    val intelligenceApi = IntelligenceApi(rfSessionEngine, rfEventRecorderEngine, deviceIdentityEngine, rfAnomalyEngine, rfPatternEngine, environmentMappingEngine, rfIntelligenceEngine, rfCrossSessionEngine, evidenceEngine)
""")

content = content.replace("rfIntelligenceEngine.updateGraph()", "rfIntelligenceEngine.updateGraph()\n                    evidenceEngine.addEvidence(emptyList()) // Trigger evidence processing if needed inside engines")

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
