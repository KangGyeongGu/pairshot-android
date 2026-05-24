package com.pairshot.feature.tutorial.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.pairshot.core.designsystem.ProvideAppTextScaleDensity
import com.pairshot.core.domain.tutorial.AnchorBounds
import com.pairshot.feature.tutorial.R
import com.pairshot.feature.tutorial.domain.RotationHint
import com.pairshot.feature.tutorial.domain.TutorialStepDef
import com.pairshot.feature.tutorial.domain.TutorialStepDefinitions

internal val BALLOON_GAP = 16.dp
internal val BALLOON_OUTER_PADDING = 16.dp

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
private const val BALLOON_MAX_HEIGHT_RATIO = 0.5f
private val BALLOON_MIN_WIDTH = 220.dp
private val BALLOON_MAX_WIDTH = 320.dp

internal data class TutorialBalloonPositionProvider(
    val anchor: AnchorBounds?,
    val actionAnchor: AnchorBounds?,
    val gapPx: Int,
    val topInsetPx: Int,
    val bottomInsetPx: Int,
    val horizontalMarginPx: Int,
    val centerMode: Boolean,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val safeTop = topInsetPx
        val safeBottom = (windowSize.height - bottomInsetPx).coerceAtLeast(safeTop)
        val safeLeft = horizontalMarginPx
        val safeRight = (windowSize.width - horizontalMarginPx).coerceAtLeast(safeLeft)

        val maxY = (safeBottom - popupContentSize.height).coerceAtLeast(safeTop)
        val maxX = (safeRight - popupContentSize.width).coerceAtLeast(safeLeft)

        val avoid = actionAnchor ?: anchor
        if (centerMode || avoid == null) {
            val cx = ((safeLeft + safeRight - popupContentSize.width) / 2).coerceIn(safeLeft, maxX)
            val cy = ((safeTop + safeBottom - popupContentSize.height) / 2).coerceIn(safeTop, maxY)
            return IntOffset(cx, cy)
        }

        val avoidLeft = avoid.left.toInt()
        val avoidTop = avoid.top.toInt()
        val avoidRight = (avoid.left + avoid.width).toInt()
        val avoidBottom = (avoid.top + avoid.height).toInt()

        val desiredX = (avoidLeft + avoidRight) / 2 - popupContentSize.width / 2
        val x = desiredX.coerceIn(safeLeft, maxX)

        val spaceBelow = safeBottom - avoidBottom - gapPx
        val spaceAbove = avoidTop - gapPx - safeTop
        val placeBelow = spaceBelow >= popupContentSize.height || spaceBelow >= spaceAbove

        val y =
            if (placeBelow) {
                (avoidBottom + gapPx).coerceIn(safeTop, maxY)
            } else {
                (avoidTop - gapPx - popupContentSize.height).coerceIn(safeTop, maxY)
            }
        return IntOffset(x, y)
    }
}

@Composable
internal fun TutorialPopupBalloon(
    anchorBounds: AnchorBounds?,
    actionAnchorBounds: AnchorBounds?,
    def: TutorialStepDef,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    val density = LocalDensity.current
    val statusBars = WindowInsets.statusBars
    val navigationBars = WindowInsets.navigationBars
    val topInsetPx = statusBars.getTop(density)
    val bottomInsetPx = navigationBars.getBottom(density)
    val gapPx = with(density) { BALLOON_GAP.roundToPx() }
    val outerMarginPx = with(density) { BALLOON_OUTER_PADDING.roundToPx() }

    val positionProvider =
        remember(
            anchorBounds,
            actionAnchorBounds,
            def.centerMessage,
            topInsetPx,
            bottomInsetPx,
            gapPx,
            outerMarginPx,
        ) {
            TutorialBalloonPositionProvider(
                anchor = anchorBounds,
                actionAnchor = actionAnchorBounds,
                gapPx = gapPx,
                topInsetPx = topInsetPx,
                bottomInsetPx = bottomInsetPx,
                horizontalMarginPx = outerMarginPx,
                centerMode = def.centerMessage,
            )
        }

    Popup(
        popupPositionProvider = positionProvider,
        properties =
        PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = true,
        ),
    ) {
        ProvideAppTextScaleDensity {
            BalloonCard(def = def, onSkip = onSkip, onNext = onNext)
        }
    }
}

@Composable
private fun BalloonCard(
    def: TutorialStepDef,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    val density = LocalDensity.current
    val statusBars = WindowInsets.statusBars
    val navigationBars = WindowInsets.navigationBars
    val configuration = LocalConfiguration.current
    val maxBalloonHeight =
        with(density) {
            val windowHeightPx = configuration.screenHeightDp.dp.roundToPx()
            val safeHeightPx =
                (windowHeightPx - statusBars.getTop(density) - navigationBars.getBottom(density))
                    .coerceAtLeast(0)
            (safeHeightPx * BALLOON_MAX_HEIGHT_RATIO).toInt().toDp()
        }

    val stepIndex = TutorialStepDefinitions.indexOf(def.id)
    val section = TutorialStepDefinitions.sectionOf(def.id)
    val totalSteps = section?.let { TutorialStepDefinitions.totalOf(it) } ?: 0
    val hasHeader = stepIndex >= 0 || def.showSkip
    Surface(
        shape = RoundedCornerShape(BALLOON_CORNER_RADIUS_DP.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = BALLOON_TONAL_ELEVATION_DP.dp,
        shadowElevation = BALLOON_SHADOW_ELEVATION_DP.dp,
        modifier =
        Modifier
            .widthIn(min = BALLOON_MIN_WIDTH, max = BALLOON_MAX_WIDTH)
            .heightIn(max = maxBalloonHeight),
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            if (hasHeader) {
                BalloonHeader(
                    stepIndex = stepIndex,
                    totalSteps = totalSteps,
                    showSkip = def.showSkip,
                    onSkip = onSkip,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA),
                    thickness = Dp.Hairline,
                )
            }
            BalloonBody(def = def)
            if (def.nextButtonLabelResId != null) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA),
                    thickness = Dp.Hairline,
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
        val message = stringResource(messageResId)
        val keyword = def.messageKeywordResId?.let { stringResource(it) }
        val highlightColor = MaterialTheme.colorScheme.primary
        val annotated =
            remember(message, keyword, highlightColor) {
                buildHighlightedMessage(message, keyword, highlightColor)
            }
        Text(
            text = annotated,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            lineHeight = TextUnit(BODY_LINE_HEIGHT_SP, TextUnitType.Sp),
        )
    }
}

private fun buildHighlightedMessage(
    message: String,
    keyword: String?,
    highlightColor: Color,
): AnnotatedString {
    if (keyword.isNullOrEmpty()) {
        return AnnotatedString(message)
    }
    val start = message.indexOf(keyword)
    if (start < 0) {
        return AnnotatedString(message)
    }
    return buildAnnotatedString {
        append(message)
        addStyle(
            style =
            SpanStyle(
                fontWeight = FontWeight.Bold,
                color = highlightColor,
            ),
            start = start,
            end = start + keyword.length,
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
    key(hint) {
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
