import re

with open('app/src/main/java/com/example/EnvironmentalBaselineEngine.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""            if (blip.provenance == DataProvenance.SIMULATED || blip.provenance == DataProvenance.REPLAY) {
                state = BaselineState.UNKNOWN // Simulations do not affect baseline
            } else if (fp == null) {""",
"            if (fp == null) {"
)

with open('app/src/main/java/com/example/EnvironmentalBaselineEngine.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ExplainableAnomalyEngine.kt', 'r') as f:
    content = f.read()

content = re.sub(
    r'        // AI Compatibility: Maintain Provenance Intact.*?        var score = 0',
    '        var score = 0',
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/example/ExplainableAnomalyEngine.kt', 'w') as f:
    f.write(content)
