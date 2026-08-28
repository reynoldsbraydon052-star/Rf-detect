package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class DeviceAlertEvent(
    val id: String,
    val deviceName: String,
    val macAddress: String,
    val rssi: Int,
    val distanceMeters: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val isNewDevice: Boolean = true
)

class ScannerBackgroundAlertService : Service() {

    companion object {
        const val CHANNEL_ID = "scanner_threshold_alerts_channel"
        const val CHANNEL_NAME = "Scanner Signal Threshold Device Alerts"
        const val NOTIFICATION_ID_FOREGROUND = 1001

        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val ACTION_UPDATE_SETTINGS = "ACTION_UPDATE_SETTINGS"
        const val EXTRA_RSSI_THRESHOLD = "EXTRA_RSSI_THRESHOLD"
        const val EXTRA_ENABLE_HAPTIC = "EXTRA_ENABLE_HAPTIC"
        const val EXTRA_ENABLE_NOTIF = "EXTRA_ENABLE_NOTIF"

        private val _alertEvents = MutableSharedFlow<DeviceAlertEvent>(extraBufferCapacity = 64)
        val alertEvents = _alertEvents.asSharedFlow()

        var isServiceRunning = false
            private set

        private val notifiedMacAddresses = mutableSetOf<String>()

        fun notifyDeviceDetected(
            context: Context,
            macAddress: String,
            deviceName: String,
            rssi: Int,
            distanceMeters: Float,
            thresholdRssi: Int,
            isNewDevice: Boolean,
            enableHaptics: Boolean = true,
            enableVisualNotif: Boolean = true
        ) {
            if (rssi < thresholdRssi) return

            // Fire if new device or first time meeting signal threshold
            if (isNewDevice || !notifiedMacAddresses.contains(macAddress)) {
                notifiedMacAddresses.add(macAddress)

                val alert = DeviceAlertEvent(
                    id = macAddress,
                    deviceName = deviceName,
                    macAddress = macAddress,
                    rssi = rssi,
                    distanceMeters = distanceMeters,
                    isNewDevice = isNewDevice
                )

                _alertEvents.tryEmit(alert)

                if (enableHaptics) {
                    triggerHapticPulse(context)
                }

                if (enableVisualNotif) {
                    postNotification(context, alert)
                }
            }
        }

        fun triggerHapticPulse(context: Context) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = VibrationEffect.createWaveform(
                            longArrayOf(0, 120, 60, 220, 60, 120),
                            intArrayOf(0, 255, 0, 255, 0, 200),
                            -1
                        )
                        vibrator.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 120, 60, 220), -1)
                    }
                }
            } catch (_: Throwable) {
                // Hardware fallback safety
            }
        }

        fun postNotification(context: Context, alert: DeviceAlertEvent) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            ensureNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("TARGET_MAC", alert.macAddress)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, alert.macAddress.hashCode(), intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("🚨 TARGET MATCHED SIGNAL THRESHOLD (${alert.rssi} dBm)")
                .setContentText("${alert.deviceName} [${alert.macAddress}] • Distance: %.1fm".format(alert.distanceMeters))
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_LIGHTS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notifId = (alert.macAddress.hashCode() and 0x7FFFFFFF) + 3000
            notificationManager.notify(notifId, notification)
        }

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Triggers visual notifications and haptic alerts when scanner detects signal matching threshold"
                    enableVibration(true)
                    enableLights(true)
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
            }
        }

        fun updateForegroundTelemetry(
            context: Context,
            activeAntennas: Int,
            activeNodes: Int,
            nearestTargetDistMeters: Float
        ) {
            if (!isServiceRunning) return
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val notificationIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val telemetryText = "📡 Antennas: $activeAntennas | 🎯 Nodes: $activeNodes | 📏 Nearest: %.1fm".format(nearestTargetDistMeters)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("RF Spectrum Radar • Background Scan Engine")
                .setContentText(telemetryText)
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            notificationManager.notify(NOTIFICATION_ID_FOREGROUND, notification)
        }

        fun clearNotifiedHistory() {
            notifiedMacAddresses.clear()
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var rssiThresholdDbm: Int = -75
    private var isHapticEnabled: Boolean = true
    private var isVisualNotifEnabled: Boolean = true

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                rssiThresholdDbm = intent.getIntExtra(EXTRA_RSSI_THRESHOLD, -75)
                isHapticEnabled = intent.getBooleanExtra(EXTRA_ENABLE_HAPTIC, true)
                isVisualNotifEnabled = intent.getBooleanExtra(EXTRA_ENABLE_NOTIF, true)
                startForegroundServiceMode()
            }
            ACTION_STOP_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE_SETTINGS -> {
                rssiThresholdDbm = intent.getIntExtra(EXTRA_RSSI_THRESHOLD, rssiThresholdDbm)
                isHapticEnabled = intent.getBooleanExtra(EXTRA_ENABLE_HAPTIC, isHapticEnabled)
                isVisualNotifEnabled = intent.getBooleanExtra(EXTRA_ENABLE_NOTIF, isVisualNotifEnabled)
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceMode() {
        isServiceRunning = true
        try {
            ensureNotificationChannel(this)
            val notificationIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("RF Scanner Background Service Active")
                .setContentText("Monitoring new devices meeting $rssiThresholdDbm dBm threshold...")
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(
                        NOTIFICATION_ID_FOREGROUND,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    )
                } catch (e: Throwable) {
                    startForeground(NOTIFICATION_ID_FOREGROUND, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID_FOREGROUND, notification)
            }
        } catch (e: Throwable) {
            // Non-fatal fallback for container or restricted background environments
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        serviceScope.cancel()
    }
}
