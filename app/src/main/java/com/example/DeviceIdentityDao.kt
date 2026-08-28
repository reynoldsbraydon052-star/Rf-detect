package com.example

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DeviceIdentityDao {
    @Query("SELECT * FROM device_identity_hypothesis")
    suspend fun getAllHypotheses(): List<DeviceIdentityEntity>

    @Query("SELECT * FROM device_identity_hypothesis WHERE sessionId = :sessionId")
    suspend fun getHypothesesForSession(sessionId: String): List<DeviceIdentityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHypothesis(hypothesis: DeviceIdentityEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHypotheses(hypotheses: List<DeviceIdentityEntity>)
    
    @Query("DELETE FROM device_identity_hypothesis WHERE id = :id")
    suspend fun deleteHypothesis(id: String)
    
    @Query("DELETE FROM device_identity_hypothesis")
    suspend fun clearAll()
}
