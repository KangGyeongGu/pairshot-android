package com.pairshot.feature.camera.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.pairshot.core.domain.tutorial.AnchorKey
import com.pairshot.feature.tutorial.ui.modifier.tutorialAnchor

@Composable
fun OverlayGuide(
    bitmap: Bitmap?,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    if (bitmap == null || alpha <= 0f) return
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier =
        modifier
            .alpha(alpha)
            .tutorialAnchor(AnchorKey.AFTER_CAMERA_OVERLAY),
    )
}
