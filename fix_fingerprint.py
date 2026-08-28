with open('app/src/main/java/com/example/SignalFingerprintEngine.kt', 'r') as f:
    content = f.read()

content = content.replace("}\n    fun processObservation", "    fun processObservation")

with open('app/src/main/java/com/example/SignalFingerprintEngine.kt', 'w') as f:
    f.write(content)
