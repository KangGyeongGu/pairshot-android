package com.pairshot.core.promotion.domain

import javax.inject.Inject

class ActivatePromotionUseCase
@Inject
constructor(
    private val repository: PromotionRepository,
) {
    suspend operator fun invoke(code: String): ActivationResult = repository.activate(code)
}
