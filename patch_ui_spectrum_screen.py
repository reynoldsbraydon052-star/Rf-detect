import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """fun SpectrumAnalyzerScreen(
    uiState: SignalRadarUiState,
    onFilterSelected: (String) -> Unit,
    onSelectTargetDevice: (String?) -> Unit = {},
    onToggleAudioSonar: () -> Unit = {},
    onOpenArCameraForTarget: (String) -> Unit = {},
    onOpenCalibration: () -> Unit = {},
    onInterrogateGatt: ((RadarBlip) -> Unit)? = null,
    windowSizeClass: WindowSizeClass = rememberWindowSizeClass()
) {"""

replacement = """fun SpectrumAnalyzerScreen(
    uiState: SignalRadarUiState,
    onFilterSelected: (String) -> Unit,
    onSelectTargetDevice: (String?) -> Unit = {},
    onToggleAudioSonar: () -> Unit = {},
    onOpenArCameraForTarget: (String) -> Unit = {},
    onOpenCalibration: () -> Unit = {},
    onInterrogateGatt: ((RadarBlip) -> Unit)? = null,
    onToggleLearning: () -> Unit = {},
    onResetBaseline: () -> Unit = {},
    windowSizeClass: WindowSizeClass = rememberWindowSizeClass()
) {"""
content = content.replace(target, replacement)

target2 = """                if (uiState.selectedTargetDeviceId != null) {
                    PinpointDeviceHUDCard("""

replacement2 = """                EnvironmentalBaselineCard(
                    summary = uiState.baselineSummary,
                    onToggleLearning = onToggleLearning,
                    onResetBaseline = onResetBaseline
                )
                if (uiState.selectedTargetDeviceId != null) {
                    PinpointDeviceHUDCard("""
content = content.replace(target2, replacement2)

target3 = """            if (uiState.selectedTargetDeviceId != null) {
                item {
                    PinpointDeviceHUDCard("""
                    
replacement3 = """            item {
                EnvironmentalBaselineCard(
                    summary = uiState.baselineSummary,
                    onToggleLearning = onToggleLearning,
                    onResetBaseline = onResetBaseline
                )
            }
            if (uiState.selectedTargetDeviceId != null) {
                item {
                    PinpointDeviceHUDCard("""
content = content.replace(target3, replacement3)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
