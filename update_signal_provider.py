with open('app/src/main/java/com/example/SignalProvider.kt', 'r') as f:
    content = f.read()

replacements = [
    ("val carrierFreqMhz: Float\n)", "val carrierFreqMhz: Float,\n    val provenance: DataProvenance = DataProvenance.UNKNOWN\n)"),
    ("val carrierFreqMhz: Float)", "val carrierFreqMhz: Float,\n    val provenance: DataProvenance = DataProvenance.UNKNOWN)"),
    ("val elevationDegrees: Float\n)", "val elevationDegrees: Float,\n    val provenance: DataProvenance = DataProvenance.UNKNOWN\n)"),
    ("val elevationDegrees: Float)", "val elevationDegrees: Float,\n    val provenance: DataProvenance = DataProvenance.UNKNOWN)"),
    ("val fftMagnitudes: FloatArray = FloatArray(64)\n)", "val fftMagnitudes: FloatArray = FloatArray(64),\n    val provenance: DataProvenance = DataProvenance.UNKNOWN\n)"),
    ("val fftMagnitudes: FloatArray = FloatArray(64))", "val fftMagnitudes: FloatArray = FloatArray(64),\n    val provenance: DataProvenance = DataProvenance.UNKNOWN)")
]

for old, new in replacements:
    content = content.replace(old, new)

with open('app/src/main/java/com/example/SignalProvider.kt', 'w') as f:
    f.write(content)
