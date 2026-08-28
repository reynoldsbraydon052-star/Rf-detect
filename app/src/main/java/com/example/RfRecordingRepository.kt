package com.example

import kotlinx.coroutines.flow.Flow

class RfRecordingRepository(private val dao: RfRecordedEventDao) {
    val allEvents: Flow<List<RfRecordedEventEntity>> = dao.getAllEvents()
    val eventCount: Flow<Int> = dao.getEventCount()

    suspend fun getEventsBySessionId(sessionId: String): List<RfRecordedEventEntity> = dao.getEventsBySessionId(sessionId)

    suspend fun insertEvent(event: RfRecordedEventEntity) {
        dao.insertEvent(event)
    }

    suspend fun insertEvents(events: List<RfRecordedEventEntity>) {
        dao.insertEvents(events)
    }

    suspend fun clearAllEvents() {
        dao.clearAllEvents()
    }
    
    fun getEventsByTimeRange(startTime: Long, endTime: Long) = dao.getEventsByTimeRange(startTime, endTime)
    fun getEventsByDeviceId(deviceId: String) = dao.getEventsByDeviceId(deviceId)
    fun getEventsBySignalType(signalType: String) = dao.getEventsBySignalType(signalType)
}
