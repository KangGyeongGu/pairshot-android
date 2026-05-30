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
import com.pairshot.core.adsui.component.PairCardGridSection
import com.pairshot.core.adsui.component.PairShotBannerAd
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.domain.tutorial.AnchorKey
import com.pairshot.core.infra.location.LocationResult
import com.pairshot.core.model.Album
import com.pairshot.core.model.PhotoPair
import com.pairshot.core.model.SortOrder
import com.pairshot.core.ui.component.DeletePairConfirmDialog
import com.pairshot.core.ui.component.PairShotDialog
import com.pairshot.core.ui.state.SelectionState
import com.pairshot.feature.home.R
import com.pairshot.feature.home.component.HomeAlbumGridSection
import com.pairshot.feature.home.component.HomeAlbumSelectionBottomBar
import com.pairshot.feature.home.component.HomeEmptyAction
import com.pairshot.feature.home.component.HomeFilterRow
import com.pairshot.feature.home.component.HomePrimaryActionBar
import com.pairshot.feature.home.component.HomeSelectionBottomBar
import com.pairshot.feature.home.component.HomeTopBar
import com.pairshot.feature.home.dialog.CreateAlbumDialog
import com.pairshot.feature.home.viewmodel.HomeMode
import com.pairshot.feature.tutorial.ui.modifier.tutorialAnchor
import kotlinx.collections.immutable.ImmutableList
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.pairshot.core.ui.R as CoreR

private val HomeDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy. MM. dd", Locale.KOREAN)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mode: HomeMode,
    pairs: ImmutableList<PhotoPair>,
    albums: ImmutableList<Album>,
    pairSelection: SelectionState,
    albumSelection: SelectionState,
    currentLocation: LocationResult?,
    showCreateAlbumDialog: Boolean,
    sortOrder: SortOrder,
    onModeChange: (HomeMode) -> Unit,
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
    onDeleteSelection: () -> Unit,
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

    val inSelectionMode = pairSelection.isSelectionMode || albumSelection.isSelectionMode
    val listIsEmpty = if (mode == HomeMode.PAIRS) pairs.isEmpty() else albums.isEmpty()
    val primaryLabel =
        if (mode == HomeMode.PAIRS) {
            stringResource(CoreR.string.common_button_start_capture)
        } else {
            stringResource(R.string.home_button_create_album)
        }
    val primaryAction: () -> Unit =
        if (mode == HomeMode.PAIRS) onNavigateToCamera else onCreateAlbumClick
    val currentTotalCount = if (albumSelection.isSelectionMode) albums.size else pairs.size
    val currentSelectedCount =
        if (albumSelection.isSelectionMode) albumSelection.selectedCount else pairSelection.selectedCount
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
                if (albumSelection.isSelectionMode) onExitAlbumSelectionMode else onExitSelectionMode,
                onToggleSelectAll = onToggleSelectAll,
                onEnterSelectionMode = onEnterSelectionMode,
                onNavigateToSettings = onNavigateToSettings,
            )
        },
        bottomBar = {
            when {
                pairSelection.isSelectionMode -> {
                    HomeSelectionBottomBar(
                        selectedCount = pairSelection.selectedCount,
                        onShare = onShare,
                        onSaveToDevice = onSaveToDevice,
                        onDelete = { showDeleteConfirmDialog = true },
                        onExportSettings = onExportSettings,
                    )
                }

                albumSelection.isSelectionMode -> {
                    HomeAlbumSelectionBottomBar(
                        selectedCount = albumSelection.selectedCount,
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
                onModeChange = onModeChange,
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
                val today = remember { LocalDate.now(ZoneId.systemDefault()) }
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (mode) {
                        HomeMode.PAIRS -> {
                            PairCardGridSection(
                                pairs = pairs,
                                selectedIds = pairSelection.selectedIds,
                                isSelectionMode = pairSelection.isSelectionMode,
                                sortOrder = sortOrder,
                                onPairClick = onPairClick,
                                onPairLongPress = onPairLongClick,
                                contentPadding = contentPadding,
                                dateHeaderLabel = { date ->
                                    formatHomeDateLabel(date = date, today = today)
                                },
                                firstPairModifier = Modifier.tutorialAnchor(AnchorKey.HOME_PAIR_CARD_FIRST),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        HomeMode.ALBUMS -> {
                            HomeAlbumGridSection(
                                albums = albums,
                                isSelectionMode = albumSelection.isSelectionMode,
                                selectedAlbumIds = albumSelection.selectedIds,
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
            pairs.count { it.id in pairSelection.selectedIds && it.hasCombined }
        DeletePairConfirmDialog(
            pairCount = pairSelection.selectedCount,
            combinedCount = combinedInSelection,
            onDeleteAll = {
                showDeleteConfirmDialog = false
                onDeleteSelection()
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
                        albumSelection.selectedCount,
                        albumSelection.selectedCount,
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
            albums.firstOrNull { it.id in albumSelection.selectedIds }?.name.orEmpty()
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

@Composable
private fun formatHomeDateLabel(
    date: LocalDate,
    today: LocalDate,
): String {
    val base = date.format(HomeDateFormatter)
    return when (date) {
        today -> stringResource(R.string.home_date_suffix_today, base)
        today.minusDays(1) -> stringResource(R.string.home_date_suffix_yesterday, base)
        else -> base
    }
}
