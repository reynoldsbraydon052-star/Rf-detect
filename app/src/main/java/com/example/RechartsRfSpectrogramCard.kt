package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * High-performance Recharts & WebGL/Canvas RF Spectrum and Scrolling Wave Visualizer.
 * Supports:
 * 1) Scrolling RF Wave (Time-domain real-time waveform with phosphor trails and RSSI bounds)
 * 2) Waterfall Spectrogram (Cascading frequency-time thermal density heatmap)
 * 3) FFT Channel Spectrum Analyzer (Discrete frequency band distribution)
 */
@Composable
fun RechartsRfSpectrogramCard(
    activeBlips: List<RadarBlip>,
    selectedTargetDeviceId: String?,
    onSelectTargetDevice: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var vizMode by remember { mutableStateOf("WAVE") } // "WAVE", "WATERFALL", "FFT"
    var colormap by remember { mutableStateOf("THERMAL") } // "THERMAL", "MATRIX", "CYBER", "PLASMA"
    var isPaused by remember { mutableStateOf(false) }
    var timeWindowSec by remember { mutableStateOf(30) } // 15, 30, 60
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var inspectedFrequency by remember { mutableStateOf<String?>(null) }
    var inspectedRssi by remember { mutableStateOf<String?>(null) }
    var inspectedDeviceName by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val primaryBlip = remember(activeBlips, selectedTargetDeviceId) {
        if (selectedTargetDeviceId != null) {
            activeBlips.firstOrNull { it.id == selectedTargetDeviceId || it.name == selectedTargetDeviceId }
        } else {
            activeBlips.minByOrNull { it.distance }
        }
    }

    val currentPeakRssi = remember(activeBlips) {
        if (activeBlips.isNotEmpty()) activeBlips.maxOf { it.rssi } else -75
    }
    val avgRssi = remember(activeBlips) {
        if (activeBlips.isNotEmpty()) activeBlips.map { it.rssi }.average().toInt() else -80
    }
    val wifiCount = remember(activeBlips) { activeBlips.count { it.type.equals("WIFI", true) } }
    val bleCount = remember(activeBlips) { activeBlips.count { it.type.equals("BLE", true) } }
    val cellCount = remember(activeBlips) { activeBlips.count { it.type.equals("CELLULAR", true) } }

    // Stream telemetry updates into WebView engine
    LaunchedEffect(activeBlips, selectedTargetDeviceId, isPaused, vizMode, colormap, timeWindowSec) {
        if (!isPaused && webViewRef != null) {
            try {
                val blipsArray = JSONArray()
                activeBlips.forEach { blip ->
                    val obj = JSONObject().apply {
                        put("id", blip.id)
                        put("name", blip.name)
                        put("type", blip.type)
                        put("rssi", blip.rssi)
                        put("distance", blip.distance.toDouble())
                        put("freqMhz", blip.frequencyMhz)
                        put("bandLabel", blip.bandLabel)
                        put("isSelected", blip.id == selectedTargetDeviceId || blip.name == selectedTargetDeviceId)
                    }
                    blipsArray.put(obj)
                }

                val configObj = JSONObject().apply {
                    put("mode", vizMode)
                    put("colormap", colormap)
                    put("timeWindowSec", timeWindowSec)
                    put("selectedTargetId", selectedTargetDeviceId ?: "")
                }

                val script = "if (window.updateRfTelemetry) { window.updateRfTelemetry($blipsArray, $configObj); }"
                webViewRef?.evaluateJavascript(script, null)
            } catch (_: Exception) {}
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_rf_spectrogram_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF040C08)),
        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Bar
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
                            .background(Color(0xFF00FF66).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "RF Spectrogram",
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "RF SPECTRUM & SCROLLING WAVE",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.6.sp,
                                fontSize = 12.5.sp
                            ),
                            color = Color(0xFF00FF66)
                        )
                        Text(
                            text = "Recharts-Engineered Signal Intensity Wave & Spectrogram",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            ),
                            color = Color.Gray
                        )
                    }
                }

                // Mode Tabs
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "WAVE" to "WAVE",
                        "WATERFALL" to "SPECTROGRAM",
                        "FFT" to "FFT"
                    ).forEach { (modeKey, label) ->
                        val isSel = vizMode == modeKey
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Color(0xFF00FF66) else Color(0xFF0E2417),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFF00FF66) else Color(0xFF00FF66).copy(alpha = 0.25f)),
                            modifier = Modifier
                                .clickable {
                                    vizMode = modeKey
                                    val script = "if (window.setVizMode) { window.setVizMode('$modeKey'); }"
                                    webViewRef?.evaluateJavascript(script, null)
                                }
                                .testTag("recharts_mode_$modeKey")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (isSel) Color.Black else Color(0xFF00FF66),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Quick Telemetry & Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF07170E), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryMetricItem("PEAK RSSI", "$currentPeakRssi dBm", Color(0xFFFFCC00))
                TelemetryMetricItem("AVG RSSI", "$avgRssi dBm", Color(0xFF00E5FF))
                TelemetryMetricItem("WI-FI APs", "$wifiCount", Color(0xFF00FF66))
                TelemetryMetricItem("BLE BEACONS", "$bleCount", Color(0xFF388E3C))
                TelemetryMetricItem("CELLULAR", "$cellCount", Color(0xFFFF9800))
            }

            // Inspection Callout Banner if user tapped/scrubbed in the webview
            if (inspectedDeviceName != null || selectedTargetDeviceId != null) {
                val displayName = inspectedDeviceName ?: primaryBlip?.name ?: "Selected Target"
                val displayFreq = inspectedFrequency ?: "${primaryBlip?.frequencyMhz?.toInt() ?: 2400} MHz"
                val displayRssi = inspectedRssi ?: "${primaryBlip?.rssi ?: -70} dBm"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0D281A), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF00FF66), CircleShape)
                        )
                        Column {
                            Text(
                                text = "LOCKED INSPECTION • $displayName",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp
                                ),
                                color = Color(0xFF00FF66)
                            )
                            Text(
                                text = "Freq: $displayFreq | Signal: $displayRssi",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.5.sp
                                ),
                                color = Color.LightGray
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            inspectedDeviceName = null
                            inspectedFrequency = null
                            inspectedRssi = null
                            onSelectTargetDevice(null)
                        },
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1B3D28)
                    ) {
                        Text(
                            text = "CLEAR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF00FF66),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Interactive Controls Row (Colormap, Time Window, Pause/Resume)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colormap selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PALETTE:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp
                        ),
                        color = Color.Gray
                    )
                    listOf("THERMAL", "MATRIX", "CYBER", "PLASMA").forEach { p ->
                        val isSel = colormap == p
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSel) Color(0xFF00FF66).copy(alpha = 0.25f) else Color.Transparent,
                            border = BorderStroke(0.8.dp, if (isSel) Color(0xFF00FF66) else Color.DarkGray),
                            modifier = Modifier.clickable {
                                colormap = p
                                val script = "if (window.setColormap) { window.setColormap('$p'); }"
                                webViewRef?.evaluateJavascript(script, null)
                            }
                        ) {
                            Text(
                                text = p.take(4),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 7.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSel) Color(0xFF00FF66) else Color.Gray,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Time window & pause button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(15 to "15s", 30 to "30s", 60 to "60s").forEach { (sec, label) ->
                        val isSel = timeWindowSec == sec
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSel) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color.Transparent,
                            border = BorderStroke(0.8.dp, if (isSel) Color(0xFF00E5FF) else Color.DarkGray),
                            modifier = Modifier.clickable {
                                timeWindowSec = sec
                                val script = "if (window.setTimeWindow) { window.setTimeWindow($sec); }"
                                webViewRef?.evaluateJavascript(script, null)
                            }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 7.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSel) Color(0xFF00E5FF) else Color.Gray,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Play/Pause button
                    IconButton(
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            tint = if (isPaused) Color(0xFFFFCC00) else Color(0xFF00FF66),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Embedded HTML5 Recharts & Canvas Spectrogram View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF010603))
                    .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.cacheMode = WebSettings.LOAD_NO_CACHE
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    // Send initial state immediately
                                    try {
                                        val blipsArray = JSONArray()
                                        activeBlips.forEach { blip ->
                                            val obj = JSONObject().apply {
                                                put("id", blip.id)
                                                put("name", blip.name)
                                                put("type", blip.type)
                                                put("rssi", blip.rssi)
                                                put("distance", blip.distance.toDouble())
                                                put("freqMhz", blip.frequencyMhz)
                                                put("bandLabel", blip.bandLabel)
                                                put("isSelected", blip.id == selectedTargetDeviceId || blip.name == selectedTargetDeviceId)
                                            }
                                            blipsArray.put(obj)
                                        }
                                        val configObj = JSONObject().apply {
                                            put("mode", vizMode)
                                            put("colormap", colormap)
                                            put("timeWindowSec", timeWindowSec)
                                            put("selectedTargetId", selectedTargetDeviceId ?: "")
                                        }
                                        view?.evaluateJavascript("if (window.updateRfTelemetry) { window.updateRfTelemetry($blipsArray, $configObj); }", null)
                                    } catch (_: Exception) {}
                                }
                            }

                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun onDeviceInspected(deviceId: String, name: String, freq: String, rssi: String) {
                                    inspectedDeviceName = name
                                    inspectedFrequency = freq
                                    inspectedRssi = rssi
                                    onSelectTargetDevice(deviceId)
                                }
                            }, "AndroidBridge")

                            loadDataWithBaseURL(null, getRechartsRfHtml(), "text/html", "UTF-8", null)
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        webViewRef = view
                    }
                )
            }
        }
    }
}

