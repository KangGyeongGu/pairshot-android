package com.pairshot.core.promotion.domain

import kotlinx.serialization.Serializable

/**
 * Snapshot of the user's promotion-side membership as reported by the server.
 * Subscription is NOT considered here — composition with billing happens at the entitlement layer.
 */
@Serializable
data class PromotionState(
    val proActive: Boolean,
    val proExpiresAtEpochMillis: Long?,
    val adFreeActive: Boolean,
    val adFreeExpiresAtEpochMillis: Long?,
    val promotions: List<Promotion>,
) {
    companion object {
        val Empty: PromotionState =
            PromotionState(
                proActive = false,
                proExpiresAtEpochMillis = null,
                adFreeActive = false,
                adFreeExpiresAtEpochMillis = null,
                promotions = emptyList(),
            )
    }
}
