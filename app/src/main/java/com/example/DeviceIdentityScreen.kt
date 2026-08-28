package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeviceIdentityScreen(
    uiState: SignalRadarUiState,
    identityEngine: DeviceIdentityEngine
) {
    val hypothesesMap by identityEngine.hypotheses.collectAsStateWithLifecycle()
    val hypotheses = hypothesesMap.values.sortedByDescending { it.lastSeenMs }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Identity Graph",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Device Identity & Correlation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Probabilistic physical device matching.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (hypotheses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No identity hypotheses generated yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(hypotheses, key = { it.id }) { hypothesis ->
                    DeviceIdentityCard(
                        hypothesis = hypothesis,
                        otherHypotheses = hypotheses.filter { it.id != hypothesis.id },
                        onMerge = { targetId, sourceId ->
                            coroutineScope.launch {
                                identityEngine.mergeHypotheses(targetId, sourceId)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceIdentityCard(
    hypothesis: DeviceIdentityHypothesis,
    otherHypotheses: List<DeviceIdentityHypothesis>,
    onMerge: (String, String) -> Unit
) {
    var showMergeDialog by remember { mutableStateOf(false) }

    if (showMergeDialog) {
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            title = {
                Text(
                    text = "Manual Physical Match Correlation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You are manually identifying this target as the same physical transmitter as another observed target. This consolidates all associated MAC addresses, combines their positive/negative evidence, and updates the confidence probability.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select an observed device to merge into this hypothesis:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(otherHypotheses) { other ->
                            Card(
                                onClick = {
                                    onMerge(hypothesis.id, other.id)
                                    showMergeDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = other.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "MAC: ${other.primaryMac.cleanDeviceId()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Text(
                                        text = "${other.confidenceScorePercent}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMergeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Hypothesis ID: ${hypothesis.id.take(8)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = hypothesis.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hypothesis.semanticName != null || hypothesis.semanticProfile?.localName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "MAC: ${hypothesis.primaryMac.cleanDeviceId()}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (MacOuidResolver.isRandomized(hypothesis.primaryMac)) {
                            val label = if (MacOuidResolver.isResolvablePrivateAddress(hypothesis.primaryMac)) "RPA" else "Randomized"
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp
                                )
                            }
                        }
                        // Semantic Appearance Badge
                        val appLabel = hypothesis.semanticAppearance ?: hypothesis.semanticProfile?.appearanceDisplayName
                        if (!appLabel.isNullOrBlank() && appLabel != "Unknown Appearance") {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = appLabel,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp
                                )
                            }
                        }
                        // Ecosystem Badge
                        val ecoLabel = hypothesis.semanticEcosystem ?: hypothesis.semanticProfile?.proprietaryEcosystemType
                        if (!ecoLabel.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = ecoLabel,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }
                
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Confidence Badge
                    val (badgeColor, text) = when (hypothesis.confidenceLevel) {
                        IdentityConfidenceLevel.VERY_STRONG -> Color(0xFF4CAF50) to "Very Strong"
                        IdentityConfidenceLevel.STRONG -> Color(0xFF8BC34A) to "Strong"
                        IdentityConfidenceLevel.PROBABLE -> Color(0xFFFFC107) to "Probable"
                        IdentityConfidenceLevel.POSSIBLE -> Color(0xFFFF9800) to "Possible"
                        IdentityConfidenceLevel.VERY_WEAK -> Color(0xFFF44336) to "Very Weak"
                    }
                    
                    Surface(
                        color = badgeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor)
                    ) {
                        Text(
                            text = "${hypothesis.confidenceScorePercent}% $text",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (otherHypotheses.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = { showMergeDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Manual Link",
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Correlate", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            
            // Historical Identity Graph / Evidence
            Text(
                text = "Identity Hypothesis:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "${hypothesis.confidenceLevel.displayName}. The system evaluates these observations as potentially originating from the same physical device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            // Evidence Breakdown
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Supporting Evidence
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Supporting Evidence",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (hypothesis.supportingEvidence.isEmpty()) {
                        Text("- None", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        hypothesis.supportingEvidence.forEach { ev ->
                            Text(
                                text = "✓ ${ev.description}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                // Contradicting Evidence
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Contradicting Evidence",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF44336),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (hypothesis.contradictingEvidence.isEmpty()) {
                        Text("- None", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        hypothesis.contradictingEvidence.forEach { ev ->
                            Text(
                                text = "✗ ${ev.description}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            
            // Associated Macs
            if (hypothesis.associatedMacs.size > 1) {
                Text(
                    text = "Associated Identities: ${hypothesis.associatedMacs.joinToString(", ") { it.cleanDeviceId() }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            // Stats
            val df = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.US) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("First Seen", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(df.format(Date(hypothesis.firstSeenMs)), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Observations", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${hypothesis.observationCount}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Last Seen", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(df.format(Date(hypothesis.lastSeenMs)), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

fun String.cleanDeviceId(): String {
    return this.replace("ble_db_", "").replace("wifi_db_", "")
}
