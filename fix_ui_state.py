import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

if "val geminiStatus:" not in content:
    content = content.replace(
        "val selectedTab: RadarTab = RadarTab.SWEEP_RADAR,",
        "val selectedTab: RadarTab = RadarTab.SWEEP_RADAR,\n    val geminiStatus: GeminiStatus = GeminiStatus.READY,"
    )

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
