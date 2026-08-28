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
        val currentResult = _rangingResult.value ?: return
        
        _isAnalyzing.value = true
        _errorMessage.value = null
        _auditResult.value = null

        viewModelScope.launch {
            try {
                val emitter = FlaggedThreatEmitter(
                    id = currentResult.targetMac,
                    name = "Tracked Ranging Target",
                    macAddress = currentResult.targetMac,
                    signalType = if (currentResult.method == RangingMethod.BLE_CHANNEL_SOUNDING) "BLE_CS" else "BLE_RSSI",
                    rssiDbm = currentResult.rttOrRssiDb,
                    distanceMeters = currentResult.distanceMeters.toFloat(),
                    threatCategory = ThreatCategory.UNKNOWN_ANOMALOUS_NODE,
                    threatScore = if (currentResult.quality == SignalQuality.HIGH) 35 else 70,
                    riskSummary = "Active target captured during high-frequency physical range detection.",
                    recommendedAction = "Verify link layer vulnerability vectors immediately."
                )

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
                    magneticFluxMicroTesla = 48f,
                    compassHeading = 0f,
                    breachCount = 0
                )

                val result = aiGateway.performTargetDeepAudit(emitter, snapshot)
                _auditResult.value = result
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Connection fault with multi-model AI Gateway."
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
