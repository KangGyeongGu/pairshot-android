package com.pairshot.core.ui.component

import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.pairshot.core.ui.R

@Composable
fun ProfiledAsyncImage(
    data: Any?,
    profile: ImageProfile,
    contentDescription: String?,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    placeholderColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onAspectRatio: ((Float) -> Unit)? = null,
) {
    val model =
        remember(data) {
            when (data) {
                is Uri -> data
                is String -> runCatching { Uri.parse(data) }.getOrNull()
                else -> null
            }
        }

    if (model == null) {
        ErrorPlaceholder(modifier)
        return
    }

    val context = LocalContext.current
    val placeholderArgb = placeholderColor.toArgb()
    val aspectRatioCallback by rememberUpdatedState(onAspectRatio)

    val scaleType =
        when (contentScale) {
            ContentScale.Crop -> ImageView.ScaleType.CENTER_CROP
            ContentScale.Fit, ContentScale.Inside -> ImageView.ScaleType.FIT_CENTER
            else -> ImageView.ScaleType.CENTER_CROP
        }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                ImageView(ctx).apply {
                    setBackgroundColor(placeholderArgb)
                    this.scaleType = scaleType
                    this.contentDescription = contentDescription
                }
            },
            update = { view ->
                view.setBackgroundColor(placeholderArgb)
                view.scaleType = scaleType
                Glide
                    .with(context)
                    .load(model)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(
                        object : RequestListener<Drawable> {
                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>?,
                                dataSource: DataSource,
                                isFirstResource: Boolean,
                            ): Boolean {
                                val width = resource.intrinsicWidth
                                val height = resource.intrinsicHeight
                                if (width > 0 && height > 0) {
                                    aspectRatioCallback?.invoke(width.toFloat() / height.toFloat())
                                }
                                return false
                            }

                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean,
                            ): Boolean = false
                        },
                    ).into(view)
            },
            onRelease = { view ->
                Glide.with(context).clear(view)
            },
        )
    }
}

@Composable
private fun ErrorPlaceholder(modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        ErrorContent()
    }
}

@Composable
private fun ErrorContent() {
    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.image_file_missing),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}
