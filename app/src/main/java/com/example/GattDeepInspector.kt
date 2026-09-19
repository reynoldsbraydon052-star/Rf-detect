package com.example

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.*

data class GattServiceSummary(
    val serviceUuid: String,
    val characteristics: List<String>
)

data class GattInterceptDossier(
    val deviceAddress: String = "",
    val deviceName: String = "UNKNOWN_BLE_TARGET",
    val manufacturerString: String = "NOT_REPORTED",
    val modelNumberString: String = "NOT_REPORTED",
    val firmwareRevisionString: String = "NOT_REPORTED",
    val discoveredServices: List<GattServiceSummary> = emptyList(),
    val isInterrogated: Boolean = true
)

object GattDeepInspector {

    private val DIS_SERVICE_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
    private val MFR_NAME_UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
    private val MODEL_NUM_UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")
    private val FW_REV_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")

    @SuppressLint("MissingPermission")
    fun connectAndInterrogate(
        context: Context,
        device: BluetoothDevice? = null,
        targetAddress: String = "",
        targetName: String = "",
        onDossierReady: (GattInterceptDossier) -> Unit
    ) {
        val address = try { device?.address ?: targetAddress } catch (_: Throwable) { targetAddress }
        if (address.isBlank()) {
            onDossierReady(
                GattInterceptDossier(
                    deviceAddress = "UNAVAILABLE",
                    deviceName = targetName.ifBlank { "UNKNOWN_BLE_TARGET" },
                    manufacturerString = "NOT_AVAILABLE: NO_DEVICE_ADDRESS",
                    modelNumberString = "NOT_AVAILABLE",
                    firmwareRevisionString = "NOT_AVAILABLE",
                    isInterrogated = false
                )
            )
            return
        }

        val name = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) device?.name else null
            } else device?.name
        } catch (_: Throwable) { null } ?: targetName.ifEmpty { "UNKNOWN_BLE_TARGET" }

        if (device == null) {
            onDossierReady(
                GattInterceptDossier(
                    deviceAddress = address,
                    deviceName = name,
                    manufacturerString = "NOT_AVAILABLE: DEVICE_HANDLE_MISSING",
                    modelNumberString = "NOT_AVAILABLE",
                    firmwareRevisionString = "NOT_AVAILABLE",
                    isInterrogated = false
                )
            )
            return
        }

        val handler = Handler(Looper.getMainLooper())
        var bluetoothGatt: BluetoothGatt? = null
        var delivered = false

        fun deliver(dossier: GattInterceptDossier, gatt: BluetoothGatt? = null) {
            if (delivered) return
            delivered = true
            handler.removeCallbacksAndMessages(null)
            handler.post {
                onDossierReady(dossier)
                try {
                    gatt?.disconnect()
                    gatt?.close()
                } catch (_: Exception) {}
            }
        }

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    deliver(
                        GattInterceptDossier(
                            deviceAddress = address,
                            deviceName = name,
                            manufacturerString = "NOT_AVAILABLE: CONNECTION_FAILED ($status)",
                            modelNumberString = "NOT_AVAILABLE",
                            firmwareRevisionString = "NOT_AVAILABLE",
                            isInterrogated = false
                        ),
                        gatt
                    )
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    deliver(
                        GattInterceptDossier(
                            deviceAddress = address,
                            deviceName = name,
                            manufacturerString = "NOT_AVAILABLE: SERVICE_DISCOVERY_FAILED ($status)",
                            modelNumberString = "NOT_AVAILABLE",
                            firmwareRevisionString = "NOT_AVAILABLE",
                            isInterrogated = false
                        ),
                        gatt
                    )
                    return
                }

                val serviceList = gatt.services.map { svc ->
                    GattServiceSummary(
                        serviceUuid = svc.uuid.toString(),
                        characteristics = svc.characteristics.map { char -> char.uuid.toString() }
                    )
                }
                var mfr = "NOT_REPORTED"
                var model = "NOT_REPORTED"
                var firmware = "NOT_REPORTED"
                gatt.getService(DIS_SERVICE_UUID)?.let { dis ->
                    dis.getCharacteristic(MFR_NAME_UUID)?.value?.let { mfr = String(it).trim().ifBlank { "NOT_REPORTED" } }
                    dis.getCharacteristic(MODEL_NUM_UUID)?.value?.let { model = String(it).trim().ifBlank { "NOT_REPORTED" } }
                    dis.getCharacteristic(FW_REV_UUID)?.value?.let { firmware = String(it).trim().ifBlank { "NOT_REPORTED" } }
                }
                deliver(
                    GattInterceptDossier(
                        deviceAddress = address,
                        deviceName = name,
                        manufacturerString = mfr,
                        modelNumberString = model,
                        firmwareRevisionString = firmware,
                        discoveredServices = serviceList,
                        isInterrogated = true
                    ),
                    gatt
                )
            }
        }

        try {
            bluetoothGatt = device.connectGatt(context, false, callback)
            handler.postDelayed({
                deliver(
                    GattInterceptDossier(
                        deviceAddress = address,
                        deviceName = name,
                        manufacturerString = "NOT_AVAILABLE: INSPECTION_TIMEOUT",
                        modelNumberString = "NOT_AVAILABLE",
                        firmwareRevisionString = "NOT_AVAILABLE",
                        isInterrogated = false
                    ),
                    bluetoothGatt
                )
            }, 10_000L)
        } catch (e: Exception) {
            deliver(
                GattInterceptDossier(
                    deviceAddress = address,
                    deviceName = name,
                    manufacturerString = "NOT_AVAILABLE: ${e.javaClass.simpleName}",
                    modelNumberString = "NOT_AVAILABLE",
                    firmwareRevisionString = "NOT_AVAILABLE",
                    isInterrogated = false
                )
            )
        }
    }
}

@Composable
fun GattDossierDialog(
    dossier: GattInterceptDossier,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .testTag("gatt_dossier_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF071210),
            border = BorderStroke(1.5.dp, Color(0xFF00E5FF))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bluetooth, contentDescription = "GATT Intercept", tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                        Column {
                            Text("GATT INTERCEPT DOSSIER", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp), color = Color(0xFF00E5FF))
                            Text(dossier.deviceAddress, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = Color.Gray)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_gatt_dossier_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                HorizontalDivider(color = Color(0xFF00E5FF).copy(alpha = 0.3f))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F231D)),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("TARGET NAME: ${dossier.deviceName}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = Color(0xFF00FF66))
                        Text("MANUFACTURER: ${dossier.manufacturerString}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = Color.White)
                        Text("MODEL NUMBER: ${dossier.modelNumberString}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = Color.LightGray)
                        Text("FIRMWARE REV: ${dossier.firmwareRevisionString}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = Color.LightGray)
                    }
                }

                Text("DISCOVERED GATT SERVICES (${dossier.discoveredServices.size})", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = Color(0xFF00E5FF))

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dossier.discoveredServices) { service ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF091814)), border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("SVC: ${service.serviceUuid}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = Color.Yellow)
                                Text("Characteristics (${service.characteristics.size}):", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = Color.Gray)
                                service.characteristics.forEach { charUuid ->
                                    Text("  • $charUuid [DISCOVERED]", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp), color = Color(0xFF00FF66))
                                }
                            }
                        }
                    }
                }

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().testTag("dismiss_gatt_dossier_button"), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)), shape = RoundedCornerShape(10.dp)) {
                    Text("CLOSE DOSSIER", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
