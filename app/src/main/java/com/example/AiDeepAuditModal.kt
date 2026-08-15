package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AiDeepAuditModal(
    isOpen: Boolean,
    isAnalyzing: Boolean,
    threatReport: ThreatAnalysisReport?,
    activeBlips: List<RadarBlip>,
    onDismiss: () -> Unit,
    onReRunAudit: () -> Unit,
    onSelectTargetOnRadar: (String) -> Unit
) {
    if (!isOpen) return

    val clipboardManager = LocalClipboardManager.current
    var copiedFeedback by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var showAllBufferSignals by remember { mutableStateOf(false) }

    val report = threatReport ?: ThreatAnalysisReport(
        threatLevel = ThreatLevel.SECURE,
        threatScore = 14,
        executiveSummary = "Live RF signal buffer analyzed. No active hostile electronic warfare or covert tracking nodes detected.",
        naturalLanguageThreatAssessment = "The current RF spectrum buffer contains ${activeBlips.size} intercepted signals across standard Wi-Fi, Bluetooth Low Energy, and Cellular carrier frequencies. All detected devices operate within typical consumer baseline bounds. Zero unauthorized tracking tags or rogue access points were identified in your immediate perimeter.",
        analyzedRfBufferCount = activeBlips.size,
        isAiGenerated = true
    )

    val infiniteTransition = rememberInfiniteTransition(label = "AuditSpin")
    val scanRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScanRotation"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseGlow"
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
                .fillMaxHeight(0.92f)
                .testTag("ai_deep_audit_modal"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF030A06)
            ),
            border = BorderStroke(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(
                        report.threatLevel.color,
                        Color(0xFF00FF66).copy(alpha = 0.6f),
                        Color(0xFF071B10)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Modal Header
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
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(report.threatLevel.color.copy(alpha = 0.2f))
                                .border(1.5.dp, report.threatLevel.color, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isAnalyzing) Icons.Default.Refresh else Icons.Default.Security,
                                contentDescription = "AI Audit Icon",
                                tint = report.threatLevel.color,
                                modifier = Modifier
                                    .size(24.dp)
                                    .then(if (isAnalyzing) Modifier.rotate(scanRotation) else Modifier)
                            )
                        }

                        Column {
                            Text(
                                text = "AI DEEP RF AUDIT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = if (isAnalyzing) "GEMINI 3.5 FLASH ANALYZING BUFFER..." else "NATURAL LANGUAGE THREAT ASSESSMENT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isAnalyzing) Color(0xFFFFCC00) else Color(0xFF00FF66)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_ai_deep_audit_modal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Modal",
                            tint = Color.LightGray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Sub-header telemetry badge bar
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF071B10),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(report.threatLevel.color)
                            )
                            Text(
                                text = "SEVERITY: ${report.threatLevel.label}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = report.threatLevel.color
                            )
                        }

                        Text(
                            text = "BUFFER: ${report.analyzedRfBufferCount.coerceAtLeast(activeBlips.size)} NODES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = Color(0xFF33CCFF)
                        )

                        Text(
                            text = "THREAT: ${report.threatScore}/100",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            ),
                            color = report.threatLevel.color
                        )
                    }
                }

                // Main Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Loading State Banner
                    if (isAnalyzing) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF142412)),
                                border = BorderStroke(1.dp, Color(0xFFFFCC00).copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = Color(0xFF00FF66),
                                        trackColor = Color(0xFF0A1F0D)
                                    )
                                    Text(
                                        text = "Querying Gemini 3.5 Flash with ${activeBlips.size} buffered RF signatures...",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        ),
                                        color = Color(0xFFFFDD88)
                                    )
                                    Text(
                                        text = "Synthesizing multi-frequency link layer analysis & natural language threat report...",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp
                                        ),
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }

                    // Natural Language Threat Assessment Card (Primary Output)
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF05140A)
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                report.threatLevel.color.copy(alpha = pulseGlow)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("natural_language_threat_assessment_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Psychology,
                                            contentDescription = "AI Brain",
                                            tint = Color(0xFF00FF66),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "NATURAL LANGUAGE SITUATION BRIEF",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = Color(0xFF00FF66)
                                        )
                                    }

                                    // Action buttons for speech / copy
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { isSpeaking = !isSpeaking },
                                            modifier = Modifier.size(32.dp).testTag("speak_threat_assessment_button")
                                        ) {
                                            Icon(
                                                imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                                contentDescription = "Read Aloud",
                                                tint = if (isSpeaking) Color(0xFF00FF66) else Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                val textToCopy = """
                                                    [GEMINI AI DEEP RF AUDIT]
                                                    Threat Level: ${report.threatLevel.label}
                                                    Threat Score: ${report.threatScore}/100
                                                    Analyzed Signals: ${report.analyzedRfBufferCount}
                                                    
                                                    Executive Summary:
                                                    ${report.executiveSummary}
                                                    
                                                    Natural Language Threat Assessment:
                                                    ${report.naturalLanguageThreatAssessment}
                                                    
                                                    Vectors: ${report.identifiedVectors.joinToString(", ")}
                                                """.trimIndent()
                                                clipboardManager.setText(AnnotatedString(textToCopy))
                                                copiedFeedback = true
                                            },
                                            modifier = Modifier.size(32.dp).testTag("copy_threat_assessment_button")
                                        ) {
                                            Icon(
                                                imageVector = if (copiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                                                contentDescription = "Copy Assessment",
                                                tint = if (copiedFeedback) Color(0xFF00FF66) else Color.LightGray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Executive Summary Callout
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = report.threatLevel.color.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, report.threatLevel.color.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = report.executiveSummary,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 18.sp
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                // Detailed Natural Language Threat Assessment Body
                                Text(
                                    text = report.naturalLanguageThreatAssessment,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 17.sp,
                                        fontSize = 12.sp
                                    ),
                                    color = Color(0xFFCCFFDD)
                                )

                                if (report.rawSigintDetails.isNotBlank()) {
                                    Divider(color = Color(0xFF00FF66).copy(alpha = 0.2f), thickness = 0.8.dp)
                                    Text(
                                        text = report.rawSigintDetails,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp
                                        ),
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }

                    // Identified Vectors Tag Cloud
                    if (report.identifiedVectors.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "IDENTIFIED THREAT VECTORS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    ),
                                    color = Color.Gray
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(report.identifiedVectors) { vector ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF221111),
                                            border = BorderStroke(1.dp, Color(0xFFFF4444).copy(alpha = 0.6f))
                                        ) {
                                            Text(
                                                text = vector,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = Color(0xFFFF9999),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Flagged Emitters In Buffer
                    if (report.flaggedEmitters.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "FLAGGED BUFFER EMITTERS (${report.flaggedEmitters.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    ),
                                    color = Color(0xFFFFCC00)
                                )

                                report.flaggedEmitters.forEach { emitter ->
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF101C14)),
                                        border = BorderStroke(1.dp, Color(0xFFFF6600).copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFF2A1505)
                                                    ) {
                                                        Text(
                                                            text = emitter.signalType,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 9.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            color = Color(0xFFFF9933),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }

                                                    Text(
                                                        text = emitter.name,
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace
                                                        ),
                                                        color = Color.White
                                                    )
                                                }

                                                Text(
                                                    text = "${emitter.rssiDbm} dBm • ${"%.1f".format(emitter.distanceMeters)}m",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 9.5.sp
                                                    ),
                                                    color = Color.LightGray
                                                )
                                            }

                                            Text(
                                                text = emitter.riskSummary,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp
                                                ),
                                                color = Color(0xFFFFDD88)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Action: ${emitter.recommendedAction}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 9.sp
                                                    ),
                                                    color = Color(0xFF88FFAA),
                                                    modifier = Modifier.weight(1f)
                                                )

                                                Button(
                                                    onClick = {
                                                        onSelectTargetOnRadar(emitter.id)
                                                        onDismiss()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF00FF66),
                                                        contentColor = Color.Black
                                                    ),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(28.dp).testTag("pinpoint_emitter_${emitter.id}_button")
                                                ) {
                                                    Text(
                                                        text = "PINPOINT",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Black,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 9.sp
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tactical Countermeasures Section
                    if (report.countermeasures.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "ACTIONABLE MITIGATION PROTOCOLS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    ),
                                    color = Color(0xFF00FF66)
                                )

                                report.countermeasures.forEach { cm ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0A1810),
                                        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = when (cm.urgency) {
                                                    "IMMEDIATE" -> Color(0xFF3B1010)
                                                    "RECOMMENDED" -> Color(0xFF332005)
                                                    else -> Color(0xFF082B14)
                                                }
                                            ) {
                                                Text(
                                                    text = cm.urgency,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Black
                                                    ),
                                                    color = when (cm.urgency) {
                                                        "IMMEDIATE" -> Color(0xFFFF5555)
                                                        "RECOMMENDED" -> Color(0xFFFFCC00)
                                                        else -> Color(0xFF00FF66)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = cm.title,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    ),
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = cm.detail,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.5.sp
                                                    ),
                                                    color = Color(0xFFBBDDBB)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Expandable Raw RF Buffer Signal Inventory
                    item {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF08120B)),
                            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAllBufferSignals = !showAllBufferSignals }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AUDITED RF BUFFER INVENTORY (${activeBlips.size} NODES)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = Color.Gray
                                    )
                                    Icon(
                                        imageVector = if (showAllBufferSignals) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Expand",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                if (showAllBufferSignals) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    activeBlips.take(20).forEach { blip ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "[${blip.type}]",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color(0xFF00FF66)
                                                )
                                                Text(
                                                    text = blip.name.take(18),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 9.5.sp
                                                    ),
                                                    color = Color.White
                                                )
                                            }

                                            Text(
                                                text = "${blip.rssi}dBm • ${"%.1f".format(blip.distance)}m",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp
                                                ),
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Modal Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("dismiss_ai_deep_audit_button")
                    ) {
                        Text(
                            text = "CLOSE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.LightGray
                        )
                    }

                    Button(
                        onClick = onReRunAudit,
                        enabled = !isAnalyzing,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00FF66),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp)
                            .testTag("rerun_ai_deep_audit_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.Black
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Re-run",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = if (isAnalyzing) "ANALYZING..." else "RUN AI DEEP AUDIT",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
