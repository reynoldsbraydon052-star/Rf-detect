import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                }
            }
        }
    }
}

@Composable"""

replacement = """                }
            }
            if (isSelectedTarget && correlations.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).background(Color(0xFF111A22), RoundedCornerShape(6.dp)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "CORRELATED EVENTS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = Color(0xFF00E5FF))
                    correlations.forEach { event ->
                        val otherObs = event.observations.firstOrNull { it.id != blip.id }
                        if (otherObs != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(text = otherObs.type, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 10.sp), color = Color.White)
                                    Text(text = "Δt: ${event.maxTimeSeparationMs}ms", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp), color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Score: ${String.format("%.2f", event.correlationScore)}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp), color = if (event.correlationScore > 0.7f) Color(0xFF00FF66) else Color(0xFFFFCC00))
                                    Text(text = event.spatialRelationship.name, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp), color = Color.Gray)
                                }
                            }
                            Text(text = event.notes, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp), color = Color.DarkGray)
                            androidx.compose.material3.HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable"""

if "CORRELATED EVENTS" not in content:
    content = content.replace(target, replacement)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
