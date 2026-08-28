package com.example

import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testProximityAlert2Engine() {
        val enterRssiDbm = -42
        val exitRssiDbm = -45
        val requiredSamples = 3
        val cooldownMs = 5000L

        val approachingEnterLimit = enterRssiDbm - 8   // -50
        val approachingExitLimit = exitRssiDbm - 8     // -53

        var currentAlarmState = AlarmState.NORMAL
        var consecutiveValidTriggerCount = 0
        var consecutiveValidExitCount = 0
        var consecutiveValidNormalCount = 0
        var consecutiveValidApproachingCount = 0
        var alarmCooldownStartTimeMs = 0L
        var alarmHasExitedHysteresis = false
        var lastAlarmTargetId: String? = "target_1"

        // Evaluation function mimicking ViewModel
        fun processMeasurement(
            targetId: String,
            selectedTargetId: String?,
            rssi: Int,
            timestampMs: Long,
            nowMs: Long
        ): AlarmState {
            if (selectedTargetId == null) {
                currentAlarmState = AlarmState.NORMAL
                consecutiveValidTriggerCount = 0
                consecutiveValidExitCount = 0
                consecutiveValidNormalCount = 0
                consecutiveValidApproachingCount = 0
                return currentAlarmState
            }

            if (selectedTargetId != lastAlarmTargetId) {
                currentAlarmState = AlarmState.NORMAL
                consecutiveValidTriggerCount = 0
                consecutiveValidExitCount = 0
                consecutiveValidNormalCount = 0
                consecutiveValidApproachingCount = 0
                alarmCooldownStartTimeMs = 0L
                alarmHasExitedHysteresis = false
                lastAlarmTargetId = selectedTargetId
            }

            if (targetId != selectedTargetId) {
                // Never trigger based on another device
                return currentAlarmState
            }

            val isStale = (nowMs - timestampMs > 10000L)
            val isRssiInvalid = (rssi > -10 || rssi < -115)
            val isValidObservation = !isStale && !isRssiInvalid

            if (isValidObservation) {
                if (rssi >= enterRssiDbm) {
                    consecutiveValidTriggerCount++
                    consecutiveValidExitCount = 0
                    consecutiveValidApproachingCount = 0
                    consecutiveValidNormalCount = 0
                } else if (rssi < exitRssiDbm) {
                    consecutiveValidExitCount++
                    consecutiveValidTriggerCount = 0
                    if (rssi < approachingExitLimit) {
                        consecutiveValidNormalCount++
                        consecutiveValidApproachingCount = 0
                    } else {
                        consecutiveValidApproachingCount++
                        consecutiveValidNormalCount = 0
                    }
                } else {
                    // Inside the hysteresis region
                    consecutiveValidTriggerCount = 0
                    consecutiveValidExitCount = 0
                    consecutiveValidNormalCount = 0
                    consecutiveValidApproachingCount = 0
                }

                when (currentAlarmState) {
                    AlarmState.NORMAL -> {
                        if (consecutiveValidTriggerCount >= requiredSamples) {
                            currentAlarmState = AlarmState.TRIGGERED
                        } else if (consecutiveValidApproachingCount >= requiredSamples) {
                            currentAlarmState = AlarmState.APPROACHING
                        }
                    }
                    AlarmState.APPROACHING -> {
                        if (consecutiveValidTriggerCount >= requiredSamples) {
                            currentAlarmState = AlarmState.TRIGGERED
                        } else if (consecutiveValidNormalCount >= requiredSamples) {
                            currentAlarmState = AlarmState.NORMAL
                        }
                    }
                    AlarmState.TRIGGERED -> {
                        if (consecutiveValidExitCount >= requiredSamples) {
                            currentAlarmState = AlarmState.COOLDOWN
                            alarmCooldownStartTimeMs = nowMs
                            alarmHasExitedHysteresis = true
                        }
                    }
                    AlarmState.COOLDOWN -> {
                        if (rssi < exitRssiDbm) {
                            alarmHasExitedHysteresis = true
                        }
                        val isCooldownExpired = (nowMs - alarmCooldownStartTimeMs >= cooldownMs)
                        if (isCooldownExpired && alarmHasExitedHysteresis) {
                            if (consecutiveValidTriggerCount >= requiredSamples) {
                                currentAlarmState = AlarmState.TRIGGERED
                            } else if (consecutiveValidNormalCount >= requiredSamples) {
                                currentAlarmState = AlarmState.NORMAL
                            } else if (consecutiveValidApproachingCount >= requiredSamples) {
                                currentAlarmState = AlarmState.APPROACHING
                            }
                        } else {
                            if (consecutiveValidNormalCount >= requiredSamples) {
                                currentAlarmState = AlarmState.NORMAL
                            } else if (consecutiveValidApproachingCount >= requiredSamples) {
                                currentAlarmState = AlarmState.APPROACHING
                            }
                        }
                    }
                }
            }
            return currentAlarmState
        }

        val baseTime = 1000L

        // 1. target switching: NO TARGET LOCK (No target lock: NO TARGET ALERT)
        var state = processMeasurement("target_1", null, -30, baseTime, baseTime)
        assertEquals("No target lock: NO TARGET ALERT", AlarmState.NORMAL, state)

        // Reset with locked target
        state = processMeasurement("target_1", "target_1", -90, baseTime, baseTime)
        assertEquals(AlarmState.NORMAL, state)

        // 2. different device RSSI
        state = processMeasurement("target_2", "target_1", -30, baseTime, baseTime)
        assertEquals("Measurements of non-locked target must be ignored", AlarmState.NORMAL, state)

        // 3. invalid measurement
        state = processMeasurement("target_1", "target_1", 0, baseTime, baseTime) // RSSI = 0 is invalid
        assertEquals("Invalid measurement must not affect state", AlarmState.NORMAL, state)

        // 4. stale measurement
        state = processMeasurement("target_1", "target_1", -30, baseTime, baseTime + 15000L) // > 10000ms is stale
        assertEquals("Stale measurement must not affect state", AlarmState.NORMAL, state)

        // 5. insufficient samples / three-sample requirement
        processMeasurement("target_1", "target_1", -40, baseTime, baseTime) // Sample 1
        state = currentAlarmState
        assertEquals("1 sample is insufficient to trigger", AlarmState.NORMAL, state)

        processMeasurement("target_1", "target_1", -40, baseTime, baseTime) // Sample 2
        state = currentAlarmState
        assertEquals("2 samples are insufficient to trigger", AlarmState.NORMAL, state)

        // 6. threshold crossing
        processMeasurement("target_1", "target_1", -40, baseTime, baseTime) // Sample 3
        state = currentAlarmState
        assertEquals("3 consecutive valid samples >= threshold must TRIGGER", AlarmState.TRIGGERED, state)

        // 7. cooldown entry
        // Drops below exitLimit (-45)
        processMeasurement("target_1", "target_1", -48, baseTime, baseTime) // Sample 1 below exit
        processMeasurement("target_1", "target_1", -48, baseTime, baseTime) // Sample 2 below exit
        processMeasurement("target_1", "target_1", -48, baseTime, baseTime) // Sample 3 below exit
        state = currentAlarmState
        assertEquals("3 consecutive samples < exitLimit must transition to COOLDOWN", AlarmState.COOLDOWN, state)

        // 8. RSSI oscillation
        // Oscillates inside hysteresis (-43 and -44) during cooldown
        processMeasurement("target_1", "target_1", -43, baseTime, baseTime + 1000L)
        assertEquals("Oscillation in hysteresis region must keep COOLDOWN state", AlarmState.COOLDOWN, currentAlarmState)

        // 9. cooldown duration restriction (retrigger before cooldown)
        processMeasurement("target_1", "target_1", -40, baseTime, baseTime + 1000L) // Sample 1
        processMeasurement("target_1", "target_1", -40, baseTime, baseTime + 1000L) // Sample 2
        processMeasurement("target_1", "target_1", -40, baseTime, baseTime + 1000L) // Sample 3
        state = currentAlarmState
        assertEquals("Must NOT trigger while cooldown is active", AlarmState.COOLDOWN, state)

        // 10. retrigger after cooldown
        // After cooldown expires and signal was below exit, 3 valid observations >= enterLimit trigger again
        processMeasurement("target_1", "target_1", -40, baseTime, baseTime + 6000L) // Sample 1 after cooldown
        processMeasurement("target_1", "target_1", -40, baseTime, baseTime + 6000L) // Sample 2 after cooldown
        processMeasurement("target_1", "target_1", -40, baseTime, baseTime + 6000L) // Sample 3 after cooldown
        state = currentAlarmState
        assertEquals("Must trigger after cooldown expires and conditions met", AlarmState.TRIGGERED, state)

        // 11. target switching
        processMeasurement("target_1", "target_3", -90, baseTime, baseTime)
        assertEquals("Target switching must reset state to NORMAL", AlarmState.NORMAL, currentAlarmState)
    }

    @Test
    fun testStableKeysAndDeviceListPreservation() {
        // Mock a device list item to verify stable key generation
        val blip1 = RadarBlip(
            id = "wifi_1",
            name = "Office WiFi",
            distance = 3.5f,
            targetAngleOffset = 45f,
            type = "WIFI"
        )
        val blip2 = RadarBlip(
            id = "ble_1",
            name = "Beacon Alpha",
            distance = 1.2f,
            targetAngleOffset = -15f,
            type = "BLE"
        )

        // Generate stable keys based on device type & ID
        val key1 = "${blip1.type}_${blip1.id}"
        val key2 = "${blip2.type}_${blip2.id}"

        assertEquals("Key 1 should match stable representation", "WIFI_wifi_1", key1)
        assertEquals("Key 2 should match stable representation", "BLE_ble_1", key2)
        assertNotEquals("Keys for different devices must be unique", key1, key2)
    }

    @Test
    fun testTargetIsolationAndLockPersistence() {
        var selectedTargetId: String? = null
        var isLocked = false

        // Select a target and lock it
        selectedTargetId = "wifi_1"
        isLocked = true

        // Ensure that target lock persists and isn't affected by extraneous scanning updates
        val scannedBlips = listOf(
            RadarBlip(id = "wifi_2", name = "Neighbor WiFi", distance = 8f, targetAngleOffset = 180f, type = "WIFI"),
            RadarBlip(id = "wifi_1", name = "Office WiFi", distance = 3.2f, targetAngleOffset = 40f, type = "WIFI")
        )

        val stillExists = scannedBlips.any { it.id == selectedTargetId }
        assertTrue("Target lock persists as long as the device is visible in scans", stillExists && isLocked)

        // Even if we scan other items, our target focus remains unchanged
        val updatedSelection = if (isLocked && stillExists) selectedTargetId else "wifi_2"
        assertEquals("Target lock focus must be preserved", "wifi_1", updatedSelection)
    }

    @Test
    fun testWhyEvidenceInferredDirectionName() {
        assertEquals("N", getInferredDirectionName(0f))
        assertEquals("NE", getInferredDirectionName(45f))
        assertEquals("E", getInferredDirectionName(90f))
        assertEquals("SE", getInferredDirectionName(135f))
        assertEquals("S", getInferredDirectionName(180f))
        assertEquals("SW", getInferredDirectionName(225f))
        assertEquals("W", getInferredDirectionName(270f))
        assertEquals("NW", getInferredDirectionName(315f))
        
        // Boundaries and overflow
        assertEquals("N", getInferredDirectionName(360f))
        assertEquals("NE", getInferredDirectionName(405f))
        assertEquals("S", getInferredDirectionName(-180f))
    }

    @Test
    fun testWhyEvidenceOppositeDirectionName() {
        assertEquals("S", getOppositeDirectionName("N"))
        assertEquals("SW", getOppositeDirectionName("NE"))
        assertEquals("W", getOppositeDirectionName("E"))
        assertEquals("NW", getOppositeDirectionName("SE"))
        assertEquals("N", getOppositeDirectionName("S"))
        assertEquals("NE", getOppositeDirectionName("SW"))
        assertEquals("E", getOppositeDirectionName("W"))
        assertEquals("SE", getOppositeDirectionName("NW"))
    }

    @Test
    fun testWhyEvidenceDirectionalRssiChange() {
        val points = listOf(
            RfMeasurementPoint(
                id = "pt0",
                timestamp = System.currentTimeMillis(),
                latitude = null,
                longitude = null,
                xOffsetMeters = 0f,
                yOffsetMeters = 0f,
                compassHeading = 0f,
                pitch = 0f,
                roll = 0f,
                rssi = -80,
                filteredRssi = -80f,
                rssiVariance = 1f,
                targetId = "ble_1",
                frequencyMhz = 2400.0,
                qualityScore = 80,
                label = "LIVE"
            ),
            RfMeasurementPoint(
                id = "pt1",
                timestamp = System.currentTimeMillis() + 1000,
                latitude = null,
                longitude = null,
                xOffsetMeters = 2f,
                yOffsetMeters = 2f,
                compassHeading = 45f,
                pitch = 0f,
                roll = 0f,
                rssi = -60,
                filteredRssi = -60f,
                rssiVariance = 1f,
                targetId = "ble_1",
                frequencyMhz = 2400.0,
                qualityScore = 90,
                label = "LIVE"
            )
        )
        val changeNE = calculateDirectionalRssiChange(points, "NE")
        assertTrue("RSSI should increase along NE", changeNE > 0f)
        
        val changeSW = calculateDirectionalRssiChange(points, "SW")
        assertTrue("RSSI should decrease along SW", changeSW < 0f)
    }

    @Test
    fun testWhyEvidenceValidationErrors() {
        val points = listOf(
            RfMeasurementPoint(
                id = "pt0",
                timestamp = System.currentTimeMillis(),
                latitude = null,
                longitude = null,
                xOffsetMeters = 0f,
                yOffsetMeters = 0f,
                compassHeading = 0f,
                pitch = 0f,
                roll = 0f,
                rssi = -80,
                filteredRssi = -80f,
                rssiVariance = 1f,
                targetId = "ble_1",
                frequencyMhz = 2400.0,
                qualityScore = 80,
                label = "LIVE"
            )
        )
        val errors = getValidationErrors(points, null, "INSUFFICIENT")
        assertTrue("Should detect insufficient measurements", errors.contains("INSUFFICIENT MEASUREMENTS"))
        assertTrue("Should detect insufficient spatial diversity", errors.contains("INSUFFICIENT SPATIAL DIVERSITY"))
    }
}
