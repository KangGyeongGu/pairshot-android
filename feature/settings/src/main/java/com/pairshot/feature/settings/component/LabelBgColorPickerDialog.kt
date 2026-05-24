package com.pairshot.feature.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotStroke
import com.pairshot.core.ui.R
import com.pairshot.core.ui.component.PairShotDialog
import com.pairshot.core.ui.component.colorpicker.ColorPickerContent
import com.pairshot.core.ui.component.colorpicker.rememberColorPickerState
import com.pairshot.core.ui.component.colorpicker.toHexRgbString
import com.pairshot.feature.settings.R as FeatureR

private const val DISABLED_TEXT_ALPHA = 0.38f

@Composable
internal fun LabelBgColorPickerDialog(
    initialColor: Int,
    borderColorArgb: Int,
    borderEnabled: Boolean,
    initialMatchesBorder: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (color: Int, matchesBorder: Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var matchesBorder by remember { mutableStateOf(initialMatchesBorder) }
    val colorState = rememberColorPickerState(initialColor)

    PairShotDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(FeatureR.string.combine_dialog_bg_color_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PairShotSpacing.lg)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(FeatureR.string.combine_dialog_match_border_color),
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                        if (borderEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_TEXT_ALPHA)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = matchesBorder,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            matchesBorder = it
                        },
                        enabled = borderEnabled,
                    )
                }

                if (matchesBorder) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.sm),
                    ) {
                        Box(
                            modifier =
                            Modifier
                                .size(PairShotSpacing.xl)
                                .clip(MaterialTheme.shapes.small)
                                .background(Color(borderColorArgb))
                                .border(
                                    PairShotStroke.hairline,
                                    MaterialTheme.colorScheme.outline,
                                    MaterialTheme.shapes.small,
                                ),
                        )
                        Text(
                            text = Color(borderColorArgb).toHexRgbString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    ColorPickerContent(state = colorState)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(colorState.currentArgb(), matchesBorder) }) {
                Text(stringResource(R.string.common_button_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_button_cancel))
            }
        },
    )
}
