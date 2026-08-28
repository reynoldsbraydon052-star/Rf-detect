import re
with open('app/src/main/java/com/example/RfAnomalyCorrelationEngine.kt', 'r') as f:
    content = f.read()

# Replace invalid strings
content = content.replace('relatedAnomalyIdsJson = "["" + ids.joinToString("","") + ""]",', 'relatedAnomalyIdsJson = "[\\"" + ids.joinToString("\\",\\"") + "\\"]",')
content = content.replace('supportingEvidenceJson = "["Multiple anomalies detected in tight time window"]",', 'supportingEvidenceJson = "[\\"Multiple anomalies detected in tight time window\\"]",')
content = content.replace('sourceEventIdsJson = "["" + sourceEvents.joinToString("","") + ""]",', 'sourceEventIdsJson = "[\\"" + sourceEvents.joinToString("\\",\\"") + "\\"]",')
content = content.replace('supportingEvidenceJson = "["" + supporting.joinToString("","") + ""]",', 'supportingEvidenceJson = "[\\"" + supporting.joinToString("\\",\\"") + "\\"]",')
content = content.replace('contradictingEvidenceJson = "["" + contradicting.joinToString("","") + ""]",', 'contradictingEvidenceJson = "[\\"" + contradicting.joinToString("\\",\\"") + "\\"]",')

with open('app/src/main/java/com/example/RfAnomalyCorrelationEngine.kt', 'w') as f:
    f.write(content)
