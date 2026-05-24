package com.pairshot.core.domain.combine

import com.pairshot.core.model.ExportHistoryKind
import javax.inject.Inject

class DeleteCombinedPhotosUseCase
@Inject
constructor(
    private val exportHistoryRepository: ExportHistoryRepository,
) {
    suspend operator fun invoke(pairIds: List<Long>) {
        runCatching {
            exportHistoryRepository.deleteByPairIdsAndKind(pairIds, ExportHistoryKind.COMBINED)
        }
    }
}
