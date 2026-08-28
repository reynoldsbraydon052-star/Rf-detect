import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# I need to add ENVIRONMENT_MAP to the tabs
tab_icons = """                                RadarTab.HISTORIC_HEATMAP -> Icons.Default.Map
                                RadarTab.ENVIRONMENT_MAP -> Icons.Default.Public"""

content = content.replace("                                RadarTab.HISTORIC_HEATMAP -> Icons.Default.Map", tab_icons)

tab_labels = """                                RadarTab.HISTORIC_HEATMAP -> "Heatmap"
                                RadarTab.ENVIRONMENT_MAP -> "RF Map\""""

content = content.replace("                                RadarTab.HISTORIC_HEATMAP -> \"Heatmap\"", tab_labels)

tab_screens = """                        RadarTab.HISTORIC_HEATMAP -> HistoricHeatmapScreen(
                            uiState = uiState
                        )
                        RadarTab.ENVIRONMENT_MAP -> RfEnvironmentMapScreen(
                            uiState = uiState,
                            mappingEngine = viewModel.environmentMappingEngine
                        )"""

content = content.replace("""                        RadarTab.HISTORIC_HEATMAP -> HistoricHeatmapScreen(
                            uiState = uiState
                        )""", tab_screens)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
