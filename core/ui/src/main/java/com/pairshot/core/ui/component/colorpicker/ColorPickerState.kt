package com.pairshot.core.ui.component.colorpicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

@Stable
class ColorPickerState internal constructor(initialColor: Int) {
    private val initialHsv =
        FloatArray(HSV_COMPONENT_COUNT).also {
            android.graphics.Color.colorToHSV(initialColor, it)
        }

    internal var selectedIdx by mutableIntStateOf(nearestPresetIdx(initialHsv))
        private set
    internal var brightness by mutableFloatStateOf(initialHsv[2].coerceIn(0f, HSV_FULL_VALUE))
        private set

    val currentColor: Color
        get() {
            val hue = HUE_PRESETS[selectedIdx]
            return if (hue == null || hue == -HSV_FULL_VALUE) {
                Color.hsv(0f, 0f, brightness)
            } else {
                Color.hsv(hue, HSV_FULL_VALUE, brightness)
            }
        }

    fun currentArgb(): Int = currentColor.toArgb()

    internal fun onSwatchSelected(idx: Int) {
        when (HUE_PRESETS[idx]) {
            null -> brightness = HSV_FULL_VALUE
            -HSV_FULL_VALUE -> brightness = BRIGHTNESS_DARK_DEFAULT
            else -> Unit
        }
        selectedIdx = idx
    }

    internal fun onBrightnessChange(value: Float) {
        brightness = value
    }
}

@Composable
fun rememberColorPickerState(initialColor: Int): ColorPickerState =
    remember(initialColor) { ColorPickerState(initialColor) }

fun Color.toHexRgbString(): String = "#%06X".format(toArgb() and RGB_CHANNEL_MASK)
