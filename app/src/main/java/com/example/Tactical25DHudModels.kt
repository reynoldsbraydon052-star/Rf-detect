package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Geometric Leader Line configuration connecting a locked radar target to the floating HUD card.
 */
data class TargetLockLeaderLine(
    val originNodePos: Offset,
    val inflectionPoint: Offset,
    val hudAnchorPos: Offset,
    val lineColor: Color = Color(0xFF00E5FF),
    val strokeWidthPx: Float = 1.5f,
    val isVisible: Boolean = false
)
