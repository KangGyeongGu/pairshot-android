package com.pairshot.core.billing.data.repository

import android.app.Activity
import android.content.Intent
import com.pairshot.core.billing.BillingProductCatalog
import com.pairshot.core.billing.BillingRepository
import com.pairshot.core.billing.domain.BillingOffer
import com.pairshot.core.billing.domain.SubscriptionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeBillingRepository
    @Inject
    constructor() : BillingRepository {
        private val _status = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.Inactive)
        override val subscriptionStatus: StateFlow<SubscriptionStatus> = _status.asStateFlow()

        override fun start() = Unit

        override suspend fun refresh() = Unit

        override suspend fun loadOffers(): Result<List<BillingOffer>> {
            delay(LOAD_DELAY_MS)
            val offers =
                listOf(
                    BillingOffer(
                        productId = BillingProductCatalog.PRO_SUBSCRIPTION,
                        basePlanId = BillingProductCatalog.BASE_PLAN_MONTHLY,
                        offerId = BillingProductCatalog.OFFER_TRIAL14,
                        offerToken = "fake-trial-token",
                        priceFormatted = "₩4,500",
                        priceAmountMicros = MONTHLY_PRICE_MICROS,
                        priceCurrencyCode = "KRW",
                        billingPeriodIso = "P1M",
                        trialDays = TRIAL_DAYS,
                    ),
                    BillingOffer(
                        productId = BillingProductCatalog.PRO_SUBSCRIPTION,
                        basePlanId = BillingProductCatalog.BASE_PLAN_YEARLY,
                        offerId = null,
                        offerToken = "fake-yearly-token",
                        priceFormatted = "₩43,200",
                        priceAmountMicros = YEARLY_PRICE_MICROS,
                        priceCurrencyCode = "KRW",
                        billingPeriodIso = "P1Y",
                        trialDays = null,
                    ),
                )
            return Result.success(offers)
        }

        override suspend fun launchPurchaseFlow(
            activity: Activity,
            offer: BillingOffer,
        ): Result<Unit> {
            delay(PURCHASE_DELAY_MS)
            val now = System.currentTimeMillis()
            val window = if (offer.basePlanId == BillingProductCatalog.BASE_PLAN_YEARLY) YEARLY_WINDOW_MS else MONTHLY_WINDOW_MS
            _status.value =
                SubscriptionStatus.Active(
                    productId = offer.productId,
                    expiryEpochMs = now + window,
                    autoRenew = true,
                )
            return Result.success(Unit)
        }

        override fun manageSubscriptionsIntent(productId: String?): Intent = Intent()

        private companion object {
            const val LOAD_DELAY_MS = 200L
            const val PURCHASE_DELAY_MS = 500L
            const val TRIAL_DAYS = 14
            const val MONTHLY_PRICE_MICROS = 4_500_000_000L
            const val YEARLY_PRICE_MICROS = 43_200_000_000L
            const val DAY_MS = 24L * 60 * 60 * 1000
            const val MONTHLY_WINDOW_MS = 30L * DAY_MS
            const val YEARLY_WINDOW_MS = 365L * DAY_MS
        }
    }
