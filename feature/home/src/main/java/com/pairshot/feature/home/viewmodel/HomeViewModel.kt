package com.pairshot.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pairshot.core.domain.album.AlbumRepository
import com.pairshot.core.domain.album.CreateAlbumUseCase
import com.pairshot.core.domain.album.DeleteAlbumUseCase
import com.pairshot.core.domain.album.RenameAlbumUseCase
import com.pairshot.core.domain.combine.DeleteCombinedPhotosUseCase
import com.pairshot.core.domain.membership.MembershipProvider
import com.pairshot.core.domain.pair.CanCreatePairUseCase
import com.pairshot.core.domain.pair.DeletePairsUseCase
import com.pairshot.core.domain.pair.PairNavigationTarget
import com.pairshot.core.domain.pair.PhotoPairRepository
import com.pairshot.core.domain.pair.ResolvePairNavigationTargetUseCase
import com.pairshot.core.domain.pair.SyncMissingSourcesUseCase
import com.pairshot.core.domain.settings.AppSettingsRepository
import com.pairshot.core.domain.tutorial.TutorialModeProvider
import com.pairshot.core.domain.tutorial.TutorialPairTracker
import com.pairshot.core.infra.location.LocationProvider
import com.pairshot.core.infra.location.LocationResult
import com.pairshot.core.model.Album
import com.pairshot.core.model.PhotoPair
import com.pairshot.core.model.SortOrder
import com.pairshot.core.ui.text.UiText
import com.pairshot.feature.home.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val WHILE_SUBSCRIBED_TIMEOUT_MS = 5_000L

enum class HomeMode { PAIRS, ALBUMS }

sealed interface HomeEvent {
    data class NavigateToAlbumDetail(
        val albumId: Long,
    ) : HomeEvent

    data class NavigateToPairPreview(
        val pairId: Long,
    ) : HomeEvent

    data class NavigateToAfterCamera(
        val pairId: Long,
    ) : HomeEvent

    data class NavigateToBeforeRetake(
        val pairId: Long,
    ) : HomeEvent

    data class NavigateToExportSettings(
        val pairIds: Set<Long>,
    ) : HomeEvent

    data class ShareSelected(
        val pairIds: Set<Long>,
    ) : HomeEvent

    data class SaveToDevice(
        val pairIds: Set<Long>,
    ) : HomeEvent

    data object DeleteCompleted : HomeEvent

    data class ShowError(
        val message: UiText,
    ) : HomeEvent
}

