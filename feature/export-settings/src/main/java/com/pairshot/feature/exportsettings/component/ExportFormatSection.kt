package com.pairshot.feature.exportsettings.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pairshot.core.designsystem.PairShotCard
import com.pairshot.core.designsystem.PairShotProBadge
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotTouchTarget
import com.pairshot.core.model.ExportFormat
import com.pairshot.core.ui.component.SettingsCard
import com.pairshot.core.ui.component.SettingsDivider
import com.pairshot.feature.exportsettings.R

@Composable
fun ExportFormatSection(
    format: ExportFormat,
    isProSubscriber: Boolean,
    onFormatChange: (ExportFormat) -> Unit,
    onProLock: () -> Unit,
) {
    SettingsCard {
        ExportFormatRadioItem(
            label = stringResource(R.string.export_format_image),
            selected = format == ExportFormat.INDIVIDUAL,
            locked = false,
            onClick = { onFormatChange(ExportFormat.INDIVIDUAL) },
        )
        SettingsDivider()
        ExportFormatRadioItem(
            label = stringResource(R.string.export_format_zip),
            selected = format == ExportFormat.ZIP,
            locked = !isProSubscriber,
            onClick = {
                if (isProSubscriber) onFormatChange(ExportFormat.ZIP) else onProLock()
            },
        )
    }
}

@Composable
private fun ExportFormatRadioItem(
    label: String,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
) {
    val labelColor =
        if (locked) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_LABEL_ALPHA)
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = PairShotTouchTarget.large)
            .padding(horizontal = PairShotCard.innerPadding, vertical = PairShotSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor,
            modifier = Modifier.weight(1f),
        )
        if (locked) {
            PairShotProBadge()
            Spacer(modifier = Modifier.width(PairShotSpacing.sm))
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = !locked,
        )
    }
}

private const val DISABLED_LABEL_ALPHA = 0.38f
