package com.pairshot.feature.camera.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.pairshot.core.designsystem.PairShotCameraTokens
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pairshot.core.ui.component.ImageProfile
import com.pairshot.core.ui.component.ProfiledAsyncImage
import com.pairshot.core.designsystem.spec.CameraSpec
import com.pairshot.core.designsystem.PairShotIconSize
import com.pairshot.core.designsystem.PairShotStroke
import com.pairshot.feature.camera.R
import com.pairshot.feature.camera.component.ShutterButton
import com.pairshot.core.ui.R as CoreR

private val ThumbnailSize = CameraSpec.thumbnailSize
private val ThumbnailCornerRadius = CameraSpec.thumbnailCornerRadius

@Composable
internal fun CameraBottomBar(
    isSaving: Boolean,
    shutterEnabled: Boolean,
    height: Dp,
    onToggleSettings: () -> Unit,
    onShutterClick: () -> Unit,
    lastPairThumbnailUri: String? = null,
    onThumbnailClick: () -> Unit = {},
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height)
                .background(PairShotCameraTokens.Letterbox)
                .padding(horizontal = CameraSpec.bottomBarHorizontalPadding),
    ) {
        ThumbnailOrHomeButton(
            thumbnailUri = lastPairThumbnailUri,
            onClick = onThumbnailClick,
            modifier = Modifier.align(Alignment.CenterStart),
        )

        IconButton(
            onClick = onToggleSettings,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(CoreR.string.common_desc_settings),
                tint = PairShotCameraTokens.Foreground,
                modifier = Modifier.size(PairShotIconSize.lg),
            )
        }

        ShutterButton(
            onClick = onShutterClick,
            enabled = !isSaving && shutterEnabled,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun ThumbnailOrHomeButton(
    thumbnailUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(ThumbnailCornerRadius)
    Box(
        modifier =
            modifier
                .size(ThumbnailSize)
                .clip(shape)
                .border(
                    width = PairShotStroke.hairline,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = shape,
                ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnailUri != null) {
            ProfiledAsyncImage(
                data = thumbnailUri,
                profile = ImageProfile.THUMBNAIL,
                contentDescription = stringResource(R.string.camera_desc_last_thumbnail),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(ThumbnailSize),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = stringResource(R.string.camera_desc_home),
                tint = PairShotCameraTokens.Foreground,
                modifier = Modifier.size(PairShotIconSize.lg),
            )
        }
    }
}
