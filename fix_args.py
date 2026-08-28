import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

# fix hardwareCaps
content = content.replace('if (hardwareSpectrumManager.isSdrConnected.value) hardwareCaps.add("USB SDR attached (24MHz-1766MHz)")', '')
content = content.replace('if (bleScannerService.isScanning.value) hardwareCaps.add("BLE Scanner")', 'hardwareCaps.add("BLE Scanner")')

# fix baselineSummary
content = content.replace('val baselineSummary = "Environment Baseline: ${baseline.summaryText}"', 'val baselineSummary = "Environment Baseline: ${baseline.knownFingerprints} known, ${baseline.newFingerprints} new"')

# fix correlations
content = content.replace('.map { "Correlation: ${it.description} (Score: ${it.correlationScore}, Confidence: ${it.confidenceScore})" }', '.map { "Correlation: ${it.notes} (Score: ${it.correlationScore}, Confidence: ${it.confidence})" }')

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
