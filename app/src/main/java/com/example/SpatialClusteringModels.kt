package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Threat severity classification for radar nodes.
 */
enum class RadarThreatGrade(val color: Color, val hexLabel: String) {
    LOW_RISK(Color(0xFF00E676), "#00E676"),       // Stable, known peripheral
    MEDIUM_ALERT(Color(0xFFFFB300), "#FFB300"),   // Signal surge, rapid RSSI drift, MAC rotation
    HIGH_THREAT(Color(0xFFFF1744), "#FF1744")     // Active tracker, breach proximity (<3ft), active alert
}

/**
 * Micro-glyph classification for tactical target markers.
 */
enum class RadarGlyphType {
    TRACKER_TAG,      // AirTag, SmartTag, Tile, Tracker
    BROADCAST_TOWER,  // WiFi Access Point, Router
    BLE_CORE_DOT,     // Unknown BLE peripheral, sensor
    MAGNETIC_ANOMALY, // EMF / Magnetic spike
    AUDIO_ULTRASONIC  // Acoustic beacon
}

/**
 * Single spatial target node with 2.5D position and phosphor trail history.
 */
data class SpatialTargetNode(
    val blip: RadarBlip,
    val projected: IsometricRadarTransformer.Projected25DPoint,
    val glyphType: RadarGlyphType,
    val threatGrade: RadarThreatGrade,
    val isSelected: Boolean,
    val isNearest: Boolean,
    val trailHistory: List<Offset> = emptyList() // Last 5 historical positions for phosphor trail
)

/**
 * Clustered node grouping multiple targets within close spatial proximity.
 */
data class SpatialClusterNode(
    val clusterId: String,
    val anchorPosition: Offset,
    val elevatedPosition: Offset,
    val targets: List<SpatialTargetNode>,
    val dominantThreat: RadarThreatGrade,
    val isExpanded: Boolean = false,
    val isSelected: Boolean = false,
    val cameraDepth: Float = 0f
) {
    val count: Int get() = targets.size
}

/**
 * Combined output of spatial clustering containing single nodes and clustered nodes.
 */
data class ClusteredRadarFrame(
    val singleNodes: List<SpatialTargetNode>,
    val clusterNodes: List<SpatialClusterNode>,
    val selectedTarget: SpatialTargetNode? = null,
    val selectedCluster: SpatialClusterNode? = null
) {
    companion object {
        val EMPTY = ClusteredRadarFrame(emptyList(), emptyList(), null, null)
    }
}
