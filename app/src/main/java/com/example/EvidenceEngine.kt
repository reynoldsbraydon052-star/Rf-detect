package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EvidenceEngine(private val dao: EvidenceDao) {
    private val _evidenceList = MutableStateFlow<List<EvidenceItem>>(emptyList())
    val evidenceList: StateFlow<List<EvidenceItem>> = _evidenceList.asStateFlow()

    suspend fun loadEvidenceForSession(sessionId: String) {
        val loaded = dao.getEvidenceForSession(sessionId)
        _evidenceList.value = loaded
    }

    suspend fun calculateScore(evidenceItems: List<EvidenceItem>): Int {
        if (evidenceItems.isEmpty()) return 0
        
        var totalWeightedScore = 0f
        var maxPossibleWeight = 0f
        
        val uniqueSources = mutableSetOf<String>()
        
        for (item in evidenceItems) {
            // Avoid double counting correlated evidence from same source if needed
            val sourceKey = "${item.sourceEventId}_${item.type}"
            if (uniqueSources.contains(sourceKey)) continue
            uniqueSources.add(sourceKey)
            
            val itemImpact = item.weight * item.reliability * item.confidence
            if (item.isSupporting) {
                totalWeightedScore += itemImpact
            } else {
                totalWeightedScore -= itemImpact
            }
            maxPossibleWeight += item.weight
        }
        
        if (maxPossibleWeight == 0f) return 0
        
        val rawScore = (totalWeightedScore / maxPossibleWeight) * 100f
        return rawScore.toInt().coerceIn(0, 100)
    }

    suspend fun addEvidence(evidence: List<EvidenceItem>) {
        dao.insertEvidence(evidence)
        _evidenceList.update { current ->
            val updated = current.toMutableList()
            updated.addAll(evidence)
            updated
        }
    }
}
