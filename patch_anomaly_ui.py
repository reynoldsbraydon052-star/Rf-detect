import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                }
            }
        }
    }
}

@Composable
fun SpectrumAnalyzerScreen("""

replacement = """                }
            }
            if (isSelectedTarget && blip.anomalyResult != null) {
                val anomaly = blip.anomalyResult
                val categoryColor = when (anomaly.category) {
                    AnomalyCategory.HIGH_DEVIATION -> Color(0xFFFF3366)
                    AnomalyCategory.MODERATE_DEVIATION -> Color(0xFFFF8800)
                    AnomalyCategory.LOW_DEVIATION -> Color(0xFFFFCC00)
                    AnomalyCategory.NORMAL -> Color(0xFF00FF66)
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "ANOMALY EXPLANATION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "${anomaly.score} / 100", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = categoryColor)
                    }
                    Text(text = "Confidence: ${String.format("%.0f", anomaly.confidence * 100)}% | Category: ${anomaly.category.name.replace("_", " ")}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                    
                    if (anomaly.explanations.isNotEmpty()) {
                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                        anomaly.explanations.forEach { exp ->
                            val sign = if (exp.scoreImpact > 0) "+" else ""
                            val impactColor = if (exp.scoreImpact > 0) Color(0xFFFF8800) else if (exp.scoreImpact < 0) Color(0xFF00FF66) else Color.Gray
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = exp.description, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "$sign${exp.scoreImpact}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp), color = impactColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpectrumAnalyzerScreen("""

if "ANOMALY EXPLANATION" not in content:
    content = content.replace(target, replacement)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
