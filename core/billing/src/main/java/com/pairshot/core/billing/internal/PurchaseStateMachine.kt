package com.pairshot.core.billing.internal

import com.android.billingclient.api.Purchase
import com.pairshot.core.billing.domain.SubscriptionStatus

object PurchaseStateMachine {
    fun toStatus(purchase: Purchase?): SubscriptionStatus {
        if (purchase == null) return SubscriptionStatus.Inactive
        val productId = purchase.products.firstOrNull() ?: return SubscriptionStatus.Inactive
        return when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                SubscriptionStatus.Active(productId = productId, autoRenew = purchase.isAutoRenewing)
            }

            Purchase.PurchaseState.PENDING -> {
                SubscriptionStatus.Pending(productId)
            }

            else -> {
                SubscriptionStatus.Inactive
            }
        }
    }
}
