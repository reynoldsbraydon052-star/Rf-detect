with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("var selectedTab by remember { mutableStateOf(0) }", "var selectedTab by remember { mutableStateOf(0) }\n    val scope = rememberCoroutineScope()")

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'w') as f:
    f.write(content)
