import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "val jsonData = rfEventRecorderEngine.generateJsonExport(_uiState.value)",
    "val jsonData = rfEventRecorderEngine.generateJsonExport(_uiState.value, deviceIdentityEngine.hypotheses.value.values.toList())"
)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
