package com.pairshot.feature.settings.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.rendering.PreviewSampleProvider
import com.pairshot.core.rendering.WatermarkRenderer
import com.pairshot.feature.settings.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREVIEW_SCALE_FACTOR = 0.5f
private const val PREVIEW_MIN_DIMENSION_PX = 200
private const val PLACEHOLDER_ASPECT_RATIO = 1f

@Composable
internal fun WatermarkPreviewSection(
    config: WatermarkConfig,
    watermarkRenderer: WatermarkRenderer,
    previewSampleProvider: PreviewSampleProvider,
    modifier: Modifier = Modifier,
) {
    val previewConfig =
        remember(config) {
            config.copy(enabled = true)
        }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(previewConfig) {
        val sample = previewSampleProvider.get()
        val result =
            withContext(Dispatchers.Default) {
                val scaled =
                    Bitmap.createScaledBitmap(
                        sample,
                        (sample.width * PREVIEW_SCALE_FACTOR).toInt().coerceAtLeast(PREVIEW_MIN_DIMENSION_PX),
                        (sample.height * PREVIEW_SCALE_FACTOR).toInt().coerceAtLeast(PREVIEW_MIN_DIMENSION_PX),
                        true,
                    )
                val applied = watermarkRenderer.apply(scaled, previewConfig)
                if (applied !== scaled) scaled.recycle()
                applied
            }
        previewBitmap?.let { old ->
            if (old !== result && !old.isRecycled) old.recycle()
        }
        previewBitmap = result
    }

    val bmp = previewBitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = stringResource(R.string.watermark_preview_desc),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
            modifier =
                modifier
                    .fillMaxWidth()
                    .aspectRatio(
                        bmp.width.toFloat().coerceAtLeast(1f) /
                            bmp.height.toFloat().coerceAtLeast(1f),
                    ),
        )
    } else {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .aspectRatio(PLACEHOLDER_ASPECT_RATIO),
        )
    }
}
