package com.pairshot.feature.album.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pairshot.core.ui.component.PairShotActionBar
import com.pairshot.core.ui.component.PairShotActionBarItem
import com.pairshot.core.ui.R as CoreR

@Composable
fun AlbumSelectionBottomBar(
    selectedCount: Int,
    onShareClick: () -> Unit,
    onSaveToDeviceClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSelection = selectedCount > 0

    PairShotActionBar(modifier = modifier) {
        PairShotActionBarItem(
            label = stringResource(CoreR.string.common_button_share),
            onClick = onShareClick,
            enabled = hasSelection,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(CoreR.string.common_button_share),
                    tint =
                    if (hasSelection) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ICON_ALPHA)
                    },
                )
            },
        )
        PairShotActionBarItem(
            label = stringResource(CoreR.string.common_button_save_to_device),
            onClick = onSaveToDeviceClick,
            enabled = hasSelection,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = stringResource(CoreR.string.common_button_save_to_device),
                    tint =
                    if (hasSelection) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ICON_ALPHA)
                    },
                )
            },
        )
        PairShotActionBarItem(
            label = stringResource(CoreR.string.common_button_delete),
            onClick = onDeleteClick,
            enabled = hasSelection,
            labelColor = MaterialTheme.colorScheme.error,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(CoreR.string.common_button_delete),
                    tint =
                    if (hasSelection) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.error.copy(alpha = DISABLED_ICON_ALPHA)
                    },
                )
            },
        )
        PairShotActionBarItem(
            label = stringResource(CoreR.string.common_button_export_settings),
            onClick = onExportSettingsClick,
            enabled = hasSelection,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = stringResource(CoreR.string.common_button_export_settings),
                    tint =
                    if (hasSelection) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ICON_ALPHA)
                    },
                )
            },
        )
    }
}

private const val DISABLED_ICON_ALPHA = 0.38f
