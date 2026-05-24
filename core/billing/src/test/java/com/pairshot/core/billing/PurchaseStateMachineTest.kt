package com.pairshot.core.billing

import com.android.billingclient.api.Purchase
import com.pairshot.core.billing.domain.SubscriptionStatus
import com.pairshot.core.billing.internal.PurchaseStateMachine
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class PurchaseStateMachineTest {
    @Test
    fun `null purchase yields Inactive`() {
        assertEquals(SubscriptionStatus.Inactive, PurchaseStateMachine.toStatus(null))
    }

    @Test
    fun `purchased + auto-renewing yields Active autoRenew=true with productId`() {
        val purchase = purchase(
            state = Purchase.PurchaseState.PURCHASED,
            autoRenew = true,
            products = listOf("pairshot_pro")
        )
        val status = PurchaseStateMachine.toStatus(purchase) as SubscriptionStatus.Active
        assertEquals("pairshot_pro", status.productId)
        assertEquals(true, status.autoRenew)
    }

    @Test
    fun `purchased + not renewing yields Active autoRenew=false`() {
        val purchase = purchase(
            state = Purchase.PurchaseState.PURCHASED,
            autoRenew = false,
            products = listOf("pairshot_pro")
        )
        val status = PurchaseStateMachine.toStatus(purchase) as SubscriptionStatus.Active
        assertEquals(false, status.autoRenew)
    }

    @Test
    fun `pending purchase yields Pending with productId`() {
        val purchase = purchase(
            state = Purchase.PurchaseState.PENDING,
            autoRenew = false,
            products = listOf("pairshot_pro")
        )
        val status = PurchaseStateMachine.toStatus(purchase) as SubscriptionStatus.Pending
        assertEquals("pairshot_pro", status.productId)
    }

    @Test
    fun `unspecified state yields Inactive`() {
        val purchase = purchase(
            state = Purchase.PurchaseState.UNSPECIFIED_STATE,
            autoRenew = false,
            products = listOf("pairshot_pro")
        )
        assertEquals(SubscriptionStatus.Inactive, PurchaseStateMachine.toStatus(purchase))
    }

    @Test
    fun `empty products yields Inactive`() {
        val purchase = purchase(state = Purchase.PurchaseState.PURCHASED, autoRenew = true, products = emptyList())
        assertEquals(SubscriptionStatus.Inactive, PurchaseStateMachine.toStatus(purchase))
    }

    @Test
    fun `multiple products picks first as productId`() {
        val purchase = purchase(
            state = Purchase.PurchaseState.PURCHASED,
            autoRenew = true,
            products = listOf("primary_id", "extra_id")
        )
        val status = PurchaseStateMachine.toStatus(purchase) as SubscriptionStatus.Active
        assertEquals("primary_id", status.productId)
    }

    private fun purchase(
        state: Int,
        autoRenew: Boolean,
        products: List<String>,
    ): Purchase {
        val p: Purchase = mockk()
        every { p.products } returns products
        every { p.purchaseState } returns state
        every { p.isAutoRenewing } returns autoRenew
        return p
    }
}
