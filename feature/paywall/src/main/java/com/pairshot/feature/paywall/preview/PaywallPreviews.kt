package com.pairshot.feature.paywall.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.pairshot.core.billing.domain.BillingOffer
import com.pairshot.core.designsystem.PairShotTheme
import com.pairshot.feature.paywall.PaywallScreen
import com.pairshot.feature.paywall.PaywallUiState

private const val PREVIEW_MONTHLY_MICROS = 4_500_000_000L
private const val PREVIEW_YEARLY_MICROS = 43_200_000_000L
private const val PREVIEW_TRIAL_DAYS = 14

private fun sampleOffer(
    plan: String,
    price: String,
    micros: Long,
    trial: Int? = null,
): BillingOffer =
    BillingOffer(
        productId = "pairshot_pro",
        basePlanId = plan,
        offerId = if (trial != null) "trial14" else null,
        offerToken = "preview-token",
        priceFormatted = price,
        priceAmountMicros = micros,
        priceCurrencyCode = "KRW",
        billingPeriodIso = if (plan == "yearly") "P1Y" else "P1M",
        trialDays = trial,
    )

@Preview(name = "Paywall — Hard (first launch)", showBackground = true, heightDp = 800)
@Composable
@Suppress("UnusedPrivateMember")
private fun PaywallHardPreview() {
    PairShotTheme {
        PaywallScreen(
            state =
            PaywallUiState(
                loading = false,
                trialOffer = sampleOffer("monthly", "₩4,500", PREVIEW_MONTHLY_MICROS, trial = PREVIEW_TRIAL_DAYS),
                monthlyOffer = sampleOffer("monthly", "₩4,500", PREVIEW_MONTHLY_MICROS),
                yearlyOffer = sampleOffer("yearly", "₩43,200", PREVIEW_YEARLY_MICROS),
            ),
            dismissible = false,
            onDismiss = {},
            onStartTrial = {},
            onPurchaseYearly = {},
            onPurchaseMonthly = {},
            onContinueFree = {},
            onRestore = {},
            onRetryLoad = {},
            termsUrl = "",
            privacyUrl = "",
        )
    }
}

@Preview(name = "Paywall — Dismissible", showBackground = true, heightDp = 800)
@Composable
@Suppress("UnusedPrivateMember")
private fun PaywallDismissiblePreview() {
    PairShotTheme {
        PaywallScreen(
            state =
            PaywallUiState(
                loading = false,
                trialOffer = sampleOffer("monthly", "₩4,500", PREVIEW_MONTHLY_MICROS, trial = PREVIEW_TRIAL_DAYS),
                monthlyOffer = sampleOffer("monthly", "₩4,500", PREVIEW_MONTHLY_MICROS),
                yearlyOffer = sampleOffer("yearly", "₩43,200", PREVIEW_YEARLY_MICROS),
            ),
            dismissible = true,
            onDismiss = {},
            onStartTrial = {},
            onPurchaseYearly = {},
            onPurchaseMonthly = {},
            onContinueFree = {},
            onRestore = {},
            onRetryLoad = {},
            termsUrl = "",
            privacyUrl = "",
        )
    }
}

@Preview(name = "Paywall — Loading", showBackground = true, heightDp = 800)
@Composable
@Suppress("UnusedPrivateMember")
private fun PaywallLoadingPreview() {
    PairShotTheme {
        PaywallScreen(
            state = PaywallUiState(loading = true),
            dismissible = false,
            onDismiss = {},
            onStartTrial = {},
            onPurchaseYearly = {},
            onPurchaseMonthly = {},
            onContinueFree = {},
            onRestore = {},
            onRetryLoad = {},
            termsUrl = "",
            privacyUrl = "",
        )
    }
}

@Preview(name = "Paywall — Load Error", showBackground = true, heightDp = 800)
@Composable
@Suppress("UnusedPrivateMember")
private fun PaywallLoadErrorPreview() {
    PairShotTheme {
        PaywallScreen(
            state = PaywallUiState(loading = false, loadError = true),
            dismissible = false,
            onDismiss = {},
            onStartTrial = {},
            onPurchaseYearly = {},
            onPurchaseMonthly = {},
            onContinueFree = {},
            onRestore = {},
            onRetryLoad = {},
            termsUrl = "",
            privacyUrl = "",
        )
    }
}
