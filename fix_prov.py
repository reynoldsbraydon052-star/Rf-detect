with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("currentState.isSimulationModeActive", "(obs.any { it.provenance == DataProvenance.SIMULATED })")
content = content.replace("currentState.isReplayModeActive", "(obs.any { it.provenance == DataProvenance.REPLAY })")

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
