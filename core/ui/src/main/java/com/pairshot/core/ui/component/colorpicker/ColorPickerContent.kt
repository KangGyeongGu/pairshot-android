package com.pairshot.core.ui.component.colorpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.pairshot.core.designsystem.PairShotSpacing
import com.pairshot.core.designsystem.PairShotStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerContent(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
) {
    val selectedHue = HUE_PRESETS[state.selectedIdx]
    val isGrayscale = selectedHue == null || selectedHue == -HSV_FULL_VALUE
    val gradientStart =
        if (isGrayscale) {
            Color.Black
        } else {
            Color.hsv(selectedHue!!, HSV_FULL_VALUE, HSV_DARK_BRIGHTNESS_START)
        }
    val gradientEnd =
        if (isGrayscale) {
            Color.White
        } else {
            Color.hsv(selectedHue!!, HSV_FULL_VALUE, HSV_FULL_VALUE)
        }
    val sliderRange = if (isGrayscale) 0f..HSV_FULL_VALUE else HSV_DARK_BRIGHTNESS_START..HSV_FULL_VALUE
    val currentColor = state.currentColor

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(PairShotSpacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.xs),
        ) {
            HUE_PRESETS.forEachIndexed { idx, hue ->
                val swatchColor =
                    when {
                        hue == null -> Color.White
                        hue == -HSV_FULL_VALUE -> Color.Black
                        else -> Color.hsv(hue, HSV_FULL_VALUE, HSV_SWATCH_BRIGHTNESS)
                    }
                val isSelected = idx == state.selectedIdx
                val needsOutline = hue == null || hue == -HSV_FULL_VALUE
                Box(
                    modifier =
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.small)
                        .background(swatchColor)
                        .then(
                            when {
                                isSelected ->
                                    Modifier.border(
                                        PairShotStroke.thin,
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.shapes.small,
                                    )

                                needsOutline ->
                                    Modifier.border(
                                        PairShotStroke.hairline,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        MaterialTheme.shapes.small,
                                    )

                                else -> Modifier
                            },
                        ).clickable { state.onSwatchSelected(idx) },
                )
            }
        }

        Slider(
            value = state.brightness,
            onValueChange = { state.onBrightnessChange(it) },
            valueRange = sliderRange,
            track = { _ ->
                Box(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(PairShotSpacing.md)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(gradientStart, gradientEnd))),
                )
            },
            thumb = { _ ->
                Box(
                    modifier =
                    Modifier
                        .size(PairShotSpacing.lg)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(PairShotStroke.thin, MaterialTheme.colorScheme.outline, CircleShape),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PairShotSpacing.sm),
        ) {
            Box(
                modifier =
                Modifier
                    .size(PairShotSpacing.xl)
                    .clip(MaterialTheme.shapes.small)
                    .background(currentColor)
                    .border(
                        PairShotStroke.hairline,
                        MaterialTheme.colorScheme.outline,
                        MaterialTheme.shapes.small,
                    ),
            )
            Text(
                text = currentColor.toHexRgbString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
