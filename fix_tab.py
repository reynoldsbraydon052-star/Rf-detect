with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("enum class RadarTab {\n    SWEEP_RADAR,", "enum class RadarTab {\n    SWEEP_RADAR,\n    SIMULATION_LAB,")

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
