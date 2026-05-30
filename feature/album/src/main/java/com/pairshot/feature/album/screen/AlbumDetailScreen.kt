package com.pairshot.feature.album.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pairshot.core.adsui.component.PairCardGridSection
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.ui.component.ConfirmActionBottomSheet
import com.pairshot.core.ui.component.DeletePairsBottomSheet
import com.pairshot.feature.album.R
import com.pairshot.feature.album.component.AlbumDetailTopBar
import com.pairshot.feature.album.component.AlbumEmptyActions
import com.pairshot.feature.album.component.AlbumFilterRow
import com.pairshot.feature.album.component.AlbumPrimaryActionBar
import com.pairshot.feature.album.component.AlbumSelectionBottomBar
import com.pairshot.feature.album.dialog.RenameAlbumDialog
import com.pairshot.feature.album.viewmodel.AlbumDetailUiState
import kotlinx.collections.immutable.toImmutableList
import com.pairshot.core.ui.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    uiState: AlbumDetailUiState.Success,
    onNavigateBack: () -> Unit,
    onPairClick: (Long) -> Unit,
    onPairLongPress: (Long) -> Unit,
    onExitSelectionMode: () -> Unit,
    onCaptureBeforeClick: () -> Unit,
    onAddPairsClick: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onShareClick: () -> Unit,
    onSaveToDeviceClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportSettingsClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteAlbumClick: () -> Unit,
    onRenameConfirm: (String) -> Unit,
    onRenameDismiss: () -> Unit,
    onDeleteAlbumConfirm: () -> Unit,
    onDeleteAlbumDismiss: () -> Unit,
    onRemoveFromAlbum: () -> Unit,
    onDeletePairs: () -> Unit,
    onDeleteCombinedOnly: () -> Unit,
    onDeletePairsDismiss: () -> Unit,
    onToggleSortOrder: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pairsEmpty = uiState.pairs.isEmpty()

    Scaffold(
        modifier = modifier,
        topBar = {
            AlbumDetailTopBar(
                title = uiState.album.name,
                isSelectionMode = uiState.selection.isSelectionMode,
                selectedCount = uiState.selection.selectedCount,
                onNavigateBack = onNavigateBack,
                onExitSelection = onExitSelectionMode,
                onAddPairsClick = onAddPairsClick,
                onRenameClick = onRenameClick,
                onDeleteAlbumClick = onDeleteAlbumClick,
            )
        },
        bottomBar = {
            when {
                uiState.selection.isSelectionMode -> {
                    AlbumSelectionBottomBar(
                        selectedCount = uiState.selection.selectedCount,
                        onShareClick = onShareClick,
                        onSaveToDeviceClick = onSaveToDeviceClick,
                        onDeleteClick = onDeleteClick,
                        onExportSettingsClick = onExportSettingsClick,
                    )
                }

                !pairsEmpty -> {
                    AlbumPrimaryActionBar(
                        label = stringResource(CoreR.string.common_button_start_capture),
                        onClick = onCaptureBeforeClick,
                    )
                }
            }
        },
    ) { innerPadding ->
        if (pairsEmpty) {
            AlbumEmptyActions(
                onAddPairsClick = onAddPairsClick,
                onCaptureBeforeClick = onCaptureBeforeClick,
                modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            Column(modifier = Modifier.padding(innerPadding)) {
                if (!uiState.selection.isSelectionMode) {
                    AlbumFilterRow(
                        sortOrder = uiState.sortOrder,
                        onToggleSortOrder = onToggleSortOrder,
                        onEnterSelectionMode = onEnterSelectionMode,
                    )
                }
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    PairCardGridSection(
                        pairs = uiState.pairs.toImmutableList(),
                        selectedIds = uiState.selection.selectedIds,
                        isSelectionMode = uiState.selection.isSelectionMode,
                        sortOrder = uiState.sortOrder,
                        onPairClick = onPairClick,
                        onPairLongPress = onPairLongPress,
                        contentPadding =
                        PaddingValues(
                            bottom = PairShotSpacing.md,
                            start = PairShotSpacing.md,
                            end = PairShotSpacing.md,
                        ),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    if (uiState.showRenameDialog) {
        RenameAlbumDialog(
            currentName = uiState.album.name,
            onConfirm = onRenameConfirm,
            onDismiss = onRenameDismiss,
        )
    }

    if (uiState.showDeleteAlbumDialog) {
        ConfirmActionBottomSheet(
            title = stringResource(R.string.album_dialog_delete_title),
            message = stringResource(R.string.album_dialog_delete_message),
            confirmLabel = stringResource(CoreR.string.common_button_delete),
            onConfirm = onDeleteAlbumConfirm,
            onDismiss = onDeleteAlbumDismiss,
            confirmIsDestructive = true,
        )
    }

    if (uiState.showDeletePairsDialog) {
        val combinedInSelection =
            uiState.pairs.count { it.id in uiState.selection.selectedIds && it.hasCombined }
        DeletePairsBottomSheet(
            pairCount = uiState.selection.selectedCount,
            combinedCount = combinedInSelection,
            onDeletePairs = onDeletePairs,
            onDeleteCombinedOnly = onDeleteCombinedOnly,
            onDismiss = onDeletePairsDismiss,
            removeFromAlbumLabel = stringResource(R.string.album_button_remove_from_album),
            onRemoveFromAlbum = onRemoveFromAlbum,
        )
    }
}
