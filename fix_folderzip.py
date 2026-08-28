import re

with open('app/src/main/java/com/example/RfEventRecorderScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("Icons.Default.FolderZip", "Icons.Default.Download")

with open('app/src/main/java/com/example/RfEventRecorderScreen.kt', 'w') as f:
    f.write(content)