@HiltViewModel
@Suppress("LongParameterList")
class HomeViewModel
@Inject
constructor(
    private val photoPairRepository: PhotoPairRepository,
    private val albumRepository: AlbumRepository,
    private val createAlbumUseCase: CreateAlbumUseCase,
    private val deleteAlbumUseCase: DeleteAlbumUseCase,
    private val renameAlbumUseCase: RenameAlbumUseCase,
    private val deletePairsUseCase: DeletePairsUseCase,
    private val deleteCombinedPhotosUseCase: DeleteCombinedPhotosUseCase,
    private val resolvePairNavigationTargetUseCase: ResolvePairNavigationTargetUseCase,
    private val syncMissingSourcesUseCase: SyncMissingSourcesUseCase,
    private val locationProvider: LocationProvider,
    private val appSettingsRepository: AppSettingsRepository,
    private val canCreatePairUseCase: CanCreatePairUseCase,
    tutorialModeProvider: TutorialModeProvider,
    tutorialPairTracker: TutorialPairTracker,
    membershipProvider: MembershipProvider,
) : ViewModel() {
    suspend fun isCameraEntryAllowed(): Boolean = canCreatePairUseCase() is CanCreatePairUseCase.Result.Allowed

    val isProSubscriber: StateFlow<Boolean> =
        membershipProvider
            .observe()
            .map { it.isPro }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(WHILE_SUBSCRIBED_TIMEOUT_MS),
                initialValue = false,
            )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _mode = MutableStateFlow(HomeMode.PAIRS)
    val mode: StateFlow<HomeMode> = _mode.asStateFlow()

    val pairs: StateFlow<List<PhotoPair>> =
        combine(
            photoPairRepository.observeAll(),
            tutorialModeProvider.isActive,
            tutorialPairTracker.trackedPairIds,
        ) { all, tutorialActive, trackedIds ->
            if (tutorialActive) all.filter { it.id in trackedIds } else all
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(WHILE_SUBSCRIBED_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    val albums: StateFlow<List<Album>> =
        albumRepository
            .getAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(WHILE_SUBSCRIBED_TIMEOUT_MS),
                initialValue = emptyList(),
            )

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _albumSelectionMode = MutableStateFlow(false)
    val albumSelectionMode: StateFlow<Boolean> = _albumSelectionMode.asStateFlow()

    private val _selectedAlbumIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedAlbumIds: StateFlow<Set<Long>> = _selectedAlbumIds.asStateFlow()

    private val _currentLocation = MutableStateFlow<LocationResult?>(null)
    val currentLocation: StateFlow<LocationResult?> = _currentLocation.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events = _events.asSharedFlow()

    val sortOrder: StateFlow<SortOrder> =
        appSettingsRepository.homeSortOrderFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SortOrder.DESC,
        )

    fun toggleSortOrder() {
        viewModelScope.launch {
            val next = if (sortOrder.value == SortOrder.DESC) SortOrder.ASC else SortOrder.DESC
            appSettingsRepository.updateHomeSortOrder(next)
        }
    }

    fun setMode(mode: HomeMode) {
        _mode.value = mode
        if (mode == HomeMode.ALBUMS) exitSelectionMode() else exitAlbumSelectionMode()
    }

    fun enterSelectionMode(initialId: Long) {
        _selectionMode.value = true
        _selectedIds.value = setOf(initialId)
    }

    fun enterSelectionMode() {
        when (_mode.value) {
            HomeMode.PAIRS -> {
                _selectionMode.value = true
                _selectedIds.value = emptySet()
            }

            HomeMode.ALBUMS -> {
                _albumSelectionMode.value = true
                _selectedAlbumIds.value = emptySet()
            }
        }
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun toggleSelection(id: Long) {
        _selectedIds.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun toggleSelectAll() {
        when {
            _selectionMode.value -> {
                val allIds = pairs.value.map { it.id }.toSet()
                _selectedIds.value = if (_selectedIds.value == allIds) emptySet() else allIds
            }

            _albumSelectionMode.value -> {
                val allIds = albums.value.map { it.id }.toSet()
                _selectedAlbumIds.value =
                    if (_selectedAlbumIds.value == allIds) emptySet() else allIds
            }
        }
    }

    fun onPairCardClick(pairId: Long) {
        if (_selectionMode.value) {
            toggleSelection(pairId)
            return
        }
        val pair = pairs.value.firstOrNull { it.id == pairId } ?: return
        viewModelScope.launch {
            val event =
                when (val target = resolvePairNavigationTargetUseCase(pair)) {
                    is PairNavigationTarget.AfterCamera -> HomeEvent.NavigateToAfterCamera(target.pairId)
                    is PairNavigationTarget.BeforeRetakeCamera -> HomeEvent.NavigateToBeforeRetake(target.pairId)
                    is PairNavigationTarget.PairPreview -> HomeEvent.NavigateToPairPreview(target.pairId)
                }
            _events.emit(event)
        }
    }

    fun onAlbumCardClick(albumId: Long) {
        if (_albumSelectionMode.value) {
            toggleAlbumSelection(albumId)
            return
        }
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToAlbumDetail(albumId))
        }
    }

    fun onAlbumLongPress(albumId: Long) {
        _albumSelectionMode.value = true
        _selectedAlbumIds.value = setOf(albumId)
    }

    fun toggleAlbumSelection(albumId: Long) {
        _selectedAlbumIds.update { current ->
            if (albumId in current) current - albumId else current + albumId
        }
        if (_selectedAlbumIds.value.isEmpty()) _albumSelectionMode.value = false
    }

    fun exitAlbumSelectionMode() {
        _albumSelectionMode.value = false
        _selectedAlbumIds.value = emptySet()
    }

    fun renameSelectedAlbum(newName: String) {
        val albumId = _selectedAlbumIds.value.singleOrNull() ?: return
        viewModelScope.launch {
            renameAlbumUseCase(albumId, newName)
            exitAlbumSelectionMode()
        }
    }

    fun deleteSelectedAlbums() {
        val ids = _selectedAlbumIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { deleteAlbumUseCase(it) }
            exitAlbumSelectionMode()
        }
    }

    fun deleteSelected() {
        val ids = _selectedIds.value.toSet()
        viewModelScope.launch {
            val toDelete = pairs.value.filter { it.id in ids }
            val result = deletePairsUseCase(toDelete)
            exitSelectionMode()
            if (result.failed > 0) {
                _events.emit(
                    HomeEvent.ShowError(
                        UiText.Plural(
                            resId = R.plurals.home_event_delete_result,
                            count = result.deleted + result.failed,
                            args = listOf(result.deleted, result.failed),
                        ),
                    ),
                )
            }
            _events.emit(HomeEvent.DeleteCompleted)
        }
    }

    fun deleteCombinedOnly() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            deleteCombinedPhotosUseCase(ids)
            exitSelectionMode()
            _events.emit(HomeEvent.DeleteCompleted)
        }
    }

    fun onShareSelected() {
        val ids = _selectedIds.value.toSet()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _events.emit(HomeEvent.ShareSelected(ids))
        }
    }

    fun onSaveToDevice() {
        val ids = _selectedIds.value.toSet()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _events.emit(HomeEvent.SaveToDevice(ids))
        }
    }

    fun onExportSettings() {
        val ids = _selectedIds.value.toSet()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToExportSettings(ids))
        }
    }

    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _currentLocation.value = locationProvider.getCurrentLocation()
        }
    }

    fun createAlbum(
        name: String,
        address: String?,
        latitude: Double?,
        longitude: Double?,
    ) {
        viewModelScope.launch {
            try {
                val albumId = createAlbumUseCase(name, address, latitude, longitude)
                _events.emit(HomeEvent.NavigateToAlbumDetail(albumId))
            } catch (_: Exception) {
                _events.emit(
                    HomeEvent.ShowError(UiText.Resource(R.string.home_event_album_create_failed)),
                )
            }
        }
    }

    fun cleanupStaleSelections() {
        val validIds = pairs.value.map { it.id }.toSet()
        _selectedIds.update { current -> current.intersect(validIds) }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            runCatching { syncMissingSourcesUseCase() }
            _isRefreshing.value = false
        }
    }
}
