package com.pairshot.core.domain.export

import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.WatermarkConfig

data class PreparedZip(
    val filePath: String,
    val suggestedName: String,
)

interface ExportRepository {
    suspend fun composeCombinedForGallery(
        pairIds: List<Long>,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        onProgress: (current: Int, total: Int) -> Unit,
    ): Int

    suspend fun saveDecoratedOriginals(
        pairIds: List<Long>,
        preset: ExportPreset,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        onProgress: (current: Int, total: Int) -> Unit,
    ): Int

    suspend fun prepareZipForSave(
        pairIds: List<Long>,
        preset: ExportPreset,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        onProgress: (current: Int, total: Int) -> Unit,
    ): PreparedZip?

    suspend fun discardPreparedZip(filePath: String)

    suspend fun buildShareablePayload(
        pairIds: List<Long>,
        preset: ExportPreset,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        onProgress: (current: Int, total: Int) -> Unit,
    ): ExportAction
}
