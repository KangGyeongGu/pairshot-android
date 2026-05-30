package com.pairshot.feature.album.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.pairshot.core.adsui.component.PairCardGridSection
import com.pairshot.core.designsystem.PairShotScreen
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.model.SortOrder
import com.pairshot.feature.album.R
import com.pairshot.feature.album.viewmodel.PairPickerUiState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairPickerScreen(
    state: PairPickerUiState.Ready,
    onToggle: (Long) -> Unit,
    onConfirm: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val countLabel =
                        if (state.selection.hasSelection) {
                            pluralStringResource(
                                R.plurals.pair_picker_title_selected,
                                state.selection.selectedCount,
                                state.selection.selectedCount,
                            )
                        } else {
                            stringResource(R.string.pair_picker_title)
                        }
                    Text(
                        text = countLabel,
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.pair_picker_desc_close),
                        )
                    }
                },
                colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
            ) {
                Button(
                    onClick = onConfirm,
                    enabled = state.selection.hasSelection && !state.isConfirming,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = PairShotScreen.horizontalPadding, vertical = PairShotSpacing.md),
                ) {
                    Text(
                        text =
                        stringResource(
                            if (state.isConfirming) R.string.pair_picker_button_adding else R.string.pair_picker_button_add,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        if (state.pairs.isEmpty()) {
            Box(
                modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.pair_picker_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }
        PairCardGridSection(
            pairs = state.pairs.toImmutableList(),
            selectedIds = state.selection.selectedIds,
            isSelectionMode = true,
            sortOrder = SortOrder.DESC,
            onPairClick = onToggle,
            showAds = false,
            disabledIds = state.alreadyInAlbumIds.toImmutableSet(),
            disabledLabel = stringResource(R.string.pair_picker_already_added),
            contentPadding =
            PaddingValues(
                top = innerPadding.calculateTopPadding() + PairShotSpacing.md,
                bottom = innerPadding.calculateBottomPadding() + PairShotSpacing.md,
                start = PairShotSpacing.md,
                end = PairShotSpacing.md,
            ),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
