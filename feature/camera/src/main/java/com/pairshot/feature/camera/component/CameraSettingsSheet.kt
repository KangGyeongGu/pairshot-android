package com.pairshot.feature.camera.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FlashAuto
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Grid3x3
import androidx.compose.material.icons.outlined.HdrOn
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.pairshot.core.designsystem.LocalPairShotExtendedColors
import com.pairshot.core.designsystem.PairShotCameraTokens
import com.pairshot.core.designsystem.PairShotGlassTokens
import com.pairshot.core.designsystem.PairShotMotionTokens
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotTypographyTokens
import com.pairshot.core.model.AspectRatio
import com.pairshot.core.model.CameraCapabilities
import com.pairshot.core.model.FlashMode
import com.pairshot.core.model.SortOrder
import com.pairshot.feature.camera.R
import com.pairshot.feature.camera.state.CameraSettingsState
import kotlin.math.roundToInt

private const val SETTINGS_ITEMS_PER_ROW = 3

@Composable
fun CameraSettingsSheet(
    visible: Boolean,
    settingsState: CameraSettingsState,
    capabilities: CameraCapabilities,
    onToggleGrid: () -> Unit,
    onCycleFlash: () -> Unit,
    onToggleNightMode: () -> Unit,
    onToggleHdr: () -> Unit,
    onToggleLevel: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    overlayEnabled: Boolean? = null,
    onToggleOverlay: (() -> Unit)? = null,
    overlayAlpha: Float? = null,
    onOverlayAlphaChange: ((Float) -> Unit)? = null,
    sortOrder: SortOrder? = null,
    onToggleSortOrder: (() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = PairShotMotionTokens.panelEnterTween()),
            exit = fadeOut(animationSpec = PairShotMotionTokens.panelExitTween()),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(PairShotCameraTokens.Letterbox.copy(alpha = 0.52f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { onDismiss() },
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter =
                fadeIn(animationSpec = PairShotMotionTokens.panelEnterTween()) +
                    scaleIn(initialScale = 0.96f, animationSpec = PairShotMotionTokens.panelEnterTween()),
            exit =
                fadeOut(animationSpec = PairShotMotionTokens.panelExitTween()) +
                    scaleOut(targetScale = 0.98f, animationSpec = PairShotMotionTokens.panelExitTween()),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(horizontal = PairShotSpacing.cardPadding)
                        .widthIn(max = 520.dp)
                        .clip(PairShotGlassTokens.shape)
                        .background(PairShotGlassTokens.surfaceColor)
                        .border(PairShotGlassTokens.border.width, PairShotGlassTokens.border.brush, PairShotGlassTokens.shape)
                        .padding(horizontal = PairShotSpacing.screenPadding, vertical = PairShotSpacing.screenPadding)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { },
            ) {
                val settingItems =
                    buildSettingItems(
                        state = settingsState,
                        capabilities = capabilities,
                        onToggleGrid = onToggleGrid,
                        onCycleFlash = onCycleFlash,
                        onToggleNightMode = onToggleNightMode,
                        onToggleHdr = onToggleHdr,
                        onToggleLevel = onToggleLevel,
                        onCycleAspectRatio = onCycleAspectRatio,
                        overlayEnabled = overlayEnabled,
                        onToggleOverlay = onToggleOverlay,
                        sortOrder = sortOrder,
                        onToggleSortOrder = onToggleSortOrder,
                    )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    settingItems.chunked(SETTINGS_ITEMS_PER_ROW).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowItems.forEach { item ->
                                SettingIconItem(
                                    icon = item.icon,
                                    iconText = item.iconText,
                                    label = item.label,
                                    isActive = item.isActive,
                                    isEnabled = item.isEnabled,
                                    onClick = item.onClick,
                                    iconFlippedVertical = item.iconFlippedVertical,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(SETTINGS_ITEMS_PER_ROW - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (overlayAlpha != null && onOverlayAlphaChange != null) {
                    Spacer(modifier = Modifier.height(PairShotSpacing.iconSize))
                    OverlayAlphaSlider(
                        alpha = overlayAlpha,
                        enabled = overlayEnabled == true,
                        onAlphaChange = onOverlayAlphaChange,
                    )
                }
            }
        }
    }
}

private data class SettingItem(
    val icon: ImageVector?,
    val label: String,
    val isActive: Boolean,
    val onClick: () -> Unit,
    val iconFlippedVertical: Boolean = false,
    val iconText: String? = null,
    val isEnabled: Boolean = true,
)

@Composable
private fun buildSettingItems(
    state: CameraSettingsState,
    capabilities: CameraCapabilities,
    onToggleGrid: () -> Unit,
    onCycleFlash: () -> Unit,
    onToggleNightMode: () -> Unit,
    onToggleHdr: () -> Unit,
    onToggleLevel: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    overlayEnabled: Boolean? = null,
    onToggleOverlay: (() -> Unit)? = null,
    sortOrder: SortOrder? = null,
    onToggleSortOrder: (() -> Unit)? = null,
): List<SettingItem> {
    val items = mutableListOf<SettingItem>()

    items.add(
        SettingItem(
            icon = Icons.Outlined.Grid3x3,
            label = stringResource(R.string.camera_settings_grid),
            isActive = state.gridEnabled,
            onClick = onToggleGrid,
        ),
    )

    if (capabilities.hasFlash) {
        val flashIcon =
            when (state.flashMode) {
                FlashMode.OFF -> Icons.Outlined.FlashOff
                FlashMode.AUTO -> Icons.Outlined.FlashAuto
                FlashMode.ON -> Icons.Outlined.FlashOn
                FlashMode.TORCH -> Icons.Outlined.FlashlightOn
            }
        items.add(
            SettingItem(
                icon = flashIcon,
                label = stringResource(R.string.camera_settings_flash),
                isActive = state.flashMode != FlashMode.OFF,
                onClick = onCycleFlash,
            ),
        )
    }

    if (capabilities.nightModeAvailable) {
        items.add(
            SettingItem(
                icon = Icons.Outlined.NightsStay,
                label = stringResource(R.string.camera_settings_night_mode),
                isActive = state.nightModeEnabled,
                onClick = onToggleNightMode,
            ),
        )
    }

    if (capabilities.hdrAvailable) {
        items.add(
            SettingItem(
                icon = Icons.Outlined.HdrOn,
                label = "HDR",
                isActive = state.hdrEnabled,
                onClick = onToggleHdr,
            ),
        )
    }

    if (overlayEnabled != null && onToggleOverlay != null) {
        items.add(
            SettingItem(
                icon = Icons.Outlined.Layers,
                label = stringResource(R.string.camera_settings_overlay),
                isActive = overlayEnabled,
                onClick = onToggleOverlay,
            ),
        )
    }

    items.add(
        SettingItem(
            icon = Icons.Outlined.Straighten,
            label = stringResource(R.string.camera_settings_level),
            isActive = state.levelEnabled,
            onClick = onToggleLevel,
        ),
    )

    val ratioText =
        when (state.aspectRatio) {
            AspectRatio.RATIO_4_3 -> "4:3"
            AspectRatio.RATIO_16_9 -> "16:9"
            AspectRatio.RATIO_1_1 -> "1:1"
        }
    items.add(
        SettingItem(
            icon = null,
            iconText = ratioText,
            label = stringResource(R.string.camera_settings_aspect_ratio),
            isActive = true,
            isEnabled = !state.aspectRatioLocked,
            onClick = onCycleAspectRatio,
        ),
    )

    if (sortOrder != null && onToggleSortOrder != null) {
        items.add(
            SettingItem(
                icon = Icons.AutoMirrored.Outlined.Sort,
                label = stringResource(R.string.camera_settings_sort),
                isActive = true,
                onClick = onToggleSortOrder,
                iconFlippedVertical = sortOrder == SortOrder.ASC,
            ),
        )
    }

    return items
}

@Composable
private fun SettingIconItem(
    icon: ImageVector?,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconText: String? = null,
    isEnabled: Boolean = true,
    iconFlippedVertical: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val clickableModifier =
        if (isEnabled) {
            modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            )
        } else {
            modifier
        }
    val effectiveTint =
        when {
            !isEnabled -> PairShotCameraTokens.Foreground.copy(alpha = 0.30f)
            isActive -> MaterialTheme.colorScheme.primary
            else -> PairShotCameraTokens.Foreground.copy(alpha = 0.55f)
        }
    Column(
        modifier = clickableModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isEnabled && isActive) {
                            PairShotCameraTokens.Foreground.copy(alpha = 0.18f)
                        } else {
                            Color.Transparent
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (iconText != null) {
                Text(
                    text = iconText,
                    style = PairShotTypographyTokens.labelExtraSmall,
                    color = effectiveTint,
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier =
                        Modifier
                            .size(24.dp)
                            .graphicsLayer(scaleY = if (iconFlippedVertical) -1f else 1f),
                    tint = effectiveTint,
                )
            }
        }
        Text(
            text = label,
            style = PairShotTypographyTokens.labelExtraSmall,
            color = effectiveTint,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverlayAlphaSlider(
    alpha: Float,
    enabled: Boolean,
    onAlphaChange: (Float) -> Unit,
) {
    var localAlpha by remember { mutableFloatStateOf(alpha) }
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()

    LaunchedEffect(alpha) {
        if (!isDragged) localAlpha = alpha
    }

    val contentColor =
        if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val primaryColor =
        if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val sliderColors =
        SliderDefaults.colors(
            thumbColor = primaryColor,
            activeTrackColor = primaryColor,
            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
            disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledActiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.camera_settings_overlay_opacity),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
            )
            Text(
                text = "${(localAlpha * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = primaryColor,
            )
        }
        Slider(
            value = localAlpha,
            onValueChange = { localAlpha = it },
            onValueChangeFinished = { onAlphaChange(localAlpha) },
            valueRange = 0f..1.0f,
            enabled = enabled,
            interactionSource = interactionSource,
            colors = sliderColors,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    thumbSize = DpSize(1.dp, 16.dp),
                    colors = sliderColors,
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.graphicsLayer(scaleY = 0.3f),
                    colors = sliderColors,
                    drawTick = { _, _ -> },
                    drawStopIndicator = null,
                )
            },
        )
        AnimatedVisibility(
            visible = localAlpha > 0.75f,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            val warningColor = LocalPairShotExtendedColors.current.warning
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = warningColor,
                )
                Text(
                    text = stringResource(R.string.camera_settings_overlay_opacity_hint),
                    style = PairShotTypographyTokens.labelExtraSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                    color = warningColor,
                )
            }
        }
    }
}
