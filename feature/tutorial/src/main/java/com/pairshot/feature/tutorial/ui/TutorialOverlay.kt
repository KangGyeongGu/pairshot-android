package com.pairshot.feature.tutorial.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.core.domain.tutorial.AnchorBounds
import com.pairshot.feature.tutorial.R
import com.pairshot.feature.tutorial.domain.AdvanceCondition
import com.pairshot.feature.tutorial.domain.RotationHint
import com.pairshot.feature.tutorial.domain.TutorialAnchorRegistry
import com.pairshot.feature.tutorial.domain.TutorialCoordinator
import com.pairshot.feature.tutorial.domain.TutorialStepDef
import com.pairshot.feature.tutorial.domain.TutorialStepDefinitions
import com.pairshot.feature.tutorial.domain.TutorialStepId
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

private const val SPOTLIGHT_PADDING_PX = 12f
private const val SPOTLIGHT_CORNER_RADIUS_PX = 24f
private const val ROTATION_ANIM_DURATION_MS = 1200
private const val ROTATION_HINT_ICON_SIZE_DP = 56
private const val ROTATION_LEFT_ANGLE = -90f
private const val ROTATION_RIGHT_ANGLE = 90f
private const val BALLOON_CORNER_RADIUS_DP = 24
private const val BALLOON_TONAL_ELEVATION_DP = 6
private const val BALLOON_SHADOW_ELEVATION_DP = 16
private const val BALLOON_BODY_HORIZONTAL_PADDING_DP = 24
private const val BALLOON_BODY_VERTICAL_PADDING_DP = 24
private const val BALLOON_FOOTER_HORIZONTAL_PADDING_DP = 20
private const val BALLOON_FOOTER_VERTICAL_PADDING_DP = 12
private const val BALLOON_HEADER_HORIZONTAL_PADDING_DP = 20
private const val BALLOON_HEADER_VERTICAL_PADDING_DP = 10
private const val BALLOON_ICON_GAP_DP = 16
private const val FOOTER_META_ALPHA = 0.65f
private const val DIVIDER_ALPHA = 0.5f
private const val BODY_LINE_HEIGHT_SP = 22f
private val BALLOON_MIN_WIDTH = 220.dp
private val BALLOON_MAX_WIDTH = 300.dp
private val BALLOON_GAP = 16.dp
private val BALLOON_OUTER_PADDING = 32.dp

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
        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            drawRect(color = Color.Black.copy(alpha = def.dimAlpha))
            if (anchorBounds != null) {
                drawSpotlight(anchorBounds)
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
            val allowed = listOfNotNull(anchorBounds, actionAnchorBounds)
            if (allowed.isNotEmpty()) {
                SpotlightTouchBlocker(
                    allowedAnchors = allowed,
                    overlaySize = overlaySize,
                    stepId = def.id,
                )
            }
        }

        if (def.isVisible) {
            MessageBalloon(
                anchorBounds = anchorBounds,
                overlaySize = overlaySize,
                def = def,
                onSkip = onSkip,
                onNext = onNext,
            )
        }
    }
}

private fun DrawScope.drawSpotlight(bounds: AnchorBounds) {
    val padding = SPOTLIGHT_PADDING_PX
    drawRoundRect(
        color = Color.Transparent,
        topLeft = Offset(bounds.left - padding, bounds.top - padding),
        size = Size(bounds.width + padding * 2, bounds.height + padding * 2),
        cornerRadius = CornerRadius(SPOTLIGHT_CORNER_RADIUS_PX, SPOTLIGHT_CORNER_RADIUS_PX),
        blendMode = BlendMode.Clear,
    )
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
    allowedAnchors: List<AnchorBounds>,
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

@Composable
private fun MessageBalloon(
    anchorBounds: AnchorBounds?,
    overlaySize: IntSize,
    def: TutorialStepDef,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    val density = LocalDensity.current
    val placeBelow =
        anchorBounds == null ||
            overlaySize.height == 0 ||
            (anchorBounds.top + anchorBounds.height / 2f) < overlaySize.height / 2f

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = BALLOON_OUTER_PADDING),
        verticalArrangement = if (placeBelow) Arrangement.Top else Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (placeBelow && anchorBounds != null) {
            val gapDp = with(density) { (anchorBounds.top + anchorBounds.height).toDp() }
            Spacer(modifier = Modifier.height(gapDp + BALLOON_GAP))
        }
        BalloonCard(def = def, onSkip = onSkip, onNext = onNext)
        if (!placeBelow && anchorBounds != null) {
            val gapDp = with(density) { (overlaySize.height - anchorBounds.top).toDp() }
            Spacer(modifier = Modifier.height(gapDp + BALLOON_GAP))
        }
    }
}

