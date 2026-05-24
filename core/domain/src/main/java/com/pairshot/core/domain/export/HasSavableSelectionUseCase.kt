package com.pairshot.core.domain.export

import com.pairshot.core.domain.pair.PhotoPairRepository
import com.pairshot.core.model.ExportFormat
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.WatermarkConfig
import javax.inject.Inject

class HasSavableSelectionUseCase
@Inject
constructor(
    private val photoPairRepository: PhotoPairRepository,
) {
    suspend operator fun invoke(
        pairIds: List<Long>,
        preset: ExportPreset,
        watermarkConfig: WatermarkConfig?,
    ): Boolean {
        if (pairIds.isEmpty()) return false
        val pairs = photoPairRepository.getByIds(pairIds)
        return pairs.any { pair ->
            val beforeValid = !pair.beforePhotoUri.isNullOrBlank()
            val afterValid = !pair.afterPhotoUri.isNullOrBlank()
            canProduceOutput(preset, watermarkConfig, beforeValid, afterValid)
        }
    }

    private fun canProduceOutput(
        preset: ExportPreset,
        watermarkConfig: WatermarkConfig?,
        beforeValid: Boolean,
        afterValid: Boolean,
    ): Boolean =
        canProduceCombined(preset, beforeValid, afterValid) ||
            canProduceWatermarked(preset, watermarkConfig, beforeValid, afterValid) ||
            canProduceZipOriginals(preset, beforeValid, afterValid)

    private fun canProduceCombined(
        preset: ExportPreset,
        beforeValid: Boolean,
        afterValid: Boolean,
    ): Boolean = preset.includeCombined && beforeValid && afterValid

    private fun canProduceWatermarked(
        preset: ExportPreset,
        watermarkConfig: WatermarkConfig?,
        beforeValid: Boolean,
        afterValid: Boolean,
    ): Boolean {
        if (watermarkConfig == null) return false
        val canBefore = preset.includeBefore && beforeValid
        val canAfter = preset.includeAfter && afterValid
        return canBefore || canAfter
    }

    private fun canProduceZipOriginals(
        preset: ExportPreset,
        beforeValid: Boolean,
        afterValid: Boolean,
    ): Boolean {
        if (preset.format != ExportFormat.ZIP) return false
        val canBefore = preset.includeBefore && beforeValid
        val canAfter = preset.includeAfter && afterValid
        return canBefore || canAfter
    }
}
