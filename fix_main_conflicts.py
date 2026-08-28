import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("sessionEngine = viewModel.sessionEngine,", "sessionEngine = viewModel.rfSessionEngine,")
content = content.replace("anomalyEngine = viewModel.anomalyEngine,", "anomalyEngine = viewModel.rfAnomalyEngine,")
content = content.replace("patternEngine = viewModel.patternEngine,", "patternEngine = viewModel.rfPatternEngine,")
content = content.replace("intelligenceEngine = viewModel.intelligenceEngine", "intelligenceEngine = viewModel.rfIntelligenceEngine")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
