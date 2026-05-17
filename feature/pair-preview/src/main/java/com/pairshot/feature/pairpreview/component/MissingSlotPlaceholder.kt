package com.pairshot.feature.pairpreview.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pairshot.core.designsystem.PairShotCard
import com.pairshot.core.designsystem.PairShotScreen
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.feature.pairpreview.R

@Composable
fun MissingSlotPlaceholder(
    side: MissingSlotSide,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val messageRes =
        when (side) {
            MissingSlotSide.BEFORE -> R.string.pair_preview_slot_missing_before
            MissingSlotSide.AFTER -> R.string.pair_preview_slot_missing_after
        }
    val buttonRes =
        when (side) {
            MissingSlotSide.BEFORE -> R.string.pair_preview_slot_capture_before
            MissingSlotSide.AFTER -> R.string.pair_preview_slot_capture_after
        }
    Box(
        modifier = modifier.fillMaxSize().padding(PairShotScreen.horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PairShotCard.innerPadding),
        ) {
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onCapture) {
                Text(text = stringResource(buttonRes))
            }
        }
    }
}

enum class MissingSlotSide { BEFORE, AFTER }
