package com.pairshot.core.model

data class RenderProfile(
    val maxOutputPx: Int,
) {
    companion object {
        val FULL: RenderProfile = RenderProfile(maxOutputPx = 0)
        val PREVIEW: RenderProfile = RenderProfile(maxOutputPx = 2560)

        fun export(maxOutputPx: Int): RenderProfile = RenderProfile(maxOutputPx = maxOutputPx)

        fun forExport(preset: ImageQualityPreset): RenderProfile = export(preset.maxOutputPx)
    }
}
