import re

with open('app/src/main/java/com/example/RfIntelligenceCorrelationEngine.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val raw = a.relatedDeviceIdsJson.removePrefix("[\\"").removeSuffix("\\"]")',
    'val raw = a.relatedDeviceIdsJson.removePrefix("[\\"").removeSuffix("\\"]")'
)

# Actually, the easiest is to just use clean regex
content = re.sub(
    r'val raw = a\.relatedDeviceIdsJson\..*',
    r'val raw = a.relatedDeviceIdsJson.replace("[", "").replace("]", "").replace("\\"", "")',
    content
)

with open('app/src/main/java/com/example/RfIntelligenceCorrelationEngine.kt', 'w') as f:
    f.write(content)
