import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("Icons.Default.FiberManualRecord", "Icons.Default.List")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
