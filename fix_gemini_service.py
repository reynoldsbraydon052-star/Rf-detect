import re

with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'r') as f:
    content = f.read()

# Fix ThreatCategory parsing
content = content.replace("val threatCategory = try { ThreatCategory.valueOf(em.optString(\"threatCategory\", \"UNKNOWN_ANOMALOUS_NODE\")) } catch (e: Exception) { ThreatCategory.UNKNOWN_ANOMALOUS_NODE }", "")
content = content.replace("threatCategory = em.optString(\"threatCategory\", \"UNKNOWN_ANOMALOUS_NODE\"),", 'threatCategory = try { ThreatCategory.valueOf(em.optString("threatCategory", "UNKNOWN_ANOMALOUS_NODE")) } catch (e: Exception) { ThreatCategory.UNKNOWN_ANOMALOUS_NODE },')

# Fix Countermeasure to TacticalCountermeasure
content = content.replace("val countermeasures = mutableListOf<Countermeasure>()", "val countermeasures = mutableListOf<TacticalCountermeasure>()")
content = content.replace("countermeasures.add(\n                        Countermeasure(", "countermeasures.add(\n                        TacticalCountermeasure(")

with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'w') as f:
    f.write(content)
