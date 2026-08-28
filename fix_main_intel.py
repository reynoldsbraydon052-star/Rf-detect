import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# I need to add INTELLIGENCE_DASHBOARD to the tabs
tab_icons = """                                RadarTab.IDENTITY_GRAPH -> Icons.Default.Fingerprint
                                RadarTab.INTELLIGENCE_DASHBOARD -> Icons.Default.Hub"""

content = content.replace("                                RadarTab.IDENTITY_GRAPH -> Icons.Default.Fingerprint", tab_icons)

tab_labels = """                                RadarTab.IDENTITY_GRAPH -> "Identity"
                                RadarTab.INTELLIGENCE_DASHBOARD -> "Intelligence\""""

content = content.replace("                                RadarTab.IDENTITY_GRAPH -> \"Identity\"", tab_labels)

tab_screens = """                        RadarTab.IDENTITY_GRAPH -> DeviceIdentityScreen(
                            uiState = uiState,
                            identityEngine = viewModel.deviceIdentityEngine
                        )
                        RadarTab.INTELLIGENCE_DASHBOARD -> IntelligenceDashboardScreen(
                            uiState = uiState,
                            sessionEngine = viewModel.sessionEngine,
                            anomalyEngine = viewModel.anomalyEngine,
                            patternEngine = viewModel.patternEngine,
                            intelligenceEngine = viewModel.intelligenceEngine
                        )"""

content = content.replace("""                        RadarTab.IDENTITY_GRAPH -> DeviceIdentityScreen(
                            uiState = uiState,
                            identityEngine = viewModel.deviceIdentityEngine
                        )""", tab_screens)

# Add Hub import
if "import androidx.compose.material.icons.filled.Hub" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Fingerprint", "import androidx.compose.material.icons.filled.Fingerprint\nimport androidx.compose.material.icons.filled.Hub")


with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
