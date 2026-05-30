package com.pairshot.feature.pairpreview.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pairshot.core.ui.component.PairShotActionBar
import com.pairshot.core.ui.component.PairShotActionBarItem
import com.pairshot.feature.pairpreview.R
import com.pairshot.core.ui.R as CoreR

@Composable
fun PairPreviewBottomBar(
    hasAfter: Boolean,
    onShareClick: () -> Unit,
    onSaveToDeviceClick: () -> Unit,
    onDeleteAfterClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PairShotActionBar(modifier = modifier) {
        PairShotActionBarItem(
            label = stringResource(CoreR.string.common_button_share),
            onClick = onShareClick,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(CoreR.string.common_button_share),
                )
            },
        )
        PairShotActionBarItem(
            label = stringResource(CoreR.string.common_button_save_to_device),
            onClick = onSaveToDeviceClick,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = stringResource(CoreR.string.common_button_save_to_device),
                )
            },
        )
        PairShotActionBarItem(
            label = stringResource(R.string.pair_preview_action_delete_after),
            onClick = onDeleteAfterClick,
            enabled = hasAfter,
            labelColor = MaterialTheme.colorScheme.error,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.HideImage,
                    contentDescription = stringResource(R.string.pair_preview_action_delete_after),
                    tint = MaterialTheme.colorScheme.error,
                )
            },
        )
        PairShotActionBarItem(
            label = stringResource(CoreR.string.common_button_delete),
            onClick = onDeleteClick,
            labelColor = MaterialTheme.colorScheme.error,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(CoreR.string.common_button_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            },
        )
    }
}
