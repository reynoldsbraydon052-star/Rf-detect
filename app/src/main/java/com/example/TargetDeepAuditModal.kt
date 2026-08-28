package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TargetDeepAuditModal(
    isOpen: Boolean,
    rangingResult: TacticalRangingResult?,
    onDismiss: () -> Unit,
    aiGateway: TacticalAiGateway,
    viewModel: TargetAuditViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    if (!isOpen) return

    val scope = rememberCoroutineScope()

    // Initialize view model with ranging result when modal opens
    LaunchedEffect(isOpen, rangingResult) {
        if (isOpen && rangingResult != null) {
            viewModel.setRangingResult(rangingResult)
        }
    }

    val modelRangingResult by viewModel.rangingResult.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val auditResult by viewModel.auditResult.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val currentRangingResult = modelRangingResult ?: rangingResult

    // Ranging result fallbacks if nothing is provided yet
    val mac = currentRangingResult?.targetMac ?: "00:11:22:AA:BB:CC"
    val distance = currentRangingResult?.distanceMeters ?: 12.4
    val method = currentRangingResult?.method ?: RangingMethod.BLE_RSSI_ESTIMATE
    val quality = currentRangingResult?.quality ?: SignalQuality.LOW
    val confidence = currentRangingResult?.confidenceScore ?: 0.35
    val rssiDbm = currentRangingResult?.rttOrRssiDb ?: -72

    // Color definitions
    val qualityColor = when (quality) {
        SignalQuality.HIGH -> Color(0xFF00FF66)
        SignalQuality.MEDIUM -> Color(0xFFFFCC00)
        SignalQuality.LOW -> Color(0xFFFF4444)
    }

    val methodLabel = when (method) {
        RangingMethod.BLE_CHANNEL_SOUNDING -> "Bluetooth Channel Sounding (BT-CS)"
        RangingMethod.BLE_RSSI_ESTIMATE -> "RSSI Path Loss Estimate"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "AuditSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Spin"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .testTag("target_deep_audit_modal"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020904)),
            border = BorderStroke(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(
                        qualityColor,
                        Color(0xFF00FF66).copy(alpha = 0.5f),
                        Color(0xFF041006)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(qualityColor.copy(alpha = 0.2f))
                                .border(1.5.dp, qualityColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isAnalyzing) Icons.Default.Sync else Icons.Default.Analytics,
                                contentDescription = "Audit Status",
                                tint = qualityColor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .then(if (isAnalyzing) Modifier.rotate(rotation) else Modifier)
                            )
                        }

                        Column {
                            Text(
                                text = "TARGET DEEP AUDIT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "TACTICAL SIGINT PROFILING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF00FF66)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("close_audit_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Telemetry Header Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF061509)),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MAC: $mac",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = qualityColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, qualityColor.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "QUALITY: ${quality.name}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.5.sp
                                    ),
                                    color = qualityColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFF00FF66).copy(alpha = 0.1f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "DISTANCE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Gray
                                    )
                                )
                                Text(
                                    text = "${String.format("%.2f", distance)}m",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "CONFIDENCE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Gray
                                    )
                                )
                                Text(
                                    text = "${String.format("%.0f", confidence * 100)}%",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = qualityColor
                                    )
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "ACQUISITION METHOD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            )
                            Text(
                                text = methodLabel,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.LightGray
                                )
                            )
                        }
                    }
                }

                // AI Action Trigger Button
                Button(
                    onClick = {
                        viewModel.initiateDeepAudit(aiGateway)
                    },
                    enabled = !isAnalyzing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("initiate_deep_audit_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FF66),
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF041908)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.Black
                            )
                        }
                        Text(
                            text = if (isAnalyzing) "RUNNING FORENSICS..." else "INITIATE DEEP AUDIT",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                // AI Response Canvas / Scrollable Logs
                Text(
                    text = "SIGINT FORENSIC ASSESSMENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF010603))
                        .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.15f))
                ) {
                    if (isAnalyzing) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = Color(0xFF00FF66),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Establishing secure link with Multi-Model AI Gateway...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.LightGray
                                )
                            )
                        }
                    } else if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Red
                                )
                            )
                        }
                    } else if (auditResult != null) {
                        val audit = auditResult!!
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .testTag("ai_response_canvas"),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "MANUFACTURER / VENDOR PROFILE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF00FF66),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = audit.manufacturerVendor,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }

                            item {
                                Text(
                                    text = "RADIO FINGERPRINT SUMMARY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF00FF66),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = audit.radioFingerprintSummary,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.LightGray
                                    )
                                )
                            }

                            item {
                                Text(
                                    text = "SURVEILLANCE RISK ANALYSIS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF00FF66),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = audit.surveillanceRiskAnalysis,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.LightGray
                                    )
                                )
                            }

                            item {
                                Text(
                                    text = "HARDWARE VECTOR ANALYSIS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF00FF66),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = audit.hardwareVectorAnalysis,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.LightGray
                                    )
                                )
                            }

                            item {
                                Text(
                                    text = "CRYPTOGRAPHIC PROFILE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF00FF66),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = audit.cryptographicProfile,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.LightGray
                                    )
                                )
                            }

                            if (audit.vulnerabilities.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "PROTOCOL VULNERABILITIES DETECTED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFFF5555),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                items(audit.vulnerabilities) { vuln ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C0D0D)),
                                        border = BorderStroke(1.dp, Color(0xFFFF5555).copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Protocol: ${vuln.protocol}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                )
                                                Text(
                                                    text = "Risk: ${vuln.riskLevel.label}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFFFF5555),
                                                        fontWeight = FontWeight.Black
                                                    )
                                                )
                                            }
                                            Text(
                                                text = "Surface: ${vuln.attackSurface}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.LightGray,
                                                    fontSize = 11.sp
                                                )
                                            )
                                            Text(
                                                text = "Exploit: ${vuln.exploitationVector}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFFFF9999),
                                                    fontSize = 11.sp
                                                )
                                            )
                                            Text(
                                                text = "Containment: ${vuln.containmentFix}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFF88FFAA),
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            if (audit.stepByStepNeutralizationPlan.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "STEP-BY-STEP MITIGATION PLAN",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF33CCFF),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                items(audit.stepByStepNeutralizationPlan) { step ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DoubleArrow,
                                            contentDescription = null,
                                            tint = Color(0xFF33CCFF),
                                            modifier = Modifier.size(14.dp).padding(top = 2.dp)
                                        )
                                        Text(
                                            text = step,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.LightGray
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Forensic buffer clear. Tap 'INITIATE DEEP AUDIT' above to trigger live multi-model AI thread assessment.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
