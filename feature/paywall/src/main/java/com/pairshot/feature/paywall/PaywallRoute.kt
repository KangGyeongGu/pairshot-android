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
import dagger.hilt.android.EntryPointAccessors

private const val TERMS_URL = "https://pairshot.kangkyeonggu.com/terms"
private const val PRIVACY_URL = "https://pairshot.kangkyeonggu.com/privacy"

@Composable
fun PaywallRoute(
    dismissible: Boolean,
    onDismiss: () -> Unit,
    onEntitled: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                PaywallEvent.PurchaseFailed -> Unit
                PaywallEvent.RestoreSuccess -> onEntitled()
                PaywallEvent.RestoreEmpty -> Unit
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
        termsUrl = TERMS_URL,
        privacyUrl = PRIVACY_URL,
    )
}
