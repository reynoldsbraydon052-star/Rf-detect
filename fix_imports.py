import re

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'r') as f:
    content = f.read()

# Remove the incorrectly placed imports
content = content.replace("import androidx.compose.animation.AnimatedVisibility\nimport androidx.compose.foundation.clickable\n\n@Composable\nfun AnomaliesTab", "@Composable\nfun AnomaliesTab")

# Add them to the top if not present
if "import androidx.compose.animation.AnimatedVisibility" not in content:
    content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.compose.animation.AnimatedVisibility\nimport androidx.compose.foundation.clickable\n")

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'w') as f:
    f.write(content)

