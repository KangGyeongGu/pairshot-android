package com.pairshot.feature.camera.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pairshot.core.designsystem.PairShotCameraTokens
import com.pairshot.core.designsystem.PairShotAppBar
import com.pairshot.core.designsystem.PairShotRadius
import com.pairshot.core.designsystem.PairShotStroke
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.spec.CameraSpec
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val FOCUS_RING_SIZE = PairShotAppBar.height
private val FOCUS_RING_STROKE = PairShotStroke.hairline
private val EV_BAR_HEIGHT = CameraSpec.evBarHeight
private val EV_BAR_WIDTH = CameraSpec.evBarWidth
private val EV_SUN_SIZE = PairShotSpacing.md
private val EV_BAR_GAP = PairShotSpacing.lg
private val EV_HIT_AREA_LEFT_EXPANSION = CameraSpec.evHitAreaLeftExpansion
private val EV_HIT_AREA_RIGHT_EXPANSION = CameraSpec.evHitAreaRightExpansion
private val EV_HIT_AREA_VERTICAL_EXPANSION = CameraSpec.evHitAreaVerticalExpansion
private val EV_TEXT_TOP_SPACING = PairShotRadius.sm
private val SUN_OUTLINE_STROKE = PairShotStroke.hairline
private const val DRAG_DP_PER_EV_STEP = 30f
private const val AUTO_HIDE_DELAY_MS = 2000L
private const val RING_FADE_OUT_MS = 400
private const val RING_ALPHA_IN_MS = 150
private const val RING_SCALE_IN_MS = 200
private const val RING_START_SCALE = 1.3f
private const val DRAG_DETECTION_THRESHOLD_PX = 3f
private const val MULTI_TOUCH_THRESHOLD = 2
private const val EV_BAR_ALPHA_FACTOR = 0.7f
private const val EV_TEXT_VISIBILITY_THRESHOLD = 0.3f
private const val EV_TEXT_FONT_SIZE_SP = 12
private const val HALF = 2f

private sealed interface InitialGestureResult {
    data object Cancelled : InitialGestureResult

    data class Completed(
        val isDrag: Boolean,
    ) : InitialGestureResult
}

private suspend fun AwaitPointerEventScope.detectInitialGesture(): InitialGestureResult {
    var isDrag = false
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.size >= MULTI_TOUCH_THRESHOLD) return InitialGestureResult.Cancelled
        val change = event.changes.firstOrNull() ?: return InitialGestureResult.Completed(isDrag)
        if (event.type == PointerEventType.Release || !change.pressed) {
            return InitialGestureResult.Completed(isDrag)
        }
        val delta = change.position - change.previousPosition
        if (abs(delta.x) > DRAG_DETECTION_THRESHOLD_PX || abs(delta.y) > DRAG_DETECTION_THRESHOLD_PX) {
            isDrag = true
        }
    }
}

private suspend fun AwaitPointerEventScope.trackEvDrag(
    dragThresholdPx: Float,
    dragStartEvIndex: Int,
    exposureIndexMin: Int,
    exposureIndexMax: Int,
    currentEvIndex: () -> Int,
    onEvIndexChanged: (Int) -> Unit,
) {
    var totalDragY = 0f
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.size >= MULTI_TOUCH_THRESHOLD) return
        val change = event.changes.firstOrNull() ?: return
        if (event.type == PointerEventType.Release || !change.pressed) return

        totalDragY += (change.position - change.previousPosition).y
        val evSteps = -(totalDragY / dragThresholdPx).roundToInt()
        val newIndex =
            (dragStartEvIndex + evSteps)
                .coerceIn(exposureIndexMin, exposureIndexMax)
        if (newIndex != currentEvIndex()) {
            onEvIndexChanged(newIndex)
        }
        change.consume()
    }
}

