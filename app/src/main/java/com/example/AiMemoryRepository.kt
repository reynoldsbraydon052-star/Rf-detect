package com.example

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlin.math.sqrt

interface AiMemoryRepository {
    fun getAllMemoriesFlow(): Flow<List<AiMemoryEntity>>
    suspend fun getAllMemories(): List<AiMemoryEntity>
    suspend fun getMemoriesByTargetId(targetId: String): List<AiMemoryEntity>
    suspend fun saveMemory(memory: AiMemoryEntity)
    suspend fun generateEmbedding(text: String): FloatArray
    suspend fun deleteMemory(id: Int)
}

fun cosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
    if (vectorA.isEmpty() || vectorB.isEmpty()) {
        throw IllegalArgumentException("Zero-length vectors rejected")
    }
    if (vectorA.size != vectorB.size) {
        throw IllegalArgumentException("Vector dimension mismatch: ${vectorA.size} != ${vectorB.size}")
    }
    var dotProduct = 0.0f
    var normA = 0.0f
    var normB = 0.0f
    for (i in vectorA.indices) {
        val a = vectorA[i]
        val b = vectorB[i]
        if (a.isNaN() || a.isInfinite() || b.isNaN() || b.isInfinite()) {
            throw IllegalArgumentException("NaN or Infinity elements rejected")
        }
        dotProduct += a * b
        normA += a * a
        normB += b * b
    }
    if (normA == 0.0f || normB == 0.0f) return 0.0f
    return dotProduct / (sqrt(normA) * sqrt(normB))
}

class AiMemoryRepositoryImpl(
    private val dao: AiMemoryDao,
    private val embeddingProvider: EmbeddingProvider,
    private val memoryHistoryLimit: Int = 100 // Bounds memory size
) : AiMemoryRepository {

    override fun getAllMemoriesFlow(): Flow<List<AiMemoryEntity>> = dao.getAllMemoriesFlow()

    override suspend fun getAllMemories(): List<AiMemoryEntity> = dao.getAllMemories()

    override suspend fun getMemoriesByTargetId(targetId: String): List<AiMemoryEntity> =
        dao.getMemoriesByTargetId(targetId)

    override suspend fun saveMemory(memory: AiMemoryEntity) {
        try {
            // Validate vector before save
            if (memory.embedding.isEmpty()) {
                Log.d("AiMemoryRepository", "Skipping save: Zero-length embedding vector")
                return
            }
            if (memory.embedding.any { it.isNaN() || it.isInfinite() }) {
                Log.d("AiMemoryRepository", "Skipping save: NaN or Infinity found in embedding")
                return
            }

            dao.insertMemory(memory)
            dao.pruneOldMemories(memoryHistoryLimit) // strictly enforce bounded storage limits
        } catch (e: Exception) {
            Log.d("AiMemoryRepository", "Failed to save AI memory: ${e.message}")
        }
    }

    override suspend fun generateEmbedding(text: String): FloatArray {
        return embeddingProvider.embed(text)
    }

    override suspend fun deleteMemory(id: Int) = dao.deleteMemory(id)
}
