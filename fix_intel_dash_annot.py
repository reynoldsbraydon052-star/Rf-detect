import re

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'r') as f:
    content = f.read()

# Add a section in SessionOverviewTab
annot_ui = """
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            Text("Session Annotations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            val annotations by sessionEngine.activeSessionAnnotations.collectAsStateWithLifecycle()
            
            if (annotations.isEmpty()) {
                Text("No annotations added yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                annotations.forEach { annot ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(annot.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                val tf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                                Text(tf.format(java.util.Date(annot.timestampMs)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Text(annot.text, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            
            var showAddAnnotDialog by remember { mutableStateOf(false) }
            if (showAddAnnotDialog) {
                var annotText by remember { mutableStateOf("") }
                var annotCategory by remember { mutableStateOf("NOTE") }
                AlertDialog(
                    onDismissRequest = { showAddAnnotDialog = false },
                    title = { Text("Add Annotation") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = annotText,
                                onValueChange = { annotText = it },
                                label = { Text("Note") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            kotlinx.coroutines.GlobalScope.launch {
                                sessionEngine.addAnnotation(annotText, annotCategory)
                            }
                            showAddAnnotDialog = false
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddAnnotDialog = false }) { Text("Cancel") }
                    }
                )
            }
            
            Button(onClick = { showAddAnnotDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add Annotation")
            }
"""

content = content.replace(
    "Text(\"Export & Sharing\", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)",
    annot_ui + "\n            Text(\"Export & Sharing\", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)"
)

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'w') as f:
    f.write(content)

