
package com.example

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class ReplayState {
    STOPPED,
    PLAYING,
    PAUSED
}

class ReplayEngine(
    private val signalRadarViewModel: SignalRadarViewModel,
    private val repository: RfRecordingRepository,
    private val sessionEngine: RfInvestigationSessionEngine
) {
    private val _replayState = MutableStateFlow(ReplayState.STOPPED)
    val replayState: StateFlow<ReplayState> = _replayState.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _activeSession = MutableStateFlow<RfSessionEntity?>(null)
    val activeSession: StateFlow<RfSessionEntity?> = _activeSession.asStateFlow()
    
    private var eventsList = listOf<RfRecordedEventEntity>()

    private var replayJob: Job? = null
    private var currentIndex = 0
    private var isScrubbing = false

    fun loadSession(session: RfSessionEntity) {
        stopReplay()
        _activeSession.value = session
        _currentPositionMs.value = 0L
        currentIndex = 0
        
        CoroutineScope(Dispatchers.IO).launch {
            eventsList = repository.getEventsBySessionId(session.id)
        }
    }

    fun play() {
        val session = _activeSession.value ?: return
        if (_replayState.value == ReplayState.PLAYING) return
        _replayState.value = ReplayState.PLAYING
        
        replayJob = CoroutineScope(Dispatchers.Default).launch {
            if (eventsList.isEmpty() || currentIndex >= eventsList.size) {
                stopReplay()
                return@launch
            }
            
            var lastRealTime = System.currentTimeMillis()
            var lastEventTime = eventsList[currentIndex].timestampMs
            
            while (isActive && currentIndex < eventsList.size) {
                if (isScrubbing) {
                    delay(50)
                    continue
                }
                
                val event = eventsList[currentIndex]
                val currentRealTime = System.currentTimeMillis()
                
                val elapsedRealTime = currentRealTime - lastRealTime
                val timeToNextEvent = ((event.timestampMs - lastEventTime) / _playbackSpeed.value).toLong()
                
                if (elapsedRealTime >= timeToNextEvent) {
                    processEvent(event)
                    _currentPositionMs.value = event.timestampMs - session.startTimeMs
                    lastEventTime = event.timestampMs
                    lastRealTime = currentRealTime
                    currentIndex++
                } else {
                    delay(10)
                }
            }
            if (currentIndex >= eventsList.size) {
                stopReplay()
            }
        }
    }

    fun pause() {
        replayJob?.cancel()
        _replayState.value = ReplayState.PAUSED
    }

    fun stopReplay() {
        replayJob?.cancel()
        _replayState.value = ReplayState.STOPPED
        _currentPositionMs.value = 0L
        currentIndex = 0
        signalRadarViewModel.clearReplayState()
    }
    
    fun beginScrub() {
        isScrubbing = true
    }
    
    fun endScrub() {
        isScrubbing = false
        // Re-sync logic
        if (currentIndex < eventsList.size) {
            // fast forward state internally without playing time to update the UI
            // However, a true scrub would re-evaluate the window of events up to this point
        }
    }

    fun seekTo(positionMs: Long) {
        val session = _activeSession.value ?: return
        val targetTimestamp = session.startTimeMs + positionMs
        
        currentIndex = eventsList.indexOfFirst { it.timestampMs >= targetTimestamp }
        if (currentIndex == -1) currentIndex = eventsList.size - 1
        if (currentIndex < 0) currentIndex = 0
        
        _currentPositionMs.value = positionMs
        
        // Compute valid state at target timestamp and publish
        val validEvents = eventsList.take(currentIndex)
        signalRadarViewModel.reconstructStateFromEvents(validEvents)
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    private fun processEvent(entity: RfRecordedEventEntity) {
        // Convert back to RadarBlip for UI processing
        val anomalyResult = if (entity.anomalyScore != null || entity.classification != null) {
            AnomalyResult(
                score = entity.anomalyScore?.toInt() ?: 0,
                confidence = entity.classificationConfidence ?: 0f,
                explanations = if (entity.classification != null) listOf(AnomalyExplanation(entity.classification, 1)) else emptyList()
            )
        } else null
        
        val blip = RadarBlip(
            id = entity.deviceId,
            name = entity.manufacturerInfo ?: "Unknown Replay Device",
            distance = entity.distanceMeters ?: 0f,
            targetAngleOffset = 0f,
            type = entity.signalType,
            rssi = entity.rssi,
            frequencyMhz = entity.frequencyMhz,
            bandLabel = entity.bandLabel,
            anomalyResult = anomalyResult,
            provenance = DataProvenance.REPLAY,
            timestampMs = entity.timestampMs
        )
        
        signalRadarViewModel.injectReplayBlip(blip)
    }
}
