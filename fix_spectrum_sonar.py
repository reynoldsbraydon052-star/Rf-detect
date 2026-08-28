import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "fun SpectrumAnalyzerScreen(",
    "fun SpectrumAnalyzerScreen(\n    sonarState: SonarState = SonarState.IDLE,"
)

content = content.replace(
    "RadarTab.SPECTRUM_ANALYZER -> SpectrumAnalyzerScreen(",
    "RadarTab.SPECTRUM_ANALYZER -> SpectrumAnalyzerScreen(\n                            sonarState = sonarState,"
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
