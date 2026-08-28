import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

# in init block:
init_code = """
        viewModelScope.launch {
            geminiThreatService.geminiStatus.collect { status ->
                _uiState.update { it.copy(geminiStatus = status) }
            }
        }
"""

content = content.replace(
    "    init {\n",
    "    init {\n" + init_code
)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
