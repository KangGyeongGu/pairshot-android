package com.pairshot.core.domain.export

import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.WatermarkConfig
import javax.inject.Inject

sealed interface ExportAction {
    data class ShareImages(
        val uris: List<String>,
    ) : ExportAction

    data class ShareZip(
        val filePath: String,
    ) : ExportAction
}

class ShareSelectionUseCase
@Inject
constructor(
    private val shareExportRepository: ShareExportRepository,
) {
    suspend operator fun invoke(
        pairIds: List<Long>,
        preset: ExportPreset,
        watermarkConfig: WatermarkConfig?,
        combineConfig: CombineConfig,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
    ): ExportAction {
        require(pairIds.isNotEmpty()) { "no pairs to share" }
        require(preset.includeBefore || preset.includeAfter || preset.includeCombined) {
            "at least one include option is required"
        }

        val effectiveCombine = if (preset.applyCombineConfig) combineConfig else CombineConfig.NoDecoration

        return shareExportRepository.buildShareablePayload(
            pairIds = pairIds,
            preset = preset,
            combineConfig = effectiveCombine,
            watermarkConfig = watermarkConfig,
            onProgress = onProgress,
        )
    }
}
