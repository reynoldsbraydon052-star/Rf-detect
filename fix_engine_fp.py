import re

with open('app/src/main/java/com/example/DeviceIdentityEngine.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "if (fp != null && hypothesis.associatedMacs.contains(fp.macAddress)) {",
    "if (fp != null && blip.fingerprintId != null) {"
)

with open('app/src/main/java/com/example/DeviceIdentityEngine.kt', 'w') as f:
    f.write(content)
