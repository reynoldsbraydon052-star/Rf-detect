import re

with open('app/src/main/java/com/example/RfRecordingDatabase.kt', 'r') as f:
    content = f.read()

content = content.replace("version = 5", "version = 6")

with open('app/src/main/java/com/example/RfRecordingDatabase.kt', 'w') as f:
    f.write(content)

