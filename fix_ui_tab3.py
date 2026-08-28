import re

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'r') as f:
    content = f.read()

# Replace "${dr.confidenceScore}% Confidence" with string based on confidence
content = content.replace('Text("${dr.confidenceScore}% Confidence", color = MaterialTheme.colorScheme.primary)', 
"""
                            val drConfText = when {
                                dr.confidenceScore >= 90 -> "Strong correlation"
                                dr.confidenceScore >= 70 -> "Probable"
                                dr.confidenceScore >= 40 -> "Likely"
                                dr.confidenceScore >= 20 -> "Possible"
                                else -> "Weak correlation"
                            }
                            Text(drConfText, color = MaterialTheme.colorScheme.primary)
""")

content = content.replace('Text("${pr.confidenceScore}% Confidence", color = MaterialTheme.colorScheme.primary)', 
"""
                            val prConfText = when {
                                pr.confidenceScore >= 90 -> "Strong correlation"
                                pr.confidenceScore >= 70 -> "Probable"
                                pr.confidenceScore >= 40 -> "Likely"
                                pr.confidenceScore >= 20 -> "Possible"
                                else -> "Weak correlation"
                            }
                            Text(prConfText, color = MaterialTheme.colorScheme.primary)
""")

with open('app/src/main/java/com/example/IntelligenceDashboardScreen.kt', 'w') as f:
    f.write(content)
