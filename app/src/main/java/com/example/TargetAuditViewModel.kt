package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TargetAuditViewModel : ViewModel() {

    private val _rangingResult = MutableStateFlow<TacticalRangingResult?>(null)
    val rangingResult: StateFlow<TacticalRangingResult?> = _rangingResult.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _auditResult = MutableStateFlow<DetailedTargetAudit?>(null)
    val auditResult: StateFlow<DetailedTargetAudit?> = _auditResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setRangingResult(result: TacticalRangingResult) {
        _rangingResult.value = result
    }

    fun initiateDeepAudit(aiGateway: TacticalAiGateway) {
        val currentResult = _rangingResult.value
        if (currentResult == null) {
            _errorMessage.value = "No measured target is selected. Select a detected device before auditing."
            return
        }

        _isAnalyzing.value = true
        _errorMessage.value = null
        _auditResult.value = null

        viewModelScope.launch {
            try {
                val signalType = when (currentResult.method) {
                    RangingMethod.BLE_CHANNEL_SOUNDING -> "BLE_CS"
                    RangingMethod.BLE_RSSI_ESTIMATE -> "BLE_RSSI"
                }
                val targetName = "Measured ranging target"
                val emitter = FlaggedThreatEmitter(
                    id = currentResult.targetMac,
                    name = targetName,
                    macAddress = currentResult.targetMac,
                    signalType = signalType,
                    rssiDbm = currentResult.rttOrRssiDb,
                    distanceMeters = currentResult.distanceMeters.toFloat(),
                    threatCategory = ThreatCategory.UNKNOWN_ANOMALOUS_NODE,
                    threatScore = 0,
                    riskSummary = "A measured radio target requires additional evidence before risk can be classified.",
                    recommendedAction = "Collect repeated measurements and inspect the device locally."
                )

                // Do not send an empty/unknown environment to the audit. The selected
                // ranging target is a real measured observation and must be included.
                val snapshot = RfEnvironmentSnapshot(
                    totalBlipsCount = 1,
                    activeBlips = emptyList(),
                    nearestBlip = null,
                    isRfJammingDetected = false,
                    isGnssSpoofingDetected = false,
                    isImsiAlertActive = false,
                    isUltrasonicAlertActive = false,
                    ultrasonicFreqHz = 0,
                    ultrasonicDb = 0f,
                    magneticFluxMicroTesla = 0f,
                    compassHeading = 0f,
                    breachCount = 0,
                    environmentDataAvailable = true
                )

                _auditResult.value = aiGateway.performTargetDeepAudit(emitter, snapshot)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "The measured target audit could not be completed."
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun clearResult() {
        _auditResult.value = null
        _errorMessage.value = null
    }
}
