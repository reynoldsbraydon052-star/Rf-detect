import re

with open('app/src/main/java/com/example/RfEventRecorderEngine.kt', 'r') as f:
    content = f.read()

content = content.replace('?: \\"\\"', '?: ""')

with open('app/src/main/java/com/example/RfEventRecorderEngine.kt', 'w') as f:
    f.write(content)
