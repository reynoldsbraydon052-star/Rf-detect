import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""                val newInterpretation = AiInterpretation(
                    assessment = assessment,
                    confidence = assessment.confidence
                )""",
"""                val newInterpretation = AiInterpretation(
                    assessment = assessment,
                    confidence = assessment.confidence,
                    operatingMode = state.operatingMode
                )"""
)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
