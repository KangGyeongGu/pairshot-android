package com.pairshot.core.rendering

private const val ROTATE_QUARTER_DEG = 90
private const val ROTATE_HALF_DEG = 180
private const val ROTATE_THREE_QUARTER_DEG = 270

object OverlayTransformCalculator {
    const val LANDSCAPE_LEFT_ROTATION = 90f
    const val LANDSCAPE_RIGHT_ROTATION = 270f

    fun fromImageOrientation(
        pixelWidth: Int,
        pixelHeight: Int,
        exifDegrees: Int,
    ): Float {
        val swapped = exifDegrees == ROTATE_QUARTER_DEG || exifDegrees == ROTATE_THREE_QUARTER_DEG
        val correctedWidth = if (swapped) pixelHeight else pixelWidth
        val correctedHeight = if (swapped) pixelWidth else pixelHeight
        if (correctedHeight >= correctedWidth) return 0f
        return if (exifDegrees == ROTATE_HALF_DEG) LANDSCAPE_RIGHT_ROTATION else LANDSCAPE_LEFT_ROTATION
    }

    fun fromExifOnly(exifDegrees: Int): Float =
        when (exifDegrees) {
            ROTATE_QUARTER_DEG, ROTATE_THREE_QUARTER_DEG -> 0f
            ROTATE_HALF_DEG -> LANDSCAPE_RIGHT_ROTATION
            else -> LANDSCAPE_LEFT_ROTATION
        }
}
