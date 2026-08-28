import re

with open('app/src/main/java/com/example/SessionModels.kt', 'r') as f:
    content = f.read()

content = re.sub(r'data class InvestigationSession\(.*?\)', '', content, flags=re.DOTALL)
content = re.sub(r'data class SessionEvent\(.*?\)', '', content, flags=re.DOTALL)
content = re.sub(r'enum class SessionEventType \{.*?\}', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/SessionModels.kt', 'w') as f:
    f.write(content)

