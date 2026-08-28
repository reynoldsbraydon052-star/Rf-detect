import re

with open('app/src/main/java/com/example/RfEventRecorderScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("onExportJson: (Uri) -> Unit,", "onExportJson: (Uri) -> Unit,\n    onExportCsv: (Uri) -> Unit,\n    onExportCaptures: () -> Unit\n) {")

content = content.replace("onExportCsv: (Uri) -> Unit\n) {", "")

new_buttons = """                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportJson,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("JSON")
                        }
                        OutlinedButton(
                            onClick = onExportCsv,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV")
                        }
                        OutlinedButton(
                            onClick = onExportCaptures,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Captures")
                        }
                    }"""

content = re.sub(r'                    Row\(\s*modifier = Modifier\.fillMaxWidth\(\),\s*horizontalArrangement = Arrangement\.spacedBy\(8\.dp\)\s*\).*?JSON Export"\)\s*}\s*OutlinedButton\(\s*onClick = onExportCsv,.*?CSV Export"\)\s*}\s*}', new_buttons, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/RfEventRecorderScreen.kt', 'w') as f:
    f.write(content)
