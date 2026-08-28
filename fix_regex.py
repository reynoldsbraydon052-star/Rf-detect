import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

# Replace any sequence of whitespaces + `}` + `@Composable` with `}\n            }\n        }\n    }\n}\n\n@Composable`
# But wait, it's safer to just replace `}@Composable` with `}\n            }\n        }\n    }\n}\n\n@Composable`
text = re.sub(r'(\s*)\}@Composable', r'\1}\n            }\n        }\n    }\n}\n\n@Composable', text)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)
