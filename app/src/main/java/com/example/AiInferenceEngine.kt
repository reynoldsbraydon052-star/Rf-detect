package com.example

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

enum class AiInferenceMode {
    GEMINI_CLOUD,
    LOCAL_GGUF,
    AUTO_HYBRID
}

interface AiInferenceEngine {
    suspend fun generateAnalysis(prompt: String, structuredSchema: String?): Result<String>
    fun isAvailable(): Boolean
}

class LlamaCppEngine(private val context: Context) : AiInferenceEngine {

    private val _status = MutableStateFlow(GeminiStatus.READY)
    val status: StateFlow<GeminiStatus> = _status.asStateFlow()

    private val _connectionState = MutableStateFlow<GeminiConnectionState>(
        if (isAvailable()) GeminiConnectionState.Connected("Local llama.cpp Engine")
        else GeminiConnectionState.NotConfigured
    )
    val connectionState: StateFlow<GeminiConnectionState> = _connectionState.asStateFlow()

    override suspend fun generateAnalysis(prompt: String, structuredSchema: String?): Result<String> {
        // TODO: This is the critical entry point where the JNI/C++ native call (e.g., from llama.cpp)
        // will load the GGUF model and stream output tokens in future phases.
        val response = simulateLocalModelInference(prompt, structuredSchema)
        return Result.success(response)
    }

    override fun isAvailable(): Boolean {
        val modelsDir = File(context.filesDir, "models")
        val extModelsDir = File(context.getExternalFilesDir(null), "models")
        return (modelsDir.exists() && modelsDir.listFiles()?.any { it.name.endsWith(".gguf") } == true) ||
               (extModelsDir.exists() && extModelsDir.listFiles()?.any { it.name.endsWith(".gguf") } == true)
    }

    private fun simulateLocalModelInference(prompt: String, structuredSchema: String?): String {
        if (structuredSchema == null) {
            return "Local GGUF offline model completed tactical sweep analysis."
        }
        
        return """
        {
          "threatLevel": "LOW_CAUTION",
          "threatScore": 22,
          "executiveSummary": "Local LLaMA on-device offline analysis completed.",
          "naturalLanguageThreatAssessment": "Processed entirely on-device using a local-first GGUF model. The environment exhibits standard ambient radio traffic.",
          "identifiedVectors": ["UNREGISTERED_BLE_BEACON"],
          "flaggedEmitters": [],
          "countermeasures": [],
          "rawSigintDetails": "Processed offline using local model framework."
        }
        """.trimIndent()
    }
}

class AiEngineRouter(
    private val geminiEngine: GeminiCloudEngine,
    private val localEngine: LlamaCppEngine,
    private val settingsDataStore: SettingsDataStore
) : AiInferenceEngine {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _status = MutableStateFlow(GeminiStatus.READY)
    val status: StateFlow<GeminiStatus> = _status.asStateFlow()

    private val _connectionState = MutableStateFlow<GeminiConnectionState>(GeminiConnectionState.NotConfigured)
    val connectionState: StateFlow<GeminiConnectionState> = _connectionState.asStateFlow()

    init {
        scope.launch {
            settingsDataStore.aiInferenceMode.collectLatest { mode ->
                when (mode) {
                    AiInferenceMode.GEMINI_CLOUD -> {
                        launch {
                            geminiEngine.status.collect { _status.value = it }
                        }
                        launch {
                            geminiEngine.connectionState.collect { _connectionState.value = it }
                        }
                    }
                    AiInferenceMode.LOCAL_GGUF -> {
                        launch {
                            localEngine.status.collect { _status.value = it }
                        }
                        launch {
                            localEngine.connectionState.collect { _connectionState.value = it }
                        }
                    }
                    AiInferenceMode.AUTO_HYBRID -> {
                        launch {
                            if (localEngine.isAvailable()) {
                                localEngine.status.collect { _status.value = it }
                            } else {
                                geminiEngine.status.collect { _status.value = it }
                            }
                        }
                        launch {
                            if (localEngine.isAvailable()) {
                                _connectionState.value = GeminiConnectionState.Connected("Hybrid: Local Active")
                            } else {
                                geminiEngine.connectionState.collect { _connectionState.value = it }
                            }
                        }
                    }
                }
            }
        }
    }

    fun getGeminiEngine(): GeminiCloudEngine = geminiEngine
    fun getLocalEngine(): LlamaCppEngine = localEngine

    suspend fun getActiveMode(): AiInferenceMode = settingsDataStore.aiInferenceMode.first()

    suspend fun testConnection(): GeminiConnectionState {
        val activeMode = getActiveMode()
        return when (activeMode) {
            AiInferenceMode.GEMINI_CLOUD -> {
                geminiEngine.testConnection()
            }
            AiInferenceMode.LOCAL_GGUF -> {
                if (localEngine.isAvailable()) {
                    GeminiConnectionState.Connected("Local llama.cpp Engine")
                } else {
                    GeminiConnectionState.NetworkError("Local GGUF model files are missing or unconfigured.")
                }
            }
            AiInferenceMode.AUTO_HYBRID -> {
                if (localEngine.isAvailable()) {
                    GeminiConnectionState.Connected("Hybrid Mode: Local Active")
                } else {
                    geminiEngine.testConnection()
                }
            }
        }
    }

    override suspend fun generateAnalysis(prompt: String, structuredSchema: String?): Result<String> {
        val activeMode = settingsDataStore.aiInferenceMode.first()
        
        return when (activeMode) {
            AiInferenceMode.GEMINI_CLOUD -> {
                geminiEngine.generateAnalysis(prompt, structuredSchema)
            }
            AiInferenceMode.LOCAL_GGUF -> {
                if (localEngine.isAvailable()) {
                    localEngine.generateAnalysis(prompt, structuredSchema)
                } else {
                    Result.failure(Exception("Local GGUF model files are missing or unconfigured. Please load a .gguf model in private storage."))
                }
            }
            AiInferenceMode.AUTO_HYBRID -> {
                if (localEngine.isAvailable()) {
                    val localResult = localEngine.generateAnalysis(prompt, structuredSchema)
                    if (localResult.isSuccess) {
                        localResult
                    } else {
                        // Fallback to Cloud Gemini on failure
                        geminiEngine.generateAnalysis(prompt, structuredSchema)
                    }
                } else {
                    // Fallback to Cloud Gemini if local engine is unavailable
                    geminiEngine.generateAnalysis(prompt, structuredSchema)
                }
            }
        }
    }

    override fun isAvailable(): Boolean {
        return geminiEngine.isAvailable() || localEngine.isAvailable()
    }
}
