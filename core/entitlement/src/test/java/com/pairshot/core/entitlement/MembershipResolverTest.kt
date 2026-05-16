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
                assertFalse(m.isAdFree)
                assertNull(m.proExpiresAtEpochMillis)
                assertNull(m.adFreeExpiresAtEpochMillis)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `active subscription grants pro and adFree with unlimited expiry`() =
        runTest {
            every { promotion.observe() } returns flowOf(PromotionState.Empty)
            subFlow.value = SubscriptionStatus.Active("pro", autoRenew = true)
            resolver().observe().test {
                val m = awaitItem()
                assertTrue(m.isPro)
                assertTrue(m.isAdFree)
                assertNull(m.proExpiresAtEpochMillis)
                assertNull(m.adFreeExpiresAtEpochMillis)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `pro promotion without subscription grants pro and adFree with promotion expiry`() =
        runTest {
            val expiry = 1_700_000_000_000L
            every { promotion.observe() } returns
                flowOf(
                    PromotionState(
                        proActive = true,
                        proExpiresAtEpochMillis = expiry,
                        adFreeActive = true,
                        adFreeExpiresAtEpochMillis = expiry,
                        promotions = emptyList(),
                    ),
                )
            subFlow.value = SubscriptionStatus.Inactive
            resolver().observe().test {
                val m = awaitItem()
                assertTrue(m.isPro)
                assertTrue(m.isAdFree)
                assertEquals(expiry, m.proExpiresAtEpochMillis)
                assertEquals(expiry, m.adFreeExpiresAtEpochMillis)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ad_free promotion alone grants adFree but not pro`() =
        runTest {
            val expiry = 1_700_000_000_000L
            every { promotion.observe() } returns
                flowOf(
                    PromotionState(
                        proActive = false,
                        proExpiresAtEpochMillis = null,
                        adFreeActive = true,
                        adFreeExpiresAtEpochMillis = expiry,
                        promotions = emptyList(),
                    ),
                )
            subFlow.value = SubscriptionStatus.Inactive
            resolver().observe().test {
                val m = awaitItem()
                assertFalse(m.isPro)
                assertTrue(m.isAdFree)
                assertNull(m.proExpiresAtEpochMillis)
                assertEquals(expiry, m.adFreeExpiresAtEpochMillis)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `subscription overrides promotion expiry — both pro and adFree become unlimited`() =
        runTest {
            val expiry = 1_700_000_000_000L
            every { promotion.observe() } returns
                flowOf(
                    PromotionState(
                        proActive = true,
                        proExpiresAtEpochMillis = expiry,
                        adFreeActive = true,
                        adFreeExpiresAtEpochMillis = expiry,
                        promotions = emptyList(),
                    ),
                )
            subFlow.value = SubscriptionStatus.Active("pro", autoRenew = true)
            resolver().observe().test {
                val m = awaitItem()
                assertTrue(m.isPro)
                assertTrue(m.isAdFree)
                assertNull(m.proExpiresAtEpochMillis)
                assertNull(m.adFreeExpiresAtEpochMillis)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
