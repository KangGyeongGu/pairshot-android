package com.pairshot.feature.tutorial.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.core.designsystem.ProvideAppTextScaleDensity
import com.pairshot.core.domain.tutorial.AnchorBounds
import com.pairshot.feature.tutorial.R
import com.pairshot.feature.tutorial.domain.AdvanceCondition
import com.pairshot.feature.tutorial.domain.TutorialAnchorRegistry
import com.pairshot.feature.tutorial.domain.TutorialCoordinator
import com.pairshot.feature.tutorial.domain.TutorialStepDef
import com.pairshot.feature.tutorial.domain.TutorialStepDefinitions
import com.pairshot.feature.tutorial.domain.TutorialStepId
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val SPOTLIGHT_PADDING_PX = 12f
private const val SPOTLIGHT_CORNER_RADIUS_PX = 24f
private const val SPOTLIGHT_STROKE_WIDTH_PX = 6f

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TutorialOverlayEntryPoint {
    fun tutorialCoordinator(): TutorialCoordinator

    fun tutorialAnchorRegistry(): TutorialAnchorRegistry
}

@Composable
fun TutorialOverlay() {
    val context = LocalContext.current
    val entryPoint =
        remember(context) {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                TutorialOverlayEntryPoint::class.java,
            )
        }
    val coordinator = remember(entryPoint) { entryPoint.tutorialCoordinator() }
    val registry = remember(entryPoint) { entryPoint.tutorialAnchorRegistry() }

    val currentStep by coordinator.currentStep.collectAsStateWithLifecycle()
    val anchors by registry.bounds.collectAsStateWithLifecycle()

    val def =
        currentStep
            ?.takeIf { it != TutorialStepId.DONE }
            ?.let { TutorialStepDefinitions.get(it) }
            ?: return
    val anchorBounds = def.anchor?.let { anchors[it] }
    val actionAnchorBounds = def.actionAnchor?.let { anchors[it] }
    val anchorMissing = def.anchor != null && anchorBounds == null
    val actionAnchorMissing = def.actionAnchor != null && actionAnchorBounds == null
    if (anchorMissing || actionAnchorMissing) return

    OverlayContent(
        def = def,
        anchorBounds = anchorBounds,
        actionAnchorBounds = actionAnchorBounds,
        onTapAnywhere = { coordinator.onTapAnywhere() },
        onAdvanceManual = { coordinator.advanceManual() },
        onSkip = { coordinator.skip() },
    )
}

@Composable
private fun OverlayContent(
    def: TutorialStepDef,
    anchorBounds: AnchorBounds?,
    actionAnchorBounds: AnchorBounds?,
    onTapAnywhere: () -> Unit,
    onAdvanceManual: () -> Unit,
    onSkip: () -> Unit,
) {
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var showSkipConfirm by remember { mutableStateOf(false) }
    val isTapToAdvance = def.advance is AdvanceCondition.TapAnywhere
    val onNext: () -> Unit =
        when (def.advance) {
            is AdvanceCondition.TapAnywhere -> onTapAnywhere
            is AdvanceCondition.Manual -> onAdvanceManual
            is AdvanceCondition.UserAction -> onAdvanceManual
        }

    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .onSizeChanged { overlaySize = it },
    ) {
        val spotlightStrokeColor = Color.White
        Canvas(
            modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            drawRect(color = Color.Black.copy(alpha = def.dimAlpha))
            if (anchorBounds != null) {
                val anchorStroke = if (def.strokeAnchor) spotlightStrokeColor else null
                drawSpotlight(anchorBounds, anchorStroke)
            }
            if (actionAnchorBounds != null && actionAnchorBounds !== anchorBounds) {
                drawSpotlight(actionAnchorBounds, spotlightStrokeColor)
            }
        }

        if (isTapToAdvance) {
            Box(
                modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(def.id) {
                        detectTapGestures { onTapAnywhere() }
                    },
            )
        } else if (def.advance is AdvanceCondition.UserAction && overlaySize != IntSize.Zero) {
            val allowed = persistentListOf<AnchorBounds>().builder().apply {
                anchorBounds?.let { add(it) }
                actionAnchorBounds?.let { add(it) }
            }.build()
            if (allowed.isNotEmpty()) {
                SpotlightTouchBlocker(
                    allowedAnchors = allowed,
                    overlaySize = overlaySize,
                    stepId = def.id,
                )
            }
        }

        if (def.isVisible) {
            TutorialPopupBalloon(
                anchorBounds = anchorBounds,
                actionAnchorBounds = actionAnchorBounds,
                def = def,
                onSkip = { showSkipConfirm = true },
                onNext = onNext,
            )
        }

        if (showSkipConfirm) {
            SkipConfirmDialog(
                onConfirm = {
                    showSkipConfirm = false
                    onSkip()
                },
                onDismiss = { showSkipConfirm = false },
            )
        }
    }
}

