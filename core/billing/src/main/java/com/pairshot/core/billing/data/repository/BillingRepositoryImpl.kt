package com.pairshot.core.billing.data.repository

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.pairshot.core.billing.BillingClientHolder
import com.pairshot.core.billing.BillingProductCatalog
import com.pairshot.core.billing.BillingPurchaseUpdatesListener
import com.pairshot.core.billing.BillingRepository
import com.pairshot.core.billing.domain.BillingOffer
import com.pairshot.core.billing.domain.SubscriptionStatus
import com.pairshot.core.billing.internal.PurchaseStateMachine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val holder: BillingClientHolder,
        private val updates: BillingPurchaseUpdatesListener,
    ) : BillingRepository {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private val _status = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.Inactive)
        override val subscriptionStatus: StateFlow<SubscriptionStatus> = _status.asStateFlow()

        private val productDetailsCache: ConcurrentHashMap<String, ProductDetails> = ConcurrentHashMap()

        override fun start() {
            holder.ensureConnected()
            scope.launch {
                updates.updates.collect { update ->
                    val ok = update.result.responseCode == BillingClient.BillingResponseCode.OK
                    if (!ok) {
                        Timber.w("PurchasesUpdated non-OK: ${update.result.debugMessage}")
                        return@collect
                    }
                    update.purchases.forEach { handlePurchase(it) }
                    syncStatusFromQuery()
                }
            }
            scope.launch { syncStatusFromQuery() }
        }

        override suspend fun refresh() {
            holder.ensureConnected()
            syncStatusFromQuery()
        }

        override suspend fun loadOffers(): Result<List<BillingOffer>> =
            runCatching {
                awaitReady()
                val params =
                    QueryProductDetailsParams
                        .newBuilder()
                        .setProductList(
                            BillingProductCatalog.subscriptionIds.map { id ->
                                QueryProductDetailsParams.Product
                                    .newBuilder()
                                    .setProductId(id)
                                    .setProductType(BillingClient.ProductType.SUBS)
                                    .build()
                            },
                        ).build()
                val result = holder.client.queryProductDetails(params)
                if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    error("queryProductDetails failed: ${result.billingResult.debugMessage}")
                }
                val products = result.productDetailsList.orEmpty()
                products.forEach { productDetailsCache[it.productId] = it }
                products.flatMap { product ->
                    product.subscriptionOfferDetails.orEmpty().map { offer ->
                        val finalPhase = offer.pricingPhases.pricingPhaseList.last()
                        val trialPhase = offer.pricingPhases.pricingPhaseList.firstOrNull { it.priceAmountMicros == 0L }
                        BillingOffer(
                            productId = product.productId,
                            basePlanId = offer.basePlanId,
                            offerId = offer.offerId,
                            offerToken = offer.offerToken,
                            priceFormatted = finalPhase.formattedPrice,
                            priceAmountMicros = finalPhase.priceAmountMicros,
                            priceCurrencyCode = finalPhase.priceCurrencyCode,
                            billingPeriodIso = finalPhase.billingPeriod,
                            trialDays = trialPhase?.billingPeriod?.let(::parseIso8601Days),
                        )
                    }
                }
            }

        override suspend fun launchPurchaseFlow(
            activity: Activity,
            offer: BillingOffer,
        ): Result<Unit> =
            runCatching {
                awaitReady()
                val productDetails =
                    productDetailsCache[offer.productId]
                        ?: error("ProductDetails not loaded for ${offer.productId} — call loadOffers() first")
                val flowParams =
                    BillingFlowParams
                        .newBuilder()
                        .setProductDetailsParamsList(
                            listOf(
                                BillingFlowParams.ProductDetailsParams
                                    .newBuilder()
                                    .setProductDetails(productDetails)
                                    .setOfferToken(offer.offerToken)
                                    .build(),
                            ),
                        ).build()
                val result = holder.client.launchBillingFlow(activity, flowParams)
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    error("launchBillingFlow failed: ${result.debugMessage} (code ${result.responseCode})")
                }
            }

        override fun manageSubscriptionsIntent(productId: String?): Intent {
            val base = "https://play.google.com/store/account/subscriptions"
            val uri =
                if (productId != null) {
                    "$base?sku=$productId&package=${context.packageName}"
                } else {
                    base
                }
            return Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        private suspend fun syncStatusFromQuery() {
            if (!awaitReadyOrFalse()) {
                _status.value = SubscriptionStatus.Inactive
                return
            }
            val params =
                QueryPurchasesParams
                    .newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            val result = holder.client.queryPurchasesAsync(params)
            if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Timber.w("queryPurchases failed: ${result.billingResult.debugMessage}")
                return
            }
            val active = result.purchasesList.firstOrNull()
            val newStatus = PurchaseStateMachine.toStatus(active)
            _status.value = newStatus
            if (active != null && !active.isAcknowledged && active.purchaseState == Purchase.PurchaseState.PURCHASED) {
                acknowledge(active)
            }
        }

        private suspend fun handlePurchase(purchase: Purchase) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
            if (purchase.isAcknowledged) return
            acknowledge(purchase)
        }

        private suspend fun acknowledge(purchase: Purchase) {
            val params =
                AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            val result = holder.client.acknowledgePurchase(params)
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Timber.i("Acknowledged ${purchase.products.firstOrNull()}")
            } else {
                Timber.e("Acknowledge failed: ${result.debugMessage}")
            }
        }

        private suspend fun awaitReady() {
            holder.ensureConnected()
            val ok =
                withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
                    holder.ready.filter { it }.first()
                } ?: false
            if (!ok) error("Billing client not ready within timeout")
        }

        private suspend fun awaitReadyOrFalse(): Boolean {
            holder.ensureConnected()
            return withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
                holder.ready.filter { it }.first()
            } != null
        }

        private fun parseIso8601Days(period: String): Int? {
            val match = Regex("P(?:(\\d+)W)?(?:(\\d+)D)?").matchEntire(period) ?: return null
            val weeks = match.groupValues[1].toIntOrNull() ?: 0
            val days = match.groupValues[2].toIntOrNull() ?: 0
            val total = weeks * DAYS_PER_WEEK + days
            return total.takeIf { it > 0 }
        }

        private companion object {
            const val CONNECT_TIMEOUT_MS = 8_000L
            const val DAYS_PER_WEEK = 7
        }
    }
