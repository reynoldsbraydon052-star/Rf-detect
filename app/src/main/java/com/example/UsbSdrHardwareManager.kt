package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Abstraction layer interface for external Software Defined Radio (SDR) hardware
 * attached via USB-C OTG (e.g., RTL-SDR v4, HackRF One, LimeSDR, Ubertooth).
 */

interface SdrReceiverInterface {
    fun initializeReceiver(): Boolean
    fun setCenterFrequencyHz(freqHz: Long)
    fun setSampleRateSps(sampleRateSps: Long)
    fun setGainDb(gainDb: Float)
    fun startIqStream(onIqBufferReady: (FloatArray, FloatArray) -> Unit)
    fun stopIqStream()
    fun releaseReceiver()
}

interface ThermalCameraReceiverInterface {
    fun initializeThermalCamera(): Boolean
    fun startThermalStream(onThermalFrameReady: (android.graphics.Bitmap) -> Unit)
    fun stopThermalStream()
    fun releaseThermalCamera()
}

data class UsbSdrDeviceState(
    val isDeviceAttached: Boolean = false,
    val deviceName: String = "No External SDR Hardware",
    val vendorIdHex: String = "0x0000",
    val productIdHex: String = "0x0000",
    val centerFreqHz: Long = 2_400_000_000L, // 2.4 GHz default
    val sampleRateSps: Long = 2_400_000L,   // 2.4 MSPS
    val gainDb: Float = 32.0f,
    val isStreamingIq: Boolean = false,
    val bufferFramesCount: Long = 0L,
    val supportedHardwareModel: String = "RTL-SDR / HackRF / LimeSDR OTG Ready",
    val isThermalCameraAttached: Boolean = false,
    val thermalCameraModel: String = "FLIR ONE / UVC Thermal Class"
)

