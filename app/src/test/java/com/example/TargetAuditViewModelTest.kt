package com.example

import org.junit.Assert.*
import org.junit.Test

class TargetAuditViewModelTest {

    @Test
    fun testRangingResultStateUpdates() {
        val viewModel = TargetAuditViewModel()
        assertNull(viewModel.rangingResult.value)

        val result = TacticalRangingResult(
            targetMac = "FF:AA:BB:CC:DD:EE",
            distanceMeters = 5.4,
            method = RangingMethod.BLE_CHANNEL_SOUNDING,
            confidenceScore = 0.95,
            quality = SignalQuality.HIGH,
            rttOrRssiDb = -12
        )

        viewModel.setRangingResult(result)
        assertEquals(result, viewModel.rangingResult.value)
        assertEquals("FF:AA:BB:CC:DD:EE", viewModel.rangingResult.value?.targetMac)
        assertEquals(5.4, viewModel.rangingResult.value?.distanceMeters ?: 0.0, 0.001)
    }

    @Test
    fun testClearResultResetsData() {
        val viewModel = TargetAuditViewModel()
        val result = TacticalRangingResult(
            targetMac = "FF:AA:BB:CC:DD:EE",
            distanceMeters = 5.4,
            method = RangingMethod.BLE_CHANNEL_SOUNDING,
            confidenceScore = 0.95,
            quality = SignalQuality.HIGH,
            rttOrRssiDb = -12
        )

        viewModel.setRangingResult(result)
        assertNull(viewModel.auditResult.value)

        viewModel.clearResult()
        assertNull(viewModel.auditResult.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun testAcquisitionMethodFormatting() {
        val methodCS = RangingMethod.BLE_CHANNEL_SOUNDING
        val methodRSSI = RangingMethod.BLE_RSSI_ESTIMATE

        assertEquals("BLE_CHANNEL_SOUNDING", methodCS.name)
        assertEquals("BLE_RSSI_ESTIMATE", methodRSSI.name)
    }
}
