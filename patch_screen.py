import re

with open('app/src/main/java/com/example/AiThreatIntelScreen.kt', 'r') as f:
    content = f.read()

# I want to add UI for investigatorAssessment
assessment_ui = """
        if (investigatorAssessment != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Evidence-Based Assessment", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(investigatorAssessment.assessment, style = MaterialTheme.typography.bodyLarge)
                        
                        if (investigatorAssessment.facts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Facts", style = MaterialTheme.typography.titleMedium, color = Color(0xFF00FF66))
                            investigatorAssessment.facts.forEach { Text("- $it", style = MaterialTheme.typography.bodyMedium) }
                        }
                        
                        if (investigatorAssessment.inferences.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Inferences", style = MaterialTheme.typography.titleMedium, color = Color(0xFF33CCFF))
                            investigatorAssessment.inferences.forEach { Text("- $it", style = MaterialTheme.typography.bodyMedium) }
                        }
                        
                        if (investigatorAssessment.hypotheses.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Hypotheses & Explanations", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFCC00))
                            investigatorAssessment.hypotheses.forEach { Text("- $it", style = MaterialTheme.typography.bodyMedium) }
                            investigatorAssessment.alternativeExplanations.forEach { Text("- $it", style = MaterialTheme.typography.bodyMedium) }
                        }
                        
                        if (investigatorAssessment.unknowns.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Unknowns / Limitations", style = MaterialTheme.typography.titleMedium, color = Color(0xFF999999))
                            investigatorAssessment.unknowns.forEach { Text("- $it", style = MaterialTheme.typography.bodyMedium) }
                        }
                        
                        if (investigatorAssessment.recommendedMeasurements.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Recommended Next Measurements", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                            investigatorAssessment.recommendedMeasurements.forEach { Text("- $it", style = MaterialTheme.typography.bodyMedium) }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Confidence: ${(investigatorAssessment.confidence * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onSaveInterpretation) {
                            Text("Save as AI Interpretation")
                        }
                    }
                }
            }
        }
"""

content = content.replace("item {\n            Card(\n                modifier = Modifier.fillMaxWidth(),\n                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)\n            ) {\n                Column(", assessment_ui + "\n        item {\n            Card(\n                modifier = Modifier.fillMaxWidth(),\n                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)\n            ) {\n                Column(")

with open('app/src/main/java/com/example/AiThreatIntelScreen.kt', 'w') as f:
    f.write(content)