/**
 * Self-contained, zero-dependency ultra-high performance Recharts-styled HTML5/SVG/WebGL-Canvas engine.
 * Includes:
 * 1) Recharts-style Area & Line Chart with smooth Bezier curve, gradient fill, grid ticks, dynamic scanline, phosphor glow.
 * 2) Rolling 60 FPS Waterfall Spectrogram with heat color interpolation.
 * 3) FFT Channel Power spectrum graph.
 */
private fun getRechartsRfHtml(): String {
    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }
  body {
    background-color: #010603;
    color: #00FF66;
    font-family: 'Courier New', Courier, monospace;
    overflow: hidden;
    width: 100vw;
    height: 100vh;
    display: flex;
    flex-direction: column;
  }
  #chart-container {
    position: relative;
    width: 100%;
    height: 100%;
  }
  canvas {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
  }
  .hud-overlay {
    position: absolute;
    top: 6px;
    right: 8px;
    font-size: 10px;
    color: #00E5FF;
    background: rgba(0, 20, 10, 0.7);
    padding: 3px 6px;
    border-radius: 4px;
    border: 1px solid rgba(0, 255, 102, 0.3);
    pointer-events: none;
  }
</style>
</head>
<body>
<div id="chart-container">
  <canvas id="rfCanvas"></canvas>
  <div id="hud" class="hud-overlay">LIVE 60FPS SWEEP</div>
