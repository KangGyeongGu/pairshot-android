package com.pairshot.feature.exportsettings.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.pairshot.feature.exportsettings.R
import com.pairshot.core.ui.R as CoreR

private const val SWITCH_SCALE = 0.67f

@Composable
fun ExportWatermarkSection(
    applyWatermark: Boolean,
    onApplyWatermarkChange: (Boolean) -> Unit,
    onNavigateToWatermarkSettings: () -> Unit,
) {
    SettingsCard {
        ExportSwitchWithGearItem(
            label = stringResource(R.string.export_watermark_apply),
            checked = applyWatermark,
            onCheckedChange = onApplyWatermarkChange,
            onGearClick = onNavigateToWatermarkSettings,
        )
    }
}

@Composable
internal fun ExportSwitchWithGearItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onGearClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(PairShotTouchTarget.large)
                .padding(start = PairShotCard.innerPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(it)
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
        IconButton(onClick = onGearClick) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(CoreR.string.common_desc_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
