package com.pairshot.feature.settings.section

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.pairshot.core.designsystem.PairShotCard
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotTouchTarget
import com.pairshot.core.ui.component.SettingsCard
import com.pairshot.core.ui.component.SettingsDivider
import com.pairshot.core.ui.component.SettingsItem
import com.pairshot.core.ui.component.SettingsSliderItem
import com.pairshot.core.ui.component.WarningBadge
import com.pairshot.feature.settings.R
import kotlin.math.roundToInt

private const val SWITCH_SCALE = 0.67f
private const val OPACITY_WARNING_THRESHOLD = 0.75f

@Composable
internal fun SettingsCaptureSection(
    qualityLabel: String,
    onQualityClick: () -> Unit,
    overlayEnabled: Boolean,
    overlayAlpha: Float,
    onOverlayEnabledChange: (Boolean) -> Unit,
    onOverlayAlphaChange: (Float) -> Unit,
    prefixDisplay: String,
    onPrefixClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    SettingsCard {
        SettingsItem(
            label = stringResource(R.string.settings_item_image_quality),
            trailing = qualityLabel,
            onClick = onQualityClick,
        )
        SettingsDivider()
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(PairShotTouchTarget.large)
                    .padding(horizontal = PairShotCard.innerPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_item_overlay_opacity),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = overlayEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOverlayEnabledChange(it)
                    },
                    colors =
                    SwitchDefaults.colors(
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier =
                    Modifier
                        .wrapContentHeight(unbounded = true)
                        .scale(SWITCH_SCALE),
                )
            }
            AnimatedVisibility(
                visible = overlayEnabled,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                SettingsSliderItem(
                    label = "",
                    value = overlayAlpha,
                    valueRange = 0f..1.0f,
                    steps = 99,
                    valueLabel = { "${(it * 100).roundToInt()}%" },
                    onValueChange = onOverlayAlphaChange,
                    footer = {
                        AnimatedVisibility(
                            visible = overlayAlpha > OPACITY_WARNING_THRESHOLD,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Row(
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = PairShotSpacing.xs),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                WarningBadge(
                                    text = stringResource(R.string.settings_warning_opacity_high),
                                )
                            }
                        }
                    },
                )
            }
        }
        SettingsDivider()
        SettingsItem(
            label = stringResource(R.string.settings_item_file_name_prefix),
            trailing = prefixDisplay,
            onClick = onPrefixClick,
        )
    }
}
