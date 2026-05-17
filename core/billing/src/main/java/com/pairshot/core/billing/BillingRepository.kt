package com.pairshot.core.billing

import android.app.Activity
import android.content.Intent
import com.pairshot.core.billing.domain.BillingOffer
import com.pairshot.core.billing.domain.PurchaseError
import com.pairshot.core.billing.domain.SubscriptionStatus
import kotlinx.coroutines.flow.StateFlow

sealed interface PurchaseLaunchResult {
    data object Launched : PurchaseLaunchResult

    data object AlreadyOwned : PurchaseLaunchResult

    data class Failed(
        val error: PurchaseError,
    ) : PurchaseLaunchResult
}

interface BillingRepository {
    val subscriptionStatus: StateFlow<SubscriptionStatus>

    fun start()

    suspend fun refresh()

    suspend fun loadOffers(): Result<List<BillingOffer>>

    suspend fun launchPurchaseFlow(
        activity: Activity,
        offer: BillingOffer,
    ): PurchaseLaunchResult

    fun manageSubscriptionsIntent(productId: String? = null): Intent
}
