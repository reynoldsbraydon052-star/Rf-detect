                        }
                    }
                }
            }
            if (isSelectedTarget && correlations.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).background(Color(0xFF111A22), RoundedCornerShape(6.dp)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "CORRELATED EVENTS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = Color(0xFF00E5FF))
                    correlations.forEach { event ->
                        val otherObs = event.observations.firstOrNull { it.id != blip.id }
                        if (otherObs != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(text = otherObs.type, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 10.sp), color = Color.White)
                                    Text(text = "Δt: ${event.maxTimeSeparationMs}ms", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp), color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Score: ${String.format("%.2f", event.correlationScore)}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp), color = if (event.correlationScore > 0.7f) Color(0xFF00FF66) else Color(0xFFFFCC00))
                                    Text(text = event.spatialRelationship.name, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp), color = Color.Gray)
                                }
                            }
                            Text(text = event.notes, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp), color = Color.DarkGray)
                            androidx.compose.material3.HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinpointDeviceHUDCard(
    uiState: SignalRadarUiState,
    onUnlockTarget: () -> Unit,
    onToggleAudioSonar: () -> Unit,
    onOpenCalibration: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    CompassDeviceFinderCard(
        uiState = uiState,
        onSelectTargetDevice = { id -> if (id == null) onUnlockTarget() },
        onToggleAudioSonar = onToggleAudioSonar,
        onOpenCalibration = onOpenCalibration,
        modifier = modifier
    )
}

@Composable
fun SpectrumInterceptCard(
    blip: RadarBlip,
    perimeterThresholdMeters: Float,
    selectedTargetDeviceId: String? = null,
    onSelectTargetDevice: ((String?) -> Unit)? = null,
    onInterrogateGatt: ((RadarBlip) -> Unit)? = null,
    correlations: List<CorrelationEvent> = emptyList()
) {
    val isSelectedTarget = selectedTargetDeviceId != null &&
            (blip.id == selectedTargetDeviceId || blip.name == selectedTargetDeviceId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("spectrum_item_${blip.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelectedTarget) Color(0xFF0F2618) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.5.dp,
            when {
                isSelectedTarget -> Color(0xFF00FF66)
                blip.baselineState == BaselineState.ANOMALOUS -> Color(0xFFFF3366)
                blip.baselineState == BaselineState.CHANGED -> Color(0xFFFF8800)
                blip.baselineState == BaselineState.NEW -> Color(0xFFFFCC00)
                blip.distance < perimeterThresholdMeters -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = blip.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (blip.baselineState != BaselineState.UNKNOWN) {
                            val stateColor = when (blip.baselineState) {
                                BaselineState.KNOWN -> Color(0xFF00E5FF)
                                BaselineState.NEW -> Color(0xFFFFCC00)
                                BaselineState.CHANGED -> Color(0xFFFF8800)
                                BaselineState.ANOMALOUS -> Color(0xFFFF3366)
                                else -> Color.Gray
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = stateColor.copy(alpha = 0.2f), border = BorderStroke(1.dp, stateColor)) {
                                Text(
                                    text = blip.baselineState.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace),
                                    color = stateColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (isSelectedTarget) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF00FF66)
                            ) {
                                Text(
                                    text = "PINPOINTED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${blip.bandLabel} | ${blip.frequencyMhz.toInt()} MHz",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Gray
                    )
                }

                // Kalman Distance Badge & Pinpoint Quick Action
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (blip.distance < perimeterThresholdMeters) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${String.format("%.1f", blip.distance * 3.28084f)}m",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (blip.distance < perimeterThresholdMeters) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (blip.type.contains("BLE", ignoreCase = true) && onInterrogateGatt != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF003344))
                                .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                                .clickable { onInterrogateGatt.invoke(blip) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("interrogate_gatt_button_${blip.id}")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = "Interrogate GATT",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "INTERROGATE GATT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF00E5FF)
                                )
                            }
                        }
                    }

                    if (onSelectTargetDevice != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelectedTarget) Color(0xFF00FF66) else Color(0xFF142E1F))
                                .clickable {
                                    onSelectTargetDevice.invoke(if (isSelectedTarget) null else blip.id)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Pinpoint Target",
                                    tint = if (isSelectedTarget) Color.Black else Color(0xFF00FF66),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isSelectedTarget) "LOCKED" else "PINPOINT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isSelectedTarget) Color.Black else Color(0xFF00FF66)
                                )
                            }
                        }
                    }
                }
            }

            // RSSI Signal Strength Progress Bar & Quality Percentage
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val pct = ((blip.rssi + 100) * 2).coerceIn(0, 100)
                    Text(
                        text = "RSSI: ${blip.rssi} dBm (${pct}% Signal)",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Gray
                    )
                    Text(
                        text = "Type: ${blip.type}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                val normalizedRssi = ((blip.rssi + 100) / 70f).coerceIn(0.05f, 1f)
                LinearProgressIndicator(
                    progress = { normalizedRssi },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when (blip.type.uppercase()) {
                        "WIFI" -> Color(0xFF00FF66)
                        "CELLULAR" -> Color(0xFFFF3366)
                        "BLE" -> Color(0xFF00E5FF)
                        "MAGNETIC" -> Color(0xFFFF00FF)
                        else -> Color(0xFFFFCC00)
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            if (blip.fingerprintId != null) {
                val conf = blip.fingerprintConfidence ?: 0f
                val confColor = if (conf > 0.9f) Color(0xFF00FF66) else if (conf > 0.7f) Color(0xFFFFCC00) else Color(0xFFFF3366)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).background(Color(0xFF0A1A10), RoundedCornerShape(4.dp)).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "SIGNAL FINGERPRINT", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = Color.Gray)
                        Text(text = blip.fingerprintId.take(8).uppercase(), style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = Color(0xFF00E5FF))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Match confidence: ${String.format("%.2f", conf)}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace), color = confColor)
                        Text(text = if (conf > 0.8f) "Possible persistent emitter" else "New signal pattern", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                    }
                }
            }
            if (isSelectedTarget && blip.anomalyResult != null) {
                val anomaly = blip.anomalyResult
                val categoryColor = when (anomaly.category) {
                    AnomalyCategory.HIGH_DEVIATION -> Color(0xFFFF3366)
                    AnomalyCategory.MODERATE_DEVIATION -> Color(0xFFFF8800)
                    AnomalyCategory.LOW_DEVIATION -> Color(0xFFFFCC00)
                    AnomalyCategory.NORMAL -> Color(0xFF00FF66)
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "ANOMALY EXPLANATION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "${anomaly.score} / 100", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = categoryColor)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Confidence: ${String.format("%.0f", anomaly.confidence * 100)}% | Category: ${anomaly.category.name.replace("_", " ")}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                        if (anomaly.previousScore != null) {
                            Text(text = "Prev: ${anomaly.previousScore}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace), color = Color.Gray)
                        }
                    }