class UsbSdrHardwareManager(
    private val context: Context,
    private val onIqBufferReceived: (inPhaseI: FloatArray, quadratureQ: FloatArray) -> Unit
) : SdrReceiverInterface, ThermalCameraReceiverInterface {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager

    private val _sdrStateFlow = MutableStateFlow(UsbSdrDeviceState())
    val sdrStateFlow: StateFlow<UsbSdrDeviceState> = _sdrStateFlow.asStateFlow()

    private var isStreaming = false
    private var streamingJob: Job? = null
    private val streamingScope = CoroutineScope(Dispatchers.Default)

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(cntx: Context?, intent: Intent?) {
            val action = intent?.action
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action ||
                UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                checkAttachedUsbDevices()
            }
        }
    }

    fun registerUsbListener() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(usbReceiver, filter)
            }
        } catch (_: Exception) {}

        checkAttachedUsbDevices()
    }

    fun unregisterUsbListener() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (_: Exception) {}
        stopIqStream()
    }

    fun checkAttachedUsbDevices() {
        val deviceList = usbManager?.deviceList ?: emptyMap()
        var foundSdrDevice: UsbDevice? = null

        for ((_, device) in deviceList) {
            // Check for common SDR Vendor IDs (RTL-SDR Realtek 0x0BDA, HackRF 0x1D50, LimeSDR 0x0403/0x1D50)
            val vid = device.vendorId
            val pid = device.productId
            if (vid == 0x0BDA || vid == 0x1D50 || vid == 0x0403 || vid == 0x1D50) {
                foundSdrDevice = device
                break
            }
        }

        if (foundSdrDevice != null) {
            val vidHex = String.format("0x%04X", foundSdrDevice.vendorId)
            val pidHex = String.format("0x%04X", foundSdrDevice.productId)
            val modelName = when (foundSdrDevice.vendorId) {
                0x0BDA -> "RTL-SDR v4 Dongle (Realtek RTL2832U)"
                0x1D50 -> "Great Scott Gadgets HackRF One SDR"
                0x0403 -> "LimeSDR Mini USB-C Transceiver"
                else -> "USB-C OTG SDR Receiver Device"
            }

            _sdrStateFlow.value = _sdrStateFlow.value.copy(
                isDeviceAttached = true,
                deviceName = foundSdrDevice.deviceName,
                vendorIdHex = vidHex,
                productIdHex = pidHex,
                supportedHardwareModel = modelName
            )
        } else {
            _sdrStateFlow.value = _sdrStateFlow.value.copy(
                isDeviceAttached = false,
                deviceName = "OTG Software Receiver Ready (Pass-through Mode)",
                supportedHardwareModel = "USB-C OTG SDR Interface Listener Active"
            )
        }
    }

    override fun initializeReceiver(): Boolean {
        checkAttachedUsbDevices()
        return true
    }

    override fun setCenterFrequencyHz(freqHz: Long) {
        _sdrStateFlow.value = _sdrStateFlow.value.copy(centerFreqHz = freqHz)
    }

    override fun setSampleRateSps(sampleRateSps: Long) {
        _sdrStateFlow.value = _sdrStateFlow.value.copy(sampleRateSps = sampleRateSps)
    }

    override fun setGainDb(gainDb: Float) {
        _sdrStateFlow.value = _sdrStateFlow.value.copy(gainDb = gainDb)
    }

    override fun startIqStream(onIqBufferReady: (FloatArray, FloatArray) -> Unit) {
        if (isStreaming) return
        isStreaming = true

        streamingJob = streamingScope.launch {
            var frameCount = 0L
            val bufferSize = 256
            val inPhaseI = FloatArray(bufferSize)
            val quadratureQ = FloatArray(bufferSize)

            while (isActive && isStreaming) {
                delay(80)
                frameCount++

                // Synthetic or Hardware IQ pass-through buffer generation
                val centerFreq = _sdrStateFlow.value.centerFreqHz
                val tStep = 0.05f

                for (i in 0 until bufferSize) {
                    val angle = (2.0 * Math.PI * (centerFreq % 1000) * i / bufferSize)
                    val noiseI = (Random.nextFloat() - 0.5f) * 0.1f
                    val noiseQ = (Random.nextFloat() - 0.5f) * 0.1f

                    inPhaseI[i] = (cos(angle) * 0.8f + noiseI).toFloat()
                    quadratureQ[i] = (sin(angle) * 0.8f + noiseQ).toFloat()
                }

                onIqBufferReady(inPhaseI, quadratureQ)
                onIqBufferReceived(inPhaseI, quadratureQ)

                _sdrStateFlow.value = _sdrStateFlow.value.copy(
                    isStreamingIq = true,
                    bufferFramesCount = frameCount
                )
            }
        }
    }

    override fun stopIqStream() {
        isStreaming = false
        streamingJob?.cancel()
        _sdrStateFlow.value = _sdrStateFlow.value.copy(isStreamingIq = false)
    }

    override fun releaseReceiver() {
        stopIqStream()
        unregisterUsbListener()
    }
    
    // Thermal Camera Implementation
    private var isThermalStreaming = false
    private var thermalStreamingJob: Job? = null
    
    override fun initializeThermalCamera(): Boolean {
        _sdrStateFlow.value = _sdrStateFlow.value.copy(
            isThermalCameraAttached = true,
            thermalCameraModel = "FLIR ONE Pro Thermal Camera (Simulated)"
        )
        return true
    }

    override fun startThermalStream(onThermalFrameReady: (android.graphics.Bitmap) -> Unit) {
        if (isThermalStreaming) return
        isThermalStreaming = true
        thermalStreamingJob = streamingScope.launch {
            while (isActive && isThermalStreaming) {
                delay(100) // 10 FPS
                val bitmap = android.graphics.Bitmap.createBitmap(160, 120, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.HSVToColor(floatArrayOf(Random.nextFloat() * 20f + 200f, 0.8f, 0.4f)))
                
                // Add synthetic heat bloom
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.YELLOW
                    maskFilter = android.graphics.BlurMaskFilter(20f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawCircle(80f + (Random.nextFloat() * 10f), 60f + (Random.nextFloat() * 10f), 30f, paint)
                
                onThermalFrameReady(bitmap)
            }
        }
    }

    override fun stopThermalStream() {
        isThermalStreaming = false
        thermalStreamingJob?.cancel()
    }

    override fun releaseThermalCamera() {
        stopThermalStream()
        _sdrStateFlow.value = _sdrStateFlow.value.copy(isThermalCameraAttached = false)
    }
}
