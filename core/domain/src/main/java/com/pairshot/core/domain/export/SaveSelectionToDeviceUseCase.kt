package com.pairshot.core.domain.export

import com.pairshot.core.domain.entitlement.ProEntitlementProvider
import com.pairshot.core.domain.entitlement.isPaidSubscriber
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
        private val exportRepository: ExportRepository,
        private val entitlementProvider: ProEntitlementProvider,
    ) {
        suspend operator fun invoke(
            pairIds: List<Long>,
            preset: ExportPreset,
            watermarkConfig: WatermarkConfig?,
            combineConfig: CombineConfig,
            onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
        ): SaveToDeviceResult {
            require(pairIds.isNotEmpty()) { "no pairs to export" }

            val effectiveCombine = if (preset.applyCombineConfig) combineConfig else CombineConfig()
            val effectiveFormat = enforceProFormat(preset.format)

            return when (effectiveFormat) {
                ExportFormat.ZIP -> saveZip(pairIds, preset, effectiveCombine, watermarkConfig, onProgress)
                ExportFormat.INDIVIDUAL -> saveIndividuals(pairIds, preset, effectiveCombine, watermarkConfig, onProgress)
            }
        }

        private suspend fun enforceProFormat(format: ExportFormat): ExportFormat =
            if (format == ExportFormat.ZIP && !entitlementProvider.current().isPaidSubscriber) {
                ExportFormat.INDIVIDUAL
            } else {
                format
            }

        private suspend fun saveZip(
            pairIds: List<Long>,
            preset: ExportPreset,
            combineConfig: CombineConfig,
            watermarkConfig: WatermarkConfig?,
            onProgress: (current: Int, total: Int) -> Unit,
        ): SaveToDeviceResult {
            val prepared =
                exportRepository.prepareZipForSave(
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
                    exportRepository.composeCombinedForGallery(
                        pairIds = pairIds,
                        combineConfig = combineConfig,
                        watermarkConfig = watermarkConfig,
                        onProgress = onProgress,
                    )
                } else {
                    0
                }

            val watermarkedCount =
                if (watermarkConfig != null && (preset.includeBefore || preset.includeAfter)) {
                    exportRepository.saveWatermarkedOriginals(
                        pairIds = pairIds,
                        preset = preset,
                        watermarkConfig = watermarkConfig,
                        onProgress = onProgress,
                    )
                } else {
                    0
                }

            val total = combinedCount + watermarkedCount
            return if (total > 0) SaveToDeviceResult.SavedImagesToGallery(total) else SaveToDeviceResult.Nothing
        }
    }
