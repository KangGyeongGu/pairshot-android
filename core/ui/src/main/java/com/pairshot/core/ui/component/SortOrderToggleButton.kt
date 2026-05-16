package com.pairshot.core.ui.component

import com.pairshot.core.designsystem.PairShotRadius
import com.pairshot.core.designsystem.PairShotSpacing
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pairshot.core.model.SortOrder
import com.pairshot.core.ui.R

@Composable
fun SortOrderToggleButton(
    sortOrder: SortOrder,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val label =
        when (sortOrder) {
            SortOrder.DESC -> stringResource(R.string.common_label_sort_descending)
            SortOrder.ASC -> stringResource(R.string.common_label_sort_ascending)
        }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier =
            modifier
                .clickable(onClick = onToggle)
                .padding(horizontal = PairShotSpacing.md, vertical = PairShotRadius.sm),
    )
}
