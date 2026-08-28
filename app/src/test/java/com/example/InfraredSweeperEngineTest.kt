package com.example

import android.content.Context
import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class InfraredSweeperEngineTest {

    private lateinit var context: Context
    private lateinit var processor: InfraredLuminanceProcessor
    private lateinit var engine: InfraredSweeperEngine

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        processor = InfraredLuminanceProcessor(
            luminanceThreshold = 210,
            minBloomRadius = 1.0f,
            maxBloomRadius = 50.0f
        )
        engine = InfraredSweeperEngine(context, processor)
    }

    @Test
    fun `1 ambient monochrome frame maps to high-contrast grayscale bitmap`() {
        val width = 20
        val height = 20
        val total = width * height
        // Ambient background luminance around 60 (dark room)
        val ambientBuffer = ByteArray(total) { 60.toByte() }

        val (bitmap, targets) = processor.processMonochromeFrame(ambientBuffer, width, height, width)

        assertNotNull(bitmap)
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
        assertTrue("Ambient dark room should have no bloom targets", targets.isEmpty())

        // Check pixel value: grayscale ARGB for 60 is 0xFF3C3C3C (-12829636)
        val pixel = bitmap.getPixel(10, 10)
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        assertEquals(60, r)
        assertEquals(60, g)
        assertEquals(60, b)
    }

    @Test
    fun `2 high intensity IR emitter bloom maps to neon false-color and extracts centroid`() {
        val width = 40
        val height = 40
        val total = width * height
        val buffer = ByteArray(total) { 40.toByte() } // Ambient background

        // Inject simulated 850nm/940nm high-intensity night-vision LED cluster at center (20, 20)
        val emitterCenterX = 20
        val emitterCenterY = 20
        val radius = 3.0f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - emitterCenterX
                val dy = y - emitterCenterY
                if (sqrt((dx * dx + dy * dy).toDouble()) <= radius) {
                    buffer[y * width + x] = 250.toByte() // Strong IR bloom
                }
            }
        }

        val (bitmap, targets) = processor.processMonochromeFrame(buffer, width, height, width)

        assertEquals(1, targets.size)
        val target = targets[0]
        assertEquals(20.0f, target.xPixel, 1.0f)
        assertEquals(20.0f, target.yPixel, 1.0f)
        assertEquals(250, target.peakLuminance)
        assertTrue("Confidence must be strong (>0.7)", target.confidence > 0.7f)

        // Center pixel should be mapped to False-Color Neon Magenta
        val bloomPixel = bitmap.getPixel(20, 20)
        assertEquals(InfraredLuminanceProcessor.COLOR_NEON_MAGENTA, bloomPixel)
    }

    @Test
    fun `3 dynamic threshold adjustment alters bloom sensitivity`() {
        val width = 30
        val height = 30
        val total = width * height
        val buffer = ByteArray(total) { 50.toByte() }

        // Draw moderate glint with luminance = 190
        for (y in 14..16) {
            for (x in 14..16) {
                buffer[y * width + x] = 190.toByte()
            }
        }

        // At default threshold 210 -> Ignored
        processor.luminanceThreshold = 210
        val (_, targetsHigh) = processor.processMonochromeFrame(buffer, width, height, width)
        assertTrue("At threshold 210, luminance 190 must be ignored", targetsHigh.isEmpty())

        // Lower threshold to 180 -> Detected
        processor.luminanceThreshold = 180
        val (_, targetsLow) = processor.processMonochromeFrame(buffer, width, height, width)
        assertEquals(1, targetsLow.size)
        assertEquals(190, targetsLow[0].peakLuminance)
    }

    @Test
    fun `4 zero allocation luminance buffer extraction handles contiguous and strided buffers`() {
        val width = 8
        val height = 4
        val stridedWidth = 16 // Row stride with padding

        val directBuf = ByteBuffer.allocateDirect(stridedWidth * height)
        for (y in 0 until height) {
            for (x in 0 until stridedWidth) {
                directBuf.put(y * stridedWidth + x, (if (x < width) 100 + y * 10 + x else 0).toByte())
            }
        }

        val dest = ByteArray(width * height)
        engine.extractLuminanceBuffer(directBuf, dest, width, height, stridedWidth)

        // Validate first row
        assertEquals(100.toByte(), dest[0])
        assertEquals(107.toByte(), dest[7])
        // Validate second row unpacked without stride padding
        assertEquals(110.toByte(), dest[8])
        assertEquals(117.toByte(), dest[15])
    }

    @Test
    fun `5 engine direct frame ingestion updates reactive telemetry state flow`() {
        val width = 30
        val height = 30
        val total = width * height
        val frameData = ByteArray(total) { 30.toByte() }

        // Inject emitter spot
        for (y in 10..12) {
            for (x in 10..12) {
                frameData[y * width + x] = 245.toByte()
            }
        }

        val (bitmap, targets) = engine.processFrameDirectly(frameData, width, height)

        assertNotNull(bitmap)
        assertEquals(1, targets.size)

        val telemetry = engine.telemetry.value
        assertEquals(1L, telemetry.processedFrames)
        assertEquals(245, telemetry.peakLuminance)
        assertEquals(1, telemetry.bloomTargets.size)
        assertEquals(210, telemetry.thresholdLuminance)
    }

    @Test
    fun `6 processor resource cleanup releases memory cleanly`() {
        val width = 20
        val height = 20
        val buffer = ByteArray(width * height) { 50.toByte() }

        processor.processMonochromeFrame(buffer, width, height, width)
        processor.release()

        // Subsequent call re-allocates on demand without throwing
        val (recreatedBitmap, _) = processor.processMonochromeFrame(buffer, width, height, width)
        assertNotNull(recreatedBitmap)
        assertFalse(recreatedBitmap.isRecycled)
    }
}
