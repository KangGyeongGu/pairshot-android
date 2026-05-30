package com.pairshot.core.domain.export

import com.pairshot.core.domain.membership.MembershipProvider
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ExportFormat
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.WatermarkConfig
import javax.inject.Inject

sealed interface SaveToDeviceResult {
    data class SavedImagesToGallery(
        val count: Int,
    ) : SaveToDeviceResult

    data class ZipReadyForSave(
        val filePath: String,
        val suggestedName: String,
    ) : SaveToDeviceResult

    data object Nothing : SaveToDeviceResult
}

class SaveSelectionToDeviceUseCase
@Inject
constructor(
    private val galleryExportRepository: GalleryExportRepository,
    private val zipExportRepository: ZipExportRepository,
    private val membershipProvider: MembershipProvider,
) {
    suspend operator fun invoke(
        pairIds: List<Long>,
        preset: ExportPreset,
        watermarkConfig: WatermarkConfig?,
        combineConfig: CombineConfig,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
    ): SaveToDeviceResult {
        require(pairIds.isNotEmpty()) { "no pairs to export" }

        val isPro = membershipProvider.current().isPro
        val baseCombine = if (preset.applyCombineConfig) combineConfig else CombineConfig.NoDecoration
        val effectiveCombine = enforceProCombine(baseCombine, isPro)
        val effectiveWatermark = enforceProWatermark(watermarkConfig, isPro)
        val effectiveFormat = enforceProFormat(preset.format, isPro)

        return when (effectiveFormat) {
            ExportFormat.ZIP -> saveZip(pairIds, preset, effectiveCombine, effectiveWatermark, onProgress)
            ExportFormat.INDIVIDUAL -> saveIndividuals(
                pairIds,
                preset,
                effectiveCombine,
                effectiveWatermark,
                onProgress,
            )
        }
    }

    private fun enforceProFormat(
        format: ExportFormat,
        isPro: Boolean,
    ): ExportFormat =
        if (!isPro && format == ExportFormat.ZIP) ExportFormat.INDIVIDUAL else format

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

    private suspend fun saveZip(
        pairIds: List<Long>,
        preset: ExportPreset,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        onProgress: (current: Int, total: Int) -> Unit,
    ): SaveToDeviceResult {
        val prepared =
            zipExportRepository.prepareZipForSave(
                pairIds = pairIds,
                preset = preset,
                combineConfig = combineConfig,
                watermarkConfig = watermarkConfig,
                onProgress = onProgress,
            ) ?: return SaveToDeviceResult.Nothing
        return SaveToDeviceResult.ZipReadyForSave(
            filePath = prepared.filePath,
            suggestedName = prepared.suggestedName,
        )
    }

    private suspend fun saveIndividuals(
        pairIds: List<Long>,
        preset: ExportPreset,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        onProgress: (current: Int, total: Int) -> Unit,
    ): SaveToDeviceResult {
        val combinedCount =
            if (preset.includeCombined) {
                galleryExportRepository.composeCombinedForGallery(
                    pairIds = pairIds,
                    combineConfig = combineConfig,
                    watermarkConfig = watermarkConfig,
                    onProgress = onProgress,
                )
            } else {
                0
            }

        val individualCount =
            if ((preset.includeBefore || preset.includeAfter) &&
                needsIndividualDecoration(combineConfig, watermarkConfig)
            ) {
                galleryExportRepository.saveDecoratedOriginals(
                    pairIds = pairIds,
                    preset = preset,
                    combineConfig = combineConfig,
                    watermarkConfig = watermarkConfig,
                    onProgress = onProgress,
                )
            } else {
                0
            }

        val total = combinedCount + individualCount
        return if (total > 0) SaveToDeviceResult.SavedImagesToGallery(total) else SaveToDeviceResult.Nothing
    }
}

fun needsIndividualDecoration(
    combineConfig: CombineConfig,
    watermarkConfig: WatermarkConfig?,
): Boolean {
    val watermarkOn = watermarkConfig != null && watermarkConfig.enabled
    val borderOn = combineConfig.borderEnabled
    val labelOn = combineConfig.labelEnabled
    return watermarkOn || borderOn || labelOn
}
