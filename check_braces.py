with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

def check_braces(text):
    count = 0
    lines = text.split('\n')
    for i, line in enumerate(lines):
        count += line.count('{')
        count -= line.count('}')
        if count < 0:
            print(f"Error: Negative brace count at line {i+1}: {line}")
            break
    print(f"Final brace count: {count}")

check_braces(content)
