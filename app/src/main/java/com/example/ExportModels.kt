package com.example

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InvestigationExport(
    val metadata: ExportMetadata,
    val configuration: ExportConfiguration,
    val hardwareCapabilities: List<String>,
    val sensorSourceInformation: List<String>,
    val provenanceInformation: ProvenanceInformation,
    val events: List<RfRecordedEventEntity>,
    val identityHypotheses: List<DeviceIdentityHypothesis>? = null,
    val anomalies: List<RfAnomalyEntity>? = null,
    val patterns: List<RfPatternEntity>? = null,
    val sessions: List<RfSessionEntity>? = null,
    val annotations: List<RfAnnotationEntity>? = null,
    val pcapReferences: List<PcapReference>? = null,
    val sdrReferences: List<SdrReference>? = null
)


@JsonClass(generateAdapter = true)
data class ProvenanceInformation(
    val generator: String,
    val securityHash: String?,
    val originDevice: String,
    val certificationStatus: String
)

@JsonClass(generateAdapter = true)data class ExportMetadata(
    val applicationVersion: String,
    val recordingStartTimeMs: Long,
    val recordingEndTimeMs: Long,
    val exportTimeMs: Long
)

@JsonClass(generateAdapter = true)
data class ExportConfiguration(
    val perimeterThresholdMeters: Float,
    val rssiAlertThresholdDbm: Int,
    val isRssiAlertEnabled: Boolean,
    val isPerimeterAlarmEnabled: Boolean,
    val stealthModeEnabled: Boolean,
    val activeScanMode: String
)

@JsonClass(generateAdapter = true)
data class PcapReference(
    val filename: String,
    val timestampMs: Long,
    val packetCount: Int,
    val interfaceName: String
)

@JsonClass(generateAdapter = true)
data class SdrReference(
    val filename: String,
    val timestampMs: Long,
    val sampleRateHz: Long,
    val centerFrequencyHz: Long,
    val bandwidthHz: Long,
    val hardware: String,
    val gainDb: Float,
    val antenna: String
)
