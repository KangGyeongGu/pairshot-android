package com.pairshot.feature.settings.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.PairShotCard
import com.pairshot.core.designsystem.PairShotIconSize
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotTouchTarget
import com.pairshot.core.model.LogoPosition
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.ui.component.SettingsCard
import com.pairshot.core.ui.component.SettingsDivider
import com.pairshot.core.ui.component.SettingsItem
import com.pairshot.core.ui.component.SettingsSliderItem
import com.pairshot.feature.settings.R
import java.io.File
import kotlin.math.roundToInt

private val logoPositionOrder =
    listOf(
        LogoPosition.TOP_LEFT,
        LogoPosition.TOP_CENTER,
        LogoPosition.TOP_RIGHT,
        LogoPosition.CENTER_LEFT,
        LogoPosition.CENTER,
        LogoPosition.CENTER_RIGHT,
        LogoPosition.BOTTOM_LEFT,
        LogoPosition.BOTTOM_CENTER,
        LogoPosition.BOTTOM_RIGHT,
    )

@Composable
internal fun WatermarkLogoSection(
    watermarkConfig: WatermarkConfig,
    onWatermarkConfigChange: (WatermarkConfig) -> Unit,
    onSelectLogo: () -> Unit,
    onRemoveLogo: () -> Unit,
) {
    val hasLogo = watermarkConfig.logoPath.isNotEmpty()
    SettingsCard {
        if (hasLogo) {
            LogoRegisteredRow(
                fileName = File(watermarkConfig.logoPath).name,
                onChange = onSelectLogo,
                onRemove = onRemoveLogo,
            )
        } else {
            SettingsItem(
                label = stringResource(R.string.watermark_logo_label),
                trailing = stringResource(R.string.watermark_logo_choose),
                onClick = onSelectLogo,
            )
        }
        SettingsDivider()
        PositionPickerGridRow(
            label = stringResource(R.string.watermark_field_position),
            positions = logoPositionOrder,
            selectedPosition = watermarkConfig.logoPosition,
            onPositionChange = { position ->
                onWatermarkConfigChange(watermarkConfig.copy(logoPosition = position))
            },
        )
        SettingsDivider()
        SettingsSliderItem(
            label = stringResource(R.string.watermark_field_size),
            value = watermarkConfig.logoSizeRatio,
            valueRange = 0f..1.0f,
            valueLabel = { "${(it * 100).roundToInt()}%" },
            onValueChange = { v -> onWatermarkConfigChange(watermarkConfig.copy(logoSizeRatio = v)) },
            onLiveUpdate = { v -> onWatermarkConfigChange(watermarkConfig.copy(logoSizeRatio = v)) },
        )
        SettingsDivider()
        SettingsSliderItem(
            label = stringResource(R.string.watermark_field_opacity),
            value = watermarkConfig.logoAlpha,
            valueRange = 0f..1f,
            valueLabel = { "${(it * 100).roundToInt()}%" },
            onValueChange = { v -> onWatermarkConfigChange(watermarkConfig.copy(logoAlpha = v)) },
            onLiveUpdate = { v -> onWatermarkConfigChange(watermarkConfig.copy(logoAlpha = v)) },
        )
    }
}

private val ActionIconButtonSize = 40.dp
private val ActionIconGap = 4.dp

@Composable
private fun LogoRegisteredRow(
    fileName: String,
    onChange: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(PairShotTouchTarget.large)
                .padding(horizontal = PairShotCard.innerPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.watermark_logo_label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(PairShotSpacing.sm))
        Text(
            text = fileName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onChange,
            modifier = Modifier.size(ActionIconButtonSize),
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.watermark_logo_change),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(PairShotIconSize.sm),
            )
        }
        Spacer(modifier = Modifier.width(ActionIconGap))
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(ActionIconButtonSize),
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.watermark_logo_remove),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(PairShotIconSize.sm),
            )
        }
    }
}
