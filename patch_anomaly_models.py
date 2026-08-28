import re

with open('app/src/main/java/com/example/AnomalyModels.kt', 'r') as f:
    content = f.read()

content = content.replace("val explanations: List<AnomalyExplanation> = emptyList()", "val explanations: List<AnomalyExplanation> = emptyList(),\n    val previousScore: Int? = null")

with open('app/src/main/java/com/example/AnomalyModels.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ExplainableAnomalyEngine.kt', 'r') as f:
    engine = f.read()

engine = engine.replace("category = category,", "category = category,\n            previousScore = fp?.lastAnomalyScore,")

with open('app/src/main/java/com/example/ExplainableAnomalyEngine.kt', 'w') as f:
    f.write(engine)

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    ui = f.read()

target_ui = """                    Text(text = "Confidence: ${String.format("%.0f", anomaly.confidence * 100)}% | Category: ${anomaly.category.name.replace("_", " ")}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)"""

replacement_ui = """                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Confidence: ${String.format("%.0f", anomaly.confidence * 100)}% | Category: ${anomaly.category.name.replace("_", " ")}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                        if (anomaly.previousScore != null) {
                            Text(text = "Prev: ${anomaly.previousScore}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                        }
                    }"""

if "Prev: " not in ui:
    ui = ui.replace(target_ui, replacement_ui)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(ui)
