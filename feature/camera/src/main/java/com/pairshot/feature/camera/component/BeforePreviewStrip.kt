package com.pairshot.feature.camera.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.pairshot.core.designsystem.PairShotCameraTokens
import com.pairshot.core.designsystem.PairShotIconSize
import com.pairshot.core.designsystem.PairShotStroke
import com.pairshot.core.designsystem.spec.CameraSpec
import com.pairshot.core.domain.tutorial.AnchorKey
import com.pairshot.core.ui.component.ImageProfile
import com.pairshot.core.ui.component.ProfiledAsyncImage
import com.pairshot.feature.camera.R
import com.pairshot.feature.tutorial.ui.modifier.tutorialAnchor
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.abs

internal val BeforeStripHeight: Dp = CameraSpec.beforeStripHeight

private val ACTIVE_CARD_WIDTH = CameraSpec.beforeCardWidth
private val ACTIVE_CARD_HEIGHT = CameraSpec.beforeCardHeight
private val CARD_SPACING = CameraSpec.beforeCardSpacing
private val FALLBACK_HORIZONTAL_PADDING = CameraSpec.beforeCardFallbackHPadding
private val PROGRESS_INDICATOR_HEIGHT = PairShotIconSize.lg
private const val INACTIVE_SCALE = CameraSpec.BEFORE_CARD_INACTIVE_SCALE
private val INACTIVE_BORDER_WIDTH = PairShotStroke.hairline
private val ACTIVE_BORDER_WIDTH = PairShotStroke.thick
private const val TRANSFORM_ORIGIN_CENTER_X = 0.5f
private const val TRANSFORM_ORIGIN_BOTTOM_Y = 1f
private const val SCALE_ANIMATION_MS = 180
private val CARD_CORNER_RADIUS = CameraSpec.beforeCardCornerRadius

data class StripProgress(
    val completed: Int,
    val total: Int,
)

@Composable
fun BeforePreviewStrip(
    beforePreviewUris: ImmutableList<String>,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
    onSelectIndex: ((Int) -> Unit)? = null,
    onLongPressIndex: ((Int) -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
    emptyMessage: String = stringResource(R.string.camera_strip_empty),
    stripHeight: Dp = BeforeStripHeight,
    allActiveSize: Boolean = false,
    progress: StripProgress? = null,
) {
    val snapEnabled = onSelectIndex != null
    val haptic = LocalHapticFeedback.current

    if (snapEnabled) {
        val snappedIndex by remember(listState) {
            derivedStateOf {
                val info = listState.layoutInfo
                val items = info.visibleItemsInfo
                if (items.isEmpty()) return@derivedStateOf -1
                val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
                items.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }?.index ?: -1
            }
        }
        val isDragging by listState.interactionSource.collectIsDraggedAsState()
        var userInteracting by remember { mutableStateOf(false) }
        var lastHapticIndex by remember { mutableIntStateOf(-1) }
        LaunchedEffect(isDragging) {
            if (isDragging) {
                userInteracting = true
                lastHapticIndex = snappedIndex
            }
        }
        LaunchedEffect(listState.isScrollInProgress) {
            if (!listState.isScrollInProgress) userInteracting = false
        }
        LaunchedEffect(snappedIndex) {
            if (snappedIndex < 0) return@LaunchedEffect
            if (lastHapticIndex < 0) {
                lastHapticIndex = snappedIndex
                return@LaunchedEffect
            }
            if (userInteracting && snappedIndex != lastHapticIndex) {
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onSelectIndex?.invoke(snappedIndex)
                lastHapticIndex = snappedIndex
            }
        }
    }

    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .height(stripHeight)
            .background(PairShotCameraTokens.Letterbox),
    ) {
        if (beforePreviewUris.isEmpty()) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier =
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = FALLBACK_HORIZONTAL_PADDING),
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                BoxWithConstraints(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    val horizontalPadding =
                        if (snapEnabled) {
                            ((maxWidth - ACTIVE_CARD_WIDTH) / 2).coerceAtLeast(FALLBACK_HORIZONTAL_PADDING)
                        } else {
                            FALLBACK_HORIZONTAL_PADDING
                        }
                    val flingBehavior =
                        if (snapEnabled) {
                            rememberSnapFlingBehavior(
                                lazyListState = listState,
                                snapPosition = SnapPosition.Center,
                            )
                        } else {
                            ScrollableDefaults.flingBehavior()
                        }
                    LazyRow(
                        state = listState,
                        flingBehavior = flingBehavior,
                        contentPadding = PaddingValues(horizontal = horizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(CARD_SPACING),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(
                            items = beforePreviewUris,
                            key = { index, uri -> "$uri-$index" },
                        ) { index, beforeUri ->
                            val isSelected = selectedIndex == index
                            val useActiveSize = allActiveSize || isSelected
                            val scale by animateFloatAsState(
                                targetValue = if (useActiveSize) 1f else INACTIVE_SCALE,
                                animationSpec = tween(durationMillis = SCALE_ANIMATION_MS),
                                label = "stripCardScale",
                            )
                            val borderWidth = if (isSelected) ACTIVE_BORDER_WIDTH else INACTIVE_BORDER_WIDTH
                            val borderColor =
                                if (isSelected) {
                                    PairShotCameraTokens.SelectionHighlight
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }

                            ProfiledAsyncImage(
                                data = beforeUri,
                                profile = ImageProfile.THUMBNAIL,
                                contentDescription =
                                stringResource(
                                    R.string.camera_strip_thumbnail_desc,
                                    index + 1,
                                ),
                                contentScale = ContentScale.Crop,
                                modifier =
                                Modifier
                                    .size(width = ACTIVE_CARD_WIDTH, height = ACTIVE_CARD_HEIGHT)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        transformOrigin =
                                            TransformOrigin(TRANSFORM_ORIGIN_CENTER_X, TRANSFORM_ORIGIN_BOTTOM_Y)
                                    }.clip(RoundedCornerShape(CARD_CORNER_RADIUS))
                                    .border(
                                        width = borderWidth,
                                        color = borderColor,
                                        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
                                    ).then(
                                        if (isSelected) {
                                            Modifier.tutorialAnchor(AnchorKey.AFTER_CAMERA_SELECTED_CARD)
                                        } else {
                                            Modifier
                                        },
                                    ).then(
                                        if (onSelectIndex != null) {
                                            Modifier.combinedClickable(
                                                onClick = { onSelectIndex(index) },
                                                onLongClick =
                                                onLongPressIndex?.let { callback ->
                                                    {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                        callback(index)
                                                    }
                                                },
                                            )
                                        } else {
                                            Modifier
                                        },
                                    ),
                            )
                        }
                    }
                }

                if (progress != null) {
                    StripProgressIndicator(
                        completed = progress.completed,
                        total = progress.total,
                    )
                }
            }
        }
    }
}

@Composable
private fun StripProgressIndicator(
    completed: Int,
    total: Int,
) {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(PROGRESS_INDICATOR_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = pluralStringResource(R.plurals.camera_strip_progress, total, completed, total),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = PairShotCameraTokens.Foreground,
        )
    }
}
