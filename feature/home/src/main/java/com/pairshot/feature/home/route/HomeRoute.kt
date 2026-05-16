package com.pairshot.feature.home.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.core.navigation.PaywallTrigger
import com.pairshot.feature.home.screen.HomeScreen
import com.pairshot.feature.home.viewmodel.HomeEvent
import com.pairshot.feature.home.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    onNavigateToPairPreview: (Long) -> Unit,
    onNavigateToAfterCamera: (Long) -> Unit,
    onNavigateToBeforeRetake: (Long) -> Unit,
    onNavigateToAlbumDetail: (Long) -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToPaywall: (PaywallTrigger) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToExportSettings: (Set<Long>) -> Unit,
    onShareSelected: (Set<Long>) -> Unit,
    onSaveToDevice: (Set<Long>) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val pairs by viewModel.pairs.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val albumSelectionMode by viewModel.albumSelectionMode.collectAsStateWithLifecycle()
    val selectedAlbumIds by viewModel.selectedAlbumIds.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isProSubscriber by viewModel.isProSubscriber.collectAsStateWithLifecycle()

    var showCreateAlbumDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pairs) {
        viewModel.cleanupStaleSelections()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToPairPreview -> onNavigateToPairPreview(event.pairId)
                is HomeEvent.NavigateToAfterCamera -> onNavigateToAfterCamera(event.pairId)
                is HomeEvent.NavigateToBeforeRetake -> onNavigateToBeforeRetake(event.pairId)
                is HomeEvent.NavigateToAlbumDetail -> onNavigateToAlbumDetail(event.albumId)
                is HomeEvent.NavigateToExportSettings -> onNavigateToExportSettings(event.pairIds)
                is HomeEvent.ShareSelected -> onShareSelected(event.pairIds)
                is HomeEvent.SaveToDevice -> onSaveToDevice(event.pairIds)
                is HomeEvent.DeleteCompleted -> Unit
                is HomeEvent.ShowError -> Unit
            }
        }
    }

    HomeScreen(
        mode = mode,
        pairs = pairs,
        albums = albums,
        selectionMode = selectionMode,
        selectedIds = selectedIds,
        albumSelectionMode = albumSelectionMode,
        selectedAlbumIds = selectedAlbumIds,
        currentLocation = currentLocation,
        showCreateAlbumDialog = showCreateAlbumDialog,
        sortOrder = sortOrder,
        onModeSelected = viewModel::setMode,
        onToggleSortOrder = viewModel::toggleSortOrder,
        onPairClick = viewModel::onPairCardClick,
        onPairLongClick = { id ->
            if (!selectionMode) {
                viewModel.enterSelectionMode(id)
            } else {
                viewModel.toggleSelection(id)
            }
        },
        onAlbumClick = viewModel::onAlbumCardClick,
        onAlbumLongPress = viewModel::onAlbumLongPress,
        onEnterSelectionMode = viewModel::enterSelectionMode,
        onExitAlbumSelectionMode = viewModel::exitAlbumSelectionMode,
        onRenameAlbum = viewModel::renameSelectedAlbum,
        onDeleteAlbums = viewModel::deleteSelectedAlbums,
        onExitSelectionMode = viewModel::exitSelectionMode,
        onToggleSelectAll = viewModel::toggleSelectAll,
        onShare = viewModel::onShareSelected,
        onSaveToDevice = viewModel::onSaveToDevice,
        onDeleteSelected = viewModel::deleteSelected,
        onDeleteCombinedOnly = viewModel::deleteCombinedOnly,
        onExportSettings = viewModel::onExportSettings,
        onCreateAlbumClick = { showCreateAlbumDialog = true },
        onDismissCreateAlbumDialog = { showCreateAlbumDialog = false },
        onConfirmCreateAlbum = { name, address, lat, lng ->
            showCreateAlbumDialog = false
            viewModel.createAlbum(name, address, lat, lng)
        },
        onFetchLocation = viewModel::fetchCurrentLocation,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToCamera = {
            scope.launch {
                if (viewModel.isCameraEntryAllowed()) {
                    onNavigateToCamera()
                } else {
                    onNavigateToPaywall(PaywallTrigger.DAILY_LIMIT)
                }
            }
        },
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        isProSubscriber = isProSubscriber,
    )
}
