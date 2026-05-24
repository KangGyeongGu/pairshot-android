package com.pairshot.feature.camera.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pairshot.core.designsystem.PairShotCameraTokens
import com.pairshot.core.designsystem.PairShotIconSize
import com.pairshot.core.designsystem.PairShotStroke
import com.pairshot.core.designsystem.spec.CameraSpec
import com.pairshot.core.domain.tutorial.AnchorKey
import com.pairshot.feature.camera.R
import com.pairshot.feature.tutorial.ui.modifier.tutorialAnchor

private const val SHUTTER_DISABLED_ALPHA = 0.5f

@Composable
fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    innerColor: Color = PairShotCameraTokens.Foreground,
    enabled: Boolean = true,
    tutorialAnchorKey: AnchorKey? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.9f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "shutter_scale",
    )
    val shutterDesc = stringResource(R.string.camera_desc_shutter)

    Box(
        modifier =
        modifier
            .size(CameraSpec.shutterOuterSize)
            .scale(scale)
            .alpha(if (enabled) 1f else SHUTTER_DISABLED_ALPHA)
            .border(
                width = CameraSpec.shutterBorderWidth,
                color = PairShotCameraTokens.Foreground,
                shape = CircleShape
            )
            .semantics { contentDescription = shutterDesc }
            .let { base -> if (tutorialAnchorKey != null) base.tutorialAnchor(tutorialAnchorKey) else base }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(CameraSpec.shutterInnerSize),
            shape = CircleShape,
            color = innerColor,
            content = {},
        )
        if (!enabled) {
            CircularProgressIndicator(
                modifier = Modifier.size(PairShotIconSize.md),
                color = PairShotCameraTokens.Foreground,
                strokeWidth = PairShotStroke.thin,
            )
        }
    }
}
