with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

target = "val acousticData: AcousticFrequencyData = AcousticFrequencyData(),"
replacement = "val acousticData: AcousticFrequencyData = AcousticFrequencyData(),\n    val correlationEvents: List<CorrelationEvent> = emptyList(),"

if "val correlationEvents" not in content:
    content = content.replace(target, replacement)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
