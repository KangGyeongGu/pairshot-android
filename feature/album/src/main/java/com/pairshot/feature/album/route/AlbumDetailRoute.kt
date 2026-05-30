package com.pairshot.feature.album.route

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.core.navigation.PaywallTrigger
import com.pairshot.feature.album.screen.AlbumDetailScreen
import com.pairshot.feature.album.viewmodel.AlbumDetailEvent
import com.pairshot.feature.album.viewmodel.AlbumDetailUiState
import com.pairshot.feature.album.viewmodel.AlbumDetailViewModel
import kotlinx.coroutines.launch

@Composable
fun AlbumDetailRoute(
    onNavigateBack: () -> Unit,
    onNavigateToPairPreview: (pairId: Long) -> Unit,
    onNavigateToAfterCamera: (pairId: Long, albumId: Long) -> Unit,
    onNavigateToBeforeRetake: (pairId: Long) -> Unit,
    onNavigateToCamera: (albumId: Long) -> Unit,
    onNavigateToPairPicker: (albumId: Long) -> Unit,
    onNavigateToPaywall: (PaywallTrigger) -> Unit,
    onNavigateToExportSettings: (pairIds: Set<Long>) -> Unit,
    onShareSelection: (pairIds: Set<Long>) -> Unit,
    onSaveSelectionToDevice: (pairIds: Set<Long>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    val currentOnNavigateToPairPreview by rememberUpdatedState(onNavigateToPairPreview)
    val currentOnNavigateToAfterCamera by rememberUpdatedState(onNavigateToAfterCamera)
    val currentOnNavigateToBeforeRetake by rememberUpdatedState(onNavigateToBeforeRetake)
    val currentOnNavigateToCamera by rememberUpdatedState(onNavigateToCamera)
    val currentOnNavigateToPairPicker by rememberUpdatedState(onNavigateToPairPicker)
    val currentOnNavigateToPaywall by rememberUpdatedState(onNavigateToPaywall)

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is AlbumDetailEvent.NavigateBack -> currentOnNavigateBack()
                is AlbumDetailEvent.NavigateToPairPreview -> currentOnNavigateToPairPreview(event.pairId)
                is AlbumDetailEvent.NavigateToAfterCamera ->
                    currentOnNavigateToAfterCamera(event.pairId, event.albumId)
                is AlbumDetailEvent.NavigateToBeforeRetake -> currentOnNavigateToBeforeRetake(event.pairId)
                is AlbumDetailEvent.NavigateToCamera -> currentOnNavigateToCamera(event.albumId)
                is AlbumDetailEvent.NavigateToPairPicker -> currentOnNavigateToPairPicker(event.albumId)
            }
        }
    }

    when (val state = uiState) {
        AlbumDetailUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        is AlbumDetailUiState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = state.message.asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        is AlbumDetailUiState.Success -> {
            AlbumDetailScreen(
                uiState = state,
                onNavigateBack = onNavigateBack,
                onPairClick = viewModel::onPairClick,
                onPairLongPress = viewModel::onPairLongPress,
                onExitSelectionMode = viewModel::exitSelectionMode,
                onCaptureBeforeClick = {
                    scope.launch {
                        if (viewModel.isCameraEntryAllowed()) {
                            viewModel.onFabClick()
                        } else {
                            currentOnNavigateToPaywall(PaywallTrigger.DAILY_LIMIT)
                        }
                    }
                },
                onAddPairsClick = viewModel::onAddPairsClick,
                onShareClick = { onShareSelection(state.selectedIds) },
                onSaveToDeviceClick = { onSaveSelectionToDevice(state.selectedIds) },
                onDeleteClick = viewModel::showDeletePairsDialog,
                onExportSettingsClick = { onNavigateToExportSettings(state.selectedIds) },
                onRenameClick = viewModel::showRenameDialog,
                onDeleteAlbumClick = viewModel::showDeleteAlbumDialog,
                onRenameConfirm = viewModel::confirmRenameAlbum,
                onRenameDismiss = viewModel::dismissRenameDialog,
                onDeleteAlbumConfirm = viewModel::confirmDeleteAlbum,
                onDeleteAlbumDismiss = viewModel::dismissDeleteAlbumDialog,
                onRemoveFromAlbum = viewModel::removeSelectedFromAlbum,
                onDeletePairs = viewModel::deleteSelectedPairs,
                onDeleteCombinedOnly = viewModel::deleteSelectedCombinedOnly,
                onDeletePairsDismiss = viewModel::dismissDeletePairsDialog,
                onToggleSortOrder = viewModel::toggleSortOrder,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = modifier,
            )
        }
    }
}
