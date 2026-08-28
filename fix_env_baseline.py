import re

with open('app/src/main/java/com/example/EnvironmentalBaselineEngine.kt', 'r') as f:
    content = f.read()

replacement = """            var state = BaselineState.UNKNOWN
            
            if (blip.provenance == DataProvenance.SIMULATED || blip.provenance == DataProvenance.REPLAY) {
                // Ignore simulated/replay data for baseline calculations
                return@map blip.copy(baselineState = BaselineState.UNKNOWN)
            }
            
            val fp = blip.fingerprintId?.let { fingerprintDb[it] }"""

content = content.replace("            var state = BaselineState.UNKNOWN\n            val fp = blip.fingerprintId?.let { fingerprintDb[it] }", replacement)

with open('app/src/main/java/com/example/EnvironmentalBaselineEngine.kt', 'w') as f:
    f.write(content)
