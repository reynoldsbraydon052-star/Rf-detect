with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("cachedFingerprints[fpId] = updatedFp // Update local cache", "cachedFingerprints = cachedFingerprints + (fpId to updatedFp) // Update local cache")

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)
