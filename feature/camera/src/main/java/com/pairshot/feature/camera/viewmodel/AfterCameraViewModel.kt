package com.pairshot.feature.camera.viewmodel

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pairshot.core.domain.album.AlbumRepository
import com.pairshot.core.domain.pair.GetLatestBeforeThumbnailUseCase
import com.pairshot.core.domain.pair.PhotoPairRepository
import com.pairshot.core.domain.settings.AppSettingsRepository
import com.pairshot.core.domain.tutorial.TutorialModeProvider
import com.pairshot.core.domain.tutorial.TutorialPairTracker
import com.pairshot.core.model.AppSettings
import com.pairshot.core.model.AspectRatio
import com.pairshot.core.model.CameraCapabilities
import com.pairshot.core.model.FlashMode
import com.pairshot.core.model.LensFacing
import com.pairshot.core.model.PairStatus
import com.pairshot.core.model.PhotoPair
import com.pairshot.core.model.SortOrder
import com.pairshot.core.navigation.AfterCamera
import com.pairshot.feature.camera.component.ZoomStateHolder
import com.pairshot.feature.camera.component.ZoomUiState
import com.pairshot.feature.camera.state.CameraSettingsState
import com.pairshot.feature.camera.state.CameraSettingsStateHolder
import com.pairshot.feature.camera.state.CapabilityAdjustment
import com.pairshot.feature.camera.state.InitialCameraSessionConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

sealed interface AfterCameraEvent {
    data object AllCompleted : AfterCameraEvent

    data class AfterSaved(
        val pairId: Long,
    ) : AfterCameraEvent

    data class CaptureError(
        val message: String,
    ) : AfterCameraEvent

    data class SaveError(
        val message: String,
    ) : AfterCameraEvent
}

data class OverlayInputs(
    val pair: PhotoPair?,
    val enabled: Boolean,
    val alpha: Float,
    val lensFacing: LensFacing,
)

