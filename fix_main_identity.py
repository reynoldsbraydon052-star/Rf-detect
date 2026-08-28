import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# I need to add IDENTITY_GRAPH to the tabs
tab_icons = """                                RadarTab.ENVIRONMENT_MAP -> Icons.Filled.Public
                                RadarTab.IDENTITY_GRAPH -> Icons.Default.Fingerprint"""

content = content.replace("                                RadarTab.ENVIRONMENT_MAP -> Icons.Filled.Public", tab_icons)

tab_labels = """                                RadarTab.ENVIRONMENT_MAP -> "RF Map"
                                RadarTab.IDENTITY_GRAPH -> "Identity\""""

content = content.replace("                                RadarTab.ENVIRONMENT_MAP -> \"RF Map\"", tab_labels)

tab_screens = """                        RadarTab.ENVIRONMENT_MAP -> RfEnvironmentMapScreen(
                            uiState = uiState,
                            mappingEngine = viewModel.environmentMappingEngine
                        )
                        RadarTab.IDENTITY_GRAPH -> DeviceIdentityScreen(
                            uiState = uiState,
                            identityEngine = viewModel.deviceIdentityEngine
                        )"""

content = content.replace("""                        RadarTab.ENVIRONMENT_MAP -> RfEnvironmentMapScreen(
                            uiState = uiState,
                            mappingEngine = viewModel.environmentMappingEngine
                        )""", tab_screens)

# Add Fingerprint import
if "import androidx.compose.material.icons.filled.Fingerprint" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Public", "import androidx.compose.material.icons.filled.Public\nimport androidx.compose.material.icons.filled.Fingerprint")


with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
