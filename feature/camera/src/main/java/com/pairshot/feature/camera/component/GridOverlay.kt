package com.pairshot.feature.camera.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.pairshot.core.designsystem.PairShotCameraTokens
import com.pairshot.core.designsystem.PairShotStroke

private const val GRID_DIVISIONS = 3f
private const val SECOND_LINE_MULTIPLIER = 2f

@Composable
fun GridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = PairShotStroke.hairline.toPx()
        val color = PairShotCameraTokens.Foreground.copy(alpha = 0.3f)
        val thirdW = size.width / GRID_DIVISIONS
        val thirdH = size.height / GRID_DIVISIONS

        drawLine(color, Offset(thirdW, 0f), Offset(thirdW, size.height), strokeWidth)
        drawLine(
            color,
            Offset(thirdW * SECOND_LINE_MULTIPLIER, 0f),
            Offset(thirdW * SECOND_LINE_MULTIPLIER, size.height),
            strokeWidth
        )

        drawLine(color, Offset(0f, thirdH), Offset(size.width, thirdH), strokeWidth)
        drawLine(
            color,
            Offset(0f, thirdH * SECOND_LINE_MULTIPLIER),
            Offset(size.width, thirdH * SECOND_LINE_MULTIPLIER),
            strokeWidth
        )
    }
}
