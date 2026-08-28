with open('app/src/main/java/com/example/RechartsRfSpectrogramCard.kt', 'r') as f:
    content = f.read()

idx = content.find("private fun getRechartsRfHtml(): String {")
if idx != -1:
    before = content[:idx]
    after = content[idx:]
    after = after.replace("\\${", "${'$'}{")
    content = before + after

with open('app/src/main/java/com/example/RechartsRfSpectrogramCard.kt', 'w') as f:
    f.write(content)

