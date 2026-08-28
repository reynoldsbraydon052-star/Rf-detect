package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

sealed class GeminiResult<out T> {
    data class Success<T>(val value: T) : GeminiResult<T>()
    data class ApiError(val code: Int, val message: String) : GeminiResult<Nothing>()
    data class NetworkError(val message: String) : GeminiResult<Nothing>()
    data class ParseError(val message: String) : GeminiResult<Nothing>()
    data object MissingApiKey : GeminiResult<Nothing>()
}

sealed class GeminiConnectionState {
    object NotConfigured : GeminiConnectionState()
    object Testing : GeminiConnectionState()
    data class Connected(val model: String) : GeminiConnectionState()
    data class AuthenticationError(val code: Int, val message: String) : GeminiConnectionState()
    data class HttpError(val code: Int, val message: String) : GeminiConnectionState()
    data class NetworkError(val message: String) : GeminiConnectionState()
}

enum class GeminiStatus {
    READY,
    MISSING_KEY,
    AUTH_ERROR,
    QUOTA_EXCEEDED,
    MODEL_ERROR,
    NETWORK_ERROR,
    SERVER_ERROR,
    PARSE_ERROR,
    UNKNOWN_ERROR
}

class GeminiCloudEngine : AiInferenceEngine {
    private val _status = MutableStateFlow(GeminiStatus.READY)
    val status: StateFlow<GeminiStatus> = _status.asStateFlow()

    private val _connectionState = MutableStateFlow<GeminiConnectionState>(GeminiConnectionState.NotConfigured)
    val connectionState: StateFlow<GeminiConnectionState> = _connectionState.asStateFlow()

    private fun updateStatus(newStatus: GeminiStatus) {
        _status.value = newStatus
    }

