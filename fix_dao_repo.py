import re

with open('app/src/main/java/com/example/RfRecordedEventDao.kt', 'r') as f:
    content = f.read()
if "getEventsBySessionId" not in content:
    content = content.replace(
        "fun getEventCount(): Flow<Int>",
        "fun getEventCount(): Flow<Int>\n\n    @Query(\"SELECT * FROM rf_recorded_events WHERE sessionId = :sessionId ORDER BY timestampMs ASC\")\n    suspend fun getEventsBySessionId(sessionId: String): List<RfRecordedEventEntity>\n"
    )
with open('app/src/main/java/com/example/RfRecordedEventDao.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/RfRecordingRepository.kt', 'r') as f:
    content = f.read()
if "getEventsBySessionId" not in content:
    content = content.replace(
        "val eventCount: Flow<Int> = dao.getEventCount()",
        "val eventCount: Flow<Int> = dao.getEventCount()\n\n    suspend fun getEventsBySessionId(sessionId: String): List<RfRecordedEventEntity> = dao.getEventsBySessionId(sessionId)"
    )
with open('app/src/main/java/com/example/RfRecordingRepository.kt', 'w') as f:
    f.write(content)