@Composable
private fun SkipConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            ProvideAppTextScaleDensity {
                androidx.compose.material3.Text(text = stringResource(R.string.tutorial_skip_confirm_title))
            }
        },
        text = {
            ProvideAppTextScaleDensity {
                androidx.compose.material3.Text(text = stringResource(R.string.tutorial_skip_confirm_body))
            }
        },
        confirmButton = {
            ProvideAppTextScaleDensity {
                androidx.compose.material3.TextButton(onClick = onConfirm) {
                    androidx.compose.material3.Text(
                        text = stringResource(R.string.tutorial_skip_confirm_yes),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        dismissButton = {
            ProvideAppTextScaleDensity {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    androidx.compose.material3.Text(text = stringResource(R.string.tutorial_skip_confirm_no))
                }
            }
        },
    )
}

private fun DrawScope.drawSpotlight(
    bounds: AnchorBounds,
    strokeColor: Color?,
) {
    val padding = SPOTLIGHT_PADDING_PX
    val cornerRadius = CornerRadius(SPOTLIGHT_CORNER_RADIUS_PX, SPOTLIGHT_CORNER_RADIUS_PX)
    drawRoundRect(
        color = Color.Transparent,
        topLeft = Offset(bounds.left - padding, bounds.top - padding),
        size = Size(bounds.width + padding * 2, bounds.height + padding * 2),
        cornerRadius = cornerRadius,
        blendMode = BlendMode.Clear,
    )
    if (strokeColor != null) {
        val halfStroke = SPOTLIGHT_STROKE_WIDTH_PX / 2f
        val strokeLeft = (bounds.left - padding).coerceAtLeast(halfStroke)
        val strokeTop = (bounds.top - padding).coerceAtLeast(halfStroke)
        val strokeRight = (bounds.left + bounds.width + padding).coerceAtMost(size.width - halfStroke)
        val strokeBottom = (bounds.top + bounds.height + padding).coerceAtMost(size.height - halfStroke)
        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(strokeLeft, strokeTop),
            size = Size(strokeRight - strokeLeft, strokeBottom - strokeTop),
            cornerRadius = cornerRadius,
            style = Stroke(width = SPOTLIGHT_STROKE_WIDTH_PX),
        )
    }
}

private data class TouchRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    fun isEmpty(): Boolean = right <= left || bottom <= top
}

private fun TouchRect.intersects(other: TouchRect): Boolean =
    left < other.right && right > other.left && top < other.bottom && bottom > other.top

private fun TouchRect.subtract(hole: TouchRect): List<TouchRect> {
    if (!intersects(hole)) return listOf(this)
    val result = mutableListOf<TouchRect>()
    if (top < hole.top) result += TouchRect(left, top, right, hole.top)
    if (hole.bottom < bottom) result += TouchRect(left, hole.bottom, right, bottom)
    val midTop = maxOf(top, hole.top)
    val midBottom = minOf(bottom, hole.bottom)
    if (midTop < midBottom) {
        if (left < hole.left) result += TouchRect(left, midTop, hole.left, midBottom)
        if (hole.right < right) result += TouchRect(hole.right, midTop, right, midBottom)
    }
    return result.filterNot { it.isEmpty() }
}

private fun AnchorBounds.toAllowedTouchRect(
    padding: Float,
    widthPx: Float,
    heightPx: Float,
): TouchRect =
    TouchRect(
        left = (left - padding).coerceIn(0f, widthPx),
        top = (top - padding).coerceIn(0f, heightPx),
        right = (left + width + padding).coerceIn(0f, widthPx),
        bottom = (top + height + padding).coerceIn(0f, heightPx),
    )

@Composable
private fun BoxScope.SpotlightTouchBlocker(
    allowedAnchors: ImmutableList<AnchorBounds>,
    overlaySize: IntSize,
    stepId: TutorialStepId,
) {
    val padding = SPOTLIGHT_PADDING_PX
    val density = LocalDensity.current
    val widthPx = overlaySize.width.toFloat()
    val heightPx = overlaySize.height.toFloat()

    val blockerRects =
        remember(allowedAnchors, widthPx, heightPx) {
            var rects = listOf(TouchRect(0f, 0f, widthPx, heightPx))
            allowedAnchors.forEach { anchor ->
                val hole = anchor.toAllowedTouchRect(padding, widthPx, heightPx)
                if (!hole.isEmpty()) {
                    rects = rects.flatMap { it.subtract(hole) }
                }
            }
            rects
        }

    blockerRects.forEachIndexed { index, rect ->
        val leftDp = with(density) { rect.left.toDp() }
        val topDp = with(density) { rect.top.toDp() }
        val widthDp = with(density) { (rect.right - rect.left).toDp() }
        val heightDp = with(density) { (rect.bottom - rect.top).toDp() }
        Box(
            modifier =
            Modifier
                .absoluteOffset(x = leftDp, y = topDp)
                .size(width = widthDp, height = heightDp)
                .blockAllTouches(stepId, index),
        )
    }
}

private fun Modifier.blockAllTouches(
    stepId: TutorialStepId,
    index: Int,
): Modifier =
    pointerInput(stepId, index) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent().changes.forEach { it.consume() }
            }
        }
    }
