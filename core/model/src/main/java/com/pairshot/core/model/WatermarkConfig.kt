package com.pairshot.core.model

import kotlinx.serialization.Serializable

@Serializable
data class WatermarkConfig(
    val enabled: Boolean = false,
    val type: WatermarkType = WatermarkType.TEXT,
    val text: String = "",
    val alpha: Float = 0.5f,
    val diagonalCount: Int = 10,
    val repeatDensity: Float = 1.5f,
    val textSizeRatio: Float = 0.03f,
    val logoPath: String = "",
    val logoPosition: LogoPosition = LogoPosition.CENTER,
    val logoSizeRatio: Float = 0.5f,
    val logoAlpha: Float = 0.5f,
)

fun WatermarkConfig.isContentMissing(): Boolean =
    when (type) {
        WatermarkType.TEXT -> text.isBlank()
        WatermarkType.LOGO -> logoPath.isBlank()
    }
