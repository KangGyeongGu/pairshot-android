package com.pairshot.feature.home.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.core.domain.tutorial.TutorialActionIds
import com.pairshot.core.navigation.PaywallTrigger
import com.pairshot.feature.home.screen.HomeScreen
import com.pairshot.feature.home.viewmodel.HomeEvent
import com.pairshot.feature.home.viewmodel.HomeViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
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
    onShareSelection: (Set<Long>) -> Unit,
    onSaveToDevice: (Set<Long>) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val tutorialEntryPoint =
        remember(context) {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                HomeTutorialEntryPoint::class.java,
            )
        }
    val tutorialActions = remember(tutorialEntryPoint) { tutorialEntryPoint.tutorialActionDispatcher() }
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

    val currentOnNavigateToPairPreview by rememberUpdatedState(onNavigateToPairPreview)
    val currentOnNavigateToAfterCamera by rememberUpdatedState(onNavigateToAfterCamera)
    val currentOnNavigateToBeforeRetake by rememberUpdatedState(onNavigateToBeforeRetake)
    val currentOnNavigateToAlbumDetail by rememberUpdatedState(onNavigateToAlbumDetail)
    val currentOnNavigateToExportSettings by rememberUpdatedState(onNavigateToExportSettings)
    val currentOnShareSelection by rememberUpdatedState(onShareSelection)
    val currentOnSaveToDevice by rememberUpdatedState(onSaveToDevice)

    LaunchedEffect(pairs) {
        viewModel.cleanupStaleSelections()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NavigateToPairPreview -> {
                    tutorialActions.report(TutorialActionIds.HOME_PAIR_CARD_TAPPED)
                    currentOnNavigateToPairPreview(event.pairId)
                }

                is HomeEvent.NavigateToAfterCamera -> {
                    tutorialActions.report(TutorialActionIds.HOME_PAIR_CARD_TAPPED)
                    currentOnNavigateToAfterCamera(event.pairId)
                }

                is HomeEvent.NavigateToBeforeRetake -> {
                    currentOnNavigateToBeforeRetake(event.pairId)
                }

                is HomeEvent.NavigateToAlbumDetail -> {
                    currentOnNavigateToAlbumDetail(event.albumId)
                }

                is HomeEvent.NavigateToExportSettings -> {
                    currentOnNavigateToExportSettings(event.pairIds)
                }

                is HomeEvent.ShareSelected -> {
                    currentOnShareSelection(event.pairIds)
                }

                is HomeEvent.SaveToDevice -> {
                    currentOnSaveToDevice(event.pairIds)
                }

                is HomeEvent.DeleteCompleted -> {
                    Unit
                }

                is HomeEvent.ShowError -> {
                    Unit
                }
            }
        }
    }

    HomeScreen(
        mode = mode,
        pairs = pairs.toImmutableList(),
        albums = albums.toImmutableList(),
        selectionMode = selectionMode,
        selectedIds = selectedIds.toImmutableSet(),
        albumSelectionMode = albumSelectionMode,
        selectedAlbumIds = selectedAlbumIds.toImmutableSet(),
        currentLocation = currentLocation,
        showCreateAlbumDialog = showCreateAlbumDialog,
        sortOrder = sortOrder,
        onModeChange = viewModel::setMode,
        onToggleSortOrder = viewModel::toggleSortOrder,
        onPairClick = viewModel::onPairCardClick,
        onPairLongClick = { id ->
            if (!selectionMode) {
                viewModel.enterSelectionMode(id)
                tutorialActions.report(TutorialActionIds.HOME_SELECTION_MODE_ENTERED)
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
        onExitSelectionMode = {
            viewModel.exitSelectionMode()
            tutorialActions.report(TutorialActionIds.HOME_SELECTION_EXITED)
        },
        onToggleSelectAll = viewModel::toggleSelectAll,
        onShare = viewModel::onShareSelection,
        onSaveToDevice = viewModel::onSaveToDevice,
        onDeleteSelection = viewModel::deleteSelected,
        onDeleteCombinedOnly = viewModel::deleteCombinedOnly,
        onExportSettings = viewModel::onExportSettings,
        onCreateAlbumClick = { showCreateAlbumDialog = true },
        onDismissCreateAlbumDialog = { showCreateAlbumDialog = false },
        onConfirmCreateAlbum = { name, address, lat, lng ->
            showCreateAlbumDialog = false
            viewModel.createAlbum(name, address, lat, lng)
        },
        onFetchLocation = viewModel::fetchCurrentLocation,
        onNavigateToSettings = {
            tutorialActions.report(TutorialActionIds.HOME_SETTINGS_OPENED)
            onNavigateToSettings()
        },
        onNavigateToCamera = {
            scope.launch {
                if (viewModel.isCameraEntryAllowed()) {
                    tutorialActions.report(TutorialActionIds.HOME_SHOOT_CLICKED)
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
