package com.pairshot.core.entitlement

import com.pairshot.core.billing.BillingRepository
import com.pairshot.core.billing.domain.SubscriptionStatus
import com.pairshot.core.domain.membership.Membership
import com.pairshot.core.domain.membership.MembershipProvider
import com.pairshot.core.promotion.domain.PromotionRepository
import com.pairshot.core.promotion.domain.PromotionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MembershipResolver
@Inject
constructor(
    private val billingRepository: BillingRepository,
    private val promotionRepository: PromotionRepository,
) : MembershipProvider {
    override fun observe(): Flow<Membership> =
        combine(
            billingRepository.subscriptionStatus,
            promotionRepository.observe(),
        ) { sub, promo -> resolve(sub, promo) }.distinctUntilChanged()

    override suspend fun current(): Membership = observe().first()

    private fun resolve(
        sub: SubscriptionStatus,
        promo: PromotionState,
    ): Membership {
        val subActive = sub is SubscriptionStatus.Active
        val isPro = subActive || promo.proActive
        val proExpiry = if (subActive) null else promo.proExpiresAtEpochMillis
        return Membership(
            isPro = isPro,
            proExpiresAtEpochMillis = proExpiry,
        )
    }
}