    private fun updateConnectionState(newState: GeminiConnectionState) {
        _connectionState.value = newState
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(): GeminiConnectionState = withContext(Dispatchers.IO) {
        updateConnectionState(GeminiConnectionState.Testing)
        val store = AiCredentialStore.get()
        val apiKey = store?.getGeminiApiKey()
        if (apiKey.isNullOrBlank()) {
            updateStatus(GeminiStatus.MISSING_KEY)
            updateConnectionState(GeminiConnectionState.NotConfigured)
            return@withContext GeminiConnectionState.NotConfigured
        }

        try {
            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray()
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "ping"))
                    })
                })
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("maxOutputTokens", 1)
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val state = when (response.code) {
                    401, 403 -> {
                        updateStatus(GeminiStatus.AUTH_ERROR)
                        GeminiConnectionState.AuthenticationError(response.code, "Authentication/Permission Error")
                    }
                    404 -> {
                        updateStatus(GeminiStatus.MODEL_ERROR)
                        GeminiConnectionState.HttpError(response.code, "Model not found")
                    }
                    else -> {
                        updateStatus(GeminiStatus.SERVER_ERROR)
                        GeminiConnectionState.HttpError(response.code, "HTTP Error: $responseBody")
                    }
                }
                updateConnectionState(state)
                return@withContext state
            }

            val state = GeminiConnectionState.Connected("gemini-3.5-flash")
            updateStatus(GeminiStatus.READY)
            updateConnectionState(state)
            state
        } catch (e: SocketTimeoutException) {
            updateStatus(GeminiStatus.NETWORK_ERROR)
            val state = GeminiConnectionState.NetworkError("Timeout: ${e.message}")
            updateConnectionState(state)
            state
        } catch (e: UnknownHostException) {
            updateStatus(GeminiStatus.NETWORK_ERROR)
            val state = GeminiConnectionState.NetworkError("Host unreachable: ${e.message}")
            updateConnectionState(state)
            state
        } catch (e: IOException) {
            updateStatus(GeminiStatus.NETWORK_ERROR)
            val state = GeminiConnectionState.NetworkError("Network error: ${e.message}")
            updateConnectionState(state)
            state
        } catch (e: Exception) {
            updateStatus(GeminiStatus.UNKNOWN_ERROR)
            val state = GeminiConnectionState.HttpError(-1, "Unexpected error: ${e.message}")
            updateConnectionState(state)
            state
        }
    }

    suspend fun checkNetworkSpeed(url: String = "https://clients3.google.com/generate_204"): Boolean = withContext(Dispatchers.IO) {
        val client = okHttpClient.newBuilder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(url)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful || response.code == 204
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun generateContent(
        prompt: String,
        systemInstruction: String? = null,
        responseMimeType: String? = null,
        responseSchema: JSONObject? = null,
        temperature: Double = 0.2,
        maxOutputTokens: Int = 1200
    ): GeminiResult<String> = withContext(Dispatchers.IO) {
        val store = AiCredentialStore.get()
        val apiKey = store?.getGeminiApiKey()
        if (apiKey.isNullOrBlank()) {
            updateStatus(GeminiStatus.MISSING_KEY)
            updateConnectionState(GeminiConnectionState.NotConfigured)
            return@withContext GeminiResult.MissingApiKey
        }

        try {
            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray()
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
                put("contents", contentsArray)

                if (!systemInstruction.isNullOrBlank()) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", systemInstruction))
                        })
                    })
                }

                put("generationConfig", JSONObject().apply {
                    put("temperature", temperature)
                    put("maxOutputTokens", maxOutputTokens)
                    if (responseMimeType != null) {
                        put("responseMimeType", responseMimeType)
                    }
                    if (responseSchema != null) {
                        put("responseSchema", responseSchema)
                    }
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val state = when (response.code) {
                    401, 403 -> {
                        updateStatus(GeminiStatus.AUTH_ERROR)
                        updateConnectionState(GeminiConnectionState.AuthenticationError(response.code, "Authentication Error"))
                        GeminiResult.ApiError(response.code, "Authentication/Permission Error")
                    }
                    404 -> {
                        updateStatus(GeminiStatus.MODEL_ERROR)
                        updateConnectionState(GeminiConnectionState.HttpError(response.code, "Model not found"))
                        GeminiResult.ApiError(404, "Model not found")
                    }
                    429 -> {
                        updateStatus(GeminiStatus.QUOTA_EXCEEDED)
                        GeminiResult.ApiError(429, "Quota exceeded or rate limited")
                    }
                    in 500..599 -> {
                        updateStatus(GeminiStatus.SERVER_ERROR)
                        GeminiResult.ApiError(response.code, "Server Error: $responseBody")
                    }
                    else -> {
                        updateStatus(GeminiStatus.UNKNOWN_ERROR)
                        GeminiResult.ApiError(response.code, "Unknown API Error: $responseBody")
                    }
                }
                return@withContext state
            }

            if (responseBody.isBlank()) {
                updateStatus(GeminiStatus.PARSE_ERROR)
                return@withContext GeminiResult.ParseError("Empty response body")
            }

            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                updateStatus(GeminiStatus.PARSE_ERROR)
                return@withContext GeminiResult.ParseError("No candidates returned. Body: $responseBody")
            }
            
            val candidate = candidates.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            var text = parts?.optJSONObject(0)?.optString("text")

            if (text == null) {
                updateStatus(GeminiStatus.PARSE_ERROR)
                return@withContext GeminiResult.ParseError("Missing text in response. Body: $responseBody")
            }
            
            if (responseMimeType == "application/json") {
                text = text.trim()
                if (text.startsWith("```json", ignoreCase = true)) {
                    text = text.removePrefix("```json").removePrefix("```JSON")
                } else if (text.startsWith("```")) {
                    text = text.removePrefix("```")
                }
                if (text.endsWith("```")) {
                    text = text.removeSuffix("```")
                }
                text = text.trim()
            }

            updateStatus(GeminiStatus.READY)
            updateConnectionState(GeminiConnectionState.Connected("gemini-3.5-flash"))
            GeminiResult.Success(text)

        } catch (e: JSONException) {
            updateStatus(GeminiStatus.PARSE_ERROR)
            GeminiResult.ParseError("Malformed JSON: ${e.message}")
        } catch (e: SocketTimeoutException) {
            updateStatus(GeminiStatus.NETWORK_ERROR)
            updateConnectionState(GeminiConnectionState.NetworkError("Timeout: ${e.message}"))
            GeminiResult.NetworkError("Connection timed out: ${e.message}")
        } catch (e: UnknownHostException) {
            updateStatus(GeminiStatus.NETWORK_ERROR)
            updateConnectionState(GeminiConnectionState.NetworkError("Unreachable: ${e.message}"))
            GeminiResult.NetworkError("Network disconnected or host unreachable: ${e.message}")
        } catch (e: IOException) {
            updateStatus(GeminiStatus.NETWORK_ERROR)
            updateConnectionState(GeminiConnectionState.NetworkError("Network error: ${e.message}"))
            GeminiResult.NetworkError("Network error: ${e.message}")
        } catch (e: Exception) {
            updateStatus(GeminiStatus.UNKNOWN_ERROR)
            GeminiResult.ApiError(-1, "Unexpected error: ${e.message}")
        }
    }

    override suspend fun generateAnalysis(prompt: String, structuredSchema: String?): Result<String> {
        val schemaJson = structuredSchema?.let {
            try {
                JSONObject(it)
            } catch (e: Exception) {
                null
            }
        }
        
        val result = generateContent(
            prompt = prompt,
            responseSchema = schemaJson,
            responseMimeType = if (schemaJson != null) "application/json" else null
        )
        
        return when (result) {
            is GeminiResult.Success -> Result.success(result.value)
            is GeminiResult.MissingApiKey -> Result.failure(Exception("Gemini API Key is missing. Please check your credentials."))
            is GeminiResult.ApiError -> Result.failure(Exception("Gemini API error (Code ${result.code}): ${result.message}"))
            is GeminiResult.NetworkError -> Result.failure(Exception("Gemini Network error: ${result.message}"))
            is GeminiResult.ParseError -> Result.failure(Exception("Gemini Parse error: ${result.message}"))
        }
    }

    override fun isAvailable(): Boolean {
        val s = status.value
        return s != GeminiStatus.MISSING_KEY && s != GeminiStatus.AUTH_ERROR
    }
}
