import re

with open('app/src/test/java/com/example/EnvironmentalBaselineEngineTest.kt', 'r') as f:
    content = f.read()

content = content.replace('rssiMean = -50.0)', 'rssiMean = -50.0, frequencyMean = 2400.0, bandwidthMean = 20.0, timingIntervalMean = 100.0)')
content = content.replace('rssiMean = -70.0)', 'rssiMean = -70.0, frequencyMean = 2400.0, bandwidthMean = 20.0, timingIntervalMean = 100.0)')

with open('app/src/test/java/com/example/EnvironmentalBaselineEngineTest.kt', 'w') as f:
    f.write(content)
