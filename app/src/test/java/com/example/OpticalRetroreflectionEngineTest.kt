package com.example

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OpticalRetroreflectionEngineTest {

    private lateinit var filter: OpticalMorphologicalFilter
    private lateinit var tracker: OpticalTargetTracker
    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        filter = OpticalMorphologicalFilter(
            minLuminanceDelta = 40,
            minDiameterPixels = 2.0f,
            maxDiameterPixels = 15.0f,
            minCircularityRatio = 0.80f
        )
        tracker = OpticalTargetTracker(
            matchDistanceTolerancePixels = 15.0f,
            persistenceThresholdFrames = 5,
            maxMissedFrames = 3
        )
    }

    @Test
    fun `1 luminance differencing calculates absolute delta across Y plane correctly`() {
        val frameOn = byteArrayOf(100.toByte(), 250.toByte(), 30.toByte(), 0.toByte())
        val frameOff = byteArrayOf(50.toByte(), 50.toByte(), 200.toByte(), 0.toByte())
        val diff = ByteArray(4)

        filter.computeLuminanceDifference(frameOn, frameOff, diff, 4)

        assertEquals(50, diff[0].toInt() and 0xFF)
        assertEquals(200, diff[1].toInt() and 0xFF)
        assertEquals(170, diff[2].toInt() and 0xFF)
        assertEquals(0, diff[3].toInt() and 0xFF)
    }

    @Test
    fun `2 circular airy disk glint is detected and accepted by morphological filter`() {
        val width = 50
        val height = 50
        val diffBuffer = ByteArray(width * height)

        // Draw a synthetic 5x5 circular disk at center (25, 25)
        val centerX = 25
        val centerY = 25
        val radius = 2.5f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - centerX
                val dy = y - centerY
                if (sqrt((dx * dx + dy * dy).toDouble()) <= radius) {
                    diffBuffer[y * width + x] = 200.toByte() // Strong delta
                }
            }
        }

        val candidates = filter.extractGlintCandidates(diffBuffer, width, height, width)

        assertEquals(1, candidates.size)
        val target = candidates[0]
        assertEquals(25.0f, target.xPixel, 1.0f)
        assertEquals(25.0f, target.yPixel, 1.0f)
        assertTrue("Diameter must be within 2-15 px", target.diameterPixels in 2.0f..15.0f)
        assertTrue("Circularity must be >= 0.80 for disk", target.circularityRatio >= 0.80f)
        assertEquals(200, target.deltaLuminance)
        assertTrue("Confidence must be positive", target.confidence > 0.5f)
    }

    @Test
    fun `3 non-circular thin scratch is rejected by morphological circularity filter`() {
        val width = 60
        val height = 60
        val diffBuffer = ByteArray(width * height)

        // Draw a long thin scratch: 1 pixel wide, 25 pixels long at y = 30
        for (x in 10..35) {
            diffBuffer[30 * width + x] = 220.toByte()
        }

        val candidates = filter.extractGlintCandidates(diffBuffer, width, height, width)

        // Must be rejected because circularity of a line is far below 0.80 and diameter exceeds 15px
        assertTrue("Elongated scratch must be rejected", candidates.isEmpty())
    }

    @Test
    fun `4 oversized specular mirror or flat reflection is rejected by diameter bounds`() {
        val width = 100
        val height = 100
        val diffBuffer = ByteArray(width * height)

        // Draw a large 30x30 bright region
        for (y in 20..50) {
            for (x in 20..50) {
                diffBuffer[y * width + x] = 250.toByte()
            }
        }

        val candidates = filter.extractGlintCandidates(diffBuffer, width, height, width)
        assertTrue("Oversized reflection (diameter > 15px) must be rejected", candidates.isEmpty())
    }

    @Test
    fun `5 sub-threshold luminance delta is ignored`() {
        val width = 40
        val height = 40
        val diffBuffer = ByteArray(width * height)

        // Draw a circular glint with weak delta (25 < 40 threshold)
        for (y in 18..22) {
            for (x in 18..22) {
                diffBuffer[y * width + x] = 25.toByte()
            }
        }

        val candidates = filter.extractGlintCandidates(diffBuffer, width, height, width)
        assertTrue("Sub-threshold delta must be ignored", candidates.isEmpty())
    }

    @Test
    fun `6 target tracker accumulates persistence and triggers telephoto handoff after 5 frames`() {
        val candidate = OpticalGlintTarget(
            id = "TEMP_1",
            xPixel = 120.0f,
            yPixel = 150.0f,
            normalizedX = 0.25f,
            normalizedY = 0.35f,
            diameterPixels = 6.0f,
            circularityRatio = 0.92f,
            deltaLuminance = 180,
            confidence = 0.85f
        )

        // Cycles 1 to 5: Not yet ready for telephoto handoff (threshold = 5)
        for (frame in 1..5) {
            val (targets, handoff) = tracker.updateTracks(listOf(candidate))
            assertEquals(1, targets.size)
            assertEquals(frame, targets[0].persistenceFrameCount)
            assertFalse("Frames <= 5 must not be handoff ready", targets[0].isTelephotoHandoffReady)
            assertNull("Handoff target must be null before 6 frames", handoff)
        }

        // Cycle 6 (Persistence = 6 > 5): Telephoto handoff triggers
        val (targets6, handoff6) = tracker.updateTracks(listOf(candidate))
        assertEquals(1, targets6.size)
        assertEquals(6, targets6[0].persistenceFrameCount)
        assertTrue("Frame 6 must qualify for telephoto handoff", targets6[0].isTelephotoHandoffReady)
        assertNotNull("Active handoff target must be populated", handoff6)
        assertEquals(120.0f, handoff6!!.xPixel, 0.1f)
        assertEquals(150.0f, handoff6.yPixel, 0.1f)
    }

    @Test
    fun `7 optical strobe scanner end-to-end synthetic frame pair processing`() {
        val scanner = OpticalStrobeScanner(context, filter, tracker)

        val width = 100
        val height = 100
        val total = width * height

        val frameOn = ByteArray(total) { 30.toByte() }
        val frameOff = ByteArray(total) { 30.toByte() }

        // Inject retroreflective glint into frameOn
        for (y in 48..52) {
            for (x in 48..52) {
                frameOn[y * width + x] = 230.toByte() // Flash illuminated retroreflection
            }
        }

        // Process 6 consecutive strobe pairs to trigger handoff
        var finalResult: Pair<List<OpticalGlintTarget>, OpticalGlintTarget?>? = null
        for (i in 1..6) {
            finalResult = scanner.processFramePair(frameOn, frameOff, width, height)
        }

        assertNotNull(finalResult)
        assertEquals(1, finalResult!!.first.size)
        assertTrue(finalResult.first[0].isTelephotoHandoffReady)
        assertNotNull(finalResult.second)
        assertEquals(50.0f, finalResult.second!!.xPixel, 1.0f)
        assertEquals(50.0f, finalResult.second!!.yPixel, 1.0f)

        val state = scanner.targetState.value
        assertEquals(6L, state.processedFrameCount)
        assertNotNull(state.activeHandoffTarget)
    }

    @Test
    fun `8 zero allocation buffer extraction and boundary safety`() {
        val scanner = OpticalStrobeScanner(context, filter, tracker)
        val width = 10
        val height = 10
        val total = width * height
        val dest = ByteArray(total)

        val byteBuffer = java.nio.ByteBuffer.allocateDirect(total)
        for (i in 0 until total) {
            byteBuffer.put(i, (i + 5).toByte())
        }

        scanner.extractYPlaneToBuffer(byteBuffer, dest, width, height, width)
        assertEquals(5.toByte(), dest[0])
        assertEquals(14.toByte(), dest[9])
        assertEquals(104.toByte(), dest[99])
    }
}
