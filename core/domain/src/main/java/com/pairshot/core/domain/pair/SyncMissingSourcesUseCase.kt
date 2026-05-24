package com.pairshot.core.domain.pair

import javax.inject.Inject

class SyncMissingSourcesUseCase
@Inject
constructor(
    private val photoPairRepository: PhotoPairRepository,
) {
    suspend operator fun invoke(): SyncMissingSourcesResult {
        val results = photoPairRepository.pruneAllMissingSources()
        var beforeDropped = 0
        var afterDropped = 0
        var deleted = 0
        results.forEach { result ->
            when (result) {
                PrunePairResult.BeforeDropped -> beforeDropped += 1
                PrunePairResult.AfterDropped -> afterDropped += 1
                PrunePairResult.DeletedEntirely -> deleted += 1
                PrunePairResult.Healthy, PrunePairResult.NotFound -> Unit
            }
        }
        return SyncMissingSourcesResult(
            beforeDropped = beforeDropped,
            afterDropped = afterDropped,
            deletedEntirely = deleted,
        )
    }
}

data class SyncMissingSourcesResult(
    val beforeDropped: Int,
    val afterDropped: Int,
    val deletedEntirely: Int,
) {
    val anyChanged: Boolean
        get() = beforeDropped + afterDropped + deletedEntirely > 0
}
