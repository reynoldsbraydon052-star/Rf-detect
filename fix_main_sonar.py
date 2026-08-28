import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Pass sonarState down
content = content.replace(
    "fun SweepRadarScreen(",
    "fun SweepRadarScreen(\n    sonarState: SonarState = SonarState.IDLE,"
)

content = content.replace(
    "RadarTab.SWEEP_RADAR -> SweepRadarScreen(",
    "val sonarState by viewModel.sonarState.collectAsStateWithLifecycle()\n                        RadarTab.SWEEP_RADAR -> SweepRadarScreen(\n                            sonarState = sonarState,"
)

# And PinpointDeviceHUDCard
content = content.replace(
    "fun PinpointDeviceHUDCard(",
    "fun PinpointDeviceHUDCard(\n    sonarState: SonarState = SonarState.IDLE,"
)

content = content.replace(
    "PinpointDeviceHUDCard(\n                        uiState = uiState,",
    "PinpointDeviceHUDCard(\n                        sonarState = sonarState,\n                        uiState = uiState,"
)
content = content.replace(
    "PinpointDeviceHUDCard(\n                    uiState = uiState,",
    "PinpointDeviceHUDCard(\n                    sonarState = sonarState,\n                    uiState = uiState,"
)
content = content.replace(
    "PinpointDeviceHUDCard(\n                        uiState = state,",
    "PinpointDeviceHUDCard(\n                        sonarState = sonarState,\n                        uiState = state,"
)


# Add import
if "import com.example.SonarState" not in content:
    content = content.replace("package com.example", "package com.example\nimport com.example.SonarState")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)

