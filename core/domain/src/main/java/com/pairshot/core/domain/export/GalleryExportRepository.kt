package com.pairshot.core.domain.export

import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.WatermarkConfig

interface GalleryExportRepository {
    suspend fun composeCombinedForGallery(
        pairIds: List<Long>,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        progressBase: Int = 0,
        progressTotal: Int = pairIds.size,
        onProgress: (current: Int, total: Int) -> Unit,
    ): Int

    suspend fun saveDecoratedOriginals(
        pairIds: List<Long>,
        preset: ExportPreset,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        progressBase: Int = 0,
        progressTotal: Int = pairIds.size,
        onProgress: (current: Int, total: Int) -> Unit,
    ): Int
}
