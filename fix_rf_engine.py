import re

with open('app/src/main/java/com/example/RfEnvironmentMappingEngine.kt', 'r') as f:
    content = f.read()

# Add spatialObservations to EnvironmentMapState
content = content.replace("val currentUserY: Float = 0f", "val currentUserY: Float = 0f,\n    val spatialObservations: List<SpatialObservation> = emptyList()")

# Update updateMap to generate spatial observations
new_update_logic = """
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
"""

content = re.sub(r'val newCells = state\.cells\.toMutableMap\(\).*?// Add user location to path', new_update_logic + "\n            // Add user location to path", content, flags=re.DOTALL)
content = content.replace("currentUserY = userY", "currentUserY = userY,\n                spatialObservations = newObservations")

with open('app/src/main/java/com/example/RfEnvironmentMappingEngine.kt', 'w') as f:
    f.write(content)
