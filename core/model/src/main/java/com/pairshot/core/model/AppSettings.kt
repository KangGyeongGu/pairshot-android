package com.pairshot.core.model

data class AppSettings(
    val imageQuality: ImageQualityPreset = ImageQualityPreset.DEFAULT,
    val fileNamePrefix: String = DEFAULT_FILE_NAME_PREFIX,
    val overlayEnabled: Boolean = true,
    val defaultOverlayAlpha: Float = DEFAULT_OVERLAY_ALPHA,
    val cameraGridEnabled: Boolean = false,
    val cameraLevelEnabled: Boolean = false,
    val cameraFlashMode: FlashMode = FlashMode.DEFAULT,
    val cameraNightModeEnabled: Boolean = false,
    val cameraHdrEnabled: Boolean = false,
    val cameraAspectRatio: AspectRatio = AspectRatio.DEFAULT,
) {
    companion object {
        const val DEFAULT_FILE_NAME_PREFIX = "PAIRSHOT"
        const val DEFAULT_OVERLAY_ALPHA = 0.35f
    }
}
