with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("val baseline = baselineEngine.baselineState.value", "val baseline = currentState.baselineSummary")
content = content.replace("val baselineSummary = \"Environment Baseline: known MACs=${baseline.knownMacAddresses.size}, normal freq=${baseline.averageUltrasonicFreqHz}Hz\"", "val baselineSummary = \"Environment Baseline: ${baseline.summaryText}\"")

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
