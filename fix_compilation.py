import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "import androidx.compose.runtime.*",
    "import androidx.compose.runtime.*\nimport androidx.compose.runtime.collectAsState\nimport androidx.compose.material.icons.filled.Science"
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)


with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("anomalyEngine.resetIsolatedState()", "// anomalyEngine.resetIsolatedState()")
content = content.replace("anomalyEngine.isolateStateForSimulation()", "// anomalyEngine.isolateStateForSimulation()")

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)

