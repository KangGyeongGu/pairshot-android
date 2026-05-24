package com.pairshot.feature.camera.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.PairShotCameraTokens
import com.pairshot.core.designsystem.PairShotIconSize
import com.pairshot.core.designsystem.PairShotRadius
import com.pairshot.core.designsystem.PairShotScreen
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotStroke
import com.pairshot.core.designsystem.spec.CameraSpec
import com.pairshot.feature.camera.R
import kotlin.math.abs
import kotlin.math.roundToInt

private val LensButtonSize = CameraSpec.lensButtonSize
private val LensButtonIconSize = CameraSpec.lensButtonIconSize
private const val ZOOM_DRAG_MIN_DELTA = 0.05f
private const val ZOOM_DIAL_RANGE_SPAN_DP = 300f
private const val ZOOM_RANGE_MIN_SPAN = 0.01f
private const val ZOOM_TICKS_PER_UNIT = 10
private const val ZOOM_TICKS_PER_UNIT_F = 10f
private const val ZOOM_MAJOR_TICK_INTERVAL = 10

@Composable
fun ZoomControls(
    zoomUiState: ZoomUiState,
    onZoomRatioChange: (Float) -> Unit,
    onPresetTap: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleLens: (() -> Unit)? = null,
) {
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val latestRatio by rememberUpdatedState(zoomUiState.currentRatio)
    val latestOnZoomChanged by rememberUpdatedState(onZoomRatioChange)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)

    val rangeSpanPx = with(density) { ZOOM_DIAL_RANGE_SPAN_DP.dp.toPx() }
    val zoomRange = (zoomUiState.maxRatio - zoomUiState.minRatio).coerceAtLeast(ZOOM_RANGE_MIN_SPAN)
    val pxPerZoom = rangeSpanPx / zoomRange

    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    var lastTickIndex by remember { mutableIntStateOf((zoomUiState.currentRatio * ZOOM_TICKS_PER_UNIT).roundToInt()) }

    Box(
        modifier =
        modifier
            .pointerInput(zoomUiState.minRatio, zoomUiState.maxRatio) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragAccumulator = 0f
                        lastTickIndex = (latestRatio * ZOOM_TICKS_PER_UNIT).roundToInt()
                    },
                    onDragEnd = {
                        isDragging = false
                        latestOnDragEnd()
                    },
                    onDragCancel = {
                        isDragging = false
                        latestOnDragEnd()
                    },
                ) { _, dragAmount ->
                    dragAccumulator -= dragAmount
                    val deltaZoom = dragAccumulator / pxPerZoom
                    if (abs(deltaZoom) >= ZOOM_DRAG_MIN_DELTA) {
                        val newRatio =
                            (latestRatio + deltaZoom)
                                .coerceIn(zoomUiState.minRatio, zoomUiState.maxRatio)
                        latestOnZoomChanged(newRatio)
                        dragAccumulator = 0f

                        val newTickIndex = (newRatio * ZOOM_TICKS_PER_UNIT).roundToInt()
                        if (newTickIndex != lastTickIndex) {
                            val isMajorTick = newTickIndex % ZOOM_MAJOR_TICK_INTERVAL == 0
                            if (isMajorTick) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            lastTickIndex = newTickIndex
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = isDragging,
            transitionSpec = {
                if (targetState) {
                    (fadeIn() + slideInVertically { it / 2 })
                        .togetherWith(fadeOut() + slideOutVertically { -it / 2 })
                } else {
                    (fadeIn() + slideInVertically { -it / 2 })
                        .togetherWith(fadeOut() + slideOutVertically { it / 2 })
                }.using(SizeTransform(clip = false) { _, _ -> snap() })
            },
            contentAlignment = Alignment.Center,
            label = "zoom-control-switch",
        ) { dragging ->
            if (dragging) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    ZoomDialWithLabel(zoomUiState = zoomUiState, pxPerZoom = pxPerZoom)
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    ZoomPresetCard(
                        zoomUiState = zoomUiState,
                        onPresetTap = onPresetTap,
                    )
                }
            }
        }

        if (onToggleLens != null) {
            val toggleLens: () -> Unit = onToggleLens
            AnimatedVisibility(
                visible = !isDragging,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = PairShotSpacing.md),
            ) {
                Box(
                    modifier =
                    Modifier
                        .size(LensButtonSize)
                        .clip(CircleShape)
                        .background(PairShotCameraTokens.Letterbox.copy(alpha = 0.35f))
                        .clickable(onClick = toggleLens),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = stringResource(R.string.camera_desc_switch),
                        tint = PairShotCameraTokens.Foreground,
                        modifier = Modifier.size(LensButtonIconSize),
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomPresetCard(
    zoomUiState: ZoomUiState,
    onPresetTap: (Float) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val labelStyle = MaterialTheme.typography.labelMedium
    val shape = MaterialTheme.shapes.small

    val activePreset =
        zoomUiState.presetRatios
            .minByOrNull { abs(it - zoomUiState.currentRatio) }

    Row(
        modifier =
        Modifier
            .background(
                color = PairShotCameraTokens.Letterbox.copy(alpha = 0.35f),
                shape = shape,
            ).padding(horizontal = PairShotSpacing.xs, vertical = PairShotSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(PairShotStroke.thin),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        zoomUiState.presetRatios.forEach { preset ->
            val isActive = preset == activePreset
            val displayRatio = zoomUiState.customRatios[preset] ?: preset
            val bgColor = if (isActive) primary else Color.Transparent
            val textColor = if (isActive) onPrimary else PairShotCameraTokens.Foreground.copy(alpha = 0.7f)

            Box(
                modifier =
                Modifier
                    .height(PairShotIconSize.lg)
                    .background(color = bgColor, shape = shape)
                    .clickable { onPresetTap(preset) }
                    .padding(horizontal = PairShotSpacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = formatZoomLabel(displayRatio),
                    color = textColor,
                    style = labelStyle,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ZoomDialWithLabel(
    zoomUiState: ZoomUiState,
    pxPerZoom: Float,
) {
    val density = LocalDensity.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val labelStyle = MaterialTheme.typography.labelSmall

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PairShotStroke.thin),
    ) {
        Box(
            modifier =
            Modifier
                .background(
                    color = PairShotCameraTokens.Letterbox.copy(alpha = 0.35f),
                    shape = MaterialTheme.shapes.small,
                ).padding(horizontal = PairShotSpacing.md, vertical = PairShotSpacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formatZoomLabel(zoomUiState.currentRatio),
                color = PairShotCameraTokens.Foreground,
                style = labelStyle,
                fontWeight = FontWeight.Medium,
            )
        }

        Canvas(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(PairShotScreen.horizontalPadding),
        ) {
            val centerX = size.width / 2f
            val canvasH = size.height

            val visibleZoomSpan = size.width / pxPerZoom
            val visibleMin =
                (zoomUiState.currentRatio - visibleZoomSpan / 2)
                    .coerceAtLeast(zoomUiState.minRatio)
            val visibleMax =
                (zoomUiState.currentRatio + visibleZoomSpan / 2)
                    .coerceAtMost(zoomUiState.maxRatio)

            val startTick = (visibleMin * ZOOM_TICKS_PER_UNIT).toInt()
            val endTick = ((visibleMax * ZOOM_TICKS_PER_UNIT).toInt()) + 1
            for (i in startTick..endTick) {
                val tick = i / ZOOM_TICKS_PER_UNIT_F
                val offset = (tick - zoomUiState.currentRatio) * pxPerZoom
                val x = centerX + offset
                if (x < 0f || x > size.width) continue

                val isMajor = i % ZOOM_MAJOR_TICK_INTERVAL == 0
                val tickHeightPx =
                    if (isMajor) {
                        with(
                            density
                        ) { PairShotSpacing.md.toPx() }
                    } else {
                        with(density) { PairShotRadius.sm.toPx() }
                    }
                val tickWidthPx =
                    if (isMajor) {
                        with(
                            density
                        ) { PairShotStroke.thin.toPx() }
                    } else {
                        with(density) { PairShotStroke.hairline.toPx() }
                    }
                val tickColor = if (isMajor) {
                    PairShotCameraTokens.Foreground
                } else {
                    PairShotCameraTokens.Foreground.copy(
                        alpha = 0.5f
                    )
                }
                val topY = canvasH - tickHeightPx

                drawLine(
                    color = tickColor,
                    start = Offset(x, topY),
                    end = Offset(x, canvasH),
                    strokeWidth = tickWidthPx,
                )
            }

            val indicatorH = with(density) { PairShotSpacing.lg.toPx() }
            val indicatorW = with(density) { PairShotStroke.thin.toPx() }
            drawLine(
                color = primaryColor,
                start = Offset(centerX, canvasH - indicatorH),
                end = Offset(centerX, canvasH),
                strokeWidth = indicatorW,
            )
        }
    }
}

private fun formatZoomLabel(ratio: Float): String =
    when {
        ratio == ratio.roundToInt().toFloat() -> "${ratio.roundToInt()}x"
        else -> "${"%.1f".format(ratio)}x"
    }
