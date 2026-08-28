with open('app/src/main/java/com/example/SignalProvider.kt', 'r') as f:
    content = f.read()

content = content.replace("    val provenance: DataProvenance = DataProvenance.UNKNOWN,\n    val signalLevelPercent: Int = 0,\n    val capabilities: String = \"\"", "    val signalLevelPercent: Int = 0,\n    val capabilities: String = \"\",\n    val provenance: DataProvenance = DataProvenance.UNKNOWN")

with open('app/src/main/java/com/example/SignalProvider.kt', 'w') as f:
    f.write(content)
