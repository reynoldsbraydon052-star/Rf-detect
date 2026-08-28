package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.cos
import kotlin.math.sin

data class SpatialObservation(
    val x: Float,
    val y: Float,
    val timestampMs: Long,
    val rssi: Int,
    val frequencyMhz: Double,
    val bandLabel: String,
    val signalType: String,
    val deviceId: String,
    val noiseFloor: Float,
    val classification: String?,
    val evidenceScore: Float,
    val sensorSource: String,
    val isEstimatedSourceLocation: Boolean,
    val localizationConfidence: LocalizationConfidence
)

data class SpatialCell(
    val xIndex: Int,
    val yIndex: Int,
    val centerX: Float,
    val centerY: Float,
    val observationCount: Int = 0,
    val averageRssi: Float = -100f,
    val maxRssi: Int = -100,
    val minRssi: Int = 0,
    val estimatedNoiseFloor: Float = -100f,
    val uniqueDevices: Set<String> = emptySet(),
    val wifiCount: Int = 0,
    val bleCount: Int = 0,
    val cellularCount: Int = 0,
    val frequencies: Map<Double, Int> = emptyMap(),
    val bands: Map<String, Int> = emptyMap(),
    val anomalyCount: Int = 0,
    val totalEvidenceScore: Float = 0f
) {
    val averageEvidenceScore: Float
        get() = if (anomalyCount > 0) totalEvidenceScore / anomalyCount else 0f
        
    val dominantFrequency: Double?
        get() = frequencies.maxByOrNull { it.value }?.key
        
    val dominantBand: String?
        get() = bands.maxByOrNull { it.value }?.key
}

data class EnvironmentMapState(
    val cells: Map<Pair<Int, Int>, SpatialCell> = emptyMap(),
    val cellSizeMeters: Float = 1.0f,
    val userPath: List<Pair<Float, Float>> = emptyList(),
    val currentUserX: Float = 0f,
    val currentUserY: Float = 0f,
    val spatialObservations: List<SpatialObservation> = emptyList()
)

class RfEnvironmentMappingEngine {
    private val _mapState = MutableStateFlow(EnvironmentMapState())
    val mapState: StateFlow<EnvironmentMapState> = _mapState.asStateFlow()

    fun updateMap(blips: List<RadarBlip>, headingDegrees: Float, userX: Float = 0f, userY: Float = 0f) {
        _mapState.update { state ->
            
            val newCells = state.cells.toMutableMap()
            val newObservations = state.spatialObservations.toMutableList()
            
            blips.forEach { blip ->
                val absoluteAngleDeg = (headingDegrees + blip.targetAngleOffset) % 360f
                val absoluteAngleRad = Math.toRadians(absoluteAngleDeg.toDouble())
                
                // Estimated source location relative to user
                val targetX = userX + (blip.distance * sin(absoluteAngleRad)).toFloat()
                val targetY = userY - (blip.distance * cos(absoluteAngleRad)).toFloat()
                
                val observation = SpatialObservation(
                    x = targetX,
                    y = targetY,
                    timestampMs = blip.timestampMs,
                    rssi = blip.rssi,
                    frequencyMhz = blip.frequencyMhz,
                    bandLabel = blip.bandLabel,
                    signalType = blip.type,
                    deviceId = blip.id,
                    noiseFloor = -95f, // Estimated
                    classification = blip.anomalyResult?.category?.name,
                    evidenceScore = blip.anomalyResult?.score?.toFloat() ?: 0f,
                    sensorSource = blip.provenance.name,
                    isEstimatedSourceLocation = true,
                    localizationConfidence = blip.localizationConfidence
                )
                
                // Keep history bounded to avoid memory leaks
                if (newObservations.size >= 5000) {
                    newObservations.removeAt(0)
                }
                newObservations.add(observation)
                
                val xIndex = Math.floor(targetX / state.cellSizeMeters.toDouble()).toInt()
                val yIndex = Math.floor(targetY / state.cellSizeMeters.toDouble()).toInt()
                val key = Pair(xIndex, yIndex)
                
                val currentCell = newCells[key] ?: SpatialCell(
                    xIndex = xIndex,
                    yIndex = yIndex,
                    centerX = (xIndex * state.cellSizeMeters) + (state.cellSizeMeters / 2f),
                    centerY = (yIndex * state.cellSizeMeters) + (state.cellSizeMeters / 2f)
                )
                
                val newCount = currentCell.observationCount + 1
                val newAvgRssi = ((currentCell.averageRssi * currentCell.observationCount) + blip.rssi) / newCount
                val newMaxRssi = maxOf(currentCell.maxRssi, blip.rssi)
                val newMinRssi = if (currentCell.minRssi == 0) blip.rssi else minOf(currentCell.minRssi, blip.rssi)
                
                val newUnique = currentCell.uniqueDevices + blip.id
                val newWifi = currentCell.wifiCount + if (blip.type == "WIFI") 1 else 0
                val newBle = currentCell.bleCount + if (blip.type == "BLE") 1 else 0
                val newCellular = currentCell.cellularCount + if (blip.type == "CELLULAR") 1 else 0
                
                val newFreqs = currentCell.frequencies.toMutableMap()
                newFreqs[blip.frequencyMhz] = (newFreqs[blip.frequencyMhz] ?: 0) + 1
                
                val newBands = currentCell.bands.toMutableMap()
                newBands[blip.bandLabel] = (newBands[blip.bandLabel] ?: 0) + 1
                
                val isAnomaly = blip.anomalyResult != null
                val newAnomalyCount = currentCell.anomalyCount + if (isAnomaly) 1 else 0
                val evidence = blip.anomalyResult?.score?.toFloat() ?: 0f
                val newTotalEvidence = currentCell.totalEvidenceScore + evidence
                
                newCells[key] = currentCell.copy(
                    observationCount = newCount,
                    averageRssi = newAvgRssi,
                    maxRssi = newMaxRssi,
                    minRssi = newMinRssi,
                    estimatedNoiseFloor = newMinRssi - 10f,
                    uniqueDevices = newUnique,
                    wifiCount = newWifi,
                    bleCount = newBle,
                    cellularCount = newCellular,
                    frequencies = newFreqs,
                    bands = newBands,
                    anomalyCount = newAnomalyCount,
                    totalEvidenceScore = newTotalEvidence
                )
            }

            // Add user location to path
            val lastPathNode = state.userPath.lastOrNull()
            val newPath = if (lastPathNode == null || distance(lastPathNode.first, lastPathNode.second, userX, userY) > 1f) {
                state.userPath + Pair(userX, userY)
            } else {
                state.userPath
            }
            
            state.copy(
                cells = newCells,
                userPath = newPath,
                currentUserX = userX,
                currentUserY = userY,
                spatialObservations = newObservations
            )
        }
    }
    
    fun setCellSize(sizeMeters: Float) {
        _mapState.update { it.copy(cellSizeMeters = sizeMeters) }
    }
    
    fun clearMap() {
        _mapState.update { EnvironmentMapState(cellSizeMeters = it.cellSizeMeters) }
    }
    
    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return Math.sqrt(Math.pow((x2 - x1).toDouble(), 2.0) + Math.pow((y2 - y1).toDouble(), 2.0)).toFloat()
    }
}
