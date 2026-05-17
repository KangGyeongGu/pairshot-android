package com.pairshot.feature.camera.screen

import androidx.camera.core.SurfaceRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pairshot.core.adsui.component.PairShotBannerAd
import com.pairshot.core.designsystem.PairShotCameraTokens
import com.pairshot.core.designsystem.PairShotSnackbarTokens
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.spec.CameraSpec
import com.pairshot.core.model.CameraCapabilities
import com.pairshot.core.ui.component.PairShotSnackbarController
import com.pairshot.core.ui.component.PairShotSnackbarHost
import com.pairshot.feature.camera.chrome.CameraBottomBar
import com.pairshot.feature.camera.component.BeforePreviewStrip
import com.pairshot.feature.camera.component.BeforeStripHeight
import com.pairshot.feature.camera.component.CameraSettingsSheet
import com.pairshot.feature.camera.component.ZoomUiState
import com.pairshot.feature.camera.state.CameraSettingsState
import com.pairshot.feature.tutorial.ui.modifier.tutorialAnchor

val CameraShutterSectionHeight = CameraSpec.shutterSectionHeight
val CameraBottomSpacer = CameraSpec.bottomSpacer

data class CameraScreenCallbacks(
    val onZoomRatioChanged: (Float) -> Unit,
    val onPresetTapped: (Float) -> Unit,
    val onDragEnd: () -> Unit,
    val onExposureReset: () -> Unit,
    val onExposureAdjust: (Int) -> Unit,
    val onTapToFocus: (Float, Float, Int, Int) -> Unit,
    val onToggleLens: () -> Unit,
    val onToggleSettings: () -> Unit,
    val onShutter: () -> Unit,
    val onThumbnailClick: () -> Unit,
    val onToggleGrid: () -> Unit,
    val onCycleFlash: () -> Unit,
    val onToggleNightMode: () -> Unit,
    val onToggleHdr: () -> Unit,
    val onToggleLevel: () -> Unit,
    val onCycleAspectRatio: () -> Unit,
    val onDismissSettings: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CameraScreenContent(
    surfaceRequest: SurfaceRequest?,
    zoomUiState: ZoomUiState,
    isSaving: Boolean,
    settingsState: CameraSettingsState,
    capabilities: CameraCapabilities,
    roll: Float,
    blackoutAlpha: Float,
    beforePreviewUris: List<String>,
    callbacks: CameraScreenCallbacks,
    snackbarController: PairShotSnackbarController = remember { PairShotSnackbarController() },
    thumbnailListState: LazyListState = rememberLazyListState(),
) {
    Box(modifier = Modifier.fillMaxSize().background(PairShotCameraTokens.Letterbox)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                com.pairshot.feature.camera.preview.CameraPreviewPane(
                    surfaceRequest = surfaceRequest,
                    zoomUiState = zoomUiState,
                    blackoutAlpha = blackoutAlpha,
                    gridEnabled = settingsState.gridEnabled,
                    levelEnabled = settingsState.levelEnabled,
                    roll = roll,
                    exposureIndexMin = capabilities.exposureIndexMin,
                    exposureIndexMax = capabilities.exposureIndexMax,
                    currentExposureIndex = settingsState.exposureIndex,
                    exposureStepNumerator = capabilities.exposureStepNumerator,
                    exposureStepDenominator = capabilities.exposureStepDenominator,
                    selectedAspectRatio = settingsState.aspectRatio,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .tutorialAnchor(com.pairshot.core.domain.tutorial.AnchorKey.CAMERA_PREVIEW),
                    onZoomRatioChanged = callbacks.onZoomRatioChanged,
                    onPresetTapped = callbacks.onPresetTapped,
                    onDragEnd = callbacks.onDragEnd,
                    onExposureReset = callbacks.onExposureReset,
                    onExposureAdjust = callbacks.onExposureAdjust,
                    onTapToFocus = callbacks.onTapToFocus,
                    onToggleLens = callbacks.onToggleLens,
                )

                BeforePreviewStrip(
                    beforePreviewUris = beforePreviewUris,
                    modifier = Modifier.height(BeforeStripHeight),
                    listState = thumbnailListState,
                    stripHeight = BeforeStripHeight,
                    allActiveSize = true,
                )

                CameraBottomBar(
                    isSaving = isSaving,
                    shutterEnabled = true,
                    height = CameraShutterSectionHeight,
                    onToggleSettings = callbacks.onToggleSettings,
                    onShutterClick = callbacks.onShutter,
                    onThumbnailClick = callbacks.onThumbnailClick,
                )

                Spacer(modifier = Modifier.height(CameraBottomSpacer))
            }

            PairShotBannerAd(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
            )

            CameraSettingsSheet(
                visible = settingsState.showPanel,
                settingsState = settingsState,
                capabilities = capabilities,
                onToggleGrid = callbacks.onToggleGrid,
                onCycleFlash = callbacks.onCycleFlash,
                onToggleNightMode = callbacks.onToggleNightMode,
                onToggleHdr = callbacks.onToggleHdr,
                onToggleLevel = callbacks.onToggleLevel,
                onCycleAspectRatio = callbacks.onCycleAspectRatio,
                onDismiss = callbacks.onDismissSettings,
            )

            PairShotSnackbarHost(
                controller = snackbarController,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
                        .padding(top = PairShotSnackbarTokens.topOffset),
            )
        }
    }
}
