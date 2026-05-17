package com.pairshot.feature.camera.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.PairShotCameraTokens
import com.pairshot.core.designsystem.PairShotStroke
import kotlin.math.abs
import kotlin.math.roundToInt

private const val LEVEL_THRESHOLD_DEGREES = 2f
private const val LEVEL_LINE_WIDTH_FRACTION = 0.4f

@Composable
fun LevelOverlay(
    roll: Float,
    modifier: Modifier = Modifier,
) {
    val isLevel = abs(roll) <= LEVEL_THRESHOLD_DEGREES
    val lineColor = if (isLevel) MaterialTheme.colorScheme.primary else PairShotCameraTokens.Foreground
    val strokeWidth = if (isLevel) PairShotStroke.thin else PairShotStroke.hairline
    val angleText = if (isLevel) "0°" else "${roll.roundToInt()}°"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth(LEVEL_LINE_WIDTH_FRACTION)
                    .height(PairShotStroke.thin),
        ) {
            rotate(degrees = roll) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = strokeWidth.toPx(),
                )
            }
        }
        Text(
            text = angleText,
            color = lineColor,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
