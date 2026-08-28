package com.example

import kotlin.math.sqrt

/**
 * Tracks candidate optical retroreflection glints over consecutive strobe frame cycles.
 *
 * Implements:
 * 1. Spatial Nearest-Neighbor Matching (tolerance radius: 15.0 pixels).
 * 2. Persistence Accumulation (increments persistence count for matched tracks).
 * 3. Telephoto Handoff Activation: When persistence exceeds 5 frames, flags [isTelephotoHandoffReady] = true
 *    and elevates to [activeHandoffTarget].
 * 4. Stale Target Eviction: Purges tracks unobserved for > maxMissedFrames cycles.
 */
class OpticalTargetTracker(
    val matchDistanceTolerancePixels: Float = 15.0f,
    val persistenceThresholdFrames: Int = 5,
    val maxMissedFrames: Int = 3
) {

    private data class TrackedTargetInternal(
        var target: OpticalGlintTarget,
        var consecutiveHits: Int = 1,
        var missedCycles: Int = 0
    )

    private val activeTracks = mutableListOf<TrackedTargetInternal>()
    private var trackCounter = 0L

    /**
     * Updates active tracks with newly extracted candidates from the current strobe differencing cycle.
     */
    @Synchronized
    fun updateTracks(
        candidates: List<OpticalGlintTarget>,
        timestampMs: Long = System.currentTimeMillis()
    ): Pair<List<OpticalGlintTarget>, OpticalGlintTarget?> {
        val matchedTrackIndices = mutableSetOf<Int>()
        val matchedCandidateIndices = mutableSetOf<Int>()

        // Spatial nearest-neighbor matching
        for (cIdx in candidates.indices) {
            val candidate = candidates[cIdx]
            var bestTrackIdx = -1
            var bestDistance = Float.MAX_VALUE

            for (tIdx in activeTracks.indices) {
                if (matchedTrackIndices.contains(tIdx)) continue
                val existing = activeTracks[tIdx].target
                val dx = candidate.xPixel - existing.xPixel
                val dy = candidate.yPixel - existing.yPixel
                val dist = sqrt(dx * dx + dy * dy)

                if (dist <= matchDistanceTolerancePixels && dist < bestDistance) {
                    bestDistance = dist
                    bestTrackIdx = tIdx
                }
            }

            if (bestTrackIdx != -1) {
                matchedTrackIndices.add(bestTrackIdx)
                matchedCandidateIndices.add(cIdx)

                val track = activeTracks[bestTrackIdx]
                track.consecutiveHits++
                track.missedCycles = 0

                val isHandoffReady = track.consecutiveHits > persistenceThresholdFrames

                // Smooth coordinates with Exponential Moving Average
                val smoothedX = (0.7f * candidate.xPixel) + (0.3f * track.target.xPixel)
                val smoothedY = (0.7f * candidate.yPixel) + (0.3f * track.target.yPixel)

                track.target = candidate.copy(
                    id = track.target.id,
                    xPixel = smoothedX,
                    yPixel = smoothedY,
                    persistenceFrameCount = track.consecutiveHits,
                    isTelephotoHandoffReady = isHandoffReady,
                    timestampMs = timestampMs
                )
            }
        }

        // Add unmatched candidates as new tracks
        for (cIdx in candidates.indices) {
            if (!matchedCandidateIndices.contains(cIdx)) {
                trackCounter++
                val newTarget = candidates[cIdx].copy(
                    id = "TARGET_${trackCounter}_%04d".format((candidates[cIdx].xPixel).toInt()),
                    persistenceFrameCount = 1,
                    isTelephotoHandoffReady = false,
                    timestampMs = timestampMs
                )
                activeTracks.add(TrackedTargetInternal(newTarget, consecutiveHits = 1, missedCycles = 0))
            }
        }

        // Increment missed count for unmatched tracks
        val iterator = activeTracks.iterator()
        while (iterator.hasNext()) {
            val track = iterator.next()
            val trackIdx = activeTracks.indexOf(track)
            if (!matchedTrackIndices.contains(trackIdx) && !matchedCandidateIndices.contains(trackIdx)) {
                track.missedCycles++
                if (track.missedCycles > maxMissedFrames) {
                    iterator.remove()
                }
            }
        }

        val resultList = activeTracks.map { it.target }
        val handoffTarget = resultList
            .filter { it.isTelephotoHandoffReady }
            .maxByOrNull { it.persistenceFrameCount * 10 + it.deltaLuminance }

        return Pair(resultList, handoffTarget)
    }

    /**
     * Clears all active tracking targets.
     */
    @Synchronized
    fun reset() {
        activeTracks.clear()
        trackCounter = 0L
    }
}
