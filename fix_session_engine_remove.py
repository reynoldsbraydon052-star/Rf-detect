import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

# Replace the block
to_remove = """        if (_uiState.value.operatingMode == OperatingMode.LIVE) {
            sessionEngine.recordEvent(SessionEvent(
                timestampMs = System.currentTimeMillis(),
                type = SessionEventType.BLIP,
                blip = rawBlip
            ))
        }"""
content = content.replace(to_remove, "")

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)

