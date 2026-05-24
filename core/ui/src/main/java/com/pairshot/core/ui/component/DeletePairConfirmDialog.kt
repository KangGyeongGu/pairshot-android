package com.pairshot.core.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.pairshot.core.ui.R

@Composable
fun DeletePairConfirmDialog(
    pairCount: Int,
    combinedCount: Int,
    onDeleteAll: () -> Unit,
    onDeleteCombinedOnly: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (combinedCount > 0) {
        PairShotDialog(
            onDismissRequest = onDismiss,
            modifier = modifier,
            title = {
                Text(
                    text = stringResource(R.string.dialog_delete_pair_method_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    text =
                    pluralStringResource(
                        R.plurals.dialog_delete_pair_summary,
                        pairCount,
                        pairCount,
                        combinedCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = onDeleteAll) {
                        Text(
                            text = stringResource(R.string.dialog_delete_pair_button_all),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    TextButton(onClick = onDeleteCombinedOnly) {
                        Text(
                            text = stringResource(R.string.dialog_delete_pair_button_combined_only),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.common_button_cancel))
                    }
                }
            },
        )
    } else {
        PairShotDialog(
            onDismissRequest = onDismiss,
            modifier = modifier,
            title = {
                Text(
                    text = stringResource(R.string.dialog_delete_pair_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    text =
                    pluralStringResource(
                        R.plurals.dialog_delete_pair_confirm,
                        pairCount,
                        pairCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = onDeleteAll) {
                        Text(
                            text = stringResource(R.string.common_button_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.common_button_cancel))
                    }
                }
            },
        )
    }
}
