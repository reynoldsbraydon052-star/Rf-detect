import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

bad1 = r'val pkg = captureEvidencePackage\(\)\s*val answer = geminiThreatService\.askTacticalCopilot\(query, pkg, updatedList\)\s*val report = geminiThreatService\.analyzeRfEnvironment\(snapshot\)'
good1 = 'val snapshot = captureRfSnapshot()\n            val report = geminiThreatService.analyzeRfEnvironment(snapshot)'
content = re.sub(bad1, good1, content)

bad2 = r'val pkg = captureEvidencePackage\(\)\s*val answer = geminiThreatService\.askTacticalCopilot\(query, pkg, updatedList\)\s*val auditResult = geminiThreatService\.performTargetDeepAudit\(emitter, snapshot\)'
good2 = 'val snapshot = captureRfSnapshot()\n            val auditResult = geminiThreatService.performTargetDeepAudit(emitter, snapshot)'
content = re.sub(bad2, good2, content)

bad3 = r'val pkg = captureEvidencePackage\(\)\s*val answer = geminiThreatService\.askTacticalCopilot\(query, pkg, updatedList\)\s*val result = geminiThreatService\.performAi3dPinpoint\(blip, snapshot\)'
good3 = 'val snapshot = captureRfSnapshot()\n            val result = geminiThreatService.performAi3dPinpoint(blip, snapshot)'
content = re.sub(bad3, good3, content)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)

