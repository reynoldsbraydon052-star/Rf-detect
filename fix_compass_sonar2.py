import re

with open('app/src/main/java/com/example/CompassDeviceFinderCard.kt', 'r') as f:
    content = f.read()

sonar_ui = """
                if (uiState.isAudioSonarActive && sonarState != SonarState.IDLE && sonarState != SonarState.UNAVAILABLE) {
                    val sonarLabel = when(sonarState) {
                        SonarState.FAR -> "FAR"
                        SonarState.APPROACHING -> "APPROACHING"
                        SonarState.CLOSE -> "CLOSE"
                        SonarState.TARGET_REACHED -> "TARGET REACHED"
                        else -> ""
                    }
                    val sonarDots = when(sonarState) {
                        SonarState.FAR -> "● ○ ○ ○"
                        SonarState.APPROACHING -> "● ● ○ ○"
                        SonarState.CLOSE -> "● ● ● ○"
                        SonarState.TARGET_REACHED -> "● ● ● ●"
                        else -> ""
                    }
                    val sonarColor = if (sonarState == SonarState.TARGET_REACHED) Color(0xFF00FF66) else Color(0xFFFFCC00)
                    
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = sonarColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, sonarColor.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "SONAR ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                                    color = sonarColor.copy(alpha = 0.8f)
                                )
                                Text(
                                    sonarLabel,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                    color = sonarColor
                                )
                            }
                            Text(
                                sonarDots,
                                style = MaterialTheme.typography.titleMedium,
                                color = sonarColor
                            )
                        }
                    }
                }
"""

content = content.replace(
    "                        )\n                    }\n                }\n            }\n        }\n    }\n}\n\n@Composable\nprivate fun TextTextButton(",
    "                        )\n                    }\n                }\n" + sonar_ui + "\n            }\n        }\n    }\n}\n\n@Composable\nprivate fun TextTextButton("
)

with open('app/src/main/java/com/example/CompassDeviceFinderCard.kt', 'w') as f:
    f.write(content)
