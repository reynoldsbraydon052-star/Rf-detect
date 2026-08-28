with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

count = 0
for i, line in enumerate(content.split('\n')):
    import re
    line_clean = re.sub(r'".*?"', '', line)
    line_clean = re.sub(r'//.*', '', line_clean)
    count += line_clean.count('{')
    count -= line_clean.count('}')
    
    if 645 <= i + 1 <= 857:
        print(f"Line {i+1}: count={count}  {line}")

