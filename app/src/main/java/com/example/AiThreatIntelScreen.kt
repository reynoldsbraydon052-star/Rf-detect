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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AiThreatIntelScreen(
    uiState: SignalRadarUiState,
    threatReport: ThreatAnalysisReport?,
    isAnalyzing: Boolean,
    copilotMessages: List<TacticalCopilotMessage>,
    isCopilotThinking: Boolean,
    selectedDeepAuditTarget: DetailedTargetAudit? = null,
    isDeepAuditingEmitterId: String? = null,
    onRunAiThreatScan: () -> Unit,
    onSendCopilotQuery: (String) -> Unit,
    onSelectTargetOnRadar: (String) -> Unit,
    onOpenRadarTab: () -> Unit,
    onTriggerDeepAudit: (FlaggedThreatEmitter) -> Unit = {},
    onCloseDeepAudit: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var copilotInputText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    var copiedFeedback by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val currentReport = threatReport ?: remember {
        ThreatAnalysisReport(
            threatLevel = ThreatLevel.SECURE,
            threatScore = 14,
            executiveSummary = "Environment scan ready. Tap 'RUN GEMINI DEEP THREAT SCAN' for real-time SIGINT & AI counter-surveillance assessment.",
            isAiGenerated = false
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "RadarSpin")
    val scanRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScanRotation"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030A06))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("ai_threat_intel_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_threat_header_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF071B10)
                ),
                border = BorderStroke(
                    1.5.dp,
                    Brush.horizontalGradient(
                        listOf(Color(0xFF00FF66), currentReport.threatLevel.color, Color(0xFF00FF66))
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(currentReport.threatLevel.color.copy(alpha = 0.2f))
                                    .border(1.dp, currentReport.threatLevel.color, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAnalyzing) Icons.Default.Refresh else Icons.Default.Shield,
                                    contentDescription = "Threat Icon",
                                    tint = currentReport.threatLevel.color,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .then(if (isAnalyzing) Modifier.rotate(scanRotation) else Modifier)
                                )
                            }

                            Column {
                                Text(
                                    text = "GEMINI SIGINT THREAT INTEL",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color.White
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isAnalyzing) Color(0xFFFFCC00) else Color(0xFF00FF66))
                                    )
                                    Text(
                                        text = if (isAnalyzing) "ANALYZING RF SPECTRUM..." else "GEMINI 3.5 FLASH READY",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isAnalyzing) Color(0xFFFFCC00) else Color(0xFF00FF66)
                                    )
                                }
                            }
                        }

                        // Threat Score Dial Badge
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "${currentReport.threatScore}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = currentReport.threatLevel.color
                            )
                            Text(
                                text = "THREAT INDEX",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.5.sp
                                ),
                                color = Color.Gray
                            )
                        }
                    }

                    // Threat Status Pill & Summary
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = currentReport.threatLevel.color.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, currentReport.threatLevel.color.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "STATUS: ${currentReport.threatLevel.label}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = currentReport.threatLevel.color
                            )

                            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(currentReport.timestampMs))
                            Text(
                                text = "BUFFER: ${currentReport.analyzedRfBufferCount.coerceAtLeast(uiState.activeBlips.size)} NODES • $timeStr",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                ),
                                color = Color.LightGray
                            )
                        }
                    }

                    Text(
                        text = currentReport.executiveSummary,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 16.sp
                        ),
                        color = Color.White
                    )

                    // Dedicated Natural Language Threat Assessment Brief Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF041007),
                        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f)),
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
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = "AI Brief",
                                        tint = Color(0xFF00FF66),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "NATURAL LANGUAGE THREAT ASSESSMENT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.5.sp
                                        ),
                                        color = Color(0xFF00FF66)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val textToCopy = """
                                            [AI DEEP AUDIT THREAT ASSESSMENT]
                                            Level: ${currentReport.threatLevel.label}
                                            Score: ${currentReport.threatScore}/100
                                            Buffer Nodes: ${currentReport.analyzedRfBufferCount}
                                            
                                            ${currentReport.naturalLanguageThreatAssessment}
                                        """.trimIndent()
                                        clipboardManager.setText(AnnotatedString(textToCopy))
                                        copiedFeedback = true
                                    },
                                    modifier = Modifier.size(24.dp).testTag("copy_threat_assessment_brief_button")
                                ) {
                                    Icon(
                                        imageVector = if (copiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = "Copy Assessment",
                                        tint = if (copiedFeedback) Color(0xFF00FF66) else Color.LightGray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Text(
                                text = currentReport.naturalLanguageThreatAssessment,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                ),
                                color = Color(0xFFDDFFDD)
                            )
                        }
                    }

                    // Identified Vectors Tag Cloud
                    if (currentReport.identifiedVectors.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VECTORS:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.Gray
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(currentReport.identifiedVectors) { vector ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF1E1010),
                                        border = BorderStroke(1.dp, Color(0xFFFF5555).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = vector,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color(0xFFFF8888),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Main Action Scan Button
                    Button(
                        onClick = onRunAiThreatScan,
                        enabled = !isAnalyzing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAnalyzing) Color(0xFF1E3A28) else Color(0xFF00FF66),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("run_ai_deep_audit_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF00FF66)
                                )
                                Text(
                                    text = "GEMINI DEEP AUDITING SPECTRUM BUFFER...",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF00FF66)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Scan Icon",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "RUN AI DEEP AUDIT",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tactical Prompt Preset Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "QUICK TACTICAL QUERIES & CHECKS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    ),
                    color = Color.Gray
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val promptChips = listOf(
                        "🔍 Audit for Hidden AirTags/Trackers",
                        "📶 Detect Rogue Wi-Fi / Evil Twins",
                        "🔊 Analyze Ultrasonic Eavesdropping",
                        "⚡ Check EMF Flux / Electronic Bugs",
                        "🛡️ Recommend Immediate Countermeasures"
                    )

                    items(promptChips) { chipText ->
                        Surface(
                            onClick = { onSendCopilotQuery(chipText) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0B2215),
                            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.35f)),
                            modifier = Modifier.testTag("ai_prompt_chip_${chipText.take(10)}")
                        ) {
                            Text(
                                text = chipText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF99FFCC),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Flagged Hostile / Anomalous Emitters
        if (currentReport.flaggedEmitters.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FLAGGED SUSPICIOUS EMITTERS (${currentReport.flaggedEmitters.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFFFF6600)
                    )
                }
            }

            items(currentReport.flaggedEmitters) { emitter ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("flagged_emitter_${emitter.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF140808)),
                    border = BorderStroke(1.dp, Color(0xFFFF4444).copy(alpha = 0.4f))
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
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF330000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Threat",
                                        tint = Color.Red,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = emitter.name.ifBlank { "Unknown Hostile Node" },
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "MAC: ${emitter.macAddress ?: "Randomized"} | ${emitter.signalType}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.5.sp
                                        ),
                                        color = Color.Gray
                                    )
                                }
                            }

                            // Threat category pill
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF3B1212),
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = emitter.threatCategory.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFFFF7777),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Distance & Signal Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EST. DISTANCE: ${"%.1f".format(emitter.distanceMeters)}m",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (emitter.distanceMeters < 5f) Color.Red else Color(0xFFFFCC00)
                            )
                            Text(
                                text = "${emitter.rssiDbm} dBm",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.LightGray
                            )
                        }

                        Text(
                            text = "RISK: ${emitter.riskSummary}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = Color(0xFFFFCCCC)
                        )

                        Text(
                            text = "ACTION: ${emitter.recommendedAction}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF00FF66)
                        )

                        // Action Buttons: Track On Radar & Copilot Audit
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onSelectTargetOnRadar(emitter.id)
                                    onOpenRadarTab()
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = "Track",
                                    tint = Color(0xFF00FF66),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "TRACK ON RADAR",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF00FF66)
                                )
                            }

                            val isAuditingThis = isDeepAuditingEmitterId == emitter.id
                            Button(
                                onClick = {
                                    onTriggerDeepAudit(emitter)
                                },
                                enabled = !isAuditingThis,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAuditingThis) Color(0xFF33200B) else Color(0xFF28180A),
                                    contentColor = Color(0xFFFF9900)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_deep_audit_btn_${emitter.id}")
                            ) {
                                if (isAuditingThis) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFFFF9900)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Audit",
                                        tint = Color(0xFFFF9900),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAuditingThis) "AUDITING..." else "AI DEEP AUDIT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFFFF9900)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Actionable Tactical Countermeasures Checklist
        if (currentReport.countermeasures.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF081910)),
                    border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Countermeasures",
                                tint = Color(0xFF00FF66),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "TACTICAL COUNTERMEASURES",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                        }

                        currentReport.countermeasures.forEach { cm ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF050F09),
                                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (cm.urgency) {
                                            "IMMEDIATE" -> Color(0xFF4A0E0E)
                                            "RECOMMENDED" -> Color(0xFF382C07)
                                            else -> Color(0xFF0F261B)
                                        }
                                    ) {
                                        Text(
                                            text = cm.urgency,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = when (cm.urgency) {
                                                "IMMEDIATE" -> Color(0xFFFF5555)
                                                "RECOMMENDED" -> Color(0xFFFFCC00)
                                                else -> Color(0xFF00FF66)
                                            },
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
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
                                                fontSize = 9.5.sp,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // SIGINT Tactical Copilot Chat
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sigint_copilot_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF06140D)),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.35f))
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Copilot",
                                tint = Color(0xFF00FF66),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "SIGINT TACTICAL COPILOT",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                val fullDossier = buildString {
                                    appendLine("=== SIGINT GEMINI THREAT REPORT ===")
                                    appendLine("Threat Level: ${currentReport.threatLevel.label}")
                                    appendLine("Threat Score: ${currentReport.threatScore}/100")
                                    appendLine("Summary: ${currentReport.executiveSummary}")
                                    appendLine("\nIdentified Vectors: ${currentReport.identifiedVectors.joinToString(", ")}")
                                    appendLine("\nFlagged Emitters:")
                                    currentReport.flaggedEmitters.forEach {
                                        appendLine("- ${it.name} [${it.signalType}] MAC:${it.macAddress} RSSI:${it.rssiDbm}dBm Dist:${"%.1f".format(it.distanceMeters)}m -> ${it.riskSummary}")
                                    }
                                    appendLine("\nCountermeasures:")
                                    currentReport.countermeasures.forEach {
                                        appendLine("- [${it.urgency}] ${it.title}: ${it.detail}")
                                    }
                                }
                                clipboardManager.setText(AnnotatedString(fullDossier))
                                copiedFeedback = true
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (copiedFeedback) Icons.Default.CheckCircle else Icons.Default.Download,
                                contentDescription = "Copy Report",
                                tint = if (copiedFeedback) Color(0xFF00FF66) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Conversation Messages
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF030906), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (copilotMessages.isEmpty()) {
                            Text(
                                text = "Tactical Copilot ready. Ask any question regarding active radio emitters, signal anomalies, or security countermeasures.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = Color.Gray
                            )
                        } else {
                            copilotMessages.takeLast(8).forEach { msg ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (msg.isUser) Color(0xFF0F3B20) else Color(0xFF0A1F13),
                                        border = BorderStroke(1.dp, if (msg.isUser) Color(0xFF00FF66).copy(alpha = 0.4f) else Color(0xFF00FF66).copy(alpha = 0.2f)),
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = if (msg.isUser) "OPERATOR" else "GEMINI COPILOT",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                color = if (msg.isUser) Color(0xFF88FFAA) else Color(0xFF00FF66)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = msg.text,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (isCopilotThinking) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(4.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = Color(0xFF00FF66)
                                )
                                Text(
                                    text = "Gemini is analyzing spectrum telemetry...",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp
                                    ),
                                    color = Color(0xFF00FF66)
                                )
                            }
                        }
                    }

                    // Input Field & Send Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = copilotInputText,
                            onValueChange = { copilotInputText = it },
                            placeholder = {
                                Text(
                                    text = "Ask Gemini about current signals...",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                    color = Color.DarkGray
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (copilotInputText.isNotBlank()) {
                                        val text = copilotInputText.trim()
                                        copilotInputText = ""
                                        onSendCopilotQuery(text)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00FF66),
                                unfocusedBorderColor = Color(0xFF00FF66).copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF00FF66)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("copilot_input_field")
                        )

                        IconButton(
                            onClick = {
                                if (copilotInputText.isNotBlank()) {
                                    val text = copilotInputText.trim()
                                    copilotInputText = ""
                                    onSendCopilotQuery(text)
                                }
                            },
                            enabled = copilotInputText.isNotBlank() && !isCopilotThinking,
                            modifier = Modifier
                                .size(44.dp)
                                .background(if (copilotInputText.isNotBlank()) Color(0xFF00FF66) else Color(0xFF0F261B), RoundedCornerShape(8.dp))
                                .testTag("copilot_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Send Query",
                                tint = if (copilotInputText.isNotBlank()) Color.Black else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet / Dialog for In-Depth Emitter Hardware & Cryptographic SIGINT Audit
    if (selectedDeepAuditTarget != null) {
        TargetDeepAuditDialog(
            audit = selectedDeepAuditTarget,
            onDismiss = onCloseDeepAudit,
            onSelectOnRadar = {
                onSelectTargetOnRadar(it)
                onCloseDeepAudit()
                onOpenRadarTab()
            },
            onAskCopilot = { query ->
                onCloseDeepAudit()
                onSendCopilotQuery(query)
            }
        )
    }
}

@Composable
fun TargetDeepAuditDialog(
    audit: DetailedTargetAudit,
    onDismiss: () -> Unit,
    onSelectOnRadar: (String) -> Unit,
    onAskCopilot: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedAuditFeedback by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .testTag("target_deep_audit_dialog"),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66), contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("deep_audit_close_btn")
            ) {
                Text(
                    text = "DISMISS DOSSIER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onSelectOnRadar(audit.targetId) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00FF66)),
                border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.6f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("deep_audit_track_radar_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "RADAR TRACK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        containerColor = Color(0xFF06140D),
        title = {
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF28180A))
                            .border(1.dp, Color(0xFFFF9900), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Audit",
                            tint = Color(0xFFFF9900),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "AI DEEP SIGINT AUDIT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "${audit.targetName.take(20)} • ${audit.macAddress}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            ),
                            color = Color.Gray
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val dossier = buildString {
                            appendLine("=== GEMINI TARGET DEEP AUDIT ===")
                            appendLine("Target: ${audit.targetName} (${audit.macAddress})")
                            appendLine("Protocol: ${audit.signalType} | RSSI: ${audit.rssiDbm} dBm | Dist: ${"%.1f".format(audit.estimatedDistanceMeters)}m")
                            appendLine("Threat Category: ${audit.threatCategory.label} | Score: ${audit.threatScore}/100")
                            appendLine("Vendor: ${audit.manufacturerVendor}")
                            appendLine("Tracking Confidence: ${audit.trackingHeuristicConfidence}%")
                            appendLine("\nRadio Fingerprint:\n${audit.radioFingerprintSummary}")
                            appendLine("\nSurveillance Risk Analysis:\n${audit.surveillanceRiskAnalysis}")
                            appendLine("\nHardware Analysis:\n${audit.hardwareVectorAnalysis}")
                            appendLine("\nCryptographic Profile:\n${audit.cryptographicProfile}")
                            appendLine("\nVulnerabilities:")
                            audit.vulnerabilities.forEach { v ->
                                appendLine("- [${v.riskLevel.name}] ${v.protocol}: ${v.attackSurface} -> ${v.containmentFix}")
                            }
                            appendLine("\nNeutralization Plan:")
                            audit.stepByStepNeutralizationPlan.forEach { s ->
                                appendLine("- $s")
                            }
                        }
                        clipboardManager.setText(AnnotatedString(dossier))
                        copiedAuditFeedback = true
                    }
                ) {
                    Icon(
                        imageVector = if (copiedAuditFeedback) Icons.Default.CheckCircle else Icons.Default.Download,
                        contentDescription = "Export Target Audit",
                        tint = if (copiedAuditFeedback) Color(0xFF00FF66) else Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (audit.isAuditLoading) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2517)),
                            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color(0xFF00FF66),
                                    strokeWidth = 2.5.dp
                                )
                                Text(
                                    text = "GEMINI 3.5 FLASH DEEP REASONING IN PROGRESS...",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF00FF66)
                                )
                                Text(
                                    text = "Decompiling PHY modulation layer, validating OUI registries, and evaluating tracking heuristics.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp
                                    ),
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }

                // Metric Grid: Threat Score, Tracking Confidence, Vendor
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF140808)),
                            border = BorderStroke(1.dp, Color(0xFFFF5555).copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${audit.threatScore}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = Color(0xFFFF5555)
                                )
                                Text(
                                    text = "THREAT INDEX",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 7.5.sp
                                    ),
                                    color = Color.Gray
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1405)),
                            border = BorderStroke(1.dp, Color(0xFFFFCC00).copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${audit.trackingHeuristicConfidence}%",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = Color(0xFFFFCC00)
                                )
                                Text(
                                    text = "TRACKING RISK",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 7.5.sp
                                    ),
                                    color = Color.Gray
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1F13)),
                            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${audit.rssiDbm} dBm",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = Color(0xFF00FF66)
                                )
                                Text(
                                    text = "EST. ${"%.1f".format(audit.estimatedDistanceMeters)}M",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 7.5.sp
                                    ),
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                // Vendor & Radio Fingerprint
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF081910)),
                        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "IDENTIFIED HARDWARE & VENDOR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp
                                ),
                                color = Color(0xFF00FF66)
                            )
                            Text(
                                text = audit.manufacturerVendor,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "RADIO FINGERPRINT (PHY & LINK):",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp
                                ),
                                color = Color.Gray
                            )
                            Text(
                                text = audit.radioFingerprintSummary,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp
                                ),
                                color = Color(0xFFCCFFDD)
                            )
                        }
                    }
                }

                // In-Depth Surveillance Analysis
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF140B0B)),
                        border = BorderStroke(1.dp, Color(0xFFFF5555).copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "SURVEILLANCE & RECONNAISSANCE THREAT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.5.sp
                                ),
                                color = Color(0xFFFF6666)
                            )
                            Text(
                                text = audit.surveillanceRiskAnalysis,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp,
                                    lineHeight = 14.sp
                                ),
                                color = Color(0xFFFFEEEE)
                            )
                        }
                    }
                }

                // Cryptographic & Hardware Vector Profile
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF07141C)),
                        border = BorderStroke(1.dp, Color(0xFF33CCFF).copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "CRYPTOGRAPHIC & HARDWARE ANALYSIS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp
                                ),
                                color = Color(0xFF33CCFF)
                            )
                            Text(
                                text = "Hardware: ${audit.hardwareVectorAnalysis}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp
                                ),
                                color = Color(0xFFDDFFFF)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Crypto: ${audit.cryptographicProfile}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp
                                ),
                                color = Color(0xFFDDFFFF)
                            )
                        }
                    }
                }

                // Exploit & Protocol Vulnerabilities Matrix
                if (audit.vulnerabilities.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "PROTOCOL VULNERABILITY MATRIX",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                ),
                                color = Color(0xFFFF9900)
                            )

                            audit.vulnerabilities.forEach { v ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0F120A),
                                    border = BorderStroke(1.dp, Color(0xFFFF9900).copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = v.protocol,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                color = Color.White
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = v.riskLevel.color.copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, v.riskLevel.color.copy(alpha = 0.6f))
                                            ) {
                                                Text(
                                                    text = v.riskLevel.label,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 7.5.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = v.riskLevel.color,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Surface: ${v.attackSurface}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp
                                            ),
                                            color = Color.LightGray
                                        )
                                        Text(
                                            text = "Exploit: ${v.exploitationVector}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp
                                            ),
                                            color = Color(0xFFFFBBBB)
                                        )
                                        Text(
                                            text = "Fix: ${v.containmentFix}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color(0xFF00FF66)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Step-By-Step Neutralization SOP
                if (audit.stepByStepNeutralizationPlan.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF061B10)),
                            border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "TARGET NEUTRALIZATION SOP",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp
                                    ),
                                    color = Color(0xFF00FF66)
                                )
                                audit.stepByStepNeutralizationPlan.forEach { step ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "▶",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                            color = Color(0xFF00FF66)
                                        )
                                        Text(
                                            text = step,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Ask Copilot Button for this target
                item {
                    Button(
                        onClick = {
                            onAskCopilot("Pinpoint deep vulnerability breakdown and physical search instructions for ${audit.targetName} (${audit.macAddress}).")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E3A28),
                            contentColor = Color(0xFF00FF66)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ask_copilot_about_target_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ASK COPILOT ABOUT THIS TARGET",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    )
}
