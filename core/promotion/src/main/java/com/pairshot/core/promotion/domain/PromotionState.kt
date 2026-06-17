package com.pairshot.core.promotion.domain

import kotlinx.serialization.Serializable

@Serializable
data class PromotionState(
    val proActive: Boolean,
    val proExpiresAtEpochMillis: Long?,
    val promotions: List<Promotion>,
) {
    companion object {
        val Empty: PromotionState =
            PromotionState(
                proActive = false,
                proExpiresAtEpochMillis = null,
                promotions = emptyList(),
            )
    }
}
