import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("Icons.Default.Public", "Icons.Filled.Public")
if "import androidx.compose.material.icons.filled.Public" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Map", "import androidx.compose.material.icons.filled.Map\nimport androidx.compose.material.icons.filled.Public")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
