import re

with open('app/src/main/java/com/example/MultiSensorCorrelationEngine.kt', 'r') as f:
    content = f.read()

content = content.replace("activeCorrelations.clear()", "lastCorrelatedPairs.clear()\n        historicalPatterns.clear()")

with open('app/src/main/java/com/example/MultiSensorCorrelationEngine.kt', 'w') as f:
    f.write(content)
