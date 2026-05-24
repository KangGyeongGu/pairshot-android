package com.pairshot.feature.settings.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.CombineLayout
import com.pairshot.core.model.WatermarkConfig
import com.pairshot.core.rendering.CombinePreviewEntryPoint
import com.pairshot.core.rendering.CombinePreviewRenderer
import com.pairshot.feature.settings.R
import dagger.hilt.android.EntryPointAccessors

private const val ASPECT_RATIO_HORIZONTAL = 2f
private const val ASPECT_RATIO_VERTICAL = 0.5f

@Composable
internal fun CombinePreviewSection(
    config: CombineConfig,
    watermarkConfig: WatermarkConfig,
    modifier: Modifier = Modifier,
) {
    val renderer = rememberCombinePreviewRenderer()
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(config, watermarkConfig) {
        val result = renderer.render(config, watermarkConfig)
        previewBitmap?.let { old ->
            if (old !== result && !old.isRecycled) old.recycle()
        }
        previewBitmap = result
    }

    DisposableEffect(Unit) {
        onDispose {
            previewBitmap?.takeIf { !it.isRecycled }?.recycle()
        }
    }

    val aspectRatio =
        when (config.layout) {
            CombineLayout.HORIZONTAL -> ASPECT_RATIO_HORIZONTAL
            CombineLayout.VERTICAL -> ASPECT_RATIO_VERTICAL
        }

    val bmp = previewBitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = stringResource(R.string.combine_preview_desc),
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
                .aspectRatio(aspectRatio),
        )
    }
}

@Composable
private fun rememberCombinePreviewRenderer(): CombinePreviewRenderer {
    val context = LocalContext.current
    return remember(context) {
        EntryPointAccessors
            .fromApplication(
                context.applicationContext,
                CombinePreviewEntryPoint::class.java,
            ).combinePreviewRenderer()
    }
}
