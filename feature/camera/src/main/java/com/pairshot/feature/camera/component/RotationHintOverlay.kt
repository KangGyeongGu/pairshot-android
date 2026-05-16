package com.pairshot.feature.camera.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotCard
import com.pairshot.core.designsystem.PairShotTouchTarget

@Composable
internal fun RotationHintOverlay(
    direction: RotationHintDirection?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = direction != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            val shown = direction ?: RotationHintDirection.LEFT
            val transition = rememberInfiniteTransition(label = "rotation_hint")
            val phase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = HINT_CYCLE_DURATION_MS, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "rotation_hint_phase",
            )
            val hintAlpha =
                when {
                    phase < HINT_FADE_IN_END -> phase / HINT_FADE_IN_END
                    phase < HINT_FADE_OUT_START -> 1f
                    else -> ((1f - phase) / (1f - HINT_FADE_OUT_START)).coerceAtLeast(0f)
                }
            val signedAngle = phase * HINT_SWEEP_DEGREES * shown.angleSign

            Column(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(HintCornerRadius))
                        .background(Color.Black.copy(alpha = HINT_BACKGROUND_ALPHA))
                        .padding(
                            horizontal = PairShotCard.innerPadding,
                            vertical = PairShotCard.innerPadding,
                        ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PairShotSpacing.sm),
            ) {
                Icon(
                    imageVector = shown.icon,
                    contentDescription = stringResource(shown.descRes),
                    tint = Color.White,
                    modifier =
                        Modifier
                            .size(PairShotTouchTarget.standard)
                            .rotate(signedAngle)
                            .alpha(hintAlpha),
                )
                Text(
                    text = stringResource(shown.messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }
        }
    }
}

private val HintCornerRadius = PairShotSpacing.md
private const val HINT_BACKGROUND_ALPHA = 0.6f
private const val HINT_SWEEP_DEGREES = 90f
private const val HINT_CYCLE_DURATION_MS = 2000
private const val HINT_FADE_IN_END = 0.2f
private const val HINT_FADE_OUT_START = 0.6f
