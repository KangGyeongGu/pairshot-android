package com.pairshot.core.domain.export

import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.WatermarkConfig

interface ShareExportRepository {
    suspend fun buildShareablePayload(
        pairIds: List<Long>,
        preset: ExportPreset,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        onProgress: (current: Int, total: Int) -> Unit,
    ): ExportAction
}
