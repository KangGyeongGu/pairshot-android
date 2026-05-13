package com.pairshot.feature.camera.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.core.ui.R
import com.pairshot.core.ui.component.PairShotSnackbarController
import com.pairshot.core.ui.component.SnackbarEvent
import com.pairshot.core.ui.component.SnackbarVariant
import com.pairshot.core.ui.text.UiText
import com.pairshot.feature.camera.component.ImmersiveCameraEffect
import com.pairshot.feature.camera.viewmodel.CameraEvent
import com.pairshot.feature.camera.viewmodel.CameraSessionViewModel
import com.pairshot.feature.camera.viewmodel.CameraViewModel
import kotlinx.coroutines.launch

@Composable
internal fun CameraScreen(
    viewModel: CameraViewModel,
    onNavigateBack: () -> Unit,
    sessionViewModel: CameraSessionViewModel = hiltViewModel(),
) {
    ImmersiveCameraEffect()

    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val cameraSession = sessionViewModel.cameraSession
    val sensorSession = sessionViewModel.sensorSession

    val beforePreviewUris by viewModel.beforePreviewUris.collectAsStateWithLifecycle()
    val lastPairThumbnailUri by viewModel.lastPairThumbnailUri.collectAsStateWithLifecycle()
    val zoomUiState by viewModel.zoomUiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val capabilities by cameraSession.capabilities.collectAsStateWithLifecycle()
    val roll by sensorSession.roll.collectAsStateWithLifecycle()
    val surfaceRequest by cameraSession.surfaceRequest.collectAsStateWithLifecycle()

    val snackbarController = remember { PairShotSnackbarController() }
    val thumbnailListState = rememberLazyListState()

    LaunchedEffect(lifecycleOwner) {
        val initial = viewModel.loadInitialSettings()
        cameraSession.setFlash(initial.flashMode)
        cameraSession.setNightMode(initial.nightModeEnabled)
        cameraSession.setHdrMode(initial.hdrEnabled)
        cameraSession.setAspectRatio(initial.aspectRatio)
        sensorSession.bind(lifecycleOwner)
        cameraSession.bind(lifecycleOwner)
    }

    LaunchedEffect(cameraSession) {
        cameraSession.zoomState.collect { zoom ->
            viewModel.onCameraZoomCapabilities(zoom.min, zoom.max)
        }
    }

    LaunchedEffect(capabilities) {
        val adjustment = viewModel.adjustForCapabilities(capabilities)
        adjustment.flashMode?.let { cameraSession.setFlash(it) }
        adjustment.nightModeEnabled?.let { cameraSession.setNightMode(it) }
        adjustment.hdrEnabled?.let { cameraSession.setHdrMode(it) }
    }

    var showBlackout by remember { mutableStateOf(false) }
    val blackoutAlpha by animateFloatAsState(
        targetValue = if (showBlackout) 0.6f else 0f,
        animationSpec = tween(durationMillis = if (showBlackout) 30 else 100),
        label = "capture_blackout",
        finishedListener = { if (showBlackout) showBlackout = false },
    )

    LaunchedEffect(Unit) {
        viewModel.observeBeforeUris()
    }

    val isReplaceBeforeMode = viewModel.replaceBeforeForPairId != null

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CameraEvent.PhotoSaved -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isReplaceBeforeMode) {
                        onNavigateBack()
                    }
                }

                is CameraEvent.CaptureError -> {
                    snackbarController.show(
                        SnackbarEvent(
                            UiText.Resource(R.string.snackbar_error_capture_failed),
                            SnackbarVariant.ERROR,
                        ),
                    )
                }

                is CameraEvent.SaveError -> {
                    snackbarController.show(
                        SnackbarEvent(
                            UiText.Resource(R.string.snackbar_error_unknown),
                            SnackbarVariant.ERROR,
                        ),
                    )
                }
            }
        }
    }

    LaunchedEffect(beforePreviewUris.size) {
        if (beforePreviewUris.isNotEmpty()) {
            thumbnailListState.animateScrollToItem(beforePreviewUris.lastIndex)
        }
    }

    val callbacks =
        CameraScreenCallbacks(
            onZoomRatioChanged = { newRatio ->
                viewModel.updateZoomRatio(newRatio)
                cameraSession.setZoom(newRatio)
            },
            onPresetTapped = { preset ->
                viewModel.onPresetTapped(preset)
                cameraSession.setZoom(viewModel.zoomUiState.value.currentRatio)
            },
            onDragEnd = { viewModel.applyCustomRatio() },
            onExposureReset = {
                viewModel.setExposureIndex(0)
                cameraSession.setExposureIndex(0)
            },
            onExposureAdjust = { index ->
                viewModel.setExposureIndex(index)
                cameraSession.setExposureIndex(index)
            },
            onTapToFocus = { x, y, w, h ->
                cameraSession.startFocusAndMetering(x, y, w.toFloat(), h.toFloat())
            },
            onToggleLens = {
                val next = viewModel.toggleLensFacing()
                cameraSession.setLensFacing(next)
                cameraSession.setZoom(viewModel.zoomUiState.value.currentRatio)
            },
            onToggleSettings = { viewModel.toggleSettingsPanel() },
            onShutter = {
                if (isSaving) return@CameraScreenCallbacks
                showBlackout = true
                scope.launch {
                    viewModel.startCapturing()
                    val captureResult = cameraSession.capture()
                    val tempUri = captureResult.getOrNull()
                    if (captureResult.isFailure || tempUri == null) {
                        viewModel.emitCaptureError(
                            captureResult.exceptionOrNull()?.message ?: "capture failed",
                        )
                        viewModel.finishCapturing()
                        return@launch
                    }
                    viewModel.saveBeforePhoto(
                        tempUri = tempUri,
                        zoomLevel = viewModel.zoomUiState.value.currentRatio,
                    )
                }
            },
            onThumbnailClick = onNavigateBack,
            onToggleGrid = viewModel::toggleGrid,
            onCycleFlash = {
                val next = viewModel.cycleFlash()
                cameraSession.setFlash(next)
            },
            onToggleNightMode = {
                val next = viewModel.toggleNightMode()
                cameraSession.setNightMode(next)
                if (next) cameraSession.setHdrMode(false)
            },
            onToggleHdr = {
                val next = viewModel.toggleHdr()
                cameraSession.setHdrMode(next)
                if (next) cameraSession.setNightMode(false)
            },
            onToggleLevel = viewModel::toggleLevel,
            onCycleAspectRatio = {
                val next = viewModel.cycleAspectRatio()
                if (next != null) cameraSession.setAspectRatio(next)
            },
            onDismissSettings = viewModel::dismissSettingsPanel,
        )

    CameraScreenContent(
        surfaceRequest = surfaceRequest,
        zoomUiState = zoomUiState,
        isSaving = isSaving,
        settingsState = settingsState,
        capabilities = capabilities,
        roll = roll,
        blackoutAlpha = blackoutAlpha,
        beforePreviewUris = beforePreviewUris,
        lastPairThumbnailUri = lastPairThumbnailUri,
        callbacks = callbacks,
        snackbarController = snackbarController,
        thumbnailListState = thumbnailListState,
    )
}