@Composable
private fun BalloonCard(
    def: TutorialStepDef,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    val stepIndex = TutorialStepDefinitions.indexOf(def.id)
    val section = TutorialStepDefinitions.sectionOf(def.id)
    val totalSteps = section?.let { TutorialStepDefinitions.totalOf(it) } ?: 0
    val hasHeader = stepIndex >= 0 || def.showSkip
    Surface(
        shape = RoundedCornerShape(BALLOON_CORNER_RADIUS_DP.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = BALLOON_TONAL_ELEVATION_DP.dp,
        shadowElevation = BALLOON_SHADOW_ELEVATION_DP.dp,
        modifier = Modifier.widthIn(min = BALLOON_MIN_WIDTH, max = BALLOON_MAX_WIDTH),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (hasHeader) {
                BalloonHeader(
                    stepIndex = stepIndex,
                    totalSteps = totalSteps,
                    showSkip = def.showSkip,
                    onSkip = onSkip,
                )
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA),
                    thickness = androidx.compose.ui.unit.Dp.Hairline,
                )
            }
            BalloonBody(def = def)
            if (def.nextButtonLabelResId != null) {
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA),
                    thickness = androidx.compose.ui.unit.Dp.Hairline,
                )
                BalloonFooter(
                    nextButtonLabelResId = def.nextButtonLabelResId,
                    onNext = onNext,
                )
            }
        }
    }
}

@Composable
private fun BalloonBody(def: TutorialStepDef) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = BALLOON_BODY_HORIZONTAL_PADDING_DP.dp,
                    vertical = BALLOON_BODY_VERTICAL_PADDING_DP.dp,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (def.rotationHint != RotationHint.NONE) {
            RotationHintIcon(def.rotationHint)
            Spacer(modifier = Modifier.height(BALLOON_ICON_GAP_DP.dp))
        }
        val messageResId = def.messageResId ?: return@Column
        Text(
            text = stringResource(messageResId),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight =
                androidx.compose.ui.unit
                    .TextUnit(BODY_LINE_HEIGHT_SP, androidx.compose.ui.unit.TextUnitType.Sp),
        )
    }
}

@Composable
private fun BalloonHeader(
    stepIndex: Int,
    totalSteps: Int,
    showSkip: Boolean,
    onSkip: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = BALLOON_HEADER_HORIZONTAL_PADDING_DP.dp,
                    vertical = BALLOON_HEADER_VERTICAL_PADDING_DP.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (stepIndex >= 0) {
            Text(
                text = "${stepIndex + 1} / $totalSteps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = FOOTER_META_ALPHA),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (showSkip) {
            Text(
                text = stringResource(R.string.tutorial_button_skip),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = FOOTER_META_ALPHA),
                modifier = Modifier.clickable(onClick = onSkip),
            )
        }
    }
}

@Composable
private fun BalloonFooter(
    nextButtonLabelResId: Int,
    onNext: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = BALLOON_FOOTER_HORIZONTAL_PADDING_DP.dp,
                    vertical = BALLOON_FOOTER_VERTICAL_PADDING_DP.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(nextButtonLabelResId),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onNext),
        )
    }
}

@Composable
private fun RotationHintIcon(hint: RotationHint) {
    if (hint == RotationHint.PORTRAIT) {
        Icon(
            imageVector = Icons.Outlined.StayCurrentPortrait,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(ROTATION_HINT_ICON_SIZE_DP.dp),
        )
        return
    }
    androidx.compose.runtime.key(hint) {
        val targetAngle = if (hint == RotationHint.LEFT) ROTATION_LEFT_ANGLE else ROTATION_RIGHT_ANGLE
        val transition = rememberInfiniteTransition(label = "rotation-hint")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetAngle,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = ROTATION_ANIM_DURATION_MS),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "rotation-hint-angle",
        )
        Icon(
            imageVector = Icons.Outlined.StayCurrentPortrait,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .size(ROTATION_HINT_ICON_SIZE_DP.dp)
                    .rotate(angle),
        )
    }
}
