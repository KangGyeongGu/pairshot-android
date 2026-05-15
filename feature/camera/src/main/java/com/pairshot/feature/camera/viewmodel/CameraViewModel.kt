package com.pairshot.feature.camera.viewmodel

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.pairshot.core.domain.pair.CanCreatePairUseCase
import com.pairshot.core.domain.pair.GetLatestBeforeThumbnailUseCase
import com.pairshot.core.domain.pair.PhotoPairRepository
import com.pairshot.core.model.AspectRatio
import com.pairshot.core.model.CameraCapabilities
import com.pairshot.core.model.FlashMode
import com.pairshot.core.model.LensFacing
import com.pairshot.core.navigation.Camera
import com.pairshot.feature.camera.component.ZoomStateHolder
import com.pairshot.feature.camera.component.ZoomUiState
import com.pairshot.feature.camera.state.CameraSettingsState
import com.pairshot.feature.camera.state.CameraSettingsStateHolder
import com.pairshot.feature.camera.state.CapabilityAdjustment
import com.pairshot.feature.camera.state.InitialCameraSessionConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

sealed interface CameraEvent {
    data class PhotoSaved(
        val pairId: Long,
    ) : CameraEvent

    data class CaptureError(
        val message: String,
    ) : CameraEvent

    data class SaveError(
        val message: String,
    ) : CameraEvent
}

@HiltViewModel
class CameraViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val photoPairRepository: PhotoPairRepository,
        private val getLatestBeforeThumbnailUseCase: GetLatestBeforeThumbnailUseCase,
        private val cameraSettings: CameraSettingsStateHolder,
        private val canCreatePairUseCase: CanCreatePairUseCase,
    ) : ViewModel() {
        private val cameraRoute = savedStateHandle.toRoute<Camera>()
        private val albumId: Long? = cameraRoute.albumId
        val replaceBeforeForPairId: Long? = cameraRoute.replaceBeforeForPairId
        private val sessionStartTimestamp: Long = System.currentTimeMillis()

        private val _events = MutableSharedFlow<CameraEvent>()
        val events: SharedFlow<CameraEvent> = _events.asSharedFlow()

        private val _lensFacing = MutableStateFlow(LensFacing.BACK)
        val lensFacing: StateFlow<LensFacing> = _lensFacing.asStateFlow()

        private val zoomHolder = ZoomStateHolder()
        val zoomUiState: StateFlow<ZoomUiState> = zoomHolder.zoomUiState

        private val _isSaving = MutableStateFlow(false)
        val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

        private val _beforePreviewUris = MutableStateFlow<List<String>>(emptyList())
        val beforePreviewUris: StateFlow<List<String>> = _beforePreviewUris.asStateFlow()

        val lastPairThumbnailUri: StateFlow<String?> =
            getLatestBeforeThumbnailUseCase()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), null)

        val settingsState: StateFlow<CameraSettingsState> = cameraSettings.state

        private var observePairsJob: Job? = null

        suspend fun loadInitialSettings(): InitialCameraSessionConfig = cameraSettings.loadInitial()

        suspend fun canCreatePair(): CanCreatePairUseCase.Result =
            if (replaceBeforeForPairId != null) {
                CanCreatePairUseCase.Result.Allowed
            } else {
                canCreatePairUseCase()
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

        fun adjustForCapabilities(caps: CameraCapabilities): CapabilityAdjustment = cameraSettings.adjustForCapabilities(caps)

        fun startCapturing() {
            _isSaving.value = true
        }

        fun finishCapturing() {
            _isSaving.value = false
        }

        fun emitCaptureError(message: String) {
            viewModelScope.launch {
                _events.emit(CameraEvent.CaptureError(message))
            }
        }

        fun saveBeforePhoto(
            tempUri: String,
            zoomLevel: Float,
        ) {
            val currentRatio: AspectRatio = cameraSettings.state.value.aspectRatio
            viewModelScope.launch {
                try {
                    val pairId =
                        if (replaceBeforeForPairId != null) {
                            photoPairRepository.replaceBeforePhoto(
                                pairId = replaceBeforeForPairId,
                                tempFileUri = tempUri,
                            )
                            replaceBeforeForPairId
                        } else {
                            photoPairRepository.saveBeforePhoto(
                                tempFileUri = tempUri,
                                zoomLevel = zoomLevel,
                                albumId = albumId,
                                aspectRatio = currentRatio,
                            )
                        }
                    _events.emit(CameraEvent.PhotoSaved(pairId))
                } catch (e: IOException) {
                    _events.emit(CameraEvent.SaveError(e.message ?: "save failed"))
                } catch (e: SecurityException) {
                    _events.emit(CameraEvent.SaveError(e.message ?: "save failed"))
                } catch (e: SQLiteException) {
                    _events.emit(CameraEvent.SaveError(e.message ?: "save failed"))
                } catch (e: IllegalStateException) {
                    _events.emit(CameraEvent.SaveError(e.message ?: "save failed"))
                } catch (e: IllegalArgumentException) {
                    _events.emit(CameraEvent.SaveError(e.message ?: "save failed"))
                } finally {
                    _isSaving.value = false
                }
            }
        }

        fun toggleLensFacing(): LensFacing {
            val next = if (_lensFacing.value == LensFacing.BACK) LensFacing.FRONT else LensFacing.BACK
            _lensFacing.value = next
            zoomHolder.resetZoomForLensSwitch()
            return next
        }

        fun observeBeforeUris() {
            if (observePairsJob?.isActive == true) return
            observePairsJob =
                viewModelScope.launch {
                    photoPairRepository.observeAll().collect { pairs ->
                        _beforePreviewUris.value =
                            pairs
                                .filter { it.beforeTimestamp >= sessionStartTimestamp }
                                .mapNotNull { it.beforePhotoUri }
                    }
                }
        }

        fun toggleGrid() = cameraSettings.toggleGrid(viewModelScope)

        fun toggleLevel() = cameraSettings.toggleLevel(viewModelScope)

        fun cycleFlash(): FlashMode = cameraSettings.cycleFlash(viewModelScope)

        fun toggleNightMode(): Boolean = cameraSettings.toggleNightMode(viewModelScope)

        fun toggleHdr(): Boolean = cameraSettings.toggleHdr(viewModelScope)

        fun cycleAspectRatio(): AspectRatio? = cameraSettings.cycleAspectRatio(viewModelScope)

        fun setExposureIndex(index: Int) = cameraSettings.setExposureIndex(index)

        fun toggleSettingsPanel() = cameraSettings.toggleSettingsPanel()

        fun dismissSettingsPanel() = cameraSettings.dismissSettingsPanel()

        companion object {
            private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        }
    }
