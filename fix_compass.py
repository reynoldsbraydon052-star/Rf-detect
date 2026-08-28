import re

with open('app/src/main/java/com/example/CompassDeviceFinderCard.kt', 'r') as f:
    content = f.read()

# Pass sonarState down
content = content.replace(
    "fun CompassDeviceFinderCard(",
    "fun CompassDeviceFinderCard(\n    sonarState: SonarState = SonarState.IDLE,"
)

# And add the import
if "import com.example.SonarState" not in content:
    content = content.replace("package com.example", "package com.example\nimport com.example.SonarState")

with open('app/src/main/java/com/example/CompassDeviceFinderCard.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    main_content = f.read()

main_content = main_content.replace(
    "CompassDeviceFinderCard(\n        uiState = uiState,",
    "CompassDeviceFinderCard(\n        sonarState = sonarState,\n        uiState = uiState,"
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(main_content)
