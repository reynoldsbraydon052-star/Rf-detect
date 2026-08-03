package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BleDatabaseTrackerCard(
    bleDevices: List<BleDeviceEntity>,
    isScannerActive: Boolean,
    selectedTargetDeviceId: String? = null,
    isAudioSonarActive: Boolean = false,
    onToggleScanner: () -> Unit,
    onClearDatabase: () -> Unit,
    onDeleteDevice: (String) -> Unit,
    onSelectTargetDevice: (String) -> Unit = {},
    onPlayTestPing: (Double) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedProximityFilter by remember { mutableStateOf("ALL") }

    val filteredDevices = remember(bleDevices, searchQuery, selectedProximityFilter) {
        bleDevices.filter { device ->
            val matchesSearch = searchQuery.isBlank() ||
                    device.deviceName.contains(searchQuery, ignoreCase = true) ||
                    device.macAddress.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedProximityFilter) {
                "BT60_CS" -> device.isChannelSoundingCapable
                "MICRO" -> device.distanceMeters <= 5.0f
                "IMMEDIATE" -> device.distanceMeters > 5.0f && device.distanceMeters <= 15.0f
                "FAR" -> device.distanceMeters > 15.0f
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ble_database_tracker_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BluetoothSearching,
                        contentDescription = "BLE Scanner Room DB",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "BLE SCANNER • ROOM DATABASE",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF00E5FF)
                        )
                        Text(
                            text = "SQLite Persistence Engine (`ble_devices` table)",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Gray
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onToggleScanner,
                        modifier = Modifier.testTag("toggle_ble_scanner_button")
                    ) {
                        Icon(
                            imageVector = if (isScannerActive) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = "Toggle Scanner",
                            tint = if (isScannerActive) Color(0xFF00FF66) else Color.Yellow
                        )
                    }

                    IconButton(
                        onClick = onClearDatabase,
                        modifier = Modifier.testTag("clear_ble_db_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Database",
                            tint = Color(0xFFFF3366)
                        )
                    }
                }
            }

            // Status Bar & Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ble_db_search_input"),
                placeholder = {
                    Text(
                        "Search MAC Address or Device Name...",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Gray
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF00E5FF)
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF1E3A2B),
                    focusedContainerColor = Color(0xFF060D0A),
                    unfocusedContainerColor = Color(0xFF060D0A)
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Proximity & Protocol Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedProximityFilter == "ALL",
                    onClick = { selectedProximityFilter = "ALL" },
                    label = {
                        Text(
                            "ALL (${bleDevices.size})",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF),
                        selectedLabelColor = Color.Black
                    )
                )

                val csCount = bleDevices.count { it.isChannelSoundingCapable }
                FilterChip(
                    selected = selectedProximityFilter == "BT60_CS",
                    onClick = { selectedProximityFilter = "BT60_CS" },
                    label = {
                        Text(
                            "BT 6.0 CS ($csCount)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00FF66),
                        selectedLabelColor = Color.Black
                    )
                )

                FilterChip(
                    selected = selectedProximityFilter == "MICRO",
                    onClick = { selectedProximityFilter = "MICRO" },
                    label = {
                        Text(
                            "< 5m",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF3366),
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = selectedProximityFilter == "IMMEDIATE",
                    onClick = { selectedProximityFilter = "IMMEDIATE" },
                    label = {
                        Text(
                            "5-15m NEAR",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFCC00),
                        selectedLabelColor = Color.Black
                    )
                )

                FilterChip(
                    selected = selectedProximityFilter == "FAR",
                    onClick = { selectedProximityFilter = "FAR" },
                    label = {
                        Text(
                            "> 15m FAR",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00FF66),
                        selectedLabelColor = Color.Black
                    )
                )
            }

            // List of Saved BLE Devices from Room DB
            if (filteredDevices.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF080E0B),
                    border = BorderStroke(1.dp, Color(0xFF1B3A2B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Empty",
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No BLE devices stored in Room DB matching query.",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredDevices.forEach { device ->
                        BleDeviceRoomTile(
                            device = device,
                            isSelectedTarget = selectedTargetDeviceId == device.macAddress || selectedTargetDeviceId == device.deviceName,
                            isAudioSonarActive = isAudioSonarActive,
                            onToggleAudioLock = { onSelectTargetDevice(device.macAddress) },
                            onPlayTestPing = { onPlayTestPing(device.distanceMeters.toDouble()) },
                            onDelete = { onDeleteDevice(device.macAddress) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BleDeviceRoomTile(
    device: BleDeviceEntity,
    isSelectedTarget: Boolean = false,
    isAudioSonarActive: Boolean = false,
    onToggleAudioLock: () -> Unit = {},
    onPlayTestPing: () -> Unit = {},
    onDelete: () -> Unit
) {
    val proximityColor = when (device.proximityCategory) {
        "MICRO_PERIMETER" -> Color(0xFFFF3366)
        "IMMEDIATE" -> Color(0xFFFFCC00)
        else -> Color(0xFF00FF66)
    }

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formattedTime = remember(device.lastSeenTimestamp) {
        dateFormat.format(Date(device.lastSeenTimestamp))
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelectedTarget) Color(0xFF141F0A) else Color(0xFF080E0B),
        border = BorderStroke(
            if (isSelectedTarget) 2.dp else 1.dp,
            if (isSelectedTarget) Color.Yellow else proximityColor.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ble_device_tile_${device.macAddress}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(proximityColor)
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = device.deviceName,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                            if (device.isChannelSoundingCapable) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF00FF66)
                                ) {
                                    Text(
                                        text = "BT 6.0 CS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            if (isSelectedTarget) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Yellow
                                ) {
                                    Text(
                                        text = "TARGET LOCKED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "MAC: ${device.macAddress}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFF00E5FF)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (device.isChannelSoundingCapable) Color(0xFF00FF66).copy(alpha = 0.25f) else proximityColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (device.isChannelSoundingCapable) "%.2f m ±%.2fm".format(device.distanceMeters, device.csEstimatedAccuracyMeters) else "%.1f m".format(device.distanceMeters),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (device.isChannelSoundingCapable) Color(0xFF00FF66) else proximityColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp).testTag("delete_device_${device.macAddress}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Delete Entity",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Signal Strength Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { device.signalStrengthPercent / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (device.isChannelSoundingCapable) Color(0xFF00FF66) else proximityColor,
                    trackColor = Color(0xFF1E3A2B)
                )
                Text(
                    text = "${device.rssi} dBm (${device.signalStrengthPercent}%)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Yellow
                )
            }

            // Bluetooth 6.0 Channel Sounding High-Precision Ranging Metrics
            if (device.isChannelSoundingCapable) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF06140D),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CHANNEL SOUNDING PBR + RTT METRICS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF00FF66)
                            )
                            Text(
                                text = "±%.2fm CS Accuracy".format(device.csEstimatedAccuracyMeters),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF00E5FF)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Method: ${device.csRangingMethod}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontFamily = FontFamily.Monospace),
                                color = Color.LightGray
                            )
                            Text(
                                text = "Phase Quality: ${device.csPhaseQualityIndex}% (${device.csChannelCount} Channels)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontFamily = FontFamily.Monospace),
                                color = Color.LightGray
                            )
                        }

                        if (device.csRttTimeOfFlightNs > 0f) {
                            Text(
                                text = "Flight Time: %.1f ns ToF • Sub-meter multipath anti-fading active".format(device.csRttTimeOfFlightNs),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontFamily = FontFamily.Monospace),
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                }
            }

            // Proximity Audio Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onToggleAudioLock,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("lock_audio_sonar_${device.macAddress}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelectedTarget) Color.Yellow else Color(0xFF00FF66).copy(alpha = 0.18f),
                        contentColor = if (isSelectedTarget) Color.Black else Color(0xFF00FF66)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = "Lock Audio Sonar",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSelectedTarget) "AUDIO LOCKED" else "LOCK AUDIO SONAR",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    )
                }

                OutlinedButton(
                    onClick = onPlayTestPing,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("ping_sonar_${device.macAddress}"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Test Ping",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PING TONE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            // Meta Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Payload: ${device.advertisementPayload}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.Gray
                )
                Text(
                    text = "Hits: #${device.hitCount} • Last: $formattedTime",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.LightGray
                )
            }
        }
    }
}
