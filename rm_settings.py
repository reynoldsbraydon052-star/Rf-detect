with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
in_settings = False
for line in lines:
    if "fun SettingsScreen(" in line and "uiState: SignalRadarUiState" in next_line(lines, line, 1):
        in_settings = True
    if in_settings:
        if line.startswith("}"): # End of function ? Hard to tell. Let's just use line numbers.
            pass

