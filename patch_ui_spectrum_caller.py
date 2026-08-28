import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                    SpectrumAnalyzerScreen(
                        uiState = uiState,
                        onFilterSelected = { filter -> onFilterSelected(filter) },
                        onSelectTargetDevice = { id -> onSelectTargetDevice(id) },
                        onToggleAudioSonar = onToggleAudioSonar,
                        onOpenArCameraForTarget = onOpenArCameraForTarget,
                        onOpenCalibration = onOpenCalibration,
                        onInterrogateGatt = onInterrogateGatt,
                        windowSizeClass = windowSizeClass
                    )"""

replacement = """                    SpectrumAnalyzerScreen(
                        uiState = uiState,
                        onFilterSelected = { filter -> onFilterSelected(filter) },
                        onSelectTargetDevice = { id -> onSelectTargetDevice(id) },
                        onToggleAudioSonar = onToggleAudioSonar,
                        onOpenArCameraForTarget = onOpenArCameraForTarget,
                        onOpenCalibration = onOpenCalibration,
                        onInterrogateGatt = onInterrogateGatt,
                        onToggleLearning = { viewModel.toggleBaselineLearning() },
                        onResetBaseline = { viewModel.resetBaseline() },
                        windowSizeClass = windowSizeClass
                    )"""
                    
content = content.replace(target, replacement)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
