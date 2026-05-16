package com.pairshot.feature.paywall

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pairshot.core.ads.di.AdsEntryPoint
import com.pairshot.core.billing.domain.PurchaseError
import com.pairshot.core.navigation.PaywallTrigger
import com.pairshot.core.ui.component.PairShotSnackbarController
import com.pairshot.core.ui.component.SnackbarEvent
import com.pairshot.core.ui.component.SnackbarVariant
import com.pairshot.core.ui.text.UiText
import dagger.hilt.android.EntryPointAccessors

private const val TERMS_URL = "https://pairshot.kangkyeonggu.com/terms"
private const val PRIVACY_URL = "https://pairshot.kangkyeonggu.com/privacy"

@Composable
fun PaywallRoute(
    dismissible: Boolean,
    onDismiss: () -> Unit,
    onEntitled: () -> Unit,
    trigger: PaywallTrigger = PaywallTrigger.NONE,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarController = remember { PairShotSnackbarController() }

    LaunchedEffect(trigger) {
        val messageRes =
            when (trigger) {
                PaywallTrigger.DAILY_LIMIT -> R.string.pro_hint_daily_limit
                PaywallTrigger.FEATURE_LOCKED -> R.string.pro_hint_feature_locked
                PaywallTrigger.NONE -> return@LaunchedEffect
            }
        snackbarController.show(
            SnackbarEvent(
                UiText.Resource(messageRes),
                SnackbarVariant.PRO_HINT,
            ),
        )
    }

    val fullscreenAdState =
        remember(context) {
            EntryPointAccessors
                .fromApplication(context.applicationContext, AdsEntryPoint::class.java)
                .fullscreenAdState()
        }

    DisposableEffect(fullscreenAdState) {
        val claimed = fullscreenAdState.markShown()
        onDispose {
            if (claimed) fullscreenAdState.markDismissed()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                PaywallEvent.EntitlementGranted -> onEntitled()
                PaywallEvent.AlreadyOwned -> {
                    snackbarController.show(
                        SnackbarEvent(
                            UiText.Resource(R.string.paywall_already_owned),
                            SnackbarVariant.SUCCESS,
                        ),
                    )
                    onEntitled()
                }
                is PaywallEvent.PurchaseFailed ->
                    snackbarController.show(
                        SnackbarEvent(
                            UiText.Resource(purchaseFailedStringRes(event.reason)),
                            SnackbarVariant.ERROR,
                        ),
                    )
                PaywallEvent.RestoreSuccess -> onEntitled()
                PaywallEvent.RestoreEmpty ->
                    snackbarController.show(
                        SnackbarEvent(
                            UiText.Resource(R.string.paywall_restore_no_subscription),
                            SnackbarVariant.WARNING,
                        ),
                    )
                PaywallEvent.ContinuedFree -> onEntitled()
            }
        }
    }

    PaywallScreen(
        state = uiState,
        dismissible = dismissible,
        onDismiss = onDismiss,
        onStartTrial = { offer -> activity?.let { viewModel.purchase(it, offer) } },
        onPurchaseYearly = { offer -> activity?.let { viewModel.purchase(it, offer) } },
        onPurchaseMonthly = { offer -> activity?.let { viewModel.purchase(it, offer) } },
        onContinueFree = viewModel::continueFree,
        onRestore = viewModel::restore,
        onRetryLoad = viewModel::loadOffers,
        snackbarController = snackbarController,
        termsUrl = TERMS_URL,
        privacyUrl = PRIVACY_URL,
    )
}

private fun purchaseFailedStringRes(error: PurchaseError): Int =
    when (error) {
        PurchaseError.BillingUnavailable -> R.string.paywall_purchase_failed_billing_unavailable
        PurchaseError.ServiceDisconnected,
        PurchaseError.ServiceUnavailable,
        -> R.string.paywall_purchase_failed_service_disconnected
        PurchaseError.NetworkError -> R.string.paywall_purchase_failed_network
        PurchaseError.ItemUnavailable -> R.string.paywall_purchase_failed_item_unavailable
        PurchaseError.AlreadyOwned,
        PurchaseError.UserCanceled,
        PurchaseError.DeveloperError,
        is PurchaseError.Unknown,
        -> R.string.paywall_purchase_failed_generic
    }
