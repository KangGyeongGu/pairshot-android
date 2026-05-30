package com.pairshot.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.pairshot.core.designsystem.PairShotRadius
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.model.SortOrder
import com.pairshot.core.ui.R

@Composable
fun SortOrderDualLabel(
    sortOrder: SortOrder,
    onSelect: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.xs),
    ) {
        SortOrderLabel(
            label = stringResource(R.string.common_label_sort_descending),
            selected = sortOrder == SortOrder.DESC,
            onClick = { onSelect(SortOrder.DESC) },
        )
        Text(
            text = "|",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        SortOrderLabel(
            label = stringResource(R.string.common_label_sort_ascending),
            selected = sortOrder == SortOrder.ASC,
            onClick = { onSelect(SortOrder.ASC) },
        )
    }
}

@Composable
private fun SortOrderLabel(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color =
        if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier =
        Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = PairShotSpacing.xs, vertical = PairShotRadius.sm),
    )
}
