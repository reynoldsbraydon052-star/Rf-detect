import re

with open('app/src/main/java/com/example/RfEventRecorderEngine.kt', 'r') as f:
    content = f.read()

content = content.replace("val totalDbEventCount = repository.eventCount",
"""val totalDbEventCount = repository.eventCount

    fun getRecentEvents() = repository.allEvents""")

with open('app/src/main/java/com/example/RfEventRecorderEngine.kt', 'w') as f:
    f.write(content)
