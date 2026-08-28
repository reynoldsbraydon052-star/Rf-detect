package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.math.sin

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EmfAnomalyEngineTest {

    private lateinit var processor: EmfSignalProcessor
    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        processor = EmfSignalProcessor(
            windowSize = 50,
            emaAlpha = 0.01f,
            staticDcThresholdUtd = 15.0f,
            dynamicAcRippleThresholdUtd = 2.0f
        )
    }

    @Test
    fun `1 total magnitude calculation matches Euclidean norm on known vectors`() {
        // (3, 4, 0) -> 5.0
        val mag1 = processor.calculateMagnitude(3.0f, 4.0f, 0.0f)
        assertEquals(5.0f, mag1, 0.001f)

        // (10, 20, 20) -> sqrt(100 + 400 + 400) = sqrt(900) = 30.0
        val mag2 = processor.calculateMagnitude(10.0f, 20.0f, 20.0f)
        assertEquals(30.0f, mag2, 0.001f)

        // (0, 0, 48.5) -> 48.5
        val mag3 = processor.calculateMagnitude(0.0f, 0.0f, 48.5f)
        assertEquals(48.5f, mag3, 0.001f)

        // Zero vector
        val magZero = processor.calculateMagnitude(0.0f, 0.0f, 0.0f)
        assertEquals(0.0f, magZero, 0.001f)
    }

    @Test
    fun `2 EMA baseline converges predictably toward synthetic stepped inputs`() {
        processor.resetBaseline(50.0f)
        assertEquals(50.0f, processor.updateBaseline(50.0f), 0.001f)

        // Step up to 100 µT for 100 samples with alpha = 0.01
        var baseline = 50.0f
        for (i in 1..100) {
            baseline = processor.updateBaseline(100.0f)
        }

        // Expected analytical value after 100 steps: 100 - (100 - 50) * (0.99)^100 ≈ 100 - 50 * 0.3660 = 81.698 µT
        assertTrue("Baseline should adapt towards 100 µT", baseline > 80.0f && baseline < 83.5f)

        // Step back down to 50 µT for 400 steps
        for (i in 1..400) {
            baseline = processor.updateBaseline(50.0f)
        }
        // Baseline should return close to 50.0 µT
        assertTrue("Baseline should return towards 50 µT", baseline < 51.5f && baseline >= 50.0f)
    }

    @Test
    fun `3 rolling MAD distinguishes static DC offset from dynamic AC ripple`() {
        processor.resetBaseline(45.0f)

        // Scenario A: Static DC shift (constant 90 µT without micro-oscillation)
        var dcState: EmfTelemetryState? = null
        for (i in 1..60) {
            dcState = processor.processRaw(x = 0f, y = 0f, z = 90.0f)
        }

        assertNotNull(dcState)
        assertEquals(0.0f, dcState!!.jitterRippleUtd, 0.01f)
        assertFalse("Pure DC shift must not trigger dynamic AC", dcState.isDynamicAcDetected)
        assertTrue("High Delta with zero jitter must be flagged as Static DC anomaly", dcState.isStaticDcAnomaly)

        // Scenario B: Dynamic AC Ripple (50 µT baseline carrier + 10 µT 60Hz oscillation)
        processor.resetBaseline(50.0f)
        var acState: EmfTelemetryState? = null
        for (i in 1..100) {
            val ripple = (10.0f * sin(i * 0.5f))
            val z = 50.0f + ripple
            acState = processor.processRaw(x = 0f, y = 0f, z = z)
        }

        assertNotNull(acState)
        assertTrue("Oscillating ripple should produce elevated MAD jitter (>= 2.0)", acState!!.jitterRippleUtd >= 2.0f)
        assertTrue("Elevated MAD jitter must trigger dynamic AC detection", acState.isDynamicAcDetected)
        assertTrue("Dynamic AC detection should boost normalized threat score", acState.normalizedEmfThreatScore > 0.35f)
    }

    @Test
    fun `4 processor safely rejects NaN and infinite inputs without crashing`() {
        val nanMag = processor.calculateMagnitude(Float.NaN, 10f, 20f)
        assertEquals(0.0f, nanMag, 0.001f)

        val infMag = processor.calculateMagnitude(Float.POSITIVE_INFINITY, 0f, 0f)
        assertEquals(0.0f, infMag, 0.001f)

        val negInfMag = processor.calculateMagnitude(0f, Float.NEGATIVE_INFINITY, 0f)
        assertEquals(0.0f, negInfMag, 0.001f)

        val stateFromNan = processor.processRaw(Float.NaN, Float.NaN, Float.NaN)
        assertNull("Corrupted NaN vector must return null state", stateFromNan)

        val stateFromInf = processor.processRaw(Float.POSITIVE_INFINITY, 20f, 30f)
        assertNull("Infinite input vector must return null state", stateFromInf)
    }

    @Test
    fun `5 empty buffer and initial state boundaries produce safe defaults`() {
        val freshProcessor = EmfSignalProcessor()
        val jitterOnEmpty = freshProcessor.updateWindowAndComputeJitter(45.0f)
        assertEquals("Single sample should yield 0.0 MAD jitter", 0.0f, jitterOnEmpty, 0.001f)

        val initialScore = freshProcessor.calculateThreatScore(0.0f, 0.0f, false)
        assertEquals(0.0f, initialScore, 0.001f)

        val maxThreat = freshProcessor.calculateThreatScore(500.0f, 50.0f, true)
        assertEquals(1.0f, maxThreat, 0.001f)
    }

    @Test
    fun `6 engine manages lifecycle and listener cancellation cleanly`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val scanner = EmfSensorScanner(context)
        val engine = EmfAnomalyEngine(
            scanner = scanner,
            processor = processor,
            dispatcher = testDispatcher
        )

        assertFalse(engine.isRunning)

        engine.start(this)
        assertTrue(engine.isRunning)

        // Ingest synthetic samples directly
        engine.ingestRaw(x = 0f, y = 0f, z = 48f, timestampNs = 1_000_000_000L)
        val state1 = engine.telemetryState.value
        assertEquals(48.0f, state1.rawMagnitudeUtd, 0.01f)

        engine.stop()
        assertFalse(engine.isRunning)

        engine.reset()
        assertEquals(0.0f, engine.telemetryState.value.rawMagnitudeUtd, 0.001f)
    }
}
