import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()

# We know the file should be perfectly balanced.
# Let's remove the 10 insertions I just made.
with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

text = text.replace("                }\n            }\n        }\n    }\n}\n@Composable", "@Composable")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)

