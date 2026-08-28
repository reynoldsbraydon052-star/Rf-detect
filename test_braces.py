with open('app/src/main/java/com/example/GeminiThreatAnalysisService.kt', 'r') as f:
    lines = f.readlines()

count = 0
for i, line in enumerate(lines):
    count += line.count('{') - line.count('}')
    print(f"{i+1}: {count}")

