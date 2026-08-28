import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "val savedSessions by viewModel.sessionEngine.savedSessions.collectAsStateWithLifecycle()",
    "val savedSessions by viewModel.rfSessionEngine.allSessions.collectAsStateWithLifecycle(initialValue = emptyList())"
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)

