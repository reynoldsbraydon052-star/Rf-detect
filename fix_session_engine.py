import re

with open('app/src/main/java/com/example/RfInvestigationSessionEngine.kt', 'r') as f:
    content = f.read()

content = content.replace("SessionState.ACTIVE.name", "SessionState.RECORDING.name")
content = content.replace("SessionState.CLOSED.name", "SessionState.COMPLETED.name")

with open('app/src/main/java/com/example/RfInvestigationSessionEngine.kt', 'w') as f:
    f.write(content)

