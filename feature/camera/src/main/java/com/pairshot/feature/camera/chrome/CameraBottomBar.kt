package com.pairshot.feature.camera.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.pairshot.core.designsystem.PairShotCameraTokens
import com.pairshot.core.designsystem.PairShotIconSize
import com.pairshot.core.designsystem.spec.CameraSpec
import com.pairshot.feature.camera.R
import com.pairshot.feature.camera.component.ShutterButton
import com.pairshot.feature.tutorial.ui.modifier.tutorialAnchor
import com.pairshot.core.ui.R as CoreR

private val ThumbnailSize = CameraSpec.thumbnailSize

@Composable
internal fun CameraBottomBar(
    isSaving: Boolean,
    shutterEnabled: Boolean,
    height: Dp,
    onToggleSettings: () -> Unit,
    onShutterClick: () -> Unit,
    onThumbnailClick: () -> Unit = {},
    shutterAnchorKey: com.pairshot.core.domain.tutorial.AnchorKey =
        com.pairshot.core.domain.tutorial.AnchorKey.CAMERA_SHUTTER_BUTTON,
) {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(PairShotCameraTokens.Letterbox)
            .padding(horizontal = CameraSpec.bottomBarHorizontalPadding),
    ) {
        HomeButton(
            onClick = onThumbnailClick,
            modifier =
            Modifier
                .align(Alignment.CenterStart)
                .tutorialAnchor(com.pairshot.core.domain.tutorial.AnchorKey.CAMERA_BACK_BUTTON),
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
            tutorialAnchorKey = shutterAnchorKey,
        )
    }
}

@Composable
private fun HomeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(ThumbnailSize),
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = stringResource(R.string.camera_desc_home),
            tint = PairShotCameraTokens.Foreground,
            modifier = Modifier.size(PairShotIconSize.lg),
        )
    }
}
