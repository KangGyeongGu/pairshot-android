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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.ui.R

@Composable
fun ConfirmActionBottomSheet(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmIsDestructive: Boolean = false,
) {
    PairShotBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = PairShotSpacing.sm),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = PairShotSpacing.lg),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = DIVIDER_ALPHA))
        Text(
            text = confirmLabel,
            style = MaterialTheme.typography.bodyLarge,
            color =
            if (confirmIsDestructive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onConfirm)
                .padding(vertical = PairShotSpacing.lg),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = DIVIDER_ALPHA))
        Text(
            text = stringResource(R.string.common_button_cancel),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onDismiss)
                .padding(vertical = PairShotSpacing.lg),
        )
        Spacer(modifier = Modifier.height(PairShotSpacing.sm))
    }
}

private const val DIVIDER_ALPHA = 0.2f
