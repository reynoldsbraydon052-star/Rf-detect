import re

with open('app/src/main/java/com/example/RfEventRecorderScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""fun RfEventRecorderScreen(
    uiState: SignalRadarUiState,
    recorderEngine: RfEventRecorderEngine,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit
)""",
"""import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

@Composable
fun RfEventRecorderScreen(
    uiState: SignalRadarUiState,
    recorderEngine: RfEventRecorderEngine,
    onExportJson: (Uri) -> Unit,
    onExportCsv: (Uri) -> Unit
)""")

launchers = """    val recentEvents by recorderEngine.getRecentEvents().collectAsStateWithLifecycle(initialValue = emptyList())

    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { onExportJson(it) }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { onExportCsv(it) }
    }
"""

content = content.replace("    val recentEvents by recorderEngine.getRecentEvents().collectAsStateWithLifecycle(initialValue = emptyList())", launchers)

content = content.replace("onClick = onExportJson,", 
"""onClick = {
                                val sdf = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
                                val timestamp = sdf.format(Date())
                                createJsonLauncher.launch("SignalRadar_Investigation_$timestamp.json")
                            },""")

content = content.replace("onClick = onExportCsv,", 
"""onClick = {
                                val sdf = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
                                val timestamp = sdf.format(Date())
                                createCsvLauncher.launch("SignalRadar_Investigation_$timestamp.csv")
                            },""")

with open('app/src/main/java/com/example/RfEventRecorderScreen.kt', 'w') as f:
    f.write(content)
