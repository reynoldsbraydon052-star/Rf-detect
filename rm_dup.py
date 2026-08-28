with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

import re
# Find the index of "@Composable\nfun SettingsScreen(\n    uiState: SignalRadarUiState,\n    onSetScanMode: (ScanMode) -> Unit,"
start_idx = content.find("@Composable\nfun SettingsScreen(\n    uiState: SignalRadarUiState,\n    onSetScanMode: (ScanMode) -> Unit")

if start_idx != -1:
    brace_count = 0
    in_function = False
    end_idx = -1
    for i in range(start_idx, len(content)):
        if content[i] == '{':
            brace_count += 1
            in_function = True
        elif content[i] == '}':
            brace_count -= 1
            if in_function and brace_count == 0:
                end_idx = i + 1
                break
    
    if end_idx != -1:
        new_content = content[:start_idx] + content[end_idx:]
        with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
            f.write(new_content)
        print("Removed duplicate SettingsScreen")
else:
    print("Not found")

