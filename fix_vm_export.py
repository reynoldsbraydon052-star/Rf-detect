import re

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "kotlinx.coroutines.flow.firstOrNull(rfSessionEngine.allSessions)",
    "kotlinx.coroutines.flow.firstOrNull(rfSessionEngine.allSessions)"
)
# wait, actually let's use the extension function.
content = content.replace(
    "kotlinx.coroutines.flow.firstOrNull(rfSessionEngine.allSessions)",
    "rfSessionEngine.allSessions.firstOrNull()"
)
content = content.replace(
    "kotlinx.coroutines.flow.firstOrNull(db.rfAnnotationDao().getAnnotationsBySessionId(rfSessionEngine.getActiveSessionId() ?: \"\"))",
    "db.rfAnnotationDao().getAnnotationsBySessionId(rfSessionEngine.getActiveSessionId() ?: \"\").firstOrNull()"
)
content = content.replace(
    "import kotlinx.coroutines.flow.firstOrNull",
    ""
)

# And make sure it is imported
if "import kotlinx.coroutines.flow.firstOrNull" not in content:
    content = content.replace(
        "import kotlinx.coroutines.flow.StateFlow",
        "import kotlinx.coroutines.flow.StateFlow\nimport kotlinx.coroutines.flow.firstOrNull"
    )

with open('app/src/main/java/com/example/SignalRadarViewModel.kt', 'w') as f:
    f.write(content)

