import re

with open('app/src/main/java/com/example/RfEventRecorderEngine.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "suspend fun generateJsonExport(uiState: SignalRadarUiState, hypotheses: List<DeviceIdentityHypothesis>, anomalies: List<RfAnomalyEntity>? = null, patterns: List<RfPatternEntity>? = null): String {",
    "suspend fun generateJsonExport(uiState: SignalRadarUiState, hypotheses: List<DeviceIdentityHypothesis>, anomalies: List<RfAnomalyEntity>? = null, patterns: List<RfPatternEntity>? = null, sessions: List<RfSessionEntity>? = null, annotations: List<RfAnnotationEntity>? = null): String {"
)

content = content.replace(
    "anomalies = anomalies,\n            patterns = patterns",
    "anomalies = anomalies,\n            patterns = patterns,\n            sessions = sessions,\n            annotations = annotations"
)

with open('app/src/main/java/com/example/RfEventRecorderEngine.kt', 'w') as f:
    f.write(content)


with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    vm_content = f.read()

vm_content = vm_content.replace(
    "val jsonData = rfEventRecorderEngine.generateJsonExport(_uiState.value, deviceIdentityEngine.hypotheses.value.values.toList(), rfAnomalyEngine.anomalies.value, rfPatternEngine.patterns.value)",
    "val jsonData = rfEventRecorderEngine.generateJsonExport(_uiState.value, deviceIdentityEngine.hypotheses.value.values.toList(), rfAnomalyEngine.anomalies.value, rfPatternEngine.patterns.value, kotlinx.coroutines.flow.firstOrNull(rfSessionEngine.allSessions), kotlinx.coroutines.flow.firstOrNull(db.rfAnnotationDao().getAnnotationsBySessionId(rfSessionEngine.getActiveSessionId() ?: \"\")))"
)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(vm_content)

