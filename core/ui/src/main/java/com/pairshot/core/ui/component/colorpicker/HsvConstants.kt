package com.pairshot.core.ui.component.colorpicker

internal const val HUE_DEGREES_MAX = 360f
internal const val HSV_COMPONENT_COUNT = 3
internal const val HSV_GRAYSCALE_THRESHOLD = 0.15f
internal const val HSV_DARK_BRIGHTNESS_START = 0.15f
internal const val HSV_SWATCH_BRIGHTNESS = 0.9f
internal const val HSV_FULL_VALUE = 1f
internal const val BRIGHTNESS_DARK_DEFAULT = 0.05f
internal const val RGB_CHANNEL_MASK = 0xFFFFFF

private const val HUE_PRESET_RED = 0f
private const val HUE_PRESET_ORANGE = 30f
private const val HUE_PRESET_YELLOW = 60f
private const val HUE_PRESET_GREEN = 120f
private const val HUE_PRESET_CYAN = 210f
private const val HUE_PRESET_BLUE = 240f
private const val HUE_PRESET_PURPLE = 270f

internal val HUE_PRESETS: List<Float?> =
    listOf(
        null,
        -HSV_FULL_VALUE,
        HUE_PRESET_RED,
        HUE_PRESET_ORANGE,
        HUE_PRESET_YELLOW,
        HUE_PRESET_GREEN,
        HUE_PRESET_CYAN,
        HUE_PRESET_BLUE,
        HUE_PRESET_PURPLE,
    )

internal fun nearestPresetIdx(hsv: FloatArray): Int {
    val isBlack = hsv[2] < HSV_GRAYSCALE_THRESHOLD && hsv[1] < HSV_GRAYSCALE_THRESHOLD
    val isWhiteish = !isBlack && hsv[1] < HSV_GRAYSCALE_THRESHOLD
    return when {
        isBlack -> 1
        isWhiteish -> 0
        else -> {
            HUE_PRESETS.indices.drop(2).minByOrNull { idx ->
                val hue = HUE_PRESETS[idx] ?: 0f
                val diff = kotlin.math.abs(hue - hsv[0])
                minOf(diff, HUE_DEGREES_MAX - diff)
            } ?: 2
        }
    }
}
