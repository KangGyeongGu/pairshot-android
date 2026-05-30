package com.pairshot.feature.album.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pairshot.core.designsystem.PairShotScreen
import com.pairshot.core.model.SortOrder
import com.pairshot.core.ui.component.SortOrderDualLabel

@Composable
fun AlbumFilterRow(
    sortOrder: SortOrder,
    onToggleSortOrder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(horizontal = PairShotScreen.horizontalPadding),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SortOrderDualLabel(
            sortOrder = sortOrder,
            onSelect = { next -> if (next != sortOrder) onToggleSortOrder() },
        )
    }
}
