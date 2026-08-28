package com.example

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RfRecordedEventDao {
    @Query("SELECT * FROM rf_recorded_events ORDER BY timestampMs DESC")
    fun getAllEvents(): Flow<List<RfRecordedEventEntity>>

    @Query("SELECT COUNT(*) FROM rf_recorded_events")
    fun getEventCount(): Flow<Int>

    @Query("SELECT * FROM rf_recorded_events WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    suspend fun getEventsBySessionId(sessionId: String): List<RfRecordedEventEntity>


    @Query("SELECT * FROM rf_recorded_events WHERE timestampMs >= :startTimeMs AND timestampMs <= :endTimeMs ORDER BY timestampMs DESC")
    fun getEventsByTimeRange(startTimeMs: Long, endTimeMs: Long): Flow<List<RfRecordedEventEntity>>

    @Query("SELECT * FROM rf_recorded_events WHERE deviceId = :deviceId ORDER BY timestampMs DESC")
    fun getEventsByDeviceId(deviceId: String): Flow<List<RfRecordedEventEntity>>

    @Query("SELECT * FROM rf_recorded_events WHERE signalType = :signalType ORDER BY timestampMs DESC")
    fun getEventsBySignalType(signalType: String): Flow<List<RfRecordedEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: RfRecordedEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<RfRecordedEventEntity>)

    @Query("DELETE FROM rf_recorded_events")
    suspend fun clearAllEvents()
}
