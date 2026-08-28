with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""                            viewModelScope.launch(Dispatchers.IO) {
                                bleDatabase.signalFingerprintDao().updateFingerprint(updatedFp.toEntity())
                            }""",
"""                            if (_uiState.value.operatingMode == OperatingMode.LIVE) {
                                viewModelScope.launch(Dispatchers.IO) {
                                    bleDatabase.signalFingerprintDao().updateFingerprint(updatedFp.toEntity())
                                }
                            }"""
)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
