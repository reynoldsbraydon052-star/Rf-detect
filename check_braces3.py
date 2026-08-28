with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

count = 0
for i, line in enumerate(content.split('\n')):
    import re
    line_clean = re.sub(r'".*?"', '', line)
    line_clean = re.sub(r'//.*', '', line_clean)
    count += line_clean.count('{')
    count -= line_clean.count('}')
    if count == 1 and line_clean.strip() != "}":
        # we are at class level
        pass
    if count == 2 and line_clean.strip() == "}":
        # ending a function
        pass
    if "fun sendCopilotQuery" in line or "fun sendCopilotQuery" in content.split('\n')[i-1]:
        pass

# Let's just find where count doesn't return to 1 between functions.
# We will print the last function that didn't return to 1.
last_fun = ""
for i, line in enumerate(content.split('\n')):
    import re
    line_clean = re.sub(r'".*?"', '', line)
    line_clean = re.sub(r'//.*', '', line_clean)
    if "fun " in line_clean and count == 1:
        last_fun = line.strip()
    
    count += line_clean.count('{')
    count -= line_clean.count('}')

    if "fun " in line_clean and count > 2:
        print(f"Nested fun? Line {i+1}: {line.strip()} (count before={count})")

