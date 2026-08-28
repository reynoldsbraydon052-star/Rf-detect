package com.example

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RfSessionDao {
    @Query("SELECT * FROM rf_session ORDER BY startTimeMs DESC")
    fun getAllSessionsFlow(): Flow<List<RfSessionEntity>>

    @Query("SELECT * FROM rf_session WHERE state = 'ACTIVE' LIMIT 1")
    suspend fun getActiveSession(): RfSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: RfSessionEntity)
    
    @Query("SELECT * FROM rf_session WHERE id = :id")
    suspend fun getSessionById(id: String): RfSessionEntity?
    
    @Query("DELETE FROM rf_session WHERE id = :id")
    suspend fun deleteSessionById(id: String)
    
    @Query("DELETE FROM rf_session")
    suspend fun clearAll()
}

@Dao
interface RfAnomalyDao {
    @Query("SELECT * FROM rf_anomaly WHERE sessionId = :sessionId ORDER BY timestampMs DESC")
    fun getAnomaliesForSessionFlow(sessionId: String): Flow<List<RfAnomalyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnomaly(anomaly: RfAnomalyEntity)
    
    @Query("DELETE FROM rf_anomaly")
    suspend fun clearAll()
}

@Dao
interface RfPatternDao {
    @Query("SELECT * FROM rf_pattern ORDER BY lastObservedMs DESC")
    suspend fun getAllPatterns(): List<RfPatternEntity>

    @Query("SELECT * FROM rf_pattern WHERE sessionId = :sessionId ORDER BY lastObservedMs DESC")
    fun getPatternsForSessionFlow(sessionId: String): Flow<List<RfPatternEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: RfPatternEntity)
    
    @Query("DELETE FROM rf_pattern")
    suspend fun clearAll()
}

@Dao
interface AnomalyCorrelationDao {
    @Query("SELECT * FROM rf_anomaly_correlation")
    fun getAllCorrelationsFlow(): Flow<List<AnomalyCorrelationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrelation(correlation: AnomalyCorrelationEntity)
}


@Dao
interface RfAnnotationDao {
    @Query("SELECT * FROM rf_annotations WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    fun getAnnotationsBySessionId(sessionId: String): Flow<List<RfAnnotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: RfAnnotationEntity)

    @Query("DELETE FROM rf_annotations WHERE id = :id")
    suspend fun deleteAnnotation(id: String)
}
