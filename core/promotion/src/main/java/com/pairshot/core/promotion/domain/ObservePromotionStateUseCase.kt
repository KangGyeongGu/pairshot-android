package com.pairshot.core.promotion.domain

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePromotionStateUseCase
@Inject
constructor(
    private val repository: PromotionRepository,
) {
    operator fun invoke(): Flow<PromotionState> = repository.observe()
}
