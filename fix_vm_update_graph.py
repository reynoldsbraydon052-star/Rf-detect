import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("rfIntelligenceEngine.updateGraph()", "viewModelScope.launch { rfIntelligenceEngine.updateGraph() }")

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
