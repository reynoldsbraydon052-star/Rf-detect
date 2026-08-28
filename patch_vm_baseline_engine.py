import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

target = "private val fingerprintEngine = SignalFingerprintEngine(bleDatabase.signalFingerprintDao())"
if "private val baselineEngine =" not in content:
    replacement = target + "\n    private val baselineEngine = EnvironmentalBaselineEngine()"
    content = content.replace(target, replacement)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
