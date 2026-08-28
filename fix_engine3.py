import re

with open('app/src/main/java/com/example/RfIntelligenceCorrelationEngine.kt', 'r') as f:
    content = f.read()

content = content.replace('return@invoke', 'return@withContext')

with open('app/src/main/java/com/example/RfIntelligenceCorrelationEngine.kt', 'w') as f:
    f.write(content)
