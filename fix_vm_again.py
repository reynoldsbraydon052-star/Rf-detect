import re
with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()
content = content.replace("val sessionEngine = InvestigationSessionEngine()", "")
with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
