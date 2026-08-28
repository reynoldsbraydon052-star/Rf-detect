import re
with open('app/src/main/java/com/example/SimulationEngine.kt', 'r') as f:
    content = f.read()

content = content.replace("package com.example\n\nimport", "package com.example\nimport")
with open('app/src/main/java/com/example/SimulationEngine.kt', 'w') as f:
    f.write(content)

