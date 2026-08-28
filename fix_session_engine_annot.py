import re

with open('app/src/main/java/com/example/RfInvestigationSessionEngine.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "class RfInvestigationSessionEngine(\n    private val sessionDao: RfSessionDao\n)",
    "class RfInvestigationSessionEngine(\n    private val sessionDao: RfSessionDao,\n    private val annotationDao: RfAnnotationDao\n)"
)

# collect annotations when session changes
new_fields = """
    private val _activeSessionAnnotations = MutableStateFlow<List<RfAnnotationEntity>>(emptyList())
    val activeSessionAnnotations: StateFlow<List<RfAnnotationEntity>> = _activeSessionAnnotations.asStateFlow()
    
    private var annotationJob: Job? = null
"""

content = content.replace(
    "val activeSession: StateFlow<RfSessionEntity?> = _activeSession.asStateFlow()",
    "val activeSession: StateFlow<RfSessionEntity?> = _activeSession.asStateFlow()\n" + new_fields
)

init_block_start = """    init {
        CoroutineScope(Dispatchers.IO).launch {
            val active = sessionDao.getActiveSession()
            if (active != null) {
                _activeSession.value = active
                listenToAnnotations(active.id)
            } else {
                startNewSession()
            }
        }
    }"""

listen_fn = """
    private fun listenToAnnotations(sessionId: String) {
        annotationJob?.cancel()
        annotationJob = CoroutineScope(Dispatchers.IO).launch {
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
"""

content = content.replace(
    """    init {
        CoroutineScope(Dispatchers.IO).launch {
            val active = sessionDao.getActiveSession()
            if (active != null) {
                _activeSession.value = active
            } else {
                startNewSession()
            }
        }
    }""",
    init_block_start + "\n" + listen_fn
)

content = content.replace(
    "_activeSession.value = newSession",
    "_activeSession.value = newSession\n        listenToAnnotations(newSession.id)"
)

content = content.replace(
    "_activeSession.value = session",
    "_activeSession.value = session\n            listenToAnnotations(session.id)"
)

content = content.replace(
    "_activeSession.value = null",
    "_activeSession.value = null\n            annotationJob?.cancel()\n            _activeSessionAnnotations.value = emptyList()"
)


with open('app/src/main/java/com/example/RfInvestigationSessionEngine.kt', 'w') as f:
    f.write(content)


