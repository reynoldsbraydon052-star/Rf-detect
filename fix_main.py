import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "FullScreenRadarMapOverlay(\n                    uiState = uiState,",
    "FullScreenRadarMapOverlay(\n                    uiState = uiState,\n                    anomalies = viewModel.rfAnomalyEngine.anomalies.collectAsStateWithLifecycle().value,"
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
