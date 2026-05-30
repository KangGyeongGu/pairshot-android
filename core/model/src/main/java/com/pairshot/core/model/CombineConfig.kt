package com.pairshot.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CombineConfig(
    val layout: CombineLayout = CombineLayout.HORIZONTAL,
    val borderEnabled: Boolean = true,
    val borderThicknessDp: Int = 16,
    val borderColorArgb: Int = 0xFFFFFFFF.toInt(),
    val labelEnabled: Boolean = false,
    val beforeLabel: String = "BEFORE",
    val afterLabel: String = "AFTER",
    val labelPlacement: LabelPlacement = LabelPlacement.INSIDE_IMAGE,
    val labelPositionMode: LabelPositionMode = LabelPositionMode.FREE,
    val labelPosition: LabelPosition = LabelPosition.BOTTOM,
    val beforeLabelAnchor: LabelAnchor = LabelAnchor.TOP_LEFT,
    val afterLabelAnchor: LabelAnchor = LabelAnchor.TOP_LEFT,
    val labelBgEnabled: Boolean = true,
    val labelBgCornerDp: Int = 25,
    val labelSizeRatio: Float = 0.05f,
    val labelTextColorArgb: Int = 0xFF000000.toInt(),
    val labelBgColorArgb: Int = 0xFF000000.toInt(),
    val labelBgAlpha: Float = 0.5f,
    val labelBgMatchesBorder: Boolean = true,
) {
    companion object {
        val NoDecoration: CombineConfig =
            CombineConfig(
                borderEnabled = false,
                labelEnabled = false,
            )
    }
}

fun CombineConfig.effectiveLabelBgColor(): Int = if (labelBgMatchesBorder && borderEnabled) borderColorArgb else labelBgColorArgb
