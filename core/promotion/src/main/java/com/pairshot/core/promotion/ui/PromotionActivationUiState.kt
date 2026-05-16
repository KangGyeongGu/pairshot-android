package com.pairshot.core.promotion.ui

import com.pairshot.core.promotion.domain.ActivationResult
import com.pairshot.core.promotion.domain.PromotionEntitlement

sealed interface PromotionActivationUiState {
    data object Idle : PromotionActivationUiState

    data object Loading : PromotionActivationUiState

    data class Success(
        val entitlement: PromotionEntitlement,
        val durationDays: Long?,
    ) : PromotionActivationUiState

    data class Failure(
        val failure: ActivationResult.Failure,
    ) : PromotionActivationUiState
}
