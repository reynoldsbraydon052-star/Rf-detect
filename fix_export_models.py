import re

with open('app/src/main/java/com/example/ExportModels.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "val patterns: List<RfPatternEntity>? = null,",
    "val patterns: List<RfPatternEntity>? = null,\n    val sessions: List<RfSessionEntity>? = null,\n    val annotations: List<RfAnnotationEntity>? = null,"
)

with open('app/src/main/java/com/example/ExportModels.kt', 'w') as f:
    f.write(content)

