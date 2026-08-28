import re

with open('app/src/main/java/com/example/ExplainableAnomalyEngine.kt', 'r') as f:
    content = f.read()

replacement = """        var score = 0
        var confidence = 1.0f
        val explanations = mutableListOf<AnomalyExplanation>()

        if (blip.provenance == DataProvenance.SIMULATED || blip.provenance == DataProvenance.REPLAY) {
            return AnomalyResult(
                score = 0,
                confidence = 0f,
                category = AnomalyCategory.NORMAL,
                explanations = listOf(AnomalyExplanation("Simulated/Replay data ignored", 0))
            )
        }

        val fp = blip.fingerprintId?.let { fingerprintDb[it] }"""

content = content.replace("        var score = 0\n        var confidence = 1.0f\n        val explanations = mutableListOf<AnomalyExplanation>()\n\n        val fp = blip.fingerprintId?.let { fingerprintDb[it] }", replacement)

with open('app/src/main/java/com/example/ExplainableAnomalyEngine.kt', 'w') as f:
    f.write(content)
