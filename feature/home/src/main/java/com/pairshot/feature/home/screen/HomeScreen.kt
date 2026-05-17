package com.pairshot.feature.home.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pairshot.core.adsui.component.PairShotBannerAd
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.infra.location.LocationResult
import com.pairshot.core.model.Album
import com.pairshot.core.model.PhotoPair
import com.pairshot.core.model.SortOrder
import com.pairshot.core.ui.component.DeletePairConfirmDialog
import com.pairshot.core.ui.component.PairShotDialog
import com.pairshot.feature.home.R
import com.pairshot.feature.home.component.HomeAlbumGridSection
import com.pairshot.feature.home.component.HomeAlbumSelectionBottomBar
import com.pairshot.feature.home.component.HomeEmptyAction
import com.pairshot.feature.home.component.HomeFilterRow
import com.pairshot.feature.home.component.HomePairGridSection
import com.pairshot.feature.home.component.HomePrimaryActionBar
import com.pairshot.feature.home.component.HomeSelectionBottomBar
import com.pairshot.feature.home.component.HomeTopBar
import com.pairshot.feature.home.dialog.CreateAlbumDialog
import com.pairshot.feature.home.viewmodel.HomeMode
import com.pairshot.core.ui.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mode: HomeMode,
    pairs: List<PhotoPair>,
    albums: List<Album>,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    albumSelectionMode: Boolean,
    selectedAlbumIds: Set<Long>,
    currentLocation: LocationResult?,
    showCreateAlbumDialog: Boolean,
    sortOrder: SortOrder,
    onModeSelected: (HomeMode) -> Unit,
    onToggleSortOrder: () -> Unit,
    onPairClick: (Long) -> Unit,
    onPairLongClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onAlbumLongPress: (Long) -> Unit,
    onEnterSelectionMode: () -> Unit,
    onExitAlbumSelectionMode: () -> Unit,
    onRenameAlbum: (String) -> Unit,
    onDeleteAlbums: () -> Unit,
    onExitSelectionMode: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onShare: () -> Unit,
    onSaveToDevice: () -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteCombinedOnly: () -> Unit,
    onExportSettings: () -> Unit,
    onCreateAlbumClick: () -> Unit,
    onDismissCreateAlbumDialog: () -> Unit,
    onConfirmCreateAlbum: (String, String?, Double?, Double?) -> Unit,
    onFetchLocation: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCamera: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    isProSubscriber: Boolean,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAlbumDeleteDialog by remember { mutableStateOf(false) }
    var showAlbumRenameDialog by remember { mutableStateOf(false) }

    val inSelectionMode = selectionMode || albumSelectionMode
    val listIsEmpty = if (mode == HomeMode.PAIRS) pairs.isEmpty() else albums.isEmpty()
    val primaryLabel =
        if (mode == HomeMode.PAIRS) {
            stringResource(CoreR.string.common_button_start_capture)
        } else {
            stringResource(R.string.home_button_create_album)
        }
    val primaryAction: () -> Unit =
        if (mode == HomeMode.PAIRS) onNavigateToCamera else onCreateAlbumClick
    val currentTotalCount = if (albumSelectionMode) albums.size else pairs.size
    val currentSelectedCount =
        if (albumSelectionMode) selectedAlbumIds.size else selectedIds.size
    val allSelected = currentTotalCount > 0 && currentSelectedCount == currentTotalCount

    Scaffold(
        modifier = modifier,
        topBar = {
            HomeTopBar(
                selectionMode = inSelectionMode,
                selectedCount = currentSelectedCount,
                allSelected = allSelected,
                isProSubscriber = isProSubscriber,
                onExitSelectionMode =
                    if (albumSelectionMode) onExitAlbumSelectionMode else onExitSelectionMode,
                onToggleSelectAll = onToggleSelectAll,
                onEnterSelectionMode = onEnterSelectionMode,
                onNavigateToSettings = onNavigateToSettings,
            )
        },
        bottomBar = {
            when {
                selectionMode -> {
                    HomeSelectionBottomBar(
                        selectedCount = selectedIds.size,
                        onShare = onShare,
                        onSaveToDevice = onSaveToDevice,
                        onDelete = { showDeleteConfirmDialog = true },
                        onExportSettings = onExportSettings,
                    )
                }

                albumSelectionMode -> {
                    HomeAlbumSelectionBottomBar(
                        selectedCount = selectedAlbumIds.size,
                        onRename = { showAlbumRenameDialog = true },
                        onDelete = { showAlbumDeleteDialog = true },
                    )
                }

                !listIsEmpty -> {
                    HomePrimaryActionBar(
                        label = primaryLabel,
                        onClick = primaryAction,
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            PairShotBannerAd()
            Spacer(modifier = Modifier.height(PairShotSpacing.sm))
            HomeFilterRow(
                selectedMode = mode,
                inSelectionMode = inSelectionMode,
                sortOrder = sortOrder,
                onModeSelected = onModeSelected,
                onToggleSortOrder = onToggleSortOrder,
            )
            Spacer(modifier = Modifier.height(PairShotSpacing.sm))

            if (listIsEmpty && !inSelectionMode) {
                HomeEmptyAction(
                    label = primaryLabel,
                    onClick = primaryAction,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val contentPadding =
                    PaddingValues(
                        start = PairShotSpacing.md,
                        end = PairShotSpacing.md,
                        bottom = PairShotSpacing.sm,
                    )
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (mode) {
                        HomeMode.PAIRS -> {
                            HomePairGridSection(
                                pairs = pairs,
                                selectedIds = selectedIds,
                                selectionMode = selectionMode,
                                sortOrder = sortOrder,
                                onPairClick = onPairClick,
                                onPairLongClick = onPairLongClick,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        HomeMode.ALBUMS -> {
                            HomeAlbumGridSection(
                                albums = albums,
                                isSelectionMode = albumSelectionMode,
                                selectedAlbumIds = selectedAlbumIds,
                                onAlbumClick = onAlbumClick,
                                onAlbumLongPress = onAlbumLongPress,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateAlbumDialog) {
        CreateAlbumDialog(
            currentLocation = currentLocation,
            onFetchLocation = onFetchLocation,
            onConfirm = onConfirmCreateAlbum,
            onDismiss = onDismissCreateAlbumDialog,
        )
    }

    if (showDeleteConfirmDialog) {
        val combinedInSelection =
            pairs.count { it.id in selectedIds && it.hasCombined }
        DeletePairConfirmDialog(
            pairCount = selectedIds.size,
            combinedCount = combinedInSelection,
            onDeleteAll = {
                showDeleteConfirmDialog = false
                onDeleteSelected()
            },
            onDeleteCombinedOnly = {
                showDeleteConfirmDialog = false
                onDeleteCombinedOnly()
            },
            onDismiss = { showDeleteConfirmDialog = false },
        )
    }

    if (showAlbumDeleteDialog) {
        PairShotDialog(
            onDismissRequest = { showAlbumDeleteDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.home_dialog_album_delete_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.home_dialog_album_delete_confirm,
                            selectedAlbumIds.size,
                            selectedAlbumIds.size,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAlbumDeleteDialog = false
                        onDeleteAlbums()
                    },
                ) {
                    Text(
                        text = stringResource(CoreR.string.common_button_delete),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAlbumDeleteDialog = false }) {
                    Text(
                        text = stringResource(CoreR.string.common_button_cancel),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
        )
    }

    if (showAlbumRenameDialog) {
        val currentName =
            albums.firstOrNull { it.id in selectedAlbumIds }?.name.orEmpty()
        AlbumRenameDialog(
            currentName = currentName,
            onConfirm = { newName ->
                showAlbumRenameDialog = false
                onRenameAlbum(newName)
            },
            onDismiss = { showAlbumRenameDialog = false },
        )
    }
}
