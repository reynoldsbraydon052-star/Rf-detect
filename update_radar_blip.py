with open('app/src/main/java/com/example/RadarBlip.kt', 'r') as f:
    content = f.read()

target = 'val csRangingMethod: String = "RSSI"'
if target in content and "val provenance: DataProvenance" not in content:
    content = content.replace(target, target + ",\n    val provenance: DataProvenance = DataProvenance.UNKNOWN,\n    val localizationConfidence: LocalizationConfidence = LocalizationConfidence.NONE")
    
with open('app/src/main/java/com/example/RadarBlip.kt', 'w') as f:
    f.write(content)
