package com.pairshot.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ExportPresetSlot(
    val id: String,
    val name: String,
    val exportPreset: ExportPreset = ExportPreset(),
    val watermarkConfig: WatermarkConfig = WatermarkConfig(),
    val combineConfig: CombineConfig = CombineConfig(),
) {
    companion object {
        const val DEFAULT_ID: String = "default"
        const val MAX_SLOTS: Int = 4
        const val FREE_SLOT_LIMIT: Int = 2
        const val MAX_NAME_LENGTH: Int = 12
    }
}
