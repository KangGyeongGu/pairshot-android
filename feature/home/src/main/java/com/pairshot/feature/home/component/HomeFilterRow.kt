package com.pairshot.feature.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pairshot.core.designsystem.PairShotScreen
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.model.SortOrder
import com.pairshot.core.ui.component.SortOrderDualLabel
import com.pairshot.feature.home.R
import com.pairshot.feature.home.viewmodel.HomeMode

@Composable
fun HomeFilterRow(
    selectedMode: HomeMode,
    inSelectionMode: Boolean,
    sortOrder: SortOrder,
    onModeChange: (HomeMode) -> Unit,
    onToggleSortOrder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(horizontal = PairShotScreen.horizontalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeMode.entries.forEach { mode ->
                val label =
                    when (mode) {
                        HomeMode.PAIRS -> stringResource(R.string.home_filter_all)
                        HomeMode.ALBUMS -> stringResource(R.string.home_filter_album)
                    }
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onModeChange(mode) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border =
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedMode == mode,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }

        if (selectedMode == HomeMode.PAIRS && !inSelectionMode) {
            SortOrderDualLabel(
                sortOrder = sortOrder,
                onSelect = { next -> if (next != sortOrder) onToggleSortOrder() },
            )
        }
    }
}
