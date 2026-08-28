import re

with open('app/src/main/java/com/example/EvidenceDao.kt', 'r') as f:
    content = f.read()

content = content.replace("evidence_items", "rf_evidence")

with open('app/src/main/java/com/example/EvidenceDao.kt', 'w') as f:
    f.write(content)
