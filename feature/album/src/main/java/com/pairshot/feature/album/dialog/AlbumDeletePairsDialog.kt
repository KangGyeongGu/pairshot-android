package com.pairshot.feature.album.dialog

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
import com.pairshot.core.ui.component.PairShotBottomSheet
import com.pairshot.feature.album.R
import com.pairshot.core.ui.R as CoreR

@Composable
fun AlbumDeletePairsDialog(
    pairCount: Int,
    combinedCount: Int,
    onRemoveFromAlbum: () -> Unit,
    onDeletePairs: () -> Unit,
    onDeleteCombinedOnly: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PairShotBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(CoreR.string.dialog_delete_pair_method_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = PairShotSpacing.sm),
        )
        Text(
            text =
            if (combinedCount > 0) {
                pluralStringResource(
                    CoreR.plurals.dialog_delete_pair_summary,
                    pairCount,
                    pairCount,
                    combinedCount,
                )
            } else {
                pluralStringResource(
                    CoreR.plurals.dialog_delete_pair_confirm,
                    pairCount,
                    pairCount,
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = PairShotSpacing.lg),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ActionRow(
            label = stringResource(R.string.album_button_remove_from_album),
            onClick = onRemoveFromAlbum,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ActionRow(
            label = stringResource(CoreR.string.dialog_delete_pair_button_all),
            onClick = onDeletePairs,
            color = MaterialTheme.colorScheme.error,
        )
        if (combinedCount > 0) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ActionRow(
                label = stringResource(CoreR.string.dialog_delete_pair_button_combined_only),
                onClick = onDeleteCombinedOnly,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ActionRow(
            label = stringResource(CoreR.string.common_button_cancel),
            onClick = onDismiss,
        )
        Spacer(modifier = Modifier.height(PairShotSpacing.sm))
    }
}

@Composable
private fun ActionRow(
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
