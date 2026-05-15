package com.pairshot.core.entitlement

import com.pairshot.core.billing.BillingRepository
import com.pairshot.core.billing.domain.SubscriptionStatus
import com.pairshot.core.coupon.data.CouponStatusCalculator
import com.pairshot.core.coupon.local.CouponPreferencesSource
import com.pairshot.core.coupon.local.StoredCouponState
import com.pairshot.core.domain.entitlement.EntitlementSource
import com.pairshot.core.domain.entitlement.ProEntitlement
import com.pairshot.core.domain.entitlement.ProEntitlementProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompositeProEntitlementProvider
    @Inject
    constructor(
        private val couponPreferences: CouponPreferencesSource,
        private val billingRepository: BillingRepository,
    ) : ProEntitlementProvider {
        override fun observe(): Flow<ProEntitlement> =
            combine(
                couponPreferences.state,
                billingRepository.subscriptionStatus,
            ) { coupon, sub -> resolve(coupon, sub) }.distinctUntilChanged()

        override suspend fun current(): ProEntitlement = observe().first()

        private fun resolve(
            coupon: StoredCouponState?,
            sub: SubscriptionStatus,
        ): ProEntitlement {
            val now = System.currentTimeMillis()
            return when {
                sub is SubscriptionStatus.Active ->
                    ProEntitlement(true, EntitlementSource.SUBSCRIPTION, null)

                CouponStatusCalculator.isActive(coupon, now) ->
                    ProEntitlement(true, EntitlementSource.COUPON, CouponStatusCalculator.expiresAt(coupon, now))

                else -> ProEntitlement(false, EntitlementSource.NONE, null)
            }
        }
    }
