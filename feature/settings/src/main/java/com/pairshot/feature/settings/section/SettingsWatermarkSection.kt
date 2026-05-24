package com.pairshot.feature.settings.section

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.pairshot.core.designsystem.PairShotCard
import com.pairshot.core.designsystem.PairShotTouchTarget
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.model.isContentMissing
import com.pairshot.core.ui.component.SettingsDivider
import com.pairshot.core.ui.component.SettingsItem
import com.pairshot.feature.settings.R
import com.pairshot.feature.settings.component.HighlightableSettingsCard

private const val SWITCH_SCALE = 0.67f

@Composable
internal fun SettingsWatermarkSection(
    watermarkConfig: WatermarkConfig,
    pulse: Boolean,
    onWatermarkConfigChange: (WatermarkConfig) -> Unit,
    onWatermarkSettingsClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    HighlightableSettingsCard(pulse = pulse) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(PairShotTouchTarget.large)
                .padding(horizontal = PairShotCard.innerPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_item_watermark_use),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = watermarkConfig.enabled,
                onCheckedChange = { checked ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onWatermarkConfigChange(watermarkConfig.copy(enabled = checked))
                },
                modifier =
                Modifier
                    .wrapContentHeight(unbounded = true)
                    .scale(SWITCH_SCALE),
            )
        }
        AnimatedVisibility(
            visible = watermarkConfig.enabled,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column {
                SettingsDivider()
                SettingsItem(
                    label = stringResource(R.string.settings_item_user_settings),
                    trailing =
                    if (watermarkConfig.isContentMissing()) {
                        stringResource(R.string.settings_warning_required)
                    } else {
                        null
                    },
                    trailingIsError = watermarkConfig.isContentMissing(),
                    onClick = onWatermarkSettingsClick,
                )
            }
        }
    }
}
