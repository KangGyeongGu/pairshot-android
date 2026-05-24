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
import com.pairshot.core.billing.PurchaseLaunchResult
import com.pairshot.core.billing.domain.BillingOffer
import com.pairshot.core.billing.domain.PurchaseError
import com.pairshot.core.billing.domain.SubscriptionStatus
import com.pairshot.core.billing.internal.PurchaseStateMachine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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

    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.Inactive)
    override val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()

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
    ): PurchaseLaunchResult =
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
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    PurchaseLaunchResult.Launched
                }

                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    syncStatusFromQuery()
                    PurchaseLaunchResult.AlreadyOwned
                }

                else -> {
                    PurchaseLaunchResult.Failed(mapBillingError(result.responseCode, result.debugMessage))
                }
            }
        }.getOrElse { error ->
            Timber.w(error, "launchPurchaseFlow threw")
            PurchaseLaunchResult.Failed(PurchaseError.Unknown(-1, error.message.orEmpty()))
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
            _subscriptionStatus.value = SubscriptionStatus.Inactive
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
        _subscriptionStatus.value = newStatus
        if (active != null && !active.isAcknowledged && active.purchaseState == Purchase.PurchaseState.PURCHASED) {
            acknowledgeWithRetry(active)
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (purchase.isAcknowledged) return
        acknowledgeWithRetry(purchase)
    }

    private suspend fun acknowledgeWithRetry(purchase: Purchase) {
        val params =
            AcknowledgePurchaseParams
                .newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        var delayMs = ACK_INITIAL_BACKOFF_MS
        repeat(ACK_MAX_ATTEMPTS) { attempt ->
            val result = holder.client.acknowledgePurchase(params)
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Timber.i("Acknowledged ${purchase.products.firstOrNull()} (attempt ${attempt + 1})")
                return
            }
            Timber.w("Acknowledge attempt ${attempt + 1} failed: ${result.debugMessage}")
            if (attempt < ACK_MAX_ATTEMPTS - 1) {
                delay(delayMs)
                delayMs *= 2
            }
        }
        Timber.e("Acknowledge gave up after $ACK_MAX_ATTEMPTS attempts")
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

    private fun mapBillingError(
        code: Int,
        debug: String,
    ): PurchaseError =
        when (code) {
            BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseError.UserCanceled
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> PurchaseError.BillingUnavailable
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> PurchaseError.ServiceDisconnected
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> PurchaseError.ServiceUnavailable
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> PurchaseError.ItemUnavailable
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> PurchaseError.DeveloperError
            BillingClient.BillingResponseCode.NETWORK_ERROR -> PurchaseError.NetworkError
            else -> PurchaseError.Unknown(code, debug)
        }

    private fun parseIso8601Days(period: String): Int? {
        val match = Regex("P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)W)?(?:(\\d+)D)?").matchEntire(period) ?: return null
        val (y, m, w, d) = match.destructured
        val years = y.toIntOrNull() ?: 0
        val months = m.toIntOrNull() ?: 0
        val weeks = w.toIntOrNull() ?: 0
        val days = d.toIntOrNull() ?: 0
        val total = years * DAYS_PER_YEAR + months * DAYS_PER_MONTH + weeks * DAYS_PER_WEEK + days
        return total.takeIf { it > 0 }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 8_000L
        const val DAYS_PER_WEEK = 7
        const val DAYS_PER_MONTH = 30
        const val DAYS_PER_YEAR = 365
        const val ACK_MAX_ATTEMPTS = 3
        const val ACK_INITIAL_BACKOFF_MS = 1_000L
    }
}
