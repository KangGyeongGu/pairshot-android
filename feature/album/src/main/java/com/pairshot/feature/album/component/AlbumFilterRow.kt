package com.pairshot.feature.album.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pairshot.core.designsystem.PairShotScreen
import com.pairshot.core.model.SortOrder
import com.pairshot.core.ui.component.SortOrderDualLabel
import com.pairshot.feature.album.R

@Composable
fun AlbumFilterRow(
    sortOrder: SortOrder,
    onToggleSortOrder: () -> Unit,
    onEnterSelectionMode: () -> Unit,
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
        IconButton(onClick = onEnterSelectionMode) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = stringResource(R.string.album_desc_enter_selection),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        SortOrderDualLabel(
            sortOrder = sortOrder,
            onSelect = { next -> if (next != sortOrder) onToggleSortOrder() },
        )
    }
}
