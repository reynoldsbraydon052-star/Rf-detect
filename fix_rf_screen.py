import re

with open('app/src/main/java/com/example/RfEventRecorderScreen.kt', 'r') as f:
    content = f.read()

# Make sure we don't have unresolved references in RfEventRecorderScreen
# Like FiberManualRecord
if "FiberManualRecord" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.*", "import androidx.compose.material.icons.filled.*\nimport androidx.compose.material.icons.filled.FiberManualRecord")

with open('app/src/main/java/com/example/RfEventRecorderScreen.kt', 'w') as f:
    f.write(content)
