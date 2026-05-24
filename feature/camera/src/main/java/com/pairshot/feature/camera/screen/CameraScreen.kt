package com.pairshot.feature.camera.screen

import android.view.Surface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.core.domain.pair.CanCreatePairUseCase
import com.pairshot.core.domain.tutorial.TutorialActionIds
import com.pairshot.core.navigation.PaywallTrigger
import com.pairshot.core.ui.R
import com.pairshot.core.ui.component.PairShotSnackbarController
import com.pairshot.core.ui.component.SnackbarEvent
import com.pairshot.core.ui.component.SnackbarVariant
import com.pairshot.core.ui.text.UiText
import com.pairshot.feature.camera.component.ImmersiveCameraEffect
import com.pairshot.feature.camera.viewmodel.CameraEvent
import com.pairshot.feature.camera.viewmodel.CameraSessionViewModel
import com.pairshot.feature.camera.viewmodel.CameraViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@Composable
internal fun CameraScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: (PaywallTrigger) -> Unit,
    viewModel: CameraViewModel = hiltViewModel(),
    sessionViewModel: CameraSessionViewModel = hiltViewModel(),
) {
    ImmersiveCameraEffect()

    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val cameraSession = sessionViewModel.cameraSession
    val sensorSession = sessionViewModel.sensorSession

    val beforePreviewUris by viewModel.beforePreviewUris.collectAsStateWithLifecycle()
    val zoomUiState by viewModel.zoomUiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val capabilities by cameraSession.capabilities.collectAsStateWithLifecycle()
    val roll by sensorSession.roll.collectAsStateWithLifecycle()
    val deviceOrientation by sensorSession.deviceOrientation.collectAsStateWithLifecycle()
    val surfaceRequest by cameraSession.surfaceRequest.collectAsStateWithLifecycle()

    val snackbarController = remember { PairShotSnackbarController() }
    val thumbnailListState = rememberLazyListState()
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    val currentOnNavigateToPaywall by rememberUpdatedState(onNavigateToPaywall)

    val context = LocalContext.current
    val tutorialActions =
        remember(context) {
            EntryPointAccessors
                .fromApplication(context.applicationContext, CameraScreenTutorialEntryPoint::class.java)
                .tutorialActionDispatcher()
        }

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
                        currentOnNavigateBack()
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
            onZoomRatioChange = { newRatio ->
                viewModel.updateZoomRatio(newRatio)
                cameraSession.setZoom(newRatio)
            },
            onPresetTap = { preset ->
                viewModel.onPresetTap(preset)
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
                scope.launch {
                    when (viewModel.canCreatePair()) {
                        is CanCreatePairUseCase.Result.LimitReached -> {
                            currentOnNavigateToPaywall(PaywallTrigger.DAILY_LIMIT)
                            return@launch
                        }

                        CanCreatePairUseCase.Result.Allowed -> {
                            Unit
                        }
                    }
                    showBlackout = true
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
                    val actionId =
                        when (deviceOrientation) {
                            Surface.ROTATION_90 -> TutorialActionIds.CAMERA_SHUTTER_LANDSCAPE_LEFT
                            Surface.ROTATION_270 -> TutorialActionIds.CAMERA_SHUTTER_LANDSCAPE_RIGHT
                            else -> TutorialActionIds.CAMERA_SHUTTER_PORTRAIT
                        }
                    tutorialActions.report(actionId)
                }
            },
            onThumbnailClick = {
                tutorialActions.report(TutorialActionIds.CAMERA_BACK_TO_HOME)
                currentOnNavigateBack()
            },
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
        beforePreviewUris = beforePreviewUris.toImmutableList(),
        callbacks = callbacks,
        snackbarController = snackbarController,
        thumbnailListState = thumbnailListState,
    )
}
