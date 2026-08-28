import re
with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "Box(modifier = Modifier.weight(1f)) {\n                    when (uiState.selectedTab) {\n                        val sonarState by viewModel.sonarState.collectAsStateWithLifecycle()",
    "val sonarState by viewModel.sonarState.collectAsStateWithLifecycle()\n                Box(modifier = Modifier.weight(1f)) {\n                    when (uiState.selectedTab) {"
)
with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
