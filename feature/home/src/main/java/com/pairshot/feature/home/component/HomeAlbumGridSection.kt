package com.pairshot.feature.home.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotCard
import com.pairshot.core.model.Album

@Composable
fun HomeAlbumGridSection(
    albums: List<Album>,
    isSelectionMode: Boolean,
    selectedAlbumIds: Set<Long>,
    onAlbumClick: (Long) -> Unit,
    onAlbumLongPress: (Long) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        itemsIndexed(
            items = albums,
            key = { _, album -> album.id },
        ) { index, album ->
            val isFirst = index == 0
            val isLast = index == albums.lastIndex
            val baseShape = MaterialTheme.shapes.medium
            val itemShape =
                when {
                    isFirst && isLast -> {
                        baseShape
                    }

                    isFirst -> {
                        baseShape.copy(
                            bottomStart = CornerSize(0.dp),
                            bottomEnd = CornerSize(0.dp),
                        )
                    }

                    isLast -> {
                        baseShape.copy(
                            topStart = CornerSize(0.dp),
                            topEnd = CornerSize(0.dp),
                        )
                    }

                    else -> {
                        RectangleShape
                    }
                }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = itemShape,
                color = MaterialTheme.colorScheme.surface,
            ) {
                AlbumCard(
                    album = album,
                    isSelectionMode = isSelectionMode,
                    isSelected = album.id in selectedAlbumIds,
                    isFirst = isFirst,
                    isLast = isLast,
                    onClick = { onAlbumClick(album.id) },
                    onLongPress = { onAlbumLongPress(album.id) },
                )
            }
            if (!isLast) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = PairShotCard.innerPadding),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}
