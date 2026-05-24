package com.pairshot.feature.pairpreview.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.pairshot.feature.pairpreview.R

private const val MIN_ZOOM_SCALE = 1f
private const val MAX_ZOOM_SCALE = 4f

@Composable
fun PairPreviewCenter(
    livePreviewBitmap: Bitmap?,
    livePreviewFailed: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(MIN_ZOOM_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier =
        modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
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
        contentAlignment = Alignment.Center,
    ) {
        LivePreviewContent(
            bitmap = livePreviewBitmap,
            failed = livePreviewFailed,
            onRetry = onRetry,
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

@Composable
private fun LivePreviewContent(
    bitmap: Bitmap?,
    failed: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        bitmap != null && !bitmap.isRecycled -> {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.pair_preview_desc_combined_preview),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.High,
                modifier = modifier,
            )
        }

        failed -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.pair_preview_error_compose_failed),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onRetry) {
                        Text(text = stringResource(R.string.pair_preview_action_retry))
                    }
                }
            }
        }

        else -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
