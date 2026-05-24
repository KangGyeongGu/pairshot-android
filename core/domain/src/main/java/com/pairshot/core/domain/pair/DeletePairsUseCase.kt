package com.pairshot.core.domain.pair

import com.pairshot.core.domain.combine.ExportHistoryRepository
import com.pairshot.core.model.PhotoPair
import javax.inject.Inject

data class DeletePairsResult(
    val deleted: Int,
    val failed: Int,
)

class DeletePairsUseCase
@Inject
constructor(
    private val photoPairRepository: PhotoPairRepository,
    private val exportHistoryRepository: ExportHistoryRepository,
) {
    suspend operator fun invoke(pairs: List<PhotoPair>): DeletePairsResult {
        if (pairs.isEmpty()) return DeletePairsResult(deleted = 0, failed = 0)

        runCatching { exportHistoryRepository.deleteByPairIds(pairs.map { it.id }) }

        var deleted = 0
        var failed = 0
        pairs.forEach { pair ->
            val outcome = runCatching { photoPairRepository.delete(pair) }
            if (outcome.isSuccess) deleted++ else failed++
        }
        return DeletePairsResult(deleted = deleted, failed = failed)
    }
}