@HiltViewModel
@Suppress("TooManyFunctions")
class AfterCameraViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val photoPairRepository: PhotoPairRepository,
        private val getLatestBeforeThumbnailUseCase: GetLatestBeforeThumbnailUseCase,
        private val appSettingsRepository: AppSettingsRepository,
        private val cameraSettings: CameraSettingsStateHolder,
        albumRepository: AlbumRepository,
        tutorialModeProvider: TutorialModeProvider,
        tutorialPairTracker: TutorialPairTracker,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<AfterCamera>()
        private val initialPairId: Long? = route.initialPairId
        private val albumId: Long? = route.albumId

        private val isDateFilterActive: Boolean = albumId == null && initialPairId != null

        private val targetBeforeDate = MutableStateFlow<LocalDate?>(null)

        private val retakeTargetPair = MutableStateFlow<PhotoPair?>(null)
        val isRetakeMode: StateFlow<Boolean> =
            retakeTargetPair
                .map { it != null }
                .stateIn(viewModelScope, SharingStarted.Eagerly, false)

        private val retakeResolved = MutableStateFlow(initialPairId == null)

        val sortOrder: StateFlow<SortOrder> =
            if (albumId != null) {
                appSettingsRepository.albumSortOrderFlow
            } else {
                appSettingsRepository.homeSortOrderFlow
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = SortOrder.DESC,
            )

        fun toggleSortOrder() {
            viewModelScope.launch {
                val next = if (sortOrder.value == SortOrder.DESC) SortOrder.ASC else SortOrder.DESC
                if (albumId != null) {
                    appSettingsRepository.updateAlbumSortOrder(next)
                } else {
                    appSettingsRepository.updateHomeSortOrder(next)
                }
            }
        }

        val totalPairCount: StateFlow<Int> =
            combine(
                when {
                    albumId != null -> {
                        albumRepository.observePairs(albumId).map { it.size }
                    }

                    initialPairId != null -> {
                        combine(photoPairRepository.observeAll(), targetBeforeDate) { all, date ->
                            if (date == null) 0 else all.count { it.beforeTimestamp.toLocalDate() == date }
                        }
                    }

                    else -> {
                        photoPairRepository.countAll()
                    }
                },
                retakeTargetPair,
            ) { count, retake ->
                if (retake != null) 1 else count
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), 0)

        init {
            if (initialPairId != null) {
                viewModelScope.launch {
                    try {
                        val pair = photoPairRepository.getById(initialPairId)
                        if (pair != null && pair.status == PairStatus.PAIRED) {
                            retakeTargetPair.value = pair
                        } else if (isDateFilterActive) {
                            targetBeforeDate.value = pair?.beforeTimestamp?.toLocalDate()
                        }
                    } finally {
                        retakeResolved.value = true
                    }
                }
            }
        }

        private val baseUnpairedPhotos =
            combine(
                if (albumId != null) {
                    photoPairRepository.observeUnpairedByAlbum(albumId)
                } else {
                    photoPairRepository.observeUnpaired()
                },
                sortOrder,
                targetBeforeDate,
                retakeTargetPair,
            ) { list, order, targetDate, retake ->
                if (retake != null) return@combine listOf(retake)
                val filtered =
                    when {
                        !isDateFilterActive -> list
                        targetDate == null -> emptyList()
                        else -> list.filter { it.beforeTimestamp.toLocalDate() == targetDate }
                    }
                when (order) {
                    SortOrder.DESC -> filtered.sortedByDescending { it.beforeTimestamp }
                    SortOrder.ASC -> filtered.sortedBy { it.beforeTimestamp }
                }
            }

        val unpairedPhotos: StateFlow<List<PhotoPair>> =
            combine(
                baseUnpairedPhotos,
                tutorialModeProvider.isActive,
                tutorialPairTracker.trackedPairIds,
            ) { list, tutorialActive, trackedIds ->
                if (tutorialActive) list.filter { it.id in trackedIds } else list
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), emptyList())

        val lastPairThumbnailUri: StateFlow<String?> =
            getLatestBeforeThumbnailUseCase()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), null)

        private val _currentIndex = MutableStateFlow(0)
        val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

        private val _lensFacing = MutableStateFlow(LensFacing.BACK)
        val lensFacing: StateFlow<LensFacing> = _lensFacing.asStateFlow()

        private val zoomHolder = ZoomStateHolder()
        val zoomUiState: StateFlow<ZoomUiState> = zoomHolder.zoomUiState

        private val _isSaving = MutableStateFlow(false)
        val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

        private val _overlayEnabled = MutableStateFlow(true)
        val overlayEnabled: StateFlow<Boolean> = _overlayEnabled.asStateFlow()

        private val _overlayAlpha = MutableStateFlow(DEFAULT_OVERLAY_ALPHA)
        val overlayAlpha: StateFlow<Float> = _overlayAlpha.asStateFlow()

        val overlayInputs: StateFlow<OverlayInputs> =
            combine(
                unpairedPhotos,
                _currentIndex,
                _overlayEnabled,
                _overlayAlpha,
                _lensFacing,
            ) { photos, index, enabled, alpha, lens ->
                OverlayInputs(
                    pair = photos.getOrNull(index),
                    enabled = enabled,
                    alpha = alpha,
                    lensFacing = lens,
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                OverlayInputs(null, true, DEFAULT_OVERLAY_ALPHA, LensFacing.BACK),
            )

        val settingsState: StateFlow<CameraSettingsState> = cameraSettings.state

        val lockedAspectRatio: StateFlow<AspectRatio?> =
            combine(unpairedPhotos, _currentIndex) { photos, idx ->
                photos.getOrNull(idx)?.aspectRatio
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), null)

        private val _events = MutableSharedFlow<AfterCameraEvent>()
        val events: SharedFlow<AfterCameraEvent> = _events.asSharedFlow()

        private val _pairsLoaded = MutableStateFlow(false)
        val pairsLoaded: StateFlow<Boolean> = _pairsLoaded.asStateFlow()

        private var initialIndexSet = false

        suspend fun loadInitialSettings(): InitialCameraSessionConfig {
            val config = cameraSettings.loadInitial()
            val settings = appSettingsRepository.getCurrent()
            _overlayEnabled.value = settings.overlayEnabled
            _overlayAlpha.value = settings.defaultOverlayAlpha.coerceIn(0f, 1f)
            return config
        }

        fun onCameraZoomCapabilities(
            min: Float,
            max: Float,
        ) {
            zoomHolder.initFromZoomState(min, max)
        }

        fun updateZoomRatio(ratio: Float) {
            zoomHolder.updateZoomRatio(ratio)
        }

        fun onPresetTapped(preset: Float) {
            zoomHolder.onPresetTapped(preset)
        }

        fun applyCustomRatio() {
            zoomHolder.applyCustomRatio()
        }

        fun resetZoomForLensSwitch() {
            zoomHolder.resetZoomForLensSwitch()
        }

        fun restoreZoomForPair(zoomLevel: Float?) {
            zoomHolder.restoreZoomForPair(zoomLevel)
        }

        fun adjustForCapabilities(caps: CameraCapabilities): CapabilityAdjustment = cameraSettings.adjustForCapabilities(caps)

        fun onUnpairedPhotosUpdated(photos: List<PhotoPair>) {
            _pairsLoaded.value = true
            if (!initialIndexSet && photos.isNotEmpty() && initialPairId != null) {
                val idx = photos.indexOfFirst { it.id == initialPairId }
                if (idx >= 0) {
                    _currentIndex.value = idx
                    initialIndexSet = true
                }
            }
            if (photos.isNotEmpty() && _currentIndex.value >= photos.size) {
                _currentIndex.value = photos.size - 1
            }
        }

        fun selectIndex(index: Int) {
            _currentIndex.value = index
        }

        fun moveToNext() {
            val photos = unpairedPhotos.value
            if (_currentIndex.value < photos.size - 1) {
                _currentIndex.value++
            }
        }

        fun moveToPrevious() {
            if (_currentIndex.value > 0) {
                _currentIndex.value--
            }
        }

        fun startCapturing() {
            _isSaving.value = true
        }

        fun finishCapturing() {
            _isSaving.value = false
        }

        fun emitCaptureError(message: String) {
            viewModelScope.launch {
                _events.emit(AfterCameraEvent.CaptureError(message))
            }
        }

        fun saveAfterPhoto(tempUri: String) {
            val photos = unpairedPhotos.value
            if (photos.isEmpty()) {
                _isSaving.value = false
                return
            }
            val currentPair = photos.getOrNull(_currentIndex.value)
            if (currentPair == null) {
                _isSaving.value = false
                return
            }
            viewModelScope.launch {
                try {
                    photoPairRepository.saveAfterPhoto(
                        pairId = currentPair.id,
                        tempFileUri = tempUri,
                    )
                    _events.emit(AfterCameraEvent.AfterSaved(currentPair.id))
                } catch (e: IOException) {
                    _events.emit(AfterCameraEvent.SaveError(e.message ?: "save failed"))
                } catch (e: SecurityException) {
                    _events.emit(AfterCameraEvent.SaveError(e.message ?: "save failed"))
                } catch (e: SQLiteException) {
                    _events.emit(AfterCameraEvent.SaveError(e.message ?: "save failed"))
                } catch (e: IllegalStateException) {
                    _events.emit(AfterCameraEvent.SaveError(e.message ?: "save failed"))
                } catch (e: IllegalArgumentException) {
                    _events.emit(AfterCameraEvent.SaveError(e.message ?: "save failed"))
                } finally {
                    _isSaving.value = false
                }
            }
        }

        fun emitAllCompleted() {
            viewModelScope.launch {
                if (!retakeResolved.value) return@launch
                if (retakeTargetPair.value != null) return@launch
                _events.emit(AfterCameraEvent.AllCompleted)
            }
        }

        fun toggleOverlay() {
            val next = !_overlayEnabled.value
            _overlayEnabled.value = next
            viewModelScope.launch {
                appSettingsRepository.updateOverlayEnabled(next)
            }
        }

        fun updateOverlayAlpha(alpha: Float) {
            val clamped = alpha.coerceIn(0f, 1f)
            _overlayAlpha.value = clamped
            viewModelScope.launch {
                appSettingsRepository.updateOverlayAlpha(clamped)
            }
        }

        fun toggleLensFacing(): LensFacing {
            val next = if (_lensFacing.value == LensFacing.BACK) LensFacing.FRONT else LensFacing.BACK
            _lensFacing.value = next
            zoomHolder.resetZoomForLensSwitch()
            return next
        }

        fun toggleGrid() = cameraSettings.toggleGrid(viewModelScope)

        fun toggleLevel() = cameraSettings.toggleLevel(viewModelScope)

        fun cycleFlash(): FlashMode = cameraSettings.cycleFlash(viewModelScope)

        fun toggleNightMode(): Boolean = cameraSettings.toggleNightMode(viewModelScope)

        fun toggleHdr(): Boolean = cameraSettings.toggleHdr(viewModelScope)

        fun applyLockedAspectRatio(ratio: AspectRatio?) = cameraSettings.applyLockedAspectRatio(ratio)

        fun cycleAspectRatio(): AspectRatio? = cameraSettings.cycleAspectRatio(viewModelScope)

        fun setExposureIndex(index: Int) = cameraSettings.setExposureIndex(index)

        fun toggleSettingsPanel() = cameraSettings.toggleSettingsPanel()

        fun dismissSettingsPanel() = cameraSettings.dismissSettingsPanel()

        companion object {
            private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
            private val DEFAULT_OVERLAY_ALPHA = AppSettings.DEFAULT_OVERLAY_ALPHA
        }
    }

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
