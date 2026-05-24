package com.pairshot.feature.settings.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

internal const val HIGHLIGHT_PULSE_ON_MS = 600L
internal const val HIGHLIGHT_PULSE_OFF_MS = 400L
private const val HIGHLIGHT_COLOR_ALPHA = 0.3f

@Composable
internal fun HighlightableSettingsCard(
    pulse: Boolean,
    content: @Composable () -> Unit,
) {
    val targetColor =
        if (pulse) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = HIGHLIGHT_COLOR_ALPHA)
        } else {
            MaterialTheme.colorScheme.surface
        }
    val backgroundColor: State<Color> =
        animateColorAsState(
            targetValue = targetColor,
            animationSpec = tween(durationMillis = HIGHLIGHT_PULSE_ON_MS.toInt()),
            label = "settings_highlight_bg",
        )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = backgroundColor.value,
    ) {
        Column {
            content()
        }
    }
}
