package com.example

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalAiRagService(
    private val repository: AiMemoryRepository,
    private val inferenceEngine: LocalInferenceEngine,
    private val embeddingProvider: EmbeddingProvider
) {
    suspend fun query(
        userQuery: String,
        targetId: String? = null,
        maxMemories: Int = 5,
        similarityThreshold: Float = 0.3f,
        maxContextSize: Int = 1500
    ): String = withContext(Dispatchers.IO) {
        try {
            // 1. Generate search query embedding with error protection
            val queryEmbedding = try {
                embeddingProvider.embed(userQuery)
            } catch (e: Exception) {
                return@withContext "ERROR: Local RAG Pipeline failed - Embedding Generation Failure: ${e.message}"
            }

            // 2. Retrieve local memories off-UI thread
            val allMemories = if (!targetId.isNullOrBlank()) {
                repository.getMemoriesByTargetId(targetId)
            } else {
                repository.getAllMemories()
            }

            // 3. Compute deterministic cosine similarities and filter by similarityThreshold
            val scoredMemories = allMemories.mapNotNull { memory ->
                try {
                    val similarity = cosineSimilarity(queryEmbedding, memory.embedding)
                    if (similarity >= similarityThreshold) {
                        memory to similarity
                    } else null
                } catch (e: Exception) {
                    // Fail-safe handling for invalid vector sizes, NaN, etc.
                    null
                }
            }.sortedByDescending { it.second }
             .take(maxMemories)

            // 4. Construct the structured markdown context
            val contextBuilder = StringBuilder()
            if (scoredMemories.isNotEmpty()) {
                contextBuilder.append("\nHISTORICAL CONTEXT:\n")
                for ((memory, similarity) in scoredMemories) {
                    val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(memory.timestamp))
                    val item = StringBuilder().apply {
                        append("- [$timeStr] Target: ${memory.displayName} (${memory.sanitizedAddress})\n")
                        append("  Device Type: ${memory.deviceType}, Protocol: ${memory.protocol}, RSSI: ${memory.rssi} dBm\n")
                        if (!memory.anomalySummary.isNullOrEmpty()) {
                            append("  Previous anomaly: ${memory.anomalySummary}\n")
                        }
                        if (!memory.measurementSummary.isNullOrEmpty()) {
                            append("  Previous behavior: ${memory.measurementSummary}\n")
                        }
                        append("  Match Similarity: ${(similarity * 100).toInt()}%\n\n")
                    }.toString()

                    // Respect context size limit
                    if (contextBuilder.length + item.length > maxContextSize) {
                        break
                    }
                    contextBuilder.append(item)
                }

                contextBuilder.append("\nMODEL LIMITATIONS:\n")
                contextBuilder.append("Historical context may be stale and is not a direct measurement.\n")
            }

            // 5. Construct user prompt distinguishing observation from history
            val systemPrompt = "You are SignalRadar Local AI, an offline tactical RF analysis model. Analyze the current observation in light of the historical database contexts provided below."
            
            val userPromptBuilder = StringBuilder()
            userPromptBuilder.append("CURRENT OBSERVATION:\n")
            userPromptBuilder.append("Query: $userQuery\n")
            userPromptBuilder.append("Active Target Context: ${targetId ?: "None Selected"}\n")
            
            if (contextBuilder.isNotEmpty()) {
                userPromptBuilder.append(contextBuilder)
            }

            val userPrompt = userPromptBuilder.toString().trimIndent()

            // 6. Execute inference with structured error handling
            try {
                inferenceEngine.generate(systemPrompt, userPrompt)
            } catch (e: Exception) {
                "ERROR: Local RAG Pipeline failed - Inference Engine Exception: ${e.message}"
            }
        } catch (e: Exception) {
            "ERROR: Local RAG Pipeline failed - Internal Exception: ${e.message}"
        }
    }
}

