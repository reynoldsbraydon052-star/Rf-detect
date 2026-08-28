with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

target = "val magnetometerData: MagnetometerData = MagnetometerData(),"
if "val baselineSummary: BaselineSummary = BaselineSummary()," not in content:
    replacement = target + "\n    val baselineSummary: BaselineSummary = BaselineSummary(),"
    content = content.replace(target, replacement)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
