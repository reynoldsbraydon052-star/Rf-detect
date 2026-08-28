with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'r') as f:
    lines = f.readlines()

for i in range(len(lines) - 1, -1, -1):
    if lines[i].strip() == "}":
        # remove the last }
        lines.pop(i)
        break

with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'w') as f:
    f.writelines(lines)
    f.write("}\n")

