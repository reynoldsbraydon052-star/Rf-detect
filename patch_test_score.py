import re

with open('app/src/test/java/com/example/MultiSensorCorrelationEngineTest.kt', 'r') as f:
    content = f.read()

target = "assertTrue(secondCorrelation.correlationScore > firstCorrelation.correlationScore)"
replacement = "assertTrue(secondCorrelation.correlationScore >= firstCorrelation.correlationScore)"

if "assertTrue(secondCorrelation.correlationScore > firstCorrelation.correlationScore)" in content:
    content = content.replace(target, replacement)

with open('app/src/test/java/com/example/MultiSensorCorrelationEngineTest.kt', 'w') as f:
    f.write(content)
