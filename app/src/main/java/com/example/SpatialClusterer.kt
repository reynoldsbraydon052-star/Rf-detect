package com.example

import androidx.compose.ui.geometry.Offset
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

/**
 * Dynamic Spatial Clustering and Micro-Glyph Engine.
 *
 * Implements:
 * 1. LOD Grid Bucket Filter: Groups spatial targets within a tight screen threshold (e.g. < 32px / 0.5m)
 *    into compact hexagonal cluster nodes with count badges "[ n ]".
 * 2. Threat Grading: Classifies targets into LOW_RISK (Green), MEDIUM_ALERT (Amber), HIGH_THREAT (Red).
 * 3. Micro-Glyph Classification: Assigns compact vector glyphs (Tracker Tag, Broadcast Tower, Core Dot).
 * 4. Phosphor Trail Ring Buffer: Retains the last 5 historical coordinates per target ID with temporal decay.
 */
class SpatialClusterer(
    var clusterDistanceThresholdPx: Float = 32.0f,
    private val maxTrailPoints: Int = 5
) {
    // Snail trail history ring buffer per target ID
    private val trailHistoryMap = ConcurrentHashMap<String, ArrayDeque<Offset>>()

    // Set of expanded cluster IDs
    private val expandedClusterIds = mutableSetOf<String>()

    fun toggleClusterExpanded(clusterId: String) {
        if (expandedClusterIds.contains(clusterId)) {
            expandedClusterIds.remove(clusterId)
        } else {
            expandedClusterIds.add(clusterId)
        }
    }

    /**
     * Clusters raw radar blips after 2.5D isometric projection.
     */
    fun clusterTargets(
        blips: List<RadarBlip>,
        transformer: IsometricRadarTransformer,
        mapRangeMeters: Float,
        centerX: Float,
        centerY: Float,
        maxRadius: Float,
        headingRotationDeg: Float = 0f,
        zoomFactor: Float = 1.0f,
        panOffset: Offset = Offset.Zero,
        selectedTargetId: String? = null,
        nearestBlipId: String? = null,
        perimeterThresholdMeters: Float = 3.0f
    ): ClusteredRadarFrame {
        if (blips.isEmpty()) {
            return ClusteredRadarFrame(emptyList(), emptyList())
        }

        // Step 1: Project all blips to 2.5D space and record phosphor trail
        val targetNodes = blips.map { blip ->
            val projected = transformer.transformTo25D(
                distanceMeters = blip.distance,
                angleOffsetDeg = blip.targetAngleOffset,
                zOffsetMeters = blip.estimatedZOffsetMeters,
                mapRangeMeters = mapRangeMeters,
                centerX = centerX,
                centerY = centerY,
                maxRadius = maxRadius,
                headingRotationDeg = headingRotationDeg,
                zoomFactor = zoomFactor,
                panOffset = panOffset
            )

            // Update phosphor trail ring buffer
            val queue = trailHistoryMap.computeIfAbsent(blip.id) { ArrayDeque() }
            val currentPos = projected.elevatedScreenPos
            if (queue.isEmpty() || distanceBetween(queue.last(), currentPos) > 4.0f) {
                queue.addLast(currentPos)
                if (queue.size > maxTrailPoints) {
                    queue.removeFirst()
                }
            }

            val glyph = classifyGlyph(blip)
            val threat = evaluateThreat(blip, perimeterThresholdMeters)

            SpatialTargetNode(
                blip = blip,
                projected = projected,
                glyphType = glyph,
                threatGrade = threat,
                isSelected = (blip.id == selectedTargetId),
                isNearest = (blip.id == nearestBlipId),
                trailHistory = queue.toList()
            )
        }

        // Clean stale trails for disappeared targets
        val activeIds = blips.map { it.id }.toSet()
        trailHistoryMap.keys.retainAll(activeIds)

        // Step 2: Spatial Aggregation / Grid Bucketing
        val singleNodes = mutableListOf<SpatialTargetNode>()
        val clusterNodes = mutableListOf<SpatialClusterNode>()
        val visited = BooleanArray(targetNodes.size)

        for (i in targetNodes.indices) {
            if (visited[i]) continue

            val baseNode = targetNodes[i]
            val clusterGroup = mutableListOf<SpatialTargetNode>()
            clusterGroup.add(baseNode)
            visited[i] = true

            // Look for neighbors within clusterDistanceThresholdPx
            for (j in i + 1 until targetNodes.size) {
                if (visited[j]) continue
                val candidate = targetNodes[j]
                val dist = distanceBetween(baseNode.projected.elevatedScreenPos, candidate.projected.elevatedScreenPos)

                if (dist <= clusterDistanceThresholdPx) {
                    clusterGroup.add(candidate)
                    visited[j] = true
                }
            }

            if (clusterGroup.size == 1) {
                singleNodes.add(clusterGroup[0])
            } else {
                // Generate deterministic cluster ID
                val clusterId = "cluster_${clusterGroup.minOf { it.blip.id }}"
                val isExpanded = expandedClusterIds.contains(clusterId)

                if (isExpanded) {
                    // When expanded, expose individual nodes
                    singleNodes.addAll(clusterGroup)
                } else {
                    // Compute mean centroid with zero intermediate allocations
                    var sumAnchorX = 0f
                    var sumAnchorY = 0f
                    var sumElevX = 0f
                    var sumElevY = 0f
                    var sumDepth = 0f
                    var dominantThreat = RadarThreatGrade.LOW_RISK
                    var hasSelected = false

                    for (node in clusterGroup) {
                        sumAnchorX += node.projected.groundAnchor.x
                        sumAnchorY += node.projected.groundAnchor.y
                        sumElevX += node.projected.elevatedScreenPos.x
                        sumElevY += node.projected.elevatedScreenPos.y
                        sumDepth += node.projected.cameraDepth

                        if (node.threatGrade == RadarThreatGrade.HIGH_THREAT) {
                            dominantThreat = RadarThreatGrade.HIGH_THREAT
                        } else if (node.threatGrade == RadarThreatGrade.MEDIUM_ALERT && dominantThreat != RadarThreatGrade.HIGH_THREAT) {
                            dominantThreat = RadarThreatGrade.MEDIUM_ALERT
                        }

                        if (node.isSelected) {
                            hasSelected = true
                        }
                    }

                    val groupSize = clusterGroup.size.toFloat()
                    val avgAnchorX = sumAnchorX / groupSize
                    val avgAnchorY = sumAnchorY / groupSize
                    val avgElevX = sumElevX / groupSize
                    val avgElevY = sumElevY / groupSize
                    val avgDepth = sumDepth / groupSize

                    clusterNodes.add(
                        SpatialClusterNode(
                            clusterId = clusterId,
                            anchorPosition = Offset(avgAnchorX, avgAnchorY),
                            elevatedPosition = Offset(avgElevX, avgElevY),
                            targets = clusterGroup,
                            dominantThreat = dominantThreat,
                            isExpanded = false,
                            isSelected = hasSelected,
                            cameraDepth = avgDepth
                        )
                    )
                }
            }
        }

        // In-place sort by camera depth descending (furthest first, closest last for Painter's algorithm)
        singleNodes.sortWith { a, b -> b.projected.cameraDepth.compareTo(a.projected.cameraDepth) }
        clusterNodes.sortWith { a, b -> b.cameraDepth.compareTo(a.cameraDepth) }

        var selectedTarget: SpatialTargetNode? = null
        for (node in singleNodes) {
            if (node.isSelected) {
                selectedTarget = node
                break
            }
        }
        if (selectedTarget == null) {
            for (cluster in clusterNodes) {
                for (node in cluster.targets) {
                    if (node.isSelected) {
                        selectedTarget = node
                        break
                    }
                }
                if (selectedTarget != null) break
            }
        }

        var selectedCluster: SpatialClusterNode? = null
        for (cluster in clusterNodes) {
            if (cluster.isSelected) {
                selectedCluster = cluster
                break
            }
        }

        return ClusteredRadarFrame(
            singleNodes = singleNodes,
            clusterNodes = clusterNodes,
            selectedTarget = selectedTarget,
            selectedCluster = selectedCluster
        )
    }

    private fun classifyGlyph(blip: RadarBlip): RadarGlyphType {
        val nameLower = blip.name.lowercase()
        val typeUpper = blip.type.uppercase()

        return when {
            nameLower.contains("airtag") || nameLower.contains("smarttag") ||
            nameLower.contains("tile") || nameLower.contains("tracker") ||
            blip.isHighRiskVendor -> RadarGlyphType.TRACKER_TAG

            typeUpper == "WIFI" || nameLower.contains("ap_") ||
            nameLower.contains("router") || nameLower.contains("netgear") -> RadarGlyphType.BROADCAST_TOWER

            typeUpper == "MAGNETIC" -> RadarGlyphType.MAGNETIC_ANOMALY
            typeUpper == "AUDIO" -> RadarGlyphType.AUDIO_ULTRASONIC
            else -> RadarGlyphType.BLE_CORE_DOT
        }
    }

    private fun evaluateThreat(blip: RadarBlip, perimeterThresholdMeters: Float): RadarThreatGrade {
        return when {
            blip.isHighRiskVendor || blip.distance < 1.0f || blip.anomalyResult != null -> RadarThreatGrade.HIGH_THREAT
            blip.baselineState == BaselineState.ANOMALOUS || blip.distance <= perimeterThresholdMeters -> RadarThreatGrade.MEDIUM_ALERT
            else -> RadarThreatGrade.LOW_RISK
        }
    }

    private fun distanceBetween(p1: Offset, p2: Offset): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }

    fun clearTrails() {
        trailHistoryMap.clear()
        expandedClusterIds.clear()
    }
}
