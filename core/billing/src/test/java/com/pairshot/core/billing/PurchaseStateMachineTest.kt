package com.pairshot.core.billing

import com.android.billingclient.api.Purchase
import com.pairshot.core.billing.domain.SubscriptionStatus
import com.pairshot.core.billing.internal.PurchaseStateMachine
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseStateMachineTest {
    @Test
    fun `null purchase yields Inactive`() {
        val status = PurchaseStateMachine.toStatus(null)
        assertEquals(SubscriptionStatus.Inactive, status)
    }

    @Test
    fun `purchased + auto-renewing yields Active autoRenew=true`() {
        val purchase: Purchase = mockk()
        every { purchase.products } returns listOf("pairshot_pro")
        every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { purchase.isAutoRenewing } returns true
        every { purchase.purchaseTime } returns 0L
        val status = PurchaseStateMachine.toStatus(purchase)
        assertTrue(status is SubscriptionStatus.Active)
        assertEquals(true, (status as SubscriptionStatus.Active).autoRenew)
    }

    @Test
    fun `purchased + not renewing yields Active autoRenew=false`() {
        val purchase: Purchase = mockk()
        every { purchase.products } returns listOf("pairshot_pro")
        every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { purchase.isAutoRenewing } returns false
        every { purchase.purchaseTime } returns 0L
        val status = PurchaseStateMachine.toStatus(purchase)
        assertTrue(status is SubscriptionStatus.Active)
        assertEquals(false, (status as SubscriptionStatus.Active).autoRenew)
    }

    @Test
    fun `pending purchase yields Pending`() {
        val purchase: Purchase = mockk()
        every { purchase.products } returns listOf("pairshot_pro")
        every { purchase.purchaseState } returns Purchase.PurchaseState.PENDING
        val status = PurchaseStateMachine.toStatus(purchase)
        assertTrue(status is SubscriptionStatus.Pending)
    }

    @Test
    fun `empty products yields Inactive`() {
        val purchase: Purchase = mockk()
        every { purchase.products } returns emptyList()
        val status = PurchaseStateMachine.toStatus(purchase)
        assertEquals(SubscriptionStatus.Inactive, status)
    }
}
