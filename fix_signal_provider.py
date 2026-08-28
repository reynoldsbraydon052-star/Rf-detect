with open('app/src/main/java/com/example/SignalProvider.kt', 'r') as f:
    content = f.read()

target = "AcousticFftMetric(\n                            peakFrequencyHz = baseFreq,\n                            peakMagnitudeDb = baseDb,\n                            isUltrasonicDetected = false,\n                            isCoilWhineDetected = false,\n                            fftMagnitudes = dummyMagnitudes.clone()\n                        )"
replacement = "AcousticFftMetric(\n                            peakFrequencyHz = baseFreq,\n                            peakMagnitudeDb = baseDb,\n                            isUltrasonicDetected = false,\n                            isCoilWhineDetected = false,\n                            fftMagnitudes = dummyMagnitudes.clone(),\n                            provenance = DataProvenance.SIMULATED\n                        )"
content = content.replace(target, replacement)

with open('app/src/main/java/com/example/SignalProvider.kt', 'w') as f:
    f.write(content)
