package com.pairshot.core.billing

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PurchaseUpdate(
    val result: BillingResult,
    val purchases: List<Purchase>,
)

@Singleton
class BillingPurchaseUpdatesListener
@Inject
constructor() : PurchasesUpdatedListener {
    private val _updates =
        MutableSharedFlow<PurchaseUpdate>(
            replay = 0,
            extraBufferCapacity = UPDATE_BUFFER,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val updates: SharedFlow<PurchaseUpdate> = _updates.asSharedFlow()

    override fun onPurchasesUpdated(
        result: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        _updates.tryEmit(PurchaseUpdate(result, purchases.orEmpty()))
    }

    private companion object {
        const val UPDATE_BUFFER = 4
    }
}
