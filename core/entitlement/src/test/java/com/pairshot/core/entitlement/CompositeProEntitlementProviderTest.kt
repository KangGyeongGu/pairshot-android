package com.pairshot.core.entitlement

import app.cash.turbine.test
import com.pairshot.core.billing.BillingRepository
import com.pairshot.core.billing.domain.SubscriptionStatus
import com.pairshot.core.coupon.local.CouponPreferencesSource
import com.pairshot.core.coupon.local.StoredCouponState
import com.pairshot.core.domain.entitlement.EntitlementSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositeProEntitlementProviderTest {
    private val couponPreferences: CouponPreferencesSource = mockk()
    private val billing: BillingRepository = mockk()
    private val subFlow = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.Inactive)

    private fun provider(): CompositeProEntitlementProvider {
        every { billing.subscriptionStatus } returns subFlow
        return CompositeProEntitlementProvider(couponPreferences, billing)
    }

    @Test
    fun `inactive subscription and no coupon yields NONE`() =
        runTest {
            every { couponPreferences.state } returns flowOf(null)
            subFlow.value = SubscriptionStatus.Inactive
            provider().observe().test {
                val result = awaitItem()
                assertEquals(EntitlementSource.NONE, result.source)
                assertFalse(result.isActive)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `active subscription yields SUBSCRIPTION`() =
        runTest {
            every { couponPreferences.state } returns flowOf(null)
            subFlow.value = SubscriptionStatus.Active("pro", 1_000L, autoRenew = true)
            provider().observe().test {
                val result = awaitItem()
                assertEquals(EntitlementSource.SUBSCRIPTION, result.source)
                assertTrue(result.isActive)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `unlimited coupon without subscription yields COUPON`() =
        runTest {
            every { couponPreferences.state } returns
                flowOf(StoredCouponState("c1", 100L, expiresAtEpochMillis = null))
            subFlow.value = SubscriptionStatus.Inactive
            provider().observe().test {
                val result = awaitItem()
                assertEquals(EntitlementSource.COUPON, result.source)
                assertTrue(result.isActive)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `subscription takes precedence over coupon`() =
        runTest {
            every { couponPreferences.state } returns
                flowOf(StoredCouponState("c1", 100L, expiresAtEpochMillis = null))
            subFlow.value = SubscriptionStatus.Active("pro", 1_000L, autoRenew = true)
            provider().observe().test {
                val result = awaitItem()
                assertEquals(EntitlementSource.SUBSCRIPTION, result.source)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
