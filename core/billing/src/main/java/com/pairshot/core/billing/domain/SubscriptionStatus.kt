package com.pairshot.core.billing.domain

sealed interface SubscriptionStatus {
    data object Inactive : SubscriptionStatus

    data class Pending(val productId: String) : SubscriptionStatus

    data class Active(
        val productId: String,
        val expiryEpochMs: Long,
        val autoRenew: Boolean,
    ) : SubscriptionStatus
}

val SubscriptionStatus.isPro: Boolean
    get() = this is SubscriptionStatus.Active
