package com.pairshot.core.promotion.domain

import kotlinx.coroutines.flow.Flow

interface PromotionRepository {
    fun observe(): Flow<PromotionState>

    suspend fun refresh()

    suspend fun activate(code: String): ActivationResult

    suspend fun retryPendingIfAny()

    suspend fun clear()
}
