import re

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'r') as f:
    content = f.read()

# Pass viewModel to SessionOverviewTab
content = content.replace("0 -> SessionOverviewTab(sessionEngine)", "0 -> SessionOverviewTab(sessionEngine, viewModel)")

# Update SessionOverviewTab signature and content
old_tab = """@Composable
fun SessionOverviewTab(sessionEngine: RfInvestigationSessionEngine) {"""

new_tab = """import androidx.compose.ui.platform.LocalContext

@Composable
fun SessionOverviewTab(sessionEngine: RfInvestigationSessionEngine, viewModel: SignalRadarViewModel) {"""

content = content.replace(old_tab, new_tab)

old_end = """                Column(horizontalAlignment = Alignment.End) {
                    Text("Events", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${session.eventCount}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}"""

new_end = """                Column(horizontalAlignment = Alignment.End) {
                    Text("Events", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${session.eventCount}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            Text("Export & Sharing", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            val context = LocalContext.current
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.exportRfEventsJson(context, android.net.Uri.EMPTY) }, modifier = Modifier.weight(1f)) {
                    Text("Export JSON")
                }
                Button(onClick = { viewModel.exportRfEventsCsv(context, android.net.Uri.EMPTY) }, modifier = Modifier.weight(1f)) {
                    Text("Export CSV")
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { 
                    kotlinx.coroutines.GlobalScope.launch {
                        if (session.state == "ACTIVE") {
                            sessionEngine.pauseSession()
                        } else {
                            sessionEngine.resumeSession(session.id)
                        }
                    }
                }) {
                    Text(if (session.state == "ACTIVE") "Pause Session" else "Resume Session")
                }
                TextButton(onClick = { 
                    kotlinx.coroutines.GlobalScope.launch {
                        sessionEngine.closeSession()
                    }
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Close Session")
                }
            }
        }
    }
}"""

content = content.replace(old_end, new_end)

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'w') as f:
    f.write(content)
