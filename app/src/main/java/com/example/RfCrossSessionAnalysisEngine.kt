package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DeviceRecurrence(
    val primaryMac: String,
    val sessionIds: List<String>,
    val confidenceScore: Int,
    val frequencyMhzMean: Double?
)

data class PatternRecurrence(
    val type: String,
    val sessionIds: List<String>,
    val confidenceScore: Int
)

data class CrossSessionAnalysisResult(
    val deviceRecurrences: List<DeviceRecurrence>,
    val patternRecurrences: List<PatternRecurrence>
)

class RfCrossSessionAnalysisEngine(
    private val sessionDao: RfSessionDao,
    private val identityDao: DeviceIdentityDao,
    private val patternDao: RfPatternDao
) {
    private val _analysisResult = MutableStateFlow(CrossSessionAnalysisResult(emptyList(), emptyList()))
    val analysisResult: StateFlow<CrossSessionAnalysisResult> = _analysisResult.asStateFlow()

    suspend fun runAnalysis() {
        val allIdentities = identityDao.getAllHypotheses()
        
        val deviceGroups = allIdentities.groupBy { it.primaryMac }
        val deviceRecurrences = deviceGroups.mapNotNull { (mac, identities) ->
            if (identities.size > 1) {
                val sessions = identities.map { it.sessionId }.distinct()
                if (sessions.size > 1) {
                    DeviceRecurrence(
                        primaryMac = mac,
                        sessionIds = sessions,
                        confidenceScore = (sessions.size * 20).coerceAtMost(100),
                        frequencyMhzMean = null
                    )
                } else null
            } else null
        }
        
        val allPatterns = patternDao.getAllPatterns()
        val patternGroups = allPatterns.groupBy { it.type }
        val patternRecurrences = patternGroups.mapNotNull { (type, patterns) ->
            if (patterns.size > 1) {
                val sessions = patterns.map { it.sessionId }.distinct()
                if (sessions.size > 1) {
                    PatternRecurrence(
                        type = type,
                        sessionIds = sessions,
                        confidenceScore = (sessions.size * 15).coerceAtMost(100)
                    )
                } else null
            } else null
        }
        
        _analysisResult.value = CrossSessionAnalysisResult(
            deviceRecurrences = deviceRecurrences.sortedByDescending { it.confidenceScore },
            patternRecurrences = patternRecurrences.sortedByDescending { it.confidenceScore }
        )
    }
}
