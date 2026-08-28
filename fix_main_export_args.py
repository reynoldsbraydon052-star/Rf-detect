import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("""                        RadarTab.EVENT_RECORDER -> RfEventRecorderScreen(
                            uiState = uiState,
                            recorderEngine = viewModel.rfEventRecorderEngine
                        )""",
"""                        RadarTab.EVENT_RECORDER -> RfEventRecorderScreen(
                            uiState = uiState,
                            recorderEngine = viewModel.rfEventRecorderEngine,
                            onExportJson = { uri -> viewModel.exportRfEventsJson(context, uri) },
                            onExportCsv = { uri -> viewModel.exportRfEventsCsv(context, uri) }
                        )""")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
