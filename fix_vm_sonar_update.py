import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

# find _uiState.update { state -> state.copy(activeBlips = evaluatedBlips, ... ) }
# and inject updateSonarTarget() right after it.

old_block = """            _uiState.update { state ->
                state.copy(
                    activeBlips = evaluatedBlips,
                    nearestBlip = nearest,
                    perimeterBreachCount = breaches,
                    baselineSummary = summary
                )
            }"""

new_block = """            _uiState.update { state ->
                state.copy(
                    activeBlips = evaluatedBlips,
                    nearestBlip = nearest,
                    perimeterBreachCount = breaches,
                    baselineSummary = summary
                )
            }
            updateSonarTarget()"""

content = content.replace(old_block, new_block)

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
