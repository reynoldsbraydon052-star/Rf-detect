import re

with open('app/src/main/java/com/example/RfAnomalyCorrelationEngine.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "for (blip in events) {",
    "for (blip in events) {\n            if (blip.provenance != DataProvenance.MEASURED) continue\n"
)

with open('app/src/main/java/com/example/RfAnomalyCorrelationEngine.kt', 'w') as f:
    f.write(content)
