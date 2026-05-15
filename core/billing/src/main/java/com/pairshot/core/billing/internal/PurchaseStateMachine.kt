package com.pairshot.core.billing.internal

import com.android.billingclient.api.Purchase
import com.pairshot.core.billing.domain.SubscriptionStatus

object PurchaseStateMachine {
    private const val DEFAULT_RENEWAL_WINDOW_MS = 30L * 24 * 60 * 60 * 1000

    fun toStatus(purchase: Purchase?): SubscriptionStatus {
        if (purchase == null) return SubscriptionStatus.Inactive
        val productId = purchase.products.firstOrNull() ?: return SubscriptionStatus.Inactive
        return when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED ->
                SubscriptionStatus.Active(
                    productId = productId,
                    expiryEpochMs = purchase.purchaseTime + DEFAULT_RENEWAL_WINDOW_MS,
                    autoRenew = purchase.isAutoRenewing,
                )

            Purchase.PurchaseState.PENDING -> SubscriptionStatus.Pending(productId)
            else -> SubscriptionStatus.Inactive
        }
    }
}
