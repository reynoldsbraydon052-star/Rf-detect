with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

count = 0
for i, line in enumerate(content.split('\n')):
    # remove strings so we don't count braces inside them
    import re
    line_clean = re.sub(r'".*?"', '', line)
    line_clean = re.sub(r'//.*', '', line_clean)
    count += line_clean.count('{')
    count -= line_clean.count('}')
    if "override fun onCleared()" in line:
        print(f"Line {i+1}: count={count}, {line}")
