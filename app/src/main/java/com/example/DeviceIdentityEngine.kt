package com.example

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DeviceIdentityEngine(private val context: Context, private val dao: DeviceIdentityDao, private val evidenceEngine: EvidenceEngine) {

    private val _hypotheses = MutableStateFlow<Map<String, DeviceIdentityHypothesis>>(emptyMap())
    val hypotheses: StateFlow<Map<String, DeviceIdentityHypothesis>> = _hypotheses.asStateFlow()

    suspend fun loadHypothesesForSession(sessionId: String) {
        val loaded = dao.getHypothesesForSession(sessionId).map { DeviceIdentityHypothesis.fromEntity(it) }
        _hypotheses.update { 
            loaded.associateBy { it.id }
        }
    }

    suspend fun processObservations(blips: List<RadarBlip>, fingerprints: Map<String, SignalFingerprint>, sessionId: String) {
        val currentMap = _hypotheses.value.toMutableMap()
        var updated = false

        for (blip in blips) {
            val fp = fingerprints[blip.fingerprintId]
            val sanitizedMac = MacSanitizer.sanitize(blip.id)
            val resolvedVendor = blip.ouiVendor ?: OfflineOuiResolver.resolveVendorOffline(context, blip.id)
            val enrichment = HardwareIdentityEnricher.enrich(blip.id, resolvedVendor, blip.payloadBytes, blip.type)
            
            // Try to find an existing matching hypothesis
            var matchedHypothesis: DeviceIdentityHypothesis? = null
            var bestMatchScore = 0
            val evidenceList = mutableListOf<IdentityEvidence>()

            for (hypothesis in currentMap.values) {
                var score = 0
                val currentEvidence = mutableListOf<IdentityEvidence>()

                // 1. MAC Address Match (Direct Identity)
                val hypSanitizedMac = MacSanitizer.sanitize(hypothesis.primaryMac)
                val hypAssociatedSanitized = hypothesis.associatedMacs.map { MacSanitizer.sanitize(it) }.toSet()
                
                if (hypSanitizedMac == sanitizedMac || hypAssociatedSanitized.contains(sanitizedMac)) {
                    score += 50
                    currentEvidence.add(IdentityEvidence("Exact MAC address match", 50))
                } else {
                    // Different MAC, check if it's potentially randomized
                    if (MacOuidResolver.isRandomized(blip.id)) {
                        val isRpa = MacOuidResolver.isResolvablePrivateAddress(blip.id)
                        val rType = if (isRpa) "Resolvable Private Address (RPA)" else "Randomized MAC"
                        currentEvidence.add(IdentityEvidence("$rType usage detected", -10))
                    } else {
                        currentEvidence.add(IdentityEvidence("MAC address changed", -20))
                        score -= 20
                    }
                }

                // 2. RF Fingerprint Match
                if (fp != null && blip.fingerprintId != null) {
                    score += 35
                    currentEvidence.add(IdentityEvidence("Strong RF fingerprint correlation", 35))
                }

                // 3. OUI Match
                if (resolvedVendor != null && hypothesis.primaryOUI == resolvedVendor) {
                    score += 15
                    currentEvidence.add(IdentityEvidence("Same manufacturer characteristics ($resolvedVendor)", 15))
                }

                // 4. Type Match
                if (blip.type == hypothesis.primaryDeviceType) {
                    score += 10
                    currentEvidence.add(IdentityEvidence("Consistent device protocol (${blip.type})", 10))
                }

                // 5. Frequency/Channel Behavior
                if (blip.frequencyMhz > 0) {
                    score += 5
                    currentEvidence.add(IdentityEvidence("Consistent frequency behavior", 5))
                }

                // 6. Timing / Advertisement Intervals
                if (blip.pulseRepetitionIntervalMs != null) {
                    score += 15
                    currentEvidence.add(IdentityEvidence("Similar transmission interval", 15))
                }
                
                // 7. RSSI / Spatial proximity
                if (blip.rssi > -60) {
                    score += 10
                    currentEvidence.add(IdentityEvidence("Spatial continuity (Strong RSSI overlap)", 10))
                } else if (blip.rssi < -90) {
                    currentEvidence.add(IdentityEvidence("Temporary RSSI inconsistency", -5))
                    score -= 5
                }

                if (score > bestMatchScore && score > 25) { // Threshold for merging/updating
                    bestMatchScore = score
                    matchedHypothesis = hypothesis
                    evidenceList.clear()
                    evidenceList.addAll(currentEvidence)
                }
            }

            if (matchedHypothesis != null) {
                // Update existing
                val combinedEvidence = (evidenceList + matchedHypothesis.evidence)
                    .distinctBy { it.description }
                    .take(8)
                
                val calculatedScore = calculateConfidenceScore(combinedEvidence)
                
                val updatedHypothesis = matchedHypothesis.copy(
                    associatedMacs = matchedHypothesis.associatedMacs + blip.id,
                    confidenceScorePercent = calculatedScore,
                    evidence = combinedEvidence,
                    lastSeenMs = blip.timestampMs,
                    observationCount = matchedHypothesis.observationCount + 1,
                    // Enrich details
                    hardwareCategory = enrichment.hardwareCategory,
                    powerClass = enrichment.powerClass,
                    protocolFingerprint = enrichment.protocolFingerprint,
                    securityProfile = enrichment.securityProfile,
                    semanticProfile = enrichment.semanticProfile ?: matchedHypothesis.semanticProfile,
                    semanticName = enrichment.semanticProfile?.localName ?: matchedHypothesis.semanticName,
                    semanticAppearance = enrichment.semanticProfile?.appearanceDisplayName ?: matchedHypothesis.semanticAppearance,
                    semanticEcosystem = enrichment.semanticProfile?.proprietaryEcosystemType ?: matchedHypothesis.semanticEcosystem
                )
                currentMap[updatedHypothesis.id] = updatedHypothesis
                updated = true
            } else {
                // Create new
                val newEvidence = mutableListOf<IdentityEvidence>()
                newEvidence.add(IdentityEvidence("Initial observation", 10))
                if (resolvedVendor != null) {
                    newEvidence.add(IdentityEvidence("Manufacturer identified: $resolvedVendor", 15))
                }
                if (enrichment.semanticProfile?.proprietaryEcosystemType != null) {
                    newEvidence.add(IdentityEvidence("Decoded ecosystem beacon: ${enrichment.semanticProfile.proprietaryEcosystemType}", 25))
                }
                if (enrichment.semanticProfile?.localName != null) {
                    newEvidence.add(IdentityEvidence("Decoded local name: ${enrichment.semanticProfile.localName}", 20))
                }
                if (enrichment.semanticProfile?.appearanceDisplayName != null) {
                    newEvidence.add(IdentityEvidence("Decoded hardware appearance: ${enrichment.semanticProfile.appearanceDisplayName}", 15))
                }
                if (fp != null) {
                    newEvidence.add(IdentityEvidence("Distinct RF fingerprint captured", 20))
                }

                val calculatedScore = calculateConfidenceScore(newEvidence)

                val newHypothesis = DeviceIdentityHypothesis(
                    sessionId = sessionId,
                    primaryMac = blip.id,
                    associatedMacs = setOf(blip.id),
                    confidenceScorePercent = calculatedScore,
                    evidence = newEvidence,
                    firstSeenMs = blip.timestampMs,
                    lastSeenMs = blip.timestampMs,
                    observationCount = 1,
                    primaryDeviceType = blip.type,
                    primaryOUI = resolvedVendor,
                    // Enrich details
                    hardwareCategory = enrichment.hardwareCategory,
                    powerClass = enrichment.powerClass,
                    protocolFingerprint = enrichment.protocolFingerprint,
                    securityProfile = enrichment.securityProfile,
                    semanticProfile = enrichment.semanticProfile,
                    semanticName = enrichment.semanticProfile?.localName,
                    semanticAppearance = enrichment.semanticProfile?.appearanceDisplayName,
                    semanticEcosystem = enrichment.semanticProfile?.proprietaryEcosystemType
                )
                currentMap[newHypothesis.id] = newHypothesis
                updated = true
            }
        }

        if (updated) {
            _hypotheses.update { currentMap }
            dao.insertHypotheses(currentMap.values.map { it.toEntity() })
            
            // Extract evidence for provenance
            val newEvidenceItems = mutableListOf<EvidenceItem>()
            for (blip in blips) {
                val hyp = currentMap.values.find { it.primaryMac == blip.id || it.associatedMacs.contains(blip.id) }
                if (hyp != null) {
                    newEvidenceItems.add(EvidenceItem(
                        sessionId = sessionId,
                        type = EvidenceType.RSSI.name,
                        sourceEventId = blip.id,
                        sourceSensor = "Scanner",
                        timestampMs = blip.timestampMs,
                        measurement = "Signal Strength",
                        value = "${blip.rssi}",
                        unit = "dBm",
                        reliability = 0.8f,
                        confidence = 0.9f,
                        weight = 10f,
                        isSupporting = true,
                        analysisComponent = "DeviceIdentityEngine",
                        relatedDeviceId = hyp.id,
                        relatedAnomalyId = null,
                        relatedPatternId = null
                    ))
                }
            }
            if (newEvidenceItems.isNotEmpty()) {
                evidenceEngine.addEvidence(newEvidenceItems)
            }
        }
    }

    suspend fun mergeHypotheses(targetId: String, sourceId: String) {
        val currentMap = _hypotheses.value.toMutableMap()
        val target = currentMap[targetId] ?: return
        val source = currentMap[sourceId] ?: return

        // Combine fields
        val combinedMacs = target.associatedMacs + source.associatedMacs + source.primaryMac
        
        // Combine evidence lists elegantly
        val manualEvidence = IdentityEvidence("Manual physical correlation of targets", 15)
        val combinedEvidence = (target.evidence + source.evidence + manualEvidence)
            .distinctBy { it.description }
            .take(10)
            
        val calculatedScore = calculateConfidenceScore(combinedEvidence)

        val merged = target.copy(
            associatedMacs = combinedMacs,
            confidenceScorePercent = calculatedScore,
            evidence = combinedEvidence,
            lastSeenMs = maxOf(target.lastSeenMs, source.lastSeenMs),
            firstSeenMs = minOf(target.firstSeenMs, source.firstSeenMs),
            observationCount = target.observationCount + source.observationCount
        )

        // Remove source, update target
        currentMap.remove(sourceId)
        currentMap[targetId] = merged

        _hypotheses.update { currentMap }
        
        // Sync to database
        dao.deleteHypothesis(sourceId)
        dao.insertHypothesis(merged.toEntity())
    }

    private fun calculateConfidenceScore(evidence: List<IdentityEvidence>): Int {
        var probability = 50 // Baseline probability: 50%
        
        for (item in evidence) {
            if (item.isContradicting) {
                val weight = when {
                    item.description.contains("MAC address changed", ignoreCase = true) -> -20
                    item.description.contains("Randomized MAC", ignoreCase = true) || item.description.contains("RPA", ignoreCase = true) -> -10
                    item.description.contains("Temporary RSSI inconsistency", ignoreCase = true) -> -5
                    else -> item.impactScore.coerceAtMost(-5) // ensure negative impact
                }
                probability += weight
            } else {
                val weight = when {
                    item.description.contains("Exact MAC address match", ignoreCase = true) -> 15
                    item.description.contains("Spatial continuity", ignoreCase = true) -> 10
                    item.description.contains("Strong RF fingerprint correlation", ignoreCase = true) -> 12
                    item.description.contains("Distinct RF fingerprint captured", ignoreCase = true) -> 10
                    item.description.contains("Same manufacturer characteristics", ignoreCase = true) -> 8
                    item.description.contains("Manufacturer identified", ignoreCase = true) -> 8
                    item.description.contains("Consistent device protocol", ignoreCase = true) -> 8
                    item.description.contains("Similar transmission interval", ignoreCase = true) -> 10
                    item.description.contains("Consistent frequency behavior", ignoreCase = true) -> 5
                    item.description.contains("Initial observation", ignoreCase = true) -> 5
                    else -> 5
                }
                probability += weight
            }
        }
        
        return probability.coerceIn(0, 99) // Maximum probability to reach up to 99%
    }
    
    suspend fun clearIdentityData() {
        dao.clearAll()
        _hypotheses.update { emptyMap() }
    }
}
