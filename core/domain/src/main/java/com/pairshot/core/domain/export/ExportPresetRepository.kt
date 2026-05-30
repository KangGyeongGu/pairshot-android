package com.pairshot.core.domain.export

import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.ExportPresetSlot
import com.pairshot.core.model.WatermarkConfig
import kotlinx.coroutines.flow.Flow

interface ExportPresetRepository {
    val slotsFlow: Flow<List<ExportPresetSlot>>

    val activeSlotIdFlow: Flow<String>

    val activeSlotFlow: Flow<ExportPresetSlot>

    suspend fun getSlots(): List<ExportPresetSlot>

    suspend fun getActiveSlot(): ExportPresetSlot

    suspend fun createSlot(name: String): Result<String>

    suspend fun renameSlot(
        id: String,
        name: String,
    ): Result<Unit>

    suspend fun deleteSlot(id: String): Result<Unit>

    suspend fun selectSlot(id: String): Result<Unit>

    suspend fun syncActiveSlotExport(preset: ExportPreset)

    suspend fun syncActiveSlotWatermark(config: WatermarkConfig)

    suspend fun syncActiveSlotCombine(config: CombineConfig)
}
