package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class IdentityConfidenceLevel(val displayName: String) {
    VERY_WEAK("Very weak relationship"),
    POSSIBLE("Possible relationship"),
    PROBABLE("Probable relationship"),
    STRONG("Strong relationship"),
    VERY_STRONG("Very strong relationship")
}

data class IdentityEvidence(
    val description: String,
    val impactScore: Int, // Positive or negative
    val isContradicting: Boolean = impactScore < 0
)

@Entity(tableName = "device_identity_hypothesis")
data class DeviceIdentityEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val primaryMac: String,
    val associatedMacsJson: String, // JSON list of MACs/IDs
    val confidenceScorePercent: Int, // 0 to 100
    val supportingEvidenceJson: String, // JSON list of descriptions
    val contradictingEvidenceJson: String, // JSON list of descriptions
    val firstSeenMs: Long,
    val lastSeenMs: Long,
    val observationCount: Int,
    val primaryDeviceType: String,
    val primaryOUI: String?,
    val hardwareCategory: String = "Unknown Category",
    val powerClass: String = "Unknown Power Class",
    val protocolFingerprint: String = "None Detected",
    val securityProfile: String = "Unknown Security Profile",
    val semanticName: String? = null,
    val semanticAppearance: String? = null,
    val semanticEcosystem: String? = null
)

data class DeviceIdentityHypothesis(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val primaryMac: String,
    val associatedMacs: Set<String>,
    val confidenceScorePercent: Int,
    val evidence: List<IdentityEvidence>,
    val firstSeenMs: Long,
    val lastSeenMs: Long,
    val observationCount: Int,
    val primaryDeviceType: String,
    val primaryOUI: String?,
    val hardwareCategory: String = "Unknown Category",
    val powerClass: String = "Unknown Power Class",
    val protocolFingerprint: String = "None Detected",
    val securityProfile: String = "Unknown Security Profile",
    val semanticProfile: SemanticDeviceProfile? = null,
    val semanticName: String? = null,
    val semanticAppearance: String? = null,
    val semanticEcosystem: String? = null
) {
    val displayName: String
        get() {
            if (!semanticName.isNullOrBlank()) return semanticName
            if (!semanticEcosystem.isNullOrBlank()) return semanticEcosystem
            if (!semanticAppearance.isNullOrBlank()) return semanticAppearance
            if (semanticProfile?.localName?.isNotBlank() == true) return semanticProfile.localName
            if (semanticProfile?.proprietaryEcosystemType?.isNotBlank() == true) return semanticProfile.proprietaryEcosystemType
            if (semanticProfile?.appearanceDisplayName?.isNotBlank() == true) return semanticProfile.appearanceDisplayName
            if (!primaryOUI.isNullOrBlank() && primaryOUI != "Masked Platform Transmitter") return "$primaryOUI Device"
            val clean = MacSanitizer.sanitize(primaryMac)
            if (clean == "MASKED_BY_PLATFORM") return "Masked Transceiver"
            return "Target [${clean.takeLast(8)}]"
        }

    val manualState: HardwareIdentificationManualState
        get() = HardwareIdentificationManualState.fromProfile(
            macAddress = primaryMac,
            fallbackName = semanticName ?: primaryOUI,
            ouiVendor = primaryOUI,
            profile = semanticProfile
        )
    val confidenceLevel: IdentityConfidenceLevel
        get() = when {
            confidenceScorePercent <= 25 -> IdentityConfidenceLevel.VERY_WEAK
            confidenceScorePercent <= 50 -> IdentityConfidenceLevel.POSSIBLE
            confidenceScorePercent <= 75 -> IdentityConfidenceLevel.PROBABLE
            confidenceScorePercent <= 90 -> IdentityConfidenceLevel.STRONG
            else -> IdentityConfidenceLevel.VERY_STRONG
        }

    val supportingEvidence: List<IdentityEvidence>
        get() = evidence.filter { !it.isContradicting }
        
    val contradictingEvidence: List<IdentityEvidence>
        get() = evidence.filter { it.isContradicting }
        
    fun toEntity(): DeviceIdentityEntity {
        return DeviceIdentityEntity(
            id = id,
            sessionId = sessionId,
            primaryMac = primaryMac,
            associatedMacsJson = associatedMacs.joinToString(","), // Simplified for demo, could use Moshi
            confidenceScorePercent = confidenceScorePercent,
            supportingEvidenceJson = supportingEvidence.joinToString("||") { it.description },
            contradictingEvidenceJson = contradictingEvidence.joinToString("||") { it.description },
            firstSeenMs = firstSeenMs,
            lastSeenMs = lastSeenMs,
            observationCount = observationCount,
            primaryDeviceType = primaryDeviceType,
            primaryOUI = primaryOUI,
            hardwareCategory = hardwareCategory,
            powerClass = powerClass,
            protocolFingerprint = protocolFingerprint,
            securityProfile = securityProfile,
            semanticName = semanticName ?: semanticProfile?.localName,
            semanticAppearance = semanticAppearance ?: semanticProfile?.appearanceDisplayName,
            semanticEcosystem = semanticEcosystem ?: semanticProfile?.proprietaryEcosystemType
        )
    }
    
    companion object {
        fun fromEntity(entity: DeviceIdentityEntity): DeviceIdentityHypothesis {
            val associated = if (entity.associatedMacsJson.isEmpty()) emptySet() else entity.associatedMacsJson.split(",").toSet()
            val supporting = if (entity.supportingEvidenceJson.isEmpty()) emptyList() else entity.supportingEvidenceJson.split("||").map { IdentityEvidence(it, 10, false) }
            val contradicting = if (entity.contradictingEvidenceJson.isEmpty()) emptyList() else entity.contradictingEvidenceJson.split("||").map { IdentityEvidence(it, -10, true) }
            
            return DeviceIdentityHypothesis(
                id = entity.id,
                sessionId = entity.sessionId,
                primaryMac = entity.primaryMac,
                associatedMacs = associated,
                confidenceScorePercent = entity.confidenceScorePercent,
                evidence = supporting + contradicting,
                firstSeenMs = entity.firstSeenMs,
                lastSeenMs = entity.lastSeenMs,
                observationCount = entity.observationCount,
                primaryDeviceType = entity.primaryDeviceType,
                primaryOUI = entity.primaryOUI,
                hardwareCategory = entity.hardwareCategory,
                powerClass = entity.powerClass,
                protocolFingerprint = entity.protocolFingerprint,
                securityProfile = entity.securityProfile,
                semanticName = entity.semanticName,
                semanticAppearance = entity.semanticAppearance,
                semanticEcosystem = entity.semanticEcosystem
            )
        }
    }
}
