package com.pairshot.core.model

data class AppSettings(
    val jpegQuality: Int = 95,
    val fileNamePrefix: String = "PAIRSHOT",
    val overlayEnabled: Boolean = true,
    val defaultOverlayAlpha: Float = 0.35f,
    val cameraGridEnabled: Boolean = false,
    val cameraLevelEnabled: Boolean = false,
    val cameraFlashMode: String = "OFF",
    val cameraNightModeEnabled: Boolean = false,
    val cameraHdrEnabled: Boolean = false,
    val cameraAspectRatio: AspectRatio = AspectRatio.RATIO_4_3,
)
