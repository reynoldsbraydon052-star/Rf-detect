with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

# Only keep pkg = captureEvidencePackage() for runAiInvestigator and askTacticalCopilot
# The others should be snapshot = captureRfSnapshot()

import re

# find runAiDeepAudit which uses captureRfSnapshot
def fix(match):
    return "val snapshot = captureRfSnapshot()\n            val report = geminiThreatService.analyzeRfEnvironment(snapshot)"

content = re.sub(r'val pkg = captureEvidencePackage\(\)\s*val report = geminiThreatService\.analyzeRfEnvironment\(snapshot\)', fix, content)

def fix_audit(match):
    return "val snapshot = captureRfSnapshot()\n            val auditResult = geminiThreatService.performTargetDeepAudit(emitter, snapshot)"

content = re.sub(r'val pkg = captureEvidencePackage\(\)\s*val answer = geminiThreatService\.askTacticalCopilot\(query, pkg, updatedList\)\s*val auditResult = geminiThreatService\.performTargetDeepAudit\(emitter, snapshot\)', fix_audit, content)

def fix_pinpoint(match):
    return "val snapshot = captureRfSnapshot()\n            val result = geminiThreatService.performAi3dPinpoint(blip, snapshot)"

content = re.sub(r'val pkg = captureEvidencePackage\(\)\s*val answer = geminiThreatService\.askTacticalCopilot\(query, pkg, updatedList\)\s*val result = geminiThreatService\.performAi3dPinpoint\(blip, snapshot\)', fix_pinpoint, content)


with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)

