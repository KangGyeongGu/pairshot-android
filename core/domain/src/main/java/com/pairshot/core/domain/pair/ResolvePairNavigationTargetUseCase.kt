package com.pairshot.core.domain.pair

import com.pairshot.core.model.PairStatus
import com.pairshot.core.model.PhotoPair
import javax.inject.Inject

class ResolvePairNavigationTargetUseCase
@Inject
constructor() {
    operator fun invoke(pair: PhotoPair): PairNavigationTarget =
        when (pair.status) {
            PairStatus.BEFORE_ONLY -> PairNavigationTarget.AfterCamera(pair.id)
            PairStatus.AFTER_ONLY -> PairNavigationTarget.BeforeRetakeCamera(pair.id)
            PairStatus.PAIRED -> PairNavigationTarget.PairPreview(pair.id)
        }
}
