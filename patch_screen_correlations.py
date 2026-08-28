with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target1 = """                        SpectrumInterceptCard(
                            blip = blip,
                            perimeterThresholdMeters = uiState.perimeterThresholdMeters,
                            selectedTargetDeviceId = uiState.selectedTargetDeviceId,
                            onSelectTargetDevice = onSelectTargetDevice,
                            onInterrogateGatt = onInterrogateGatt
                        )"""

replacement1 = """                        SpectrumInterceptCard(
                            blip = blip,
                            perimeterThresholdMeters = uiState.perimeterThresholdMeters,
                            selectedTargetDeviceId = uiState.selectedTargetDeviceId,
                            onSelectTargetDevice = onSelectTargetDevice,
                            onInterrogateGatt = onInterrogateGatt,
                            correlations = uiState.correlationEvents.filter { event -> event.observations.any { it.id == blip.id } }
                        )"""

target2 = """                SpectrumInterceptCard(
                    blip = it,
                    perimeterThresholdMeters = uiState.perimeterThresholdMeters,
                    selectedTargetDeviceId = uiState.selectedTargetDeviceId,
                    onSelectTargetDevice = onSelectTargetDevice,
                    onInterrogateGatt = onInterrogateGatt
                )"""

replacement2 = """                SpectrumInterceptCard(
                    blip = it,
                    perimeterThresholdMeters = uiState.perimeterThresholdMeters,
                    selectedTargetDeviceId = uiState.selectedTargetDeviceId,
                    onSelectTargetDevice = onSelectTargetDevice,
                    onInterrogateGatt = onInterrogateGatt,
                    correlations = uiState.correlationEvents.filter { event -> event.observations.any { obs -> obs.id == it.id } }
                )"""

if "uiState.correlationEvents" not in content:
    content = content.replace(target1, replacement1)
    content = content.replace(target2, replacement2)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
