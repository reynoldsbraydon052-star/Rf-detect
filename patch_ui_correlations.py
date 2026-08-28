import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target_card = """fun SpectrumInterceptCard(
    blip: RadarBlip,
    perimeterThresholdMeters: Float,
    selectedTargetDeviceId: String? = null,
    onSelectTargetDevice: ((String?) -> Unit)? = null,
    onInterrogateGatt: ((RadarBlip) -> Unit)? = null
) {"""

replacement_card = """fun SpectrumInterceptCard(
    blip: RadarBlip,
    perimeterThresholdMeters: Float,
    selectedTargetDeviceId: String? = null,
    onSelectTargetDevice: ((String?) -> Unit)? = null,
    onInterrogateGatt: ((RadarBlip) -> Unit)? = null,
    correlations: List<CorrelationEvent> = emptyList()
) {"""

if "correlations: List<CorrelationEvent> =" not in content:
    content = content.replace(target_card, replacement_card)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
