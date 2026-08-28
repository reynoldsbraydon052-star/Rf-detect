import re

with open('app/src/main/java/com/example/RfRecordedEventEntity.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "@PrimaryKey val eventId: String = UUID.randomUUID().toString(),",
    "@PrimaryKey val eventId: String = UUID.randomUUID().toString(),\n    val sessionId: String = \"\", // Added for Feature 25"
)

with open('app/src/main/java/com/example/RfRecordedEventEntity.kt', 'w') as f:
    f.write(content)

