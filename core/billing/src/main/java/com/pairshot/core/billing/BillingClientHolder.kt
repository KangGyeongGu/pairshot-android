package com.pairshot.core.billing

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingClientHolder
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val purchasesListener: BillingPurchaseUpdatesListener,
) {
    private val pendingParams =
        PendingPurchasesParams
            .newBuilder()
            .enableOneTimeProducts()
            .enablePrepaidPlans()
            .build()

    val client: BillingClient =
        BillingClient
            .newBuilder(context)
            .setListener(purchasesListener)
            .enablePendingPurchases(pendingParams)
            .enableAutoServiceReconnection()
            .build()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    fun ensureConnected() {
        if (client.isReady) {
            _ready.value = true
            return
        }
        client.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    val ok = result.responseCode == BillingClient.BillingResponseCode.OK
                    _ready.value = ok
                    if (!ok) {
                        Timber.w("Billing setup failed: ${result.debugMessage} (code ${result.responseCode})")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _ready.value = false
                    Timber.w("Billing service disconnected")
                }
            },
        )
    }
}
