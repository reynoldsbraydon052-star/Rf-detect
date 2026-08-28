import re

with open('app/src/main/java/com/example/AudioRadarTracker.kt', 'r') as f:
    content = f.read()

# Replace currentState with a MutableStateFlow
content = content.replace(
    "private var currentState = SonarState.IDLE",
    "private val _currentState = MutableStateFlow(SonarState.IDLE)\n    val currentState: StateFlow<SonarState> = _currentState.asStateFlow()"
)
content = content.replace("currentState = SonarState", "_currentState.value = SonarState")
content = content.replace("currentState !=", "_currentState.value !=")
content = content.replace("fun getCurrentState(): SonarState = currentState", "")

if "import kotlinx.coroutines.flow.MutableStateFlow" not in content:
    content = content.replace(
        "import kotlinx.coroutines.*",
        "import kotlinx.coroutines.*\nimport kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.StateFlow\nimport kotlinx.coroutines.flow.asStateFlow"
    )

with open('app/src/main/java/com/example/AudioRadarTracker.kt', 'w') as f:
    f.write(content)

