import re

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("import androidx.compose.ui.platform.LocalContext\n\n@Composable", "@Composable")

if "import androidx.compose.ui.platform.LocalContext" not in content[:1000]:
    content = content.replace("package com.example", "package com.example\n\nimport androidx.compose.ui.platform.LocalContext")

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'w') as f:
    f.write(content)
