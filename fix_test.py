import re

with open('app/src/test/java/com/example/AiInvestigatorTest.kt', 'r') as f:
    content = f.read()

# Replace Double with Float in RadarBlip initialization
content = content.replace("frequencyMhz = 2400f", "frequencyMhz = 2400.0")

with open('app/src/test/java/com/example/AiInvestigatorTest.kt', 'w') as f:
    f.write(content)

