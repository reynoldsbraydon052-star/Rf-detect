package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalAiModelScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val modelManager = remember { ModelManager(context) }

    // Track active states of each model dynamically
    var modelsList by remember {
        mutableStateOf(ModelCatalog.MODELS.map { modelManager.getModelStatus(it) })
    }
    
    var selectedActiveModel by remember {
        mutableStateOf(modelManager.getActiveModel())
    }

    var diagnosticOutput by remember { mutableStateOf("") }
    var userQueryText by remember { mutableStateOf("") }
    var isQuerying by remember { mutableStateOf(false) }

    // Periodically update progress of downloading models
    LaunchedEffect(Unit) {
        while (true) {
            modelsList = ModelCatalog.MODELS.map { modelManager.getModelStatus(it) }
            selectedActiveModel = modelManager.getActiveModel()
            delay(1000)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "LOCAL AI SUBSYSTEM",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF00FF66)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_local_ai_model_screen")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF00FF66)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF06140A),
                    titleContentColor = Color(0xFF00FF66)
                )
            )
        },
        containerColor = Color(0xFF06140A)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Warning Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF221111)),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("storage_warning_card")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "STORAGE SPACE WARNING",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Red
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "GGUF weights consume substantial storage (1GB - 3GB+). Ensure you are connected to unmetered Wi-Fi before initiating downloads.",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }

            // Model List Section
            item {
                Text(
                    text = "AVAILABLE LOCAL GGUF MODELS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }

            items(modelsList) { modelInfo ->
                val catalogModel = remember { ModelCatalog.MODELS.first { it.id == modelInfo.id } }
                val isSelected = selectedActiveModel?.id == modelInfo.id

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF0E2816) else Color(0xFF0C1F13)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) Color(0xFF00FF66) else Color(0xFF00FF66).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("model_card_${modelInfo.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = modelInfo.displayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = modelInfo.repository,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = modelInfo.sizeEstimate,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF00FF66)
                            )
                        }

                        // Progress bar for downloading
                        if (modelInfo.status == DownloadStatus.DOWNLOADING || modelInfo.status == DownloadStatus.QUEUED) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (modelInfo.status == DownloadStatus.QUEUED) "Queued..." else "Downloading...",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Yellow
                                    )
                                    Text(
                                        text = "${(modelInfo.progress * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Yellow
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { modelInfo.progress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = Color(0xFF00FF66),
                                    trackColor = Color.Black.copy(alpha = 0.5f),
                                )
                            }
                        }

                        // Action Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (modelInfo.status) {
                                DownloadStatus.NOT_INSTALLED, DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                                    Button(
                                        onClick = {
                                            modelManager.cancelDownload(catalogModel)
                                            modelManager.startDownload(catalogModel)
                                            modelsList = ModelCatalog.MODELS.map { modelManager.getModelStatus(it) }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f).testTag("download_btn_${modelInfo.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("DOWNLOAD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                                DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> {
                                    OutlinedButton(
                                        onClick = {
                                            modelManager.cancelDownload(catalogModel)
                                            modelsList = ModelCatalog.MODELS.map { modelManager.getModelStatus(it) }
                                        },
                                        border = BorderStroke(1.dp, Color.Red),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f).testTag("cancel_btn_${modelInfo.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = Color.Red,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("CANCEL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                                    }
                                }
                                DownloadStatus.AVAILABLE -> {
                                    // Use model selection
                                    Button(
                                        onClick = {
                                            modelManager.setActiveModel(if (isSelected) null else catalogModel)
                                            selectedActiveModel = modelManager.getActiveModel()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) Color(0xFF00FF66) else Color.Black
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFF00FF66)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1.5f).testTag("select_btn_${modelInfo.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.Black else Color(0xFF00FF66),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isSelected) "ACTIVE SYSTEM MODEL" else "SELECT MODEL",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else Color(0xFF00FF66)
                                        )
                                    }

                                    // Deletion trigger
                                    IconButton(
                                        onClick = {
                                            modelManager.deleteModel(catalogModel)
                                            if (isSelected) {
                                                modelManager.setActiveModel(null)
                                                selectedActiveModel = null
                                            }
                                            modelsList = ModelCatalog.MODELS.map { modelManager.getModelStatus(it) }
                                        },
                                        modifier = Modifier.testTag("delete_btn_${modelInfo.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Local RAG Memory Diagnostics
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "LOCAL RAG CO-PILOT DIAGNOSTICS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F13)),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("rag_diagnostics_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Interact with your local memory store offline. Enter a scan keyword below to execute a similarity vector search on historical RF events.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )

                        OutlinedTextField(
                            value = userQueryText,
                            onValueChange = { userQueryText = it },
                            modifier = Modifier.fillMaxWidth().testTag("rag_test_query_input"),
                            label = {
                                Text(
                                    "Enter search query...",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00FF66),
                                unfocusedBorderColor = Color(0xFF00FF66).copy(alpha = 0.5f),
                                cursorColor = Color(0xFF00FF66)
                            ),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (userQueryText.isNotBlank()) {
                                    isQuerying = true
                                    diagnosticOutput = "Analyzing RAG index..."
                                    scope.launch {
                                        try {
                                            val db = AiMemoryDatabase.getDatabase(context)
                                            val repo = AiMemoryRepositoryImpl(db.aiMemoryDao(), DevelopmentEmbeddingProvider())
                                            val inf = LocalInferenceEngineImpl(context)
                                            val rag = LocalAiRagService(repo, inf, DevelopmentEmbeddingProvider())
                                            
                                            val result = rag.query(userQueryText)
                                            diagnosticOutput = result
                                        } catch (e: Exception) {
                                            diagnosticOutput = "Error: ${e.message}"
                                        } finally {
                                            isQuerying = false
                                        }
                                    }
                                }
                            },
                            enabled = !isQuerying,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                            modifier = Modifier.fillMaxWidth().testTag("rag_search_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "EXECUTE LOCAL RAG QUERY",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black
                            )
                        }

                        AnimatedVisibility(visible = diagnosticOutput.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "LOCAL AI INFERENCE OUTPUT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF00FF66)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = diagnosticOutput,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.LightGray,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
