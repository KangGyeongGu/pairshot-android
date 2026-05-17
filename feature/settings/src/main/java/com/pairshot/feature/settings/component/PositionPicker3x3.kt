package com.pairshot.feature.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.PairShotCard
import com.pairshot.core.designsystem.PairShotIconSize
import com.pairshot.core.designsystem.PairShotSpacing

private const val GRID_CELL_COUNT = 9
private const val GRID_ROW_COUNT = 3
private val CheckIconSize = PairShotSpacing.lg

@Composable
internal fun <T> PositionPicker3x3Row(
    label: String,
    positions: List<T>,
    selectedPosition: T,
    onPositionChange: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(positions.size == GRID_CELL_COUNT) { "positions must contain exactly 9 entries" }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PairShotCard.innerPadding,
                    vertical = PairShotCard.innerPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Column(verticalArrangement = Arrangement.spacedBy(PairShotSpacing.sm)) {
            positions.chunked(GRID_ROW_COUNT).forEach { rowPositions ->
                Row(horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.sm)) {
                    rowPositions.forEach { position ->
                        val isSelected = position == selectedPosition
                        Box(
                            modifier =
                                Modifier
                                    .size(PairShotIconSize.md)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                    ).semantics { selected = isSelected }
                                    .clickable { onPositionChange(position) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(CheckIconSize),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
