package com.pairshot.core.domain.export

import com.pairshot.core.domain.membership.MembershipProvider
import com.pairshot.core.domain.pair.PhotoPairRepository
import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ExportFormat
import com.pairshot.core.model.ExportPreset
import com.pairshot.core.model.PhotoPair
import com.pairshot.core.model.WatermarkConfig
import javax.inject.Inject

class PlanExportUseCase
@Inject
constructor(
    private val photoPairRepository: PhotoPairRepository,
    private val membershipProvider: MembershipProvider,
) {
    suspend operator fun invoke(
        pairIds: List<Long>,
        preset: ExportPreset,
        combineConfig: CombineConfig,
        watermarkConfig: WatermarkConfig?,
        destination: ExportDestination,
    ): ExportPlan {
        require(pairIds.isNotEmpty()) { "pairIds must not be empty" }

        val isPro = membershipProvider.current().isPro
        val effectiveFormat = enforceProFormat(preset.format, isPro)
        val effectiveWatermark = enforceProWatermark(watermarkConfig, isPro)
        val effectiveCombine = enforceProCombine(combineConfig, isPro)

        val pairs = photoPairRepository.getByIds(pairIds)
        val baseCombine = if (preset.applyCombineConfig) effectiveCombine else CombineConfig.NoDecoration

        val items = buildList {
            pairs.forEach { pair ->
                if (preset.includeCombined && pair.hasBoth()) {
                    add(ExportItem.CombinedImage(pairId = pair.id))
                }
                if (preset.includeBefore) {
                    pair.beforePhotoUri?.takeIf { it.isNotBlank() }?.let { uri ->
                        add(ExportItem.BeforeImage(pairId = pair.id, sourceUri = uri))
                    }
                }
                if (preset.includeAfter) {
                    pair.afterPhotoUri?.takeIf { it.isNotBlank() }?.let { uri ->
                        add(ExportItem.AfterImage(pairId = pair.id, sourceUri = uri))
                    }
                }
            }
        }

        return ExportPlan(
            destination = destination,
            format = effectiveFormat,
            pairs = pairs,
            items = items,
            combineConfig = baseCombine,
            watermarkConfig = effectiveWatermark,
            applyCombineConfig = preset.applyCombineConfig,
        )
    }

    private fun PhotoPair.hasBoth(): Boolean =
        !beforePhotoUri.isNullOrBlank() && !afterPhotoUri.isNullOrBlank()

    private fun enforceProFormat(
        format: ExportFormat,
        isPro: Boolean,
    ): ExportFormat = if (!isPro && format == ExportFormat.ZIP) ExportFormat.INDIVIDUAL else format

    private fun enforceProWatermark(
        config: WatermarkConfig?,
        isPro: Boolean,
    ): WatermarkConfig? =
        if (isPro || config == null) config else WatermarkConfig(enabled = config.enabled)

    private fun enforceProCombine(
        config: CombineConfig,
        isPro: Boolean,
    ): CombineConfig = if (isPro) config else CombineConfig()
}
