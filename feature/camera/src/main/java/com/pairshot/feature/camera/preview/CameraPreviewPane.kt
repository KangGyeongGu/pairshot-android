package com.pairshot.feature.camera.preview

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.PairShotCameraTokens
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.model.AspectRatio
import com.pairshot.feature.camera.component.CameraOverlayLayer
import com.pairshot.feature.camera.component.FocusExposureOverlay
import com.pairshot.feature.camera.component.ZoomControls
import com.pairshot.feature.camera.component.ZoomUiState

private const val FALLBACK_ASPECT_WIDTH = 3f
private const val FALLBACK_ASPECT_HEIGHT = 4f
private const val PORTRAIT_RATIO_4_3 = 0.75f
private const val PORTRAIT_RATIO_16_9 = 0.5625f
private const val PORTRAIT_RATIO_1_1 = 1f

private fun AspectRatio.toPortraitDisplayRatio(): Float =
    when (this) {
        AspectRatio.RATIO_4_3 -> PORTRAIT_RATIO_4_3
        AspectRatio.RATIO_16_9 -> PORTRAIT_RATIO_16_9
        AspectRatio.RATIO_1_1 -> PORTRAIT_RATIO_1_1
    }

@Composable
internal fun CameraPreviewPane(
    surfaceRequest: SurfaceRequest?,
    zoomUiState: ZoomUiState,
    blackoutAlpha: Float,
    gridEnabled: Boolean,
    levelEnabled: Boolean,
    roll: Float,
    exposureIndexMin: Int,
    exposureIndexMax: Int,
    currentExposureIndex: Int,
    exposureStepNumerator: Int,
    exposureStepDenominator: Int,
    modifier: Modifier = Modifier,
    selectedAspectRatio: AspectRatio? = null,
    onZoomRatioChanged: (Float) -> Unit,
    onPresetTapped: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onExposureReset: () -> Unit,
    onExposureAdjust: (Int) -> Unit,
    onTapToFocus: (x: Float, y: Float, viewWidth: Int, viewHeight: Int) -> Unit,
    onToggleLens: () -> Unit,
    overlayContent: (@Composable () -> Unit)? = null,
) {
    val latestZoomRatio = rememberUpdatedState(zoomUiState.currentRatio)

    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .pointerInput(zoomUiState.minRatio, zoomUiState.maxRatio) {
                detectTransformGestures { _, _, zoom, _ ->
                    val newRatio =
                        (latestZoomRatio.value * zoom)
                            .coerceIn(zoomUiState.minRatio, zoomUiState.maxRatio)
                    onZoomRatioChanged(newRatio)
                }
            },
    ) {
        surfaceRequest?.let { request ->
            Box {
                CameraXViewfinder(
                    surfaceRequest = request,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                if (blackoutAlpha > 0f) {
                    Box(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .background(PairShotCameraTokens.Letterbox.copy(alpha = blackoutAlpha)),
                    )
                }
            }
        }

        overlayContent?.invoke()

        BoxWithConstraints(
            modifier =
            Modifier
                .align(Alignment.Center)
                .fillMaxSize(),
        ) {
            val containerRatio =
                if (maxHeight.value > 0f) {
                    maxWidth.value / maxHeight.value
                } else {
                    FALLBACK_ASPECT_WIDTH / FALLBACK_ASPECT_HEIGHT
                }
            val requestedRatio =
                if (selectedAspectRatio != null) {
                    selectedAspectRatio.toPortraitDisplayRatio()
                } else {
                    val raw =
                        surfaceRequest?.resolution?.let { size ->
                            if (size.height > 0) {
                                size.width.toFloat() / size.height.toFloat()
                            } else {
                                containerRatio
                            }
                        } ?: containerRatio
                    when {
                        raw <= 0f -> containerRatio
                        (raw > 1f) != (containerRatio > 1f) -> 1f / raw
                        else -> raw
                    }
                }

            if (selectedAspectRatio == AspectRatio.RATIO_1_1) {
                val maskHeight = ((maxHeight - maxWidth) / 2).coerceAtLeast(0.dp)
                Box(
                    modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(maskHeight)
                        .background(PairShotCameraTokens.Letterbox),
                )
                Box(
                    modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(maskHeight)
                        .background(PairShotCameraTokens.Letterbox),
                )
            }

            val previewFrameModifier =
                if (containerRatio > requestedRatio) {
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(requestedRatio)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(requestedRatio)
                }

            Box(modifier = previewFrameModifier.align(Alignment.Center).clipToBounds()) {
                CameraOverlayLayer(
                    gridEnabled = gridEnabled,
                    levelEnabled = levelEnabled,
                    roll = roll,
                )

                FocusExposureOverlay(
                    onTapToFocus = onTapToFocus,
                    onExposureReset = onExposureReset,
                    onExposureAdjust = onExposureAdjust,
                    exposureIndexMin = exposureIndexMin,
                    exposureIndexMax = exposureIndexMax,
                    currentExposureIndex = currentExposureIndex,
                    exposureStepNumerator = exposureStepNumerator,
                    exposureStepDenominator = exposureStepDenominator,
                    modifier = Modifier.fillMaxSize(),
                )

                ZoomControls(
                    zoomUiState = zoomUiState,
                    onZoomRatioChanged = onZoomRatioChanged,
                    onPresetTapped = onPresetTapped,
                    onDragEnd = onDragEnd,
                    onToggleLens = onToggleLens,
                    modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = PairShotSpacing.md),
                )
            }
        }
    }
}
