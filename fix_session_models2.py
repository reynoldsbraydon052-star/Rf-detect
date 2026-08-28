with open('app/src/main/java/com/example/SessionModels.kt', 'w') as f:
    f.write("""package com.example

enum class OperatingMode {
    LIVE,
    REPLAY,
    SIMULATION
}

data class SessionEvent(
    val timestampMs: Long,
    val type: SessionEventType,
    val blip: RadarBlip? = null,
    val fingerprint: SignalFingerprint? = null,
    val anomaly: AnomalyResult? = null,
    val correlation: CorrelationEvent? = null,
    val bookmarkText: String? = null
)

enum class SessionEventType {
    BLIP,
    FINGERPRINT,
    ANOMALY,
    CORRELATION,
    SPECTRUM,
    BOOKMARK,
    START,
    END
}
""")
