package com.pairshot.core.domain.export

import com.pairshot.core.model.CombineConfig
import com.pairshot.core.model.ExportFormat
import com.pairshot.core.model.PhotoPair
import com.pairshot.core.model.WatermarkConfig

enum class ExportDestination { SHARE, SAVE_TO_DEVICE }

sealed interface ExportItem {
    val pairId: Long

    data class CombinedImage(
        override val pairId: Long,
    ) : ExportItem

    data class BeforeImage(
        override val pairId: Long,
        val sourceUri: String,
    ) : ExportItem

    data class AfterImage(
        override val pairId: Long,
        val sourceUri: String,
    ) : ExportItem
}

data class ExportPlan(
    val destination: ExportDestination,
    val format: ExportFormat,
    val pairs: List<PhotoPair>,
    val items: List<ExportItem>,
    val combineConfig: CombineConfig,
    val watermarkConfig: WatermarkConfig?,
    val applyCombineConfig: Boolean,
) {
    val totalUnits: Int = items.size

    val isEmpty: Boolean = items.isEmpty()
}
