package com.pairshot.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.PairShotSnackbarTokens
import kotlinx.coroutines.delay

private const val SNACKBAR_BG_ARGB = 0xF01C1C1EL
private const val DOT_SUCCESS_ARGB = 0xFF30D158L
private const val DOT_INFO_ARGB = 0xFF0A84FFL
private const val DOT_WARNING_ARGB = 0xFFFF9F0AL
private const val DOT_ERROR_ARGB = 0xFFFF453AL
private const val DOT_PRO_ARGB = 0xFFFFD60AL
private const val WARNING_HAPTIC_GAP_MS = 120L
private const val ERROR_HAPTIC_GAP_MS = 90L
private const val SNACKBAR_CORNER_RADIUS_DP = 999

internal val SnackbarBackground = Color(SNACKBAR_BG_ARGB)

private fun dotColor(variant: SnackbarVariant): Color =
    when (variant) {
        SnackbarVariant.SUCCESS -> Color(DOT_SUCCESS_ARGB)
        SnackbarVariant.INFO -> Color(DOT_INFO_ARGB)
        SnackbarVariant.WARNING -> Color(DOT_WARNING_ARGB)
        SnackbarVariant.ERROR -> Color(DOT_ERROR_ARGB)
        SnackbarVariant.PRO_HINT -> Color(DOT_PRO_ARGB)
    }

@Composable
fun PairShotSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    variant: SnackbarVariant = SnackbarVariant.INFO,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(message, variant) {
        performSnackbarHaptic(haptic, variant)
    }

    Surface(
        shape = RoundedCornerShape(SNACKBAR_CORNER_RADIUS_DP.dp),
        color = SnackbarBackground,
        shadowElevation = PairShotSnackbarTokens.elevation,
        modifier =
        modifier
            .widthIn(max = PairShotSnackbarTokens.maxWidth)
            .heightIn(min = PairShotSnackbarTokens.minHeight),
    ) {
        Row(
            modifier =
            Modifier.padding(
                horizontal = PairShotSnackbarTokens.horizontalPadding,
                vertical = PairShotSnackbarTokens.verticalPadding,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PairShotSnackbarTokens.innerGap),
        ) {
            Box(
                modifier =
                Modifier
                    .size(PairShotSnackbarTokens.dotSize)
                    .background(dotColor(variant), CircleShape),
            )
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(
                        text = actionLabel,
                        color = dotColor(variant),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

private suspend fun performSnackbarHaptic(
    haptic: HapticFeedback,
    variant: SnackbarVariant,
) {
    when (variant) {
        SnackbarVariant.SUCCESS -> {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }

        SnackbarVariant.INFO -> {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }

        SnackbarVariant.WARNING -> {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(WARNING_HAPTIC_GAP_MS)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }

        SnackbarVariant.ERROR -> {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(ERROR_HAPTIC_GAP_MS)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(ERROR_HAPTIC_GAP_MS)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }

        SnackbarVariant.PRO_HINT -> {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}
