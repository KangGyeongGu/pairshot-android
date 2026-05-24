package com.pairshot.core.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import com.pairshot.core.designsystem.LocalPairShotExtendedColors
import com.pairshot.core.designsystem.PairShotCard
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotStroke
import kotlin.math.abs

private const val SLIDER_TRACK_SCALE_Y = 0.3f
private const val SLIDER_SYNC_THRESHOLD = 1e-4f

@Composable
fun SettingsSectionLabel(
    label: String,
    modifier: Modifier = Modifier,
    trailingWarning: String? = null,
) {
    if (trailingWarning != null) {
        Row(
            modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = PairShotSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            WarningBadge(text = trailingWarning)
        }
        return
    }
    Text(
        text = label,
        modifier =
        modifier
            .fillMaxWidth()
            .padding(horizontal = PairShotSpacing.sm),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun WarningBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    val warningColor = LocalPairShotExtendedColors.current.warning
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(PairShotSpacing.lg),
            tint = warningColor,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = warningColor,
        )
    }
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    label: String,
    trailing: String? = null,
    trailingIsError: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    PairShotListItem(
        headline = label,
        trailing = trailing,
        trailingIsError = trailingIsError,
        onClick = onClick,
    )
}

@Composable
fun SettingsSwitchItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    PairShotSwitchListItem(
        headline = label,
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        onClick = onClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSliderItem(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: (Float) -> String,
    onValueChange: (Float) -> Unit,
    steps: Int = 0,
    onLiveUpdate: ((Float) -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    var sliderValue by remember { mutableFloatStateOf(value) }
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()

    LaunchedEffect(value) {
        if (!isDragged && abs(sliderValue - value) > SLIDER_SYNC_THRESHOLD) sliderValue = value
    }

    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = PairShotCard.innerPadding,
                vertical = PairShotCard.innerPadding,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueLabel(sliderValue),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onLiveUpdate?.invoke(it)
            },
            onValueChangeFinished = { onValueChange(sliderValue) },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    thumbSize = DpSize(PairShotStroke.hairline, PairShotSpacing.lg),
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.graphicsLayer(scaleY = SLIDER_TRACK_SCALE_Y),
                    drawTick = { _, _ -> },
                    drawStopIndicator = null,
                )
            },
        )
        footer?.invoke()
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = PairShotCard.innerPadding),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