</div>

<script>
  const canvas = document.getElementById('rfCanvas');
  const ctx = canvas.getContext('2d');
  const hud = document.getElementById('hud');

  let width = 0;
  let height = 0;

  function resize() {
    width = window.innerWidth;
    height = window.innerHeight;
    canvas.width = width * window.devicePixelRatio;
    canvas.height = height * window.devicePixelRatio;
    ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
  }
  window.addEventListener('resize', resize);
  resize();

  let mode = 'WAVE'; // 'WAVE', 'WATERFALL', 'FFT'
  let colormap = 'THERMAL';
  let timeWindowSec = 30;
  let selectedTargetId = '';

  // History buffer: array of { timestamp, blips: [...] }
  const historyBuffer = [];
  const maxHistoryPoints = 120;

  // Waterfall offscreen buffer
  const waterfallHistory = [];
  const maxWaterfallRows = 90;

  window.setVizMode = function(m) {
    mode = m;
  };
  window.setColormap = function(c) {
    colormap = c;
  };
  window.setTimeWindow = function(tw) {
    timeWindowSec = tw;
  };

  window.updateRfTelemetry = function(blips, config) {
    if (config) {
      if (config.mode) mode = config.mode;
      if (config.colormap) colormap = config.colormap;
      if (config.timeWindowSec) timeWindowSec = config.timeWindowSec;
      if (config.selectedTargetId !== undefined) selectedTargetId = config.selectedTargetId;
    }

    const now = Date.now();
    historyBuffer.push({
      timestamp: now,
      blips: blips || []
    });

    if (historyBuffer.length > maxHistoryPoints) {
      historyBuffer.shift();
    }

    // Generate a spectrum row for waterfall (64 frequency bins from 100MHz to 6GHz)
    const bins = new Float32Array(64);
    for (let i = 0; i < 64; i++) {
      bins[i] = -98.0 + (Math.random() * 4.0 - 2.0); // Baseline noise floor
    }

    if (blips && blips.length > 0) {
      blips.forEach(b => {
        const freq = b.freqMhz || 2400;
        // Map 100MHz-6500MHz to 0-63
        const binIdx = Math.min(63, Math.max(0, Math.floor((freq - 100) / (6500 - 100) * 64)));
        const power = b.rssi || -75;
        bins[binIdx] = Math.max(bins[binIdx], power);
        // Spread adjacent
        if (binIdx > 0) bins[binIdx - 1] = Math.max(bins[binIdx - 1], power - 8);
        if (binIdx < 63) bins[binIdx + 1] = Math.max(bins[binIdx + 1], power - 8);
      });
    }

    waterfallHistory.unshift(bins);
    if (waterfallHistory.length > maxWaterfallRows) {
      waterfallHistory.pop();
    }
  };

  // Color helper functions
  function getColorForIntensity(norm, pal) {
    norm = Math.max(0, Math.min(1, norm));
    if (pal === 'MATRIX') {
      return `rgb(0, ${'$'}{Math.floor(norm * 255)}, ${'$'}{Math.floor(norm * 80)})`;
    } else if (pal === 'CYBER') {
      const r = Math.floor(norm * 40);
      const g = Math.floor(norm * 230);
      const b = Math.floor(100 + norm * 155);
      return `rgb(${'$'}{r}, ${'$'}{g}, ${'$'}{b})`;
    } else if (pal === 'PLASMA') {
      const r = Math.floor(norm * 255);
      const g = Math.floor((1 - norm) * 40 + norm * 80);
      const b = Math.floor((1 - norm) * 180 + norm * 240);
      return `rgb(${'$'}{r}, ${'$'}{g}, ${'$'}{b})`;
    } else {
      // THERMAL
      if (norm < 0.25) {
        return `rgb(0, ${'$'}{Math.floor(norm * 4 * 200)}, 255)`;
      } else if (norm < 0.5) {
        const t = (norm - 0.25) * 4;
        return `rgb(0, 255, ${'$'}{Math.floor((1 - t) * 255)})`;
      } else if (norm < 0.75) {
        const t = (norm - 0.5) * 4;
        return `rgb(${'$'}{Math.floor(t * 255)}, 255, 0)`;
      } else {
        const t = (norm - 0.75) * 4;
        return `rgb(255, ${'$'}{Math.floor((1 - t) * 200)}, 0)`;
      }
    }
  }

  // Touch / Tap interaction for inspection
  canvas.addEventListener('click', (e) => {
    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    if (historyBuffer.length > 0) {
      const latest = historyBuffer[historyBuffer.length - 1];
      if (latest.blips && latest.blips.length > 0) {
        const blip = latest.blips[0];
        if (window.AndroidBridge && window.AndroidBridge.onDeviceInspected) {
          window.AndroidBridge.onDeviceInspected(
            blip.id,
            blip.name,
            blip.freqMhz + " MHz",
            blip.rssi + " dBm"
          );
        }
      }
    }
  });

  // Render loop
  let sweepPhase = 0;
  function render() {
    ctx.clearRect(0, 0, width, height);

    const padding = { top: 22, right: 16, bottom: 24, left: 42 };
    const graphW = width - padding.left - padding.right;
    const graphH = height - padding.top - padding.bottom;

    if (mode === 'WAVE') {
      renderScrollingWave(padding, graphW, graphH);
    } else if (mode === 'WATERFALL') {
      renderWaterfallSpectrogram(padding, graphW, graphH);
    } else {
      renderFftSpectrum(padding, graphW, graphH);
    }

    sweepPhase = (sweepPhase + 0.015) % 1;
    requestAnimationFrame(render);
  }

  function renderScrollingWave(pad, gw, gh) {
    // Draw Background Grid (Recharts Style)
    const minDbm = -100;
    const maxDbm = -20;

    ctx.strokeStyle = 'rgba(0, 255, 102, 0.12)';
    ctx.lineWidth = 1;
    ctx.setLineDash([4, 4]);

    for (let dbm = minDbm; dbm <= maxDbm; dbm += 20) {
      const y = pad.top + (1 - (dbm - minDbm) / (maxDbm - minDbm)) * gh;
      ctx.beginPath();
      ctx.moveTo(pad.left, y);
      ctx.lineTo(pad.left + gw, y);
      ctx.stroke();

      ctx.fillStyle = '#00FF66';
      ctx.font = '9px monospace';
      ctx.fillText(`${'$'}{dbm}`, 6, y + 3);
    }
    ctx.setLineDash([]);

    if (historyBuffer.length < 2) return;

    // Collect trace points for primary/selected blip & secondary signals
    const pts = [];
    const stepX = gw / (historyBuffer.length - 1);

    for (let i = 0; i < historyBuffer.length; i++) {
      const snapshot = historyBuffer[i];
      let val = -80;
      if (snapshot.blips && snapshot.blips.length > 0) {
        const target = snapshot.blips.find(b => b.isSelected) || snapshot.blips[0];
        val = target.rssi || -80;
      }
      const normY = 1 - (val - minDbm) / (maxDbm - minDbm);
      const clampedNorm = Math.max(0, Math.min(1, normY));
      pts.push({
        x: pad.left + i * stepX,
        y: pad.top + clampedNorm * gh,
        rssi: val
      });
    }

    // Draw Gradient Area under curve (Recharts style AreaChart)
    const grad = ctx.createLinearGradient(0, pad.top, 0, pad.top + gh);
    grad.addColorStop(0, 'rgba(0, 255, 102, 0.45)');
    grad.addColorStop(0.6, 'rgba(0, 229, 255, 0.20)');
    grad.addColorStop(1, 'rgba(0, 20, 10, 0.02)');

    ctx.beginPath();
    ctx.moveTo(pts[0].x, pad.top + gh);
    for (let i = 0; i < pts.length; i++) {
      if (i === 0) {
        ctx.lineTo(pts[i].x, pts[i].y);
      } else {
        const prev = pts[i - 1];
        const cx = (prev.x + pts[i].x) / 2;
        ctx.quadraticCurveTo(prev.x, prev.y, cx, (prev.y + pts[i].y) / 2);
      }
    }
    ctx.lineTo(pts[pts.length - 1].x, pts[pts.length - 1].y);
    ctx.lineTo(pts[pts.length - 1].x, pad.top + gh);
    ctx.closePath();
    ctx.fillStyle = grad;
    ctx.fill();

    // Draw Smooth Neon Glowing Curve Line
    ctx.strokeStyle = '#00FF66';
    ctx.lineWidth = 2.5;
    ctx.shadowColor = '#00FF66';
    ctx.shadowBlur = 10;

    ctx.beginPath();
    ctx.moveTo(pts[0].x, pts[0].y);
    for (let i = 1; i < pts.length; i++) {
      const prev = pts[i - 1];
      const cx = (prev.x + pts[i].x) / 2;
      ctx.quadraticCurveTo(prev.x, prev.y, cx, (prev.y + pts[i].y) / 2);
    }
    ctx.lineTo(pts[pts.length - 1].x, pts[pts.length - 1].y);
    ctx.stroke();
    ctx.shadowBlur = 0;

    // Draw Peak Leading Pulse Dot
    const lastPt = pts[pts.length - 1];
    ctx.fillStyle = '#FFFFFF';
    ctx.beginPath();
    ctx.arc(lastPt.x, lastPt.y, 4.5, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = '#00E5FF';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(lastPt.x, lastPt.y, 8, 0, Math.PI * 2);
    ctx.stroke();

    hud.innerText = `RSSI: ${'$'}{lastPt.rssi} dBm • WAVE 60FPS`;
  }

  function renderWaterfallSpectrogram(pad, gw, gh) {
    if (waterfallHistory.length === 0) return;

    const rowH = gh / maxWaterfallRows;
    const colW = gw / 64;

    for (let r = 0; r < waterfallHistory.length; r++) {
      const row = waterfallHistory[r];
      const y = pad.top + r * rowH;

      for (let c = 0; c < 64; c++) {
        const val = row[c];
        // RSSI: -100 to -30
        const norm = (val - (-100)) / ((-30) - (-100));
        ctx.fillStyle = getColorForIntensity(norm, colormap);
        ctx.fillRect(pad.left + c * colW, y, colW + 0.8, rowH + 0.8);
      }
    }

    // Grid Axis Labels
    ctx.fillStyle = '#00FF66';
    ctx.font = '8.5px monospace';
    ctx.fillText('100M', pad.left, pad.top + gh + 14);
    ctx.fillText('2.4G', pad.left + gw * 0.38, pad.top + gh + 14);
    ctx.fillText('5.0G', pad.left + gw * 0.75, pad.top + gh + 14);
    ctx.fillText('6.5G UWB', pad.left + gw - 38, pad.top + gh + 14);

    hud.innerText = `WATERFALL • ${'$'}{colormap} HEATMAP`;
  }

  function renderFftSpectrum(pad, gw, gh) {
    if (waterfallHistory.length === 0) return;
    const latestBins = waterfallHistory[0];
    const binW = (gw / 64) - 1.5;

    ctx.strokeStyle = 'rgba(0, 255, 102, 0.15)';
    ctx.lineWidth = 1;
    for (let dbm = -100; dbm <= -20; dbm += 20) {
      const y = pad.top + (1 - (dbm - (-100)) / (80)) * gh;
      ctx.beginPath();
      ctx.moveTo(pad.left, y);
      ctx.lineTo(pad.left + gw, y);
      ctx.stroke();
    }

    for (let i = 0; i < 64; i++) {
      const val = latestBins[i];
      const norm = Math.max(0, Math.min(1, (val - (-100)) / 80));
      const barH = norm * gh;
      const x = pad.left + i * (gw / 64);
      const y = pad.top + gh - barH;

      const grad = ctx.createLinearGradient(0, y, 0, pad.top + gh);
      grad.addColorStop(0, '#00FF66');
      grad.addColorStop(1, '#003311');

      ctx.fillStyle = grad;
      ctx.fillRect(x, y, binW, barH);
    }

    hud.innerText = `FFT SPECTRUM BINS (64-CH)`;
  }

  requestAnimationFrame(render);
</script>
</body>
</html>
""".trimIndent()
}
