package com.example

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiMemoryDao {
    @Query("SELECT * FROM ai_memory ORDER BY timestamp DESC")
    fun getAllMemoriesFlow(): Flow<List<AiMemoryEntity>>

    @Query("SELECT * FROM ai_memory ORDER BY timestamp DESC")
    suspend fun getAllMemories(): List<AiMemoryEntity>

    @Query("SELECT * FROM ai_memory WHERE targetId = :targetId ORDER BY timestamp DESC")
    suspend fun getMemoriesByTargetId(targetId: String): List<AiMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: AiMemoryEntity): Long

    @Query("DELETE FROM ai_memory WHERE id NOT IN (SELECT id FROM ai_memory ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun pruneOldMemories(limit: Int)

    @Query("DELETE FROM ai_memory WHERE id = :id")
    suspend fun deleteMemory(id: Int)
}
