package com.example

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AiMemoryInput(
    val targetId: String?,
    val deviceType: String,
    val protocol: String,
    val displayName: String,
    val sanitizedAddress: String,
    val rssi: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val anomalySummary: String?,
    val measurementSummary: String?
)

interface AiMemoryIngestor {
    suspend fun ingest(memory: AiMemoryInput)
}

class AiMemoryIngestorImpl(private val repository: AiMemoryRepository) : AiMemoryIngestor {
    override suspend fun ingest(memory: AiMemoryInput) = withContext(Dispatchers.IO) {
        val promptText = "Target: ${memory.displayName}, MAC: ${memory.sanitizedAddress}, " +
                "Type: ${memory.deviceType}, Protocol: ${memory.protocol}, RSSI: ${memory.rssi} dBm, " +
                "Anomaly: ${memory.anomalySummary ?: "None"}, Behavior: ${memory.measurementSummary ?: "None"}"
        
        val vector = repository.generateEmbedding(promptText)

        val entity = AiMemoryEntity(
            targetId = memory.targetId,
            deviceType = memory.deviceType,
            protocol = memory.protocol,
            displayName = memory.displayName,
            sanitizedAddress = memory.sanitizedAddress,
            rssi = memory.rssi,
            timestamp = memory.timestamp,
            anomalySummary = memory.anomalySummary,
            measurementSummary = memory.measurementSummary,
            embedding = vector
        )

        repository.saveMemory(entity)
    }
}

object AiMemoryIngestorProvider {
    @Volatile
    private var instance: AiMemoryIngestor? = null

    fun getIngestor(context: Context): AiMemoryIngestor {
        return instance ?: synchronized(this) {
            instance ?: run {
                val db = AiMemoryDatabase.getDatabase(context)
                val repo = AiMemoryRepositoryImpl(db.aiMemoryDao(), DevelopmentEmbeddingProvider())
                val newIngestor = AiMemoryIngestorImpl(repo)
                instance = newIngestor
                newIngestor
            }
        }
    }
}
