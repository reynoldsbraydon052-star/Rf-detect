import re

with open('app/src/main/java/com/example/RfEventRecorderScreen.kt', 'r') as f:
    content = f.read()

# Fix definition
content = content.replace("fun RfEventRecorderScreen(\n    uiState: SignalRadarUiState,\n    recorderEngine: RfEventRecorderEngine\n)", 
"""fun RfEventRecorderScreen(
    uiState: SignalRadarUiState,
    recorderEngine: RfEventRecorderEngine,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit
)""")

# Add buttons right above Clear All Events
export_buttons = """
                if (totalDbCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportJson,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("JSON Export")
                        }
                        OutlinedButton(
                            onClick = onExportCsv,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV Export")
                        }
                    }
                    
                    OutlinedButton("""

content = content.replace("                if (totalDbCount > 0) {\n                    OutlinedButton(", export_buttons)

with open('app/src/main/java/com/example/RfEventRecorderScreen.kt', 'w') as f:
    f.write(content)
