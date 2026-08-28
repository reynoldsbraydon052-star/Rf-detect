with open('app/src/main/java/com/example/SignalFingerprintEntity.kt', 'r') as f:
    content = f.read()

replacement = """

fun SignalFingerprintEntity.toDomainModel(): SignalFingerprint {
    return SignalFingerprint(
        id = this.id,
        signalType = this.signalType,
        frequencyMean = this.frequencyMean,
        bandwidthMean = this.bandwidthMean,
        rssiMean = this.rssiMean,
        timingIntervalMean = this.timingIntervalMean,
        observationCount = this.observationCount,
        firstObservedMs = this.firstObservedMs,
        lastObservedMs = this.lastObservedMs,
        provenance = DataProvenance.valueOf(this.provenance),
        knownMacAddress = this.knownMacAddress
    )
}
"""

if "fun SignalFingerprintEntity.toDomainModel()" not in content:
    content += replacement

with open('app/src/main/java/com/example/SignalFingerprintEntity.kt', 'w') as f:
    f.write(content)
