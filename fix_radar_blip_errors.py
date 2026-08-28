with open('app/src/main/java/com/example/HardwareSpectrumManager.kt', 'r') as f:
    content = f.read()

# Update RadarBlip creations in HardwareSpectrumManager to explicitly mark them as simulated
replacements = [
    ("val bandLabel: String = \"BLE 6.0 Channel Sounding\"\n            )", "val bandLabel: String = \"BLE 6.0 Channel Sounding\",\n                provenance = DataProvenance.SIMULATED\n            )"),
    ("val bandLabel: String = \"6.0 GHz Wi-Fi 7\"\n            )", "val bandLabel: String = \"6.0 GHz Wi-Fi 7\",\n                provenance = DataProvenance.SIMULATED\n            )"),
    ("val bandLabel: String = \"5G NR Low-Band\"\n            )", "val bandLabel: String = \"5G NR Low-Band\",\n                provenance = DataProvenance.SIMULATED\n            )"),
    ("val bandLabel: String = \"BLE 2.4 GHz\"\n            )", "val bandLabel: String = \"BLE 2.4 GHz\",\n                provenance = DataProvenance.SIMULATED\n            )")
]

for old, new in replacements:
    content = content.replace(old, new)

with open('app/src/main/java/com/example/HardwareSpectrumManager.kt', 'w') as f:
    f.write(content)
