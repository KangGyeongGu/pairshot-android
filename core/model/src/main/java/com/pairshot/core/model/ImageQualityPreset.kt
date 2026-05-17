package com.pairshot.core.model

enum class ImageQualityPreset(
    val maxOutputPx: Int,
    val jpegQuality: Int,
) {
    LOW(maxOutputPx = 2560, jpegQuality = 75),
    HIGH(maxOutputPx = 4096, jpegQuality = 85),
    BEST(maxOutputPx = 0, jpegQuality = 100),
    ;

    companion object {
        val DEFAULT: ImageQualityPreset = BEST

        private const val LEGACY_JPEG_LOW_MAX = 80
        private const val LEGACY_JPEG_BEST_MIN = 93

        fun fromName(name: String?): ImageQualityPreset = entries.firstOrNull { it.name == name } ?: DEFAULT

        fun fromLegacyJpegQuality(quality: Int): ImageQualityPreset =
            when {
                quality <= LEGACY_JPEG_LOW_MAX -> LOW
                quality >= LEGACY_JPEG_BEST_MIN -> BEST
                else -> HIGH
            }
    }
}
