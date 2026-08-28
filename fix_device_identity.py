import re

with open('app/src/main/java/com/example/DeviceIdentityEngine.kt', 'r') as f:
    content = f.read()

content = content.replace("class DeviceIdentityEngine(private val dao: DeviceIdentityDao)", "class DeviceIdentityEngine(private val dao: DeviceIdentityDao, private val evidenceEngine: EvidenceEngine)")

with open('app/src/main/java/com/example/DeviceIdentityEngine.kt', 'w') as f:
    f.write(content)
