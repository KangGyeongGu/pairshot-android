package com.pairshot.core.entitlement

import app.cash.turbine.test
import com.pairshot.core.billing.BillingRepository
import com.pairshot.core.billing.domain.SubscriptionStatus
import com.pairshot.core.promotion.domain.PromotionRepository
import com.pairshot.core.promotion.domain.PromotionState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MembershipResolverTest {
    private val billing: BillingRepository = mockk()
    private val promotion: PromotionRepository = mockk()
    private val subFlow = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.Inactive)

    private fun resolver(): MembershipResolver {
        every { billing.subscriptionStatus } returns subFlow
        return MembershipResolver(billing, promotion)
    }

    @Test
    fun `no sub and no promotion yields free`() =
        runTest {
            every { promotion.observe() } returns flowOf(PromotionState.Empty)
            subFlow.value = SubscriptionStatus.Inactive
            resolver().observe().test {
                val m = awaitItem()
                assertFalse(m.isPro)
                assertNull(m.proExpiresAtEpochMillis)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `active subscription grants pro with unlimited expiry`() =
        runTest {
            every { promotion.observe() } returns flowOf(PromotionState.Empty)
            subFlow.value = SubscriptionStatus.Active("pro", autoRenew = true)
            resolver().observe().test {
                val m = awaitItem()
                assertTrue(m.isPro)
                assertNull(m.proExpiresAtEpochMillis)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `pro promotion without subscription grants pro with promotion expiry`() =
        runTest {
            val expiry = 1_700_000_000_000L
            every { promotion.observe() } returns
                flowOf(
                    PromotionState(
                        proActive = true,
                        proExpiresAtEpochMillis = expiry,
                        promotions = emptyList(),
                    ),
                )
            subFlow.value = SubscriptionStatus.Inactive
            resolver().observe().test {
                val m = awaitItem()
                assertTrue(m.isPro)
                assertEquals(expiry, m.proExpiresAtEpochMillis)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `subscription overrides promotion expiry — pro becomes unlimited`() =
        runTest {
            val expiry = 1_700_000_000_000L
            every { promotion.observe() } returns
                flowOf(
                    PromotionState(
                        proActive = true,
                        proExpiresAtEpochMillis = expiry,
                        promotions = emptyList(),
                    ),
                )
            subFlow.value = SubscriptionStatus.Active("pro", autoRenew = true)
            resolver().observe().test {
                val m = awaitItem()
                assertTrue(m.isPro)
                assertNull(m.proExpiresAtEpochMillis)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
