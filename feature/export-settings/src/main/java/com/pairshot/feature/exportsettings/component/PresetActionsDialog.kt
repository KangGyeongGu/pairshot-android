package com.pairshot.feature.exportsettings.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotTouchTarget
import com.pairshot.core.model.ExportPresetSlot
import com.pairshot.core.ui.component.PairShotBottomSheet
import com.pairshot.feature.exportsettings.R

@Composable
fun PresetActionsDialog(
    slot: ExportPresetSlot,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDefault = slot.id == ExportPresetSlot.DEFAULT_ID
    PairShotBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        SheetHeader(name = slot.name)
        HorizontalDivider(
            modifier = Modifier.padding(vertical = PairShotSpacing.sm),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        PresetActionRow(
            icon = Icons.Outlined.DriveFileRenameOutline,
            label = stringResource(R.string.export_preset_action_rename),
            onClick = onRename,
        )
        if (!isDefault) {
            PresetActionRow(
                icon = Icons.Outlined.DeleteOutline,
                label = stringResource(R.string.export_preset_action_delete),
                tint = MaterialTheme.colorScheme.error,
                onClick = onDelete,
            )
        }
        Spacer(modifier = Modifier.size(PairShotSpacing.sm))
    }
}

@Composable
private fun SheetHeader(name: String) {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(vertical = PairShotSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PresetActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = PairShotTouchTarget.large)
            .clickable(onClick = onClick)
            .padding(horizontal = PairShotSpacing.sm, vertical = PairShotSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(ROW_ICON_SIZE),
        )
        Spacer(modifier = Modifier.width(PairShotSpacing.md))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
        )
    }
}

private val ROW_ICON_SIZE = 22.dp
