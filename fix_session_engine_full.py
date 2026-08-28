with open('app/src/main/java/com/example/RfInvestigationSessionEngine.kt', 'w') as f:
    f.write("""package com.example

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class RfInvestigationSessionEngine(
    private val sessionDao: RfSessionDao,
    private val annotationDao: RfAnnotationDao
) {

    private val _activeSession = MutableStateFlow<RfSessionEntity?>(null)
    val activeSession: StateFlow<RfSessionEntity?> = _activeSession.asStateFlow()

    private val _activeSessionAnnotations = MutableStateFlow<List<RfAnnotationEntity>>(emptyList())
    val activeSessionAnnotations: StateFlow<List<RfAnnotationEntity>> = _activeSessionAnnotations.asStateFlow()
    
    private var annotationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val allSessions = sessionDao.getAllSessionsFlow()

    suspend fun loadActiveSession() {
        val session = sessionDao.getActiveSession()
        _activeSession.value = session
        session?.let { listenToAnnotations(it.id) }
    }

    private fun listenToAnnotations(sessionId: String) {
        annotationJob?.cancel()
        annotationJob = scope.launch {
            annotationDao.getAnnotationsBySessionId(sessionId).collect {
                _activeSessionAnnotations.value = it
            }
        }
    }
    
    suspend fun addAnnotation(text: String, category: String) {
        val sessionId = getActiveSessionId() ?: return
        val entity = RfAnnotationEntity(
            sessionId = sessionId,
            timestampMs = System.currentTimeMillis(),
            text = text,
            category = category
        )
        annotationDao.insertAnnotation(entity)
    }

    suspend fun createNewSession(name: String) {
        // Pause current if active
        _activeSession.value?.let {
            sessionDao.insertSession(it.copy(state = SessionState.PAUSED.name, endTimeMs = System.currentTimeMillis()))
        }
        
        val newSession = RfSessionEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            startTimeMs = System.currentTimeMillis(),
            endTimeMs = null,
            state = SessionState.RECORDING.name,
            eventCount = 0,
            anomalyCount = 0,
            deviceCount = 0,
            mapCellCount = 0,
            configurationSnapshotJson = "{}"
        )
        
        sessionDao.insertSession(newSession)
        _activeSession.value = newSession
        listenToAnnotations(newSession.id)
    }

    suspend fun pauseSession() {
        _activeSession.value?.let {
            val paused = it.copy(state = SessionState.PAUSED.name, endTimeMs = System.currentTimeMillis())
            sessionDao.insertSession(paused)
            _activeSession.value = null
            annotationJob?.cancel()
            _activeSessionAnnotations.value = emptyList()
        }
    }
    
    suspend fun closeSession() {
         _activeSession.value?.let {
            val closed = it.copy(state = SessionState.COMPLETED.name, endTimeMs = System.currentTimeMillis())
            sessionDao.insertSession(closed)
            _activeSession.value = null
            annotationJob?.cancel()
            _activeSessionAnnotations.value = emptyList()
        }
    }

    suspend fun updateSessionStats(eventCount: Int, anomalyCount: Int, deviceCount: Int, mapCellCount: Int) {
        _activeSession.value?.let {
            val updated = it.copy(
                eventCount = eventCount,
                anomalyCount = anomalyCount,
                deviceCount = deviceCount,
                mapCellCount = mapCellCount
            )
            sessionDao.insertSession(updated)
            _activeSession.value = updated
        }
    }
    
    suspend fun resumeSession(sessionId: String) {
        pauseSession() // Pause current if active
        val session = sessionDao.getSessionById(sessionId)
        if (session != null) {
            val resumed = session.copy(state = SessionState.RECORDING.name, endTimeMs = null)
            sessionDao.insertSession(resumed)
            _activeSession.value = resumed
            listenToAnnotations(resumed.id)
        }
    }
    
    suspend fun archiveSession(sessionId: String) {
        if (_activeSession.value?.id == sessionId) {
            pauseSession()
        }
        val session = sessionDao.getSessionById(sessionId)
        if (session != null) {
            sessionDao.insertSession(session.copy(state = SessionState.ARCHIVED.name))
        }
    }
    
    suspend fun deleteSession(sessionId: String) {
        if (_activeSession.value?.id == sessionId) {
            _activeSession.value = null
            annotationJob?.cancel()
            _activeSessionAnnotations.value = emptyList()
        }
        sessionDao.deleteSessionById(sessionId)
    }
    
    fun getActiveSessionId(): String? = _activeSession.value?.id
}
""")
