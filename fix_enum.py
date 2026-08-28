with open('app/src/main/java/com/example/EvidenceModels.kt', 'r') as f:
    content = f.read()

content = content.replace("SIMULATED,", "SIMULATED,\n    REPLAY,")

with open('app/src/main/java/com/example/EvidenceModels.kt', 'w') as f:
    f.write(content)
