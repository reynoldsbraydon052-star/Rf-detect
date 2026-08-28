import re
with open('app/src/main/java/com/example/RfTemporalPatternEngine.kt', 'r') as f:
    content = f.read()

# Fix duplicate block at the end
content = re.sub(r'            newPatterns\.forEach \{ patternDao\.insertPattern\(it\) \}\n        \}\n    \}\n            newPatterns\.forEach \{ patternDao\.insertPattern\(it\) \}\n        \}\n    \}\n\}',
    r'            newPatterns.forEach { patternDao.insertPattern(it) }\n        }\n    }\n}', content)

with open('app/src/main/java/com/example/RfTemporalPatternEngine.kt', 'w') as f:
    f.write(content)
