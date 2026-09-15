package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

enum class ThrottlingStatus {
    ENABLED_THROTTLED,
    DISABLED_UNTHROTTLED,
    UNKNOWN
}

object SignalThrottlingGuard {

    /**
     * Checks if Wi-Fi scan throttling is currently enabled on the device.
     * High-rate SIGINT sweeps require this to be disabled.
     */
    @SuppressLint("WifiManagerPotentialLeak")
    fun checkWifiThrottlingStatus(context: Context): ThrottlingStatus {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && wifiManager != null) {
                // Android 14 (API 34) introduced direct API check
                val throttled = wifiManager.isScanThrottleEnabled
                if (throttled) ThrottlingStatus.ENABLED_THROTTLED else ThrottlingStatus.DISABLED_UNTHROTTLED
            } else {
                // Fallback to reading system global developer settings
                val throttleSetting = Settings.Global.getInt(
                    context.contentResolver,
                    "wifi_scan_throttle_enabled",
                    -1
                )
                when (throttleSetting) {
                    1 -> ThrottlingStatus.ENABLED_THROTTLED
                    0 -> ThrottlingStatus.DISABLED_UNTHROTTLED
                    else -> ThrottlingStatus.UNKNOWN
                }
            }
        } catch (e: Exception) {
            ThrottlingStatus.UNKNOWN
        }
    }

    /**
     * Creates an intent to open Developer Options so the user can easily toggle off Wi-Fi Scan Throttling.
     */
    fun getOpenDeveloperSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}

/**
 * Thread-safe management of system hardware wake locks and high-performance Wi-Fi locks.
 */
class TacticalPowerLockManager(context: Context) {
    private val appContext = context.applicationContext
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private val lockObject = Any()

    /**
     * Acquires a high-performance WifiLock and a Partial WakeLock with a 30-minute auto-release.
     */
    @SuppressLint("WakelockTimeout")
    fun acquireHighPerfLocks() {
        synchronized(lockObject) {
            try {
                // 1. Safe acquisition of WakeLock
                if (wakeLock == null && powerManager != null) {
                    wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "TacticalSignalRadar:WakeLock"
                    ).apply {
                        setReferenceCounted(false)
                        acquire(30 * 60 * 1000L) // Safe 30 minute auto-release timeout
                    }
                }

                // 2. Safe acquisition of High Performance / Low Latency WifiLock
                if (wifiLock == null && wifiManager != null) {
                    val lockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                    } else {
                        @Suppress("DEPRECATION")
                        WifiManager.WIFI_MODE_FULL_HIGH_PERF
                    }
                    wifiLock = wifiManager.createWifiLock(lockMode, "TacticalSignalRadar:WiFiLock").apply {
                        setReferenceCounted(false)
                        acquire()
                    }
                }
            } catch (e: Exception) {
                // Failsafe recovery: Do not let system exceptions block operations
            }
        }
    }

    /**
     * Releases active wake and wifi locks safely.
     */
    fun releaseLocks() {
        synchronized(lockObject) {
            try {
                wakeLock?.let {
                    if (it.isHeld) {
                        it.release()
                    }
                }
                wakeLock = null

                wifiLock?.let {
                    if (it.isHeld) {
                        it.release()
                    }
                }
                wifiLock = null
            } catch (e: Exception) {
                // Failsafe cleanup
            }
        }
    }
}
