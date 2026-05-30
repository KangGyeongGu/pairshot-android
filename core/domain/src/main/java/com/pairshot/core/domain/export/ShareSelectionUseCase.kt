package com.pairshot.core.domain.export

import com.pairshot.core.domain.membership.MembershipProvider
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ExportFormat
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
    private val membershipProvider: MembershipProvider,
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

        val isPro = membershipProvider.current().isPro
        val baseCombine = if (preset.applyCombineConfig) combineConfig else CombineConfig.NoDecoration
        val effectivePreset = enforceProFormat(preset, isPro)
        val effectiveCombine = enforceProCombine(baseCombine, isPro)
        val effectiveWatermark = enforceProWatermark(watermarkConfig, isPro)

        return shareExportRepository.buildShareablePayload(
            pairIds = pairIds,
            preset = effectivePreset,
            combineConfig = effectiveCombine,
            watermarkConfig = effectiveWatermark,
            onProgress = onProgress,
        )
    }

    private fun enforceProFormat(
        preset: ExportPreset,
        isPro: Boolean,
    ): ExportPreset =
        if (!isPro && preset.format == ExportFormat.ZIP) {
            preset.copy(format = ExportFormat.INDIVIDUAL)
        } else {
            preset
        }

    private fun enforceProWatermark(
        config: WatermarkConfig?,
        isPro: Boolean,
    ): WatermarkConfig? =
        if (isPro || config == null) {
            config
        } else {
            WatermarkConfig(enabled = config.enabled)
        }

    private fun enforceProCombine(
        config: CombineConfig,
        isPro: Boolean,
    ): CombineConfig = if (isPro) config else CombineConfig()
}