@Composable
fun FocusExposureOverlay(
    onTapToFocus: (x: Float, y: Float, viewWidth: Int, viewHeight: Int) -> Unit,
    onExposureReset: () -> Unit,
    onExposureAdjust: (newIndex: Int) -> Unit,
    exposureIndexMin: Int,
    exposureIndexMax: Int,
    currentExposureIndex: Int,
    exposureStepNumerator: Int,
    exposureStepDenominator: Int,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var focusPosition by remember { mutableStateOf<Offset?>(null) }
    val ringAlpha = remember { Animatable(0f) }
    val ringScale = remember { Animatable(RING_START_SCALE) }

    var showEvBar by remember { mutableStateOf(false) }
    var localEvIndex by remember { mutableIntStateOf(currentExposureIndex) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    var dragStartEvIndex by remember { mutableIntStateOf(currentExposureIndex) }

    var autoHideJob by remember { mutableStateOf<Job?>(null) }

    val exposureEnabled = exposureIndexMin < exposureIndexMax
    val dragThresholdPx = with(density) { DRAG_DP_PER_EV_STEP.dp.toPx() }
    val focusRingSizePx = with(density) { FOCUS_RING_SIZE.toPx() }
    val focusRingStrokePx = with(density) { FOCUS_RING_STROKE.toPx() }

    fun scheduleAutoHide() {
        autoHideJob?.cancel()
        autoHideJob =
            scope.launch {
                delay(AUTO_HIDE_DELAY_MS)
                showEvBar = false
                ringAlpha.animateTo(0f, animationSpec = tween(RING_FADE_OUT_MS))
                focusPosition = null
            }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .pointerInput(exposureIndexMin, exposureIndexMax, currentExposureIndex) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val tapPosition = down.position

                        val gesture = detectInitialGesture()
                        if (gesture is InitialGestureResult.Cancelled) return@awaitEachGesture
                        val isDrag = (gesture as InitialGestureResult.Completed).isDrag

                        if (!isDrag) {
                            autoHideJob?.cancel()
                            focusPosition = tapPosition
                            localEvIndex = 0
                            dragStartEvIndex = 0
                            totalDragY = 0f
                            onExposureReset()

                            scope.launch {
                                ringAlpha.snapTo(0f)
                                ringScale.snapTo(RING_START_SCALE)
                                ringAlpha.animateTo(1f, animationSpec = tween(RING_ALPHA_IN_MS))
                                ringScale.animateTo(1f, animationSpec = tween(RING_SCALE_IN_MS))
                            }
                            if (exposureEnabled) {
                                showEvBar = true
                            }
                            onTapToFocus(tapPosition.x, tapPosition.y, size.width, size.height)
                            scheduleAutoHide()
                        }
                    }
                }.pointerInput(showEvBar, exposureIndexMin, exposureIndexMax) {
                    if (!showEvBar || !exposureEnabled) return@pointerInput

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val focusPos = focusPosition ?: return@awaitEachGesture

                        val ringRadius = focusRingSizePx / HALF
                        val barX = focusPos.x + ringRadius + with(density) { EV_BAR_GAP.toPx() }
                        val hitAreaLeft =
                            focusPos.x - ringRadius - with(density) { EV_HIT_AREA_LEFT_EXPANSION.toPx() }
                        val hitAreaRight = barX + with(density) { EV_HIT_AREA_RIGHT_EXPANSION.toPx() }
                        val hitAreaTop =
                            focusPos.y -
                                with(density) { EV_BAR_HEIGHT.toPx() / HALF + EV_HIT_AREA_VERTICAL_EXPANSION.toPx() }
                        val hitAreaBottom =
                            focusPos.y +
                                with(density) { EV_BAR_HEIGHT.toPx() / HALF + EV_HIT_AREA_VERTICAL_EXPANSION.toPx() }

                        val inHitArea =
                            down.position.x in hitAreaLeft..hitAreaRight &&
                                down.position.y in hitAreaTop..hitAreaBottom

                        if (!inHitArea) return@awaitEachGesture

                        autoHideJob?.cancel()
                        dragStartEvIndex = localEvIndex
                        totalDragY = 0f

                        trackEvDrag(
                            dragThresholdPx = dragThresholdPx,
                            dragStartEvIndex = dragStartEvIndex,
                            exposureIndexMin = exposureIndexMin,
                            exposureIndexMax = exposureIndexMax,
                            currentEvIndex = { localEvIndex },
                            onEvIndexChanged = { newIndex ->
                                localEvIndex = newIndex
                                onExposureAdjust(newIndex)
                            },
                        )

                        scheduleAutoHide()
                    }
                },
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val focusPos = focusPosition
        if (focusPos != null && ringAlpha.value > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val alpha = ringAlpha.value
                val scale = ringScale.value
                val radius = (focusRingSizePx / HALF) * scale

                drawCircle(
                    color = PairShotCameraTokens.Foreground.copy(alpha = alpha),
                    radius = radius,
                    center = focusPos,
                    style = Stroke(width = focusRingStrokePx),
                )

                if (showEvBar && exposureEnabled) {
                    val barHeightPx = with(density) { EV_BAR_HEIGHT.toPx() }
                    val barWidthPx = with(density) { EV_BAR_WIDTH.toPx() }
                    val sunSizePx = with(density) { EV_SUN_SIZE.toPx() }

                    val barX = focusPos.x + radius + with(density) { EV_BAR_GAP.toPx() }
                    val barTop = focusPos.y - barHeightPx / HALF
                    val barBottom = focusPos.y + barHeightPx / HALF

                    drawLine(
                        color = PairShotCameraTokens.Foreground.copy(alpha = alpha * EV_BAR_ALPHA_FACTOR),
                        start = Offset(barX, barTop),
                        end = Offset(barX, barBottom),
                        strokeWidth = barWidthPx,
                    )

                    val evRangeSize = (exposureIndexMax - exposureIndexMin).coerceAtLeast(1)
                    val evFraction = (localEvIndex - exposureIndexMin).toFloat() / evRangeSize
                    val sunY = barBottom - (evFraction * barHeightPx)

                    drawCircle(
                        color = primaryColor.copy(alpha = alpha),
                        radius = sunSizePx / HALF,
                        center = Offset(barX, sunY),
                    )
                    drawCircle(
                        color = PairShotCameraTokens.Foreground.copy(alpha = alpha),
                        radius = sunSizePx / HALF,
                        center = Offset(barX, sunY),
                        style = Stroke(width = with(density) { SUN_OUTLINE_STROKE.toPx() }),
                    )
                }
            }

            if (showEvBar && exposureEnabled && ringAlpha.value > EV_TEXT_VISIBILITY_THRESHOLD) {
                val evValue = localEvIndex * (exposureStepNumerator.toFloat() / exposureStepDenominator.toFloat())
                val evText =
                    when {
                        evValue > 0f -> "EV +%.1f".format(evValue)
                        evValue < 0f -> "EV %.1f".format(evValue)
                        else -> "EV 0"
                    }
                val ringRadiusDp = with(density) { ((focusRingSizePx / HALF) * ringScale.value).toDp() }
                val textOffsetX = with(density) { focusPos.x.toDp() } - ringRadiusDp
                val textOffsetY = with(density) { focusPos.y.toDp() } + ringRadiusDp + EV_TEXT_TOP_SPACING

                Text(
                    text = evText,
                    color = PairShotCameraTokens.Foreground.copy(alpha = ringAlpha.value),
                    fontSize = EV_TEXT_FONT_SIZE_SP.sp,
                    modifier =
                        Modifier.offset {
                            IntOffset(
                                x = textOffsetX.roundToPx(),
                                y = textOffsetY.roundToPx(),
                            )
                        },
                )
            }
        }
    }
}
