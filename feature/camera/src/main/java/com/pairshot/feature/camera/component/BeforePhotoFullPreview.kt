package com.pairshot.feature.camera.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.pairshot.core.ui.component.ImageProfile
import com.pairshot.core.ui.component.ProfiledAsyncImage
import com.pairshot.feature.camera.R

private const val MIN_ZOOM_SCALE = 1f
private const val MAX_ZOOM_SCALE = 6f
private const val PHOTO_MAX_WIDTH_FRACTION = 0.9f
private const val PHOTO_MAX_HEIGHT_FRACTION = 0.72f
private const val FALLBACK_ASPECT_RATIO = 1f

@Composable
fun BeforePhotoFullPreview(
    uri: String,
    bottomAnchor: Dp,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onDismiss() }

    val scrimInteraction = remember { MutableInteractionSource() }
    val photoInteraction = remember { MutableInteractionSource() }
    var scale by remember { mutableFloatStateOf(MIN_ZOOM_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var aspectRatio by remember { mutableStateOf<Float?>(null) }
    val visibleAlpha by animateFloatAsState(
        targetValue = if (aspectRatio != null) 1f else 0f,
        label = "beforePreviewAlpha",
    )

    BoxWithConstraints(
        modifier =
        modifier
            .fillMaxSize()
            .clickable(
                interactionSource = scrimInteraction,
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        val ratio = aspectRatio ?: FALLBACK_ASPECT_RATIO
        val containerMaxW = maxWidth * PHOTO_MAX_WIDTH_FRACTION
        val containerMaxH = maxHeight * PHOTO_MAX_HEIGHT_FRACTION
        val containerRatio = containerMaxW.value / containerMaxH.value
        val (photoWidth, photoHeight) =
            if (ratio >= containerRatio) {
                containerMaxW to containerMaxW / ratio
            } else {
                containerMaxH * ratio to containerMaxH
            }

        Box(
            modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomAnchor)
                .size(width = photoWidth, height = photoHeight)
                .alpha(visibleAlpha)
                .clip(MaterialTheme.shapes.large)
                .clipToBounds()
                .clickable(
                    interactionSource = photoInteraction,
                    indication = null,
                    onClick = {},
                ).pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(MIN_ZOOM_SCALE, MAX_ZOOM_SCALE)
                        scale = newScale
                        offset =
                            if (newScale > MIN_ZOOM_SCALE) {
                                offset + pan
                            } else {
                                Offset.Zero
                            }
                    }
                },
        ) {
            ProfiledAsyncImage(
                data = uri,
                profile = ImageProfile.DETAIL,
                contentDescription = stringResource(R.string.camera_before_full_preview_desc),
                contentScale = ContentScale.Fit,
                placeholderColor = Color.Transparent,
                onAspectRatio = { aspectRatio = it },
                modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    ),
            )
        }
    }
}
