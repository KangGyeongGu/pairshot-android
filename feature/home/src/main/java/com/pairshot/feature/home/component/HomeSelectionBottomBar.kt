package com.pairshot.feature.home.component

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
import com.pairshot.core.domain.tutorial.AnchorKey
import com.pairshot.core.ui.R
import com.pairshot.core.ui.component.PairShotActionBar
import com.pairshot.core.ui.component.PairShotActionBarItem
import com.pairshot.feature.tutorial.ui.modifier.tutorialAnchor

@Composable
fun HomeSelectionBottomBar(
    selectedCount: Int,
    onShare: () -> Unit,
    onSaveToDevice: () -> Unit,
    onDelete: () -> Unit,
    onExportSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSelection = selectedCount > 0

    PairShotActionBar {
        PairShotActionBarItem(
            label = stringResource(R.string.common_button_share),
            onClick = onShare,
            enabled = hasSelection,
            modifier = Modifier.tutorialAnchor(AnchorKey.HOME_SELECTION_BAR_SHARE),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.common_button_share),
                    tint =
                        if (hasSelection) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                )
            },
        )
        PairShotActionBarItem(
            label = stringResource(R.string.common_button_save_to_device),
            onClick = onSaveToDevice,
            enabled = hasSelection,
            modifier = Modifier.tutorialAnchor(AnchorKey.HOME_SELECTION_BAR_SAVE),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = stringResource(R.string.common_button_save_to_device),
                    tint =
                        if (hasSelection) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                )
            },
        )
        PairShotActionBarItem(
            label = stringResource(R.string.common_button_delete),
            onClick = onDelete,
            enabled = hasSelection,
            modifier = Modifier.tutorialAnchor(AnchorKey.HOME_SELECTION_BAR_DELETE),
            labelColor = MaterialTheme.colorScheme.error,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.common_button_delete),
                    tint =
                        if (hasSelection) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
                        },
                )
            },
        )
        PairShotActionBarItem(
            label = stringResource(R.string.common_button_export_settings),
            onClick = onExportSettings,
            enabled = hasSelection,
            modifier = Modifier.tutorialAnchor(AnchorKey.HOME_SELECTION_BAR_EXPORT_SETTINGS),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = stringResource(R.string.common_button_export_settings),
                    tint =
                        if (hasSelection) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                )
            },
        )
    }
}
