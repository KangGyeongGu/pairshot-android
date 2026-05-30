package com.pairshot.feature.pairpreview.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pairshot.core.ui.component.PairShotDialog
import com.pairshot.feature.pairpreview.R
import com.pairshot.core.ui.R as CoreR

@Composable
fun DeleteAfterConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PairShotDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.pair_preview_delete_after_title),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.pair_preview_delete_after_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Row {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = stringResource(CoreR.string.common_button_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(CoreR.string.common_button_cancel))
                }
            }
        },
    )
}
