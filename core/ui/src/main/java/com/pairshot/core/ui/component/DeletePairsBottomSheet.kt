package com.pairshot.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.ui.R

@Composable
fun DeletePairsBottomSheet(
    pairCount: Int,
    combinedCount: Int,
    onDeletePairs: () -> Unit,
    onDeleteCombinedOnly: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    removeFromAlbumLabel: String? = null,
    onRemoveFromAlbum: (() -> Unit)? = null,
) {
    val showCombinedOnly = combinedCount > 0
    val showRemoveFromAlbum = removeFromAlbumLabel != null && onRemoveFromAlbum != null

    PairShotBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Text(
            text =
            stringResource(
                if (showCombinedOnly || showRemoveFromAlbum) {
                    R.string.dialog_delete_pair_method_title
                } else {
                    R.string.dialog_delete_pair_title
                },
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = PairShotSpacing.sm),
        )
        Text(
            text =
            if (showCombinedOnly) {
                pluralStringResource(
                    R.plurals.dialog_delete_pair_summary,
                    pairCount,
                    pairCount,
                    combinedCount,
                )
            } else {
                pluralStringResource(
                    R.plurals.dialog_delete_pair_confirm,
                    pairCount,
                    pairCount,
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = PairShotSpacing.lg),
        )
        DeleteActionDivider()
        if (showRemoveFromAlbum) {
            DeleteActionRow(
                label = removeFromAlbumLabel,
                onClick = onRemoveFromAlbum,
            )
            DeleteActionDivider()
        }
        DeleteActionRow(
            label = stringResource(R.string.dialog_delete_pair_button_all),
            onClick = onDeletePairs,
            color = MaterialTheme.colorScheme.error,
        )
        if (showCombinedOnly) {
            DeleteActionDivider()
            DeleteActionRow(
                label = stringResource(R.string.dialog_delete_pair_button_combined_only),
                onClick = onDeleteCombinedOnly,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        DeleteActionDivider()
        DeleteActionRow(
            label = stringResource(R.string.common_button_cancel),
            onClick = onDismiss,
        )
        Spacer(modifier = Modifier.height(PairShotSpacing.sm))
    }
}

@Composable
private fun DeleteActionRow(
    label: String,
    onClick: () -> Unit,
    color: Color = Color.Unspecified,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color,
        textAlign = TextAlign.Center,
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = PairShotSpacing.lg),
    )
}

@Composable
private fun DeleteActionDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}
