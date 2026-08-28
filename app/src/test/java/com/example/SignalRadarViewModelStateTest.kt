package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SignalRadarViewModelStateTest {

    private lateinit var context: Application
    private lateinit var viewModel: SignalRadarViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        viewModel = SignalRadarViewModel(context)
    }

    @Test
    fun testRangeSourceOfTruth() {
        // currentRadarRangeMeters is authoritative and changes sync across StateFlow and UiState
        viewModel.setMapRangeMeters(25.0f)
        assertEquals(25.0f, viewModel.currentRadarRangeMeters.value, 0.01f)
        assertEquals(25.0f, viewModel.uiState.value.currentRadarRangeMeters, 0.01f)
        assertEquals(25.0f, viewModel.uiState.value.mapRangeMeters, 0.01f)

        viewModel.setMapRangeMeters(50.0f)
        assertEquals(50.0f, viewModel.currentRadarRangeMeters.value, 0.01f)
        assertEquals(50.0f, viewModel.uiState.value.currentRadarRangeMeters, 0.01f)
    }

    @Test
    fun testTargetLockPersistence() {
        // Locking a target sets lockedTargetDeviceId and selectedTargetDeviceId
        viewModel.lockTarget("DEVICE_A")
        assertEquals("DEVICE_A", viewModel.uiState.value.lockedTargetDeviceId)
        assertEquals("DEVICE_A", viewModel.uiState.value.selectedTargetDeviceId)

        // Transient selection of another device (selectDevice) does not overwrite active locked target
        viewModel.selectDevice("DEVICE_B")
        assertEquals("DEVICE_B", viewModel.uiState.value.selectedDeviceId)
        assertEquals("DEVICE_A", viewModel.uiState.value.lockedTargetDeviceId)
        assertEquals("DEVICE_A", viewModel.uiState.value.selectedTargetDeviceId)

        // Clearing transient preview (e.g. tapping empty area) leaves locked target intact
        viewModel.selectDevice(null)
        assertNull(viewModel.uiState.value.selectedDeviceId)
        assertEquals("DEVICE_A", viewModel.uiState.value.lockedTargetDeviceId)
        assertEquals("DEVICE_A", viewModel.uiState.value.selectedTargetDeviceId)

        // Explicit unlock clears the target
        viewModel.unlockTarget()
        assertNull(viewModel.uiState.value.lockedTargetDeviceId)
        assertNull(viewModel.uiState.value.selectedTargetDeviceId)
    }

    @Test
    fun testSearchQueryAndMacFilterMaskState() {
        viewModel.setSearchQuery("ESP32")
        assertEquals("ESP32", viewModel.searchQuery.value)

        viewModel.setMacFilterMask("AA:BB:CC")
        assertEquals("AA:BB:CC", viewModel.macFilterMask.value)
    }
}
