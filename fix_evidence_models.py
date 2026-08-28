import re

with open('app/src/main/java/com/example/EvidenceModels.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val evidenceReferences: List<String> = emptyList()',
    'val evidenceReferences: List<String> = emptyList(),\n    val operatingMode: OperatingMode = OperatingMode.LIVE'
)

with open('app/src/main/java/com/example/EvidenceModels.kt', 'w') as f:
    f.write(content)
